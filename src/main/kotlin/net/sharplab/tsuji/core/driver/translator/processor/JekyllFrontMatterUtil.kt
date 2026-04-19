package net.sharplab.tsuji.core.driver.translator.processor

/**
 * Utility for extracting and replacing translatable fields in Jekyll Front Matter.
 */
object JekyllFrontMatterUtil {

    private val titleRegex = Regex("""^title:\s*(.*)$""", RegexOption.MULTILINE)
    private val synopsisRegex = Regex("""^synopsis:\s*(.*)$""", RegexOption.MULTILINE)

    /**
     * Extracts translatable field values (title, synopsis) from a Jekyll Front Matter message.
     * Returns a list of non-empty field values in order: [title, synopsis].
     */
    fun extractFields(message: String): List<String> {
        val title = titleRegex.find(message)?.groupValues?.get(1)?.trim() ?: ""
        val synopsis = synopsisRegex.find(message)?.groupValues?.get(1)?.trim() ?: ""

        val fields = mutableListOf<String>()
        if (title.isNotEmpty()) fields.add(title)
        if (synopsis.isNotEmpty()) fields.add(synopsis)
        return fields
    }

    /**
     * Replaces translatable fields in the original message with translated values.
     * [translations] must be in the same order as returned by [extractFields].
     */
    fun replaceFields(message: String, translations: List<String>): String {
        val title = titleRegex.find(message)?.groupValues?.get(1)?.trim() ?: ""
        val synopsis = synopsisRegex.find(message)?.groupValues?.get(1)?.trim() ?: ""

        var translatedIndex = 0
        var replaced = message
        if (title.isNotEmpty()) {
            val titleTranslated = translations[translatedIndex++]
            replaced = titleRegex.replace(replaced) { "title: $titleTranslated" }
        }
        if (synopsis.isNotEmpty()) {
            val synopsisTranslated = translations[translatedIndex++]
            replaced = synopsisRegex.replace(replaced) { "synopsis: $synopsisTranslated" }
        }
        return replaced
    }
}
