package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.tmx.model.Tmx
import net.sharplab.tsuji.tmx.index.TranslationIndex
import net.sharplab.tsuji.core.processor.AsciidoctorMessageProcessor
import org.slf4j.LoggerFactory
import jakarta.enterprise.context.Dependent

@Dependent
class PoTranslatorServiceImpl(
    private val translator: Translator,
    private val messageProcessor: AsciidoctorMessageProcessor,
    private val messageTranslationService: MessageTranslationService
) : PoTranslatorService {

    private val logger = LoggerFactory.getLogger(PoTranslatorServiceImpl::class.java)

    override fun translate(
            po: Po,
            source: String,
            target: String,
            isAsciidoctor: Boolean,
            useRag: Boolean
    ): Po {
        val messages = po.messages
        val translationTargets = messages.filter { messageTranslationService.shouldTranslate(it) }
        val messagesToBeFilledWithMessageId = messages.filter { messageTranslationService.shouldFillWithMessageId(it) }

        val specialFormatMessages = translationTargets.filter { messageTranslationService.isSpecialFormat(it) }
        val normalMessages = translationTargets.filterNot { messageTranslationService.isSpecialFormat(it) }

        translateSpecialFormats(specialFormatMessages, source, target, useRag)
        translateNormalMessages(normalMessages, source, target, isAsciidoctor, useRag)
        fillWithMessageId(messagesToBeFilledWithMessageId)

        return Po(target, messages)
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

    private fun translateStrings(messages: List<String>, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean): List<String> {
        return if(isAsciidoctor){
            val preProcessedMessages = messages.map { messageProcessor.preProcess(it) }
            val processedMessages = translator.translate(preProcessedMessages, srcLang, dstLang, useRag)
            processedMessages.map { messageProcessor.postProcess(it) }
        }
        else{
            translator.translate(messages, srcLang, dstLang, useRag)
        }
    }

    private fun translateNormalMessages(messages: List<PoMessage>, srcLang: String, dstLang: String, isAsciidoctor: Boolean, useRag: Boolean){
        if (messages.isEmpty()) return
        val translatedStrings = translateStrings(messages.map { it.messageId }, srcLang, dstLang, isAsciidoctor, useRag)
        translatedStrings.forEachIndexed { index, item -> messages[index].also {
            if(item.isNotEmpty()){
                it.messageString = item
            }
            else{
                it.messageString = it.messageId
            }
            it.fuzzy = true
        } } 
    }

    private fun translateSpecialFormats(messages: List<PoMessage>, srcLang: String, dstLang: String, useRag: Boolean){
        messages.forEach { message ->
            message.messageString = messageTranslationService.translateSpecialFormat(message.messageId, srcLang, dstLang, useRag) { texts, src, dst, rag ->
                translateStrings(texts, src, dst, false, rag)
            }
            message.fuzzy = true
        }
    }

    private fun fillWithMessageId(messages: List<PoMessage>){
        messages.forEach{
            it.messageString = it.messageId
            it.fuzzy = false
        }
    }

}
