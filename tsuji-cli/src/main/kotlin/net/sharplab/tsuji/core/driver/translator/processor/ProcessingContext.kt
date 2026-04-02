package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.Po

/**
 * 処理に必要なコンテキスト情報。
 */
data class ProcessingContext(
    val po: Po,              // Po全体（メタデータ等の参照用）
    val srcLang: String,     // ソース言語
    val dstLang: String,     // ターゲット言語
    val isAsciidoctor: Boolean,  // Asciidoctorフォーマットか
    val useRag: Boolean      // RAG使用有無
)
