package net.sharplab.tsuji.core.util

import java.util.Locale

/**
 * Utility for parsing locale strings in various formats.
 */
object LocaleUtils {
    /**
     * Parses a locale string in either BCP 47 ("ja-JP") or POSIX ("ja_JP") format
     * into a [Locale] instance.
     */
    fun parse(localeString: String): Locale = Locale.forLanguageTag(localeString.replace('_', '-'))
}
