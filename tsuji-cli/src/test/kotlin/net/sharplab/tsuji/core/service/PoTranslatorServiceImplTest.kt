package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.tmx.TmxCodec
import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.test.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PoTranslatorServiceImplTest {

    @Test
    fun applyFuzzyTmx_shouldApplyTranslationFromTmx() {
        // Arrange
        val poPath = TestUtil.resolveClasspath("po/fuzzy.adoc.po")
        val po = PoDriverImpl().load(poPath)
        val fuzzyTmxPath = TestUtil.resolveClasspath("tmx/fuzzy.tmx")
        val tmx = TmxCodec().load(fuzzyTmxPath)

        val target = PoTranslatorServiceImpl(
            object : Translator {
                override fun translate(po: Po, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): Po = po
            }
        )

        // Act
        val updatedPo = target.applyFuzzyTmx(tmx, po)

        // Assert
        val message = updatedPo.messages.first { it.messageId == "Sample Guide" }
        assertThat(message.messageString).isEqualTo("サンプルガイド")
        assertThat(message.fuzzy).isTrue()
    }
}