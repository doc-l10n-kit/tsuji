package net.sharplab.tsuji.core.driver.translator.exception

/**
 * LLM returned different number of translations than requested.
 */
class BatchSizeMismatchException(
    val expected: Int,
    val actual: Int,
    message: String
) : TranslationValidationException(message)
