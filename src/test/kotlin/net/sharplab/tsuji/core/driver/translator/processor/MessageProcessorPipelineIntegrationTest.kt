package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * MessageProcessor パイプライン統合テスト。
 * 複数のプロセッサーを組み合わせた処理をテストする。
 */
internal class MessageProcessorPipelineIntegrationTest {

    private fun createContext(isAsciidoctor: Boolean = true) = TranslationContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    private fun createMessage(text: String): TranslationMessage {
        val poMessage = PoMessage(
            
            messageId = "test",
            messageString = "",
            sourceReferences = emptyList()
        )
        return TranslationMessage(
            original = poMessage,
            text = text,
            needsTranslation = true
        )
    }

    @Test
    fun `should handle combined tags through pipeline`() {
        val message = createMessage(
            "<code> <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://example.com\">https://example.com</a></code>"
        )
        val context = createContext()

        // パイプライン実行
        val processors = listOf(
            LinkTagMessageProcessor(),
            DecorationTagMessageProcessor("code", "`", "`"),
            CharacterReferenceUnescaper()
        )

        val result = processors.fold(listOf(message)) { msgs, processor ->
            processor.process(msgs, context)
        }

        assertThat(result[0].text).isEqualTo("`https://example.com`")
    }
}
