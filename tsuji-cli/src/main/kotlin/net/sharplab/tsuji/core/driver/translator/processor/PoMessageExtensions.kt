package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.core.model.po.SessionKey

/**
 * Creates a copy of PoMessage with updated messageString and fuzzy flag.
 * This is necessary because fuzzy is not a constructor parameter.
 */
fun PoMessage.copyWithTranslation(messageString: String, fuzzy: Boolean): PoMessage {
    return this.copy(messageString = messageString).also {
        it.fuzzy = fuzzy
    }
}

/**
 * Returns true if this message needs translation in the current pipeline.
 * This flag is set at the beginning of the pipeline and checked by all processors.
 */
fun PoMessage.needsTranslation(): Boolean {
    return getSession(SessionKey.NEEDS_TRANSLATION) ?: false
}
