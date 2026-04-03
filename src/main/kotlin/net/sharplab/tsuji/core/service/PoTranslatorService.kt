package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.tmx.index.TranslationIndex
import net.sharplab.tsuji.tmx.model.Tmx

interface PoTranslatorService {
    fun translate(
        po: Po,
        source: String,
        target: String,
        isAsciidoctor: Boolean,
        useRag: Boolean
    ): Po

    fun applyTmx(tmx: Tmx, po: Po): Po

    fun applyFuzzyTmx(fuzzyTmx: Tmx, po: Po): Po

    /**
     * Apply TMX translations using a pre-built translation index.
     * This is more efficient when applying the same TMX to multiple PO files.
     *
     * @param translationIndex Pre-built index from TranslationIndex.create()
     * @param po PO file to apply translations to
     * @param fuzzy If true, mark applied translations as fuzzy
     * @return Updated PO with translations applied
     */
    fun applyTmxWithIndex(translationIndex: TranslationIndex, po: Po, fuzzy: Boolean): Po
}