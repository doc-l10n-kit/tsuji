package net.sharplab.tsuji.core.driver.translator.adaptive

/**
 * Provides batches from a list of items with adaptive size control.
 *
 * @param T The type of items in the batch
 */
interface BatchProvider<T> {
    /**
     * Check if there are more batches to process.
     *
     * @return true if there are more batches
     */
    fun hasNext(): Boolean

    /**
     * Peek at the next batch without consuming it.
     *
     * Returns a batch starting from current position based on current size limit.
     * Position is NOT advanced - call consumeNext() after successful processing.
     *
     * @return The next batch
     */
    fun peekNext(): List<T>

    /**
     * Consume the previously peeked batch and advance position.
     *
     * Call this after successfully processing a batch returned by peekNext().
     *
     * @return The batch that was consumed (same as the last peekNext() result)
     */
    fun consumeNext(): List<T>

    /**
     * Notify that a batch was successfully processed.
     * May increase the size limit based on the implementation's strategy.
     */
    fun notifySuccess()

    /**
     * Notify that a validation error occurred (e.g., batch too large).
     * May decrease the size limit based on the implementation's strategy.
     *
     * @return The new size limit after adjustment
     */
    fun notifyValidationError(): Int
}
