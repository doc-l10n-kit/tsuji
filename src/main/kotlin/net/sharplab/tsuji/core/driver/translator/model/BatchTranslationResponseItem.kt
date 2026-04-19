package net.sharplab.tsuji.core.driver.translator.model

/**
 * Response item for array-based batch translation.
 * Deserializes from {"index": 0, "translation": "translated text"}.
 */
data class BatchTranslationResponseItem(
    val index: Int,
    val translation: String
)
