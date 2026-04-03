package net.sharplab.tsuji.core.driver.translator.gemini

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.quarkiverse.langchain4j.RegisterAiService

@RegisterAiService
interface GeminiTranslationService {

    @SystemMessage(
        "You are a professional translator for open-source project documentation.",
        "Translate the given text from {srcLang} to {dstLang}.",
        "",
        "Rules:",
        "- Maintain the technical context of the documentation.",
        "- Preserve all HTML/Asciidoc tags (e.g., <a data-doc-l10n-kit-type=\"...\">, <code>, <em>, etc.) exactly as they are.",
        "- Do not translate the content inside <code> tags if it represents code or technical identifiers.",
        "- Ensure consistent terminology usage."
    )
    fun translate(@UserMessage text: String, @V("srcLang") srcLang: String, @V("dstLang") dstLang: String): String
}
