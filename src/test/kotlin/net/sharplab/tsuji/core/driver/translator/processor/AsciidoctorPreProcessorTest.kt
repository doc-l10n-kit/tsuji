package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.asciidoctor.Asciidoctor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class AsciidoctorPreProcessorTest {

    private val asciidoctor = Asciidoctor.Factory.create()
    private val target = AsciidoctorPreProcessor(asciidoctor)

    @AfterEach
    fun tearDown() {
        target.close()
    }

    private fun createContext(isAsciidoctor: Boolean = true) = TranslationContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    private fun createMessage(messageId: String): TranslationMessage {
        val poMessage = PoMessage(
            
            messageId = messageId,
            messageString = "",
            sourceReferences = emptyList()
        )
        return TranslationMessage.from(poMessage)
    }

    @Test
    fun `process should convert decoration tag to HTML when isAsciidoctor is true`() {
        val message = createMessage("This is an _emphasized_ string.")
        val context = createContext(isAsciidoctor = true)

        val result = target.process(listOf(message), context)

        assertThat(result).hasSize(1)
        assertThat(result[0].original.messageId).isEqualTo("This is an _emphasized_ string.")
        assertThat(result[0].text).isEqualTo("This is an <em>emphasized</em> string.")
    }

    @Test
    fun `process should skip conversion when isAsciidoctor is false`() {
        val message = createMessage("This is an _emphasized_ string.")
        val context = createContext(isAsciidoctor = false)

        val result = target.process(listOf(message), context)

        assertThat(result).hasSize(1)
        assertThat(result[0].original.messageId).isEqualTo("This is an _emphasized_ string.")
        assertThat(result[0].text).isEqualTo("This is an _emphasized_ string.")
    }

    @Test
    fun `process should convert link to HTML`() {
        val message = createMessage("This is a link:https://github.com/webauthn4j[link], to webauthn4j GitHub org.")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("This is a link:https://github.com/webauthn4j[link], to webauthn4j GitHub org.")
        assertThat(result[0].text).isEqualTo(
            "This is a <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://github.com/webauthn4j\">link</a>, to webauthn4j GitHub org."
        )
    }

    @Test
    fun `process should convert bare URL to link`() {
        val message = createMessage("You may wonder about Reactive Streams (https://www.reactive-streams.org/).")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("You may wonder about Reactive Streams (https://www.reactive-streams.org/).")
        assertThat(result[0].text).isEqualTo(
            "You may wonder about Reactive Streams ( <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://www.reactive-streams.org/\">https://www.reactive-streams.org/</a>)."
        )
    }

    @Test
    fun `process should convert xref to HTML`() {
        val message = createMessage("Follow guidance in xref:titles-headings[Titles and headings]")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("Follow guidance in xref:titles-headings[Titles and headings]")
        assertThat(result[0].text).isEqualTo(
            "Follow guidance in <a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#titles-headings\">Titles and headings</a>"
        )
    }

    @Test
    fun `process should convert xref with extension`() {
        val message = createMessage("xref:test.adoc[Test doc]")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("xref:test.adoc[Test doc]")
        assertThat(result[0].text).isEqualTo(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc\">Test doc</a>"
        )
    }

    @Test
    fun `process should convert xref with extension and section`() {
        val message = createMessage("xref:test.adoc#section[Test doc]")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("xref:test.adoc#section[Test doc]")
        assertThat(result[0].text).isEqualTo(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc#section\">Test doc</a>"
        )
    }

    @Test
    fun `process should convert xref shorthand`() {
        val message = createMessage("<<test-url,test-text>>")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("<<test-url,test-text>>")
        assertThat(result[0].text).isEqualTo(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\">test-text</a>"
        )
    }

    @Test
    fun `process should convert xref shorthand without text`() {
        val message = createMessage("<<test-url>>")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("<<test-url>>")
        assertThat(result[0].text).isEqualTo(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\"></a>"
        )
    }

    @Test
    fun `process should convert image tag`() {
        val message = createMessage("image:quarkus-reactive-stack.png[alt=Quarkus is based on a reactive engine, 50%]")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("image:quarkus-reactive-stack.png[alt=Quarkus is based on a reactive engine, 50%]")
        assertThat(result[0].text).isEqualTo(
            "<span class=\"image\"><img src=\"quarkus-reactive-stack.png\" alt=\"Quarkus is based on a reactive engine\" width=\"50%\"></span>"
        )
    }

    @Test
    fun `process should convert combined tags`() {
        val message = createMessage("`https://example.com`")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("`https://example.com`")
        assertThat(result[0].text).isEqualTo(
            "<code> <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://example.com\">https://example.com</a></code>"
        )
    }

    @Test
    fun `process should escape special characters`() {
        val message = createMessage("(>_<)")
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result[0].original.messageId).isEqualTo("(>_<)")
        assertThat(result[0].text).isEqualTo("(&gt;_&lt;)")
    }

    @Test
    fun `process should handle multiple messages in batch`() {
        val messages = listOf(
            createMessage("This is _emphasized_."),
            createMessage("This is *strong*."),
            createMessage("This is `code`.")
        )
        val context = createContext()

        val result = target.process(messages, context)

        assertThat(result).hasSize(3)
        assertThat(result[0].original.messageId).isEqualTo("This is _emphasized_.")
        assertThat(result[0].text).contains("<em>emphasized</em>")
        assertThat(result[1].original.messageId).isEqualTo("This is *strong*.")
        assertThat(result[1].text).contains("<strong>strong</strong>")
        assertThat(result[2].original.messageId).isEqualTo("This is `code`.")
        assertThat(result[2].text).contains("<code>code</code>")
    }

    @Test
    fun `process should skip empty messageId`() {
        // Empty messageId should not have NEEDS_TRANSLATION flag
        val poMessage = PoMessage(
            
            messageId = "",
            messageString = "",
            sourceReferences = emptyList()
        )
        val message = TranslationMessage(
            original = poMessage,
            text = "",
            needsTranslation = false
        )
        val context = createContext()

        val result = target.process(listOf(message), context)

        assertThat(result).hasSize(1)
        assertThat(result[0].original.messageId).isEmpty()
        assertThat(result[0].text).isEmpty()
    }
}
