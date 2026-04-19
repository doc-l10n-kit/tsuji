package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.util.MessageClassifier

/**
 * Classification result for translation messages.
 */
data class ClassifiedMessages(
    val jekyllIndices: List<Int>,
    val normalIndices: List<Int>,
    val fillIndices: List<Int>
)

/**
 * Classifies translation messages into categories for processing.
 */
fun classifyMessages(messages: List<TranslationMessage>): ClassifiedMessages {
    val jekyllIndices = mutableListOf<Int>()
    val normalIndices = mutableListOf<Int>()
    val fillIndices = mutableListOf<Int>()

    messages.forEachIndexed { index, msg ->
        when {
            !msg.needsTranslation -> {} // skip
            msg.isEmpty() -> {} // skip
            MessageClassifier.shouldFillWithMessageId(msg.original) -> fillIndices.add(index)
            MessageClassifier.isJekyllFrontMatter(msg.original.messageId) -> jekyllIndices.add(index)
            else -> normalIndices.add(index)
        }
    }

    return ClassifiedMessages(jekyllIndices, normalIndices, fillIndices)
}
