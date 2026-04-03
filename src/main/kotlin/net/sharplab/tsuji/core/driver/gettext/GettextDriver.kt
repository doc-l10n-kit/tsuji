package net.sharplab.tsuji.core.driver.gettext

import java.nio.file.Path

interface GettextDriver {
    /**
     * Normalizes a PO file using 'msgcat'.
     */
    fun normalize(path: Path)
}
