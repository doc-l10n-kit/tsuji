package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import kotlinx.coroutines.runBlocking
import net.sharplab.tsuji.core.driver.translator.adaptive.GeminiBatchProvider
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.exception.*
import net.sharplab.tsuji.core.driver.translator.gemini.BatchTranslationRequest
import net.sharplab.tsuji.core.driver.translator.gemini.BatchTranslationResponse
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationService
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * Translation processor using Gemini API.
 * Translates efficiently using batch processing with adaptive batch size and retry logic.
 */
class GeminiTranslationProcessor(
    private val geminiTranslationService: GeminiTranslationService,
    private val geminiRAGTranslationService: GeminiRAGTranslationService,
    private val maxTextsPerRequest: Int = 10,
    private val maxTextSizeBytes: Int = 50000,
    private val maxRetries: Int = 3,
    private val parallelismController: AdaptiveParallelismController
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(GeminiTranslationProcessor::class.java)

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

        // Normal translation with adaptive batch processing
        if (normalIndices.isNotEmpty()) {
            val normalTexts = normalIndices.map { messages[it].text }

            logger.info("Translating ${normalTexts.size} texts (${context.srcLang} -> ${context.dstLang})")

            // Create batch provider and executor
            val batchProvider = GeminiBatchProvider(
                items = normalTexts,
                initialLimit = maxTextsPerRequest,
                minLimit = 1,
                maxLimit = maxTextsPerRequest
            )
            val executor = net.sharplab.tsuji.core.driver.translator.adaptive.BatchedExecutor(
                batchProvider = batchProvider,
                maxRetries = maxRetries,
                maxValidationRetries = 5
            )

            val translated = executor.execute { batch ->
                // Execute translation for this batch through parallelism controller
                parallelismController.execute {
                    translateBatchWithValidation(batch, context)
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

    private suspend fun translateBatchWithValidation(
        batch: List<String>,
        context: TranslationContext
    ): List<String> {
        return try {
            if (context.useRag) {
                // TODO: RAG batch support will be added in Phase 2
                // For now, fall back to individual translation for RAG
                batch.map { text ->
                    geminiRAGTranslationService.translate(text, context.srcLang, context.dstLang)
                }
            } else {
                val request = BatchTranslationRequest(batch)
                logger.debug("Sending batch: ${batch.size} items")

                // Call batch translation
                // 送信: {"0": "text1", "1": "text2", ...}（Jacksonが自動変換）
                // 受信: String形式のJSON
                val responseJson = geminiTranslationService.translateBatch(request, context.srcLang, context.dstLang)

                // Parse JSON response manually
                val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                val response = try {
                    mapper.readValue(responseJson, BatchTranslationResponse::class.java)
                } catch (e: Exception) {
                    logger.error("Failed to parse response JSON: $responseJson", e)
                    throw ResponseParseException("Failed to parse LLM response: ${e.message}", e)
                }

                // Validate response keys match request keys
                val expectedKeys = batch.indices.map { it.toString() }.toSet()
                val actualKeys = response.keys

                if (expectedKeys != actualKeys) {
                    val missing = expectedKeys - actualKeys
                    val extra = actualKeys - expectedKeys
                    throw KeyMismatchException(
                        message = "Translation key mismatch: missing=$missing, extra=$extra",
                        expectedKeys = expectedKeys,
                        actualKeys = actualKeys
                    )
                }

                // Also check size for backward compatibility
                if (response.translations.size != batch.size) {
                    throw BatchSizeMismatchException(
                        expected = batch.size,
                        actual = response.translations.size,
                        message = "Batch translation size mismatch: expected ${batch.size}, got ${response.translations.size}"
                    )
                }

                response.translations
            }

        } catch (e: TranslationValidationException) {
            // Re-throw validation exceptions as-is
            throw e

        } catch (e: Exception) {
            // Detect rate limit errors
            if (e.message?.contains("429") == true ||
                e.message?.contains("quota") == true ||
                e.message?.contains("rate limit") == true) {
                throw RateLimitException("Rate limit exceeded: ${e.message}")
            }

            // Detect JSON parse errors (LLM returned invalid format)
            if (e is com.fasterxml.jackson.core.JsonProcessingException ||
                e.message?.contains("JSON") == true) {
                throw ResponseParseException("Failed to parse LLM response: ${e.message}", e)
            }

            // Other errors: propagate as-is (will be treated as transport errors)
            throw e
        }
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

}
