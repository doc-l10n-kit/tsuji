package net.sharplab.tsuji.core.driver.translator.exception

/**
 * Response indices do not match expected indices in array-based batch translation.
 */
class IndexMismatchException(
    message: String,
    val expectedIndices: Set<Int>,
    val actualIndices: Set<Int>
) : TranslationValidationException(message)
