package net.sharplab.tsuji.app.service

import java.nio.file.Path

interface IndexingAppService {
    fun indexTmx(tmxPath: Path, indexDir: Path? = null)
}