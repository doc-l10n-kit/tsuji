package net.sharplab.tsuji.core.driver.translator.adaptive

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory

/**
 * Adaptive parallelism controller using Semaphore-based concurrency control.
 *
 * Singleton instance shared across all files and batches.
 * Controls global API request concurrency with dynamic limit adjustment.
 *
 * ## Core Idea: Dynamic Concurrency Control via Reserved Permits
 *
 * The key insight is that we can dynamically control concurrency by **reserving (holding)
 * unused Semaphore permits**:
 *
 * 1. Create Semaphore with maximum capacity (e.g., 10 permits)
 * 2. To limit concurrency to 3: reserve 7 permits (10 - 3 = 7)
 * 3. To increase to 4: release 1 reserved permit (7 - 1 = 6 reserved)
 * 4. To decrease to 2: acquire 1 more permit to reserve (6 + 1 = 7 reserved)
 *
 * This allows dynamic adjustment without recreating the Semaphore, avoiding the issue
 * of in-flight requests bypassing the new limit during transition.
 *
 * ## Strategy:
 * - Increases concurrency after consecutive successes (AIMD: Additive Increase)
 * - Decreases concurrency on rate limit errors (AIMD: Multiplicative Decrease)
 */
class AdaptiveParallelismController(
    initialConcurrency: Int = DEFAULT_INITIAL_CONCURRENCY,
    val minConcurrency: Int = DEFAULT_MIN_CONCURRENCY,
    val maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
    private val rateLimitExceptionClass: KClass<out Exception>
) {
    private val logger = LoggerFactory.getLogger(AdaptiveParallelismController::class.java)

    // Semaphore with maximum capacity - never recreated
    private val semaphore = Semaphore(maxConcurrency)

    // Current effective concurrency limit
    // @Volatile ensures visibility across threads; all writes are protected by stateMutex
    @Volatile
    private var currentLimit: Int = initialConcurrency

    // Mutex to protect state updates (currentLimit, consecutiveSuccesses) and synchronize with semaphore
    private val stateMutex = Mutex()

    // Adaptive control state: consecutive successful API requests
    // All access protected by stateMutex
    private var consecutiveSuccesses: Int = 0

    init {
        // Reserve permits to set initial concurrency limit
        // If max=10 and initial=3, we need to hold 7 permits
        val permitsToReserve = maxConcurrency - initialConcurrency
        if (permitsToReserve > 0) {
            runBlocking {
                repeat(permitsToReserve) {
                    semaphore.acquire()
                }
            }
        }
    }

    /**
     * Execute a block with concurrency control.
     * Uses Semaphore to properly suspend when limit is reached (no busy-wait).
     * Automatically handles success/error callbacks based on execution result:
     * - Success: releases permit and calls onRequestSuccess()
     * - RateLimitException: keeps permit (becomes reserved), decreases concurrency, and re-throws
     * - Other exceptions: releases permit and re-throws
     */
    suspend fun <T> executeWithControl(block: suspend () -> T): T {
        // Acquire permit - suspends properly when none available
        semaphore.acquire()

        try {
            val result = block()

            // Success: release permit and update state
            semaphore.release()
            onRequestSuccess()
            return result
        } catch (e: Exception) {
            if (isRateLimitException(e)) {
                // Rate limit error: keep permit as reserved, decrease concurrency
                stateMutex.withLock {
                    consecutiveSuccesses = 0
                    if (currentLimit > minConcurrency) {
                        currentLimit--
                        logger.warn("Decreasing parallelism: ${currentLimit + 1} → $currentLimit (permit kept as reserved)")
                    } else {
                        // Already at minimum, release the permit
                        semaphore.release()
                    }
                }
            } else {
                // Other errors: release permit
                semaphore.release()
            }
            // Re-throw exception for caller to handle
            throw e
        }
    }

    /**
     * Get the current concurrency limit.
     * Lock-free read of @Volatile field for monitoring purposes.
     */
    fun getCurrentConcurrency(): Int = currentLimit

    // Private adaptive control methods

    /**
     * Called internally when a request succeeds.
     * Increases concurrency limit after consecutive successes.
     */
    private suspend fun onRequestSuccess() {
        stateMutex.withLock {
            // Track consecutive successes
            consecutiveSuccesses++

            // Increase concurrency if threshold met
            if (consecutiveSuccesses >= SUCCESS_THRESHOLD_FOR_INCREASE) {
                increaseConcurrencyIfBelowMax()
                consecutiveSuccesses = 0  // Reset regardless of whether increase succeeded
            }
        }
    }

    /**
     * Increase concurrency limit by releasing one reserved permit if below maximum.
     * Example: limit=3, reserved=7 → release 1 permit → limit=4, reserved=6
     *
     * MUST be called within stateMutex lock.
     */
    private fun increaseConcurrencyIfBelowMax() {
        if (currentLimit >= maxConcurrency) {
            return  // Already at maximum
        }

        // Update both currentLimit and semaphore
        val oldLimit = currentLimit
        currentLimit++
        semaphore.release()  // Release one reserved permit
        logger.info("Increasing parallelism: $oldLimit → $currentLimit")
    }

    // Private helper methods

    private fun isRateLimitException(e: Exception): Boolean {
        // Type-safe check using KClass
        return rateLimitExceptionClass.isInstance(e)
    }

    companion object {
        // Default concurrency settings
        private const val DEFAULT_INITIAL_CONCURRENCY = 3
        private const val DEFAULT_MIN_CONCURRENCY = 1
        private const val DEFAULT_MAX_CONCURRENCY = 10

        // Adaptive control parameters
        private const val SUCCESS_THRESHOLD_FOR_INCREASE = 10
    }
}
