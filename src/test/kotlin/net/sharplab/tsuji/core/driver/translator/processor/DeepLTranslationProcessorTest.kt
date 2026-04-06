package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import com.deepl.api.TextResult
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.exception.RateLimitException
import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

/**
 * DeepLTranslationProcessor のテスト。
 *
 * 旧 DeepLTranslatorTest からの移行メモ：
 * - 空の入力処理
 * - Jekyll Front Matter の翻訳
 * - バッチ処理のロジック
 * - メッセージ分類（skip/fill/jekyll/normal）
 */
internal class DeepLTranslationProcessorTest {

    private fun createMockParallelismController(): AdaptiveParallelismController {
        return AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 10,
            rateLimitExceptionClass = RateLimitException::class
        )
    }

    private fun createContext(isAsciidoctor: Boolean = false) = TranslationContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    private fun createMessage(
        messageId: String,
        messageString: String = "",
        type: MessageType = MessageType.PlainText
    ): TranslationMessage {
        val poMessage = PoMessage(
            messageId = messageId,
            messageString = messageString,
            sourceReferences = emptyList(),
            comments = if (type != MessageType.None) listOf(type.value) else emptyList()
        )
        return TranslationMessage(
            original = poMessage,
            text = if (messageString.isEmpty()) messageId else messageString,
            needsTranslation = messageString.isEmpty()
        )
    }

    @Test
    fun `process should return empty list for empty input`() {
        // Given
        val mockApi = mock<com.deepl.api.Translator>()
        val processor = DeepLTranslationProcessor(mockApi, parallelismController = createMockParallelismController())
        val context = createContext()

        // When
        val result = processor.process(emptyList(), context)

        // Then
        assertThat(result).isEmpty()
        verifyNoInteractions(mockApi)
    }

    @Test
    fun `process should skip already translated messages`() {
        // Given
        val mockApi = mock<com.deepl.api.Translator>()
        val processor = DeepLTranslationProcessor(mockApi, parallelismController = createMockParallelismController())
        val message = createMessage("Hello", messageString = "こんにちは")
        val context = createContext()

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("こんにちは")
        verifyNoInteractions(mockApi)
    }

    @Test
    fun `process should skip empty text`() {
        // Given
        val mockApi = mock<com.deepl.api.Translator>()
        val processor = DeepLTranslationProcessor(mockApi, parallelismController = createMockParallelismController())
        val message = createMessage("")
        val context = createContext()

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEmpty()
        verifyNoInteractions(mockApi)
    }

    @Test
    fun `process should translate normal messages in batch`() {
        // Given
        val mockApi = mock<com.deepl.api.Translator>()

        // Mock TextResults for batch translation
        val result1 = mock<TextResult>()
        whenever(result1.text).thenReturn("こんにちは")

        val result2 = mock<TextResult>()
        whenever(result2.text).thenReturn("世界")

        val result3 = mock<TextResult>()
        whenever(result3.text).thenReturn("テスト")

        // Mock translateText for batch (list of 3 texts)
        whenever(mockApi.translateText(
            argThat<List<String>> { this.size == 3 && this[0] == "Hello" && this[1] == "World" && this[2] == "Test" },
            eq("en"),
            eq("ja"),
            any()
        )).thenReturn(listOf(result1, result2, result3))

        val processor = DeepLTranslationProcessor(mockApi, parallelismController = createMockParallelismController())

        val messages = listOf(
            createMessage("Hello"),
            createMessage("World"),
            createMessage("Test")
        )
        val context = createContext()

        // When
        val result = processor.process(messages, context)

        // Then
        assertThat(result).hasSize(3)

        // 各メッセージが翻訳されていることを確認
        assertThat(result[0].text).isEqualTo("こんにちは")
        assertThat(result[0].fuzzy).isTrue()

        assertThat(result[1].text).isEqualTo("世界")
        assertThat(result[1].fuzzy).isTrue()

        assertThat(result[2].text).isEqualTo("テスト")
        assertThat(result[2].fuzzy).isTrue()

        // バッチ翻訳APIが1回だけ呼ばれたことを確認
        verify(mockApi, times(1)).translateText(
            argThat<List<String>> { this.size == 3 },
            eq("en"),
            eq("ja"),
            any()
        )
    }

    @Test
    fun `process should translate Jekyll Front Matter correctly`() {
        // Given
        val mockApi = mock<com.deepl.api.Translator>()

        // Mock TextResult for title and synopsis
        val titleResult = mock<TextResult>()
        whenever(titleResult.text).thenReturn("Hello!")

        val synopsisResult = mock<TextResult>()
        whenever(synopsisResult.text).thenReturn("World!")

        // Mock translateText to return list of TextResults
        whenever(mockApi.translateText(
            argThat<List<String>> { this.size == 2 && this[0] == "Hello" && this[1] == "World" },
            eq("en"),
            eq("ja"),
            any()
        )).thenReturn(listOf(titleResult, synopsisResult))

        val processor = DeepLTranslationProcessor(mockApi, parallelismController = createMockParallelismController())

        // Same format as original test
        val jekyllMessage = """title: Hello
synopsis: World
layout: post
date: 2024
tags: []
author: me"""

        val message = createMessage(jekyllMessage)
        val context = createContext()

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].fuzzy).isTrue()

        // title と synopsis が翻訳されていることを確認
        assertThat(result[0].text).contains("title: Hello!")
        assertThat(result[0].text).contains("synopsis: World!")

        // 他のフィールドは保持されていることを確認
        assertThat(result[0].text).contains("layout: post")
        assertThat(result[0].text).contains("date: 2024")
        assertThat(result[0].text).contains("tags: []")
        assertThat(result[0].text).contains("author: me")

        // 翻訳APIが呼ばれたことを確認
        verify(mockApi).translateText(
            argThat<List<String>> { this.size == 2 && this[0] == "Hello" && this[1] == "World" },
            eq("en"),
            eq("ja"),
            any()
        )
    }

    @Test
    fun `process should fill messageString with messageId for special types`() {
        // Given
        val mockApi = mock<com.deepl.api.Translator>()
        val processor = DeepLTranslationProcessor(mockApi, parallelismController = createMockParallelismController())
        val message = createMessage(
            "some-id",
            type = MessageType.DelimitedBlock
        )
        val context = createContext()

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("some-id")
        assertThat(result[0].fuzzy).isFalse()
        verifyNoInteractions(mockApi)
    }
}
