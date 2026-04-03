package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.jsoup.Jsoup

/**
 * Unescapes HTML character references (&amp;, &lt;, etc.) and
 * finally removes HTML tags to extract text only.
 */
class CharacterReferenceUnescaper : MessageProcessor {

    override fun process(messages: List<TranslationMessage>, context: TranslationContext): List<TranslationMessage> {
        if (!context.isAsciidoctor) {
            return messages
        }

        return messages.map { message ->
            if (!message.needsTranslation) {
                message
            } else {
                // First remove HTML tags and extract text only
                val doc = Jsoup.parseBodyFragment(message.text)
                val textOnly = doc.body().wholeText()

                // Then unescape character references
                val unescaped = textOnly
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")

                message.withText(unescaped)
            }
        }
    }
}
