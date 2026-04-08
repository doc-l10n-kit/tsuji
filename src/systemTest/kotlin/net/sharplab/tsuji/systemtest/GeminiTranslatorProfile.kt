package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * Test profile for Gemini translator comparison tests.
 */
class GeminiTranslatorProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> {
        return mapOf(
            "tsuji.translator.type" to "gemini",
            // Override adaptive parallelism settings for testing
            // Without these overrides, default values (initialConcurrency=3) are used
            // which causes sequential execution bottleneck
            "tsuji.translator.gemini.adaptive.initial-concurrency" to "20",
            "tsuji.translator.gemini.adaptive.min-concurrency" to "1",
            "tsuji.translator.gemini.adaptive.max-concurrency" to "30",
            // Enable DEBUG logging for diagnosis
            "quarkus.log.category.\"net.sharplab.tsuji.core.driver.translator.adaptive\".level" to "DEBUG",
            "quarkus.log.category.\"net.sharplab.tsuji.core.driver.translator.processor\".level" to "DEBUG",
            "quarkus.log.category.\"net.sharplab.tsuji.app.service\".level" to "DEBUG"
        )
    }
}
