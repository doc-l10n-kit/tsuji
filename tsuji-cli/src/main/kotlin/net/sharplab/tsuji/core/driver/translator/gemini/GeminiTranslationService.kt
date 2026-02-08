package net.sharplab.tsuji.core.driver.translator.gemini

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import io.quarkiverse.langchain4j.RegisterAiService

@RegisterAiService
interface GeminiTranslationService {

    @SystemMessage(fromResource = "prompts/translation-system-prompt.txt")
    fun translate(@UserMessage text: String, srcLang: String, dstLang: String): String
}
