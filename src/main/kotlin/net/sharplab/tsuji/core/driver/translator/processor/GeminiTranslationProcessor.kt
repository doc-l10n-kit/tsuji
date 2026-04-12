package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.adaptive.GeminiBatchProvider
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.exception.AsciidocMarkupValidationException
import net.sharplab.tsuji.core.driver.translator.exception.RateLimitException
import net.sharplab.tsuji.core.driver.translator.exception.ResponseParseException
import net.sharplab.tsuji.core.driver.translator.exception.TranslationValidationException
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationAiService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationAiService
import net.sharplab.tsuji.core.driver.translator.validator.AsciidocMarkupValidator
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * Translation processor using Gemini API.
 * Translates efficiently using batch processing with adaptive batch size and retry logic.
 */
class GeminiTranslationProcessor(
    private val geminiTranslationAiService: GeminiTranslationAiService,
    private val geminiRAGTranslationAiService: GeminiRAGTranslationAiService,
    private val initialTextsPerRequest: Int = 200,
    private val maxTextsPerRequest: Int = 200,
    private val maxRetries: Int = 3,
    private val parallelismController: AdaptiveParallelismController,
    private val asciidocMarkupValidator: AsciidocMarkupValidator
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(GeminiTranslationProcessor::class.java)

    companion object {
        private const val MAX_MARKUP_RETRIES = 2
    }

    override suspend fun process(
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

        messages.forEachIndexed { index, msg ->
            when {
                !msg.needsTranslation -> {} // skip
                msg.isEmpty() -> {} // skip
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
                .withMtEngine("gemini")
        }

        // Normal translation with adaptive batch processing
        if (normalIndices.isNotEmpty()) {
            val normalMessages = normalIndices.map { messages[it] }

            logger.info("Translating ${normalMessages.size} texts (${context.srcLang} -> ${context.dstLang})")
            logger.debug("Current parallelism controller state: concurrency=${parallelismController.getCurrentConcurrency()}")
            val processStartTime = System.currentTimeMillis()

            val batchProvider = GeminiBatchProvider(
                items = normalMessages,
                initialLimit = initialTextsPerRequest,
                minLimit = 1,
                maxLimit = maxTextsPerRequest
            )
            val executor = net.sharplab.tsuji.core.driver.translator.adaptive.BatchedExecutor(
                batchProvider = batchProvider,
                maxRetries = maxRetries,
                maxValidationRetries = 5
            )

            val translated = executor.execute { batch ->
                parallelismController.execute {
                    translateBatch(batch, context)
                }
            }

            translated.forEachIndexed { i, translatedMsg ->
                result[normalIndices[i]] = translatedMsg
            }

            val processElapsedTime = System.currentTimeMillis() - processStartTime
            logger.info("Translation process completed in ${processElapsedTime}ms for ${normalMessages.size} texts (avg: ${processElapsedTime / normalMessages.size}ms/text)")
        }

        return result
    }

    private suspend fun translateBatch(
        batch: List<TranslationMessage>,
        context: TranslationContext
    ): List<TranslationMessage> {
        val translations = callTranslationApi(batch.map { it.text }, context)
        var messages = batch.zip(translations).map { (msg, translated) ->
            msg.withText(translated).withFuzzy(true).withMtEngine("gemini")
        }

        if (!context.isAsciidoctor) {
            return messages
        }

        repeat(MAX_MARKUP_RETRIES + 1) { attempt ->
            try {
                asciidocMarkupValidator.validate(messages)
                return messages
            } catch (e: AsciidocMarkupValidationException) {
                if (attempt < MAX_MARKUP_RETRIES) {
                    logger.info("Retrying ${e.brokenMessages.size} broken translation(s) (attempt ${attempt + 1}/$MAX_MARKUP_RETRIES)")
                    messages = retranslateBroken(messages, e.brokenMessages, context)
                } else {
                    logger.warn("Asciidoc markup still broken in ${e.brokenMessages.size} translation(s), accepting anyway")
                    val brokenIds = e.brokenMessages.map { it.original.messageId }.toSet()
                    messages = messages.map { msg ->
                        if (msg.original.messageId in brokenIds) {
                            msg.withComment("WARNING: Asciidoc markup may be broken in this translation")
                        } else {
                            msg
                        }
                    }
                }
            }
        }

        return messages
    }

    private suspend fun retranslateBroken(
        messages: List<TranslationMessage>,
        broken: List<TranslationMessage>,
        context: TranslationContext
    ): List<TranslationMessage> {
        val retranslated = callTranslationApi(broken.map { it.original.messageId }, context)
        val fixes = broken.zip(retranslated).associate { (msg, newText) -> msg.original.messageId to newText }
        return messages.map { msg -> fixes[msg.original.messageId]?.let { msg.withText(it) } ?: msg }
    }

    private suspend fun callTranslationApi(
        texts: List<String>,
        context: TranslationContext
    ): List<String> {
        return try {
            if (context.useRag) {
                logger.debug("Batch of ${texts.size} texts using RAG batch translation")
                geminiRAGTranslationAiService.translate(texts, context.srcLang, context.dstLang)
            } else {
                logger.debug("Sending batch: ${texts.size} items")

                val batchStartTime = System.currentTimeMillis()
                val result = geminiTranslationAiService.translate(texts, context.srcLang, context.dstLang)
                val batchElapsedTime = System.currentTimeMillis() - batchStartTime

                logger.debug("Batch translation completed in ${batchElapsedTime}ms (batch size: ${texts.size})")
                result
            }

        } catch (e: TranslationValidationException) {
            throw e

        } catch (e: Exception) {
            if (e.message?.contains("429") == true ||
                e.message?.contains("quota") == true ||
                e.message?.contains("rate limit") == true) {
                throw RateLimitException("Rate limit exceeded: ${e.message}")
            }

            if (e is com.fasterxml.jackson.core.JsonProcessingException ||
                e.message?.contains("JSON") == true) {
                throw ResponseParseException("Failed to parse LLM response: ${e.message}", e)
            }

            throw e
        }
    }

    /**
     * Translates Jekyll Front Matter format.
     * Only translates title and synopsis fields, preserving others.
     */
    private suspend fun translateJekyllFrontMatter(message: String, srcLang: String, dstLang: String, useRag: Boolean): String {
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

        val translated = if (useRag) {
            geminiRAGTranslationAiService.translate(stringsToTranslate, srcLang, dstLang)
        } else {
            geminiTranslationAiService.translate(stringsToTranslate, srcLang, dstLang)
        }

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
