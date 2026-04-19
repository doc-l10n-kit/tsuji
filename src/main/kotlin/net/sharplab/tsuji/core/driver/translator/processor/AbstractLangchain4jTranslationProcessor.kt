package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.adaptive.BatchedExecutor
import net.sharplab.tsuji.core.driver.translator.adaptive.CountBasedBatchProvider
import net.sharplab.tsuji.core.driver.translator.exception.AsciidocMarkupValidationException
import net.sharplab.tsuji.core.driver.translator.exception.RateLimitException
import net.sharplab.tsuji.core.driver.translator.exception.ResponseParseException
import net.sharplab.tsuji.core.driver.translator.exception.TranslationValidationException
import net.sharplab.tsuji.core.driver.translator.model.BatchTranslationRequestItem
import net.sharplab.tsuji.core.driver.translator.validator.AsciidocMarkupValidator
import net.sharplab.tsuji.core.model.translation.TranslationContext
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.util.MessageClassifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Abstract base processor for LangChain4j-based translation services.
 * Provides common batch processing, Jekyll Front Matter handling, and Asciidoc validation logic.
 */
abstract class AbstractLangchain4jTranslationProcessor(
    protected val mtTag: String,
    private val initialTextsPerRequest: Int,
    private val maxTextsPerRequest: Int,
    private val maxRetries: Int,
    private val parallelismController: AdaptiveParallelismController,
    private val asciidocMarkupValidator: AsciidocMarkupValidator
) : MessageProcessor {

    protected abstract val logger: Logger

    companion object {
        private const val MAX_MARKUP_RETRIES = 2
    }

    private fun createBatchProvider(
        items: List<TranslationMessage>,
        initialLimit: Int,
        minLimit: Int,
        maxLimit: Int
    ): CountBasedBatchProvider<TranslationMessage> {
        return CountBasedBatchProvider(
            items = items,
            initialLimit = initialLimit,
            minLimit = minLimit,
            maxLimit = maxLimit
        )
    }

    /**
     * Calls the translation API (with or without RAG) for normal translation.
     */
    protected abstract suspend fun callTranslationApi(
        texts: List<String>,
        context: TranslationContext
    ): List<String>

    /**
     * Calls the translation API with notes for retry with validation guidance.
     */
    protected abstract suspend fun callTranslationApiWithNotes(
        items: List<BatchTranslationRequestItem>,
        context: TranslationContext
    ): List<String>

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
            val translated = translateJekyllFrontMatter(msg.text, context.srcLang, context.dstLang, context.useRag, context)
            result[index] = result[index]
                .withText(translated)
                .withFuzzy(true)
                .withMtEngine(mtTag)
        }

        // Normal translation with adaptive batch processing
        if (normalIndices.isNotEmpty()) {
            val normalMessages = normalIndices.map { messages[it] }

            logger.info("Translating ${normalMessages.size} texts (${context.srcLang} -> ${context.dstLang})")
            logger.debug("Current parallelism controller state: concurrency=${parallelismController.getCurrentConcurrency()}")
            val processStartTime = System.currentTimeMillis()

            val batchProvider = createBatchProvider(
                items = normalMessages,
                initialLimit = initialTextsPerRequest,
                minLimit = 1,
                maxLimit = maxTextsPerRequest
            )
            val executor = BatchedExecutor(
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
        val texts = batch.map { it.text }
        val translations = callTranslationApi(texts, context)
        var messages = batch.zip(translations).map { (msg, translated) ->
            msg.withText(translated).withFuzzy(true).withMtEngine(mtTag)
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
                    logger.info("Retrying ${e.brokenTranslations.size} broken translation(s) (attempt ${attempt + 1}/$MAX_MARKUP_RETRIES)")
                    messages = retranslateBroken(messages, e.brokenTranslations, context)
                } else {
                    logger.warn("Asciidoc markup still broken in ${e.brokenTranslations.size} translation(s), accepting anyway")
                    val brokenIds = e.brokenTranslations.map { it.message.original.messageId }.toSet()
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
        brokenTranslations: List<net.sharplab.tsuji.core.driver.translator.exception.BrokenTranslation>,
        context: TranslationContext
    ): List<TranslationMessage> {
        val items = brokenTranslations.mapIndexed { index, broken ->
            BatchTranslationRequestItem(
                index = index,
                text = broken.message.original.messageId,
                note = broken.note
            )
        }

        val retranslated = callTranslationApiWithNotes(items, context)
        val fixes = brokenTranslations.zip(retranslated).associate { (broken, newText) ->
            broken.message.original.messageId to newText
        }
        return messages.map { msg -> fixes[msg.original.messageId]?.let { msg.withText(it) } ?: msg }
    }

    /**
     * Translates Jekyll Front Matter format.
     * Only translates title and synopsis fields, preserving others.
     */
    private suspend fun translateJekyllFrontMatter(message: String, srcLang: String, dstLang: String, useRag: Boolean, context: TranslationContext): String {
        val titleRegex = Regex("""^title:\s*(.*)$""", RegexOption.MULTILINE)
        val synopsisRegex = Regex("""^synopsis:\s*(.*)$""", RegexOption.MULTILINE)

        val titleMatch = titleRegex.find(message)
        val synopsisMatch = synopsisRegex.find(message)

        val title = titleMatch?.groupValues?.get(1)?.trim() ?: ""
        val synopsis = synopsisMatch?.groupValues?.get(1)?.trim() ?: ""

        val texts = mutableListOf<String>()
        if (title.isNotEmpty()) texts.add(title)
        if (synopsis.isNotEmpty()) texts.add(synopsis)

        if (texts.isEmpty()) return message

        val translated = callTranslationApi(texts, context)

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
     * Common exception handling for translation API calls.
     */
    protected fun handleTranslationException(e: Exception): Nothing {
        when {
            e is TranslationValidationException -> throw e

            e.message?.contains("429") == true ||
            e.message?.contains("quota") == true ||
            e.message?.contains("rate limit") == true ->
                throw RateLimitException("Rate limit exceeded: ${e.message}")

            e is com.fasterxml.jackson.core.JsonProcessingException ||
            e.message?.contains("JSON") == true ||
            e.message?.contains("parse") == true ->
                throw ResponseParseException("Failed to parse translation response", e)

            else -> throw e
        }
    }
}
