package net.sharplab.tsuji.core.driver.adoc.ext

import net.sharplab.tsuji.core.driver.adoc.ext.StringExt.Companion.indexOfBeginningOfLine
import net.sharplab.tsuji.core.driver.adoc.ext.StringExt.Companion.indexOfEndOfLine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringExtTest {

    @Test
    fun indexOfBeginningOfLine_test(){
        // Given
        val text =
            """
                0123456789
                0123456789
                0123456789
            """.trimIndent()

        // When & Then
        assertThat(text.indexOfBeginningOfLine(1)).isEqualTo(0)
        assertThat(text.indexOfBeginningOfLine(2)).isEqualTo(11) // 10 + 1 since the line contains line break.
        assertThat(text.indexOfBeginningOfLine(3)).isEqualTo(22) // 10*2 + 1*2 since the line contains line break.
    }

    @Test
    fun indexOfEndOfLine_test(){
        // Given
        val text =
                """
                0123456789
                0123456789
                0123456789
            """.trimIndent()

        // When & Then
        assertThat(text.indexOfEndOfLine(1)).isEqualTo(10)
        assertThat(text.indexOfEndOfLine(2)).isEqualTo(21)
        assertThat(text.indexOfEndOfLine(3)).isEqualTo(32)
    }
}
