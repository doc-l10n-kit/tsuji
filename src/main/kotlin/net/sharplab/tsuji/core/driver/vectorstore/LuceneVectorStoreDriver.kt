package net.sharplab.tsuji.core.driver.vectorstore

import dev.langchain4j.community.rag.content.retriever.lucene.LuceneContentRetriever
import dev.langchain4j.community.rag.content.retriever.lucene.LuceneEmbeddingStore
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.content.retriever.ContentRetriever
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class LuceneVectorStoreDriver(
    private val embeddingModel: EmbeddingModel,
    private val ragIndexPath: String
) : VectorStoreDriver {

    private val logger = LoggerFactory.getLogger(LuceneVectorStoreDriver::class.java)
    private val embeddingStore: LuceneEmbeddingStore

    init {
        val indexPath = Paths.get(ragIndexPath)
        if (!indexPath.toFile().exists()) {
            indexPath.createDirectories()
        }

        val directory = FSDirectory.open(indexPath)
        embeddingStore = LuceneEmbeddingStore.builder()
            .directory(directory)
            .build()

        logger.info("Initialized Lucene embedding store at $indexPath")
    }

    override fun addAll(segments: List<TextSegment>) {
        if (segments.isEmpty()) return
        logger.info("Adding ${segments.size} segments to Lucene embedding store")
        val embeddings = embeddingModel.embedAll(segments).content()
        embeddingStore.addAll(
            segments.indices.map { "segment-$it" },
            embeddings,
            segments
        )
    }

    override fun save(indexDir: Path?) {
        // Lucene automatically saves to disk, so this is a no-op
        logger.info("Lucene index is automatically persisted to disk")
    }

    override fun asContentRetriever(maxResults: Int): ContentRetriever {
        val indexPath = Paths.get(ragIndexPath)
        val directory = FSDirectory.open(indexPath)

        return LuceneContentRetriever.builder()
            .directory(directory)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults)
            .minScore(0.5)
            .build()
    }
}
