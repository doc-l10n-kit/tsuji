package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import java.nio.file.Path

interface TmxAppService {
    fun applyConfirmedTmx(confirmedTmx: Path, po: Path? = null)

    fun applyFuzzyTmx(fuzzyTmx: Path, po: Path? = null)

    fun generateTmx(poDir: Path? = null, tmxPath: Path, mode: TmxGenerationMode)
}
