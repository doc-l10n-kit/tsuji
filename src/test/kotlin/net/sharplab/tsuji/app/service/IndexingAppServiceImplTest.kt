package net.sharplab.tsuji.app.service

import dev.langchain4j.data.segment.TextSegment
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.tmx.TmxDriver
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.core.service.IndexingService
import net.sharplab.tsuji.tmx.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.nio.file.Path
import java.nio.file.Paths

class IndexingAppServiceImplTest {

    private val tmxDriver: TmxDriver = mock()
    private val vectorStoreDriver: VectorStoreDriver = mock()
    private val indexingService: IndexingService = mock()
    private val tsujiConfig: TsujiConfig = mock()

    @Test
    fun indexTmx_should_parse_tmx_and_call_driver(@TempDir tempDir: Path) {
        // Given
        val ragConfig: TsujiConfig.Rag = mock()
        whenever(tsujiConfig.rag).thenReturn(ragConfig)
        whenever(ragConfig.indexPath).thenReturn(tempDir.resolve("index").toString())

        val target = IndexingAppServiceImpl(
            tmxDriver,
            vectorStoreDriver,
            indexingService,
            tsujiConfig
        )

        val tmxPath = Paths.get("test.tmx")
        val header = TmxHeader("tool", "1.0", "seg", "tmf", "en", "en", "plaintext")
        val variantEn = TranslationUnitVariant("en", "Hello")
        val variantJa = TranslationUnitVariant("ja", "こんにちは")
        val tu = TranslationUnit(listOf(variantEn, variantJa))
        val body = TmxBody(listOf(tu))
        val tmx = Tmx("1.4", header, body)
        val segment = TextSegment.from("Hello", dev.langchain4j.data.document.Metadata.from(mapOf("target" to "こんにちは", "lang" to "ja")))
        val segments = listOf(segment)

        whenever(tmxDriver.load(tmxPath)).thenReturn(tmx)
        whenever(indexingService.convertToSegments(tmx)).thenReturn(segments)

        // When
        target.indexTmx(tmxPath)

        // Then
        verify(vectorStoreDriver).updateIndexWithDiff(any())
        verify(vectorStoreDriver).save()
    }
}
