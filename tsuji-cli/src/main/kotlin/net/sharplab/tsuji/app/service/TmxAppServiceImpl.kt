package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.exception.TsujiAppException
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.tmx.TmxDriver
import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import net.sharplab.tsuji.core.service.PoTranslatorService
import net.sharplab.tsuji.core.service.TmxService
import net.sharplab.tsuji.tmx.model.*
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import jakarta.enterprise.context.Dependent
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

@Dependent
class TmxAppServiceImpl(
    private val poTranslatorService: PoTranslatorService,
    private val poDriver: PoDriver,
    private val tmxDriver: TmxDriver,
    private val tmxService: TmxService
) : TmxAppService {

    private val logger = LoggerFactory.getLogger(TmxAppServiceImpl::class.java)

    override fun applyConfirmedTmx(confirmedTmx: Path, po: Path) {
        fun doApplyTmx(poPath: Path){
            try{
                logger.info("Start applying TMX: ${poPath.absolutePathString()}")
                val confirmedTmxFile = tmxDriver.load(confirmedTmx)
                val poFile = poDriver.load(poPath)
                val translated = poTranslatorService.applyTmx(confirmedTmxFile, poFile)
                poDriver.save(translated, poPath)
                logger.info("Finish applying TMX: ${poPath.absolutePathString()}")
            }
            catch(e: RuntimeException){
                throw TsujiAppException("Failed applying TMX: ${poPath.absolutePathString()}", e)
            }
        }

        Files.walk(po)
                .filter { it.extension == "po" }
                .filter{ !it.isDirectory() }
                .parallel()
                .forEach(::doApplyTmx)
    }

    override fun applyFuzzyTmx(fuzzyTmx: Path, po: Path) {
        fun doApplyTmx(poPath: Path){
            logger.info("Start applying fuzzy TMX: ${poPath.absolutePathString()}")
            val fuzzyTmxFile = tmxDriver.load(fuzzyTmx)
            val poFile = poDriver.load(poPath)
            val translated = poTranslatorService.applyFuzzyTmx(fuzzyTmxFile, poFile)
            poDriver.save(translated, poPath)
            logger.info("Finish applying fuzzy TMX: ${poPath.absolutePathString()}")
        }

        Files.walk(po)
                .filter { it.extension == "po" }
                .filter{ !it.isDirectory() }
                .forEach(::doApplyTmx)
    }

    override fun generateTmx(poDir: Path, tmxPath: Path, mode: TmxGenerationMode) {
        logger.info("Generating TMX from PO files in: ${poDir.absolutePathString()}")

        val pos = Files.walk(poDir)
            .filter { it.extension == "po" }
            .filter { !it.isDirectory() }
            .map { path -> poDriver.load(path) }
            .toList()

        val tmx = tmxService.createTmxFromPos(pos, mode)

        tmxDriver.save(tmx, tmxPath)
        logger.info("TMX generated: ${tmxPath.absolutePathString()}")
    }
}
