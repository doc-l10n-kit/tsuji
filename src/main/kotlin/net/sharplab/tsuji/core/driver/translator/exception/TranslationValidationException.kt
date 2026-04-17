package net.sharplab.tsuji.core.driver.translator.exception

/**
 * Base exception for LLM response validation errors.
 * These errors indicate the batch size should be reduced.
 */
abstract class TranslationValidationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
