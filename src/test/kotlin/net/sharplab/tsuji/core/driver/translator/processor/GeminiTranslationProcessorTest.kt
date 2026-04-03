package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationService
import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

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
    fun `process should escape curly braces for LangChain4j`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()
        whenever(mockTranslationService.translate(any(), any(), any())).thenReturn("translated")

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)
        val message = createMessage("Test with {brackets}")
        val context = createContext()

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        // LangChain4j のプロンプトテンプレート用にエスケープされるべき
        verify(mockTranslationService).translate(eq("Test with {{brackets}}"), eq("en"), eq("ja"))
        assertThat(result[0].text).isEqualTo("translated")
        assertThat(result[0].fuzzy).isTrue()
    }

    @Test
    fun `process should use RAG service when useRag is true`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()
        whenever(mockRAGService.translate(any(), any(), any())).thenReturn("rag-translated")

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)
        val message = createMessage("Test text")
        val context = createContext(useRag = true)

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        verify(mockRAGService).translate(eq("Test text"), eq("en"), eq("ja"))
        verifyNoInteractions(mockTranslationService)
        assertThat(result[0].text).isEqualTo("rag-translated")
    }

    @Test
    fun `process with RAG should also escape curly braces`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()
        whenever(mockRAGService.translate(any(), any(), any())).thenReturn("translated")

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)
        val message = createMessage("{RAG} test")
        val context = createContext(useRag = true)

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        // RAGでも波括弧がエスケープされることを確認
        verify(mockRAGService).translate(eq("{{RAG}} test"), eq("en"), eq("ja"))
        verifyNoInteractions(mockTranslationService)
        assertThat(result[0].text).isEqualTo("translated")
    }

    @Test
    fun `process should skip already translated messages`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()
        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)
        val message = createMessage("Hello", messageString = "こんにちは")
        val context = createContext()

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("こんにちは")
        verifyNoInteractions(mockTranslationService, mockRAGService)
    }

    @Test
    fun `process should skip empty text`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()
        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)
        val message = createMessage("")
        val context = createContext()

        // When
        val result = processor.process(listOf(message), context)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEmpty()
        verifyNoInteractions(mockTranslationService, mockRAGService)
    }

    @Test
    fun `process should fill messageString with messageId for special types`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()
        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)
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
        verifyNoInteractions(mockTranslationService, mockRAGService)
    }

    @Test
    fun `process should translate Jekyll Front Matter correctly`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()

        // Mock translations: texts.map { it + "!" } のような簡単な変換
        whenever(mockTranslationService.translate(eq("Hello"), eq("en"), eq("ja")))
            .thenReturn("Hello!")
        whenever(mockTranslationService.translate(eq("World"), eq("en"), eq("ja")))
            .thenReturn("World!")

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)

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

        // 翻訳サービスが呼ばれたことを確認
        verify(mockTranslationService).translate(eq("Hello"), eq("en"), eq("ja"))
        verify(mockTranslationService).translate(eq("World"), eq("en"), eq("ja"))
        verifyNoMoreInteractions(mockTranslationService)
        verifyNoInteractions(mockRAGService)
    }

    @Test
    fun `process should escape curly braces in Jekyll Front Matter`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()

        // Mock translation with escaped braces
        whenever(mockTranslationService.translate(eq("Test {{variable}} here"), eq("en"), eq("ja")))
            .thenReturn("Test {{variable}} here!")

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)

        // Jekyll Front Matter には全フィールドが必要 (layout, title, date, tags, author)
        val jekyllMessage = """title: Test {variable} here
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

        // 波括弧がエスケープされて翻訳サービスに渡されることを確認
        verify(mockTranslationService).translate(eq("Test {{variable}} here"), eq("en"), eq("ja"))

        // 翻訳結果にも波括弧が含まれることを確認
        assertThat(result[0].text).contains("title: Test {{variable}} here!")
        assertThat(result[0].text).contains("layout: post")
    }

    @Test
    fun `process should handle multiple messages`() {
        // Given
        val mockTranslationService = mock<GeminiTranslationService>()
        val mockRAGService = mock<GeminiRAGTranslationService>()
        whenever(mockTranslationService.translate(any(), any(), any())).thenReturn("translated")

        val processor = GeminiTranslationProcessor(mockTranslationService, mockRAGService)
        val messages = listOf(
            createMessage("Text 1"),
            createMessage("Text 2"),
            createMessage("Text 3")
        )
        val context = createContext()

        // When
        val result = processor.process(messages, context)

        // Then
        assertThat(result).hasSize(3)
        verify(mockTranslationService, times(3)).translate(any(), eq("en"), eq("ja"))
        result.forEach {
            assertThat(it.text).isEqualTo("translated")
            assertThat(it.fuzzy).isTrue()
        }
    }
}
