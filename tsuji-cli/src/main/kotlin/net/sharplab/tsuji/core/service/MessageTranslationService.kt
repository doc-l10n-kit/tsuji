package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.PoMessage

/**
 * Service for message-level translation logic.
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
}
