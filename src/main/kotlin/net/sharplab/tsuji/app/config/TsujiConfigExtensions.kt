package net.sharplab.tsuji.app.config

/**
 * Extension function to convert TsujiConfig.Glossary to prompt text
 */
fun TsujiConfig.Glossary.toPromptText(): String {
    if (!enabled) return ""

    val entriesList = entries.orElse(emptyList())
    if (entriesList.isEmpty()) return ""

    return buildString {
        appendLine("TERMINOLOGY GLOSSARY:")
        entriesList.forEach { entry ->
            appendLine("- \"${entry.term}\" → \"${entry.translation}\"")
        }
        appendLine()
    }
}
