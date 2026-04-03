package net.sharplab.tsuji.po.util

import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.po.model.type

/**
 * Utility for classifying PO messages.
 */
object MessageClassifier {

    /**
     * Determines if a message should be filled with its messageId (no translation).
     * These are typically technical identifiers or paths that should remain unchanged.
     */
    fun shouldFillWithMessageId(msg: PoMessage): Boolean {
        return when (msg.type.value) {
            "type: delimited block -",
            "type: Target for macro image",
            "type: Hash Value: types guide categories",
            "type: Hash Value: types reference categories",
            "type: Hash Value: types tutorial categories",
            "type: Hash Value: categories cat-id",
            "type: Hash Value: types concepts id",
            "type: Hash Value: types concepts type",
            "type: Hash Value: types concepts filename",
            "type: Hash Value: types concepts url",
            "type: Hash Value: types guide id",
            "type: Hash Value: types guide type",
            "type: Hash Value: types guide filename",
            "type: Hash Value: types guide url",
            "type: Hash Value: types reference id",
            "type: Hash Value: types reference type",
            "type: Hash Value: types reference filename",
            "type: Hash Value: types reference url",
            "type: Hash Value: types howto id",
            "type: Hash Value: types howto type",
            "type: Hash Value: types howto filename",
            "type: Hash Value: types howto url",
            "type: Hash Value: types tutorial id",
            "type: Hash Value: types tutorial type",
            "type: Hash Value: types tutorial filename",
            "type: Hash Value: types tutorial url",
            "type: Hash Value: links avatar",
            "type: Hash Value: links login",
            "type: Hash Value: links name",
            "type: Hash Value: links url",
            "type: Hash Value: categories guides url" -> true
            else -> false
        }
    }

    /**
     * Detects if the given text is a Jekyll Front Matter format.
     * Jekyll Front Matter contains YAML metadata at the beginning of the file.
     */
    fun isJekyllFrontMatter(text: String): Boolean {
        val lines = text.lines()
        return lines.any { it.startsWith("layout:") } &&
               lines.any { it.startsWith("title:") } &&
               lines.any { it.startsWith("date:") } &&
               lines.any { it.startsWith("tags:") } &&
               lines.any { it.startsWith("author:") }
    }
}
