package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.core.service.PoTranslatorService
import net.sharplab.tsuji.test.TestUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking

class TranslationAppServiceImplTest {

    private lateinit var target: TranslationAppServiceImpl
    private val poTranslatorService: PoTranslatorService = mock()
    private val poDriver: PoDriver = mock()
    private val gettextDriver: GettextDriver = mock()
    private val poNormalizerService: net.sharplab.tsuji.core.service.PoNormalizerService = mock()
    private val tsujiConfig: TsujiConfig = mock()

    @BeforeEach
    fun setup() {
        val language: TsujiConfig.Language = mock()
        whenever(language.from).thenReturn("en")
        whenever(language.to).thenReturn("ja")

        val translator: TsujiConfig.Translator = mock()
        whenever(translator.targetDirectories).thenReturn(java.util.Optional.empty())

        whenever(tsujiConfig.language).thenReturn(language)
        whenever(tsujiConfig.translator).thenReturn(translator)

        target = TranslationAppServiceImpl(poTranslatorService, poDriver, gettextDriver, poNormalizerService, tsujiConfig)
    }

    @Test
    fun machineTranslatePoFiles_test() = runBlocking {
        // Given
        val poPath = TestUtil.resolveClasspath("po/sample.adoc.po")
        val dummyPo = PoDriverImpl().load(poPath)

        whenever(poDriver.load(poPath)).thenReturn(dummyPo)
        whenever(poTranslatorService.translate(dummyPo, "en", "ja", true, true)).thenReturn(dummyPo)

        // When
        target.machineTranslatePoFiles(listOf(poPath), "en", "ja", AsciidocMode.ALWAYS, true)

        // Then
        verify(poDriver).load(poPath)
        verify(poTranslatorService).translate(dummyPo, "en", "ja", true, true)
        verify(poDriver).save(dummyPo, poPath)
        verify(poNormalizerService).normalize(poPath)
    }

    @Test
    fun `machineTranslatePoFiles auto-detects asciidoc mode for adoc files`() {
        runBlocking {
            // Given
            val poPath = TestUtil.resolveClasspath("po/sample.adoc.po")
            val dummyPo = PoDriverImpl().load(poPath)

            whenever(poDriver.load(poPath)).thenReturn(dummyPo)
            whenever(poTranslatorService.translate(dummyPo, "en", "ja", true, true)).thenReturn(dummyPo)

            // When: AUTO mode with .adoc.po -> true
            target.machineTranslatePoFiles(listOf(poPath), "en", "ja", AsciidocMode.AUTO, true)

            // Then
            verify(poTranslatorService).translate(dummyPo, "en", "ja", true, true)
        }
    }

    @Test
    fun `machineTranslatePoFiles auto-detects non-asciidoc mode for html files`() {
        runBlocking {
            // Given: create a temporary .html.po file from sample
            val samplePath = TestUtil.resolveClasspath("po/sample.adoc.po")
            val tempDir = java.nio.file.Files.createTempDirectory("tsuji-test")
            val htmlPoPath = tempDir.resolve("sample.html.po")
            java.nio.file.Files.copy(samplePath, htmlPoPath)

            val dummyPo = PoDriverImpl().load(htmlPoPath)

            whenever(poDriver.load(htmlPoPath)).thenReturn(dummyPo)
            whenever(poTranslatorService.translate(dummyPo, "en", "ja", false, true)).thenReturn(dummyPo)

            // When: AUTO mode with .html.po -> false
            target.machineTranslatePoFiles(listOf(htmlPoPath), "en", "ja", AsciidocMode.AUTO, true)

            // Then
            verify(poTranslatorService).translate(dummyPo, "en", "ja", false, true)

            // Cleanup
            java.nio.file.Files.deleteIfExists(htmlPoPath)
            java.nio.file.Files.deleteIfExists(tempDir)
        }
    }
}
