package net.sharplab.tsuji.app.service

import dev.langchain4j.data.segment.TextSegment
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.tmx.TmxDriver
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.core.service.IndexingService
import net.sharplab.tsuji.tmx.model.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.nio.file.Path
import java.nio.file.Paths

class IndexingAppServiceImplTest {

    private val tmxDriver: TmxDriver = mock()
    private val vectorStoreDriver: VectorStoreDriver = mock()
    private val indexingService: IndexingService = mock()
    private val tsujiConfig: TsujiConfig = mock()

    private val target = IndexingAppServiceImpl(
        tmxDriver,
        vectorStoreDriver,
        indexingService,
        tsujiConfig
    )

    @Test
    fun indexTmx_should_parse_tmx_and_call_driver() {
        // Given
        val tmxPath = Paths.get("test.tmx")
        val header = TmxHeader("tool", "1.0", "seg", "tmf", "en", "en", "plaintext")
        val variantEn = TranslationUnitVariant("en", "Hello")
        val variantJa = TranslationUnitVariant("ja", "こんにちは")
        val tu = TranslationUnit(listOf(variantEn, variantJa))
        val body = TmxBody(listOf(tu))
        val tmx = Tmx("1.4", header, body)
        val segments = listOf(TextSegment.from("Hello", dev.langchain4j.data.document.Metadata.from("target", "こんにちは")))

        whenever(tmxDriver.load(tmxPath)).thenReturn(tmx)
        whenever(indexingService.convertToSegments(tmx)).thenReturn(segments)

        // When
        target.indexTmx(tmxPath)

        // Then
        verify(vectorStoreDriver).addAll(segments)
        verify(vectorStoreDriver).save(isNull())
    }
}
