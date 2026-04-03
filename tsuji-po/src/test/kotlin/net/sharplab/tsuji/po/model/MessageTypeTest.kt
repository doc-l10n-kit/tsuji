package net.sharplab.tsuji.po.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MessageTypeTest {

    @Test
    fun `tryParse should parse valid type comment`() {
        // Given
        val comment = "type: Hash Value: footer_text"

        // When
        val result = MessageType.tryParse(comment)

        // Then
        assertThat(result).isNotNull
        assertThat(result?.value).isEqualTo("type: Hash Value: footer_text")
    }

    @Test
    fun `tryParse should return null for non-type comment`() {
        // Given
        val comment = "This is a regular comment"

        // When
        val result = MessageType.tryParse(comment)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `tryParse should return null for empty string`() {
        // Given
        val comment = ""

        // When
        val result = MessageType.tryParse(comment)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `tryParse should handle unknown type values`() {
        // Given
        val comment = "type: Unknown Custom Type"

        // When
        val result = MessageType.tryParse(comment)

        // Then
        assertThat(result).isNotNull
        assertThat(result?.value).isEqualTo("type: Unknown Custom Type")
    }

    @Test
    fun `equality check for MessageType None`() {
        // Given
        val type1 = MessageType.None
        val type2 = MessageType("")

        // Then
        assertThat(type1).isEqualTo(type2)
    }

    @Test
    fun `equality check for known type constants`() {
        // Given
        val type1 = MessageType.PlainText
        val type2 = MessageType("type: Plain Text")

        // Then
        assertThat(type1).isEqualTo(type2)
    }

    @Test
    fun `known type constants should have correct values`() {
        assertThat(MessageType.PlainText.value).isEqualTo("type: Plain Text")
        assertThat(MessageType.Title1.value).isEqualTo("type: Title = ")
        assertThat(MessageType.YAML_HASH_VALUE_LINKS_NAME.value).isEqualTo("type: Hash Value: links name")
    }
}
