package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.model.po.SessionKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ImageTagMessageProcessorTest {

    private val processor = ImageTagMessageProcessor()

    private fun createContext(isAsciidoctor: Boolean = true) = ProcessingContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    private fun createMessage(messageString: String): PoMessage {
        return PoMessage(
            type = MessageType.PlainText,
            messageId = "test",
            messageString = messageString,
            sourceReferences = emptyList()
        )
            .setSession(SessionKey.NEEDS_TRANSLATION, true)
            .setSession(SessionKey.PREPROCESSED_TEXT, "preprocessed")
    }

    @Test
    fun `should convert image HTML to Asciidoc`() {
        val message = createMessage(
            "<span class=\"image\"><img src=\"quarkus-reactive-stack.png\" alt=\"Quarkus is based on a reactive engine\" width=\"50%\"></span>"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).isEqualTo(
            "image:quarkus-reactive-stack.png[alt=\"Quarkus is based on a reactive engine\", width=\"50%\"]"
        )
    }

    @Test
    fun `should skip when isAsciidoctor is false`() {
        val message = createMessage("<span class=\"image\"><img src=\"test.png\"></span>")
        val context = createContext(isAsciidoctor = false)

        val result = processor.process(listOf(message), context)

        assertThat(result[0].messageString).contains("<span class=\"image\">")
    }
}
