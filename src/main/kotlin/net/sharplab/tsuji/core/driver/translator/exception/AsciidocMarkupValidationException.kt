package net.sharplab.tsuji.core.driver.translator.exception

/**
 * Asciidoc markup (links, images, emphasis, etc.) in translations does not match source texts.
 * Contains the broken translations with notes on how to fix them.
 */
class AsciidocMarkupValidationException(
    val brokenTranslations: List<BrokenTranslation>
) : TranslationValidationException("Asciidoc markup broken in ${brokenTranslations.size} translation(s)")
