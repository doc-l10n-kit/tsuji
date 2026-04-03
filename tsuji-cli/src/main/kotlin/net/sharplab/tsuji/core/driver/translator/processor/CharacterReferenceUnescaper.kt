package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.PoMessage
import org.jsoup.Jsoup

/**
 * Unescapes HTML character references (&amp;, &lt;, etc.) and
 * finally removes HTML tags to extract text only.
 */
class CharacterReferenceUnescaper : MessageProcessor {

    override fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage> {
        if (!context.isAsciidoctor) {
            return messages
        }

        return messages.map { message ->
            if (message.isHeader || message.messageString.isEmpty()) {
                return@map message
            }

            // First remove HTML tags and extract text only
            val doc = Jsoup.parseBodyFragment(message.messageString)
            val textOnly = doc.body().wholeText()

            // Then unescape character references
            val unescaped = textOnly
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")

            message.copy(messageString = unescaped)
        }
    }
}
