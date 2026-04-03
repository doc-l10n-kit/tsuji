package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CharacterReferenceUnescaperTest {

    private val processor = CharacterReferenceUnescaper()

    private fun createContext(isAsciidoctor: Boolean = true) = ProcessingContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    private fun createMessage(messageString: String) = PoMessage(
        type = MessageType.PlainText,
        messageId = "test",
        messageString = messageString,
        sourceReferences = emptyList()
    )

    @Test
    fun `should unescape HTML entities`() {
        val message = createMessage("previous word `(&gt;_&lt;)` next word")
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).isEqualTo("previous word `(>_<)` next word")
    }

    @Test
    fun `should unescape all common entities`() {
        val message = createMessage("&lt; &gt; &amp; &quot; &apos;")
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).isEqualTo("< > & \" '")
    }

    @Test
    fun `should skip when isAsciidoctor is false`() {
        val message = createMessage("&lt;test&gt;")
        val context = createContext(isAsciidoctor = false)

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).isEqualTo("&lt;test&gt;")
    }
}
