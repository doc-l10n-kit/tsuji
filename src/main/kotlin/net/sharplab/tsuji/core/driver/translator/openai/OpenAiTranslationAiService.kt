package net.sharplab.tsuji.core.driver.translator.openai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.chat.request.ResponseFormatType
import dev.langchain4j.model.chat.request.json.JsonArraySchema
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sharplab.tsuji.app.config.toPromptText
import net.sharplab.tsuji.core.driver.translator.gemini.BatchTranslationRequestItem
import net.sharplab.tsuji.core.driver.translator.gemini.BatchTranslationResponseItem
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

class OpenAiTranslationAiService(
    private val chatModel: ChatModel,
    private val config: net.sharplab.tsuji.app.config.TsujiConfig
) {

    private val logger = LoggerFactory.getLogger(OpenAiTranslationAiService::class.java)
    private val mapper = jacksonObjectMapper()

    private val translationSystemPrompt: String by lazy {
        config.translator.openai.prompts.batchSystemPrompt
            .map { path -> java.io.File(path).readText() }
            .orElseGet { loadClasspathPrompt("prompts/translation-system-prompt.txt") }
    }

    private fun loadClasspathPrompt(resourcePath: String): String {
        return javaClass.classLoader.getResourceAsStream(resourcePath)?.use {
            InputStreamReader(it).readText()
        } ?: throw IllegalStateException("Failed to load prompt from $resourcePath")
    }

    private fun buildSystemPrompt(template: String, srcLang: String, dstLang: String): String {
        val glossaryText = config.glossary.toPromptText()

        return template
            .replace("{srcLang}", srcLang)
            .replace("{dstLang}", dstLang)
            .replace("{glossary}", glossaryText)
    }

    companion object {
        private val BATCH_RESPONSE_TYPE_REF =
            object : com.fasterxml.jackson.core.type.TypeReference<List<BatchTranslationResponseItem>>() {}
    }

    suspend fun translate(
        texts: List<String>,
        srcLang: String,
        dstLang: String
    ): List<String> {
        val systemPrompt = buildSystemPrompt(translationSystemPrompt, srcLang, dstLang)
        val requestJson = serializeBatchRequest(texts)
        val userPrompt = "Translate the following JSON array. Return a JSON array with the SAME indices:\n$requestJson"

        val chatRequest = ChatRequest.builder()
            .messages(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
            )
            .responseFormat(createBatchResponseSchema())
            .build()

        logger.debug("Sending batch translation request with array-based schema (batch size: ${texts.size})")

        val apiStartTime = System.currentTimeMillis()
        val response = withContext(Dispatchers.IO) {
            chatModel.chat(chatRequest)
        }
        val apiElapsedTime = System.currentTimeMillis() - apiStartTime

        logger.debug("API call completed in ${apiElapsedTime}ms (batch size: ${texts.size})")

        val batchResponse = parseBatchResponse(response.aiMessage().text())

        validateIndices(texts.indices.toSet(), batchResponse)

        return toTranslations(batchResponse)
    }

    private fun serializeBatchRequest(texts: List<String>): String {
        val items = texts.mapIndexed { index, text ->
            BatchTranslationRequestItem(index, text)
        }
        return mapper.writeValueAsString(items)
    }

    private fun parseBatchResponse(responseJson: String): List<BatchTranslationResponseItem> {
        return try {
            // OpenAI returns wrapped response: {"translations": [...]}
            val tree = mapper.readTree(responseJson)
            val translationsNode = tree.get("translations") ?: throw IllegalStateException("Missing 'translations' field in response")
            mapper.readValue(translationsNode.toString(), BATCH_RESPONSE_TYPE_REF)
        } catch (e: Exception) {
            logger.error("Failed to parse batch translation response: $responseJson", e)
            throw e
        }
    }

    private fun validateIndices(expectedIndices: Set<Int>, batchResponse: List<BatchTranslationResponseItem>) {
        val actualIndices = batchResponse.map { it.index }.toSet()

        if (expectedIndices != actualIndices) {
            val missing = expectedIndices - actualIndices
            val extra = actualIndices - expectedIndices
            logger.error("Index mismatch: expected=$expectedIndices, actual=$actualIndices, missing=$missing, extra=$extra")
            throw IllegalStateException("Translation index mismatch: missing=$missing, extra=$extra")
        }
    }

    private fun toTranslations(batchResponse: List<BatchTranslationResponseItem>): List<String> {
        return batchResponse.sortedBy { it.index }.map { it.translation }
    }

    /**
     * Creates a JSON Schema for batch translation compatible with OpenAI API.
     * OpenAI requires the root element to be an object, not an array.
     */
    private fun createBatchResponseSchema(): ResponseFormat {
        // Define the schema for each array element: {"index": Int, "translation": String}
        val itemSchema = JsonObjectSchema.builder()
            .addIntegerProperty("index", "Index of the translation (0-based)")
            .addStringProperty("translation", "Translated text")
            .required(listOf("index", "translation"))
            .build()

        // Define the array schema
        val arraySchema = JsonArraySchema.builder()
            .items(itemSchema)
            .description("Array of translation results")
            .build()

        // Wrap the array in an object (OpenAI requirement)
        val rootSchema = JsonObjectSchema.builder()
            .addProperty("translations", arraySchema)
            .required(listOf("translations"))
            .build()

        val jsonSchema = JsonSchema.builder()
            .name("BatchTranslationResponse")
            .rootElement(rootSchema)
            .build()

        return ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(jsonSchema)
            .build()
    }
}
