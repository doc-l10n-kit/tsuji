package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.io.path.exists

@QuarkusMainTest
class ApplyCommandTest {

    private val logger = LoggerFactory.getLogger(ApplyCommandTest::class.java)

    @Test
    fun testApply(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val masterFile = buildDir.resolve("master.adoc")
        val poFile = buildDir.resolve("master.adoc.po")
        val localizedFile = buildDir.resolve("localized.adoc")

        masterFile.writeText("= Master\n\nHello World\n")
        
        // Create initial PO
        val initResult = launcher.launch("po", "update", "--format", "asciidoc", "--master", masterFile.toString(), "--po", poFile.toString())
        assertThat(initResult.exitCode()).isEqualTo(0)

        // Provide translation
        val poContent = poFile.readText()
        val translatedPo = poContent.replace("msgstr \"\"", "msgstr \"こんにちは世界\"")
        poFile.writeText(translatedPo)

        // When
        val result = launcher.launch("po", "apply", "--format", "asciidoc", "--master", masterFile.toString(), "--po", poFile.toString(), "--localized", localizedFile.toString())

        // Then
        if (result.exitCode() != 0) {
            logger.error("STDOUT: ${result.output}\nSTDERR: ${result.errorOutput}")
        }
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(localizedFile).exists()
        val localizedContent = localizedFile.readText()
        assertThat(localizedContent).contains("こんにちは世界")
    }
}
