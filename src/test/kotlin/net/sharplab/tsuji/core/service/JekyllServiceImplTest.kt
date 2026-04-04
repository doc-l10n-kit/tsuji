package net.sharplab.tsuji.core.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JekyllServiceImplTest {

    private val target = JekyllServiceImpl()

    @Test
    fun determineOverrideStatus_shouldReturnOkWhenOverrideIsNewer() {
        // Given
        val overrideEpoch = 1000L
        val upstreamEpoch = 900L

        // When
        val result = target.determineOverrideStatus(overrideEpoch, upstreamEpoch)

        // Then
        assertThat(result).isEqualTo("OK")
    }

    @Test
    fun determineOverrideStatus_shouldReturnOkWhenTimestampsAreEqual() {
        // Given
        val overrideEpoch = 1000L
        val upstreamEpoch = 1000L

        // When
        val result = target.determineOverrideStatus(overrideEpoch, upstreamEpoch)

        // Then
        assertThat(result).isEqualTo("OK")
    }

    @Test
    fun determineOverrideStatus_shouldReturnNgWhenOverrideIsOlder() {
        // Given
        val overrideEpoch = 900L
        val upstreamEpoch = 1000L

        // When
        val result = target.determineOverrideStatus(overrideEpoch, upstreamEpoch)

        // Then
        assertThat(result).isEqualTo("NG")
    }
}
