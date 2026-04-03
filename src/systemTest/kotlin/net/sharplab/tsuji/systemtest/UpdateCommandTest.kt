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
class UpdateCommandTest {

    private val logger = LoggerFactory.getLogger(UpdateCommandTest::class.java)

    @Test
    fun testUpdate(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val masterFile = buildDir.resolve("master.adoc")
        val poFile = buildDir.resolve("master.adoc.po")

        masterFile.writeText("= Master\n\nFirst paragraph.\n")
        
        launcher.launch("po", "update", "--format", "asciidoc", "--master", masterFile.toString(), "--po", poFile.toString()).let {
            assertThat(it.exitCode()).isEqualTo(0)
        }
        assertThat(poFile).exists()
        assertThat(poFile.readText()).contains("First paragraph.")

        masterFile.writeText("= Master\n\nFirst paragraph.\n\nSecond paragraph.\n")

        // When
        val result = launcher.launch("po", "update", "--format", "asciidoc", "--master", masterFile.toString(), "--po", poFile.toString())

        // Then
        if (result.exitCode() != 0) {
            logger.error("STDOUT: ${result.output}\nSTDERR: ${result.errorOutput}")
        }
        assertThat(result.exitCode()).isEqualTo(0)
        val updatedPo = poFile.readText()
        assertThat(updatedPo).contains("First paragraph.")
        assertThat(updatedPo).contains("Second paragraph.")
    }
}
