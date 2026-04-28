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
        val codeCount: Int,
        val emphasisTexts: List<String> = emptyList(),
        val strongTexts: List<String> = emptyList(),
        val codeTexts: List<String> = emptyList()
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

            val emElements = body.select("em")
            val strongElements = body.select("strong")
            val codeElements = body.select("code")

            MarkupFeatures(
                linkHrefs = body.select("a[href]")
                    .map { it.attr("href") }
                    .toSet(),
                imageSrcs = body.select("img[src]").map { it.attr("src") }.toSet(),
                emphasisCount = emElements.size,
                strongCount = strongElements.size,
                codeCount = codeElements.size,
                emphasisTexts = emElements.map { it.text() },
                strongTexts = strongElements.map { it.text() },
                codeTexts = codeElements.map { it.text() }
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
            val sourceDesc = if (source.codeTexts.isNotEmpty()) {
                source.codeTexts.joinToString(", ") { "`$it`" }
            } else "none"
            if (source.codeCount > translated.codeCount) {
                notes.add("Source has ${source.codeCount} code span(s) ($sourceDesc) but your translation has ${translated.codeCount}. " +
                        "Preserve all code spans with proper spacing in CJK text.")
            } else {
                notes.add("Source has ${source.codeCount} code span(s) ($sourceDesc) but your translation has ${translated.codeCount}. " +
                        "Do NOT add backticks around text that is not in backticks in the source.")
            }
        }

        if (source.strongCount != translated.strongCount) {
            val sourceDesc = if (source.strongTexts.isNotEmpty()) {
                source.strongTexts.joinToString(", ") { "*$it*" }
            } else "none"
            if (source.strongCount > translated.strongCount) {
                notes.add("Source has ${source.strongCount} bold markup ($sourceDesc) but your translation has ${translated.strongCount}. " +
                        "Preserve the bold markup with proper spacing in CJK text.")
            } else {
                notes.add("Source has ${source.strongCount} bold markup ($sourceDesc) but your translation has ${translated.strongCount}. " +
                        "Do NOT add bold markup that is not in the source.")
            }
        }

        if (source.emphasisCount != translated.emphasisCount) {
            val sourceDesc = if (source.emphasisTexts.isNotEmpty()) {
                source.emphasisTexts.joinToString(", ") { "_${it}_" }
            } else "none"
            if (source.emphasisCount > translated.emphasisCount) {
                notes.add("Source has ${source.emphasisCount} italic markup ($sourceDesc) but your translation has ${translated.emphasisCount}. " +
                        "Preserve the italic markup with proper spacing in CJK text.")
            } else {
                notes.add("Source has ${source.emphasisCount} italic markup ($sourceDesc) but your translation has ${translated.emphasisCount}. " +
                        "Do NOT add italic markup that is not in the source.")
            }
        }

        if (source.linkHrefs.size != translated.linkHrefs.size) {
            notes.add("Source has ${source.linkHrefs.size} link(s) but your translation has ${translated.linkHrefs.size}. " +
                    "Preserve all link URLs unchanged in your translation. " +
                    "IMPORTANT: In CJK text, URLs MUST be preceded by a half-width space or Asciidoctor will not recognize them as links. " +
                    "Example: ✓ \"静的ファイルは、 https://example.com[text] を参照\" ✗ \"静的ファイルは、https://example.com[text] を参照\"")
        }

        if (source.imageSrcs.size != translated.imageSrcs.size) {
            notes.add("Source has ${source.imageSrcs.size} image(s) but your translation has ${translated.imageSrcs.size}. " +
                    "Preserve all image paths unchanged in your translation.")
        }

        return if (notes.isEmpty()) null else notes.joinToString(" ")
    }
}
