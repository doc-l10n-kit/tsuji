package net.sharplab.tsuji.core.driver.translator.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BatchTranslationRequestTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `BatchTranslationRequestItem serializes to JSON object with index and text`() {
        val item = BatchTranslationRequestItem(0, "text content")

        val json = mapper.writeValueAsString(item)

        assertThat(json).contains("\"index\":0")
        assertThat(json).contains("\"text\":\"text content\"")
    }

    @Test
    fun `BatchTranslationRequestItem list serializes to JSON array format`() {
        val items = listOf("text1", "text2\nwith newline", "text3").mapIndexed { index, text ->
            BatchTranslationRequestItem(index, text)
        }

        val json = mapper.writeValueAsString(items)
        println("Serialized JSON: $json")

        // Should serialize to [{"index":0,"text":"text1"}, {"index":1,"text":"text2\nwith newline"}, ...]
        val deserialized = mapper.readValue<List<Map<String, Any>>>(json)

        assertThat(deserialized).hasSize(3)
        assertThat(deserialized[0]["index"]).isEqualTo(0)
        assertThat(deserialized[0]["text"]).isEqualTo("text1")
        assertThat(deserialized[1]["index"]).isEqualTo(1)
        assertThat(deserialized[1]["text"]).isEqualTo("text2\nwith newline")
        assertThat(deserialized[2]["index"]).isEqualTo(2)
        assertThat(deserialized[2]["text"]).isEqualTo("text3")
    }

    @Test
    fun `BatchTranslationResponseItem list deserializes from JSON array format`() {
        val json = """[{"index":0,"translation":"翻訳1"},{"index":1,"translation":"翻訳2\n改行付き"},{"index":2,"translation":"翻訳3"}]"""

        val typeRef = object : com.fasterxml.jackson.core.type.TypeReference<List<BatchTranslationResponseItem>>() {}
        val response = mapper.readValue(json, typeRef)

        assertThat(response).hasSize(3)
        assertThat(response[0]).isEqualTo(BatchTranslationResponseItem(0, "翻訳1"))
        assertThat(response[1]).isEqualTo(BatchTranslationResponseItem(1, "翻訳2\n改行付き"))
        assertThat(response[2]).isEqualTo(BatchTranslationResponseItem(2, "翻訳3"))
    }

    @Test
    fun `Round trip serialization and deserialization`() {
        val originalTexts = listOf("first", "second\nwith\nnewlines", "third")
        val items = originalTexts.mapIndexed { index, text ->
            BatchTranslationRequestItem(index, text)
        }

        // Serialize request
        val requestJson = mapper.writeValueAsString(items)
        println("Request JSON: $requestJson")

        // Simulate LLM response
        val responseJson = """[{"index":0,"translation":"最初"},{"index":1,"translation":"2番目\n改行付き"},{"index":2,"translation":"3番目"}]"""

        // Deserialize response
        val typeRef = object : com.fasterxml.jackson.core.type.TypeReference<List<BatchTranslationResponseItem>>() {}
        val response = mapper.readValue(responseJson, typeRef)

        // Verify deserialization
        assertThat(response).hasSize(3)
        assertThat(response[0]).isEqualTo(BatchTranslationResponseItem(0, "最初"))
        assertThat(response[1]).isEqualTo(BatchTranslationResponseItem(1, "2番目\n改行付き"))
        assertThat(response[2]).isEqualTo(BatchTranslationResponseItem(2, "3番目"))
    }
}
