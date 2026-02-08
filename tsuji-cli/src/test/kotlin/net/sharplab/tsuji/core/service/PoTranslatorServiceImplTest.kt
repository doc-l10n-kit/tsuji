package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.tmx.TmxCodec
import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.processor.AsciidoctorMessageProcessor
import net.sharplab.tsuji.test.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class PoTranslatorServiceImplTest {

    @Test
    fun applyFuzzyTmx_shouldApplyTranslationFromTmx() {
        // Arrange
        val poPath = TestUtil.resolveClasspath("po/fuzzy.adoc.po")
        val po = PoDriverImpl().load(poPath)
        val fuzzyTmxPath = TestUtil.resolveClasspath("tmx/fuzzy.tmx")
        val tmx = TmxCodec().load(fuzzyTmxPath)

        val processor = mock<AsciidoctorMessageProcessor>()
        whenever(processor.preProcess(any())).thenAnswer { it.arguments[0] }
        whenever(processor.postProcess(any())).thenAnswer { it.arguments[0] }

        val target = PoTranslatorServiceImpl(
            object : Translator {
                override fun translate(texts: List<String>, srcLang: String, dstLang: String, useRag: Boolean): List<String> = texts
            },
            processor,
            MessageTranslationServiceImpl()
        )

        // Act
        val updatedPo = target.applyFuzzyTmx(tmx, po)

        // Assert
        val message = updatedPo.messages.first { it.messageId == "Sample Guide" }
        assertThat(message.messageString).isEqualTo("サンプルガイド")
        assertThat(message.fuzzy).isTrue()
    }
}