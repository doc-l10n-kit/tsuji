package net.sharplab.tsuji.core.driver.translator.gemini
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.core.driver.translator.processor.*
import org.slf4j.LoggerFactory

class GeminiTranslator(
    private val geminiTranslationService: GeminiTranslationService,
    private val geminiRAGTranslationService: GeminiRAGTranslationService,
    private val asciidoctorPreProcessor: AsciidoctorPreProcessor
) : Translator {

    private val logger = LoggerFactory.getLogger(GeminiTranslator::class.java)

    // Processor pipeline (order is important)
    private val processors = listOf(
        // Preprocessing: Asciidoc → HTML
        asciidoctorPreProcessor,
        // Translation
        GeminiTranslationProcessor(geminiTranslationService, geminiRAGTranslationService),
        // Postprocessing: HTML → Asciidoc
        LinkTagMessageProcessor(),
        ImageTagMessageProcessor(),
        DecorationTagMessageProcessor("em", "_", "_"),
        DecorationTagMessageProcessor("strong", "*", "*"),
        DecorationTagMessageProcessor("monospace", "`", "`"),
        DecorationTagMessageProcessor("superscript", "^", "^"),
        DecorationTagMessageProcessor("subscript", "~", "~"),
        DecorationTagMessageProcessor("code", "`", "`"),
        CharacterReferenceUnescaper()  // Unescape at the end
    )

    override fun translate(po: Po, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): Po {
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
        val processedMessages = processors.fold(translationMessages) { msgs, processor ->
            processor.process(msgs, context)
        }

        // Convert TranslationMessage → PoMessage
        val finalMessages = processedMessages.map { it.toPoMessage() }
        return Po(po.target, finalMessages, po.header)
    }

}
