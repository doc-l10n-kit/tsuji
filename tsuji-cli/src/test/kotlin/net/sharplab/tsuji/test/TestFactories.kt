package net.sharplab.tsuji.test

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.PoMessage

fun createPoMessage(
    id: String,
    string: String = "",
    fuzzy: Boolean = false,
    type: MessageType = MessageType.PlainText
): PoMessage {
    return PoMessage(type, id, string, emptyList(), fuzzy)
}
