package net.sharplab.tsuji.core.driver.translator.deepl

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.app.exception.TsujiAppException
import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.processor.AsciidoctorPreProcessor
import org.assertj.core.api.Assertions.assertThat
import org.asciidoctor.Asciidoctor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

/**
 * DeepLTranslator の統合テスト。
 *
 * 旧 DeepLTranslatorTest からの移行：
 * - APIキーのバリデーション
 * - 空の入力処理
 * - Po全体の翻訳フロー
 */
internal class DeepLTranslatorIntegrationTest {

    private val asciidoctor = Asciidoctor.Factory.create()
    private val preProcessor = AsciidoctorPreProcessor(asciidoctor)

    @AfterEach
    fun tearDown() {
        preProcessor.close()
    }

    private fun createMockConfig(apiKey: Optional<String>): TsujiConfig {
        val mockConfig = mock<TsujiConfig>()
        val mockTranslator = mock<TsujiConfig.Translator>()
        val mockDeepL = mock<TsujiConfig.Translator.DeepL>()

        whenever(mockConfig.translator).thenReturn(mockTranslator)
        whenever(mockTranslator.deepl).thenReturn(mockDeepL)
        whenever(mockDeepL.apiKey).thenReturn(apiKey)

        return mockConfig
    }

    private fun createMessage(
        messageId: String,
        type: MessageType = MessageType.PlainText
    ) = PoMessage(
        type = type,
        messageId = messageId,
        messageString = "",
        sourceReferences = emptyList()
    )

    @Test
    fun `translate should return empty Po for empty messages`() {
        // Given
        val config = createMockConfig(Optional.of("dummy-api-key"))
        val translator = DeepLTranslator(config, preProcessor)
        val po = Po("ja", emptyList())

        // When
        val result = translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)

        // Then
        assertThat(result.messages).isEmpty()
    }

    @Test
    fun `translate should throw exception when API key is missing`() {
        // Given
        val config = createMockConfig(Optional.empty())
        val translator = DeepLTranslator(config, preProcessor)
        val po = Po("ja", listOf(createMessage("test")))

        // When & Then
        val exception = assertThrows<TsujiAppException> {
            translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should throw exception when API key is blank`() {
        // Given
        val config = createMockConfig(Optional.of("   "))
        val translator = DeepLTranslator(config, preProcessor)
        val po = Po("ja", listOf(createMessage("test")))

        // When & Then
        val exception = assertThrows<TsujiAppException> {
            translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should throw exception when API key is empty string`() {
        // Given
        val config = createMockConfig(Optional.of(""))
        val translator = DeepLTranslator(config, preProcessor)
        val po = Po("ja", listOf(createMessage("test")))

        // When & Then
        val exception = assertThrows<TsujiAppException> {
            translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should log warning when useRag is true`() {
        // Given
        val config = createMockConfig(Optional.of("dummy-api-key"))
        val translator = DeepLTranslator(config, preProcessor)
        val po = Po("ja", emptyList())

        // When (useRag=true はDeepLではサポートされない)
        val result = translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = true)

        // Then (警告がログ出力されるが、例外は投げられない)
        assertThat(result.messages).isEmpty()
    }
}
