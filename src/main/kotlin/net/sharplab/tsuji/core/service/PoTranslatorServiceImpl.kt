package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.po.model.Po
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
        return applyTmxWithIndex(translationIndex, po, fuzzy = false)
    }

    override fun applyFuzzyTmx(fuzzyTmx: Tmx, po: Po): Po {
        val translationIndex = TranslationIndex.create(fuzzyTmx, po.target)
        return applyTmxWithIndex(translationIndex, po, fuzzy = true)
    }

    override fun applyTmxWithIndex(translationIndex: TranslationIndex, po: Po, fuzzy: Boolean): Po {
        val updatedMessages = po.messages.map { msg ->
            if (msg.fuzzy || msg.messageString.isEmpty()) {
                val value = translationIndex[msg.messageId]
                if (value != null) {
                    msg.copy(messageString = value).also { it.fuzzy = fuzzy }
                } else {
                    msg
                }
            } else {
                msg
            }
        }
        return Po(po.target, updatedMessages, po.header)
    }
}
