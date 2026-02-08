package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.PoMessage

/**
 * Service for message-level translation logic, including format detection and individual processing.
 */
interface MessageTranslationService {
    /**
     * Checks if the message requires translation.
     */
    fun shouldTranslate(message: PoMessage): Boolean

    /**
     * Checks if the message should be filled with its ID instead of being translated.
     */
    fun shouldFillWithMessageId(message: PoMessage): Boolean

    /**
     * Checks if the message has a special format (like Jekyll Blog Header).
     */
    fun isSpecialFormat(message: PoMessage): Boolean

    /**
     * Translates a message with a special format.
     */
    fun translateSpecialFormat(
        message: String,
        sourceLang: String,
        targetLang: String,
        useRag: Boolean,
        translateAction: (List<String>, String, String, Boolean) -> List<String>
    ): String
}
