package net.sharplab.tsuji.core.service

import java.nio.file.Path

/**
 * POファイルの正規化サービス。
 * POファイルのフォーマットを統一する（obsolete削除、エンコーディング、改行など）。
 */
interface PoNormalizerService {

    /**
     * POファイルを正規化する。
     *
     * @param path POファイルのパス
     */
    fun normalize(path: Path)
}