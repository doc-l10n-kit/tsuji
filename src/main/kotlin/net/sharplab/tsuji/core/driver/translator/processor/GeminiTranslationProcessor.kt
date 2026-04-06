package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveBatchController
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.exception.*
import net.sharplab.tsuji.core.driver.translator.gemini.BatchTranslationRequest
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationService
import net.sharplab.tsuji.po.model.type
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
            val batchController = AdaptiveBatchController(
                initialSize = maxTextsPerRequest,
                minSize = 1,
                maxSize = maxTextsPerRequest
            )

            val translated = processBatchesSequentially(
                normalTexts,
                context,
                batchController
            )

            translated.forEachIndexed { i, translatedText ->
                val index = normalIndices[i]
                result[index] = result[index]
                    .withText(translatedText)
                    .withFuzzy(true)
            }
        }

        return result
    }

    private suspend fun processBatchesSequentially(
        texts: List<String>,
        context: TranslationContext,
        batchController: AdaptiveBatchController
    ): List<String> {
        val results = mutableListOf<String>()
        var remainingTexts = texts

        while (remainingTexts.isNotEmpty()) {
            val batchSize = batchController.getCurrentBatchSize()
            val batches = splitIntoBatches(remainingTexts, batchSize)

            logger.info("Processing ${remainingTexts.size} texts in ${batches.size} batch(es) (size: $batchSize)")

            // Sequential batch processing
            batches.forEachIndexed { index, batch ->
                val batchResult = processBatchWithRetry(
                    batch,
                    context,
                    batchController,
                    index + 1,
                    batches.size
                )
                results.addAll(batchResult)
            }

            remainingTexts = emptyList()
        }

        return results
    }

    private suspend fun processBatchWithRetry(
        batch: List<String>,
        context: TranslationContext,
        batchController: AdaptiveBatchController,
        batchNumber: Int,
        totalBatches: Int
    ): List<String> {
        repeat(maxRetries) { attempt ->
            try {
                logger.info("Translating batch $batchNumber/$totalBatches (${batch.size} texts, attempt ${attempt + 1})")

                // All API calls go through parallelismController
                val result = parallelismController.executeWithControl {
                    translateBatchWithValidation(batch, context)
                }

                batchController.onBatchSuccess()
                parallelismController.onRequestSuccess()
                return result

            } catch (e: RateLimitException) {
                // Rate limit: reduce parallelism + backoff
                parallelismController.onRateLimitError()
                logger.warn("Rate limit error, retrying in ${1000L * (attempt + 1)}ms")
                delay(1000L * (attempt + 1))
                if (attempt == maxRetries - 1) {
                    throw e
                }

            } catch (e: TranslationValidationException) {
                // LLM response validation error: reduce batch size
                logger.warn("Validation error: ${e.javaClass.simpleName}: ${e.message}")
                if (attempt == maxRetries - 1) {
                    throw e
                }

                // Reduce batch size and re-split
                val newSize = batchController.onValidationError()
                logger.info("Reducing batch size to $newSize due to validation error, re-splitting batch")

                // Re-split and retry recursively
                return batch.chunked(newSize).flatMap { subBatch ->
                    processBatchWithRetry(subBatch, context, batchController, batchNumber, totalBatches)
                }

            } catch (e: Exception) {
                // Transport errors (network, timeout, etc.): simple retry without batch size change
                logger.warn("Transport error: ${e.javaClass.simpleName}: ${e.message}, retrying")
                delay(1000L * (attempt + 1))
                if (attempt == maxRetries - 1) {
                    throw e
                }
            }
        }
        error("Unreachable")
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
                val response = geminiTranslationService.translateBatch(request, context.srcLang, context.dstLang)

                // Validation: response size must match request size
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

    /**
     * Splits texts into batches according to Gemini API limits.
     *
     * Gemini API limits (conservative settings):
     * - Maximum texts per request (adaptive, starts at maxTextsPerRequest)
     * - Maximum request body size of ~50 KiB (accounting for prompt overhead)
     */
    private fun splitIntoBatches(texts: List<String>, maxBatchSize: Int): List<List<String>> {
        val batches = mutableListOf<List<String>>()
        var currentBatch = mutableListOf<String>()
        var currentSizeBytes = 0

        texts.forEach { text ->
            val textSizeBytes = text.toByteArray(Charsets.UTF_8).size

            // Check if adding this text would exceed limits
            if (currentBatch.size >= maxBatchSize ||
                (currentBatch.isNotEmpty() && currentSizeBytes + textSizeBytes > maxTextSizeBytes)
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
}
