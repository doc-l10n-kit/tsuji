package net.sharplab.tsuji.app.service

import java.nio.file.Path

interface RoqAppService {
    fun build(translate: Boolean = true, profile: String? = null)
    fun serve(translate: Boolean = true, profile: String? = null)
    fun extract(profile: String? = null)
    fun updateOverrideFilesStats(overrideDir: Path, upstreamDir: Path, output: Path)
    fun updateRoqStats()
}
