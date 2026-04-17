package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.adaptive.BatchProvider
import net.sharplab.tsuji.core.driver.translator.adaptive.GeminiBatchProvider
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationAiService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationAiService
import net.sharplab.tsuji.core.driver.translator.validator.AsciidocMarkupValidator
import net.sharplab.tsuji.core.model.translation.TranslationContext
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Translation processor using Gemini API via LangChain4j.
 * Translates efficiently using batch processing with adaptive batch size and retry logic.
 */
class GeminiTranslationProcessor(
    private val geminiTranslationAiService: GeminiTranslationAiService,
    private val geminiRAGTranslationAiService: GeminiRAGTranslationAiService,
    initialTextsPerRequest: Int = 200,
    maxTextsPerRequest: Int = 200,
    maxRetries: Int = 3,
    parallelismController: AdaptiveParallelismController,
    asciidocMarkupValidator: AsciidocMarkupValidator
) : AbstractLangchain4jTranslationProcessor(
    mtTag = "gemini",
    initialTextsPerRequest = initialTextsPerRequest,
    maxTextsPerRequest = maxTextsPerRequest,
    maxRetries = maxRetries,
    parallelismController = parallelismController,
    asciidocMarkupValidator = asciidocMarkupValidator
) {

    override val logger: Logger = LoggerFactory.getLogger(GeminiTranslationProcessor::class.java)

    override fun createBatchProvider(
        items: List<TranslationMessage>,
        initialLimit: Int,
        minLimit: Int,
        maxLimit: Int
    ): BatchProvider<TranslationMessage> {
        return GeminiBatchProvider(
            items = items,
            initialLimit = initialLimit,
            minLimit = minLimit,
            maxLimit = maxLimit
        )
    }

    override suspend fun callTranslationApi(
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
        } catch (e: Exception) {
            handleTranslationException(e)
        }
    }

    override suspend fun callTranslationApiWithNotes(
        items: List<net.sharplab.tsuji.core.driver.translator.gemini.BatchTranslationRequestItem>,
        context: TranslationContext
    ): List<String> {
        return try {
            if (context.useRag) {
                logger.debug("Batch of ${items.size} texts using RAG batch translation with notes")
                geminiRAGTranslationAiService.translateWithNotes(items, context.srcLang, context.dstLang)
            } else {
                logger.debug("Sending batch: ${items.size} items with notes")

                val batchStartTime = System.currentTimeMillis()
                val result = geminiTranslationAiService.translateWithNotes(items, context.srcLang, context.dstLang)
                val batchElapsedTime = System.currentTimeMillis() - batchStartTime

                logger.debug("Batch translation completed in ${batchElapsedTime}ms (batch size: ${items.size})")
                result
            }
        } catch (e: Exception) {
            handleTranslationException(e)
        }
    }
}
