package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.test.createPoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MessageTranslationServiceImplTest {

    private val target = MessageTranslationServiceImpl()

    @Test
    fun shouldTranslate_shouldReturnTrueForNormalText() {
        // Given
        val msg = createPoMessage("Hello")

        // When
        val result = target.shouldTranslate(msg)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun shouldTranslate_shouldReturnFalseForIgnoredTypes() {
        // Given
        val msg = createPoMessage("some-url", type = MessageType.YAML_HASH_VALUE_LINKS_URL)

        // When
        val result = target.shouldTranslate(msg)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun isSpecialFormat_shouldDetectBlogHeader() {
        // Given
        val header = """
            layout: post
            title: My Blog Post
            date: 2024-01-01
            tags: [test]
            author: ynojima
        """.trimIndent()
        val msg = createPoMessage(header)

        // When
        val result = target.isSpecialFormat(msg)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun translateSpecialFormat_shouldTranslateOnlyTitleAndSynopsis() {
        // Given
        val header = """
            title: Hello
            synopsis: World
            layout: post
            date: 2024
            tags: []
            author: me
        """.trimIndent()

        // When
        val result = target.translateSpecialFormat(header, "en", "ja", true) { texts, _, _, _ ->
            texts.map { it + "!" } // Dummy translation
        }

        // Then
        assertThat(result).contains("title: Hello!")
        assertThat(result).contains("synopsis: World!")
        assertThat(result).contains("layout: post") // Should remain unchanged
    }
}