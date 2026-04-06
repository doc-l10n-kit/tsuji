package net.sharplab.tsuji.core.driver.translator.adaptive

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdaptiveBatchControllerTest {

    @Test
    fun `should start with initial batch size`() {
        val controller = AdaptiveBatchController(
            initialSize = 10,
            minSize = 1,
            maxSize = 20
        )

        assertEquals(10, controller.getCurrentBatchSize())
    }

    @Test
    fun `should halve batch size on validation error`() {
        val controller = AdaptiveBatchController(
            initialSize = 10,
            minSize = 1,
            maxSize = 20
        )

        assertEquals(10, controller.getCurrentBatchSize())

        // First validation error: 10 -> 5
        val newSize1 = controller.onValidationError()
        assertEquals(5, newSize1)
        assertEquals(5, controller.getCurrentBatchSize())

        // Second validation error: 5 -> 2
        val newSize2 = controller.onValidationError()
        assertEquals(2, newSize2)
        assertEquals(2, controller.getCurrentBatchSize())

        // Third validation error: 2 -> 1
        val newSize3 = controller.onValidationError()
        assertEquals(1, newSize3)
        assertEquals(1, controller.getCurrentBatchSize())
    }

    @Test
    fun `should not decrease batch size below minimum`() {
        val controller = AdaptiveBatchController(
            initialSize = 2,
            minSize = 1,
            maxSize = 20
        )

        // 2 -> 1
        controller.onValidationError()
        assertEquals(1, controller.getCurrentBatchSize())

        // Should stay at 1
        val newSize = controller.onValidationError()
        assertEquals(1, newSize)
        assertEquals(1, controller.getCurrentBatchSize())
    }

    @Test
    fun `should increase batch size after consecutive successes`() {
        val controller = AdaptiveBatchController(
            initialSize = 5,
            minSize = 1,
            maxSize = 20
        )

        assertEquals(5, controller.getCurrentBatchSize())

        // 4 successes - not enough
        repeat(4) {
            controller.onBatchSuccess()
        }
        assertEquals(5, controller.getCurrentBatchSize())

        // 5th success - should increase
        controller.onBatchSuccess()
        assertEquals(6, controller.getCurrentBatchSize())

        // Another 5 successes
        repeat(5) {
            controller.onBatchSuccess()
        }
        assertEquals(7, controller.getCurrentBatchSize())
    }

    @Test
    fun `should not increase batch size above maximum`() {
        val controller = AdaptiveBatchController(
            initialSize = 10,
            minSize = 1,
            maxSize = 10
        )

        assertEquals(10, controller.getCurrentBatchSize())

        // Try to increase
        repeat(5) {
            controller.onBatchSuccess()
        }

        // Should stay at maximum
        assertEquals(10, controller.getCurrentBatchSize())
    }

    @Test
    fun `should reset success counter on validation error`() {
        val controller = AdaptiveBatchController(
            initialSize = 5,
            minSize = 1,
            maxSize = 20
        )

        // 4 successes
        repeat(4) {
            controller.onBatchSuccess()
        }

        // Validation error resets counter and halves size
        controller.onValidationError()
        assertEquals(2, controller.getCurrentBatchSize())

        // Need 5 more successes to increase
        repeat(4) {
            controller.onBatchSuccess()
        }
        assertEquals(2, controller.getCurrentBatchSize())

        // 5th success increases
        controller.onBatchSuccess()
        assertEquals(3, controller.getCurrentBatchSize())
    }

    @Test
    fun `should reset batch size to maximum`() {
        val controller = AdaptiveBatchController(
            initialSize = 10,
            minSize = 1,
            maxSize = 10
        )

        // Reduce size
        controller.onValidationError()
        assertEquals(5, controller.getCurrentBatchSize())

        controller.onValidationError()
        assertEquals(2, controller.getCurrentBatchSize())

        // Reset to maximum
        controller.reset()
        assertEquals(10, controller.getCurrentBatchSize())
    }

    @Test
    fun `should handle AIMD pattern correctly`() {
        // Additive Increase, Multiplicative Decrease
        val controller = AdaptiveBatchController(
            initialSize = 8,
            minSize = 1,
            maxSize = 20
        )

        assertEquals(8, controller.getCurrentBatchSize())

        // Increase: 8 -> 9 (after 5 successes)
        repeat(5) { controller.onBatchSuccess() }
        assertEquals(9, controller.getCurrentBatchSize())

        // Increase: 9 -> 10
        repeat(5) { controller.onBatchSuccess() }
        assertEquals(10, controller.getCurrentBatchSize())

        // Decrease (multiplicative): 10 -> 5
        controller.onValidationError()
        assertEquals(5, controller.getCurrentBatchSize())

        // Increase (additive): 5 -> 6
        repeat(5) { controller.onBatchSuccess() }
        assertEquals(6, controller.getCurrentBatchSize())

        // Decrease (multiplicative): 6 -> 3
        controller.onValidationError()
        assertEquals(3, controller.getCurrentBatchSize())
    }
}
