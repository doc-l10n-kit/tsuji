package net.sharplab.tsuji.app.service

import dev.langchain4j.data.segment.TextSegment
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.tmx.TmxDriver
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.core.service.IndexingService
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

@jakarta.enterprise.context.ApplicationScoped
class IndexingAppServiceImpl(
    private val tmxDriver: TmxDriver,
    private val vectorStoreDriver: VectorStoreDriver,
    private val indexingService: IndexingService,
    private val tsujiConfig: TsujiConfig
) : IndexingAppService {

    private val logger = LoggerFactory.getLogger(IndexingAppServiceImpl::class.java)

    override fun indexTmx(tmxPath: Path) {
        logger.info("Start indexing TMX: ${tmxPath.absolutePathString()}")

        // 1. TMXファイル読み込み
        val tmx = tmxDriver.load(tmxPath)
        val segments = indexingService.convertToSegments(tmx)

        // 2. セグメントIDを生成
        val segmentsWithIds = segments.map { segment ->
            val id = generateSegmentId(segment)
            id to segment
        }

        // 3. 差分更新を実行
        val ragIndexPath = Path.of(tsujiConfig.rag.indexPath)
        if (!ragIndexPath.exists()) {
            ragIndexPath.createDirectories()
        }

        vectorStoreDriver.updateIndexWithDiff(segmentsWithIds)
        vectorStoreDriver.save()

        logger.info("Finish indexing TMX: ${segments.size} segments processed")
    }

    private fun generateSegmentId(segment: TextSegment): String {
        val content = "${segment.text()}:${segment.metadata().getString("target")}:${segment.metadata().getString("lang")}"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return hash
    }
}
