package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.model.BatchTranslationRequestItem
import net.sharplab.tsuji.core.driver.translator.service.RAGTranslationAiService
import net.sharplab.tsuji.core.driver.translator.service.TranslationAiService
import net.sharplab.tsuji.core.driver.translator.util.MessageTypeNoteGenerator
import net.sharplab.tsuji.core.driver.translator.validator.AsciidocMarkupValidator
import net.sharplab.tsuji.core.model.translation.TranslationContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Translation processor using Gemini API via LangChain4j.
 * Translates efficiently using batch processing with adaptive batch size and retry logic.
 */
class GeminiTranslationProcessor(
    private val translationAiService: TranslationAiService,
    private val ragTranslationAiService: RAGTranslationAiService,
    initialTextsPerRequest: Int = 200,
    maxTextsPerRequest: Int = 200,
    maxRetries: Int = 3,
    parallelismController: AdaptiveParallelismController,
    asciidocMarkupValidator: AsciidocMarkupValidator,
    messageTypeNoteGenerator: MessageTypeNoteGenerator
) : AbstractLangchain4jTranslationProcessor(
    mtTag = "gemini",
    initialTextsPerRequest = initialTextsPerRequest,
    maxTextsPerRequest = maxTextsPerRequest,
    maxRetries = maxRetries,
    parallelismController = parallelismController,
    asciidocMarkupValidator = asciidocMarkupValidator,
    messageTypeNoteGenerator = messageTypeNoteGenerator
) {

    override val logger: Logger = LoggerFactory.getLogger(GeminiTranslationProcessor::class.java)

    override suspend fun callTranslationApi(
        texts: List<String>,
        context: TranslationContext
    ): List<String> {
        return try {
            if (context.useRag) {
                logger.debug("Batch of ${texts.size} texts using RAG batch translation")
                ragTranslationAiService.translate(texts, context.srcLang, context.dstLang, context.isAsciidoctor)
            } else {
                logger.debug("Sending batch: ${texts.size} items")

                val batchStartTime = System.currentTimeMillis()
                val result = translationAiService.translate(texts, context.srcLang, context.dstLang, context.isAsciidoctor)
                val batchElapsedTime = System.currentTimeMillis() - batchStartTime

                logger.debug("Batch translation completed in ${batchElapsedTime}ms (batch size: ${texts.size})")
                result
            }
        } catch (e: Exception) {
            handleTranslationException(e)
        }
    }

    override suspend fun callTranslationApiWithNotes(
        items: List<BatchTranslationRequestItem>,
        context: TranslationContext
    ): List<String> {
        return try {
            if (context.useRag) {
                logger.debug("Batch of ${items.size} texts using RAG batch translation with notes")
                ragTranslationAiService.translateWithNotes(items, context.srcLang, context.dstLang, context.isAsciidoctor)
            } else {
                logger.debug("Sending batch: ${items.size} items with notes")

                val batchStartTime = System.currentTimeMillis()
                val result = translationAiService.translateWithNotes(items, context.srcLang, context.dstLang, context.isAsciidoctor)
                val batchElapsedTime = System.currentTimeMillis() - batchStartTime

                logger.debug("Batch translation completed in ${batchElapsedTime}ms (batch size: ${items.size})")
                result
            }
        } catch (e: Exception) {
            handleTranslationException(e)
        }
    }
}
