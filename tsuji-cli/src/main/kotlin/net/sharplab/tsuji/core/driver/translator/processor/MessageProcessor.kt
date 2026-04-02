package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.PoMessage

/**
 * Processor for transforming a list of PoMessages.
 * Transforms batches of PoMessages while referencing Po as context.
 *
 * By processing in batches, individual processing and batch processing
 * (such as translation APIs) can be handled uniformly.
 */
interface MessageProcessor {
    /**
     * Processes a list of messages.
     *
     * @param messages List of messages to process
     * @param context Context information needed for processing
     * @return New list of PoMessages after processing (does not modify original instances)
     */
    fun process(messages: List<PoMessage>, context: ProcessingContext): List<PoMessage>
}
