package net.sharplab.tsuji.po.model

/**
 * Represents a flag in PO file (e.g., fuzzy, no-wrap).
 */
data class PoFlag(val value: String) {
    companion object {
        val Fuzzy = PoFlag("fuzzy")
        val NoWrap = PoFlag("no-wrap")
        val CFormat = PoFlag("c-format")
        val NoCFormat = PoFlag("no-c-format")

        fun parse(value: String): PoFlag {
            return PoFlag(value)
        }
    }
}
