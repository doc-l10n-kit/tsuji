package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationService
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * Gemini APIを使った翻訳プロセッサー。
 * 個別メッセージ処理（バッチ処理なし）。
 */
class GeminiTranslationProcessor(
    private val geminiTranslationService: GeminiTranslationService,
    private val geminiRAGTranslationService: GeminiRAGTranslationService
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(GeminiTranslationProcessor::class.java)

    override fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage> {
        return messages.mapIndexed { index, msg ->
            when {
                // ヘッダーメッセージ（空のmessageId）
                msg.messageId.isEmpty() -> {
                    logger.info("Skipping header message [${index + 1}/${messages.size}]")
                    msg
                }
                // 既に翻訳済み
                msg.messageString.isNotEmpty() -> {
                    logger.info("Skipping already translated message [${index + 1}/${messages.size}]")
                    msg
                }
                // messageIdで埋めるべきメッセージ（翻訳不要）
                MessageClassifier.shouldFillWithMessageId(msg) -> {
                    logger.info("Filling with messageId [${index + 1}/${messages.size}]: type=${msg.type.value}")
                    msg.copyWithTranslation(messageString = msg.messageId, fuzzy = false)
                }
                // Jekyll Front Matter
                MessageClassifier.isJekyllFrontMatter(msg.messageId) -> {
                    logger.info("Translating Jekyll Front Matter [${index + 1}/${messages.size}]: source=${context.srcLang}, target=${context.dstLang}")
                    val translated = translateJekyllFrontMatter(msg.messageId, context.srcLang, context.dstLang, context.useRag)
                    msg.copyWithTranslation(messageString = translated, fuzzy = true)
                }
                // 通常翻訳
                else -> {
                    logger.info("Translating [${index + 1}/${messages.size}]: source=${context.srcLang}, target=${context.dstLang}, useRag=${context.useRag}, text='${msg.messageId.take(50)}'")
                    // LangChain4jプロンプトテンプレートの"Variable not found"エラーを避けるため、波括弧をエスケープ
                    val escapedText = msg.messageId.replace("{", "{{").replace("}", "}}")
                    val translated = if (context.useRag) {
                        geminiRAGTranslationService.translate(escapedText, context.srcLang, context.dstLang)
                    } else {
                        geminiTranslationService.translate(escapedText, context.srcLang, context.dstLang)
                    }
                    msg.copyWithTranslation(messageString = translated, fuzzy = true)
                }
            }
        }
    }

    /**
     * Jekyll Front Matter形式を翻訳する。
     * titleとsynopsisフィールドのみを翻訳し、他は保持する。
     *
     * 将来的には、プロンプトを使って構造化形式をより柔軟に処理できるように改善可能。
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

        // titleとsynopsisを個別に翻訳
        val translated = stringsToTranslate.map { text ->
            val escapedText = text.replace("{", "{{").replace("}", "}}")
            if (useRag) {
                geminiRAGTranslationService.translate(escapedText, srcLang, dstLang)
            } else {
                geminiTranslationService.translate(escapedText, srcLang, dstLang)
            }
        }

        // 元のメッセージ内で置換
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
