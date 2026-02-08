package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.Po
import java.nio.file.Path

/**
 * Service for core PO file operations and domain logic.
 */
interface PoService {
    /**
     * Checks if the given PO file path should be ignored during bulk operations.
     */
    fun isIgnored(path: Path): Boolean

    /**
     * Resolves the master (source) file path from a PO file path.
     */
    fun resolveMasterPath(poPath: Path): Path

    /**
     * Calculates translation statistics for a given PO model.
     */
    fun calculateStats(po: Po): PoStats

    /**
     * Counts words in a string.
     */
    fun countWords(text: String): Int
}

