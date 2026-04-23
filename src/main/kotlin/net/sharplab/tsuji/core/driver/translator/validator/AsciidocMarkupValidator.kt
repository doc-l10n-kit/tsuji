package net.sharplab.tsuji.core.driver.translator.validator

import net.sharplab.tsuji.core.driver.translator.exception.AsciidocMarkupValidationException
import net.sharplab.tsuji.core.driver.translator.exception.BrokenTranslation
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

/**
 * Validates that Asciidoc markup (links, images, emphasis, etc.) is preserved in translations.
 * Converts source and translated text to HTML via AsciidoctorJ, then compares DOM structure with jsoup.
 *
 * @throws AsciidocMarkupValidationException when markup is broken
 */
class AsciidocMarkupValidator(private val asciidoctor: Asciidoctor) {

    private val logger = LoggerFactory.getLogger(AsciidocMarkupValidator::class.java)

    data class MarkupFeatures(
        val linkHrefs: Set<String>,
        val imageSrcs: Set<String>,
        val emphasisCount: Int,
        val strongCount: Int,
        val codeCount: Int
    ) {
        fun isEmpty(): Boolean =
            linkHrefs.isEmpty() && imageSrcs.isEmpty() && emphasisCount == 0 && strongCount == 0 && codeCount == 0
    }

    /**
     * Validates that markup features in source texts are preserved in translations.
     * Also detects when markup is incorrectly added to translations.
     * @throws AsciidocMarkupValidationException if any markup is broken
     */
    fun validate(messages: List<TranslationMessage>) {
        val brokenTranslations = messages.mapNotNull { msg ->
            val sourceFeatures = extractMarkupFeatures(msg.original.messageId)
            val translatedFeatures = extractMarkupFeatures(msg.text)

            val note = buildValidationNote(sourceFeatures, translatedFeatures)
            if (note != null) {
                logger.warn("Broken Asciidoc markup in translation of '${msg.original.messageId.take(60)}...'")
                BrokenTranslation(msg, note)
            } else {
                null
            }
        }

        if (brokenTranslations.isNotEmpty()) {
            throw AsciidocMarkupValidationException(brokenTranslations)
        }
    }

    /**
     * Converts Asciidoc text to HTML and extracts markup features from the DOM.
     */
    fun extractMarkupFeatures(text: String): MarkupFeatures {
        if (text.isBlank()) return MarkupFeatures(emptySet(), emptySet(), 0, 0, 0)

        return try {
            val html = convertToHtml(text)
            val doc = Jsoup.parseBodyFragment(html)
            val body = doc.body()

            MarkupFeatures(
                linkHrefs = body.select("a[href]")
                    .map { it.attr("href") }
                    .toSet(),
                imageSrcs = body.select("img[src]").map { it.attr("src") }.toSet(),
                emphasisCount = body.select("em").size,
                strongCount = body.select("strong").size,
                codeCount = body.select("code").size
            )
        } catch (e: Exception) {
            logger.debug("Failed to extract markup features: ${e.message}")
            MarkupFeatures(emptySet(), emptySet(), 0, 0, 0)
        }
    }

    private fun convertToHtml(text: String): String {
        val options = Options.builder().catalogAssets(true).build()
        val document = asciidoctor.load(text, options)
        return document.convert()
    }

    /**
     * Builds a validation note if markup features don't match.
     * Returns null if everything matches.
     */
    private fun buildValidationNote(source: MarkupFeatures, translated: MarkupFeatures): String? {
        val notes = mutableListOf<String>()

        if (source.codeCount != translated.codeCount) {
            notes.add("This text contains ${source.codeCount} backtick pair(s) for inline code markup. " +
                    "Ensure your translation preserves exactly ${source.codeCount} backtick pair(s) with proper spacing in CJK text.")
        }

        if (source.strongCount != translated.strongCount) {
            notes.add("This text contains ${source.strongCount} bold markup (*text*). " +
                    "Preserve exactly ${source.strongCount} bold markup(s) with proper spacing in CJK text.")
        }

        if (source.emphasisCount != translated.emphasisCount) {
            notes.add("This text contains ${source.emphasisCount} italic markup (_text_). " +
                    "Preserve exactly ${source.emphasisCount} italic markup(s) with proper spacing in CJK text.")
        }

        if (source.linkHrefs.size != translated.linkHrefs.size) {
            notes.add("This text contains ${source.linkHrefs.size} link(s). " +
                    "Preserve all link URLs unchanged in your translation.")
        }

        if (source.imageSrcs.size != translated.imageSrcs.size) {
            notes.add("This text contains ${source.imageSrcs.size} image(s). " +
                    "Preserve all image paths unchanged in your translation.")
        }

        return if (notes.isEmpty()) null else notes.joinToString(" ")
    }
}
