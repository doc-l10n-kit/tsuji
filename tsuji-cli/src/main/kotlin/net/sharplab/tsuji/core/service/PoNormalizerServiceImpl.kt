package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import java.nio.file.Path

/**
 * PoNormalizerService の実装。
 * GettextDriver を使用してPOファイルを正規化する。
 */
class PoNormalizerServiceImpl(
    private val gettextDriver: GettextDriver
) : PoNormalizerService {

    override fun normalize(path: Path) {
        gettextDriver.normalize(path)
    }
}