package net.sharplab.tsuji.core.driver.translator

import net.sharplab.tsuji.core.model.po.Po

interface Translator {
    /**
     * Translates a Po object.
     * The Translator implementation directly modifies PoMessage.messageString and fuzzy flag.
     *
     * @param po The Po object to translate
     * @param srcLang Source language code
     * @param dstLang Destination language code
     * @param isAsciidoctor Whether the source is Asciidoctor format (requires pre/post processing)
     * @param useRag Whether to use RAG (Retrieval-Augmented Generation)
     * @return The same Po object with translated messages
     */
    fun translate(po: Po, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): Po
}
