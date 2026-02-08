package net.sharplab.tsuji.core.model.po

/**
 * Domain model representing a PO file.
 */
data class Po(
    val target: String,
    val messages: List<PoMessage>
)