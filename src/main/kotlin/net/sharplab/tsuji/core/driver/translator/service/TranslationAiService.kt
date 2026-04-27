package net.sharplab.tsuji.core.driver.translator.service

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
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.app.config.toPromptText
import net.sharplab.tsuji.core.driver.translator.exception.IndexMismatchException
import net.sharplab.tsuji.core.driver.translator.model.BatchTranslationRequestItem
import net.sharplab.tsuji.core.driver.translator.model.BatchTranslationResponseItem
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.util.Optional

/**
 * LLM-based batch translation service.
 * Handles batch translation with JSON schema validation, prompt templating, and index verification.
 */
class TranslationAiService(
    private val chatModel: ChatModel,
    private val config: TsujiConfig,
    customPromptPath: Optional<String>,
    customAsciidocMarkupRulesPath: Optional<String> = Optional.empty(),
    customHtmlMarkupRulesPath: Optional<String> = Optional.empty()
) {

    private val logger = LoggerFactory.getLogger(TranslationAiService::class.java)
    private val mapper = jacksonObjectMapper()

    private val translationSystemPrompt: String by lazy {
        customPromptPath
            .map { path -> java.io.File(path).readText() }
            .orElseGet { loadClasspathPrompt("prompts/translation-system-prompt.txt") }
    }

    private val asciidocMarkupRules: String by lazy {
        customAsciidocMarkupRulesPath
            .map { path -> java.io.File(path).readText() }
            .orElseGet { loadClasspathPrompt("prompts/asciidoc-markup-rules.txt") }
    }

    private val htmlMarkupRules: String by lazy {
        customHtmlMarkupRulesPath
            .map { path -> java.io.File(path).readText() }
            .orElseGet { loadClasspathPrompt("prompts/html-markup-rules.txt") }
    }

    private fun loadClasspathPrompt(resourcePath: String): String {
        return javaClass.classLoader.getResourceAsStream(resourcePath)?.use {
            InputStreamReader(it).readText()
        } ?: throw IllegalStateException("Failed to load prompt from $resourcePath")
    }

    private fun buildSystemPrompt(template: String, srcLang: String, dstLang: String, isAsciidoctor: Boolean): String {
        val glossaryText = config.glossary.toPromptText()
        val additionalRules = if (isAsciidoctor) asciidocMarkupRules else htmlMarkupRules

        return template
            .replace("{srcLang}", srcLang)
            .replace("{dstLang}", dstLang)
            .replace("{glossary}", glossaryText)
            .replace("{additional_rules}", additionalRules)
    }

    companion object {
        private val BATCH_RESPONSE_TYPE_REF =
            object : com.fasterxml.jackson.core.type.TypeReference<List<BatchTranslationResponseItem>>() {}
    }

    suspend fun translate(
        texts: List<String>,
        srcLang: String,
        dstLang: String,
        isAsciidoctor: Boolean
    ): List<String> {
        val items = texts.mapIndexed { index, text ->
            BatchTranslationRequestItem(index, text)
        }
        return translateWithNotes(items, srcLang, dstLang, isAsciidoctor)
    }

    internal suspend fun translateWithNotes(
        items: List<BatchTranslationRequestItem>,
        srcLang: String,
        dstLang: String,
        isAsciidoctor: Boolean
    ): List<String> {
        val systemPrompt = buildSystemPrompt(translationSystemPrompt, srcLang, dstLang, isAsciidoctor)
        val requestJson = mapper.writeValueAsString(items)
        val userPrompt = "Translate the following JSON array. Return a JSON array with the SAME indices:\n$requestJson"

        val chatRequest = ChatRequest.builder()
            .messages(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
            )
            .responseFormat(createBatchResponseSchema())
            .build()

        logger.debug("Sending batch translation request with array-based schema (batch size: ${items.size})")

        val apiStartTime = System.currentTimeMillis()
        val response = withContext(Dispatchers.IO) {
            chatModel.chat(chatRequest)
        }
        val apiElapsedTime = System.currentTimeMillis() - apiStartTime

        logger.debug("API call completed in ${apiElapsedTime}ms (batch size: ${items.size})")

        val batchResponse = parseBatchResponse(response.aiMessage().text())

        validateIndices(items.map { it.index }.toSet(), batchResponse)

        return toTranslations(batchResponse)
    }

    private fun parseBatchResponse(responseJson: String): List<BatchTranslationResponseItem> {
        return try {
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
            throw IndexMismatchException("Translation index mismatch: missing=$missing, extra=$extra", expectedIndices, actualIndices)
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
