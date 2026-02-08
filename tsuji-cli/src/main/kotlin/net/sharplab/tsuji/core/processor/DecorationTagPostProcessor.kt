package net.sharplab.tsuji.core.processor

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

class DecorationTagPostProcessor(private val tagName: String, private val openQuote: String, private val closeQuote: String) : TagPostProcessor{

    override fun postProcess(message: Element) {
        replaceToQuote(message)
    }

    private fun replaceToQuote(element: Element){
        if(element.tagName() == tagName){
            val prev = element.previousSibling()
            val next = element.nextSibling()
            val isPrevNodeEndsWithSpace= prev is TextNode && prev.text().endsWith(" ")
            val isNextNodeEndsWithSpace= next is TextNode && next.text().startsWith(" ")
            val spacingIsRequiredForPrev = prev != null && !isPrevNodeEndsWithSpace
            val spacingIsRequiredForNext = next != null && !isNextNodeEndsWithSpace

            val openStr = when {
                spacingIsRequiredForPrev -> " $openQuote"
                else -> openQuote
            }
            val closeStr = when {
                spacingIsRequiredForNext -> "$closeQuote "
                else -> closeQuote
            }
            element.replaceWith(TextNode(openStr + element.html() + closeStr))
        }
        element.children().forEach{
            replaceToQuote(it)
        }
    }

}