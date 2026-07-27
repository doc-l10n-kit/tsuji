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
    private val gettextDriver: GettextDriver,
    private val defaultTargetLanguage: String?
) : PoNormalizerService {

    override fun normalize(path: Path) {
        // Load and save to remove obsolete entries and POT-Creation-Date
        val po = poDriver.load(path)
        val normalized = if (po.target == null && defaultTargetLanguage != null) {
            po.copy(target = defaultTargetLanguage)
        } else {
            po
        }
        poDriver.save(normalized, path)

        // Run msgcat for format normalization
        gettextDriver.normalize(path)
    }
}