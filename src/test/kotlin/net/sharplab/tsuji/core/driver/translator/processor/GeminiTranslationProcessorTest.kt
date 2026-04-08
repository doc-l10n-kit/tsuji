package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.exception.RateLimitException
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationAiService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationAiService
import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlinx.coroutines.runBlocking

/**
 * GeminiTranslationProcessor のテスト。
 *
 * 旧 GeminiTranslatorTest からの移行メモ：
 * - 波括弧のエスケープ（LangChain4j対応）
 * - Jekyll Front Matter の翻訳
 * - RAG使用時の動作
 * - メッセージ分類（skip/fill/jekyll/normal）
 */
internal class GeminiTranslationProcessorTest {

    // Helper extension to call suspend function from test
    private fun MessageProcessor.processBlocking(
        messages: List<TranslationMessage>, 
        context: TranslationContext
    ): List<TranslationMessage> = runBlocking { process(messages, context) }


    private fun createMockParallelismController(): AdaptiveParallelismController {
        return AdaptiveParallelismController(
            initialConcurrency = 3,
            minConcurrency = 1,
            maxConcurrency = 10,
            rateLimitExceptionClass = RateLimitException::class
        )
    }

    private fun createContext(useRag: Boolean = false) = TranslationContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = false,
        useRag = useRag
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
    fun `process should use RAG service when useRag is true`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationAiService>()
        val mockRAGService = mock<GeminiRAGTranslationAiService>()
        whenever(mockRAGService.translate(any(), any(), any())).thenReturn("rag-translated")

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService, parallelismController = createMockParallelismController())
        val message = createMessage("Test text")
        val context = createContext(useRag = true)

        // When
        val result = processor.processBlocking(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        verify(mockRAGService).translate(eq("Test text"), eq("en"), eq("ja"))
        verifyNoInteractions(mockTranslationService)
        assertThat(result[0].text).isEqualTo("rag-translated")
    }


    @Test
    fun `process should skip already translated messages`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationAiService>()
        val mockRAGService = mock<GeminiRAGTranslationAiService>()
        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService, parallelismController = createMockParallelismController())
        val message = createMessage("Hello", messageString = "こんにちは")
        val context = createContext()

        // When
        val result = processor.processBlocking(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("こんにちは")
        verifyNoInteractions(mockTranslationService, mockRAGService)
    }

    @Test
    fun `process should skip empty text`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationAiService>()
        val mockRAGService = mock<GeminiRAGTranslationAiService>()
        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService, parallelismController = createMockParallelismController())
        val message = createMessage("")
        val context = createContext()

        // When
        val result = processor.processBlocking(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEmpty()
        verifyNoInteractions(mockTranslationService, mockRAGService)
    }

    @Test
    fun `process should fill messageString with messageId for special types`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationAiService>()
        val mockRAGService = mock<GeminiRAGTranslationAiService>()
        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService, parallelismController = createMockParallelismController())
        val message = createMessage(
            "some-id",
            type = MessageType.DelimitedBlock
        )
        val context = createContext()

        // When
        val result = processor.processBlocking(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("some-id")
        assertThat(result[0].fuzzy).isFalse()
        verifyNoInteractions(mockTranslationService, mockRAGService)
    }

    @Test
    fun `process should translate Jekyll Front Matter correctly`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationAiService>()
        val mockRAGService = mock<GeminiRAGTranslationAiService>()

        // Mock translations: texts.map { it + "!" } のような簡単な変換
        runBlocking { whenever(mockTranslationService.translate(eq("Hello"), eq("en"), eq("ja"))).thenReturn("Hello!") }
        runBlocking { whenever(mockTranslationService.translate(eq("World"), eq("en"), eq("ja"))).thenReturn("World!") }

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService, parallelismController = createMockParallelismController())

        // 元のテストと同じ形式
        val jekyllMessage = """title: Hello
synopsis: World
layout: post
date: 2024
tags: []
author: me"""

        val message = createMessage(jekyllMessage)
        val context = createContext()

        // When
        val result = processor.processBlocking(listOf(message), context)

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

        // 翻訳サービスが呼ばれたことを確認
        runBlocking { verify(mockTranslationService).translate(eq("Hello"), eq("en"), eq("ja")) }
        runBlocking { verify(mockTranslationService).translate(eq("World"), eq("en"), eq("ja")) }
        verifyNoMoreInteractions(mockTranslationService)
        verifyNoInteractions(mockRAGService)
    }


    @Test
    fun `process should use batch translation for multiple normal messages`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationAiService>()
        val mockRAGService = mock<GeminiRAGTranslationAiService>()

        val translations = listOf("translated 1", "translated 2", "translated 3")
        runBlocking { whenever(mockTranslationService.translateBatch(any(), any(), any())).thenReturn(translations) }

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService, parallelismController = createMockParallelismController())
        val messages = listOf(
            createMessage("Text 1"),
            createMessage("Text 2"),
            createMessage("Text 3")
        )
        val context = createContext()

        // When
        val result = processor.processBlocking(messages, context)

        // Then
        assertThat(result).hasSize(3)
        // Batch method should be called once, not individual translate
        runBlocking { verify(mockTranslationService, times(1)).translateBatch(any(), eq("en"), eq("ja")) }
        runBlocking { verify(mockTranslationService, never()).translate(any(), any(), any()) }

        assertThat(result[0].text).isEqualTo("translated 1")
        assertThat(result[1].text).isEqualTo("translated 2")
        assertThat(result[2].text).isEqualTo("translated 3")
        result.forEach {
            assertThat(it.fuzzy).isTrue()
        }
    }

    @Test
    fun `process should preserve curly braces in batch translation`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationAiService>()
        val mockRAGService = mock<GeminiRAGTranslationAiService>()

        val translations = listOf("translated with {variable}")
        runBlocking { whenever(mockTranslationService.translateBatch(any(), any(), any())).thenReturn(translations) }

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService, parallelismController = createMockParallelismController())
        val message = createMessage("Test {variable} here")
        val context = createContext()

        // When
        val result = processor.processBlocking(listOf(message), context)

        // Then
        // Curly braces should be passed as-is without escaping
        runBlocking {
            verify(mockTranslationService).translateBatch(
                argThat { texts ->
                    texts.size == 1 && texts[0] == "Test {variable} here"
                },
                eq("en"),
                eq("ja")
            )
        }
        assertThat(result[0].text).isEqualTo("translated with {variable}")
    }
}
