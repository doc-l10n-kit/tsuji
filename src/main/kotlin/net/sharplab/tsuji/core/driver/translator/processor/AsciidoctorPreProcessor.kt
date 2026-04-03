package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.jsoup.Jsoup
import java.nio.file.Files

/**
 * Preprocessing that converts Asciidoctor format to HTML.
 * Reads TranslationMessage.text (Asciidoctor format) and writes back HTML.
 */
class AsciidoctorPreProcessor(
    private val asciidoctor: Asciidoctor
) : MessageProcessor, AutoCloseable {

    private val tempDir = Files.createTempDirectory("asciidoc-templates-")

    init {
        val inputStream = this.javaClass.classLoader.getResourceAsStream("asciidoc-templates/inline_anchor.html.erb")
        requireNotNull(inputStream) { "Asciidoc template resource not found: asciidoc-templates/inline_anchor.html.erb" }
        val inlineAnchorTemplateFile = tempDir.toFile().resolve("inline_anchor.html.erb")
        Files.copy(inputStream, inlineAnchorTemplateFile.toPath())
    }

    override fun process(messages: List<TranslationMessage>, context: TranslationContext): List<TranslationMessage> {
        // Skip if not Asciidoctor
        if (!context.isAsciidoctor) {
            return messages
        }

        // Process each message
        return messages.map { message ->
            if (!message.needsTranslation) {
                message
            } else {
                // Convert Asciidoctor to HTML
                val options = Options.builder()
                    .templateDirs(tempDir.toFile())
                    .build()
                val document = asciidoctor.load(message.text, options)
                document.attributes["relfilesuffix"] = ".adoc"
                val html = document.convert()
                val doc = Jsoup.parseBodyFragment(html)
                val processedHtml = when (val first = doc.body().children().first()) {
                    null -> message.text
                    else -> first.children().html()
                }

                // Update working text with HTML
                message.withText(processedHtml)
            }
        }
    }

    override fun close() {
        tempDir.toFile().deleteRecursively()
    }
}
