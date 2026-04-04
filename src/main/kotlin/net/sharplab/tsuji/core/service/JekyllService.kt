package net.sharplab.tsuji.core.service

/**
 * Service for Jekyll-specific domain logic.
 */
interface JekyllService {
    /**
     * Determines if override file is up-to-date compared to upstream.
     * Returns "OK" if override is newer or equal, "NG" if override is older.
     */
    fun determineOverrideStatus(overrideEpoch: Long, upstreamEpoch: Long): String
}
