package net.sharplab.tsuji.app.service

import java.nio.file.Path

interface JekyllAppService {
    fun build(translate: Boolean = true, additionalConfigs: List<String>? = null)

    fun serve(translate: Boolean = true, additionalConfigs: List<String>? = null)

    fun updateOverrideFilesStats(overrideDir: Path, upstreamDir: Path, output: Path)

    /**
     * Updates all Jekyll related statistics (PO translation stats and override stats).
     */
    fun updateJekyllStats()
}
