package net.sharplab.tsuji.tmx.index

import net.sharplab.tsuji.tmx.TmxCodec
import net.sharplab.tsuji.tmx.model.Tmx
import net.sharplab.tsuji.tmx.model.TmxBody
import net.sharplab.tsuji.tmx.model.TmxHeader
import net.sharplab.tsuji.tmx.model.TranslationUnit
import net.sharplab.tsuji.tmx.model.TranslationUnitVariant
import net.sharplab.tsuji.test.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

class TranslationIndexTest{

    @Test
    fun tmx_test(){
        val tmxPath = TestUtil.resolveClasspath("tmx/test.tmx")
        val tmx = TmxCodec().load(tmxPath)

        assertThatCode {
            val index = TranslationIndex.create(tmx, "ja_JP")
        }.doesNotThrowAnyException()

    }

    @Test
    fun fuzzy_tmx_test(){
        val tmxPath = TestUtil.resolveClasspath("tmx/fuzzy.tmx")
        val tmx = TmxCodec().load(tmxPath)

        assertThatCode {
            val index = TranslationIndex.create(tmx, "es_ES")
        }.doesNotThrowAnyException()

    }

    @Test
    fun get_shouldKeepTrailingNewlineWhenKeyEndsWithNewline(){
        val tmx = tmxOf("A haiku for you.\n", "Um haiku para voce.\n")
        val index = TranslationIndex.create(tmx, "pt_BR")

        assertThat(index["A haiku for you.\n"]).isEqualTo("Um haiku para voce.\n")
    }

    @Test
    fun get_shouldNotAppendTrailingNewlineWhenKeyHasNone(){
        val tmx = tmxOf("quarkus update", "quarkus update\n")
        val index = TranslationIndex.create(tmx, "pt_BR")

        assertThat(index["quarkus update"]).isEqualTo("quarkus update")
    }

    @Test
    fun get_shouldKeepTrailingNewlineForMultilineKey(){
        val tmx = tmxOf("first line\nsecond line\n", "primeira linha\nsegunda linha\n")
        val index = TranslationIndex.create(tmx, "pt_BR")

        assertThat(index["first line\nsecond line\n"]).isEqualTo("primeira linha\nsegunda linha\n")
    }

    private fun tmxOf(source: String, target: String): Tmx {
        val header = TmxHeader("tsuji", "1.0.0", "sentence", "UTF-8", "en_US", "en_US", "PlainText")
        val unit = TranslationUnit(
            listOf(
                TranslationUnitVariant("en_US", source),
                TranslationUnitVariant("pt_BR", target)
            )
        )
        return Tmx("1.4", header, TmxBody(listOf(unit)))
    }

}
