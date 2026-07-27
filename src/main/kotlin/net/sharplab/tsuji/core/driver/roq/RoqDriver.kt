package net.sharplab.tsuji.core.driver.roq

import java.nio.file.Path

interface RoqDriver {
    fun prepareSource(sourceDir: Path, workDir: Path)
    fun applyOverrides(overrideDir: Path, workDir: Path)
    fun build(roqSourceDir: Path, destinationDir: Path, profile: String?, poBaseDir: Path? = null, language: String? = null)
    fun serve(roqSourceDir: Path, profile: String?, poBaseDir: Path? = null, language: String? = null)
    fun extractBuild(roqSourceDir: Path, profile: String?, poBaseDir: Path, language: String?)
}
