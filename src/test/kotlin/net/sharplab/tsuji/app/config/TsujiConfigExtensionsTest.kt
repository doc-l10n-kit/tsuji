package net.sharplab.tsuji.app.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TsujiConfigExtensionsTest {

    // Simple test implementations of the interfaces
    private class TestGlossaryEntry(
        override val term: String,
        override val translation: String,
        override val context: String = ""
    ) : TsujiConfig.GlossaryEntry

    private class TestGlossary(
        override val enabled: Boolean,
        override val entries: List<TsujiConfig.GlossaryEntry>
    ) : TsujiConfig.Glossary

    @Test
    fun `toPromptText should format entries correctly`() {
        // Given
        val entry1 = TestGlossaryEntry("test term", "テスト用語")
        val entry2 = TestGlossaryEntry("another term", "別の用語")
        val glossary = TestGlossary(true, listOf(entry1, entry2))

        // When
        val promptText = glossary.toPromptText()

        // Then
        assertThat(promptText).contains("TERMINOLOGY GLOSSARY:")
        assertThat(promptText).contains("\"test term\" → \"テスト用語\"")
        assertThat(promptText).contains("\"another term\" → \"別の用語\"")
    }

    @Test
    fun `toPromptText should include context when provided`() {
        // Given
        val entry1 = TestGlossaryEntry("dialect", "Dialect", "Hibernate ORM")
        val entry2 = TestGlossaryEntry("extension", "エクステンション")
        val glossary = TestGlossary(true, listOf(entry1, entry2))

        // When
        val promptText = glossary.toPromptText()

        // Then
        assertThat(promptText).contains("\"dialect\" → \"Dialect\" (Hibernate ORM)")
        assertThat(promptText).contains("\"extension\" → \"エクステンション\"")
        assertThat(promptText).doesNotContain("\"extension\" → \"エクステンション\" (")
    }

    @Test
    fun `toPromptText should return empty string when disabled`() {
        // Given
        val glossary = TestGlossary(false, emptyList())

        // When
        val promptText = glossary.toPromptText()

        // Then
        assertThat(promptText).isEmpty()
    }

    @Test
    fun `toPromptText should return empty string when entries list is empty`() {
        // Given
        val glossary = TestGlossary(true, emptyList())

        // When
        val promptText = glossary.toPromptText()

        // Then
        assertThat(promptText).isEmpty()
    }
}
