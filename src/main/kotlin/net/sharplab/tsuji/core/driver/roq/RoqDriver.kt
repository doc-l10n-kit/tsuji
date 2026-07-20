package net.sharplab.tsuji.core.driver.roq

import java.nio.file.Path

interface RoqDriver {
    fun prepareSource(sourceDir: Path, workDir: Path)
    fun applyOverrides(overrideDir: Path, workDir: Path)
    fun build(roqSourceDir: Path, destinationDir: Path, profile: String?)
    fun serve(roqSourceDir: Path, profile: String?)
}
