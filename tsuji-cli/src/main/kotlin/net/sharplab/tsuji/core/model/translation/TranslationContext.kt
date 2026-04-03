package net.sharplab.tsuji.core.model.translation

import net.sharplab.tsuji.core.model.po.Po

/**
 * Context information needed for translation processing.
 */
data class TranslationContext(
    val po: Po,              // Entire Po (for metadata reference, etc.)
    val srcLang: String,     // Source language
    val dstLang: String,     // Target language
    val isAsciidoctor: Boolean,  // Whether it's Asciidoctor format
    val useRag: Boolean      // Whether to use RAG
)
