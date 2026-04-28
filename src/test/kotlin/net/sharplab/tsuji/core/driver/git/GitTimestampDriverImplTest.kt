package net.sharplab.tsuji.core.driver.git

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.nio.file.Paths

class GitTimestampDriverImplTest {

    private val externalProcessDriver: ExternalProcessDriver = mock()
    private val httpClient: HttpClient = mock()
    private val target = GitTimestampDriverImpl(externalProcessDriver, httpClient)

    @Test
    fun parseGitHubRemoteUrl_https() {
        val result = target.parseGitHubRemoteUrl("https://github.com/quarkusio/ja.quarkus.io.git")
        assertThat(result).isNotNull
        assertThat(result!!.owner).isEqualTo("quarkusio")
        assertThat(result.repo).isEqualTo("ja.quarkus.io")
    }

    @Test
    fun parseGitHubRemoteUrl_https_without_git_suffix() {
        val result = target.parseGitHubRemoteUrl("https://github.com/quarkusio/ja.quarkus.io")
        assertThat(result).isNotNull
        assertThat(result!!.owner).isEqualTo("quarkusio")
        assertThat(result.repo).isEqualTo("ja.quarkus.io")
    }

    @Test
    fun parseGitHubRemoteUrl_ssh() {
        val result = target.parseGitHubRemoteUrl("git@github.com:quarkusio/ja.quarkus.io.git")
        assertThat(result).isNotNull
        assertThat(result!!.owner).isEqualTo("quarkusio")
        assertThat(result.repo).isEqualTo("ja.quarkus.io")
    }

    @Test
    fun parseGitHubRemoteUrl_non_github() {
        val result = target.parseGitHubRemoteUrl("https://gitlab.com/owner/repo.git")
        assertThat(result).isNull()
    }

    @Test
    fun getTimestamp_success() {
        val repoRoot = Paths.get("/repo")
        val filePath = Paths.get("/repo/l10n/override/ja_JP/blog.md")
        val workDir = Paths.get("/repo/l10n/override/ja_JP")

        setupMocks(repoRoot, workDir)

        @Suppress("UNCHECKED_CAST")
        val httpResponse = mock<HttpResponse<String>>()
        whenever(httpResponse.statusCode()).thenReturn(200)
        whenever(httpResponse.body()).thenReturn("""[{"commit":{"committer":{"date":"2023-08-01T04:39:33Z"}}}]""")
        whenever(httpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>())).thenReturn(httpResponse)

        val result = target.getTimestamp(filePath, workDir)

        assertThat(result.epoch).isEqualTo(1690864773L)
        assertThat(result.iso).isEqualTo("2023-08-01 04:39:33 +0000")
    }

    @Test
    fun getTimestamp_api_error_returns_unknown() {
        val repoRoot = Paths.get("/repo")
        val filePath = Paths.get("/repo/l10n/override/ja_JP/blog.md")
        val workDir = Paths.get("/repo/l10n/override/ja_JP")

        setupMocks(repoRoot, workDir)

        @Suppress("UNCHECKED_CAST")
        val httpResponse = mock<HttpResponse<String>>()
        whenever(httpResponse.statusCode()).thenReturn(403)
        whenever(httpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>())).thenReturn(httpResponse)

        val result = target.getTimestamp(filePath, workDir)

        assertThat(result.epoch).isEqualTo(0L)
        assertThat(result.iso).isEqualTo("unknown")
    }

    @Test
    fun getTimestamp_network_error_returns_unknown() {
        val repoRoot = Paths.get("/repo")
        val filePath = Paths.get("/repo/l10n/override/ja_JP/blog.md")
        val workDir = Paths.get("/repo/l10n/override/ja_JP")

        setupMocks(repoRoot, workDir)

        whenever(httpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()))
            .thenThrow(RuntimeException("Network error"))

        val result = target.getTimestamp(filePath, workDir)

        assertThat(result.epoch).isEqualTo(0L)
        assertThat(result.iso).isEqualTo("unknown")
    }

    @Test
    fun getTimestamp_non_github_remote_returns_unknown() {
        val repoRoot = Paths.get("/repo")
        val filePath = Paths.get("/repo/some/file.txt")
        val workDir = Paths.get("/repo/some")

        whenever(externalProcessDriver.executeAndGetOutput(
            eq(listOf("git", "rev-parse", "--show-toplevel")),
            eq(workDir), any(), any(), any()
        )).thenReturn(repoRoot.toString())

        whenever(externalProcessDriver.executeAndGetOutput(
            eq(listOf("git", "remote", "get-url", "origin")),
            eq(workDir), any(), any(), any()
        )).thenReturn("https://gitlab.com/owner/repo.git")

        val result = target.getTimestamp(filePath, workDir)

        assertThat(result.epoch).isEqualTo(0L)
        assertThat(result.iso).isEqualTo("unknown")
    }

    private fun setupMocks(repoRoot: Path, workDir: Path) {
        whenever(externalProcessDriver.executeAndGetOutput(
            eq(listOf("git", "rev-parse", "--show-toplevel")),
            eq(workDir), any(), any(), any()
        )).thenReturn(repoRoot.toString())

        whenever(externalProcessDriver.executeAndGetOutput(
            eq(listOf("git", "remote", "get-url", "origin")),
            eq(workDir), any(), any(), any()
        )).thenReturn("https://github.com/quarkusio/ja.quarkus.io.git")
    }
}
