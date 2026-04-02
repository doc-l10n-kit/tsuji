package net.sharplab.tsuji.core.processor

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PostProcessorsTest {

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

    /**
     * 単独MessageProcessorテストのヘルパー：
     * 最終的なテキストを抽出するため、wholeText()を呼び出す。
     */
    private fun extractText(html: String): String {
        val doc = org.jsoup.Jsoup.parseBodyFragment(html)
        return doc.body().wholeText()
    }

    @Nested
    inner class LinkTagMessageProcessorTest {

        private val processor = LinkTagMessageProcessor()

        @Test
        fun `should convert link HTML to Asciidoc`() {
            val message = createMessage(
                "これは、webauthn4j GitHub組織への<a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://github.com/webauthn4j\">リンク</a>です。"
            )
            val context = createContext()

            val result = processor.process(listOf(message), context)

            assertThat(result[0].messageString).isEqualTo(
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

            assertThat(result[0].messageString).isEqualTo(
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

            assertThat(result[0].messageString).isEqualTo(
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

            assertThat(result[0].messageString).isEqualTo(
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

            assertThat(result[0].messageString).isEqualTo("xref:test.adoc[Test doc]")
        }

        @Test
        fun `should convert xref with extension and section`() {
            val message = createMessage(
                "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc#section\">Test doc</a>"
            )
            val context = createContext()

            val result = processor.process(listOf(message), context)

            assertThat(result[0].messageString).isEqualTo("xref:test.adoc#section[Test doc]")
        }

        @Test
        fun `should convert xref shorthand without text to angle brackets`() {
            val message = createMessage(
                "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\"></a>"
            )
            val context = createContext()

            val result = processor.process(listOf(message), context)

            // MessageProcessorはhtml()を返すので、wholeText()でテキストを抽出
            assertThat(extractText(result[0].messageString)).isEqualTo("<<test-url>>")
        }

        @Test
        fun `should convert xref shorthand with text`() {
            val message = createMessage(
                "<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\">test-text</a>"
            )
            val context = createContext()

            val result = processor.process(listOf(message), context)

            assertThat(result[0].messageString).isEqualTo("xref:test-url[test-text]")
        }

        @Test
        fun `should skip when isAsciidoctor is false`() {
            val message = createMessage("<a data-doc-l10n-kit-type=\"link\">test</a>")
            val context = createContext(isAsciidoctor = false)

            val result = processor.process(listOf(message), context)

            assertThat(result[0].messageString).isEqualTo("<a data-doc-l10n-kit-type=\"link\">test</a>")
        }
    }

    @Nested
    inner class ImageTagMessageProcessorTest {

        private val processor = ImageTagMessageProcessor()

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

    @Nested
    inner class DecorationTagMessageProcessorTest {

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

    @Nested
    inner class CharacterReferenceUnescaperTest {

        private val processor = CharacterReferenceUnescaper()

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

    @Nested
    inner class PipelineIntegrationTest {

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

            // 元の期待値: `https://example.com`
            // 実際の結果を確認
            val actualResult = result[0].messageString

            // デバッグ用: 実際の結果の文字コードも表示
            val debugInfo = actualResult.map { "[$it:${it.code}]" }.joinToString("")

            // wholeText() を使っているので、スペースがトリムされるはず
            // しかし element.html() を使っているDecorationTagMessageProcessor では
            // 内部のテキストがそのまま保持される
            assertThat(actualResult)
                .withFailMessage(
                    "Expected: [`https://example.com`], but was: [$actualResult]\n" +
                    "Debug info: $debugInfo"
                )
                .isEqualTo("`https://example.com`")
        }
    }
}
