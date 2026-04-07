package net.sharplab.tsuji.core.driver.translator.gemini

/**
 * Request item for array-based batch translation.
 * Serializes to {"index": 0, "text": "text content"}.
 */
data class BatchTranslationRequestItem(
    val index: Int,
    val text: String
)
