package net.sharplab.tsuji.core.driver.translator.gemini

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * Request for batch translation that serializes to indexed format {"0": "text1", "1": "text2"}.
 */
@JsonSerialize(using = BatchTranslationRequestSerializer::class)
data class BatchTranslationRequest(val texts: List<String>)

/**
 * Serializes BatchTranslationRequest to {"0": "text1", "1": "text2"} format.
 */
class BatchTranslationRequestSerializer : JsonSerializer<BatchTranslationRequest>() {
    override fun serialize(value: BatchTranslationRequest, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        value.texts.forEachIndexed { index, text ->
            gen.writeStringField(index.toString(), text)
        }
        gen.writeEndObject()
    }
}

/**
 * Response for batch translation that deserializes from indexed format {"0": "翻訳1", "1": "翻訳2"}.
 */
@JsonDeserialize(using = BatchTranslationResponseDeserializer::class)
class BatchTranslationResponse(
    val translations: List<String>,
    val keys: Set<String>
)

/**
 * Deserializes {"0": "翻訳1", "1": "翻訳2"} format to BatchTranslationResponse.
 */
class BatchTranslationResponseDeserializer : JsonDeserializer<BatchTranslationResponse>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BatchTranslationResponse {
        val map = mutableMapOf<String, String>()

        // Read object start
        if (p.currentToken != com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
            p.nextToken()
        }

        // Read all fields
        while (p.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) {
            val key = p.currentName
            p.nextToken()
            val value = p.text
            map[key] = value
        }

        // Convert to list sorted by numeric key
        val translations = map.entries
            .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
            .map { it.value }

        return BatchTranslationResponse(translations, map.keys)
    }
}
