package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import net.sharplab.tsuji.core.driver.jekyll.JekyllDriver
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.po4a.Po4aDriver
import net.sharplab.tsuji.core.service.PoService
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import jakarta.enterprise.context.Dependent
import kotlin.io.path.*

@Dependent
class PoAppServiceImpl(
    private val gettextDriver: GettextDriver,
    private val poDriver: PoDriver,
    private val po4aDriver: Po4aDriver,
    private val jekyllDriver: JekyllDriver,
    private val poService: PoService,
    private val tsujiConfig: TsujiConfig
) : PoAppService {

    private val logger = LoggerFactory.getLogger(PoAppServiceImpl::class.java)

    override fun normalize(poPath: Path) {
        if (!poPath.exists()) {
            logger.warn("Normalization skipped: path does not exist: $poPath")
            return
        }
        if (poPath.isDirectory()) {
            logger.info("Normalizing PO files in directory: $poPath")
            Files.walk(poPath).use { stream ->
                stream.filter { it.extension == "po" }
                    .forEach { 
                        logger.debug("Normalizing: $it")
                        gettextDriver.normalize(it) 
                    }
            }
        } else {
            logger.info("Normalizing PO file: $poPath")
            gettextDriver.normalize(poPath)
        }
    }

    override fun purgeFuzzy(poPath: Path) {
        val paths = if (poPath.isDirectory()) {
            logger.info("Purging fuzzy messages in directory: $poPath")
            Files.walk(poPath).use { stream ->
                stream.filter { it.extension == "po" }.toList()
            }
        } else {
            logger.info("Purging fuzzy messages in file: $poPath")
            listOf(poPath)
        }

        paths.forEach { path ->
            logger.debug("Processing: $path")
            val po = poDriver.load(path)
            po.messages.forEach { msg ->
                if (msg.fuzzy) {
                    msg.messageString = ""
                    msg.fuzzy = false
                }
            }
            poDriver.save(po, path)
        }
    }

    override fun removeObsolete(poDir: Path, upstreamDir: Path) {
        logger.info("Removing obsolete PO files in $poDir based on $upstreamDir")
        Files.walk(poDir).use { stream ->
            stream.filter { it.extension == "po" }
                .forEach { poFile ->
                    val relative = poDir.relativize(poFile)
                    val originalFileName = relative.toString().removeSuffix(".po")
                    val originalFile = upstreamDir.resolve(originalFileName).normalize()
                    
                    if (!originalFile.exists()) {
                        logger.info("Removing obsolete po file: $poFile")
                        poFile.deleteExisting()
                    }
                }
        }
    }

    override fun update(masterFile: Path, poFile: Path, format: String) {
        logger.info("Updating PO file $poFile from $masterFile (format: $format)")
        poFile.parent.createDirectories()
        po4aDriver.updatePo(masterFile, poFile, format)
        gettextDriver.normalize(poFile)
    }

    override fun apply(masterFile: Path, poFile: Path, localizedFile: Path, format: String) {
        logger.info("Applying PO file $poFile to $masterFile -> $localizedFile (format: $format)")
        localizedFile.parent.createDirectories()
        po4aDriver.translate(masterFile, poFile, localizedFile, format)
    }

    override fun extractJekyllAdoc(poBaseDir: Path?, sourceDir: Path?, overrideDir: Path?) {
        val resolvedPoBaseDir = poBaseDir ?: Paths.get(tsujiConfig.po.baseDir)
        val resolvedSourceDir = sourceDir ?: Paths.get(tsujiConfig.jekyll.sourceDir)
        val resolvedOverrideDir = overrideDir ?: Paths.get(tsujiConfig.jekyll.overrideDir)

        withTempWorkDir { workDir ->
            logger.info("Extracting AsciiDoc PO files in $resolvedPoBaseDir using Jekyll source in $workDir")
            jekyllDriver.prepareSource(resolvedSourceDir, workDir)
            jekyllDriver.applyOverrides(resolvedOverrideDir, workDir)
            
            if (!resolvedPoBaseDir.exists()) {
                resolvedPoBaseDir.createDirectories()
            }
            jekyllDriver.extractPo(workDir, resolvedPoBaseDir)
            normalize(resolvedPoBaseDir)
        }
    }

    override fun applyPoToDirectory(workDir: Path, poBaseDir: Path) {
        logger.info("Applying PO files from $poBaseDir to $workDir")
        poBaseDir.walk().filter { it.extension == "po" }.forEach { poFile ->
            val relativePoPath = poBaseDir.relativize(poFile).toString()
            
            if (poService.isIgnored(poFile)) {
                return@forEach
            }

            val relativeMasterPath = relativePoPath.removeSuffix(".po")
            val masterFile = workDir.resolve(relativeMasterPath).normalize()
            
            if (masterFile.exists()) {
                val format = po4aDriver.determineFormat(masterFile)
                
                if (format != null) {
                    logger.info("Applying translation to $masterFile using $poFile (format: $format)")
                    po4aDriver.translate(masterFile, poFile, masterFile, format)
                } else {
                    logger.debug("Skipping $masterFile: unsupported format or explicitly ignored")
                }
            }
        }
    }

    override fun updatePoStats(poDirs: List<Path>?, output: Path?) {
        val resolvedPoDirs = poDirs ?: listOf(Paths.get(tsujiConfig.po.baseDir))
        val resolvedOutput = output ?: Paths.get(tsujiConfig.jekyll.statsDir).resolve("translation.csv")

        logger.info("Updating PO stats for directories: $resolvedPoDirs")
        val results = mutableListOf<PoStatResult>()

        resolvedPoDirs.forEach { poDir ->
            if (!poDir.exists()) {
                logger.warn("PO directory does not exist: $poDir")
                return@forEach
            }

            Files.walk(poDir).use { stream ->
                stream.filter { it.extension == "po" }.forEach { path ->
                    val po = poDriver.load(path)
                    val stats = poService.calculateStats(po)

                    if (stats.totalMessages > 0) {
                        results.add(
                            PoStatResult(
                                filename = if (resolvedPoDirs.size > 1) "$poDir/${poDir.relativize(path)}" else poDir.relativize(path).toString(),
                                fuzzyMessages = stats.fuzzyMessages,
                                totalMessages = stats.totalMessages,
                                fuzzyWords = stats.fuzzyWords,
                                totalWords = stats.totalWords,
                                achievement = "${stats.achievement}%"
                            )
                        )
                    }
                }
            }
        }

        results.sortWith(compareByDescending<PoStatResult> { it.fuzzyWords }.thenBy { it.filename })

        resolvedOutput.parent?.createDirectories()
        resolvedOutput.bufferedWriter().use { writer ->
            writer.write("Filename, Fuzzy Messages, Total Messages, Fuzzy Words, Total Words, Achievement\n")
            results.forEach { stat ->
                writer.write("${stat.filename}, ${stat.fuzzyMessages}, ${stat.totalMessages}, ${stat.fuzzyWords}, ${stat.totalWords}, ${stat.achievement}\n")
            }
        }
        logger.info("PO stats written to: $resolvedOutput")
    }

    private data class PoStatResult(
        val filename: String,
        val fuzzyMessages: Int,
        val totalMessages: Int,
        val fuzzyWords: Int,
        val totalWords: Int,
        val achievement: String
    )

    private fun <T> withTempWorkDir(action: (Path) -> T): T {
        val tempDir = Files.createTempDirectory("tsuji-jekyll-extract")
        try {
            logger.info("Created temporary work directory: $tempDir")
            return action(tempDir)
        } finally {
            logger.info("Cleaning up work directory $tempDir")
            tempDir.toFile().deleteRecursively()
        }
    }
}
