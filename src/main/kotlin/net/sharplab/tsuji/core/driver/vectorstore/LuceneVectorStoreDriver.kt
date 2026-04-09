package net.sharplab.tsuji.core.driver.vectorstore

import dev.langchain4j.community.rag.content.retriever.lucene.LuceneEmbeddingStore
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.Term
import org.apache.lucene.index.TieredMergePolicy
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
    private val directory: FSDirectory
    private val embeddingStore: LuceneEmbeddingStore

    companion object {
        private const val ID_FIELD_NAME = "id"
        private const val CONTENT_FIELD_NAME = "content"
        private const val EMBEDDING_FIELD_NAME = "embedding"
    }

    init {
        val indexPath = Paths.get(ragIndexPath)
        if (!indexPath.toFile().exists()) {
            indexPath.createDirectories()
        }

        directory = FSDirectory.open(indexPath)
        embeddingStore = LuceneEmbeddingStore.builder()
            .directory(directory)
            .build()

        logger.info("Initialized Lucene embedding store at $indexPath")
    }

    override fun addAll(segments: List<TextSegment>) {
        if (segments.isEmpty()) return
        logger.info("Adding/updating ${segments.size} segments in Lucene embedding store")

        val embeddings = embeddingModel.embedAll(segments).content()

        // Generate content-based IDs using SHA-256 hash
        // This ensures the same segment always gets the same ID
        val segmentIds = segments.map { segment ->
            val content = "${segment.text()}:${segment.metadata().getString("target")}:${segment.metadata().getString("lang")}"
            val hash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(content.toByteArray())
                .joinToString("") { "%02x".format(it) }
            hash
        }

        // Use IndexWriter directly to use updateDocuments() instead of addDocuments()
        // This allows us to update existing segments with the same ID (upsert behavior)
        val config = IndexWriterConfig(StandardAnalyzer()).apply {
            // Explicitly set to CREATE_OR_APPEND mode to update existing index
            openMode = OpenMode.CREATE_OR_APPEND

            // RAMバッファを小さくしてセグメント分割（2MBで細かく分割）
            setRAMBufferSizeMB(2.0)

            // ドキュメント数でもフラッシュ（約1000ドキュメントごと）
            setMaxBufferedDocs(1000)

            // TieredMergePolicyでセグメント管理最適化
            val mergePolicy = TieredMergePolicy()
            mergePolicy.setSegmentsPerTier(10.0)
            setMergePolicy(mergePolicy)
        }
        IndexWriter(directory, config).use { writer ->
            var updatedCount = 0
            segments.forEachIndexed { index, segment ->
                val id = segmentIds[index]
                val embedding = embeddings[index]

                // Create Lucene document
                val document = Document().apply {
                    add(StringField(ID_FIELD_NAME, id, Field.Store.YES))
                    add(TextField(CONTENT_FIELD_NAME, segment.text(), Field.Store.YES))

                    // Add vector field for similarity search
                    val vector = embedding.vector()
                    if (vector != null && vector.isNotEmpty()) {
                        add(KnnFloatVectorField(EMBEDDING_FIELD_NAME, vector))
                    }

                    // Add metadata fields
                    segment.metadata().toMap().forEach { (key, value) ->
                        add(StringField(key, value.toString(), Field.Store.YES))
                    }
                }

                // Use updateDocument to delete old document with same ID and add new one (upsert)
                writer.updateDocument(Term(ID_FIELD_NAME, id), document)
                updatedCount++
            }

            logger.info("Updated/inserted $updatedCount documents in Lucene index")
        }

        logger.info("Successfully indexed ${segments.size} segments (upsert)")
    }

    override fun save() {
        // Lucene automatically saves to disk when IndexWriter is closed
        logger.info("Lucene index is automatically persisted to disk")
    }

    override fun asContentRetriever(maxResults: Int, minScore: Double): ContentRetriever {
        // Use EmbeddingStoreContentRetriever instead of LuceneContentRetriever
        // to avoid Lucene query parser errors with HTML tags and special characters
        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults)
            .minScore(minScore)
            .build()
    }

    /**
     * Read existing document IDs from the Lucene index.
     * Returns empty set if index does not exist or cannot be read.
     */
    fun readExistingDocumentIds(): Set<String> {
        val indexPath = Paths.get(ragIndexPath)

        // インデックスが存在しない場合は空セットを返す
        if (!indexPath.toFile().exists()) {
            logger.info("Index directory does not exist, returning empty ID set")
            return emptySet()
        }

        val existingIds = mutableSetOf<String>()

        try {
            DirectoryReader.open(directory).use { reader ->
                val storedFields = reader.storedFields()
                val fieldSet = setOf(ID_FIELD_NAME)

                val numDocs = reader.numDocs()
                logger.info("Reading $numDocs live documents from existing index (maxDoc: ${reader.maxDoc()})")

                // Iterate through all leaf readers to access live docs
                for (leaf in reader.leaves()) {
                    val leafReader = leaf.reader()
                    val leafStoredFields = leafReader.storedFields()
                    val docBase = leaf.docBase
                    val maxDoc = leafReader.maxDoc()

                    for (docId in 0 until maxDoc) {
                        // Skip deleted documents
                        val liveDocs = leafReader.liveDocs
                        if (liveDocs != null && !liveDocs.get(docId)) {
                            continue
                        }

                        try {
                            val document = leafStoredFields.document(docId, fieldSet)
                            document.get(ID_FIELD_NAME)?.let { id ->
                                existingIds.add(id)
                            }
                        } catch (e: Exception) {
                            logger.warn("Failed to read document ${docBase + docId}: ${e.message}")
                        }
                    }
                }

                logger.info("Retrieved ${existingIds.size} document IDs from index")
            }
        } catch (e: org.apache.lucene.index.IndexNotFoundException) {
            // Index not found is expected for initial index creation
            logger.info("Index does not exist yet, will create new index")
            return emptySet()
        } catch (e: Exception) {
            logger.error("Failed to read existing index: ${e.message}", e)
            return emptySet()
        }

        return existingIds
    }

    /**
     * Update index with diff - only add new segments and delete removed ones.
     * Segments are identified by their content-based SHA-256 hash ID.
     */
    override fun updateIndexWithDiff(segmentsWithIds: List<Pair<String, TextSegment>>) {
        // 既存IDを読み取る
        val existingIds = readExistingDocumentIds()
        val newIds = segmentsWithIds.map { it.first }.toSet()

        // 差分計算
        val idsToDelete = existingIds - newIds
        val idsToAdd = newIds - existingIds

        if (idsToDelete.isEmpty() && idsToAdd.isEmpty()) {
            logger.info("No changes detected in segments, index is up-to-date")
            return
        }

        logger.info("Index changes: +${idsToAdd.size} to add, -${idsToDelete.size} to delete")

        // IndexWriter設定（セグメント分割を促進）
        val config = IndexWriterConfig(StandardAnalyzer()).apply {
            openMode = OpenMode.CREATE_OR_APPEND

            // RAMバッファを小さくしてセグメント分割（2MBで細かく分割）
            setRAMBufferSizeMB(2.0)

            // ドキュメント数でもフラッシュ（約1000ドキュメントごと）
            setMaxBufferedDocs(1000)

            // TieredMergePolicyでセグメント管理最適化
            val mergePolicy = TieredMergePolicy()
            mergePolicy.setSegmentsPerTier(10.0)
            setMergePolicy(mergePolicy)
        }

        IndexWriter(directory, config).use { writer ->
            // 削除処理
            if (idsToDelete.isNotEmpty()) {
                logger.info("Deleting ${idsToDelete.size} documents")
                val terms = idsToDelete.map { Term(ID_FIELD_NAME, it) }.toTypedArray()
                writer.deleteDocuments(*terms)
            }

            // 追加処理
            if (idsToAdd.isNotEmpty()) {
                logger.info("Adding ${idsToAdd.size} new documents")

                val segmentsToAdd = segmentsWithIds.filter { it.first in idsToAdd }
                val segments = segmentsToAdd.map { it.second }

                // エンベディングを一括生成
                val embeddings = embeddingModel.embedAll(segments).content()

                // ドキュメント作成と追加
                segmentsToAdd.forEachIndexed { index, (id, segment) ->
                    val document = createDocument(id, embeddings[index], segment)
                    writer.addDocument(document)
                }
            }
        }

        logger.info("Index update completed")
    }

    /**
     * Create a Lucene Document from segment ID, embedding, and text segment.
     */
    private fun createDocument(id: String, embedding: Embedding, segment: TextSegment): Document {
        return Document().apply {
            add(StringField(ID_FIELD_NAME, id, Field.Store.YES))
            add(TextField(CONTENT_FIELD_NAME, segment.text(), Field.Store.YES))

            val vector = embedding.vector()
            if (vector != null && vector.isNotEmpty()) {
                add(KnnFloatVectorField(EMBEDDING_FIELD_NAME, vector))
            }

            segment.metadata().toMap().forEach { (key, value) ->
                add(StringField(key, value.toString(), Field.Store.YES))
            }
        }
    }
}
