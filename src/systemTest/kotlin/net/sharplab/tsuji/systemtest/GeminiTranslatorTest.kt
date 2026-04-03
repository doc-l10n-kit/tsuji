package net.sharplab.tsuji.systemtest

import dev.langchain4j.data.segment.TextSegment
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslator
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@QuarkusTest
class GeminiTranslatorTest {

    private val logger = LoggerFactory.getLogger(GeminiTranslatorTest::class.java)

    @Inject
    lateinit var translator: GeminiTranslator

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
        val texts = listOf("Hello, how are you?")
        val result = translator.translate(texts, "en", "ja", useRag = false)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isNotEmpty()
        logger.info("Result: ${result[0]}")
    }

    @Test
    fun translate_withRag_shouldWork() {
        // Add sample data to the vector store so RAG has something to retrieve
        vectorStoreDriver.addAll(listOf(
            TextSegment.from("Source: WebAuthn is a standard for secure web authentication. Target: WebAuthn はセキュアなWeb認証のための標準規格です。")
        ))

        val texts = listOf("WebAuthn is a standard for secure web authentication.")
        val result = translator.translate(texts, "en", "ja", useRag = true)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isNotEmpty()
        logger.info("Result (RAG): ${result[0]}")
    }
}
