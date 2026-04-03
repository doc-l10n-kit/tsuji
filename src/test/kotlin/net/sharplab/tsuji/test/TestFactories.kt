package net.sharplab.tsuji.test

import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.PoFlag
import net.sharplab.tsuji.po.model.PoMessage

fun createPoMessage(
    id: String,
    string: String = "",
    fuzzy: Boolean = false,
    type: MessageType = MessageType.PlainText
): PoMessage {
    val flags = if (fuzzy) mutableSetOf(PoFlag.Fuzzy) else mutableSetOf()
    val comments = if (type != MessageType.None) listOf(type.value) else emptyList()
    return PoMessage(id, string, emptyList(), flags, comments)
}
