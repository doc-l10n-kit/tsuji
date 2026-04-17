package net.sharplab.tsuji.core.driver.translator.gemini

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Single entry in translation memory (original-translation pair).
 */
data class TranslationMemoryEntry(
    val original: String,
    val translation: String
)

/**
 * RAG translation request item for JSON serialization.
 * Serializes to {"index": 0, "text": "text content", "tm": [...], "note": "..."} (note is optional).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RAGBatchTranslationRequestItem(
    val index: Int,
    val text: String,
    val tm: List<TranslationMemoryEntry>,
    val note: String? = null
)
