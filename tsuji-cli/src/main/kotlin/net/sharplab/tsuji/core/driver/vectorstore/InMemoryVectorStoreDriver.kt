package net.sharplab.tsuji.core.driver.vectorstore

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class InMemoryVectorStoreDriver(
    private val embeddingModel: EmbeddingModel,
    private val ragIndexPath: String
) : VectorStoreDriver {

    private val logger = LoggerFactory.getLogger(InMemoryVectorStoreDriver::class.java)
    private val embeddingStore: InMemoryEmbeddingStore<TextSegment>

    init {
        val indexPath = Paths.get(ragIndexPath).resolve("embeddings.json")
        embeddingStore = if (indexPath.exists()) {
            logger.info("Loading embedding store from $indexPath")
            InMemoryEmbeddingStore.fromFile(indexPath)
        } else {
            InMemoryEmbeddingStore<TextSegment>()
        }
    }

    override fun addAll(segments: List<TextSegment>) {
        if (segments.isEmpty()) return
        logger.info("Adding ${segments.size} segments to embedding store")
        val embeddings = embeddingModel.embedAll(segments).content()
        embeddingStore.addAll(embeddings, segments)
    }

    override fun save(indexDir: Path?) {
        val targetIndexDir = indexDir ?: Paths.get(ragIndexPath)
        val indexPath = targetIndexDir.resolve("embeddings.json")
        
        if (!targetIndexDir.exists()) {
            targetIndexDir.createDirectories()
        }
        
        logger.info("Saving embedding store to $indexPath")
        embeddingStore.serializeToFile(indexPath)
    }

    override fun asContentRetriever(maxResults: Int): ContentRetriever {
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults)
            .build()
    }
}