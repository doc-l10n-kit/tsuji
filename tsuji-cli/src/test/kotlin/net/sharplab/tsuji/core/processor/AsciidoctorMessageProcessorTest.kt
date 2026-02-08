package net.sharplab.tsuji.core.processor

import org.asciidoctor.Asciidoctor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class AsciidoctorMessageProcessorTest {

    private val asciidoctor = Asciidoctor.Factory.create()
    private val target = AsciidoctorMessageProcessor(asciidoctor)

    @Test
    fun preProcess_decorationTag_test(){
        val messageString = target.preProcess("This is an _emphasized_ string.")
        assertThat(messageString).isEqualTo("This is an <em>emphasized</em> string.")
    }

    @Test
    fun postProcess_decorationTag_test(){
        val messageString = target.postProcess("これは、<em>強調された</em>文字列です。")
        assertThat(messageString).isEqualTo("これは、 _強調された_ 文字列です。")
    }

    @Test
    fun preProcess_link_test(){
        val messageString = target.preProcess("This is a link:https://github.com/webauthn4j[link], to webauthn4j GitHub org.")
        assertThat(messageString).isEqualTo("This is a <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://github.com/webauthn4j\">link</a>, to webauthn4j GitHub org.")
    }

    @Test
    fun postProcess_link_test(){
        val messageString = target.postProcess("これは、webauthn4j GitHub組織への<a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://github.com/webauthn4j\">リンク</a>です。")
        assertThat(messageString).isEqualTo("これは、webauthn4j GitHub組織への link:https://github.com/webauthn4j[リンク] です。")
    }


    @Test
    fun preProcess_linkTag_test2(){
        val result = target.preProcess("You may wonder about Reactive Streams (https://www.reactive-streams.org/).")
        assertThat(result).isEqualTo("You may wonder about Reactive Streams ( <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://www.reactive-streams.org/\">https://www.reactive-streams.org/</a>).")
    }

    @Test
    fun postProcess_linkTag_test2(){
        val result = target.postProcess("You may wonder about Reactive Streams ( <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://www.reactive-streams.org/\">https://www.reactive-streams.org/</a>).")
        assertThat(result).isEqualTo("You may wonder about Reactive Streams ( https://www.reactive-streams.org/ ).")
    }

    @Test
    fun postProcess_linkTag_test3(){
        val result = target.postProcess("<a data-doc-l10n-kit-target=\"#bootstrapping-the-project\">Bootstrappingプロジェクト</a>以降の手順に沿って、ステップバイステップでアプリを作成していくことをお勧めします。")
        assertThat(result).isEqualTo("link:#bootstrapping-the-project[Bootstrappingプロジェクト] 以降の手順に沿って、ステップバイステップでアプリを作成していくことをお勧めします。")
    }


    @Test
    fun preProcess_xref_test(){
        val result = target.preProcess("Follow guidance in xref:titles-headings[Titles and headings]")
        assertThat(result).isEqualTo("Follow guidance in <a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#titles-headings\">Titles and headings</a>")
    }

    @Test
    fun postProcess_xref_test(){
        val result = target.postProcess("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#titles-headings\">タイトルと見出し</a>のガイダンスに従って下さい。")
        assertThat(result).isEqualTo("xref:titles-headings[タイトルと見出し] のガイダンスに従って下さい。")
    }

    @Test
    fun preProcess_xref_with_extension_test(){
        val result = target.preProcess("xref:test.adoc[Test doc]")
        assertThat(result).isEqualTo("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc\">Test doc</a>")
    }

    @Test
    fun postProcess_xref_with_extension_test(){
        val result = target.postProcess("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc\">Test doc</a>")
        assertThat(result).isEqualTo("xref:test.adoc[Test doc]")
    }

    @Test
    fun preProcess_xref_with_extension2_test(){
        val result = target.preProcess("xref:test.adoc#section[Test doc]")
        assertThat(result).isEqualTo("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc#section\">Test doc</a>")
    }

    @Test
    fun postProcess_xref_with_extension2_test(){
        val result = target.postProcess("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"test.adoc#section\">Test doc</a>")
        assertThat(result).isEqualTo("xref:test.adoc#section[Test doc]")
    }

    @Test
    fun preProcess_xref_shorthand_test(){
        val result = target.preProcess("<<test-url,test-text>>")
        assertThat(result).isEqualTo("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\">test-text</a>")
    }

    @Test
    fun postProcess_xref_shorthand_test(){
        val result = target.postProcess("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\">test-text</a>")
        assertThat(result).isEqualTo("xref:test-url[test-text]")
    }

    @Test
    fun preProcess_xref_shorthand_without_text_test(){
        val result = target.preProcess("<<test-url>>")
        assertThat(result).isEqualTo("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\"></a>")
    }

    @Test
    fun postProcess_xref_shorthand_without_text_test(){
        val result = target.postProcess("<a data-doc-l10n-kit-type=\"xref\" data-doc-l10n-kit-target=\"#test-url\"></a>")
        assertThat(result).isEqualTo("<<test-url>>")
    }

    @Test
    fun preProcess_imageTag(){
        val result = target.preProcess("image:quarkus-reactive-stack.png[alt=Quarkus is based on a reactive engine, 50%]")
        assertThat(result).isEqualTo("<span class=\"image\"><img src=\"quarkus-reactive-stack.png\" alt=\"Quarkus is based on a reactive engine\" width=\"50%\"></span>")
    }

    @Test
    fun postProcess_imageTag(){
        val result = target.postProcess("<span class=\"image\"><img src=\"quarkus-reactive-stack.png\" alt=\"Quarkus is based on a reactive engine\" width=\"50%\"></span>")
        assertThat(result).isEqualTo("image:quarkus-reactive-stack.png[alt=\"Quarkus is based on a reactive engine\", width=\"50%\"]")
    }

    @Test
    fun preProcess_combinedTags(){
        val result = target.preProcess("`https://example.com`")
        assertThat(result).isEqualTo("<code> <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://example.com\">https://example.com</a></code>")
    }

    @Test
    fun postProcess_combinedTags(){
        val result = target.postProcess("<code> <a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://example.com\">https://example.com</a></code>")
        assertThat(result).isEqualTo("`https://example.com`")
    }

    @Test
    fun preProcess(){
        val result = target.preProcess("(>_<)")
        assertThat(result).isEqualTo("(&gt;_&lt;)")
    }

    @Test
    fun postProcess(){
        val result = target.postProcess("previous word<code>(&gt;_&lt;)</code>next word")
        assertThat(result).isEqualTo("previous word `(>_<)` next word")
    }
}
