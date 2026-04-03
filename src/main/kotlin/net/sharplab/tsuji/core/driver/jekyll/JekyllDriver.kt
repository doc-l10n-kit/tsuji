package net.sharplab.tsuji.core.driver.jekyll

import java.nio.file.Path

interface JekyllDriver {
    /**
     * Prepares Jekyll source in the work directory by copying from sourceDir.
     */
    fun prepareSource(sourceDir: Path, workDir: Path)

    /**
     * Applies overrides from overrideDir to the workDir.
     */
    fun applyOverrides(overrideDir: Path, workDir: Path)

    /**
     * Updates PO files from AsciiDoc source using jekyll-l10n plugin in update_po mode.
     */
    fun extractPo(jekyllSourceDir: Path, poBaseDir: Path)

    /**
     * Builds the Jekyll site.
     */
    fun build(jekyllSourceDir: Path, poBaseDir: Path, destinationDir: Path, siteLanguageCode: String, additionalConfigs: List<String> = emptyList(), translate: Boolean = true)

    /**
     * Serves the Jekyll site.
     */
    fun serve(jekyllSourceDir: Path, poBaseDir: Path, destinationDir: Path, siteLanguageCode: String, additionalConfigs: List<String> = emptyList(), translate: Boolean = true)
}