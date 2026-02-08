package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.io.path.exists
import kotlin.io.path.readText

@QuarkusMainTest
class JekyllBuildCommandTest {

    private val logger = LoggerFactory.getLogger(JekyllBuildCommandTest::class.java)

    @Test
    fun testJekyllBuild(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val repoDir = buildDir.resolve("ja.quarkus.io")
        
        SystemTestUtils.gitClone(buildDir, "https://github.com/quarkusio/ja.quarkus.io.git", depth = 1)
        SystemTestUtils.runCommand(repoDir, "git", "submodule", "update", "--init", "--depth", "1", "upstream")

        val upstreamDir = repoDir.resolve("upstream")
        SystemTestUtils.runCommand(upstreamDir, "bundle", "config", "set", "path", "vendor/bundle")
        SystemTestUtils.runCommand(upstreamDir, "bundle", "install")

        val poBaseDir = repoDir.resolve("l10n/po/ja_JP")
        val destinationDir = buildDir.resolve("site-out")

        System.setProperty("tsuji.jekyll.source-dir", upstreamDir.toString())
        System.setProperty("tsuji.jekyll.destination-dir", destinationDir.toString())
        System.setProperty("tsuji.jekyll.language", "ja")
        System.setProperty("tsuji.po.base-dir", poBaseDir.toString())

        // When
        val result = launcher.launch("jekyll", "build")

        // Then
        if (result.exitCode() != 0) {
            logger.error("Jekyll build failed. STDOUT: ${result.output}\nSTDERR: ${result.errorOutput}")
        }
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(destinationDir).exists()
        val indexHtml = destinationDir.resolve("index.html")
        assertThat(indexHtml).exists()
        assertThat(indexHtml.readText()).contains("Quarkus")
    }
}