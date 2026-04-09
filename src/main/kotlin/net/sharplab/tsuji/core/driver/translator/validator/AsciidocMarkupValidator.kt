package net.sharplab.tsuji.core.driver.translator.validator

import net.sharplab.tsuji.core.driver.translator.exception.AsciidocMarkupValidationException
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
     * @throws AsciidocMarkupValidationException if any markup is broken
     */
    fun validate(messages: List<TranslationMessage>) {
        val broken = messages.filter { msg ->
            val sourceFeatures = extractMarkupFeatures(msg.original.messageId)
            if (sourceFeatures.isEmpty()) return@filter false

            val translatedFeatures = extractMarkupFeatures(msg.text)
            val issues = compareFeatures(sourceFeatures, translatedFeatures)
            if (issues.isNotEmpty()) {
                logger.warn("Broken Asciidoc markup in translation of '${msg.original.messageId.take(60)}...': $issues")
                true
            } else {
                false
            }
        }

        if (broken.isNotEmpty()) {
            throw AsciidocMarkupValidationException(broken)
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
                    .map { it.attr("href").substringBefore("#") }
                    .filter { it.isNotEmpty() }
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

    private fun compareFeatures(source: MarkupFeatures, translated: MarkupFeatures): List<String> {
        val issues = mutableListOf<String>()

        val missingLinks = source.linkHrefs - translated.linkHrefs
        if (missingLinks.isNotEmpty()) issues.add("missing links: $missingLinks")

        val missingImages = source.imageSrcs - translated.imageSrcs
        if (missingImages.isNotEmpty()) issues.add("missing images: $missingImages")

        if (source.emphasisCount != translated.emphasisCount)
            issues.add("emphasis count: ${source.emphasisCount} → ${translated.emphasisCount}")

        if (source.strongCount != translated.strongCount)
            issues.add("strong count: ${source.strongCount} → ${translated.strongCount}")

        if (source.codeCount != translated.codeCount)
            issues.add("code count: ${source.codeCount} → ${translated.codeCount}")

        return issues
    }
}
