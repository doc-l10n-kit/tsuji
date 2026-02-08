package net.sharplab.tsuji.core.processor

import org.jsoup.nodes.Element

interface TagPostProcessor {
    fun postProcess(message: Element)
}
