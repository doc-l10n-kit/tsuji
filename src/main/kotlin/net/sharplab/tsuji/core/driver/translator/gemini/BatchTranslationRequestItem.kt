package net.sharplab.tsuji.core.driver.translator.gemini

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Translation request item for JSON serialization.
 * Serializes to {"index": 0, "text": "text content", "note": "..."} (note is optional).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BatchTranslationRequestItem(
    val index: Int,
    val text: String,
    val note: String? = null
)
