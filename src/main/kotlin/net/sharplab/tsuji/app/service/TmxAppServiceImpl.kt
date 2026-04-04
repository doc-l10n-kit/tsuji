package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.exception.TsujiAppException
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.tmx.TmxDriver
import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import net.sharplab.tsuji.core.service.PoTranslatorService
import net.sharplab.tsuji.core.service.TmxService
import net.sharplab.tsuji.tmx.model.*
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import jakarta.enterprise.context.Dependent
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

@Dependent
class TmxAppServiceImpl(
    private val poTranslatorService: PoTranslatorService,
    private val poDriver: PoDriver,
    private val tmxDriver: TmxDriver,
    private val tmxService: TmxService,
    private val gettextDriver: GettextDriver,
    private val poNormalizerService: net.sharplab.tsuji.core.service.PoNormalizerService,
    private val tsujiConfig: TsujiConfig
) : TmxAppService {

    private val logger = LoggerFactory.getLogger(TmxAppServiceImpl::class.java)

    override fun applyConfirmedTmx(confirmedTmx: Path, po: Path?) {
        applyTmx(confirmedTmx, po, fuzzy = false, logPrefix = "confirmed")
    }

    override fun applyFuzzyTmx(fuzzyTmx: Path, po: Path?) {
        applyTmx(fuzzyTmx, po, fuzzy = true, logPrefix = "fuzzy")
    }

    /**
     * Common logic for applying TMX to PO files.
     */
    private fun applyTmx(tmxPath: Path, po: Path?, fuzzy: Boolean, logPrefix: String) {
        val resolvedPo = po ?: Paths.get(tsujiConfig.po.baseDir)

        logger.info("Loading $logPrefix TMX file: ${tmxPath.absolutePathString()}")
        val tmxFile = tmxDriver.load(tmxPath)

        fun doApplyTmx(poPath: Path) {
            try {
                logger.info("Start applying $logPrefix TMX: ${poPath.absolutePathString()}")
                val poFile = poDriver.load(poPath)

                // Build translation index using the PO file's target language
                logger.info("Building translation index from $logPrefix TMX for language: ${poFile.target}")
                val translationIndex = net.sharplab.tsuji.tmx.index.TranslationIndex.create(
                    tmxFile,
                    poFile.target
                )

                val translated = poTranslatorService.applyTmxWithIndex(translationIndex, poFile, fuzzy = fuzzy)
                poDriver.save(translated, poPath)
                poNormalizerService.normalize(poPath)
                logger.info("Finish applying $logPrefix TMX: ${poPath.absolutePathString()}")
            } catch (e: RuntimeException) {
                throw TsujiAppException("Failed applying $logPrefix TMX: ${poPath.absolutePathString()}", e)
            }
        }

        Files.walk(resolvedPo).use { stream ->
            stream.filter { it.extension == "po" }
                .filter { !it.isDirectory() }
                .parallel()
                .forEach(::doApplyTmx)
        }
    }

    override fun generateTmx(poDir: Path?, tmxPath: Path, mode: TmxGenerationMode) {
        val resolvedPoDir = poDir ?: Paths.get(tsujiConfig.po.baseDir)
        logger.info("Generating TMX from PO files in: ${resolvedPoDir.absolutePathString()}")

        val pos = Files.walk(resolvedPoDir).use { stream ->
            stream.filter { it.extension == "po" }
                .filter { !it.isDirectory() }
                .map { path -> poDriver.load(path) }
                .toList()
        }

        val tmx = tmxService.createTmxFromPos(pos, mode)

        tmxDriver.save(tmx, tmxPath)
        logger.info("TMX generated: ${tmxPath.absolutePathString()}")
    }
}
