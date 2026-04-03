package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ImageTagMessageProcessorTest {

    private val processor = ImageTagMessageProcessor()

    private fun createContext(isAsciidoctor: Boolean = true) = TranslationContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    private fun createMessage(text: String): TranslationMessage {
        val poMessage = PoMessage(
            type = MessageType.PlainText,
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
    fun `should convert image HTML to Asciidoc`() {
        val message = createMessage(
            "<span class=\"image\"><img src=\"quarkus-reactive-stack.png\" alt=\"Quarkus is based on a reactive engine\" width=\"50%\"></span>"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo(
            "image:quarkus-reactive-stack.png[alt=\"Quarkus is based on a reactive engine\", width=\"50%\"]"
        )
    }

    @Test
    fun `should skip when isAsciidoctor is false`() {
        val message = createMessage("<span class=\"image\"><img src=\"test.png\"></span>")
        val context = createContext(isAsciidoctor = false)

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).contains("<span class=\"image\">")
    }
}
