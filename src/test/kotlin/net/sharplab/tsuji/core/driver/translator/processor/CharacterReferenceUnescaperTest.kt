package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CharacterReferenceUnescaperTest {

    private val processor = CharacterReferenceUnescaper()

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
    fun `should unescape HTML entities`() {
        val message = createMessage("previous word `(&gt;_&lt;)` next word")
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo("previous word `(>_<)` next word")
    }

    @Test
    fun `should unescape all common entities`() {
        val message = createMessage("&lt; &gt; &amp; &quot; &apos;")
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo("< > & \" '")
    }

    @Test
    fun `should skip when isAsciidoctor is false`() {
        val message = createMessage("&lt;test&gt;")
        val context = createContext(isAsciidoctor = false)

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo("&lt;test&gt;")
    }
}
