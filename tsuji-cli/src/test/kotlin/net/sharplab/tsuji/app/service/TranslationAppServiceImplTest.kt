package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.core.service.PoTranslatorService
import net.sharplab.tsuji.test.TestUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TranslationAppServiceImplTest {

    private lateinit var target: TranslationAppServiceImpl
    private val poTranslatorService: PoTranslatorService = mock()
    private val poDriver: PoDriver = mock()
    private val tsujiConfig: TsujiConfig = mock()

    @BeforeEach
    fun setup() {
        val language: TsujiConfig.Translator.Language = mock()
        whenever(language.source).thenReturn("en")
        whenever(language.destination).thenReturn("ja")

        val translator: TsujiConfig.Translator = mock()
        whenever(translator.language).thenReturn(language)

        whenever(tsujiConfig.translator).thenReturn(translator)

        target = TranslationAppServiceImpl(poTranslatorService, poDriver, tsujiConfig)
    }

    @Test
    fun machineTranslatePoFiles_test() {
        // Given
        val poPath = TestUtil.resolveClasspath("po/sample.adoc.po")
        val dummyPo = PoDriverImpl().load(poPath)
        
        whenever(poDriver.load(poPath)).thenReturn(dummyPo)
        whenever(poTranslatorService.translate(dummyPo, "en", "ja", true, true)).thenReturn(dummyPo)

        // When
        target.machineTranslatePoFiles(listOf(poPath), "en", "ja", true, true)

        // Then
        verify(poDriver).load(poPath)
        verify(poTranslatorService).translate(dummyPo, "en", "ja", true, true)
        verify(poDriver).save(dummyPo, poPath)
    }
}
