package net.sharplab.tsuji.app.service

import java.nio.file.Path

interface TranslationAppService {
    fun machineTranslatePoFiles(
        filePaths: List<Path>?,
        source: String?,
        target: String?,
        asciidocMode: AsciidocMode,
        useRag: Boolean,
        configPath: Path? = null
    )
}
