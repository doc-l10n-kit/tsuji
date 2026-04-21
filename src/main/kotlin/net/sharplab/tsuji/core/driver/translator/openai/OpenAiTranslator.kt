package net.sharplab.tsuji.core.driver.translator.openai

import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.driver.translator.processor.MessageProcessor
import net.sharplab.tsuji.core.driver.translator.processor.OpenAiTranslationProcessor
import net.sharplab.tsuji.core.driver.translator.processor.XrefTitlePostProcessor
import net.sharplab.tsuji.core.model.translation.TranslationContext
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.model.Po
import org.slf4j.LoggerFactory

class OpenAiTranslator(
    openAiTranslationProcessor: OpenAiTranslationProcessor,
    mtTag: String?
) : Translator {

    private val logger = LoggerFactory.getLogger(OpenAiTranslator::class.java)

    // Default MT tag is "openai", but can be overridden (e.g., "gemini" for Gemini OpenAI-compatible API)
    private val effectiveMtTag: String = mtTag ?: "openai"

    init {
        logger.info("Using MT tag: $effectiveMtTag")
    }

    // Processor pipeline - OpenAI handles Asciidoc markup similarly to Gemini
    private val processors: List<MessageProcessor> = listOf(
        openAiTranslationProcessor,
        XrefTitlePostProcessor()
    )

    override suspend fun translate(
        po: Po,
        srcLang: String,
        dstLang: String,
        isAsciidoctor: Boolean,
        useRag: Boolean
    ): Po {
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
