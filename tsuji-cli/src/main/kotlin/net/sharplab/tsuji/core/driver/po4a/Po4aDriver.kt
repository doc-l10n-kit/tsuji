package net.sharplab.tsuji.core.driver.po4a

import java.nio.file.Path

interface Po4aDriver {
    /**
     * Determines the po4a format name based on the file path (usually by extension).
     * Returns null if the format is not supported or should be skipped.
     */
    fun determineFormat(path: Path): String?

    fun updatePo(masterFile: Path, poFile: Path, format: String)
    fun translate(masterFile: Path, poFile: Path, localizedFile: Path, format: String)
}
