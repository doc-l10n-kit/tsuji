package net.sharplab.tsuji.core.driver.po

import net.sharplab.tsuji.po.model.Po
import java.nio.file.Path

/**
 * Driver interface for PO file operations.
 * Facade that delegates to PoCodec in tsuji-po module.
 */
interface PoDriver {

    fun load(path: Path): Po

    fun save(po: Po, path: Path)
}
