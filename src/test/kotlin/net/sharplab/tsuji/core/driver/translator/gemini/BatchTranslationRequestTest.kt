package net.sharplab.tsuji.core.driver.translator.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BatchTranslationRequestTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `BatchTranslationRequest serializes to indexed JSON format`() {
        val request = BatchTranslationRequest(listOf("text1", "text2\nwith newline", "text3"))

        val json = mapper.writeValueAsString(request)
        println("Serialized JSON: $json")

        // Should serialize to {"0": "text1", "1": "text2\nwith newline", "2": "text3"}
        val deserialized = mapper.readValue<Map<String, String>>(json)

        assertThat(deserialized).hasSize(3)
        assertThat(deserialized["0"]).isEqualTo("text1")
        assertThat(deserialized["1"]).isEqualTo("text2\nwith newline")
        assertThat(deserialized["2"]).isEqualTo("text3")
    }

    @Test
    fun `BatchTranslationResponse deserializes from indexed JSON format`() {
        val json = """{"0": "翻訳1", "1": "翻訳2\n改行付き", "2": "翻訳3"}"""

        val response = mapper.readValue<BatchTranslationResponse>(json)

        assertThat(response.translations).hasSize(3)
        assertThat(response.translations[0]).isEqualTo("翻訳1")
        assertThat(response.translations[1]).isEqualTo("翻訳2\n改行付き")
        assertThat(response.translations[2]).isEqualTo("翻訳3")
    }

    @Test
    fun `BatchTranslationResponse keys validation`() {
        val json = """{"0": "翻訳1", "1": "翻訳2", "2": "翻訳3"}"""

        val response = mapper.readValue<BatchTranslationResponse>(json)

        assertThat(response.keys).isEqualTo(setOf("0", "1", "2"))
    }

    @Test
    fun `Round trip serialization and deserialization`() {
        val originalTexts = listOf("first", "second\nwith\nnewlines", "third")
        val request = BatchTranslationRequest(originalTexts)

        // Serialize request
        val requestJson = mapper.writeValueAsString(request)
        println("Request JSON: $requestJson")

        // Simulate LLM response with same keys
        val responseJson = """{"0": "最初", "1": "2番目\n改行付き", "2": "3番目"}"""

        // Deserialize response
        val response = mapper.readValue<BatchTranslationResponse>(responseJson)

        // Verify
        assertThat(response.translations).hasSize(3)
        assertThat(response.keys).isEqualTo(setOf("0", "1", "2"))
    }
}
