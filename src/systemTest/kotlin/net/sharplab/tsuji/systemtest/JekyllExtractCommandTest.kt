package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files

@QuarkusMainTest
class JekyllExtractCommandTest {

    @Test
    fun testJekyllExtract(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val repoDir = buildDir.resolve("ja.quarkus.io")
        
        SystemTestUtils.gitClone(buildDir, "https://github.com/quarkusio/ja.quarkus.io.git", depth = 1)
        SystemTestUtils.runCommand(repoDir, "git", "submodule", "update", "--init", "--depth", "1", "upstream")

        val upstreamDir = repoDir.resolve("upstream")
        SystemTestUtils.runCommand(upstreamDir, "bundle", "config", "set", "path", "vendor/bundle")
        SystemTestUtils.runCommand(upstreamDir, "bundle", "install")

        val poBaseDir = buildDir.resolve("l10n/po/ja_JP")

        System.setProperty("tsuji.jekyll.source-dir", upstreamDir.toString())
        System.setProperty("tsuji.po.base-dir", poBaseDir.toString())

        // When
        val result = launcher.launch("jekyll", "extract")

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        val poFiles = Files.walk(poBaseDir).use { stream ->
            stream.filter { it.toString().endsWith(".po") }.count()
        }
        assertThat(poFiles).isGreaterThan(0)
    }
}
