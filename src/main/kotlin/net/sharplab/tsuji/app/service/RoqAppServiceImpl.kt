package net.sharplab.tsuji.app.service

import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.git.GitTimestampDriver
import net.sharplab.tsuji.core.driver.roq.RoqDriver
import net.sharplab.tsuji.core.service.SiteService
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

@Dependent
class RoqAppServiceImpl(
    private val roqDriver: RoqDriver,
    private val poAppService: PoAppService,
    private val gitTimestampDriver: GitTimestampDriver,
    private val siteService: SiteService,
    private val tsujiConfig: TsujiConfig
) : RoqAppService {

    private val logger = LoggerFactory.getLogger(RoqAppServiceImpl::class.java)

    override fun build(translate: Boolean, profile: String?) {
        val resolvedPoBaseDir = Paths.get(tsujiConfig.po.baseDir)
        val resolvedDestinationDir = Paths.get(tsujiConfig.roq.destinationDir)
        val resolvedProfile = profile ?: tsujiConfig.roq.quarkusProfile.orElse(null)
        val htmlInclude = tsujiConfig.roq.extract.html.include.orElse(emptyList())
        val yamlExclude = tsujiConfig.roq.extract.yaml.exclude.orElse(emptyList())

        withTempWorkDir { workDir ->
            roqDriver.prepareSource(Paths.get(tsujiConfig.roq.sourceDir), workDir)
            if (translate) {
                roqDriver.applyOverrides(Paths.get(tsujiConfig.roq.overrideDir), workDir)
                poAppService.applyPoToDirectory(
                    workDir, resolvedPoBaseDir,
                    htmlIncludeList = htmlInclude,
                    yamlExcludeList = yamlExclude
                )
            }
            roqDriver.build(
                workDir, resolvedDestinationDir, resolvedProfile,
                poBaseDir = if (translate) resolvedPoBaseDir else null,
                language = if (translate) tsujiConfig.language.to else null
            )
        }
    }

    override fun serve(translate: Boolean, profile: String?) {
        val resolvedPoBaseDir = Paths.get(tsujiConfig.po.baseDir)
        val resolvedProfile = profile ?: tsujiConfig.roq.quarkusProfile.orElse(null)
        val htmlInclude = tsujiConfig.roq.extract.html.include.orElse(emptyList())
        val yamlExclude = tsujiConfig.roq.extract.yaml.exclude.orElse(emptyList())

        withTempWorkDir { workDir ->
            roqDriver.prepareSource(Paths.get(tsujiConfig.roq.sourceDir), workDir)
            if (translate) {
                roqDriver.applyOverrides(Paths.get(tsujiConfig.roq.overrideDir), workDir)
                poAppService.applyPoToDirectory(
                    workDir, resolvedPoBaseDir,
                    htmlIncludeList = htmlInclude,
                    yamlExcludeList = yamlExclude
                )
            }
            roqDriver.serve(
                workDir, resolvedProfile,
                poBaseDir = if (translate) resolvedPoBaseDir else null,
                language = if (translate) tsujiConfig.language.to else null
            )
        }
    }

    override fun extract(profile: String?) {
        val resolvedPoBaseDir = Paths.get(tsujiConfig.po.baseDir)
        val resolvedProfile = profile ?: tsujiConfig.roq.quarkusProfile.orElse(null)
        val yamlExclude = tsujiConfig.roq.extract.yaml.exclude.orElse(emptyList())
        val htmlInclude = tsujiConfig.roq.extract.html.include.orElse(emptyList())

        withTempWorkDir { workDir ->
            roqDriver.prepareSource(Paths.get(tsujiConfig.roq.sourceDir), workDir)

            // Extract MD/YAML/HTML PO files via po4a (always skip AsciiDoc — handled by l10n-adoc)
            poAppService.extractRoq(
                poBaseDir = resolvedPoBaseDir,
                sourceDir = Paths.get(tsujiConfig.roq.sourceDir),
                overrideDir = Paths.get(tsujiConfig.roq.overrideDir),
            )

            // Extract AsciiDoc PO files via Roq build with extract-on-build=true (default)
            roqDriver.extractBuild(workDir, resolvedProfile, resolvedPoBaseDir, tsujiConfig.language.to.replace("-", "_"))
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

            val overrideTime = gitTimestampDriver.getTimestamp(path, path.parent)
            val upstreamTime = gitTimestampDriver.getTimestamp(upstreamFile, upstreamDir)
            val status = siteService.determineOverrideStatus(overrideTime.epoch, upstreamTime.epoch)

            results.add(OverrideStat(path.toString(), overrideTime.iso, upstreamTime.iso, status))
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

    override fun updateRoqStats() {
        val poBaseDir = Paths.get(tsujiConfig.po.baseDir)
        val statsDir = Paths.get(tsujiConfig.roq.statsDir)
        statsDir.createDirectories()

        val sections = tsujiConfig.roq.statsSections
        sections.forEach { (name, dirs) ->
            val poDirs = dirs.split(",").map { poBaseDir.resolve(it.trim()) }.filter { it.exists() }
            if (poDirs.isNotEmpty()) {
                poAppService.updatePoStats(poDirs, statsDir.resolve("$name-translation.csv"))
            }
        }

        updateOverrideFilesStats(
            Paths.get(tsujiConfig.roq.overrideDir),
            Paths.get(tsujiConfig.roq.sourceDir),
            statsDir.resolve("override.csv")
        )
    }

    private fun <T> withTempWorkDir(action: (Path) -> T): T {
        val tempDir = Files.createTempDirectory("tsuji-roq-work")
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
}
