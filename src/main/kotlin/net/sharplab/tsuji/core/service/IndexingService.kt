package net.sharplab.tsuji.core.service

import dev.langchain4j.data.segment.TextSegment
import net.sharplab.tsuji.tmx.model.Tmx

/**
 * Service for core indexing operations, such as converting TMX models to AI text segments.
 */
interface IndexingService {
    /**
     * Converts a TMX model into a list of TextSegments suitable for embedding.
     */
    fun convertToSegments(tmx: Tmx): List<TextSegment>
}
