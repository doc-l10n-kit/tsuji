package net.sharplab.tsuji.po.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PoFlagTest {

    @Test
    fun `parse should create PoFlag from string`() {
        // Given
        val flagValue = "fuzzy"

        // When
        val result = PoFlag.parse(flagValue)

        // Then
        assertThat(result.value).isEqualTo("fuzzy")
    }

    @Test
    fun `parse should handle unknown flags`() {
        // Given
        val flagValue = "custom-flag"

        // When
        val result = PoFlag.parse(flagValue)

        // Then
        assertThat(result.value).isEqualTo("custom-flag")
    }

    @Test
    fun `predefined flags should have correct values`() {
        assertThat(PoFlag.Fuzzy.value).isEqualTo("fuzzy")
        assertThat(PoFlag.NoWrap.value).isEqualTo("no-wrap")
        assertThat(PoFlag.CFormat.value).isEqualTo("c-format")
        assertThat(PoFlag.NoCFormat.value).isEqualTo("no-c-format")
    }

    @Test
    fun `equality check for predefined flags`() {
        // Given
        val flag1 = PoFlag.Fuzzy
        val flag2 = PoFlag.parse("fuzzy")

        // Then
        assertThat(flag1).isEqualTo(flag2)
    }

    @Test
    fun `flags with different values should not be equal`() {
        // Given
        val flag1 = PoFlag.Fuzzy
        val flag2 = PoFlag.NoWrap

        // Then
        assertThat(flag1).isNotEqualTo(flag2)
    }
}
