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
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

@ApplicationScoped
@Startup
class GeminiRAGTranslationAiService {

    private val logger = LoggerFactory.getLogger(GeminiRAGTranslationAiService::class.java)
    private val mapper = jacksonObjectMapper()

    @Inject
    lateinit var chatModel: ChatModel

    @Inject
    lateinit var vectorStoreDriver: VectorStoreDriver

    // Load prompt from resources
    private val translationSystemPrompt: String by lazy {
        loadPrompt("prompts/translation-rag-batch-system-prompt.txt")
    }

    private fun loadPrompt(resourcePath: String): String {
        return javaClass.classLoader.getResourceAsStream(resourcePath)?.use {
            InputStreamReader(it).readText()
        } ?: throw IllegalStateException("Failed to load prompt from $resourcePath")
    }

    private fun buildSystemPrompt(template: String, srcLang: String, dstLang: String): String {
        return template
            .replace("{srcLang}", srcLang)
            .replace("{dstLang}", dstLang)
    }

    // Retrieve RAG context for a single text as structured translation memory entries
    private fun retrieveContextForText(text: String): List<TranslationMemoryEntry> {
        val retriever = vectorStoreDriver.asContentRetriever()
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
        val systemPrompt = buildSystemPrompt(translationSystemPrompt, srcLang, dstLang)

        // Retrieve RAG context for each text individually
        val requestItems = texts.mapIndexed { index, text ->
            val ragContext = retrieveContextForText(text)
            RAGBatchTranslationRequestItem(index, text, ragContext)
        }

        val requestJson = mapper.writeValueAsString(requestItems)
        val userPrompt = "Translate the following JSON array. Each item has a 'tm' field with relevant translation memory. Return a JSON array with the SAME indices:\n$requestJson"

        val chatRequest = ChatRequest.builder()
            .messages(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
            )
            .responseFormat(createBatchResponseSchema())
            .build()

        logger.debug("Sending RAG batch translation request (batch size: ${texts.size})")

        val apiStartTime = System.currentTimeMillis()
        val response = withContext(Dispatchers.IO) {
            chatModel.chat(chatRequest)
        }
        val apiElapsedTime = System.currentTimeMillis() - apiStartTime

        logger.debug("RAG batch API call completed in ${apiElapsedTime}ms (batch size: ${texts.size})")

        val batchResponse = parseBatchResponse(response.aiMessage().text())
        validateIndices(texts.indices.toSet(), batchResponse)
        return toTranslations(batchResponse)
    }

    companion object {
        private val BATCH_RESPONSE_TYPE_REF =
            object : com.fasterxml.jackson.core.type.TypeReference<List<BatchTranslationResponseItem>>() {}
    }

    private fun parseBatchResponse(responseJson: String): List<BatchTranslationResponseItem> {
        return try {
            mapper.readValue(responseJson, BATCH_RESPONSE_TYPE_REF)
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

        val jsonSchema = JsonSchema.builder()
            .name("BatchTranslationResponse")
            .rootElement(arraySchema)
            .build()

        return ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(jsonSchema)
            .build()
    }
}
