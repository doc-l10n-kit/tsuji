package net.sharplab.tsuji.core.service

class SiteServiceImpl : SiteService {

    override fun determineOverrideStatus(overrideEpoch: Long, upstreamEpoch: Long): String {
        return if (overrideEpoch >= upstreamEpoch) "OK" else "NG"
    }
}
