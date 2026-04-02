package net.sharplab.tsuji.core.processor

import net.sharplab.tsuji.core.model.po.PoMessage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * HTMLの画像タグをAsciidoctor構文に変換する。
 */
class ImageTagMessageProcessor : MessageProcessor {

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
            replaceImage(body)
            val processed = body.html()

            message.copy(messageString = processed)
        }
    }

    private fun replaceImage(element: Element) {
        if (element.tagName() == "span" && element.classNames().contains("image")) {
            val imgTag = element.selectFirst("img")!!
            val src = imgTag.attr("src")
            val attrs = imgTag.attributes().filterNot { attr -> attr.key == "src" }
            val attrsText = attrs.joinToString(separator = ", ")
            var imageText = "image:%s[%s]".format(src, attrsText)

            val prev: Node? = element.previousSibling()
            val next: Node? = element.nextSibling()
            val isPrevExists = prev != null
            val isNextExists = next != null
            val isPrevSpaced = prev is TextNode && prev.text().endsWith(" ")
            val isNextSpaced = next is TextNode && next.text().startsWith(" ")

            if (isPrevExists && !isPrevSpaced) {
                imageText = " $imageText"
            }
            if (isNextExists && !isNextSpaced) {
                imageText = "$imageText "
            }
            element.replaceWith(TextNode(imageText))
        }
        element.children().forEach(this::replaceImage)
    }
}
