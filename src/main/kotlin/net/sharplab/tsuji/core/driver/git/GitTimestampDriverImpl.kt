package net.sharplab.tsuji.core.driver.git

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class GitTimestampDriverImpl(
    private val externalProcessDriver: ExternalProcessDriver,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) : GitTimestampDriver {

    private val logger = LoggerFactory.getLogger(GitTimestampDriverImpl::class.java)
    private val mapper = jacksonObjectMapper()
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx").withZone(ZoneOffset.UTC)

    // Cache per git root directory
    private val repoRootCache = ConcurrentHashMap<Path, Path>()
    private val remoteInfoCache = ConcurrentHashMap<Path, GitHubRemote?>()

    override fun getTimestamp(filePath: Path, workDir: Path): GitTimestamp {
        return try {
            val repoRoot = resolveRepoRoot(workDir)
            val remote = resolveRemoteInfo(workDir, repoRoot)
            if (remote == null) {
                logger.warn("Could not parse GitHub remote URL for {}. Returning unknown.", filePath)
                return GitTimestamp(0, "unknown")
            }
            val relativePath = repoRoot.relativize(filePath.toAbsolutePath().normalize()).toString()
            fetchTimestampFromGitHub(remote, relativePath)
                ?: GitTimestamp(0, "unknown")
        } catch (e: Exception) {
            logger.warn("Failed to get git timestamp for {}: {}", filePath, e.message)
            GitTimestamp(0, "unknown")
        }
    }

    private fun resolveRepoRoot(workDir: Path): Path {
        return repoRootCache.computeIfAbsent(workDir.toAbsolutePath().normalize()) {
            val output = externalProcessDriver.executeAndGetOutput(
                listOf("git", "rev-parse", "--show-toplevel"),
                directory = workDir
            )
            Paths.get(output)
        }
    }

    private fun resolveRemoteInfo(workDir: Path, repoRoot: Path): GitHubRemote? {
        return remoteInfoCache.computeIfAbsent(repoRoot) {
            try {
                val url = externalProcessDriver.executeAndGetOutput(
                    listOf("git", "remote", "get-url", "origin"),
                    directory = workDir
                )
                parseGitHubRemoteUrl(url)
            } catch (e: Exception) {
                logger.warn("Failed to get remote URL: {}", e.message)
                null
            }
        }
    }

    internal fun parseGitHubRemoteUrl(url: String): GitHubRemote? {
        val regex = Regex("""github\.com[:/]([^/]+)/(.+?)(?:\.git)?$""")
        val match = regex.find(url) ?: return null
        return GitHubRemote(match.groupValues[1], match.groupValues[2])
    }

    private fun fetchTimestampFromGitHub(remote: GitHubRemote, relativePath: String): GitTimestamp? {
        val encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8)
        val uri = URI("https://api.github.com/repos/${remote.owner}/${remote.repo}/commits?path=$encodedPath&per_page=1")

        val requestBuilder = HttpRequest.newBuilder(uri)
            .header("Accept", "application/vnd.github+json")

        val token = System.getenv("GITHUB_TOKEN")
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            logger.warn("GitHub API returned status {} for path: {}", response.statusCode(), relativePath)
            return null
        }

        val commits: JsonNode = mapper.readTree(response.body())
        if (!commits.isArray || commits.isEmpty) {
            logger.warn("GitHub API returned no commits for path: {}", relativePath)
            return null
        }

        val dateStr = commits[0]["commit"]["committer"]["date"].asText()
        val instant = Instant.parse(dateStr)
        return GitTimestamp(instant.epochSecond, isoFormatter.format(instant))
    }

    internal data class GitHubRemote(val owner: String, val repo: String)
}
