package net.sharplab.tsuji.tmx

import net.sharplab.tsuji.test.TestUtil
import net.sharplab.tsuji.tmx.index.TranslationIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TmxBoundaryTest {

    private val codec = TmxCodec()

    @Test
    fun load_empty_tmx_test() {
        val path = TestUtil.resolveClasspath("tmx/empty.tmx")
        val tmx = codec.load(path)
        
        assertThat(tmx.tmxBody.translationUnits).isNullOrEmpty()
        
        val index = TranslationIndex.create(tmx, "ja")
        assertThat(index["any"]).isNull()
    }

    @Test
    fun load_multi_lang_tmx_test() {
        val path = TestUtil.resolveClasspath("tmx/multi-lang.tmx")
        val tmx = codec.load(path)
        
        assertThat(tmx.tmxBody.translationUnits).hasSize(1)
        assertThat(tmx.tmxBody.translationUnits!![0].variants).hasSize(3)
        
        // search with ja
        val indexJa = TranslationIndex.create(tmx, "ja")
        assertThat(indexJa["Hello"]).isEqualTo("こんにちは")
        
        // search with fr
        val indexFr = TranslationIndex.create(tmx, "fr")
        assertThat(indexFr["Hello"]).isEqualTo("Bonjour")
    }

    @Test
    fun load_missing_source_tmx_test() {
        val path = TestUtil.resolveClasspath("tmx/missing-source.tmx")
        val tmx = codec.load(path)
        
        assertThat(tmx.tmxBody.translationUnits).hasSize(1)
        
        // Since source(en) is missing, nothing should be registered in the index
        val index = TranslationIndex.create(tmx, "ja")
        assertThat(index.get("ソース言語(en)がないエントリ")).isNull()
    }
}
