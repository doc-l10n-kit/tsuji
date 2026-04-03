package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import net.sharplab.tsuji.core.driver.po.PoDriver
import java.nio.file.Path

/**
 * Implementation of PoNormalizerService.
 * Normalizes PO files using PoDriver and GettextDriver.
 */
class PoNormalizerServiceImpl(
    private val poDriver: PoDriver,
    private val gettextDriver: GettextDriver
) : PoNormalizerService {

    override fun normalize(path: Path) {
        // Load and save to remove obsolete entries and POT-Creation-Date
        val po = poDriver.load(path)
        poDriver.save(po, path)

        // Run msgcat for format normalization
        gettextDriver.normalize(path)
    }
}