package net.sharplab.tsuji.core.driver.translator.processor

import com.deepl.api.DeepLException
import com.deepl.api.Formality
import com.deepl.api.TextTranslationOptions
import net.sharplab.tsuji.core.driver.translator.deepl.DeepLTranslatorException
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.model.po.SessionKey
import net.sharplab.tsuji.core.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * Translation processor using DeepL API.
 * Translates efficiently using batch processing.
 */
class DeepLTranslationProcessor(
    private val deepLApi: com.deepl.api.Translator
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(DeepLTranslationProcessor::class.java)

    override fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage> {
        if (messages.isEmpty()) {
            return messages
        }

        val options = TextTranslationOptions()
        options.nonSplittingTags = INLINE_ELEMENT_NAMES
        options.ignoreTags = IGNORE_ELEMENT_NAMES
        options.tagHandling = "xml"
        options.formality = Formality.PreferMore

        // Classify messages
        val jekyllIndices = mutableListOf<Int>()
        val normalIndices = mutableListOf<Int>()
        val fillIndices = mutableListOf<Int>()
        val skipIndices = mutableListOf<Int>()

        messages.forEachIndexed { index, msg ->
            when {
                msg.messageId.isEmpty() -> skipIndices.add(index)
                msg.messageString.isNotEmpty() -> skipIndices.add(index)
                MessageClassifier.shouldFillWithMessageId(msg) -> fillIndices.add(index)
                MessageClassifier.isJekyllFrontMatter(msg.messageId) -> jekyllIndices.add(index)
                else -> normalIndices.add(index)
            }
        }

        val result = messages.toMutableList()

        // Fill processing (no translation needed)
        fillIndices.forEach { index ->
            result[index] = result[index].copyWithTranslation(
                messageString = result[index].messageId,
                fuzzy = false
            )
        }

        // Jekyll Front Matter processing (individual translation)
        jekyllIndices.forEach { index ->
            logger.info("Translating Jekyll Front Matter [${index + 1}/${messages.size}]")
            val msg = messages[index]
            val textToTranslate = msg.getSession(SessionKey.PREPROCESSED_TEXT) ?: msg.messageId
            val translated = translateJekyllFrontMatter(textToTranslate, context.srcLang, context.dstLang, options)
            result[index] = result[index].copyWithTranslation(
                messageString = translated,
                fuzzy = true
            )
        }

        // Normal translation (batch processing)
        if (normalIndices.isNotEmpty()) {
            val normalTexts = normalIndices.map {
                val msg = messages[it]
                // Use preprocessed text from session if available, otherwise use messageId
                msg.getSession(SessionKey.PREPROCESSED_TEXT) ?: msg.messageId
            }
            val textBatches = splitIntoBatches(normalTexts)
            logger.info("Translating ${normalTexts.size} normal texts in ${textBatches.size} batch(es) (${context.srcLang} -> ${context.dstLang})")

            val translated = try {
                textBatches.flatMapIndexed { batchIndex, textBatch ->
                    logger.info("Translating batch ${batchIndex + 1}/${textBatches.size} (${textBatch.size} texts)")
                    deepLApi.translateText(textBatch, context.srcLang, context.dstLang, options).map { it.text }
                }
            } catch (e: DeepLException) {
                throw DeepLTranslatorException("DeepL API error occurred", e)
            }

            translated.forEachIndexed { i, translatedText ->
                val index = normalIndices[i]
                result[index] = result[index].copyWithTranslation(
                    messageString = translatedText,
                    fuzzy = true
                )
            }
        }

        return result
    }

    /**
     * Translates Jekyll Front Matter format.
     * Only translates title and synopsis fields, preserving others.
     */
    private fun translateJekyllFrontMatter(
        message: String,
        srcLang: String,
        dstLang: String,
        options: TextTranslationOptions
    ): String {
        val titleRegex = Regex("""^title:\s*(.*)$""", RegexOption.MULTILINE)
        val synopsisRegex = Regex("""^synopsis:\s*(.*)$""", RegexOption.MULTILINE)

        val titleMatch = titleRegex.find(message)
        val synopsisMatch = synopsisRegex.find(message)

        val title = titleMatch?.groupValues?.get(1)?.trim() ?: ""
        val synopsis = synopsisMatch?.groupValues?.get(1)?.trim() ?: ""

        val stringsToTranslate = mutableListOf<String>()
        if (title.isNotEmpty()) stringsToTranslate.add(title)
        if (synopsis.isNotEmpty()) stringsToTranslate.add(synopsis)

        if (stringsToTranslate.isEmpty()) return message

        // Translate title and synopsis
        val translated = try {
            deepLApi.translateText(stringsToTranslate, srcLang, dstLang, options).map { it.text }
        } catch (e: DeepLException) {
            throw DeepLTranslatorException("DeepL API error occurred", e)
        }

        // Replace in the original message
        var translatedIndex = 0
        var replaced = message
        if (title.isNotEmpty()) {
            val titleTranslated = translated[translatedIndex++]
            replaced = titleRegex.replace(replaced) { "title: $titleTranslated" }
        }
        if (synopsis.isNotEmpty()) {
            val synopsisTranslated = translated[translatedIndex++]
            replaced = synopsisRegex.replace(replaced) { "synopsis: $synopsisTranslated" }
        }
        return replaced
    }

    /**
     * Splits texts into batches according to DeepL API limits.
     *
     * DeepL API limits:
     * - Maximum 50 texts per request
     * - Maximum request body size of 128 KiB
     *
     * Uses 100,000 bytes (approx. 97 KiB) as limit to ensure margin for JSON overhead.
     */
    private fun splitIntoBatches(texts: List<String>): List<List<String>> {
        val batches = mutableListOf<List<String>>()
        var currentBatch = mutableListOf<String>()
        var currentSizeBytes = 0

        texts.forEach { text ->
            val textSizeBytes = text.toByteArray(Charsets.UTF_8).size

            // Check if adding this text would exceed limits
            if (currentBatch.size >= MAX_TEXTS_PER_REQUEST ||
                (currentBatch.isNotEmpty() && currentSizeBytes + textSizeBytes > MAX_TEXT_SIZE_BYTES)
            ) {
                // Save current batch and start a new one
                batches.add(currentBatch)
                currentBatch = mutableListOf()
                currentSizeBytes = 0
            }

            currentBatch.add(text)
            currentSizeBytes += textSizeBytes
        }

        // Add the last batch
        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch)
        }

        return batches
    }

    companion object {
        // DeepL API limits
        private const val MAX_TEXTS_PER_REQUEST = 50
        // Use 100,000 bytes (approx. 97 KiB) instead of 128 KiB to ensure margin for JSON overhead
        private const val MAX_TEXT_SIZE_BYTES = 100_000

        // Tags to ignore (not translate)
        private val IGNORE_ELEMENT_NAMES = listOf(
            "abbr", "b", "cite", "code", "data", "dfn", "kbd", "rp", "rt", "rtc", "ruby", "samp", "time", "var"
        )

        // Inline element tags not to split
        private val INLINE_ELEMENT_NAMES = listOf(
            "a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "data", "dfn", "em", "i", "kbd",
            "mark", "q", "rp", "rt", "rtc", "ruby", "s", "samp", "small", "span", "strong", "sub",
            "sup", "time", "u", "var", "wbr"
        )
    }
}
