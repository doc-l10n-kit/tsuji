package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.adaptive.BatchProvider
import net.sharplab.tsuji.core.driver.translator.adaptive.OpenAiBatchProvider
import net.sharplab.tsuji.core.driver.translator.openai.OpenAiRAGTranslationAiService
import net.sharplab.tsuji.core.driver.translator.openai.OpenAiTranslationAiService
import net.sharplab.tsuji.core.driver.translator.validator.AsciidocMarkupValidator
import net.sharplab.tsuji.core.model.translation.TranslationContext
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Translation processor using OpenAI-compatible API via LangChain4j.
 * Translates efficiently using batch processing with adaptive batch size and retry logic.
 */
class OpenAiTranslationProcessor(
    private val openAiTranslationAiService: OpenAiTranslationAiService,
    private val openAiRAGTranslationAiService: OpenAiRAGTranslationAiService,
    mtTag: String,
    initialTextsPerRequest: Int = 200,
    maxTextsPerRequest: Int = 200,
    maxRetries: Int = 3,
    parallelismController: AdaptiveParallelismController,
    asciidocMarkupValidator: AsciidocMarkupValidator
) : AbstractLangchain4jTranslationProcessor(
    mtTag = mtTag,
    initialTextsPerRequest = initialTextsPerRequest,
    maxTextsPerRequest = maxTextsPerRequest,
    maxRetries = maxRetries,
    parallelismController = parallelismController,
    asciidocMarkupValidator = asciidocMarkupValidator
) {

    override val logger: Logger = LoggerFactory.getLogger(OpenAiTranslationProcessor::class.java)

    override fun createBatchProvider(
        items: List<TranslationMessage>,
        initialLimit: Int,
        minLimit: Int,
        maxLimit: Int
    ): BatchProvider<TranslationMessage> {
        return OpenAiBatchProvider(
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
                openAiRAGTranslationAiService.translate(texts, context.srcLang, context.dstLang)
            } else {
                logger.debug("Sending batch: ${texts.size} items")

                val batchStartTime = System.currentTimeMillis()
                val result = openAiTranslationAiService.translate(texts, context.srcLang, context.dstLang)
                val batchElapsedTime = System.currentTimeMillis() - batchStartTime

                logger.debug("Batch translation completed in ${batchElapsedTime}ms (batch size: ${texts.size})")
                result
            }
        } catch (e: Exception) {
            handleTranslationException(e)
        }
    }
}
