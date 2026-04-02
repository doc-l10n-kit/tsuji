package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import java.nio.file.Path

/**
 * Implementation of PoNormalizerService.
 * Normalizes PO files using GettextDriver.
 */
class PoNormalizerServiceImpl(
    private val gettextDriver: GettextDriver
) : PoNormalizerService {

    override fun normalize(path: Path) {
        gettextDriver.normalize(path)
    }
}