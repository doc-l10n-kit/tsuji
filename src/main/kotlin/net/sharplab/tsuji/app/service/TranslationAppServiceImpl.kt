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
        isAsciidoctor: Boolean,
        useRag: Boolean,
        configPath: Path?
    ) {
        runBlocking {
            val targetDirectories = tsujiConfig.translation.targetDirectories

            if (targetDirectories.isPresent && targetDirectories.get().isNotEmpty()) {
                // Configuration-based processing: only process directories listed in config
                val baseDir = Paths.get(tsujiConfig.po.baseDir)
                val dirs = targetDirectories.get()

                logger.info("Processing with configured target directories")
                logger.info("Target directories: ${dirs.joinToString(", ")}")

                dirs.forEach { targetDir ->
                    val dirPath = baseDir.resolve(targetDir)
                    if (dirPath.exists()) {
                        translateRecursiveAsync(dirPath, source, target, isAsciidoctor, useRag)
                    } else {
                        logger.warn("Target directory does not exist, skipping: ${dirPath.absolutePathString()}")
                    }
                }
            } else {
                // Legacy behavior: process all specified paths or default base directory
                val resolvedPaths = filePaths ?: listOf(Paths.get(tsujiConfig.po.baseDir))
                resolvedPaths.forEach { filePath ->
                    translateRecursiveAsync(filePath, source, target, isAsciidoctor, useRag)
                }
            }
        }
    }

    private suspend fun translateRecursiveAsync(
        filePath: Path,
        source: String?,
        target: String?,
        isAsciidoctor: Boolean,
        useRag: Boolean
    ) {
        if (filePath.isDirectory()) {
            logger.info("Start translation for directory: ${filePath.absolutePathString()}")

            val poFiles = Files.walk(filePath).use { stream ->
                stream.filter { it.isRegularFile() && it.extension == "po" }
                    .toList()
            }

            // Parallel file processing with Flow (file-level parallelism)
            poFiles.asFlow()
                .flatMapMerge(concurrency = 10) { file ->
                    flow {
                        emit(translateSingleFile(file, source, target, isAsciidoctor, useRag))
                    }
                }
                .collect()

            logger.info("Finish translation for directory: ${filePath.absolutePathString()}")
            return
        }

        // Single file
        translateSingleFile(filePath, source, target, isAsciidoctor, useRag)
    }

    private fun translateSingleFile(
        filePath: Path,
        source: String?,
        target: String?,
        isAsciidoctor: Boolean,
        useRag: Boolean
    ) {
        val resolvedSourceLang = source ?: tsujiConfig.language.from
        val resolvedTargetLang = target ?: tsujiConfig.language.to

        logger.info("Start translation: %s (%s -> %s)".format(filePath.absolutePathString(), resolvedSourceLang, resolvedTargetLang))
        val poFile = poDriver.load(filePath)
        val translated = poTranslatorService.translate(poFile, resolvedSourceLang, resolvedTargetLang, isAsciidoctor, useRag)
        poDriver.save(translated, filePath)
        poNormalizerService.normalize(filePath)
        logger.info("Finish translation: %s".format(filePath.absolutePathString()))
    }
}
