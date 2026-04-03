package net.sharplab.tsuji.core.model.po

import java.io.File

/**
 * Domain model representing a message in a PO file.
 */
class PoMessage(
    val type: MessageType,
    val messageId: String,
    val messageString: String,
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
     * Returns true if this message is the PO file header (msgid is empty).
     */
    val isHeader: Boolean
        get() = messageId.isEmpty()

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

    /**
     * Creates a copy of this PoMessage.
     */
    fun copy(
        type: MessageType = this.type,
        messageId: String = this.messageId,
        messageString: String = this.messageString,
        sourceReferences: List<SourceReference> = this.sourceReferences,
        flags: MutableSet<PoFlag> = this._flags.toMutableSet(),
        comments: List<String> = this.comments
    ): PoMessage {
        return PoMessage(type, messageId, messageString, sourceReferences, flags, comments)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoMessage) return false

        if (type != other.type) return false
        if (messageId != other.messageId) return false
        if (messageString != other.messageString) return false
        if (sourceReferences != other.sourceReferences) return false
        if (_flags != other._flags) return false
        if (comments != other.comments) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + messageId.hashCode()
        result = 31 * result + messageString.hashCode()
        result = 31 * result + sourceReferences.hashCode()
        result = 31 * result + _flags.hashCode()
        result = 31 * result + comments.hashCode()
        return result
    }

    override fun toString(): String {
        return "PoMessage(type=$type, messageId='$messageId', messageString='$messageString', sourceReferences=$sourceReferences, flags=$flags, comments=$comments)"
    }

    data class SourceReference(val file: File, val lineNumber: Int? = null)
}