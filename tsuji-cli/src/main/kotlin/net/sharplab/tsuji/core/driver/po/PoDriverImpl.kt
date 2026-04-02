package net.sharplab.tsuji.core.driver.po

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoFlag
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
        val (target, header) = extractHeader(catalog)
        val messages = catalog.map { item -> parseMessage(item) }
        return Po(target, messages, header)
    }

    private fun extractHeader(catalog: Catalog): Pair<String, Map<String, String>> {
        val headerMap = catalog.locateHeader()?.msgstr?.split("\n")
            ?.mapNotNull { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null
                }
            }
            ?.toMap() ?: emptyMap()

        val target = headerMap["Language"] ?: "en_US"
        return target to headerMap
    }

    private fun parseMessage(item: Message): PoMessage {
        val (type, comments) = parseComments(item.extractedComments.toList())
        val sourceReferences = parseSourceReferences(item.sourceReferences.toList())
        val flags = item.formats.map { PoFlag.parse(it) }.toMutableSet()

        return PoMessage(
            type = type,
            messageId = item.msgid,
            messageString = item.msgstr,
            sourceReferences = sourceReferences,
            _flags = flags,
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
            catalog.addMessage(createHeaderMessage(po.target, po.header))
        }

        po.messages.forEach { item ->
            catalog.addMessage(createMessage(item))
        }

        return catalog
    }

    private fun createHeaderMessage(target: String, existingHeader: Map<String, String>): Message {
        return Message().apply {
            msgid = ""

            // Use existing header if available, otherwise create minimal header
            val headerMap = if (existingHeader.isNotEmpty()) {
                existingHeader.toMutableMap().apply {
                    // Always set Language to current target
                    put("Language", target)
                }
            } else {
                mutableMapOf(
                    "Language" to target,
                    "MIME-Version" to "1.0",
                    "Content-Type" to "text/plain; charset=UTF-8",
                    "Content-Transfer-Encoding" to "8bit",
                    "X-Generator" to "tsuji"
                )
            }

            // Build header string
            msgstr = headerMap.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n"
        }
    }

    private fun createMessage(item: PoMessage): Message {
        return Message().apply {
            msgid = item.messageId
            msgstr = item.messageString

            // Set all flags (including fuzzy, no-wrap, etc.)
            item.flags.forEach { flag ->
                formats.add(flag.value)
            }

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
