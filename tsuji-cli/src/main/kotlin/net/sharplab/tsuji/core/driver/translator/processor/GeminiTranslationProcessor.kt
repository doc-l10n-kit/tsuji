package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationService
import net.sharplab.tsuji.core.model.po.type
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.core.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * Translation processor using Gemini API.
 * Processes individual messages (no batch processing).
 */
class GeminiTranslationProcessor(
    private val geminiTranslationService: GeminiTranslationService,
    private val geminiRAGTranslationService: GeminiRAGTranslationService
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(GeminiTranslationProcessor::class.java)

    override fun process(messages: List<TranslationMessage>, context: TranslationContext): List<TranslationMessage> {
        return messages.mapIndexed { index, msg ->
            when {
                // Skip messages that don't need translation
                !msg.needsTranslation -> {
                    logger.info("Skipping message [${index + 1}/${messages.size}]")
                    msg
                }
                // Empty text (after preprocessing or initial text)
                msg.isEmpty() -> {
                    logger.info("Skipping empty text [${index + 1}/${messages.size}]")
                    msg
                }
                // Message that should be filled with messageId (no translation needed)
                MessageClassifier.shouldFillWithMessageId(msg.original) -> {
                    logger.info("Filling with messageId [${index + 1}/${messages.size}]: type=${msg.original.type.value}")
                    msg.withText(msg.original.messageId).withFuzzy(false)
                }
                // Jekyll Front Matter
                MessageClassifier.isJekyllFrontMatter(msg.original.messageId) -> {
                    logger.info("Translating Jekyll Front Matter [${index + 1}/${messages.size}]: source=${context.srcLang}, target=${context.dstLang}")
                    val textToTranslate = msg.text
                    val translated = translateJekyllFrontMatter(textToTranslate, context.srcLang, context.dstLang, context.useRag)
                    msg.withText(translated).withFuzzy(true)
                }
                // Normal translation
                else -> {
                    val textToTranslate = msg.text
                    logger.info("Translating [${index + 1}/${messages.size}]: source=${context.srcLang}, target=${context.dstLang}, useRag=${context.useRag}, text='${textToTranslate.take(50)}'")
                    // Escape braces to avoid LangChain4j prompt template "Variable not found" error
                    val escapedText = textToTranslate.replace("{", "{{").replace("}", "}}")
                    val translated = if (context.useRag) {
                        geminiRAGTranslationService.translate(escapedText, context.srcLang, context.dstLang)
                    } else {
                        geminiTranslationService.translate(escapedText, context.srcLang, context.dstLang)
                    }
                    msg.withText(translated).withFuzzy(true)
                }
            }
        }
    }

    /**
     * Translates Jekyll Front Matter format.
     * Only translates title and synopsis fields, preserving others.
     *
     * In the future, this could be improved to handle structured formats more flexibly using prompts.
     */
    private fun translateJekyllFrontMatter(message: String, srcLang: String, dstLang: String, useRag: Boolean): String {
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

        // Translate title and synopsis individually
        val translated = stringsToTranslate.map { text ->
            val escapedText = text.replace("{", "{{").replace("}", "}}")
            if (useRag) {
                geminiRAGTranslationService.translate(escapedText, srcLang, dstLang)
            } else {
                geminiTranslationService.translate(escapedText, srcLang, dstLang)
            }
        }

        // Replace in the original message
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
