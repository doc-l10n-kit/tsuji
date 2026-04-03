package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LinkTagMessageProcessorTest {

    private val processor = LinkTagMessageProcessor()

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

    private fun extractText(html: String): String {
        val doc = org.jsoup.Jsoup.parseBodyFragment(html)
        return doc.body().wholeText()
    }

    @Test
    fun `should convert link HTML to Asciidoc`() {
        val message = createMessage(
            "これは、webauthn4j GitHub組織への<a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://github.com/webauthn4j\">リンク</a>です。"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo(
            "これは、webauthn4j GitHub組織への link:https://github.com/webauthn4j[リンク] です。"
        )
    }

    @Test
    fun `should convert bare link to plain URL`() {
        val message = createMessage(
            "You may wonder about Reactive Streams ( <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://www.reactive-streams.org/\">https://www.reactive-streams.org/</a>)."
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo(
            "You may wonder about Reactive Streams ( https://www.reactive-streams.org/ )."
        )
    }

    @Test
    fun `should convert link without type to link with hash`() {
        val message = createMessage(
            "<a data-doc-l10n-kit-target=\"#bootstrapping-the-project\">Bootstrappingプロジェクト</a>以降の手順に沿って、ステップバイステップでアプリを作成していくことをお勧めします。"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo(
            "link:#bootstrapping-the-project[Bootstrappingプロジェクト] 以降の手順に沿って、ステップバイステップでアプリを作成していくことをお勧めします。"
        )
    }

    @Test
    fun `should convert xref HTML to Asciidoc`() {
        val message = createMessage(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#titles-headings\">タイトルと見出し</a>のガイダンスに従って下さい。"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo(
            "xref:titles-headings[タイトルと見出し] のガイダンスに従って下さい。"
        )
    }

    @Test
    fun `should convert xref with extension`() {
        val message = createMessage(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc\">Test doc</a>"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo("xref:test.adoc[Test doc]")
    }

    @Test
    fun `should convert xref with extension and section`() {
        val message = createMessage(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc#section\">Test doc</a>"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo("xref:test.adoc#section[Test doc]")
    }

    @Test
    fun `should convert xref shorthand without text to angle brackets`() {
        val message = createMessage(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\"></a>"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        // MessageProcessorはhtml()を返すので、wholeText()でテキストを抽出
        assertThat(extractText(result[0].text)).isEqualTo("<<test-url>>")
    }

    @Test
    fun `should convert xref shorthand with text`() {
        val message = createMessage(
            "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\">test-text</a>"
        )
        val context = createContext()

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo("xref:test-url[test-text]")
    }

    @Test
    fun `should skip when isAsciidoctor is false`() {
        val message = createMessage("<a data-doc-l10n-kit-type=\"link\">test</a>")
        val context = createContext(isAsciidoctor = false)

        val result = processor.process(listOf(message), context)

        assertThat(result[0].text).isEqualTo("<a data-doc-l10n-kit-type=\"link\">test</a>")
    }
}
