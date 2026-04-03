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
        // Filter out obsolete entries (#~) and header message (already stored in Po.header)
        val messages = catalog
            .filter { !it.isObsolete && it.msgid.isNotEmpty() }
            .map { item -> parseMessage(item) }
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
            when {
                // Format: "file:lineNumber"
                parts.size == 2 && parts[1].toIntOrNull() != null -> {
                    PoMessage.SourceReference(File(parts[0]), parts[1].toInt())
                }
                // Format: "file" (no line number)
                parts.size == 1 -> {
                    PoMessage.SourceReference(File(parts[0]), null)
                }
                // Format: "file:nonNumeric" (treat as filename without line number)
                parts.size == 2 -> {
                    PoMessage.SourceReference(File(ref), null)
                }
                else -> null
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

        // Create header from po.header (header message is already filtered out in load())
        val headerMessage = Message()

        // Use existing header if available, otherwise create minimal header
        val headerMap = if (po.header.isNotEmpty()) {
            po.header.toMutableMap().apply {
                // Always set Language to current target
                put("Language", po.target)
                // Remove POT-Creation-Date to prevent unnecessary diffs
                remove("POT-Creation-Date")
            }
        } else {
            mutableMapOf(
                "Language" to po.target,
                "MIME-Version" to "1.0",
                "Content-Type" to "text/plain; charset=UTF-8",
                "Content-Transfer-Encoding" to "8bit",
                "X-Generator" to "doc-l10n-kit"
            )
        }

        // Build header string
        val headerValues = headerMap.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n"

        headerMessage.msgid = ""
        headerMessage.msgstr = headerValues
        catalog.addMessage(headerMessage)

        po.messages.forEach { item ->
            catalog.addMessage(createMessage(item))
        }

        return catalog
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
                if (ref.lineNumber != null) {
                    sourceReferences.add("${ref.file.path}:${ref.lineNumber}")
                } else {
                    sourceReferences.add(ref.file.path)
                }
            }
        }
    }
}
