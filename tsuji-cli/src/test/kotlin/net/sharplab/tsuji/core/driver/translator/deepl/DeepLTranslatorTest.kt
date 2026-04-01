package net.sharplab.tsuji.core.driver.translator.deepl

import net.sharplab.tsuji.app.exception.TsujiAppException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class DeepLTranslatorTest {

    @Test
    fun `translate should return empty list for empty input`() {
        // Given
        val target = DeepLTranslator(Optional.of("dummy-key"))

        // When
        val result = target.translate(emptyList(), "en", "ja", false)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `translate should throw exception when API key is missing`() {
        // Given
        val target = DeepLTranslator(Optional.empty())

        // When & Then
        val exception = assertThrows<TsujiAppException> {
            target.translate(listOf("test"), "en", "ja", false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should throw exception when API key is blank`() {
        // Given
        val target = DeepLTranslator(Optional.of("   "))

        // When & Then
        val exception = assertThrows<TsujiAppException> {
            target.translate(listOf("test"), "en", "ja", false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }

    @Test
    fun `translate should throw exception when API key is empty string`() {
        // Given
        val target = DeepLTranslator(Optional.of(""))

        // When & Then
        val exception = assertThrows<TsujiAppException> {
            target.translate(listOf("test"), "en", "ja", false)
        }
        assertThat(exception.message).contains("DeepL API key is not configured")
    }
}
