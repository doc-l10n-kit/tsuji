package net.sharplab.tsuji.core.driver.translator.deepl
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.app.exception.TsujiAppException
import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.core.driver.translator.processor.*
import org.slf4j.LoggerFactory

class DeepLTranslator(
    private val tsujiConfig: TsujiConfig,
    private val asciidoctorPreProcessor: AsciidoctorPreProcessor,
    deepLApiForTest: com.deepl.api.Translator? = null
) : Translator {

    private val logger = LoggerFactory.getLogger(DeepLTranslator::class.java)

    private val deepLApi: com.deepl.api.Translator by lazy {
        deepLApiForTest ?: createDeepLApi(tsujiConfig)
    }

    private fun createDeepLApi(config: TsujiConfig): com.deepl.api.Translator {
        val apiKey = config.translator.deepl.apiKey
        if (apiKey.isEmpty || apiKey.get().isBlank()) {
            throw TsujiAppException("DeepL API key is not configured. Please set the 'TSUJI_TRANSLATOR_DEEPL_API_KEY' environment variable or configure 'tsuji.translator.deepl.api-key' property.")
        }
        return com.deepl.api.Translator(apiKey.get())
    }

    // Processor pipeline (order is important)
    private val processors: List<MessageProcessor> by lazy {
        listOf(
            // Preprocessing: Asciidoc → HTML
            asciidoctorPreProcessor,
            // Translation
            DeepLTranslationProcessor(deepLApi),
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
    }

    override fun translate(po: Po, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): Po {
        if (useRag) {
            logger.warn("DeepL translator does not support RAG. Ignoring useRag=true parameter.")
        }

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
