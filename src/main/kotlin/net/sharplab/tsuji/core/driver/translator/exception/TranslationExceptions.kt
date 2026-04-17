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
 * Response indices do not match expected indices in array-based batch translation.
 */
class IndexMismatchException(
    message: String,
    val expectedIndices: Set<Int>,
    val actualIndices: Set<Int>
) : TranslationValidationException(message)

/**
 * Represents a single validation error with instructions for fixing.
 */
data class ValidationError(
    val type: MarkupType,
    val expectedCount: Int,
    val actualCount: Int
) {
    /**
     * Returns instruction message for LLM to fix this specific error.
     */
    fun toInstruction(): String = when (type) {
        MarkupType.CODE ->
            "This text contains $expectedCount backtick pair(s) for inline code markup. " +
            "Ensure your translation preserves exactly $expectedCount backtick pair(s) with proper spacing in CJK text."

        MarkupType.STRONG ->
            "This text contains $expectedCount bold markup (*text*). " +
            "Preserve exactly $expectedCount bold markup(s) with proper spacing in CJK text."

        MarkupType.EMPHASIS ->
            "This text contains $expectedCount italic markup (_text_). " +
            "Preserve exactly $expectedCount italic markup(s) with proper spacing in CJK text."

        MarkupType.LINK ->
            "This text contains $expectedCount link(s). " +
            "Preserve all link URLs unchanged in your translation."

        MarkupType.IMAGE ->
            "This text contains $expectedCount image(s). " +
            "Preserve all image paths unchanged in your translation."
    }
}

/**
 * Represents markup types that can be validated.
 */
enum class MarkupType {
    CODE,      // `code`
    STRONG,    // *bold*
    EMPHASIS,  // _italic_
    LINK,      // link:url[text] or https://url[text]
    IMAGE      // image:path[alt]
}

/**
 * Represents a broken translation with specific validation errors.
 */
data class BrokenTranslation(
    val message: net.sharplab.tsuji.core.model.translation.TranslationMessage,
    val errors: List<ValidationError>
) {
    /**
     * Returns combined instruction for all errors in this translation.
     */
    fun toInstruction(): String = errors.joinToString(" ") { it.toInstruction() }
}

/**
 * Asciidoc markup (links, images, emphasis, etc.) in translations does not match source texts.
 * Contains the broken translations with specific validation errors.
 */
class AsciidocMarkupValidationException(
    val brokenTranslations: List<BrokenTranslation>
) : TranslationValidationException("Asciidoc markup broken in ${brokenTranslations.size} translation(s)") {
    /**
     * Backward compatibility: returns just the messages.
     */
    @Deprecated("Use brokenTranslations instead", ReplaceWith("brokenTranslations.map { it.message }"))
    val brokenMessages: List<net.sharplab.tsuji.core.model.translation.TranslationMessage>
        get() = brokenTranslations.map { it.message }
}

/**
 * Rate limit error from API.
 * This should reduce parallelism, not batch size.
 */
class RateLimitException(
    message: String,
    val retryAfterSeconds: Int? = null
) : RuntimeException(message)
