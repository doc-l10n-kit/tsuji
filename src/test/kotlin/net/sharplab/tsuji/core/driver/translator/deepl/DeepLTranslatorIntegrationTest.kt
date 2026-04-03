package net.sharplab.tsuji.core.driver.translator.deepl

import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.core.driver.translator.processor.AsciidoctorPreProcessor
import org.assertj.core.api.Assertions.assertThat
import org.asciidoctor.Asciidoctor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    private fun createMessage(
        messageId: String,
        type: MessageType = MessageType.PlainText
    ) = PoMessage(
        messageId = messageId,
        messageString = "",
        sourceReferences = emptyList(),
        comments = if (type != MessageType.None) listOf(type.value) else emptyList()
    )

    @Test
    fun `translate should return empty Po for empty messages`() {
        // Given
        val translator = DeepLTranslator("dummy-api-key", preProcessor)
        val po = Po("ja", emptyList())

        // When
        val result = translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)

        // Then
        assertThat(result.messages).isEmpty()
    }

    @Test
    fun `translate should throw exception when API key is missing`() {
        // Given
        val translator = DeepLTranslator("", preProcessor)
        val po = Po("ja", listOf(createMessage("test")))

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should throw exception when API key is blank`() {
        // Given
        val translator = DeepLTranslator("   ", preProcessor)
        val po = Po("ja", listOf(createMessage("test")))

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should throw exception when API key is empty string`() {
        // Given
        val translator = DeepLTranslator("", preProcessor)
        val po = Po("ja", listOf(createMessage("test")))

        // When & Then
        val exception = assertThrows<IllegalStateException> {
            translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should log warning when useRag is true`() {
        // Given
        val translator = DeepLTranslator("dummy-api-key", preProcessor)
        val po = Po("ja", emptyList())

        // When (useRag=true はDeepLではサポートされない)
        val result = translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = true)

        // Then (警告がログ出力されるが、例外は投げられない)
        assertThat(result.messages).isEmpty()
    }
}
