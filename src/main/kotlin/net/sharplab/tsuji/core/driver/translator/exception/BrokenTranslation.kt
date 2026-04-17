package net.sharplab.tsuji.core.driver.translator.exception

import net.sharplab.tsuji.core.model.translation.TranslationMessage

/**
 * Represents a broken translation with a note on how to fix it.
 */
data class BrokenTranslation(
    val message: TranslationMessage,
    val note: String
)
