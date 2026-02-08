package net.sharplab.tsuji.core.driver.po

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import org.fedorahosted.tennera.jgettext.Catalog
import org.fedorahosted.tennera.jgettext.Message
import org.fedorahosted.tennera.jgettext.PoParser
import org.fedorahosted.tennera.jgettext.PoWriter
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path

class PoDriverImpl : PoDriver {
    override fun load(path: Path): Po {
        val poParser = PoParser()
        val catalog = poParser.parseCatalog(path.toFile())
        val target = catalog.locateHeader()?.msgstr?.split("\n")
                ?.associate { line -> line.split(":").let { Pair(it.first().trim(), it.last().trim()) } }?.get("Language") ?: "en_US"
        
        val messages = catalog.map { item ->
            val typeValue = item.extractedComments.firstOrNull { it.startsWith("type:") } ?: ""
            val sourceReferences = item.sourceReferences.mapNotNull { ref ->
                val splitted = ref.split(':')
                if (splitted.size == 2) {
                    PoMessage.SourceReference(File(splitted[0]), splitted[1].toInt())
                } else {
                    null
                }
            }
            PoMessage(
                type = MessageType.fromValue(typeValue),
                messageId = item.msgid,
                messageString = item.msgstr,
                sourceReferences = sourceReferences,
                fuzzy = item.isFuzzy
            )
        }
        return Po(target, messages)
    }

    override fun save(po: Po, path: Path) {
        FileOutputStream(path.toFile()).use { outputStream ->
            val poWriter = PoWriter()
            val catalog = Catalog()
            if (po.messages.all { it.messageId.isNotEmpty() }) {
                val headerMessage = Message()
                val headerValues =
                    """Language: ${po.target}
                    MIME-Version: 1.0
                    Content-Type: text/plain; charset=UTF-8
                    Content-Transfer-Encoding: 8bit
                    X-Generator: tsuji
                    """.trimIndent() + "\n"
                headerMessage.msgid = ""
                headerMessage.msgstr = headerValues
                catalog.addMessage(headerMessage)
            }

            po.messages.forEach { item ->
                val message = Message().apply {
                    msgid = item.messageId
                    msgstr = item.messageString
                    isFuzzy = item.fuzzy
                    if (item.type != MessageType.None) {
                        extractedComments.add(item.type.value)
                    }
                    item.sourceReferences.forEach { ref ->
                        sourceReferences.add("${ref.file.path}:${ref.lineNumber}")
                    }
                }
                catalog.addMessage(message)
            }

            poWriter.write(catalog, outputStream)
        }
    }
}
