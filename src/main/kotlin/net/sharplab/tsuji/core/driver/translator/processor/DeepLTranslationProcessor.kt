package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import com.deepl.api.DeepLException
import com.deepl.api.Formality
import com.deepl.api.TextTranslationOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.sharplab.tsuji.core.driver.translator.adaptive.DeepLBatchProvider
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.deepl.DeepLTranslatorException
import net.sharplab.tsuji.core.driver.translator.exception.RateLimitException
import net.sharplab.tsuji.core.model.translation.TranslationMessage
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

    override suspend fun process(
        messages: List<TranslationMessage>,
        context: TranslationContext
    ): List<TranslationMessage> {
        if (messages.isEmpty()) {
            return messages
        }

        val options = TextTranslationOptions()
        options.nonSplittingTags = INLINE_ELEMENT_NAMES
        options.ignoreTags = IGNORE_ELEMENT_NAMES
        options.tagHandling = "html"  // Use "html" instead of "xml" to support HTML entities
        options.formality = Formality.PreferMore

        val classified = classifyMessages(messages)

        val result = messages.toMutableList()

        // Fill processing (no translation needed)
        classified.fillIndices.forEach { index ->
            result[index] = result[index]
                .withText(result[index].original.messageId)
                .withFuzzy(false)
        }

        // Jekyll Front Matter processing (individual translation with retry)
        classified.jekyllIndices.forEach { index ->
            logger.info("Translating Jekyll Front Matter [${index + 1}/${messages.size}]")
            val msg = messages[index]
            val translated = translateJekyllFrontMatterWithRetry(msg.text, context.srcLang, context.dstLang, options)
            result[index] = result[index]
                .withText(translated)
                .withFuzzy(true)
                .withMtEngine("deepl")
        }

        // Normal translation (batch processing with retry)
        if (classified.normalIndices.isNotEmpty()) {
            val normalTexts = classified.normalIndices.map { messages[it].text }

            logger.info("Translating ${normalTexts.size} texts (${context.srcLang} -> ${context.dstLang})")

            // Create batch provider and executor
            val batchProvider = DeepLBatchProvider(
                items = normalTexts,
                initialLimit = MAX_TEXT_SIZE_BYTES,
                minLimit = MAX_TEXT_SIZE_BYTES,
                maxLimit = MAX_TEXT_SIZE_BYTES
            )
            val executor = net.sharplab.tsuji.core.driver.translator.adaptive.BatchedExecutor(
                batchProvider = batchProvider,
                maxRetries = maxRetries,
                maxBatchValidationRetries = 5
            )

            val translated = executor.execute { batch ->
                // Execute translation for this batch through parallelism controller
                parallelismController.execute {
                    try {
                        withContext(Dispatchers.IO) {
                            deepLApi.translateText(batch, context.srcLang, context.dstLang, options).map { it.text }
                        }
                    } catch (e: DeepLException) {
                        // Convert DeepL rate limit errors to RateLimitException
                        if (isRateLimitError(e)) {
                            throw RateLimitException("DeepL rate limit exceeded: ${e.message}")
                        }
                        throw e
                    }
                }
            }

            translated.forEachIndexed { i, translatedText ->
                val index = classified.normalIndices[i]
                result[index] = result[index]
                    .withText(translatedText)
                    .withFuzzy(true)
                    .withMtEngine("deepl")
            }
        }

        return result
    }

    private fun isRateLimitError(e: DeepLException): Boolean {
        return e.message?.contains("429") == true ||
               e.message?.contains("quota") == true ||
               e.message?.contains("rate limit", ignoreCase = true) == true ||
               e.message?.contains("too many requests", ignoreCase = true) == true
    }

    private suspend fun translateJekyllFrontMatterWithRetry(
        message: String,
        srcLang: String,
        dstLang: String,
        options: TextTranslationOptions
    ): String {
        val fields = JekyllFrontMatterUtil.extractFields(message)
        if (fields.isEmpty()) return message
        val translated = translateTextsWithRetry(fields, srcLang, dstLang, options)
        return JekyllFrontMatterUtil.replaceFields(message, translated)
    }

    private suspend fun translateTextsWithRetry(
        texts: List<String>,
        srcLang: String,
        dstLang: String,
        options: TextTranslationOptions
    ): List<String> {
        repeat(maxRetries) { attempt ->
            try {
                // Success/error callbacks are automatically handled inside executeWithControl
                return parallelismController.execute {
                    try {
                        withContext(Dispatchers.IO) {
                            deepLApi.translateText(texts, srcLang, dstLang, options).map { it.text }
                        }
                    } catch (e: DeepLException) {
                        // Convert DeepL rate limit errors to RateLimitException
                        if (isRateLimitError(e)) {
                            throw RateLimitException("DeepL rate limit exceeded: ${e.message}")
                        }
                        throw e
                    }
                }

            } catch (e: RateLimitException) {
                // Rate limit: already handled by parallelismController, just retry
                logger.warn("DeepL rate limit error, retrying in ${1000L * (attempt + 1)}ms")
                delay(1000L * (attempt + 1))
                if (attempt == maxRetries - 1) {
                    throw e
                }

            } catch (e: DeepLException) {
                logger.warn("DeepL API error: ${e.message}, retrying")
                delay(1000L * (attempt + 1))
                if (attempt == maxRetries - 1) {
                    throw DeepLTranslatorException("DeepL API error occurred after $maxRetries retries", e)
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
