package net.sharplab.tsuji.core.processor

import net.sharplab.tsuji.core.model.po.PoMessage

/**
 * Creates a copy of PoMessage with updated messageString and fuzzy flag.
 * This is necessary because fuzzy is not a constructor parameter.
 */
fun PoMessage.copyWithTranslation(messageString: String, fuzzy: Boolean): PoMessage {
    return this.copy(messageString = messageString).also {
        it.fuzzy = fuzzy
    }
}
