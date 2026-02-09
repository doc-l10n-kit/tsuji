package net.sharplab.tsuji.app.service

import java.nio.file.Path

interface PoAppService {
    fun normalize(poPath: Path)
    fun purgeFuzzy(poPath: Path)
    fun removeObsolete(poDir: Path, upstreamDir: Path)
    fun update(masterFile: Path, poFile: Path, format: String)
    fun apply(masterFile: Path, poFile: Path, localizedFile: Path, format: String)
    fun extractJekyllAdoc(poBaseDir: Path? = null, sourceDir: Path? = null, overrideDir: Path? = null)
    fun applyPoToDirectory(workDir: Path, poBaseDir: Path)
    fun updatePoStats(poDirs: List<Path>?, output: Path?)
}
