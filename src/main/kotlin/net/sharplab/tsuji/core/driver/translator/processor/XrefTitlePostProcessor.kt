package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.translation.TranslationContext
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.slf4j.LoggerFactory

/**
 * Post-processor that replaces xref titles in translations with their translated equivalents.
 *
 * In Asciidoc, `<<Section Title>>` links to a section by its title. After translation,
 * section titles change but xrefs may still reference the original English title.
 * This processor builds a title mapping from PO comments (`type: Title`) and replaces
 * `<<English Title>>` with `<<翻訳タイトル>>` so that xref links resolve correctly.
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
     * Replaces `<<Original Title>>` with `<<Translated Title>>` in the text.
     * Also handles `<<Original Title,display text>>` by replacing only the anchor part.
     */
    private fun replaceXrefTitles(text: String, titleMap: Map<String, String>): String {
        var result = text
        for ((original, translated) in titleMap) {
            // Replace <<Original Title>> (without display text)
            result = result.replace("<<$original>>", "<<$translated>>")
            // Replace <<Original Title,text>> (with display text) — replace only the anchor part
            result = result.replace("<<$original,", "<<$translated,")
        }
        return result
    }
}
