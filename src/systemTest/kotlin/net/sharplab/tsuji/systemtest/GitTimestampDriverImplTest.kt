package net.sharplab.tsuji.systemtest

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriverImpl
import net.sharplab.tsuji.core.driver.git.GitTimestampDriverImpl
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Paths

/**
 * System test that verifies GitTimestampDriverImpl against the real GitHub API.
 * Requires GITHUB_TOKEN environment variable to be set.
 */
@EnabledIfEnvironmentVariable(named = "GITHUB_TOKEN", matches = ".+")
class GitTimestampDriverImplTest {

    private val externalProcessDriver = ExternalProcessDriverImpl()
    private val driver = GitTimestampDriverImpl(externalProcessDriver)

    @Test
    fun getTimestamp_withAbsolutePath_shouldReturnValidTimestamp() {
        // Given: a file in the tsuji repository itself
        val repoRoot = Paths.get(".").toAbsolutePath().normalize()
        val filePath = repoRoot.resolve("README.md")

        // When
        val timestamp = driver.getTimestamp(filePath, repoRoot)

        // Then
        assertThat(timestamp.epoch).isGreaterThan(0)
        assertThat(timestamp.iso).doesNotContain("unknown")
    }

    @Test
    fun getTimestamp_withRelativePath_shouldReturnValidTimestamp() {
        // Given: a relative path (this was the bug - relative paths caused 'other' is different type of Path)
        val filePath = Paths.get("README.md")
        val workDir = Paths.get(".")

        // When
        val timestamp = driver.getTimestamp(filePath, workDir)

        // Then
        assertThat(timestamp.epoch).isGreaterThan(0)
        assertThat(timestamp.iso).doesNotContain("unknown")
    }

    @Test
    fun getTimestamp_withSubdirectoryRelativePath_shouldReturnValidTimestamp() {
        // Given: a relative path in a subdirectory
        val filePath = Paths.get("src/main/resources/application.yml")
        val workDir = Paths.get(".")

        // When
        val timestamp = driver.getTimestamp(filePath, workDir)

        // Then
        assertThat(timestamp.epoch).isGreaterThan(0)
        assertThat(timestamp.iso).doesNotContain("unknown")
    }

    @Test
    fun getTimestamp_withNonExistentFile_shouldReturnUnknown() {
        // Given: a file that doesn't exist in the repository
        val filePath = Paths.get("this-file-does-not-exist-anywhere.txt")
        val workDir = Paths.get(".")

        // When
        val timestamp = driver.getTimestamp(filePath, workDir)

        // Then: GitHub API returns empty commits array, so epoch should be 0
        assertThat(timestamp.epoch).isEqualTo(0)
        assertThat(timestamp.iso).isEqualTo("unknown")
    }

    @Test
    fun getTimestamp_withGitSubmodule_shouldReturnValidTimestamp() {
        // Given: clone a repo with a submodule to test cross-repo timestamp retrieval
        val buildDir = SystemTestUtils.prepareTestDir()
        SystemTestUtils.gitClone(buildDir, "https://github.com/quarkusio/ja.quarkus.io.git", depth = 1)

        val repoDir = buildDir.resolve("ja.quarkus.io")
        SystemTestUtils.runCommand(repoDir, "git", "submodule", "update", "--init", "--depth", "1")

        val overrideFile = repoDir.resolve("l10n/override/ja_JP/CNAME")
        val upstreamFile = repoDir.resolve("upstream/CNAME")

        // When
        val overrideTimestamp = driver.getTimestamp(overrideFile, repoDir)
        val upstreamTimestamp = driver.getTimestamp(upstreamFile, repoDir.resolve("upstream"))

        // Then: both should have valid timestamps from their respective GitHub repos
        assertThat(overrideTimestamp.epoch).isGreaterThan(0)
        assertThat(overrideTimestamp.iso).doesNotContain("unknown")
        assertThat(upstreamTimestamp.epoch).isGreaterThan(0)
        assertThat(upstreamTimestamp.iso).doesNotContain("unknown")
    }
}
