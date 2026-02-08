package net.sharplab.tsuji.core.service

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import net.sharplab.tsuji.tmx.model.Tmx

class IndexingServiceImpl : IndexingService {

    override fun convertToSegments(tmx: Tmx): List<TextSegment> {
        val srcLang = tmx.tmxHeader.srcLang
        val segments = mutableListOf<TextSegment>()

        tmx.tmxBody.translationUnits?.forEach { tu ->
            val sourceText = tu.variants.find { it.lang == srcLang }?.seg ?: return@forEach
            tu.variants.filter { it.lang != srcLang }.forEach { variant ->
                // Create a TextSegment from source text and add target translation as metadata
                val segment = TextSegment.from(sourceText, Metadata.from("target", variant.seg).put("lang", variant.lang))
                segments.add(segment)
            }
        }
        return segments
    }
}
