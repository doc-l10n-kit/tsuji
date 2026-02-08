package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.readText
import kotlin.io.path.writeText

@QuarkusMainTest
class MachineTranslateCommandTest {

    @Test
    fun testMachineTranslate(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val poFile = buildDir.resolve("test.po")
        
        val content = """
            msgid ""
            msgstr ""
            "Content-Type: text/plain; charset=UTF-8\n"
            "Content-Transfer-Encoding: 8bit\n"

            msgid "This is a test message for translation."
            msgstr ""
        """.trimIndent()
        poFile.writeText(content)

        // When
        val result = launcher.launch("po", "machine-translate", "--rag=false", "--po", poFile.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        val translatedPo = poFile.readText()
        assertThat(translatedPo).contains("msgstr \"")
        assertThat(translatedPo).matches("(?s).*msgstr \".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF].*\".*")
    }
}
