package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@QuarkusMainTest
class UpdateOverrideStatsCommandTest {

    @Test
    fun testUpdateOverrideStats(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val upstreamDir = buildDir.resolve("upstream").createDirectories()
        val overrideDir = buildDir.resolve("override").createDirectories()
        val outputFile = buildDir.resolve("override.csv")

        runCommand(buildDir, "git", "init")
        runCommand(buildDir, "git", "config", "user.email", "test@example.com")
        runCommand(buildDir, "git", "config", "user.name", "Test User")

        val guideFile = upstreamDir.resolve("guide.adoc")
        guideFile.writeText("Original content")
        runCommand(buildDir, "git", "add", "upstream/guide.adoc")
        runCommand(buildDir, "git", "commit", "-m", "Add upstream guide")

        val overrideFile = overrideDir.resolve("guide.adoc")
        overrideFile.writeText("Override content")
        runCommand(buildDir, "git", "add", "override/guide.adoc")
        runCommand(buildDir, "git", "commit", "-m", "Add override guide")

        val guideFile2 = upstreamDir.resolve("guide2.adoc")
        guideFile2.writeText("Upstream 2")
        runCommand(buildDir, "git", "add", "upstream/guide2.adoc")
        runCommand(buildDir, "git", "commit", "-m", "Add upstream guide 2")

        val overrideFile2 = overrideDir.resolve("guide2.adoc")
        overrideFile2.writeText("Override 2")
        runCommand(buildDir, "git", "add", "override/guide2.adoc")
        runCommand(buildDir, "git", "commit", "-m", "Add override guide 2")

        Thread.sleep(1100) 
        guideFile2.writeText("Upstream 2 updated")
        runCommand(buildDir, "git", "add", "upstream/guide2.adoc")
        runCommand(buildDir, "git", "commit", "-m", "Update upstream guide 2")

        // When
        val result = launcher.launch("jekyll", "update-override-stats", 
            "--override-dir", overrideDir.toString(), 
            "--upstream-dir", upstreamDir.toString(), 
            "--output", outputFile.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(outputFile).exists()
        val csv = outputFile.readText()
        assertThat(csv).contains("Filename, Last modified, Upstream Last modified, Up to date")
        assertThat(csv).contains("guide.adoc", "OK")
        assertThat(csv).contains("guide2.adoc", "NG")
    }

    private fun runCommand(workingDir: Path, vararg command: String) {
        val process = ProcessBuilder(*command)
            .directory(workingDir.toFile())
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().readText()
            throw RuntimeException("Command failed: ${command.joinToString(" ")}\n$error")
        }
    }
}