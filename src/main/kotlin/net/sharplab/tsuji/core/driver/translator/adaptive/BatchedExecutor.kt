package net.sharplab.tsuji.core.driver.translator.adaptive

import kotlinx.coroutines.delay
import net.sharplab.tsuji.core.driver.translator.exception.TranslationValidationException
import org.slf4j.LoggerFactory

/**
 * Executes operations on batches with adaptive size limit and retry logic.
 *
 * Handles:
 * - Batch splitting using BatchProvider
 * - Retry logic (maxRetries for general errors, maxValidationRetries for validation errors)
 * - Automatic size limit reduction on validation errors
 * - Exponential backoff on retries
 */
class BatchedExecutor<T>(
    private val batchProvider: BatchProvider<T>,
    private val maxRetries: Int = 3,
    private val maxValidationRetries: Int = 5
) {
    private val logger = LoggerFactory.getLogger(BatchedExecutor::class.java)

    /**
     * Execute operation on all items in batches with retry logic.
     *
     * @param R The type of results returned by the operation
     * @param operation The operation to execute on each batch
     * @return Combined results from all batches
     */
    suspend fun <R> execute(
        operation: suspend (batch: List<T>) -> List<R>
    ): List<R> {
        val results = mutableListOf<R>()

        while (batchProvider.hasNext()) {
            var nextBatch = batchProvider.peekNext()
            var validationRetryCount = 0
            var generalRetryCount = 0

            // Retry loop for current batch
            while (true) {
                try {
                    val attemptNumber = generalRetryCount + 1
                    logger.info("Processing batch (${nextBatch.size} items, attempt $attemptNumber)")

                    // Execute operation on this batch
                    val batchResults = operation(nextBatch)

                    // Success
                    results.addAll(batchResults)
                    // Consume just to move next batch
                    batchProvider.consumeNext()
                    batchProvider.notifySuccess()
                    break  // Success, move to next batch

                } catch (e: TranslationValidationException) {
                    // Validation error: reduce size limit and recreate batch
                    validationRetryCount++
                    logger.warn("Validation error (retry $validationRetryCount/$maxValidationRetries): ${e.javaClass.simpleName}: ${e.message}")

                    if (validationRetryCount >= maxValidationRetries) {
                        logger.error("Exceeded maximum validation retries ($maxValidationRetries), giving up")
                        throw e
                    }

                    // Reduce size limit and recreate batch
                    val newLimit = batchProvider.notifyValidationError()
                    nextBatch = batchProvider.peekNext()
                    logger.info("Reduced size limit to $newLimit, recreated batch with ${nextBatch.size} items")

                    delay(1000L * validationRetryCount)
                } catch (e: Exception) {
                    // General errors (rate limit, transport, etc.): simple retry without size limit change
                    // Note: RateLimitException is handled by AdaptiveParallelismController (reduces concurrency)
                    generalRetryCount++
                    logger.warn("Error: ${e.javaClass.simpleName}: ${e.message}, retrying")

                    if (generalRetryCount >= maxRetries) {
                        throw e
                    }

                    delay(1000L * generalRetryCount)
                }
            }
        }

        return results
    }

}
