package net.sharplab.tsuji.core.driver.vectorstore

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import net.sharplab.tsuji.app.config.TsujiConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.nio.file.Path
import kotlin.io.path.exists

@QuarkusTest
class InMemoryVectorStoreDriverTest {

    @Inject
    lateinit var embeddingModel: EmbeddingModel

    @Inject
    lateinit var tsujiConfig: TsujiConfig

    private lateinit var target: InMemoryVectorStoreDriver

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        target = InMemoryVectorStoreDriver(embeddingModel, tempDir.toString())
    }

    @Test
    fun addAll_and_save_test(@TempDir tempDir: Path) {
        // Given
        val segment = TextSegment.from("test text")
        val savePath = tempDir.resolve("output")

        // When
        target.addAll(listOf(segment))
        target.save(savePath)

        // Then
        assertThat(savePath.resolve("embeddings.json")).exists()
    }

    @Test
    fun asContentRetriever_test() {
        // When
        val retriever = target.asContentRetriever(maxResults = 5)

        // Then
        assertThat(retriever).isNotNull
    }
}
