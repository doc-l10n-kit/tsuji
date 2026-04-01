package net.sharplab.tsuji.core.driver.translator.deepl

import com.deepl.api.DeepLException
import com.deepl.api.Formality
import com.deepl.api.TextTranslationOptions
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Typed
import net.sharplab.tsuji.app.exception.TsujiAppException
import net.sharplab.tsuji.core.driver.translator.Translator
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.util.Optional

@Dependent
@Typed(DeepLTranslator::class)
class DeepLTranslator(
    @param:ConfigProperty(name = "tsuji.translator.deepl.api-key")
    private val apiKey: Optional<String>
) : Translator {

    private val logger = LoggerFactory.getLogger(DeepLTranslator::class.java)

    private val deepLApi by lazy {
        if (apiKey.isEmpty || apiKey.get().isBlank()) {
            throw TsujiAppException("DeepL API key is not configured. Please set the 'TSUJI_TRANSLATOR_DEEPL_API_KEY' environment variable or configure 'tsuji.translator.deepl.api-key' property.")
        }
        com.deepl.api.Translator(apiKey.get())
    }

    override fun translate(texts: List<String>, srcLang: String, dstLang: String, useRag: Boolean): List<String> {
        if (useRag) {
            logger.warn("DeepL translator does not support RAG. Ignoring useRag=true parameter.")
        }

        if (texts.isEmpty()) {
            return emptyList()
        }

        val options = TextTranslationOptions()
        options.nonSplittingTags = INLINE_ELEMENT_NAMES
        options.ignoreTags = IGNORE_ELEMENT_NAMES
        options.tagHandling = "xml"
        options.formality = Formality.PreferMore

        return try {
            deepLApi.translateText(texts, srcLang, dstLang, options).map { it.text }
        } catch (e: DeepLException) {
            throw DeepLTranslatorException("DeepL API error occurred", e)
        }
    }

    companion object {
        // Tags whose content should not be translated
        private val IGNORE_ELEMENT_NAMES = listOf(
            "abbr", "b", "cite", "code", "data", "dfn", "kbd", "rp", "rt", "rtc", "ruby", "samp", "time", "var"
        )

        // Inline element tags that should be preserved as atomic units
        private val INLINE_ELEMENT_NAMES = listOf(
            "a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "data", "dfn", "em", "i", "kbd",
            "mark", "q", "rp", "rt", "rtc", "ruby", "s", "samp", "small", "span", "strong", "sub",
            "sup", "time", "u", "var", "wbr"
        )
    }
}
