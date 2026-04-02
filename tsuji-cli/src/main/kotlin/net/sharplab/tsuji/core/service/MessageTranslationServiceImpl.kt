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
}
