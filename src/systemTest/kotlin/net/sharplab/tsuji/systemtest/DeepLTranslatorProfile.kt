package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Test profile for DeepL translator comparison tests.
 */
class DeepLTranslatorProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "tsuji.translator.type" to "deepl"
        )
    }
}
