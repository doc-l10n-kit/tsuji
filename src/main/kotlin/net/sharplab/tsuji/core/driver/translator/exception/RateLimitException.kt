package net.sharplab.tsuji.core.driver.translator.exception

/**
 * Rate limit error from API.
 * This should reduce parallelism, not batch size.
 */
class RateLimitException(
    message: String,
    val retryAfterSeconds: Int? = null
) : RuntimeException(message)
