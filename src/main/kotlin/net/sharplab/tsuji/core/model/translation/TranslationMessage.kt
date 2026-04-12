package net.sharplab.tsuji.core.model.translation

import net.sharplab.tsuji.po.model.PoMessage

/**
 * Translation pipeline working model.
 * Holds the message being processed and its mutable state during translation.
 *
 * This is separate from PoMessage to maintain clear separation of concerns:
 * - PoMessage: Domain model for PO files (immutable, persisted)
 * - TranslationMessage: Working model for translation pipeline (mutable state, temporary)
 */
data class TranslationMessage(
    /**
     * Original PO message (immutable context).
     * Processors can read this for reference (e.g., messageId, sourceReferences).
     */
    val original: PoMessage,

    /**
     * Working text being processed through the pipeline.
     * Each processor reads this, processes it, and writes back.
     *
     * Pipeline flow:
     * 1. Initialized with messageId (for messages needing translation)
     * 2. PreProcessor: messageId → HTML/processed format
     * 3. Translator: processed text → translated text
     * 4. PostProcessor: translated text → final format
     */
    val text: String,

    /**
     * Whether this translation should be marked as fuzzy.
     * Processors can update this flag during processing.
     */
    val fuzzy: Boolean = false,

    /**
     * Whether this message needs translation.
     * Set at initialization, checked by all processors to skip already-translated messages.
     */
    val needsTranslation: Boolean = false
) {
    /**
     * Returns true if the working text is empty or blank.
     * Used to skip empty messages that cannot be translated.
     */
    fun isEmpty(): Boolean = text.isBlank()

    /**
     * Creates a copy with updated text.
     */
    fun withText(newText: String): TranslationMessage {
        return copy(text = newText)
    }

    /**
     * Creates a copy with updated fuzzy flag.
     */
    fun withFuzzy(fuzzy: Boolean): TranslationMessage {
        return copy(fuzzy = fuzzy)
    }

    /**
     * Creates a copy with an additional comment.
     */
    fun withComment(comment: String): TranslationMessage {
        return copy(original = original.copy(comments = original.comments + comment))
    }

    /**
     * Sets the machine translation engine comment, replacing any existing mt: comment.
     */
    fun withMtEngine(engine: String): TranslationMessage {
        val updatedComments = original.comments.filterNot { it.startsWith("mt:") } + "mt: $engine"
        return copy(original = original.copy(comments = updatedComments))
    }

    /**
     * Converts back to PoMessage with the processed result.
     * If needsTranslation is true, updates messageString and fuzzy flag.
     * Otherwise, returns the original unchanged.
     */
    fun toPoMessage(): PoMessage {
        return if (needsTranslation) {
            original.copy(messageString = text).also { it.fuzzy = fuzzy }
        } else {
            original
        }
    }

    companion object {
        /**
         * Creates TranslationMessage from PoMessage.
         * Determines if translation is needed and initializes working text accordingly.
         */
        fun from(message: PoMessage): TranslationMessage {
            val needsTranslation = !message.isHeader && message.messageString.isEmpty()
            return TranslationMessage(
                original = message,
                text = if (needsTranslation) message.messageId else message.messageString,
                fuzzy = message.fuzzy,
                needsTranslation = needsTranslation
            )
        }
    }
}
