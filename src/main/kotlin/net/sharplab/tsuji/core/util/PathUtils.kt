package net.sharplab.tsuji.core.util

import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

/**
 * Recursively copies all contents of [source] into [target].
 */
fun copyDirectory(source: Path, target: Path) {
    source.walk().forEach { path ->
        if (path == source) return@forEach
        val relative = source.relativize(path)
        val dest = target.resolve(relative)
        if (path.isDirectory()) {
            dest.createDirectories()
        } else {
            dest.parent?.createDirectories()
            path.copyTo(dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        }
    }
}

/**
 * Copies all files from [overlayDir] into [targetDir], replacing any existing files.
 * Directories in [overlayDir] are skipped; only files are overlaid.
 */
fun applyOverlayFiles(overlayDir: Path, targetDir: Path) {
    overlayDir.walk().forEach { path ->
        if (path == overlayDir || path.isDirectory()) return@forEach
        val relative = overlayDir.relativize(path)
        val dest = targetDir.resolve(relative)
        dest.parent?.createDirectories()
        path.copyTo(dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    }
}
