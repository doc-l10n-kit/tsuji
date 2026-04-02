package net.sharplab.tsuji.core.service

import java.nio.file.Path

/**
 * PO file normalization service.
 * Standardizes PO file format (remove obsolete entries, encoding, line breaks, etc.).
 */
interface PoNormalizerService {

    /**
     * Normalizes a PO file.
     *
     * @param path Path to the PO file
     */
    fun normalize(path: Path)
}