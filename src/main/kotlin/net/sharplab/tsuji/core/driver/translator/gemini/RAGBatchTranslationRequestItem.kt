package net.sharplab.tsuji.core.driver.translator.gemini

/**
 * Single entry in translation memory (original-translation pair).
 */
data class TranslationMemoryEntry(
    val original: String,
    val translation: String
)

/**
 * Request item for RAG-enabled batch translation.
 * Each item contains the text to translate along with its retrieved translation memory examples.
 * Serializes to {"index": 0, "text": "text content", "tm": [...], "instruction": "..."} (instruction is optional).
 */
data class RAGBatchTranslationRequestItem(
    val index: Int,
    val text: String,
    val tm: List<TranslationMemoryEntry>,
    val instruction: String? = null
)
