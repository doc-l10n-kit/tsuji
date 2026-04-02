package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.PoMessage
import org.jsoup.Jsoup

/**
 * HTMLの文字参照（&amp;、&lt;等）をアンエスケープし、
 * 最後にHTMLタグを除去してテキストのみを抽出する。
 */
class CharacterReferenceUnescaper : MessageProcessor {

    override fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage> {
        if (!context.isAsciidoctor) {
            return messages
        }

        return messages.map { message ->
            if (message.messageString.isEmpty()) {
                return@map message
            }

            // 先にHTMLタグを除去してテキストのみを抽出
            val doc = Jsoup.parseBodyFragment(message.messageString)
            val textOnly = doc.body().wholeText()

            // その後、文字参照をアンエスケープ
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
