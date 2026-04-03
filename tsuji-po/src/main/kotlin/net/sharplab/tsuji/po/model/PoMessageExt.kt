package net.sharplab.tsuji.po.model

/**
 * Extension property to get message type from comments.
 * Type is not part of the PO file spec, but is used by po4a and tsuji
 * as an extracted comment starting with "type:".
 */
val PoMessage.type: MessageType
    get() = comments.firstNotNullOfOrNull { MessageType.tryParse(it) } ?: MessageType.None
