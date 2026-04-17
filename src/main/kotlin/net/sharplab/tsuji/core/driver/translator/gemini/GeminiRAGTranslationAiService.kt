package net.sharplab.tsuji.core.driver.translator.gemini

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
import dev.langchain4j.rag.query.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.app.config.toPromptText
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

class GeminiRAGTranslationAiService(
    private val chatModel: ChatModel,
    private val vectorStoreDriver: VectorStoreDriver,
    private val config: TsujiConfig
) {

    private val logger = LoggerFactory.getLogger(GeminiRAGTranslationAiService::class.java)
    private val mapper = jacksonObjectMapper()

    private val translationSystemPrompt: String by lazy {
        config.translator.gemini.prompts.ragBatchSystemPrompt
            .map { path -> java.io.File(path).readText() }
            .orElseGet { loadClasspathPrompt("prompts/translation-rag-system-prompt.txt") }
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

    // Retrieve RAG context for a single text as structured translation memory entries
    private fun retrieveContextForText(text: String): List<TranslationMemoryEntry> {
        val retriever = vectorStoreDriver.asContentRetriever(
            maxResults = config.rag.maxResults,
            minScore = config.rag.minScore
        )
        val query = Query.from(text)
        val contents = retriever.retrieve(query)

        return contents.mapNotNull { content ->
            val originalText = content.textSegment().text()
            val translationText = content.textSegment().metadata()?.getString("target")

            if (translationText != null) {
                TranslationMemoryEntry(original = originalText, translation = translationText)
            } else {
                null
            }
        }
    }

    suspend fun translate(
        texts: List<String>,
        srcLang: String,
        dstLang: String
    ): List<String> {
        val items = texts.mapIndexed { index, text ->
            BatchTranslationRequestItem(index, text)
        }
        return translateWithNotes(items, srcLang, dstLang)
    }

    internal suspend fun translateWithNotes(
        items: List<BatchTranslationRequestItem>,
        srcLang: String,
        dstLang: String
    ): List<String> {
        val systemPrompt = buildSystemPrompt(translationSystemPrompt, srcLang, dstLang)

        // Retrieve RAG context for each text individually
        val indexedItems = items.map { item ->
            val ragContext = retrieveContextForText(item.text)
            RAGBatchTranslationRequestItem(item.index, item.text, ragContext, item.note)
        }

        val requestJson = mapper.writeValueAsString(indexedItems)
        val userPrompt = "Translate the following JSON array. Each item has a 'tm' field with relevant translation memory. Return a JSON array with the SAME indices:\n$requestJson"

        val chatRequest = ChatRequest.builder()
            .messages(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
            )
            .responseFormat(createBatchResponseSchema())
            .build()

        logger.debug("Sending RAG batch translation request (batch size: ${items.size})")

        val apiStartTime = System.currentTimeMillis()
        val response = withContext(Dispatchers.IO) {
            chatModel.chat(chatRequest)
        }
        val apiElapsedTime = System.currentTimeMillis() - apiStartTime

        logger.debug("RAG batch API call completed in ${apiElapsedTime}ms (batch size: ${items.size})")

        val batchResponse = parseBatchResponse(response.aiMessage().text())
        validateIndices(items.map { it.index }.toSet(), batchResponse)
        return toTranslations(batchResponse)
    }

    companion object {
        private val BATCH_RESPONSE_TYPE_REF =
            object : com.fasterxml.jackson.core.type.TypeReference<List<BatchTranslationResponseItem>>() {}
    }

    private fun parseBatchResponse(responseJson: String): List<BatchTranslationResponseItem> {
        return try {
            // Response is wrapped: {"translations": [...]}
            val tree = mapper.readTree(responseJson)
            val translationsNode = tree.get("translations") ?: throw IllegalStateException("Missing 'translations' field in response")
            mapper.readValue(translationsNode.toString(), BATCH_RESPONSE_TYPE_REF)
        } catch (e: Exception) {
            logger.error("Failed to parse RAG batch translation response: $responseJson", e)
            throw e
        }
    }

    private fun validateIndices(expectedIndices: Set<Int>, batchResponse: List<BatchTranslationResponseItem>) {
        val actualIndices = batchResponse.map { it.index }.toSet()
        if (expectedIndices != actualIndices) {
            val missing = expectedIndices - actualIndices
            val extra = actualIndices - expectedIndices
            logger.error("RAG batch index mismatch: expected=$expectedIndices, actual=$actualIndices, missing=$missing, extra=$extra")
            throw IllegalStateException("RAG batch translation index mismatch: missing=$missing, extra=$extra")
        }
    }

    private fun toTranslations(batchResponse: List<BatchTranslationResponseItem>): List<String> {
        return batchResponse.sortedBy { it.index }.map { it.translation }
    }

    private fun createBatchResponseSchema(): ResponseFormat {
        val itemSchema = JsonObjectSchema.builder()
            .addIntegerProperty("index", "Index of the translation (0-based)")
            .addStringProperty("translation", "Translated text")
            .required(listOf("index", "translation"))
            .build()

        val arraySchema = JsonArraySchema.builder()
            .items(itemSchema)
            .description("Array of translation results")
            .build()

        // Wrap the array in an object for unified structure
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
