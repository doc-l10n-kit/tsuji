package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.tmx.TmxCodec
import net.sharplab.tsuji.tmx.index.TranslationIndex
import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.test.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking

internal class PoTranslatorServiceImplTest {

    // Helper extension to call suspend function from test
    private fun Translator.translateBlocking(
        po: Po, 
        srcLang: String,
        dstLang: String,
        isAsciidoctor: Boolean,
        useRag: Boolean
    ): Po = runBlocking { translate(po, srcLang, dstLang, isAsciidoctor, useRag) }


    private val mockTranslator = object : Translator {
        override suspend fun translate(po: Po, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): Po = po
    }

    @Test
    fun applyFuzzyTmx_shouldApplyTranslationFromTmx() {
        // Arrange
        val poPath = TestUtil.resolveClasspath("po/fuzzy.adoc.po")
        val po = PoDriverImpl().load(poPath)
        val fuzzyTmxPath = TestUtil.resolveClasspath("tmx/fuzzy.tmx")
        val tmx = TmxCodec().load(fuzzyTmxPath)

        val target = PoTranslatorServiceImpl(mockTranslator)

        // Act
        val updatedPo = target.applyFuzzyTmx(tmx, po)

        // Assert
        val message = updatedPo.messages.first { it.messageId == "Sample Guide" }
        assertThat(message.messageString).isEqualTo("サンプルガイド")
        assertThat(message.fuzzy).isTrue()
    }

    @Test
    fun applyTmxWithIndex_shouldApplyTranslationWithFuzzyFalse() {
        // Arrange
        val poPath = TestUtil.resolveClasspath("po/fuzzy.adoc.po")
        val po = PoDriverImpl().load(poPath)
        val tmxPath = TestUtil.resolveClasspath("tmx/fuzzy.tmx")
        val tmx = TmxCodec().load(tmxPath)
        val translationIndex = TranslationIndex.create(tmx, po.target)

        val target = PoTranslatorServiceImpl(mockTranslator)

        // Act
        val updatedPo = target.applyTmxWithIndex(translationIndex, po, fuzzy = false)

        // Assert
        val message = updatedPo.messages.first { it.messageId == "Sample Guide" }
        assertThat(message.messageString).isEqualTo("サンプルガイド")
        assertThat(message.fuzzy).isFalse()
    }

    @Test
    fun applyTmxWithIndex_shouldApplyTranslationWithFuzzyTrue() {
        // Arrange
        val poPath = TestUtil.resolveClasspath("po/fuzzy.adoc.po")
        val po = PoDriverImpl().load(poPath)
        val tmxPath = TestUtil.resolveClasspath("tmx/fuzzy.tmx")
        val tmx = TmxCodec().load(tmxPath)
        val translationIndex = TranslationIndex.create(tmx, po.target)

        val target = PoTranslatorServiceImpl(mockTranslator)

        // Act
        val updatedPo = target.applyTmxWithIndex(translationIndex, po, fuzzy = true)

        // Assert
        val message = updatedPo.messages.first { it.messageId == "Sample Guide" }
        assertThat(message.messageString).isEqualTo("サンプルガイド")
        assertThat(message.fuzzy).isTrue()
    }
}