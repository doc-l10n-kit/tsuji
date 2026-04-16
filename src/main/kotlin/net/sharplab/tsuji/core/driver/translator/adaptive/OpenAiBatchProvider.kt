package net.sharplab.tsuji.core.driver.translator.adaptive

import org.slf4j.LoggerFactory

/**
 * Batch provider for OpenAI-compatible API translation.
 *
 * Uses count-based batching with AIMD (Additive Increase Multiplicative Decrease) control.
 * Each batch's item count must not exceed the size limit.
 */
class OpenAiBatchProvider<T>(
    private val items: List<T>,
    initialLimit: Int,
    private val minLimit: Int,
    private val maxLimit: Int
) : BatchProvider<T> {

    private val logger = LoggerFactory.getLogger(OpenAiBatchProvider::class.java)

    @Volatile
    private var currentLimit: Int = initialLimit

    private var currentPosition = 0

    override fun hasNext(): Boolean = currentPosition < items.size

    override fun peekNext(): List<T> {
        if (!hasNext()) {
            throw NoSuchElementException("No more items available")
        }

        val batch = mutableListOf<T>()
        var index = currentPosition

        while (index < items.size && batch.size < currentLimit) {
            batch.add(items[index])
            index++
        }

        return batch
    }

    override fun consumeNext(): List<T> {
        val batch = peekNext()
        currentPosition += batch.size
        return batch
    }

    override fun notifySuccess() {
        if (currentLimit < maxLimit) {
            val oldLimit = currentLimit
            currentLimit = minOf(currentLimit + ADDITIVE_INCREASE_STEP, maxLimit)
            if (currentLimit > oldLimit) {
                logger.info("Increasing batch size limit: $oldLimit → $currentLimit")
            }
        }
    }

    override fun notifyValidationError(): Int {
        val oldLimit = currentLimit
        currentLimit = maxOf((currentLimit * MULTIPLICATIVE_DECREASE_FACTOR).toInt(), minLimit)
        if (currentLimit < oldLimit) {
            logger.warn("Decreasing batch size limit: $oldLimit → $currentLimit")
        }
        return currentLimit
    }

    companion object {
        // AIMD parameters
        private const val ADDITIVE_INCREASE_STEP = 1
        private const val MULTIPLICATIVE_DECREASE_FACTOR = 0.5
    }
}
