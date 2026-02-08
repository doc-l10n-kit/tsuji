package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po

interface PoNormalizerService {

    fun normalize(po: Po): Po
}