package net.sharplab.tsuji.app.service

import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import net.sharplab.tsuji.core.driver.jekyll.JekyllDriver
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@Dependent
class JekyllAppServiceImpl(
    private val jekyllDriver: JekyllDriver,
    private val poAppService: PoAppService,
    private val externalProcessDriver: ExternalProcessDriver,
    private val tsujiConfig: TsujiConfig
) : JekyllAppService {

    private val logger = LoggerFactory.getLogger(JekyllAppServiceImpl::class.java)

    override fun build(translate: Boolean, additionalConfigs: List<String>?) {
        val resolvedPoBaseDir = Paths.get(tsujiConfig.po.baseDir)
        val resolvedDestinationDir = Paths.get(tsujiConfig.jekyll.destinationDir)
        val resolvedLanguage = tsujiConfig.language.to
        val baseConfigs = tsujiConfig.jekyll.additionalConfigs.orElse(emptyList())
        val resolvedConfigs = if (additionalConfigs != null) baseConfigs + additionalConfigs else baseConfigs

        withTempWorkDir { workDir ->
            jekyllDriver.prepareSource(
                Paths.get(tsujiConfig.jekyll.sourceDir),
                workDir
            )
            if (translate) {
                jekyllDriver.applyOverrides(Paths.get(tsujiConfig.jekyll.overrideDir), workDir)
                poAppService.applyPoToDirectory(workDir, resolvedPoBaseDir)
            }
            jekyllDriver.build(workDir, resolvedPoBaseDir, resolvedDestinationDir, resolvedLanguage, resolvedConfigs, translate)
        }
    }

    override fun serve(translate: Boolean, additionalConfigs: List<String>?) {
        val resolvedPoBaseDir = Paths.get(tsujiConfig.po.baseDir)
        val resolvedDestinationDir = Paths.get(tsujiConfig.jekyll.destinationDir)
        val resolvedLanguage = tsujiConfig.language.to
        val baseConfigs = tsujiConfig.jekyll.additionalConfigs.orElse(emptyList())
        val resolvedConfigs = if (additionalConfigs != null) baseConfigs + additionalConfigs else baseConfigs

        withTempWorkDir { workDir ->
            jekyllDriver.prepareSource(
                Paths.get(tsujiConfig.jekyll.sourceDir),
                workDir
            )
            if (translate) {
                jekyllDriver.applyOverrides(Paths.get(tsujiConfig.jekyll.overrideDir), workDir)
                poAppService.applyPoToDirectory(workDir, resolvedPoBaseDir)
            }
            jekyllDriver.serve(workDir, resolvedPoBaseDir, resolvedDestinationDir, resolvedLanguage, resolvedConfigs, translate)
        }
    }

    override fun updateOverrideFilesStats(overrideDir: Path, upstreamDir: Path, output: Path) {
        logger.info("Updating override files stats. overrideDir: $overrideDir, upstreamDir: $upstreamDir")
        val results = mutableListOf<OverrideStat>()

        if (!overrideDir.exists()) {
            logger.warn("Override directory does not exist: $overrideDir")
            return
        }

        overrideDir.walk().filter { it.isRegularFile() }.forEach { path ->
            val relativePath = overrideDir.relativize(path).toString()
            val upstreamFile = upstreamDir.resolve(relativePath)

            if (!upstreamFile.exists()) {
                logger.warn("Upstream file not found for override: $relativePath")
                return@forEach
            }

            // Get git timestamps
            val overrideTime = getGitTimestamp(path, path.parent)
            val upstreamTime = getGitTimestamp(upstreamFile, upstreamDir)

            val status = if (overrideTime.epoch >= upstreamTime.epoch) "OK" else "NG"

            results.add(
                OverrideStat(
                    filename = path.toString(),
                    lastModified = overrideTime.iso,
                    upstreamLastModified = upstreamTime.iso,
                    status = status
                )
            )
        }

        output.parent?.createDirectories()
        output.bufferedWriter().use { writer ->
            writer.write("Filename, Last modified, Upstream Last modified, Up to date\n")
            results.forEach { stat ->
                writer.write("${stat.filename}, ${stat.lastModified}, ${stat.upstreamLastModified}, ${stat.status}\n")
            }
        }
        logger.info("Override stats written to: $output")
    }

    override fun updateJekyllStats() {
        val poBaseDir = Paths.get(tsujiConfig.po.baseDir)
        val statsDir = Paths.get(tsujiConfig.jekyll.statsDir)
        statsDir.createDirectories()

        // 1. Latest Guides
        val guidesDir = poBaseDir.resolve("_guides")
        if (guidesDir.exists()) {
            poAppService.updatePoStats(listOf(guidesDir), statsDir.resolve("latest-guides-translation.csv"))
        }

        // 2. Versions
        val versionsDir = poBaseDir.resolve("_versions")
        if (versionsDir.exists()) {
            versionsDir.listDirectoryEntries().filter { it.isDirectory() }.forEach { versionDir ->
                val versionName = versionDir.name
                poAppService.updatePoStats(listOf(versionDir), statsDir.resolve("${versionName}-guides-translation.csv"))
            }
        }

        // 3. Posts
        val postsDir = poBaseDir.resolve("_posts")
        if (postsDir.exists()) {
            poAppService.updatePoStats(listOf(postsDir), statsDir.resolve("posts-translation.csv"))
        }

        // 4. Misc (Includes & Data combined)
        val miscDirs = listOf("_includes", "_data").map { poBaseDir.resolve(it) }.filter { it.exists() }
        if (miscDirs.isNotEmpty()) {
            poAppService.updatePoStats(miscDirs, statsDir.resolve("misc-translation.csv"))
        }

        // 5. Overrides
        updateOverrideFilesStats(
            Paths.get(tsujiConfig.jekyll.overrideDir),
            Paths.get(tsujiConfig.jekyll.sourceDir),
            statsDir.resolve("override.csv")
        )
    }

    private fun getGitTimestamp(path: Path, workDir: Path): GitTimestamp {
        return try {
            val epoch = externalProcessDriver.executeAndGetOutput(
                listOf("git", "log", "-1", "--format=%ct", "--", path.fileName.toString()),
                directory = path.parent
            ).toLong()

            val iso = externalProcessDriver.executeAndGetOutput(
                listOf("git", "log", "-1", "--format=%cd", "--date=iso-local", "--", path.fileName.toString()),
                directory = path.parent,
                env = mapOf("TZ" to "UTC")
            )
            GitTimestamp(epoch, iso)
        } catch (e: Exception) {
            logger.warn("Failed to get git timestamp for $path: ${e.message}")
            GitTimestamp(0, "unknown")
        }
    }

    private fun <T> withTempWorkDir(action: (Path) -> T): T {
        val tempDir = Files.createTempDirectory("tsuji-jekyll-work")
        try {
            logger.info("Created temporary work directory: $tempDir")
            return action(tempDir)
        } finally {
            logger.info("Cleaning up work directory $tempDir")
            tempDir.toFile().deleteRecursively()
        }
    }

    private data class OverrideStat(
        val filename: String,
        val lastModified: String,
        val upstreamLastModified: String,
        val status: String
    )

    private data class GitTimestamp(val epoch: Long, val iso: String)
}