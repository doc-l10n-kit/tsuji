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
        var validationRetryCount = 0
        var generalRetryCount = 0
        var batchNumber = 0

        while (batchProvider.hasNext()) {
            batchNumber++
            val nextBatch = batchProvider.peekNext()

            try {
                val attemptNumber = generalRetryCount + 1
                logger.info("Processing batch #$batchNumber (${nextBatch.size} items, attempt $attemptNumber)")

                val batchStartTime = System.currentTimeMillis()
                // Execute operation on this batch
                val batchResults = operation(nextBatch)
                val batchElapsed = System.currentTimeMillis() - batchStartTime

                logger.info("Batch #$batchNumber completed in ${batchElapsed}ms")

                // Success: consumeNext to move to next batch
                results.addAll(batchResults)
                batchProvider.consumeNext()
                batchProvider.notifySuccess()

                // Reset retry counts for next batch
                validationRetryCount = 0
                generalRetryCount = 0

            } catch (e: TranslationValidationException) {
                // Validation error: reduce size limit and retry from same position
                validationRetryCount++
                logger.warn("Validation error (retry $validationRetryCount/$maxValidationRetries): ${e.javaClass.simpleName}: ${e.message}")

                if (validationRetryCount >= maxValidationRetries) {
                    logger.error("Exceeded maximum validation retries ($maxValidationRetries), giving up")
                    throw e
                }

                // Reduce size limit (next peekNext() will create smaller batch from same position)
                val newLimit = batchProvider.notifyValidationError()
                logger.info("Reduced size limit to $newLimit, will retry with smaller batch")

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

        return results
    }

}
