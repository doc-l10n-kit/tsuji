package net.sharplab.tsuji.core.driver.translator.exception

/**
 * LLM response could not be parsed (invalid JSON, unexpected format, etc.)
 */
class ResponseParseException(
    message: String,
    cause: Throwable? = null
) : TranslationValidationException(message, cause)
