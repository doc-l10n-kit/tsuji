package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.Po

/**
 * Context information needed for processing.
 */
data class ProcessingContext(
    val po: Po,              // Entire Po (for metadata reference, etc.)
    val srcLang: String,     // Source language
    val dstLang: String,     // Target language
    val isAsciidoctor: Boolean,  // Whether it's Asciidoctor format
    val useRag: Boolean      // Whether to use RAG
)
