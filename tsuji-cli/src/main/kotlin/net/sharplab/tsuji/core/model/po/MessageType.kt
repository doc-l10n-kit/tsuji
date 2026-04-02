package net.sharplab.tsuji.core.model.po

/**
 * Represents the type information extracted from PO file comments.
 * This can be any string starting with "type: " prefix.
 */
data class MessageType(val value: String) {
    companion object {
        val None = MessageType("")
        val Title1 = MessageType("type: Title = ")
        val Title2 = MessageType("type: Title ==")
        val Title3 = MessageType("type: Title ===")
        val PlainText = MessageType("type: Plain Text")
        val DelimitedBlock1 = MessageType("type: delimited block =")
        val DelimitedBlock2 = MessageType("type: delimited block ==")
        val DelimitedBlock3 = MessageType("type: delimited block ===")
        val DelimitedBlock = MessageType("type: delimited block -")
        val TargetForMacroImage = MessageType("type: Target for macro image")
        val Table = MessageType("type: Table")

        val YAML_HASH_VALUE_TYPES_GUIDE_CATEGORIES = MessageType("type: Hash Value: types guide categories")
        val YAML_HASH_VALUE_CATEGORIES_CATEGORY = MessageType("type: Hash Value: categories category")
        val YAML_HASH_VALUE_TYPES_REFERENCE_CATEGORIES = MessageType("type: Hash Value: types reference categories")
        val YAML_HASH_VALUE_TYPES_TUTORIAL_CATEGORIES = MessageType("type: Hash Value: types tutorial categories")
        val YAML_HASH_VALUE_CATEGORIES_CAT_ID = MessageType("type: Hash Value: categories cat-id")
        val YAML_HASH_VALUE_TYPES_CONCEPTS_ID = MessageType("type: Hash Value: types concepts id")
        val YAML_HASH_VALUE_TYPES_CONCEPTS_TYPE = MessageType("type: Hash Value: types concepts type")
        val YAML_HASH_VALUE_TYPES_CONCEPTS_TITLE = MessageType("type: Hash Value: types concepts title")
        val YAML_HASH_VALUE_TYPES_CONCEPTS_SUMMARY = MessageType("type: Hash Value: types concepts summary")
        val YAML_HASH_VALUE_TYPES_CONCEPTS_FILENAME = MessageType("type: Hash Value: types concepts filename")
        val YAML_HASH_VALUE_TYPES_CONCEPTS_URL = MessageType("type: Hash Value: types concepts url")
        val YAML_HASH_VALUE_TYPES_GUIDE_ID = MessageType("type: Hash Value: types guide id")
        val YAML_HASH_VALUE_TYPES_GUIDE_TYPE = MessageType("type: Hash Value: types guide type")
        val YAML_HASH_VALUE_TYPES_GUIDE_TITLE = MessageType("type: Hash Value: types guide title")
        val YAML_HASH_VALUE_TYPES_GUIDE_SUMMARY = MessageType("type: Hash Value: types guide summary")
        val YAML_HASH_VALUE_TYPES_GUIDE_FILENAME = MessageType("type: Hash Value: types guide filename")
        val YAML_HASH_VALUE_TYPES_GUIDE_URL = MessageType("type: Hash Value: types guide url")
        val YAML_HASH_VALUE_TYPES_REFERENCE_ID = MessageType("type: Hash Value: types reference id")
        val YAML_HASH_VALUE_TYPES_REFERENCE_TYPE = MessageType("type: Hash Value: types reference type")
        val YAML_HASH_VALUE_TYPES_REFERENCE_TITLE = MessageType("type: Hash Value: types reference title")
        val YAML_HASH_VALUE_TYPES_REFERENCE_SUMMARY = MessageType("type: Hash Value: types reference summary")
        val YAML_HASH_VALUE_TYPES_REFERENCE_FILENAME = MessageType("type: Hash Value: types reference filename")
        val YAML_HASH_VALUE_TYPES_REFERENCE_URL = MessageType("type: Hash Value: types reference url")
        val YAML_HASH_VALUE_TYPES_HOWTO_ID = MessageType("type: Hash Value: types howto id")
        val YAML_HASH_VALUE_TYPES_HOWTO_TYPE = MessageType("type: Hash Value: types howto type")
        val YAML_HASH_VALUE_TYPES_HOWTO_TITLE = MessageType("type: Hash Value: types howto title")
        val YAML_HASH_VALUE_TYPES_HOWTO_SUMMARY = MessageType("type: Hash Value: types howto summary")
        val YAML_HASH_VALUE_TYPES_HOWTO_FILENAME = MessageType("type: Hash Value: types howto filename")
        val YAML_HASH_VALUE_TYPES_HOWTO_URL = MessageType("type: Hash Value: types howto url")
        val YAML_HASH_VALUE_TYPES_TUTORIAL_ID = MessageType("type: Hash Value: types tutorial id")
        val YAML_HASH_VALUE_TYPES_TUTORIAL_TYPE = MessageType("type: Hash Value: types tutorial type")
        val YAML_HASH_VALUE_TYPES_TUTORIAL_TITLE = MessageType("type: Hash Value: types tutorial title")
        val YAML_HASH_VALUE_TYPES_TUTORIAL_SUMMARY = MessageType("type: Hash Value: types tutorial summary")
        val YAML_HASH_VALUE_TYPES_TUTORIAL_FILENAME = MessageType("type: Hash Value: types tutorial filename")
        val YAML_HASH_VALUE_TYPES_TUTORIAL_URL = MessageType("type: Hash Value: types tutorial url")

        val YAML_HASH_VALUE_LINKS_AVATAR = MessageType("type: Hash Value: links avatar")
        val YAML_HASH_VALUE_LINKS_LOGIN = MessageType("type: Hash Value: links login")
        val YAML_HASH_VALUE_LINKS_NAME = MessageType("type: Hash Value: links name")
        val YAML_HASH_VALUE_LINKS_URL = MessageType("type: Hash Value: links url")

        val YAML_HASH_VALUE_CATEGORIES_GUIDES_URL = MessageType("type: Hash Value: categories guides url")
        val YAML_HASH_VALUE_CATEGORIES_GUIDES_TITLE = MessageType("type: Hash Value: categories guides title")
        val YAML_HASH_VALUE_CATEGORIES_GUIDES_DESCRIPTION = MessageType("type: Hash Value: categories guides description")

        fun tryParse(comment: String): MessageType? {
            return if (comment.startsWith("type: ")) {
                MessageType(comment)
            } else {
                null
            }
        }
    }
}
