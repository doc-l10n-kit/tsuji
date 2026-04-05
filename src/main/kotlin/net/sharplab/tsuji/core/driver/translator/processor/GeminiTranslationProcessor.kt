package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.gemini.BatchTranslationRequest
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationService
import net.sharplab.tsuji.po.model.type
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * Translation processor using Gemini API.
 * Translates efficiently using batch processing.
 */
class GeminiTranslationProcessor(
    private val geminiTranslationService: GeminiTranslationService,
    private val geminiRAGTranslationService: GeminiRAGTranslationService
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(GeminiTranslationProcessor::class.java)

    override fun process(messages: List<TranslationMessage>, context: TranslationContext): List<TranslationMessage> {
        if (messages.isEmpty()) {
            return messages
        }

        // Classify messages
        val jekyllIndices = mutableListOf<Int>()
        val normalIndices = mutableListOf<Int>()
        val fillIndices = mutableListOf<Int>()
        val skipIndices = mutableListOf<Int>()

        messages.forEachIndexed { index, msg ->
            when {
                !msg.needsTranslation -> skipIndices.add(index)
                msg.isEmpty() -> skipIndices.add(index) // Skip empty text
                MessageClassifier.shouldFillWithMessageId(msg.original) -> fillIndices.add(index)
                MessageClassifier.isJekyllFrontMatter(msg.original.messageId) -> jekyllIndices.add(index)
                else -> normalIndices.add(index)
            }
        }

        val result = messages.toMutableList()

        // Fill processing (no translation needed)
        fillIndices.forEach { index ->
            result[index] = result[index]
                .withText(result[index].original.messageId)
                .withFuzzy(false)
        }

        // Jekyll Front Matter processing (individual translation)
        jekyllIndices.forEach { index ->
            logger.info("Translating Jekyll Front Matter [${index + 1}/${messages.size}]")
            val msg = messages[index]
            val translated = translateJekyllFrontMatter(msg.text, context.srcLang, context.dstLang, context.useRag)
            result[index] = result[index]
                .withText(translated)
                .withFuzzy(true)
        }

        // Normal translation (batch processing)
        if (normalIndices.isNotEmpty()) {
            val normalTexts = normalIndices.map { messages[it].text }
            val textBatches = splitIntoBatches(normalTexts)
            logger.info("Translating ${normalTexts.size} normal texts in ${textBatches.size} batch(es) (${context.srcLang} -> ${context.dstLang})")

            val translated = textBatches.flatMapIndexed { batchIndex, textBatch ->
                logger.info("Translating batch ${batchIndex + 1}/${textBatches.size} (${textBatch.size} texts)")

                if (context.useRag) {
                    // TODO: RAG batch support will be added in Phase 2
                    // For now, fall back to individual translation for RAG
                    textBatch.map { text ->
                        geminiRAGTranslationService.translate(text, context.srcLang, context.dstLang)
                    }
                } else {
                    val request = BatchTranslationRequest(textBatch)
                    val response = geminiTranslationService.translateBatch(request, context.srcLang, context.dstLang)

                    // Size validation
                    if (response.translations.size != textBatch.size) {
                        throw RuntimeException(
                            "Batch translation size mismatch: expected ${textBatch.size}, got ${response.translations.size}"
                        )
                    }

                    response.translations
                }
            }

            translated.forEachIndexed { i, translatedText ->
                val index = normalIndices[i]
                result[index] = result[index]
                    .withText(translatedText)
                    .withFuzzy(true)
            }
        }

        return result
    }

    /**
     * Translates Jekyll Front Matter format.
     * Only translates title and synopsis fields, preserving others.
     *
     * In the future, this could be improved to handle structured formats more flexibly using prompts.
     */
    private fun translateJekyllFrontMatter(message: String, srcLang: String, dstLang: String, useRag: Boolean): String {
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

        // Translate title and synopsis individually
        val translated = stringsToTranslate.map { text ->
            if (useRag) {
                geminiRAGTranslationService.translate(text, srcLang, dstLang)
            } else {
                geminiTranslationService.translate(text, srcLang, dstLang)
            }
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
     * Splits texts into batches according to Gemini API limits.
     *
     * Gemini API limits (conservative settings):
     * - Maximum 30 texts per request (vs DeepL's 50, accounting for JSON overhead)
     * - Maximum request body size of ~50 KiB (vs DeepL's 100 KiB, accounting for prompt overhead)
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
        // Gemini API limits (conservative settings)
        private const val MAX_TEXTS_PER_REQUEST = 30
        // Use 50,000 bytes (approx. 49 KiB) to ensure margin for JSON and prompt overhead
        private const val MAX_TEXT_SIZE_BYTES = 50_000
    }
}
