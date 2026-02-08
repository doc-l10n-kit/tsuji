package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import net.sharplab.tsuji.tmx.model.Tmx

/**
 * Service for core TMX operations and domain logic.
 */
interface TmxService {
    /**
     * Creates a TMX model from a list of PO models based on the specified generation mode.
     */
    fun createTmxFromPos(pos: List<Po>, mode: TmxGenerationMode): Tmx
}
