package net.sharplab.tsuji.app.service

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.service.PoTranslatorService
import net.sharplab.tsuji.app.config.TsujiConfig
import org.slf4j.LoggerFactory
import java.nio.file.Path
import jakarta.enterprise.context.Dependent
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.*

import jakarta.enterprise.context.control.ActivateRequestContext

@Dependent
class TranslationAppServiceImpl(
    private val poTranslatorService: PoTranslatorService,
    private val poDriver: PoDriver,
    private val gettextDriver: GettextDriver,
    private val poNormalizerService: net.sharplab.tsuji.core.service.PoNormalizerService,
    private val tsujiConfig: TsujiConfig
) : TranslationAppService {

    private val logger = LoggerFactory.getLogger(TranslationAppServiceImpl::class.java)

    @ActivateRequestContext
    override fun machineTranslatePoFiles(
        filePaths: List<Path>?,
        source: String?,
        target: String?,
        asciidocMode: AsciidocMode,
        useRag: Boolean,
        configPath: Path?
    ) {
        runBlocking {
            if (filePaths != null && filePaths.isNotEmpty()) {
                // Explicit file paths provided via --po option - process only these files/directories
                logger.info("Processing explicitly specified files/directories")
                filePaths.forEach { filePath ->
                    translateRecursiveAsync(filePath, source, target, asciidocMode, useRag)
                }
            } else {
                val targetDirectories = tsujiConfig.translator.targetDirectories

                if (targetDirectories.isPresent && targetDirectories.get().isNotEmpty()) {
                    // Configuration-based processing: only process directories listed in config
                    val baseDir = Paths.get(tsujiConfig.po.baseDir)
                    val dirs = targetDirectories.get()

                    logger.info("Processing with configured target directories")
                    logger.info("Target directories: ${dirs.joinToString(", ")}")

                    dirs.forEach { targetDir ->
                        val dirPath = baseDir.resolve(targetDir)
                        if (dirPath.exists()) {
                            translateRecursiveAsync(dirPath, source, target, asciidocMode, useRag)
                        } else {
                            logger.warn("Target directory does not exist, skipping: ${dirPath.absolutePathString()}")
                        }
                    }
                } else {
                    // Default behavior: process base directory
                    logger.info("Processing default base directory")
                    val baseDir = Paths.get(tsujiConfig.po.baseDir)
                    translateRecursiveAsync(baseDir, source, target, asciidocMode, useRag)
                }
            }
        }
    }

    private suspend fun translateRecursiveAsync(
        filePath: Path,
        source: String?,
        target: String?,
        asciidocMode: AsciidocMode,
        useRag: Boolean
    ) {
        if (filePath.isDirectory()) {
            logger.info("Start translation for directory: ${filePath.absolutePathString()}")

            val poFiles = Files.walk(filePath).use { stream ->
                stream.filter { it.isRegularFile() && it.extension == "po" }
                    .toList()
            }

            // Parallel file processing with Flow (file-level parallelism)
            // Increased concurrency to maximize throughput for slower translation APIs (e.g., Gemini)
            poFiles.asFlow()
                .flatMapMerge(concurrency = 30) { file ->
                    flow {
                        emit(translateSingleFile(file, source, target, asciidocMode, useRag))
                    }
                }
                .collect()

            logger.info("Finish translation for directory: ${filePath.absolutePathString()}")
            return
        }

        // Single file
        translateSingleFile(filePath, source, target, asciidocMode, useRag)
    }

    private suspend fun translateSingleFile(
        filePath: Path,
        source: String?,
        target: String?,
        asciidocMode: AsciidocMode,
        useRag: Boolean
    ) {
        val isAsciidoctor = when (asciidocMode) {
            AsciidocMode.ALWAYS -> true
            AsciidocMode.NEVER -> false
            AsciidocMode.AUTO -> filePath.fileName.toString().endsWith(".adoc.po")
        }

        val resolvedSourceLang = source ?: tsujiConfig.language.from
        val resolvedTargetLang = target ?: tsujiConfig.language.to

        val startTime = System.currentTimeMillis()
        logger.info("Start translation: %s (%s -> %s, asciidoc=%s) at ${startTime}ms".format(filePath.fileName, resolvedSourceLang, resolvedTargetLang, isAsciidoctor))
        val poFile = poDriver.load(filePath)
        logger.debug("Loaded PO file: ${poFile.messages.size} messages")
        val translated = poTranslatorService.translate(poFile, resolvedSourceLang, resolvedTargetLang, isAsciidoctor, useRag)
        poDriver.save(translated, filePath)
        poNormalizerService.normalize(filePath)
        val elapsed = System.currentTimeMillis() - startTime
        logger.info("Finish translation: %s in ${elapsed}ms".format(filePath.fileName))
    }
}
