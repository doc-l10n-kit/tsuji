package net.sharplab.tsuji.core.service

class JekyllServiceImpl : JekyllService {

    override fun determineOverrideStatus(overrideEpoch: Long, upstreamEpoch: Long): String {
        return if (overrideEpoch >= upstreamEpoch) "OK" else "NG"
    }
}
