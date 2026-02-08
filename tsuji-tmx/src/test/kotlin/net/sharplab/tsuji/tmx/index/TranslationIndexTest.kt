package net.sharplab.tsuji.tmx.index

import net.sharplab.tsuji.tmx.TmxCodec
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

}