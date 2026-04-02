package net.sharplab.tsuji.core.processor

import net.sharplab.tsuji.core.model.po.PoMessage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * HTMLの装飾タグ（em、strong等）をAsciidoctor構文に変換する。
 */
class DecorationTagMessageProcessor(
    private val tagName: String,
    private val prefix: String,
    private val suffix: String
) : MessageProcessor {

    override fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage> {
        if (!context.isAsciidoctor) {
            return messages
        }

        return messages.map { message ->
            if (message.messageString.isEmpty()) {
                return@map message
            }

            val doc = Jsoup.parseBodyFragment(message.messageString)
            val body = doc.body()
            replaceDecoration(body)
            val processed = body.html()

            message.copy(messageString = processed)
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
