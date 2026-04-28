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

        // Use the actual target language from PO files instead of the config value,
        // so that the TMX language tag matches the PO file's Language header (e.g. "ja_JP").
        var resolvedTargetLang = targetLang
        pos.forEach { po ->
            resolvedTargetLang = po.target
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
                    TranslationUnitVariant(resolvedTargetLang, dst)
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
