package net.sharplab.tsuji.app.config

/**
 * Extension function to convert TsujiConfig.Glossary to prompt text
 */
fun TsujiConfig.Glossary.toPromptText(): String {
    if (!enabled) return ""

    if (entries.isEmpty()) return ""

    return buildString {
        appendLine("TERMINOLOGY GLOSSARY:")
        entries.forEach { entry ->
            if (entry.context.isPresent) {
                appendLine("- \"${entry.term}\" → \"${entry.translation}\" (${entry.context.get()})")
            } else {
                appendLine("- \"${entry.term}\" → \"${entry.translation}\"")
            }
        }
        appendLine()
    }
}
