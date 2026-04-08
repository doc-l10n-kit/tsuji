package net.sharplab.tsuji.systemtest

import dev.langchain4j.data.segment.TextSegment
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlinx.coroutines.runBlocking

@QuarkusTest
class GeminiTranslatorTest {

    // Helper extension to call suspend function from test
    private fun Translator.translateBlocking(
        po: Po,
        srcLang: String,
        dstLang: String,
        isAsciidoctor: Boolean,
        useRag: Boolean
    ): Po = runBlocking { translate(po, srcLang, dstLang, isAsciidoctor, useRag) }


    private val logger = LoggerFactory.getLogger(GeminiTranslatorTest::class.java)

    @Inject
    lateinit var translator: Translator

    @Inject
    lateinit var vectorStoreDriver: VectorStoreDriver

    @BeforeEach
    fun checkApiKey() {
        val apiKey = ConfigProvider.getConfig()
            .getOptionalValue("quarkus.langchain4j.ai.gemini.api-key", String::class.java)

        Assumptions.assumeTrue(
            apiKey.isPresent && apiKey.get().isNotBlank(),
            "Gemini API Key is not configured, skipping test."
        )
    }

    @Test
    fun translate_shouldWork() {
        val message = PoMessage("Hello, how are you?", "", emptyList())
        val po = Po(target = "", messages = listOf(message))
        val result = translator.translateBlocking(po, "en", "ja", isAsciidoctor = false, useRag = false)

        assertThat(result.messages).hasSize(1)
        assertThat(result.messages[0].messageString).isNotEmpty()
        logger.info("Result: ${result.messages[0].messageString}")
    }

    @Test
    fun translate_withRag_shouldWork() {
        // Add sample data to the vector store so RAG has something to retrieve
        vectorStoreDriver.addAll(listOf(
            TextSegment.from("Source: WebAuthn is a standard for secure web authentication. Target: WebAuthn はセキュアなWeb認証のための標準規格です。")
        ))

        val message = PoMessage("WebAuthn is a standard for secure web authentication.", "", emptyList())
        val po = Po(target = "", messages = listOf(message))
        val result = translator.translateBlocking(po, "en", "ja", isAsciidoctor = false, useRag = true)

        assertThat(result.messages).hasSize(1)
        assertThat(result.messages[0].messageString).isNotEmpty()
        logger.info("Result (RAG): ${result.messages[0].messageString}")
    }
}
