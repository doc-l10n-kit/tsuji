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
    @UserMessage("Translate this text: {text}")
    fun translate(@V("text") text: String, @V("srcLang") srcLang: String, @V("dstLang") dstLang: String): String

    @SystemMessage(
        "You are a professional translator for open-source project documentation.",
        "Translate the given texts from {srcLang} to {dstLang}.",
        "",
        "CRITICAL RULES:",
        "- Translate EACH text EXACTLY as provided - do NOT improvise, modify, or create alternative content.",
        "- Maintain the technical context of the documentation.",
        "- Preserve all HTML/Asciidoc tags exactly as they are.",
        "- Do not translate the content inside <code> tags if it represents code or technical identifiers.",
        "- Ensure consistent terminology usage.",
        "- Return results in the EXACT same order and count as the input.",
        "- Return results as a JSON object with a 'translations' array.",
        "",
        "IMPORTANT: ",
        "- Return ONLY the JSON object, without markdown code blocks or additional text.",
        "- Each translation must correspond 1:1 with the input text at the same index.",
        "- The output array MUST have the exact same number of elements as the input array."
    )
    @UserMessage("Translate these texts: {request}")
    fun translateBatch(
        @V("request") request: BatchTranslationRequest,
        @V("srcLang") srcLang: String,
        @V("dstLang") dstLang: String
    ): BatchTranslationResponse
}
