package net.sharplab.tsuji.core.driver.translator.util

import net.sharplab.tsuji.po.model.MessageType

/**
 * Generates translation notes based on MessageType.
 * Used to provide additional instructions to LLM-based translation engines.
 */
class MessageTypeNoteGenerator(
    private val headingNote: String?
) {
    /**
     * Generates a note for the given MessageType.
     * Returns the configured heading note for heading types (Title1/2/3) and BlockTitle.
     *
     * @param type The MessageType to generate a note for
     * @return A note string if the type is a heading/block title and headingNote is configured, null otherwise
     */
    fun generateNote(type: MessageType): String? {
        return when (type) {
            MessageType.Title1, MessageType.Title2, MessageType.Title3, MessageType.BlockTitle -> headingNote
            else -> null
        }
    }

    companion object {
        /**
         * Merges multiple notes into a single note string.
         * Filters out null and blank notes, and joins them with spaces.
         *
         * @param notes Variable number of note strings (can be null)
         * @return A merged note string, or null if all notes are null/blank
         */
        fun mergeNotes(vararg notes: String?): String? {
            return notes.filterNotNull()
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" ")
        }
    }
}
