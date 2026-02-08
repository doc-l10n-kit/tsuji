package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.test.createPoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class PoServiceImplTest {

    private val target = PoServiceImpl()

    @Test
    fun isIgnored_shouldReturnTrueForAdocPo() {
        // Given
        val path = Paths.get("test.adoc.po")

        // When
        val result = target.isIgnored(path)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun isIgnored_shouldReturnFalseForRegularPo() {
        // Given
        val path = Paths.get("test.po")

        // When
        val result = target.isIgnored(path)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun resolveMasterPath_shouldRemovePoSuffix() {
        // Given
        val poPath = Paths.get("path/to/master.adoc.po")

        // When
        val result = target.resolveMasterPath(poPath)

        // Then
        assertThat(result).isEqualTo(Paths.get("path/to/master.adoc"))
    }

    @Test
    fun calculateStats_shouldCalculateCorrectly() {
        // Given
        val msg1 = createPoMessage("Hello World", "こんにちは世界")
        val msg2 = createPoMessage("Fuzzy", "あやふや", fuzzy = true)
        val msg3 = createPoMessage("Untranslated message")
        val po = Po("ja", listOf(msg1, msg2, msg3))

        // When
        val stats = target.calculateStats(po)

        // Then
        assertThat(stats.totalMessages).isEqualTo(3)
        assertThat(stats.fuzzyMessages).isEqualTo(2)
        assertThat(stats.totalWords).isEqualTo(5)
        assertThat(stats.fuzzyWords).isEqualTo(3)
        assertThat(stats.achievement).isEqualTo(40)
    }

    @Test
    fun countWords_shouldHandleEmptyAndWhitespace() {
        // Given
        val text = "  Multiple   spaces  "

        // When
        val count = target.countWords(text)

        // Then
        assertThat(count).isEqualTo(2)
    }
}