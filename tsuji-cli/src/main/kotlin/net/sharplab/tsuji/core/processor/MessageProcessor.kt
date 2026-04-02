package net.sharplab.tsuji.core.processor

import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage

/**
 * PoMessageのリストを変換するプロセッサー。
 * Poをコンテキストとして参照しながら、PoMessageのバッチを変換する。
 *
 * バッチ単位で処理することで、個別処理とバッチ処理（翻訳API等）を
 * 統一的に扱える。
 */
interface MessageProcessor {
    /**
     * メッセージのリストを処理する。
     *
     * @param messages 処理対象のメッセージリスト
     * @param context 処理に必要なコンテキスト情報
     * @return 処理後の新しいPoMessageリスト（元のインスタンスは変更しない）
     */
    fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage>
}

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
