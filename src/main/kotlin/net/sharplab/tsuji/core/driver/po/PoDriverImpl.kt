package net.sharplab.tsuji.core.driver.po

import net.sharplab.tsuji.po.PoCodec
import net.sharplab.tsuji.po.model.Po
import java.nio.file.Path

/**
 * PoDriver implementation that wraps PoCodec.
 * Maintains backward compatibility with existing code.
 */
class PoDriverImpl(private val poCodec: PoCodec) : PoDriver {

    // Default constructor for convenience
    constructor() : this(PoCodec())

    override fun load(path: Path): Po = poCodec.load(path)

    override fun save(po: Po, path: Path) = poCodec.save(po, path)
}
