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
        val catalog = PoParser().parseCatalog(path.toFile())
        val target = extractLanguage(catalog)
        val messages = catalog.map { item -> parseMessage(item) }
        return Po(target, messages)
    }

    private fun extractLanguage(catalog: Catalog): String {
        return catalog.locateHeader()?.msgstr?.split("\n")
            ?.associate { line ->
                line.split(":", limit = 2).let {
                    it.first().trim() to it.getOrElse(1) { "" }.trim()
                }
            }
            ?.get("Language")
            ?: "en_US"
    }

    private fun parseMessage(item: Message): PoMessage {
        val (type, comments) = parseComments(item.extractedComments.toList())
        val sourceReferences = parseSourceReferences(item.sourceReferences.toList())

        return PoMessage(
            type = type,
            messageId = item.msgid,
            messageString = item.msgstr,
            sourceReferences = sourceReferences,
            fuzzy = item.isFuzzy,
            comments = comments
        )
    }

    private fun parseComments(extractedComments: List<String>): Pair<MessageType, List<String>> {
        val type = extractedComments.firstNotNullOfOrNull { MessageType.tryParse(it) } ?: MessageType.None
        val otherComments = extractedComments.filter { MessageType.tryParse(it) == null }
        return type to otherComments
    }

    private fun parseSourceReferences(refs: List<String>): List<PoMessage.SourceReference> {
        return refs.mapNotNull { ref ->
            val parts = ref.split(':', limit = 2)
            if (parts.size == 2) {
                parts[1].toIntOrNull()?.let { lineNumber ->
                    PoMessage.SourceReference(File(parts[0]), lineNumber)
                }
            } else {
                null
            }
        }
    }

    override fun save(po: Po, path: Path) {
        FileOutputStream(path.toFile()).use { outputStream ->
            val catalog = buildCatalog(po)
            PoWriter().write(catalog, outputStream)
        }
    }

    private fun buildCatalog(po: Po): Catalog {
        val catalog = Catalog()

        if (po.messages.all { it.messageId.isNotEmpty() }) {
            catalog.addMessage(createHeaderMessage(po.target))
        }

        po.messages.forEach { item ->
            catalog.addMessage(createMessage(item))
        }

        return catalog
    }

    private fun createHeaderMessage(target: String): Message {
        return Message().apply {
            msgid = ""
            msgstr = """
                Language: $target
                MIME-Version: 1.0
                Content-Type: text/plain; charset=UTF-8
                Content-Transfer-Encoding: 8bit
                X-Generator: tsuji
                """.trimIndent() + "\n"
        }
    }

    private fun createMessage(item: PoMessage): Message {
        return Message().apply {
            msgid = item.messageId
            msgstr = item.messageString
            isFuzzy = item.fuzzy

            if (item.type != MessageType.None) {
                extractedComments.add(item.type.value)
            }
            item.comments.forEach { comment ->
                extractedComments.add(comment)
            }
            item.sourceReferences.forEach { ref ->
                sourceReferences.add("${ref.file.path}:${ref.lineNumber}")
            }
        }
    }
}
