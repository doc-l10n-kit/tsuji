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
    private val _flags: MutableSet<PoFlag> = mutableSetOf(),
    val comments: List<String> = emptyList()
) {
    /**
     * Returns an immutable view of the flags.
     */
    val flags: Set<PoFlag>
        get() = _flags.toSet()

    /**
     * Convenience property for fuzzy flag.
     * Getter checks if Fuzzy flag is in flags set.
     * Setter adds or removes Fuzzy flag from flags set.
     */
    var fuzzy: Boolean
        get() = _flags.contains(PoFlag.Fuzzy)
        set(value) {
            if (value) {
                _flags.add(PoFlag.Fuzzy)
            } else {
                _flags.remove(PoFlag.Fuzzy)
            }
        }

    data class SourceReference(val file: File, val lineNumber: Int)
}