package net.sharplab.tsuji.core.driver.translator.processor
import net.sharplab.tsuji.core.model.translation.TranslationContext

import net.sharplab.tsuji.core.model.translation.TranslationMessage

/**
 * Processor for transforming translation messages in the pipeline.
 * Each processor reads TranslationMessage.text, processes it, and writes back.
 *
 * By processing in batches, individual processing and batch processing
 * (such as translation APIs) can be handled uniformly.
 */
interface MessageProcessor {
    /**
     * Processes a list of translation messages.
     *
     * @param messages List of translation messages to process
     * @param context Context information needed for processing
     * @return New list of TranslationMessages after processing (does not modify original instances)
     */
    fun process(messages: List<TranslationMessage>, context: TranslationContext): List<TranslationMessage>
}
