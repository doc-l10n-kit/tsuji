package net.sharplab.tsuji.core.driver.tmx

import net.sharplab.tsuji.tmx.TmxCodec
import net.sharplab.tsuji.tmx.model.*
import java.nio.file.Path

class TmxDriverImpl(private val tmxCodec: TmxCodec) : TmxDriver {

    override fun load(path: Path): Tmx {
        return tmxCodec.load(path)
    }

    override fun save(tmx: Tmx, path: Path) {
        tmxCodec.save(tmx, path)
    }
}
