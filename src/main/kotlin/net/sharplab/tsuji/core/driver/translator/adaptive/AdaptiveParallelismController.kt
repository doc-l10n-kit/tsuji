package net.sharplab.tsuji.core.driver.translator.adaptive

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory

/**
 * Adaptive parallelism controller using token bucket + exponential backoff.
 *
 * Singleton instance shared across all files and batches.
 * Controls global API request concurrency.
 */
class AdaptiveParallelismController(
    initialConcurrency: Int = 3,
    val minConcurrency: Int = 1,
    val maxConcurrency: Int = 10
) {
    private val logger = LoggerFactory.getLogger(AdaptiveParallelismController::class.java)
    private val semaphore = Semaphore(initialConcurrency)
    private val currentLimit = AtomicInteger(initialConcurrency)
    private val consecutiveSuccesses = AtomicInteger(0)
    private val backoffDelayMs = AtomicLong(0)

    suspend fun <T> executeWithControl(block: suspend () -> T): T {
        semaphore.acquire()
        try {
            val delayMs = backoffDelayMs.get()
            if (delayMs > 0) {
                logger.info("Rate limit backoff: waiting ${delayMs}ms")
                delay(delayMs)
            }
            return block()
        } finally {
            semaphore.release()
        }
    }

    fun onRequestSuccess() {
        backoffDelayMs.set(0)
        val successes = consecutiveSuccesses.incrementAndGet()
        if (successes >= 10 && currentLimit.get() < maxConcurrency) {
            val newLimit = currentLimit.incrementAndGet()
            logger.info("Increasing parallelism: ${newLimit - 1} → $newLimit")
            semaphore.release() // Add permit
            consecutiveSuccesses.set(0)
        }
    }

    fun onRateLimitError() {
        consecutiveSuccesses.set(0)

        // Exponential backoff
        val currentDelay = backoffDelayMs.get()
        val newDelay = minOf(maxOf(currentDelay * 2, 1000), 30000)
        backoffDelayMs.set(newDelay)
        logger.warn("Rate limit hit, backing off ${newDelay}ms")

        // Decrease concurrency
        if (currentLimit.get() > minConcurrency) {
            val newLimit = currentLimit.decrementAndGet()
            logger.warn("Decreasing parallelism: ${newLimit + 1} → $newLimit")
            runCatching { semaphore.tryAcquire() }
        }
    }

    fun getCurrentConcurrency(): Int = currentLimit.get()
}
