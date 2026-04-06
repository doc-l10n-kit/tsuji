package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import com.deepl.api.DeepLException
import com.deepl.api.Formality
import com.deepl.api.TextTranslationOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.deepl.DeepLTranslatorException
import net.sharplab.tsuji.core.driver.translator.exception.RateLimitException
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * Translation processor using DeepL API.
 * Translates efficiently using batch processing with retry logic.
 */
class DeepLTranslationProcessor(
    private val deepLApi: com.deepl.api.Translator,
    private val maxRetries: Int = 3,
    private val parallelismController: AdaptiveParallelismController
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(DeepLTranslationProcessor::class.java)

    override fun process(messages: List<TranslationMessage>, context: TranslationContext): List<TranslationMessage> {
        return runBlocking {
            processAsync(messages, context)
        }
    }

    private suspend fun processAsync(
        messages: List<TranslationMessage>,
        context: TranslationContext
    ): List<TranslationMessage> {
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

        // Jekyll Front Matter processing (individual translation with retry)
        jekyllIndices.forEach { index ->
            logger.info("Translating Jekyll Front Matter [${index + 1}/${messages.size}]")
            val msg = messages[index]
            val translated = translateJekyllFrontMatterWithRetry(msg.text, context.srcLang, context.dstLang, options)
            result[index] = result[index]
                .withText(translated)
                .withFuzzy(true)
        }

        // Normal translation (batch processing with retry)
        if (normalIndices.isNotEmpty()) {
            val normalTexts = normalIndices.map { messages[it].text }
            val textBatches = splitIntoBatches(normalTexts)
            logger.info("Translating ${normalTexts.size} normal texts in ${textBatches.size} batch(es) (${context.srcLang} -> ${context.dstLang})")

            val translated = textBatches.flatMapIndexed { batchIndex, textBatch ->
                translateBatchWithRetry(textBatch, context.srcLang, context.dstLang, options, batchIndex + 1, textBatches.size)
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

    private suspend fun translateBatchWithRetry(
        batch: List<String>,
        srcLang: String,
        dstLang: String,
        options: TextTranslationOptions,
        batchNumber: Int,
        totalBatches: Int
    ): List<String> {
        repeat(maxRetries) { attempt ->
            try {
                logger.info("Translating batch $batchNumber/$totalBatches (${batch.size} texts, attempt ${attempt + 1})")

                // All API calls go through parallelismController
                val result = parallelismController.executeWithControl {
                    deepLApi.translateText(batch, srcLang, dstLang, options).map { it.text }
                }

                parallelismController.onRequestSuccess()
                return result

            } catch (e: DeepLException) {
                // Check if it's a rate limit error
                if (isRateLimitError(e)) {
                    parallelismController.onRateLimitError()
                    logger.warn("DeepL rate limit error, retrying in ${1000L * (attempt + 1)}ms")
                    delay(1000L * (attempt + 1))
                    if (attempt == maxRetries - 1) {
                        throw RateLimitException("DeepL rate limit exceeded: ${e.message}")
                    }
                } else {
                    // Other DeepL errors: simple retry with exponential backoff
                    logger.warn("DeepL API error: ${e.message}, retrying")
                    delay(1000L * (attempt + 1))
                    if (attempt == maxRetries - 1) {
                        throw DeepLTranslatorException("DeepL API error occurred after $maxRetries retries", e)
                    }
                }
            } catch (e: Exception) {
                // Transport errors: simple retry
                logger.warn("Transport error: ${e.javaClass.simpleName}: ${e.message}, retrying")
                delay(1000L * (attempt + 1))
                if (attempt == maxRetries - 1) {
                    throw e
                }
            }
        }
        error("Unreachable")
    }

    private fun isRateLimitError(e: DeepLException): Boolean {
        return e.message?.contains("429") == true ||
               e.message?.contains("quota") == true ||
               e.message?.contains("rate limit", ignoreCase = true) == true ||
               e.message?.contains("too many requests", ignoreCase = true) == true
    }

    /**
     * Translates Jekyll Front Matter format with retry logic.
     * Only translates title and synopsis fields, preserving others.
     */
    private suspend fun translateJekyllFrontMatterWithRetry(
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

        // Translate title and synopsis with retry
        val translated = translateTextsWithRetry(stringsToTranslate, srcLang, dstLang, options)

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

    private suspend fun translateTextsWithRetry(
        texts: List<String>,
        srcLang: String,
        dstLang: String,
        options: TextTranslationOptions
    ): List<String> {
        repeat(maxRetries) { attempt ->
            try {
                return parallelismController.executeWithControl {
                    deepLApi.translateText(texts, srcLang, dstLang, options).map { it.text }
                }
            } catch (e: DeepLException) {
                if (isRateLimitError(e)) {
                    parallelismController.onRateLimitError()
                    logger.warn("DeepL rate limit error, retrying in ${1000L * (attempt + 1)}ms")
                    delay(1000L * (attempt + 1))
                    if (attempt == maxRetries - 1) {
                        throw RateLimitException("DeepL rate limit exceeded: ${e.message}")
                    }
                } else {
                    logger.warn("DeepL API error: ${e.message}, retrying")
                    delay(1000L * (attempt + 1))
                    if (attempt == maxRetries - 1) {
                        throw DeepLTranslatorException("DeepL API error occurred after $maxRetries retries", e)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Transport error: ${e.javaClass.simpleName}: ${e.message}, retrying")
                delay(1000L * (attempt + 1))
                if (attempt == maxRetries - 1) {
                    throw e
                }
            }
        }
        error("Unreachable")
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
