package net.sharplab.tsuji.core.processor

import net.sharplab.tsuji.core.model.po.PoMessage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * HTMLのリンクタグをAsciidoctor構文に変換する。
 */
class LinkTagMessageProcessor : MessageProcessor {

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
            replaceLink(body)
            val processed = body.html()

            message.copy(messageString = processed)
        }
    }

    private fun replaceLink(element: Element) {
        if (element.tagName() == "a") {
            if (element.attr("class") == "bare") {
                val url = element.attr("href")
                val linkText = " %s ".format(url)
                element.replaceWith(TextNode(linkText))
            }
            val type = element.attr("data-tsuji-type").ifEmpty { element.attr("data-doc-l10n-kit-type") }
            val targetAttr = element.attr("data-tsuji-target").ifEmpty { element.attr("data-doc-l10n-kit-target") }

            when (type) {
                "xref" -> {
                    val target = when (targetAttr.startsWith("#")) {
                        true -> targetAttr.substring(1)
                        else -> targetAttr
                    }
                    val text = element.text()
                    val attrs = element.attributes()
                        .filterNot { attr -> attr.key == "data-tsuji-type" || attr.key == "data-doc-l10n-kit-type" }
                        .filterNot { attr -> attr.key == "data-tsuji-target" || attr.key == "data-doc-l10n-kit-target" }
                        .filterNot { attr -> attr.key == "rel" && attr.value == "noopener" }
                    val attrsText: String = attrs.joinToString(separator = ", ")
                    var linkText = when (text.isNullOrEmpty()) {
                        true -> "<<%s>>".format(target)
                        false -> when (attrs.isEmpty()) {
                            true -> "xref:%s[%s]".format(target, text)
                            false -> "xref:%s[%s, %s]".format(target, text, attrsText)
                        }
                    }

                    val prev: Node? = element.previousSibling()
                    val next: Node? = element.nextSibling()
                    val isPrevExists = prev != null
                    val isNextExists = next != null
                    val isPrevSpaced = prev is TextNode && prev.text().endsWith(" ")
                    val isNextSpaced = next is TextNode && next.text().startsWith(" ")

                    if (isPrevExists && !isPrevSpaced) {
                        linkText = " $linkText"
                    }
                    if (isNextExists && !isNextSpaced) {
                        linkText = "$linkText "
                    }
                    element.replaceWith(TextNode(linkText))
                }
                else -> {
                    val target = targetAttr
                    val text = element.text()
                    val attrs = element.attributes()
                        .filterNot { attr -> attr.key == "data-tsuji-type" || attr.key == "data-doc-l10n-kit-type" }
                        .filterNot { attr -> attr.key == "data-tsuji-target" || attr.key == "data-doc-l10n-kit-target" }
                        .filterNot { attr -> attr.key == "rel" && attr.value == "noopener" }
                    val attrsText: String = attrs.joinToString(separator = ", ")
                    var linkText = when {
                        target == text -> target
                        attrs.isEmpty() -> "link:%s[%s]".format(target, text)
                        else -> "link:%s[%s, %s]".format(target, text, attrsText)
                    }

                    val prev: Node? = element.previousSibling()
                    val next: Node? = element.nextSibling()
                    val isPrevExists = prev != null
                    val isNextExists = next != null
                    val isPrevSpaced = prev is TextNode && prev.text().endsWith(" ")
                    val isNextSpaced = next is TextNode && next.text().startsWith(" ")

                    if (isPrevExists && !isPrevSpaced) {
                        linkText = " $linkText"
                    }
                    if (isNextExists && !isNextSpaced) {
                        linkText = "$linkText "
                    }
                    element.replaceWith(TextNode(linkText))
                }
            }
        }
        element.children().forEach(this::replaceLink)
    }
}
