package net.sharplab.tsuji.core.driver.translator

import net.sharplab.tsuji.po.model.Po

interface Translator {
    /**
     * Translates a Po object.
     * Returns a new Po with messageString and fuzzy flag updated in translated messages.
     *
     * @param po The Po object to translate
     * @param srcLang Source language code
     * @param dstLang Destination language code
     * @param isAsciidoctor Whether the source is Asciidoctor format (requires pre/post processing)
     * @param useRag Whether to use RAG (Retrieval-Augmented Generation)
     * @return A new Po object with translated messages
     */
    suspend fun translate(po: Po, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): Po
}
