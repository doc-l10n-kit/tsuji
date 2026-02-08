package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import net.sharplab.tsuji.tmx.model.*

class TmxServiceImpl : TmxService {

    override fun createTmxFromPos(pos: List<Po>, mode: TmxGenerationMode): Tmx {
        val translations = mutableMapOf<String, String>()
        var targetLang = "ja_JP"

        pos.forEach { po ->
            targetLang = po.target
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
                    TranslationUnitVariant("en", src),
                    TranslationUnitVariant(targetLang, dst)
                ))
            }

        val header = TmxHeader(
            creationTool = "tsuji",
            creationToolVersion = "1.0.0",
            segType = "sentence",
            oTmf = "UTF-8",
            adminLang = "en",
            srcLang = "en",
            dataType = "PlainText"
        )
        val body = TmxBody(translationUnits)
        return Tmx("1.4", header, body)
    }
}
