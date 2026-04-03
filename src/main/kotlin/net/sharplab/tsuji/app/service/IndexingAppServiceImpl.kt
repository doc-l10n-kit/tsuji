package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.tmx.TmxDriver
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.core.service.IndexingService
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.*

@jakarta.enterprise.context.ApplicationScoped
class IndexingAppServiceImpl(
    private val tmxDriver: TmxDriver,
    private val vectorStoreDriver: VectorStoreDriver,
    private val indexingService: IndexingService,
    private val tsujiConfig: TsujiConfig
) : IndexingAppService {

    private val logger = LoggerFactory.getLogger(IndexingAppServiceImpl::class.java)

    override fun indexTmx(tmxPath: Path, indexDir: Path?) {
        logger.info("Start indexing TMX: ${tmxPath.absolutePathString()}")
        
        val tmx = tmxDriver.load(tmxPath)
        val segments = indexingService.convertToSegments(tmx)

        vectorStoreDriver.addAll(segments)
        vectorStoreDriver.save(indexDir)

        logger.info("Finish indexing TMX.")
    }
}
