package net.sharplab.tsuji.core.driver.translator.exception

/**
 * Base exception for LLM response validation errors.
 * These errors indicate the batch size should be reduced.
 */
abstract class TranslationValidationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * LLM returned different number of translations than requested.
 */
class BatchSizeMismatchException(
    val expected: Int,
    val actual: Int,
    message: String
) : TranslationValidationException(message)

/**
 * LLM response could not be parsed (invalid JSON, unexpected format, etc.)
 */
class ResponseParseException(
    message: String,
    cause: Throwable? = null
) : TranslationValidationException(message, cause)

/**
 * Response keys do not match request keys in indexed batch translation.
 */
class KeyMismatchException(
    message: String,
    val expectedKeys: Set<String>,
    val actualKeys: Set<String>
) : TranslationValidationException(message)

/**
 * Rate limit error from API.
 * This should reduce parallelism, not batch size.
 */
class RateLimitException(
    message: String,
    val retryAfterSeconds: Int? = null
) : RuntimeException(message)
