package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import net.sharplab.tsuji.test.createPoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TmxServiceImplTest {

    private val target = TmxServiceImpl()

    @Test
    fun createTmxFromPos_confirmedMode_shouldIncludeOnlyConfirmed() {
        // Given
        val msg1 = createPoMessage("Hello", "こんにちは")
        val msg2 = createPoMessage("Fuzzy", "あやふや", fuzzy = true)
        val po = Po("ja_JP", listOf(msg1, msg2))

        // When
        val result = target.createTmxFromPos(listOf(po), TmxGenerationMode.CONFIRMED)

        // Then
        val tus = result.tmxBody.translationUnits ?: emptyList()
        assertThat(tus).hasSize(1)
        assertThat(tus[0].variants.find { it.lang == "en" }?.seg).isEqualTo("Hello")
        assertThat(tus[0].variants.find { it.lang == "ja_JP" }?.seg).isEqualTo("こんにちは")
    }

    @Test
    fun createTmxFromPos_fuzzyMode_shouldIncludeOnlyFuzzy() {
        // Given
        val msg1 = createPoMessage("Hello", "こんにちは")
        val msg2 = createPoMessage("Fuzzy", "あやふや", fuzzy = true)
        val po = Po("ja_JP", listOf(msg1, msg2))

        // When
        val result = target.createTmxFromPos(listOf(po), TmxGenerationMode.FUZZY)

        // Then
        val tus = result.tmxBody.translationUnits ?: emptyList()
        assertThat(tus).hasSize(1)
        assertThat(tus[0].variants.find { it.lang == "en" }?.seg).isEqualTo("Fuzzy")
        assertThat(tus[0].variants.find { it.lang == "ja_JP" }?.seg).isEqualTo("あやふや")
    }

    @Test
    fun createTmxFromPos_shouldDeDuplicateById() {
        // Given
        val msg1 = createPoMessage("Hello", "こんにちは")
        val po1 = Po("ja_JP", listOf(msg1))
        val po2 = Po("ja_JP", listOf(msg1))

        // When
        val result = target.createTmxFromPos(listOf(po1, po2), TmxGenerationMode.CONFIRMED)

        // Then
        val tus = result.tmxBody.translationUnits ?: emptyList()
        assertThat(tus).hasSize(1)
    }
}