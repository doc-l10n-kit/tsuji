package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * MessageProcessor パイプライン統合テスト。
 * 複数のプロセッサーを組み合わせた処理をテストする。
 */
internal class MessageProcessorPipelineIntegrationTest {

    private fun createContext(isAsciidoctor: Boolean = true) = ProcessingContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    private fun createMessage(messageString: String) = PoMessage(
        type = MessageType.PlainText,
        messageId = "",
        messageString = messageString,
        sourceReferences = emptyList()
    )

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

        assertThat(result[0].messageString).isEqualTo("`https://example.com`")
    }
}
