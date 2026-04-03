package net.sharplab.tsuji.po.model

/**
 * Domain model representing a PO file.
 */
data class Po(
    val target: String,
    val messages: List<PoMessage>,
    val header: Map<String, String> = emptyMap()
)
