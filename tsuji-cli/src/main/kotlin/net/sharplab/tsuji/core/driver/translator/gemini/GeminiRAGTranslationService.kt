package net.sharplab.tsuji.core.driver.translator.gemini

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.quarkiverse.langchain4j.RegisterAiService
import net.sharplab.tsuji.app.beans.TsujiRetrievalAugmentorSupplier

@RegisterAiService(retrievalAugmentor = TsujiRetrievalAugmentorSupplier::class)
interface GeminiRAGTranslationService {

    @SystemMessage(fromResource = "prompts/translation-rag-system-prompt.txt")
    fun translate(@UserMessage text: String, @V("srcLang") srcLang: String, @V("dstLang") dstLang: String): String
}
