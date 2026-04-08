package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * Converts HTML decoration tags (em, strong, etc.) to Asciidoctor syntax.
 */
class DecorationTagMessageProcessor(
    private val tagName: String,
    private val prefix: String,
    private val suffix: String
) : MessageProcessor {

    override suspend fun process(messages: List<TranslationMessage>, context: TranslationContext): List<TranslationMessage> {
        if (!context.isAsciidoctor) {
            return messages
        }

        return messages.map { message ->
            if (!message.needsTranslation) {
                message
            } else {
                val doc = Jsoup.parseBodyFragment(message.text)
                val body = doc.body()
                replaceDecoration(body)
                val processed = body.html()

                message.withText(processed)
            }
        }
    }

    private fun replaceDecoration(element: Element) {
        if (element.tagName() == tagName) {
            val prev = element.previousSibling()
            val next = element.nextSibling()
            val isPrevNodeEndsWithSpace = prev is TextNode && prev.text().endsWith(" ")
            val isNextNodeEndsWithSpace = next is TextNode && next.text().startsWith(" ")
            val spacingIsRequiredForPrev = prev != null && !isPrevNodeEndsWithSpace
            val spacingIsRequiredForNext = next != null && !isNextNodeEndsWithSpace

            val openStr = when {
                spacingIsRequiredForPrev -> " $prefix"
                else -> prefix
            }
            val closeStr = when {
                spacingIsRequiredForNext -> "$suffix "
                else -> suffix
            }
            element.replaceWith(TextNode(openStr + element.wholeText().trim() + closeStr))
        }
        element.children().forEach {
            replaceDecoration(it)
        }
    }
}
