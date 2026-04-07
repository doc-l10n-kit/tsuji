package net.sharplab.tsuji.core.driver.translator.adaptive

import org.slf4j.LoggerFactory
import java.nio.charset.Charset

/**
 * Batch provider for DeepL translation.
 *
 * Uses byte size-based batching with AIMD (Additive Increase Multiplicative Decrease) control.
 * Each batch's total byte size must not exceed the size limit.
 */
class DeepLBatchProvider(
    private val items: List<String>,
    initialLimit: Int,
    private val minLimit: Int,
    private val maxLimit: Int,
    private val charset: Charset = Charsets.UTF_8
) : BatchProvider<String> {

    private val logger = LoggerFactory.getLogger(DeepLBatchProvider::class.java)

    @Volatile
    private var currentLimit: Int = initialLimit

    private var currentPosition = 0

    override fun hasNext(): Boolean = currentPosition < items.size

    override fun peekNext(): List<String> {
        if (!hasNext()) {
            throw NoSuchElementException("No more items available")
        }

        val batch = mutableListOf<String>()
        var currentTotalSize = 0
        var index = currentPosition

        while (index < items.size) {
            val item = items[index]
            val itemSize = item.toByteArray(charset).size

            // If adding this item would exceed limit, stop (but allow first item)
            if (batch.isNotEmpty() && currentTotalSize + itemSize > currentLimit) {
                break
            }

            batch.add(item)
            currentTotalSize += itemSize
            index++
        }

        return batch
    }

    override fun consumeNext(): List<String> {
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
