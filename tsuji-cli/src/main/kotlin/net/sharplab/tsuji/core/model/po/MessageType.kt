package net.sharplab.tsuji.core.model.po

enum class MessageType(val value: String) {
    None(""),
    Title1("type: Title = "),
    Title2("type: Title =="),
    Title3("type: Title ==="),
    PlainText("type: Plain Text"),
    DelimitedBlock1("type: delimited block ="),
    DelimitedBlock2("type: delimited block =="),
    DelimitedBlock3("type: delimited block ==="),
    DelimitedBlock("type: delimited block -"),
    TargetForMacroImage("type: Target for macro image"),
    Table("type: Table"),

    YAML_HASH_VALUE_TYPES_GUIDE_CATEGORIES("type: Hash Value: types guide categories"),
    YAML_HASH_VALUE_CATEGORIES_CATEGORY("type: Hash Value: categories category"),
    YAML_HASH_VALUE_TYPES_REFERENCE_CATEGORIES("type: Hash Value: types reference categories"),
    YAML_HASH_VALUE_TYPES_TUTORIAL_CATEGORIES("type: Hash Value: types tutorial categories"),
    YAML_HASH_VALUE_CATEGORIES_CAT_ID("type: Hash Value: categories cat-id"),
    YAML_HASH_VALUE_TYPES_CONCEPTS_ID("type: Hash Value: types concepts id"),
    YAML_HASH_VALUE_TYPES_CONCEPTS_TYPE("type: Hash Value: types concepts type"),
    YAML_HASH_VALUE_TYPES_CONCEPTS_TITLE("type: Hash Value: types concepts title"),
    YAML_HASH_VALUE_TYPES_CONCEPTS_SUMMARY("type: Hash Value: types concepts summary"),
    YAML_HASH_VALUE_TYPES_CONCEPTS_FILENAME("type: Hash Value: types concepts filename"),
    YAML_HASH_VALUE_TYPES_CONCEPTS_URL("type: Hash Value: types concepts url"),
    YAML_HASH_VALUE_TYPES_GUIDE_ID("type: Hash Value: types guide id"),
    YAML_HASH_VALUE_TYPES_GUIDE_TYPE("type: Hash Value: types guide type"),
    YAML_HASH_VALUE_TYPES_GUIDE_TITLE("type: Hash Value: types guide title"),
    YAML_HASH_VALUE_TYPES_GUIDE_SUMMARY("type: Hash Value: types guide summary"),
    YAML_HASH_VALUE_TYPES_GUIDE_FILENAME("type: Hash Value: types guide filename"),
    YAML_HASH_VALUE_TYPES_GUIDE_URL("type: Hash Value: types guide url"),
    YAML_HASH_VALUE_TYPES_REFERENCE_ID("type: Hash Value: types reference id"),
    YAML_HASH_VALUE_TYPES_REFERENCE_TYPE("type: Hash Value: types reference type"),
    YAML_HASH_VALUE_TYPES_REFERENCE_TITLE("type: Hash Value: types reference title"),
    YAML_HASH_VALUE_TYPES_REFERENCE_SUMMARY("type: Hash Value: types reference summary"),
    YAML_HASH_VALUE_TYPES_REFERENCE_FILENAME("type: Hash Value: types reference filename"),
    YAML_HASH_VALUE_TYPES_REFERENCE_URL("type: Hash Value: types reference url"),
    YAML_HASH_VALUE_TYPES_HOWTO_ID("type: Hash Value: types howto id"),
    YAML_HASH_VALUE_TYPES_HOWTO_TYPE("type: Hash Value: types howto type"),
    YAML_HASH_VALUE_TYPES_HOWTO_TITLE("type: Hash Value: types howto title"),
    YAML_HASH_VALUE_TYPES_HOWTO_SUMMARY("type: Hash Value: types howto summary"),
    YAML_HASH_VALUE_TYPES_HOWTO_FILENAME("type: Hash Value: types howto filename"),
    YAML_HASH_VALUE_TYPES_HOWTO_URL("type: Hash Value: types howto url"),
    YAML_HASH_VALUE_TYPES_TUTORIAL_ID("type: Hash Value: types tutorial id"),
    YAML_HASH_VALUE_TYPES_TUTORIAL_TYPE("type: Hash Value: types tutorial type"),
    YAML_HASH_VALUE_TYPES_TUTORIAL_TITLE("type: Hash Value: types tutorial title"),
    YAML_HASH_VALUE_TYPES_TUTORIAL_SUMMARY("type: Hash Value: types tutorial summary"),
    YAML_HASH_VALUE_TYPES_TUTORIAL_FILENAME("type: Hash Value: types tutorial filename"),
    YAML_HASH_VALUE_TYPES_TUTORIAL_URL("type: Hash Value: types tutorial url"),

    YAML_HASH_VALUE_LINKS_AVATAR("type: Hash Value: links avatar"),
    YAML_HASH_VALUE_LINKS_LOGIN("type: Hash Value: links login"),
    YAML_HASH_VALUE_LINKS_NAME("type: Hash Value: links name"),
    YAML_HASH_VALUE_LINKS_URL("type: Hash Value: links url"),

    YAML_HASH_VALUE_CATEGORIES_GUIDES_URL("type: Hash Value: categories guides url"),
    YAML_HASH_VALUE_CATEGORIES_GUIDES_TITLE("type: Hash Value: categories guides title"),
    YAML_HASH_VALUE_CATEGORIES_GUIDES_DESCRIPTION("type: Hash Value: categories guides description");

    companion object {
        fun fromValue(value: String): MessageType {
            return entries.find { it.value == value } ?: None
        }
    }
}
