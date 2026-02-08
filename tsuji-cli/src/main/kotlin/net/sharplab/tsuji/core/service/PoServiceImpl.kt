package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po
import java.nio.file.Path
import kotlin.io.path.extension

class PoServiceImpl : PoService {

    override fun isIgnored(path: Path): Boolean {
        val fileName = path.fileName.toString()
        // Skip .adoc.po files as they are handled by asciidoctor-l10n plugin
        return fileName.endsWith(".adoc.po")
    }

    override fun resolveMasterPath(poPath: Path): Path {
        val fileName = poPath.fileName.toString()
        val masterFileName = fileName.removeSuffix(".po")
        return poPath.parent.resolve(masterFileName)
    }

    override fun calculateStats(po: Po): PoStats {
        var totalMessages = 0
        var fuzzyMessages = 0
        var totalWords = 0
        var fuzzyWords = 0

        po.messages.forEach { msg ->
            if (msg.messageId.isEmpty()) return@forEach

            totalMessages++
            val wordCount = countWords(msg.messageId)
            totalWords += wordCount

            if (msg.fuzzy || msg.messageString.isEmpty()) {
                fuzzyMessages++
                fuzzyWords += wordCount
            }
        }

        val achievement = if (totalWords > 0) {
            100 - (fuzzyWords * 100 / totalWords)
        } else {
            100
        }

        return PoStats(
            fuzzyMessages = fuzzyMessages,
            totalMessages = totalMessages,
            fuzzyWords = fuzzyWords,
            totalWords = totalWords,
            achievement = achievement
        )
    }

    override fun countWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split("\\s+".toRegex()).size
    }
}
