package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.tmx.model.Tmx
import net.sharplab.tsuji.tmx.index.TranslationIndex
import org.slf4j.LoggerFactory
import jakarta.enterprise.context.Dependent

@Dependent
class PoTranslatorServiceImpl(
    private val translator: Translator
) : PoTranslatorService {

    private val logger = LoggerFactory.getLogger(PoTranslatorServiceImpl::class.java)

    override fun translate(
            po: Po,
            source: String,
            target: String,
            isAsciidoctor: Boolean,
            useRag: Boolean
    ): Po {
        // Delegate translation entirely to Translator implementation
        return translator.translate(po, source, target, isAsciidoctor, useRag)
    }

    override fun applyTmx(tmx: Tmx, po: Po): Po {
        val translationIndex = TranslationIndex.create(tmx, po.target)
        val messages = po.messages
        messages.filter { it.fuzzy || it.messageString.isEmpty() }.forEach {
            val value = translationIndex[it.messageId]
            if(value != null){
                it.messageString = value
                it.fuzzy = false
            }
        }
        return Po(po.target, messages)
    }

    override fun applyFuzzyTmx(fuzzyTmx: Tmx, po: Po): Po {
        val translationIndex = TranslationIndex.create(fuzzyTmx, po.target)
        val messages = po.messages
        messages.filter { it.fuzzy || it.messageString.isEmpty() }.forEach {
            val value = translationIndex[it.messageId]
            if(value != null){
                it.messageString = value
                it.fuzzy = true
            }
        }
        return Po(po.target, messages)
    }
}
