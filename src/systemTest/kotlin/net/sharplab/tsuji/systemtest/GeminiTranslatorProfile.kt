package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Test profile for Gemini translator comparison tests.
 */
class GeminiTranslatorProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "tsuji.translator.type" to "gemini"
        )
    }
}
