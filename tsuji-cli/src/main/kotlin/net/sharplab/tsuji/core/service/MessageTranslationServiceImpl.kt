package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.PoMessage

class MessageTranslationServiceImpl : MessageTranslationService {

    override fun shouldTranslate(message: PoMessage): Boolean {
        if (message.messageId.isEmpty()) return false
        if (message.messageString.isNotEmpty()) return false

        return when (message.type) {
            MessageType.DelimitedBlock -> false
            MessageType.TargetForMacroImage -> false
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_CATEGORIES,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_CATEGORIES,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_CATEGORIES,
            MessageType.YAML_HASH_VALUE_CATEGORIES_CAT_ID,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_ID,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_URL,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_ID,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_URL,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_ID,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_URL,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_ID,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_URL,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_ID,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_URL,
            MessageType.YAML_HASH_VALUE_LINKS_AVATAR,
            MessageType.YAML_HASH_VALUE_LINKS_LOGIN,
            MessageType.YAML_HASH_VALUE_LINKS_NAME,
            MessageType.YAML_HASH_VALUE_LINKS_URL,
            MessageType.YAML_HASH_VALUE_CATEGORIES_GUIDES_URL -> false
            else -> true
        }
    }

    override fun shouldFillWithMessageId(message: PoMessage): Boolean {
        return when (message.type) {
            MessageType.DelimitedBlock,
            MessageType.TargetForMacroImage,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_CATEGORIES,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_CATEGORIES,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_CATEGORIES,
            MessageType.YAML_HASH_VALUE_CATEGORIES_CAT_ID,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_ID,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_CONCEPTS_URL,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_ID,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_GUIDE_URL,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_ID,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_REFERENCE_URL,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_ID,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_HOWTO_URL,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_ID,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_TYPE,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_FILENAME,
            MessageType.YAML_HASH_VALUE_TYPES_TUTORIAL_URL,
            MessageType.YAML_HASH_VALUE_LINKS_AVATAR,
            MessageType.YAML_HASH_VALUE_LINKS_LOGIN,
            MessageType.YAML_HASH_VALUE_LINKS_NAME,
            MessageType.YAML_HASH_VALUE_LINKS_URL,
            MessageType.YAML_HASH_VALUE_CATEGORIES_GUIDES_URL -> true
            else -> false
        }
    }

    override fun isSpecialFormat(message: PoMessage): Boolean {
        val lines = message.messageId.lines()
        return lines.any { it.startsWith("layout:") } &&
               lines.any { it.startsWith("title:") } &&
               lines.any { it.startsWith("date:") } &&
               lines.any { it.startsWith("tags:") } &&
               lines.any { it.startsWith("author:") }
    }

    override fun translateSpecialFormat(
        message: String,
        sourceLang: String,
        targetLang: String,
        useRag: Boolean,
        translateAction: (List<String>, String, String, Boolean) -> List<String>
    ): String {
        val titleRegex = Regex("""^title:\s*(.*)$""", RegexOption.MULTILINE)
        val synopsisRegex = Regex("""^synopsis:\s*(.*)$""", RegexOption.MULTILINE)

        val titleMatch = titleRegex.find(message)
        val synopsisMatch = synopsisRegex.find(message)

        val title = titleMatch?.groupValues?.get(1)?.trim() ?: ""
        val synopsis = synopsisMatch?.groupValues?.get(1)?.trim() ?: ""

        val stringsToTranslate = mutableListOf<String>()
        if (title.isNotEmpty()) stringsToTranslate.add(title)
        if (synopsis.isNotEmpty()) stringsToTranslate.add(synopsis)

        if (stringsToTranslate.isEmpty()) return message

        val translated = translateAction(stringsToTranslate, sourceLang, targetLang, useRag)

        var translatedIndex = 0
        var replaced = message
        if (title.isNotEmpty()) {
            val titleTranslated = translated[translatedIndex++]
            replaced = titleRegex.replace(replaced) { "title: $titleTranslated" }
        }
        if (synopsis.isNotEmpty()) {
            val synopsisTranslated = translated[translatedIndex++]
            replaced = synopsisRegex.replace(replaced) { "synopsis: $synopsisTranslated" }
        }
        return replaced
    }
}
