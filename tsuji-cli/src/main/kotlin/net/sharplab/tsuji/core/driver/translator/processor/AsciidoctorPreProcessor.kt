package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.model.po.SessionKey
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.jsoup.Jsoup
import java.nio.file.Files

/**
 * Preprocessing that converts Asciidoctor format messageId to HTML.
 * Stores the HTML in session data for translation, keeping messageId unchanged.
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

    override fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage> {
        // Skip if not Asciidoctor
        if (!context.isAsciidoctor) {
            return messages
        }

        // Process each message
        return messages.map { message ->
            // Skip if messageId is empty
            if (message.messageId.isEmpty()) {
                return@map message
            }

            // Convert Asciidoctor to HTML
            val options = Options.builder()
                .templateDirs(tempDir.toFile())
                .build()
            val document = asciidoctor.load(message.messageId, options)
            document.attributes["relfilesuffix"] = ".adoc"
            val html = document.convert()
            val doc = Jsoup.parseBodyFragment(html)
            val processedHtml = when (val first = doc.body().children().first()) {
                null -> message.messageId
                else -> first.children().html()
            }

            // Store preprocessed HTML in session, keeping messageId unchanged
            message.setSession(SessionKey.PREPROCESSED_TEXT, processedHtml)
        }
    }

    override fun close() {
        tempDir.toFile().deleteRecursively()
    }
}
