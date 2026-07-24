package net.sharplab.tsuji.core.service

/**
 * Service for generic site-agnostic domain logic.
 */
interface SiteService {
    /**
     * Determines if override file is up-to-date compared to upstream.
     * Returns "OK" if override is newer or equal, "NG" if override is older.
     */
    fun determineOverrideStatus(overrideEpoch: Long, upstreamEpoch: Long): String
}
