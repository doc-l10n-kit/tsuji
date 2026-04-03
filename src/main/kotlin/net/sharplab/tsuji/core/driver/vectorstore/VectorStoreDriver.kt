package net.sharplab.tsuji.core.driver.vectorstore

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.content.retriever.ContentRetriever
import java.nio.file.Path

interface VectorStoreDriver {
    fun addAll(segments: List<TextSegment>)
    fun save(indexDir: Path? = null)
    fun asContentRetriever(maxResults: Int = 3): ContentRetriever
}
