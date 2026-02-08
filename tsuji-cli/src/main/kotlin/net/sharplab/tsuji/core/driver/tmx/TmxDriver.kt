package net.sharplab.tsuji.core.driver.tmx

import net.sharplab.tsuji.tmx.model.Tmx
import java.nio.file.Path

/**
 * Driver for handling TMX (Translation Memory eXchange) files.
 */
interface TmxDriver {
    /**
     * Loads a TMX file from the given path.
     */
    fun load(path: Path): Tmx

    /**
     * Saves a Tmx object to the given path.
     */
    fun save(tmx: Tmx, path: Path)
}
