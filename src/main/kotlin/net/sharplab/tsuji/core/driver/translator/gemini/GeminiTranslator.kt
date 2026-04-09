package net.sharplab.tsuji.core.driver.translator.gemini
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.core.driver.translator.processor.*
import org.slf4j.LoggerFactory

class GeminiTranslator(
    private val geminiTranslationAiService: GeminiTranslationAiService,
    private val geminiRAGTranslationAiService: GeminiRAGTranslationAiService,
    private val initialTextsPerRequest: Int = 200,
    private val maxTextsPerRequest: Int = 200,
    private val maxTextSizeBytes: Int = 700000,
    private val maxRetries: Int = 3,
    private val parallelismController: AdaptiveParallelismController
) : Translator {

    private val logger = LoggerFactory.getLogger(GeminiTranslator::class.java)

    // Processor pipeline - Gemini handles Asciidoc markup natively
    private val processors = listOf(
        // Translation only - no Asciidoc preprocessing/postprocessing
        GeminiTranslationProcessor(
            geminiTranslationAiService,
            geminiRAGTranslationAiService,
            initialTextsPerRequest,
            maxTextsPerRequest,
            maxTextSizeBytes,
            maxRetries,
            parallelismController
        )
    )

    override suspend fun translate(po: Po, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): Po {
        val messages = po.messages
        if (messages.isEmpty()) {
            return po
        }

        // Convert PoMessage → TranslationMessage
        val translationMessages = messages.map { TranslationMessage.from(it) }

        val context = TranslationContext(
            po = po,
            srcLang = srcLang,
            dstLang = dstLang,
            isAsciidoctor = isAsciidoctor,
            useRag = useRag
        )

        // Execute processor pipeline sequentially
        var processedMessages = translationMessages
        for (processor in processors) {
            processedMessages = processor.process(processedMessages, context)
        }

        // Convert TranslationMessage → PoMessage
        val finalMessages = processedMessages.map { it.toPoMessage() }
        return Po(po.target, finalMessages, po.header)
    }

}
