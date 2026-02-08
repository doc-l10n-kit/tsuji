package net.sharplab.tsuji.core.driver.translator

interface Translator {
    fun translate(texts: List<String>, srcLang: String, dstLang: String, useRag: Boolean): List<String>
}
