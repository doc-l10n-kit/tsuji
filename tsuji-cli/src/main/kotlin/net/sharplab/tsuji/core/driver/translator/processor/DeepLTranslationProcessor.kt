package net.sharplab.tsuji.core.driver.translator.processor

import com.deepl.api.DeepLException
import com.deepl.api.Formality
import com.deepl.api.TextTranslationOptions
import net.sharplab.tsuji.core.driver.translator.deepl.DeepLTranslatorException
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.util.MessageClassifier
import org.slf4j.LoggerFactory

/**
 * DeepL APIを使った翻訳プロセッサー。
 * バッチ処理により効率的に翻訳する。
 */
class DeepLTranslationProcessor(
    private val deepLApi: com.deepl.api.Translator
) : MessageProcessor {

    private val logger = LoggerFactory.getLogger(DeepLTranslationProcessor::class.java)

    override fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage> {
        if (messages.isEmpty()) {
            return messages
        }

        val options = TextTranslationOptions()
        options.nonSplittingTags = INLINE_ELEMENT_NAMES
        options.ignoreTags = IGNORE_ELEMENT_NAMES
        options.tagHandling = "xml"
        options.formality = Formality.PreferMore

        // メッセージを分類
        val jekyllIndices = mutableListOf<Int>()
        val normalIndices = mutableListOf<Int>()
        val fillIndices = mutableListOf<Int>()
        val skipIndices = mutableListOf<Int>()

        messages.forEachIndexed { index, msg ->
            when {
                msg.messageId.isEmpty() -> skipIndices.add(index)
                msg.messageString.isNotEmpty() -> skipIndices.add(index)
                MessageClassifier.shouldFillWithMessageId(msg) -> fillIndices.add(index)
                MessageClassifier.isJekyllFrontMatter(msg.messageId) -> jekyllIndices.add(index)
                else -> normalIndices.add(index)
            }
        }

        val result = messages.toMutableList()

        // Fill処理（翻訳不要）
        fillIndices.forEach { index ->
            result[index] = result[index].copyWithTranslation(
                messageString = result[index].messageId,
                fuzzy = false
            )
        }

        // Jekyll Front Matter処理（個別翻訳）
        jekyllIndices.forEach { index ->
            logger.info("Translating Jekyll Front Matter [${index + 1}/${messages.size}]")
            val translated = translateJekyllFrontMatter(messages[index].messageId, context.srcLang, context.dstLang, options)
            result[index] = result[index].copyWithTranslation(
                messageString = translated,
                fuzzy = true
            )
        }

        // 通常翻訳（バッチ処理）
        if (normalIndices.isNotEmpty()) {
            val normalTexts = normalIndices.map { messages[it].messageId }
            val textBatches = splitIntoBatches(normalTexts)
            logger.info("Translating ${normalTexts.size} normal texts in ${textBatches.size} batch(es) (${context.srcLang} -> ${context.dstLang})")

            val translated = try {
                textBatches.flatMapIndexed { batchIndex, textBatch ->
                    logger.info("Translating batch ${batchIndex + 1}/${textBatches.size} (${textBatch.size} texts)")
                    deepLApi.translateText(textBatch, context.srcLang, context.dstLang, options).map { it.text }
                }
            } catch (e: DeepLException) {
                throw DeepLTranslatorException("DeepL API error occurred", e)
            }

            translated.forEachIndexed { i, translatedText ->
                val index = normalIndices[i]
                result[index] = result[index].copyWithTranslation(
                    messageString = translatedText,
                    fuzzy = true
                )
            }
        }

        return result
    }

    /**
     * Jekyll Front Matter形式を翻訳する。
     * titleとsynopsisフィールドのみを翻訳し、他は保持する。
     */
    private fun translateJekyllFrontMatter(
        message: String,
        srcLang: String,
        dstLang: String,
        options: TextTranslationOptions
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

        // titleとsynopsisを翻訳
        val translated = try {
            deepLApi.translateText(stringsToTranslate, srcLang, dstLang, options).map { it.text }
        } catch (e: DeepLException) {
            throw DeepLTranslatorException("DeepL API error occurred", e)
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

    /**
     * テキストをDeepL APIの制限に従ってバッチに分割する。
     *
     * DeepL APIの制限:
     * - 1リクエストあたり最大50テキスト
     * - リクエストボディの最大サイズ128 KiB
     *
     * JSONオーバーヘッドのマージンを確保するため、100,000バイト（約97 KiB）を制限として使用。
     */
    private fun splitIntoBatches(texts: List<String>): List<List<String>> {
        val batches = mutableListOf<List<String>>()
        var currentBatch = mutableListOf<String>()
        var currentSizeBytes = 0

        texts.forEach { text ->
            val textSizeBytes = text.toByteArray(Charsets.UTF_8).size

            // このテキストを追加すると制限を超えるかチェック
            if (currentBatch.size >= MAX_TEXTS_PER_REQUEST ||
                (currentBatch.isNotEmpty() && currentSizeBytes + textSizeBytes > MAX_TEXT_SIZE_BYTES)
            ) {
                // 現在のバッチを保存し、新しいバッチを開始
                batches.add(currentBatch)
                currentBatch = mutableListOf()
                currentSizeBytes = 0
            }

            currentBatch.add(text)
            currentSizeBytes += textSizeBytes
        }

        // 最後のバッチを追加
        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch)
        }

        return batches
    }

    companion object {
        // DeepL APIの制限
        private const val MAX_TEXTS_PER_REQUEST = 50
        // 128 KiBではなく100,000バイト（約97 KiB）を使用してJSONオーバーヘッドのマージンを確保
        private const val MAX_TEXT_SIZE_BYTES = 100_000

        // 翻訳しないタグ
        private val IGNORE_ELEMENT_NAMES = listOf(
            "abbr", "b", "cite", "code", "data", "dfn", "kbd", "rp", "rt", "rtc", "ruby", "samp", "time", "var"
        )

        // 分割しないインライン要素タグ
        private val INLINE_ELEMENT_NAMES = listOf(
            "a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "data", "dfn", "em", "i", "kbd",
            "mark", "q", "rp", "rt", "rtc", "ruby", "s", "samp", "small", "span", "strong", "sub",
            "sup", "time", "u", "var", "wbr"
        )
    }
}
