package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import net.sharplab.tsuji.tmx.model.*
import org.slf4j.LoggerFactory

class TmxServiceImpl(
    private val sourceLang: String,
    private val targetLang: String
) : TmxService {

    private val logger = LoggerFactory.getLogger(TmxServiceImpl::class.java)

    override fun createTmxFromPos(pos: List<Po>, mode: TmxGenerationMode): Tmx {
        val translations = mutableMapOf<String, String>()

        pos.forEach { po ->
            if (po.target != targetLang) {
                logger.warn("PO file target language '{}' does not match configured target language '{}'", po.target, targetLang)
            }
            po.messages.forEach { msg ->
                if (msg.messageId.isEmpty()) return@forEach
                if (msg.messageString.isEmpty()) return@forEach

                val shouldInclude = when (mode) {
                    TmxGenerationMode.CONFIRMED -> !msg.fuzzy
                    TmxGenerationMode.FUZZY -> msg.fuzzy
                }

                if (shouldInclude) {
                    if (!translations.containsKey(msg.messageId)) {
                        translations[msg.messageId] = msg.messageString
                    }
                }
            }
        }

        val translationUnits = translations.entries
            .sortedBy { it.key }
            .map { (src, dst) ->
                TranslationUnit(listOf(
                    TranslationUnitVariant(sourceLang, src),
                    TranslationUnitVariant(targetLang, dst)
                ))
            }

        val header = TmxHeader(
            creationTool = "tsuji",
            creationToolVersion = "1.0.0",
            segType = "sentence",
            oTmf = "UTF-8",
            adminLang = sourceLang,
            srcLang = sourceLang,
            dataType = "PlainText"
        )
        val body = TmxBody(translationUnits)
        return Tmx("1.4", header, body)
    }
}
