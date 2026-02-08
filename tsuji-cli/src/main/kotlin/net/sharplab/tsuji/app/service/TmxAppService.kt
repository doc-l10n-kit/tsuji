package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import java.nio.file.Path

interface TmxAppService {
    fun applyConfirmedTmx(confirmedTmx: Path, po: Path)
    fun applyFuzzyTmx(fuzzyTmx: Path, po: Path)
    fun generateTmx(poDir: Path, tmxPath: Path, mode: TmxGenerationMode)
}