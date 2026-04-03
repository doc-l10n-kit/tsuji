package net.sharplab.tsuji.core.model.po

/**
 * Typed key for session data in PoMessage.
 * Session data is temporary processing data that does not persist to PO files.
 */
data class SessionKey<T>(val name: String) {
    companion object {
        /**
         * Preprocessed text (HTML) for Asciidoctor translation.
         */
        val PREPROCESSED_TEXT = SessionKey<String>("preprocessedText")

        /**
         * Flag indicating whether this message needs translation in the current pipeline.
         */
        val NEEDS_TRANSLATION = SessionKey<Boolean>("needsTranslation")
    }
}
