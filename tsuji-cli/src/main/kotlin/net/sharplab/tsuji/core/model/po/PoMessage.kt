package net.sharplab.tsuji.core.model.po

import java.io.File

/**
 * Domain model representing a message in a PO file.
 */
data class PoMessage(
    val type: MessageType,
    val messageId: String,
    var messageString: String,
    val sourceReferences: List<SourceReference>,
    var fuzzy: Boolean
) {
    data class SourceReference(val file: File, val lineNumber: Int)
}