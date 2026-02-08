package net.sharplab.tsuji.core.driver.translator.gemini

import net.sharplab.tsuji.core.driver.translator.Translator
import org.slf4j.LoggerFactory
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.exception.TsujiAppException
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional

@Dependent
class GeminiTranslator(
    private val geminiTranslationService: GeminiTranslationService,
    private val geminiRAGTranslationService: GeminiRAGTranslationService,
    @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.api-key")
    private val apiKey: Optional<String>
) : Translator {

    private val logger = LoggerFactory.getLogger(GeminiTranslator::class.java)

    override fun translate(texts: List<String>, srcLang: String, dstLang: String, useRag: Boolean): List<String> {
        if (apiKey.isEmpty || apiKey.get().isBlank()) {
            throw TsujiAppException("Gemini API key is not configured. Please set the 'QUARKUS_LANGCHAIN4J_GEMINI_API_KEY' environment variable.")
        }
        return texts.mapIndexed {
            index, text ->
            logger.info("Translating [${index + 1}/${texts.size}]: source=$srcLang, target=$dstLang, useRag=$useRag, text='${text.take(50)}'")
            // Escape curly braces for LangChain4j prompt template to avoid "Variable not found" errors
            val escapedText = text.replace("{", "{{").replace("}", "}}")
            if (useRag) {
                geminiRAGTranslationService.translate(escapedText, srcLang, dstLang)
            } else {
                geminiTranslationService.translate(escapedText, srcLang, dstLang)
            }
        }
    }
}
