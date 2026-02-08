package net.sharplab.tsuji.core.processor

import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.jsoup.Jsoup
import java.nio.file.Files

class AsciidoctorMessageProcessor(private val asciidoctor: Asciidoctor) : AutoCloseable {

    private val tempDir = Files.createTempDirectory("asciidoc-templates-")

    private val postProcessors = listOf(
        LinkTagPostProcessor(),
        ImageTagPostProcessor(),
        DecorationTagPostProcessor("em", "_", "_"),
        DecorationTagPostProcessor("strong", "*", "*"),
        DecorationTagPostProcessor("monospace", "`", "`"),
        DecorationTagPostProcessor("superscript", "^", "^"),
        DecorationTagPostProcessor("subscript", "~", "~"),
        DecorationTagPostProcessor("code", "`", "`")
    )

    init {
        val inputStream = this.javaClass.classLoader.getResourceAsStream("asciidoc-templates/inline_anchor.html.erb")
        requireNotNull(inputStream) { "Asciidoc template resource not found: asciidoc-templates/inline_anchor.html.erb" }
        val inlineAnchorTemplateFile = tempDir.toFile().resolve("inline_anchor.html.erb")
        Files.copy(inputStream, inlineAnchorTemplateFile.toPath())
    }

    fun preProcess(message: String): String {
        val options = Options.builder()
            .templateDirs(tempDir.toFile())
            .build()
        val document = asciidoctor.load(message, options)
        document.attributes["relfilesuffix"] = ".adoc"
        val html = document.convert()
        val doc = Jsoup.parseBodyFragment(html)
        return when (val first = doc.body().children().first()) {
            null -> message
            else -> first.children().html()
        }
    }

    fun postProcess(message: String): String {
        val doc = Jsoup.parseBodyFragment(message)
        val body = doc.body()
        postProcessors.forEach { it.postProcess(body) }
        val wholeText = body.wholeText()
        return unescapeCharacterReference(wholeText)
    }

    override fun close() {
        tempDir.toFile().deleteRecursively()
    }

    private fun unescapeCharacterReference(str: String): String{
        return str
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
    }

}