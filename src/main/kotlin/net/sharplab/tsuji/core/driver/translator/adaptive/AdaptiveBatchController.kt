package net.sharplab.tsuji.core.driver.translator.adaptive

import java.util.concurrent.atomic.AtomicInteger

/**
 * Adaptive batch size controller using AIMD algorithm.
 *
 * - Multiplicative Decrease: Halve size on LLM response validation error
 * - Additive Increase: +1 every 5 consecutive successes
 *
 * Instance per .po file (not shared across files).
 */
class AdaptiveBatchController(
    initialSize: Int,
    val minSize: Int = 1,
    val maxSize: Int = initialSize
) {
    private val currentSize = AtomicInteger(initialSize)
    private val consecutiveSuccesses = AtomicInteger(0)

    fun getCurrentBatchSize(): Int = currentSize.get()

    fun onBatchSuccess() {
        val successes = consecutiveSuccesses.incrementAndGet()
        if (successes >= 5 && currentSize.get() < maxSize) {
            currentSize.incrementAndGet()
            consecutiveSuccesses.set(0)
        }
    }

    fun onValidationError(): Int {
        consecutiveSuccesses.set(0)
        val newSize = maxOf(currentSize.get() / 2, minSize)
        currentSize.set(newSize)
        return newSize
    }

    fun reset() {
        currentSize.set(maxSize)
        consecutiveSuccesses.set(0)
    }
}
