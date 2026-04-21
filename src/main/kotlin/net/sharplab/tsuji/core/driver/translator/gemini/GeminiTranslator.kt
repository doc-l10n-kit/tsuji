package net.sharplab.tsuji.core.driver.translator.gemini
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.core.driver.translator.processor.GeminiTranslationProcessor
import net.sharplab.tsuji.core.driver.translator.processor.MessageProcessor
import net.sharplab.tsuji.core.driver.translator.processor.XrefTitlePostProcessor

class GeminiTranslator(
    geminiTranslationProcessor: GeminiTranslationProcessor
) : Translator {

    // Processor pipeline - Gemini handles Asciidoc markup natively
    private val processors: List<MessageProcessor> = listOf(
        geminiTranslationProcessor,
        XrefTitlePostProcessor()
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
