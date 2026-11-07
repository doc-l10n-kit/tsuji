package net.sharplab.tsuji.core.util

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher

/**
 * Matches paths relative to a source directory against glob patterns in
 * [java.nio.file.FileSystem.getPathMatcher] syntax, where `**` crosses directory boundaries.
 * A pattern without glob metacharacters matches only that exact path.
 */
class PathPatternMatcher(patterns: List<String>) {

    private val matchers: List<PathMatcher> =
        patterns.map { FileSystems.getDefault().getPathMatcher("glob:$it") }

    fun matches(relativePath: Path): Boolean = matchers.any { it.matches(relativePath) }
}
