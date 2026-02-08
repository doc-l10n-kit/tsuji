package net.sharplab.tsuji.core.driver.translator.gemini

import net.sharplab.tsuji.app.exception.TsujiAppException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional

class GeminiTranslatorTest {

    private val geminiTranslationService: GeminiTranslationService = mock()
    private val geminiRAGTranslationService: GeminiRAGTranslationService = mock()
    private lateinit var target: GeminiTranslator

    @BeforeEach
    fun setup() {
        target = GeminiTranslator(geminiTranslationService, geminiRAGTranslationService, Optional.of("dummy-key"))
    }

    @Test
    fun translate_should_escape_curly_braces_for_langchain4j() {
        // Given
        val input = "Test with {brackets}"
        val expectedEscaped = "Test with {{brackets}}"
        whenever(geminiTranslationService.translate(any(), any(), any())).thenReturn("translated")

        // When
        target.translate(listOf(input), "en", "ja", useRag = false)

        // Then
        verify(geminiTranslationService).translate(eq(expectedEscaped), eq("en"), eq("ja"))
    }

    @Test
    fun translate_with_rag_should_also_escape_curly_braces() {
        // Given
        val input = "{RAG} test"
        val expectedEscaped = "{{RAG}} test"
        whenever(geminiRAGTranslationService.translate(any(), any(), any())).thenReturn("translated")

        // When
        target.translate(listOf(input), "en", "ja", useRag = true)

        // Then
        verify(geminiRAGTranslationService).translate(eq(expectedEscaped), eq("en"), eq("ja"))
    }

    @Test
    fun translate_should_throw_exception_when_apiKey_is_missing() {
        // Given
        val targetWithNoKey = GeminiTranslator(geminiTranslationService, geminiRAGTranslationService, Optional.empty())

        // When & Then
        assertThrows<TsujiAppException> {
            targetWithNoKey.translate(listOf("text"), "en", "ja", false)
        }
    }
}
