package net.sharplab.tsuji.app.service

import java.nio.file.Path

interface RoqAppService {
    fun build(translate: Boolean = true, profile: String? = null, skipAsciidoc: Boolean = false)
    fun serve(translate: Boolean = true, profile: String? = null, skipAsciidoc: Boolean = false)
    fun updateOverrideFilesStats(overrideDir: Path, upstreamDir: Path, output: Path)
    fun updateRoqStats()
}
