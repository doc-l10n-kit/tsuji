package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po
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
}