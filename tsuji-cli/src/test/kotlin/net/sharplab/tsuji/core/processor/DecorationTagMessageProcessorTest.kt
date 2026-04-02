package net.sharplab.tsuji.core.processor

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DecorationTagMessageProcessorTest {

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
    fun `should convert em tag to underscores`() {
        val processor = DecorationTagMessageProcessor("em", "_", "_")
        val message = createMessage("これは、<em>強調された</em>文字列です。")
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).isEqualTo("これは、 _強調された_ 文字列です。")
    }

    @Test
    fun `should convert strong tag to asterisks`() {
        val processor = DecorationTagMessageProcessor("strong", "*", "*")
        val message = createMessage("This is <strong>strong</strong> text.")
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).isEqualTo("This is *strong* text.")
    }

    @Test
    fun `should convert code tag to backticks and then unescape`() {
        val codeProcessor = DecorationTagMessageProcessor("code", "`", "`")
        val unescaper = CharacterReferenceUnescaper()
        val message = createMessage("previous word<code>(&gt;_&lt;)</code>next word")
        val context = createContext()

        // パイプライン実行: DecorationTagProcessor → CharacterReferenceUnescaper
        val afterCode = codeProcessor.process(listOf(message), context)
        val result = unescaper.process(afterCode, context)

        assertThat(result[0].messageString).isEqualTo("previous word `(>_<)` next word")
    }

    @Test
    fun `should skip when isAsciidoctor is false`() {
        val processor = DecorationTagMessageProcessor("em", "_", "_")
        val message = createMessage("<em>test</em>")
        val context = createContext(isAsciidoctor = false)

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).isEqualTo("<em>test</em>")
    }
}
