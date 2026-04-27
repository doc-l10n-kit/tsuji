package net.sharplab.tsuji.app.service

/**
 * Controls AsciiDoc inline markup pre/post-processing during translation.
 */
enum class AsciidocMode {
    /** Auto-detect from PO filename: .adoc.po files are processed as AsciiDoc, others are not. */
    AUTO,
    /** Always apply AsciiDoc processing regardless of filename. */
    ALWAYS,
    /** Never apply AsciiDoc processing regardless of filename. */
    NEVER
}
