package net.sharplab.tsuji.core.driver.translator.gemini

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.quarkiverse.langchain4j.RegisterAiService

@RegisterAiService
interface GeminiTranslationService {

    @SystemMessage(fromResource = "prompts/translation-system-prompt.txt")
    @UserMessage("Translate this text: {text}")
    fun translate(@V("text") text: String, @V("srcLang") srcLang: String, @V("dstLang") dstLang: String): String

    @SystemMessage(fromResource = "prompts/translation-batch-system-prompt.txt")
    @UserMessage("Translate these texts: {request}")
    fun translateBatch(
        @V("request") request: BatchTranslationRequest,
        @V("srcLang") srcLang: String,
        @V("dstLang") dstLang: String
    ): BatchTranslationResponse
}
