package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@QuarkusMainTest
class UpdatePoStatsCommandTest {

    @Test
    fun testUpdatePoStats(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val poDir = buildDir.resolve("po").createDirectories()
        val outputFile = buildDir.resolve("stats.csv")

        poDir.resolve("full.po").writeText("""
            msgid ""
            msgstr ""
            "Content-Type: text/plain; charset=UTF-8\n"
            "Language: ja_JP\n"

            msgid "Hello"
            msgstr "こんにちは"
        """.trimIndent())

        poDir.resolve("fuzzy.po").writeText("""
            msgid ""
            msgstr ""
            "Content-Type: text/plain; charset=UTF-8\n"
            "Language: ja_JP\n"

            #, fuzzy
            msgid "World"
            msgstr "世界"
        """.trimIndent())

        // When
        val result = launcher.launch("po", "update-stats", "--po", poDir.toString(), "--output", outputFile.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(outputFile).exists()
        val csv = outputFile.readText()
        val lines = csv.lines().filter { it.isNotBlank() }
        assertThat(lines[0]).isEqualTo("Filename, Fuzzy Messages, Total Messages, Fuzzy Words, Total Words, Achievement")
        assertThat(lines[1]).contains("fuzzy.po", "1, 1, 1, 1, 0%")
        assertThat(lines[2]).contains("full.po", "0, 1, 0, 1, 100%")
    }
}