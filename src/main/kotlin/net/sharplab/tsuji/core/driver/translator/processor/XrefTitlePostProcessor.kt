package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.translation.TranslationContext
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.slf4j.LoggerFactory

/**
 * Post-processor that adds translated display text to xrefs while keeping the original anchor.
 *
 * Replaces `<<English Title>>` with `<<section-id,翻訳タイトル>>` so that:
 * - The anchor resolves to the English-based section ID (preserved by jekyll-l10n)
 * - The display text shows the translated section title
 *
 * Section IDs are generated following Asciidoctor's rules with idprefix="" and idseparator="-":
 * lowercase, non-word chars removed, spaces/hyphens/periods replaced with "-".
 */
class XrefTitlePostProcessor : MessageProcessor {

    private val logger = LoggerFactory.getLogger(XrefTitlePostProcessor::class.java)

    override suspend fun process(
        messages: List<TranslationMessage>,
        context: TranslationContext
    ): List<TranslationMessage> {
        if (!context.isAsciidoctor) return messages

        val titleMap = buildTitleMap(messages)
        if (titleMap.isEmpty()) return messages

        return messages.map { msg ->
            if (!msg.needsTranslation || msg.text.isEmpty()) return@map msg

            val replaced = replaceXrefTitles(msg.text, titleMap)
            if (replaced != msg.text) msg.withText(replaced) else msg
        }
    }

    /**
     * Builds a map of original title -> translated title from messages with Title comments.
     */
    private fun buildTitleMap(messages: List<TranslationMessage>): Map<String, String> {
        return messages
            .filter { msg ->
                msg.original.comments.any { it.contains("type: Title") }
                    && msg.text.isNotEmpty()
                    && msg.original.messageId != msg.text
            }
            .associate { it.original.messageId to it.text }
    }

    /**
     * Replaces `<<Original Title>>` with `<<section-id,Translated Title>>`.
     */
    private fun replaceXrefTitles(text: String, titleMap: Map<String, String>): String {
        var result = text
        for ((original, translated) in titleMap) {
            val sectionId = toSectionId(original)
            result = result.replace("<<$original>>", "<<$sectionId,$translated>>")
        }
        return result
    }

    /**
     * Converts a section title to an Asciidoctor auto-generated section ID.
     * Follows Asciidoctor rules with idprefix="" and idseparator="-".
     */
    private fun toSectionId(title: String): String {
        return title.lowercase()
            .replace(Regex("[^\\w\\s.-]"), "")
            .replace(Regex("[\\s.-]+"), "-")
            .trim('-')
    }
}
