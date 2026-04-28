package net.sharplab.tsuji.app.beans

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel
import dev.langchain4j.model.googleai.GeminiThinkingConfig
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import net.sharplab.tsuji.app.config.TsujiConfig
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import java.util.Optional
import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import net.sharplab.tsuji.core.driver.common.ExternalProcessDriverImpl
import net.sharplab.tsuji.core.driver.git.GitTimestampDriver
import net.sharplab.tsuji.core.driver.git.GitTimestampDriverImpl
import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import net.sharplab.tsuji.core.driver.gettext.GettextDriverImpl
import net.sharplab.tsuji.core.driver.jekyll.JekyllDriver
import net.sharplab.tsuji.core.driver.jekyll.JekyllDriverImpl
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.core.driver.po4a.Po4aDriver
import net.sharplab.tsuji.core.driver.po4a.Po4aDriverImpl
import net.sharplab.tsuji.core.driver.tmx.TmxDriver
import net.sharplab.tsuji.core.driver.tmx.TmxDriverImpl
import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.driver.translator.deepl.DeepLTranslator
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslator
import net.sharplab.tsuji.core.driver.translator.openai.OpenAiTranslator
import net.sharplab.tsuji.core.driver.translator.service.RAGTranslationAiService
import net.sharplab.tsuji.core.driver.translator.service.TranslationAiService
import net.sharplab.tsuji.core.driver.translator.validator.AsciidocMarkupValidator
import net.sharplab.tsuji.core.driver.vectorstore.LuceneVectorStoreDriver
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.core.driver.translator.processor.AsciidoctorPreProcessor
import net.sharplab.tsuji.core.driver.translator.processor.GeminiTranslationProcessor
import net.sharplab.tsuji.core.driver.translator.processor.OpenAiTranslationProcessor
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
import net.sharplab.tsuji.core.driver.translator.util.MessageTypeNoteGenerator
import net.sharplab.tsuji.core.service.IndexingService
import net.sharplab.tsuji.core.service.IndexingServiceImpl
import net.sharplab.tsuji.core.service.JekyllService
import net.sharplab.tsuji.core.service.JekyllServiceImpl
import net.sharplab.tsuji.core.service.PoNormalizerService
import net.sharplab.tsuji.core.service.PoNormalizerServiceImpl
import net.sharplab.tsuji.core.service.PoService
import net.sharplab.tsuji.core.service.PoServiceImpl
import net.sharplab.tsuji.core.service.TmxService
import net.sharplab.tsuji.core.service.TmxServiceImpl
import net.sharplab.tsuji.po.PoCodec
import net.sharplab.tsuji.tmx.TmxCodec
import org.asciidoctor.Asciidoctor
import org.asciidoctor.log.Severity
import org.slf4j.LoggerFactory

@Dependent
class TsujiBeans(
    private val config: org.eclipse.microprofile.config.Config
) {

    private val logger = LoggerFactory.getLogger(TsujiBeans::class.java)

    companion object {
        // OpenAI property names
        private const val QUARKUS_LANGCHAIN4J_OPENAI_API_KEY = "quarkus.langchain4j.openai.api-key"

        private const val QUARKUS_LANGCHAIN4J_OPENAI_BASE_URL = "quarkus.langchain4j.openai.base-url"
        private const val QUARKUS_LANGCHAIN4J_OPENAI_TIMEOUT = "quarkus.langchain4j.openai.timeout"
        private const val QUARKUS_LANGCHAIN4J_OPENAI_MAX_COMPLETION_TOKENS = "quarkus.langchain4j.openai.chat-model.max-completion-tokens"
        private const val QUARKUS_LANGCHAIN4J_OPENAI_LOG_REQUESTS = "quarkus.langchain4j.openai.log-requests"
        private const val QUARKUS_LANGCHAIN4J_OPENAI_LOG_RESPONSES = "quarkus.langchain4j.openai.log-responses"

        // Gemini property names
        private const val QUARKUS_LANGCHAIN4J_GEMINI_API_KEY = "quarkus.langchain4j.ai.gemini.api-key"
        private const val QUARKUS_LANGCHAIN4J_GEMINI_MAX_OUTPUT_TOKENS = "quarkus.langchain4j.ai.gemini.chat-model.max-output-tokens"
        private const val QUARKUS_LANGCHAIN4J_GEMINI_TIMEOUT = "quarkus.langchain4j.ai.gemini.timeout"
        private const val QUARKUS_LANGCHAIN4J_GEMINI_LOG_REQUESTS = "quarkus.langchain4j.ai.gemini.log-requests"
        private const val QUARKUS_LANGCHAIN4J_GEMINI_LOG_RESPONSES = "quarkus.langchain4j.ai.gemini.log-responses"
    }

    private fun createStandardGeminiChatModel(tsujiConfig: TsujiConfig): ChatModel {
        val modelConfig = tsujiConfig.translator.gemini.model
        return createGeminiChatModel(tsujiConfig, modelConfig)
            ?: throw IllegalStateException("Gemini model ID is not configured. Set tsuji.translator.gemini.model.model-id")
    }

    private fun createEscalationGeminiChatModel(tsujiConfig: TsujiConfig): ChatModel? {
        return createGeminiChatModel(tsujiConfig, tsujiConfig.translator.gemini.escalationModel)
    }

    private fun createGeminiChatModel(tsujiConfig: TsujiConfig, modelConfig: TsujiConfig.Translator.Gemini.ModelConfig): ChatModel? {
        val modelId = modelConfig.modelId.orElse(null) ?: return null
        val resolvedApiKey = tsujiConfig.translator.gemini.key
            .or { config.getOptionalValue(QUARKUS_LANGCHAIN4J_GEMINI_API_KEY, String::class.java) }
            .orElseThrow {
                IllegalStateException(
                    "Gemini API key is not configured. " +
                    "Set tsuji.translator.gemini.key or '$QUARKUS_LANGCHAIN4J_GEMINI_API_KEY'"
                )
            }

        val maxOutputTokens = config.getOptionalValue(QUARKUS_LANGCHAIN4J_GEMINI_MAX_OUTPUT_TOKENS, Int::class.java)
            .orElse(65536)
        val timeout = config.getOptionalValue(QUARKUS_LANGCHAIN4J_GEMINI_TIMEOUT, Duration::class.java)
            .orElse(Duration.ofSeconds(300))
        val logRequests = config.getOptionalValue(QUARKUS_LANGCHAIN4J_GEMINI_LOG_REQUESTS, Boolean::class.java)
            .orElse(false)
        val logResponses = config.getOptionalValue(QUARKUS_LANGCHAIN4J_GEMINI_LOG_RESPONSES, Boolean::class.java)
            .orElse(false)

        val builder = GoogleAiGeminiChatModel.builder()
            .apiKey(resolvedApiKey)
            .modelName(modelId)
            .maxOutputTokens(maxOutputTokens)
            .timeout(timeout)
            .logRequests(logRequests)
            .logResponses(logResponses)

        val thinking = modelConfig.thinking
        thinking.thinkingBudget.ifPresent { budget ->
            logger.info("Using Gemini thinking budget: $budget")
            builder.thinkingConfig(
                GeminiThinkingConfig.builder()
                    .thinkingBudget(budget)
                    .includeThoughts(false)
                    .build()
            )
        }
        if (thinking.thinkingBudget.isEmpty) {
            thinking.thinkingLevel.ifPresent { level ->
                logger.info("Using Gemini thinking level: $level")
                builder.thinkingConfig(
                    GeminiThinkingConfig.builder()
                        .thinkingLevel(level)
                        .includeThoughts(false)
                        .build()
                )
            }
        }

        return builder.build()
    }

    private fun createStandardOpenAiChatModel(tsujiConfig: TsujiConfig): ChatModel {
        return createOpenAiChatModel(tsujiConfig, tsujiConfig.translator.openai.model)
            ?: throw IllegalStateException("OpenAI model ID is not configured. Set tsuji.translator.openai.model.model-id")
    }

    private fun createEscalationOpenAiChatModel(tsujiConfig: TsujiConfig): ChatModel? {
        return createOpenAiChatModel(tsujiConfig, tsujiConfig.translator.openai.escalationModel)
    }

    private fun createOpenAiChatModel(tsujiConfig: TsujiConfig, modelConfig: TsujiConfig.Translator.OpenAi.ModelConfig): ChatModel? {
        val modelId = modelConfig.modelId.orElse(null) ?: return null
        val resolvedApiKey = tsujiConfig.translator.openai.key
            .or { config.getOptionalValue(QUARKUS_LANGCHAIN4J_OPENAI_API_KEY, String::class.java) }
            .orElseThrow {
                IllegalStateException(
                    "OpenAI API key is not configured. " +
                    "Set tsuji.translator.openai.key or '$QUARKUS_LANGCHAIN4J_OPENAI_API_KEY'"
                )
            }

        val maxCompletionTokens = config.getOptionalValue(QUARKUS_LANGCHAIN4J_OPENAI_MAX_COMPLETION_TOKENS, Int::class.java)
            .orElse(65536)
        val timeout = config.getOptionalValue(QUARKUS_LANGCHAIN4J_OPENAI_TIMEOUT, Duration::class.java)
            .orElse(Duration.ofSeconds(300))
        val baseUrl = config.getOptionalValue(QUARKUS_LANGCHAIN4J_OPENAI_BASE_URL, String::class.java)
        val logRequests = config.getOptionalValue(QUARKUS_LANGCHAIN4J_OPENAI_LOG_REQUESTS, Boolean::class.java)
            .orElse(false)
        val logResponses = config.getOptionalValue(QUARKUS_LANGCHAIN4J_OPENAI_LOG_RESPONSES, Boolean::class.java)
            .orElse(false)

        val builder = OpenAiChatModel.builder()
            .apiKey(resolvedApiKey)
            .modelName(modelId)
            .maxCompletionTokens(maxCompletionTokens)
            .timeout(timeout)
            .logRequests(logRequests)
            .logResponses(logResponses)

        baseUrl.ifPresent { url ->
            logger.info("Using custom OpenAI base URL: $url")
            builder.baseUrl(url)
        }

        return builder.build()
    }

    @Produces
    fun poService(): PoService {
        return PoServiceImpl()
    }

    @Produces
    fun indexingService(): IndexingService {
        return IndexingServiceImpl()
    }

    @Produces
    fun tmxService(tsujiConfig: TsujiConfig): TmxService {
        return TmxServiceImpl(tsujiConfig.language.from, tsujiConfig.language.to)
    }

    @Produces
    fun jekyllService(): JekyllService {
        return JekyllServiceImpl()
    }

    @Produces
    @Singleton
    fun asciidoctor(): Asciidoctor {
        val asciidoctorLogger = LoggerFactory.getLogger("org.asciidoctor")
        val asciidoctor = Asciidoctor.Factory.create()
        asciidoctor.registerLogHandler { logRecord ->
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (logRecord.severity) {
                Severity.DEBUG -> asciidoctorLogger.debug("{}: {}", logRecord.cursor, logRecord.message)
                Severity.INFO -> asciidoctorLogger.info("{}: {}", logRecord.cursor, logRecord.message)
                Severity.WARN -> asciidoctorLogger.warn("{}: {}", logRecord.cursor, logRecord.message)
                Severity.ERROR -> asciidoctorLogger.error("{}: {}", logRecord.cursor, logRecord.message)
                Severity.FATAL -> asciidoctorLogger.error("FATAL: {}: {}", logRecord.cursor, logRecord.message)
                Severity.UNKNOWN -> asciidoctorLogger.warn("UNKNOWN: {}: {}", logRecord.cursor, logRecord.message)
            }
        }
        return asciidoctor
    }

    fun shutdownAsciidoctor(@Disposes asciidoctor: Asciidoctor) {
        asciidoctor.shutdown()
    }

    @Produces
    @ApplicationScoped
    fun asciidoctorPreProcessor(asciidoctor: Asciidoctor): AsciidoctorPreProcessor {
        return AsciidoctorPreProcessor(asciidoctor)
    }

    fun shutdownAsciidoctorPreProcessor(@Disposes processor: AsciidoctorPreProcessor) {
        processor.close()
    }

    @Produces
    fun poNormalizerService(poDriver: PoDriver, gettextDriver: GettextDriver): PoNormalizerService {
        return PoNormalizerServiceImpl(poDriver, gettextDriver)
    }

    @Produces
    fun externalProcessDriver(): ExternalProcessDriver {
        return ExternalProcessDriverImpl()
    }

    @Produces
    fun gitTimestampDriver(externalProcessDriver: ExternalProcessDriver): GitTimestampDriver {
        return GitTimestampDriverImpl(externalProcessDriver)
    }

    @Produces
    fun jekyllDriver(externalProcessDriver: ExternalProcessDriver, tsujiConfig: TsujiConfig) : JekyllDriver {
        return JekyllDriverImpl(externalProcessDriver, tsujiConfig.jekyll.jekyllL10nBranch)
    }

    @Produces
    fun po4aDriver(externalProcessDriver: ExternalProcessDriver) : Po4aDriver {
        return Po4aDriverImpl(externalProcessDriver)
    }

    @Produces
    fun poDriver() : PoDriver {
        return PoDriverImpl(PoCodec())
    }

    @Produces
    fun gettextDriver(externalProcessDriver: ExternalProcessDriver) : GettextDriver {
        return GettextDriverImpl(externalProcessDriver)
    }

    @Produces
    fun tmxDriver() : TmxDriver {
        return TmxDriverImpl(TmxCodec())
    }

    @Produces
    fun embeddingModel(): EmbeddingModel {
        return AllMiniLmL6V2QuantizedEmbeddingModel()
    }

    @Produces
    @ApplicationScoped
    fun vectorStoreDriver(embeddingModel: EmbeddingModel, tsujiConfig: TsujiConfig): VectorStoreDriver {
        logger.info("Using Lucene VectorStore")
        return LuceneVectorStoreDriver(embeddingModel, tsujiConfig.rag.indexPath)
    }

    @Produces
    @Singleton
    fun adaptiveParallelismController(tsujiConfig: TsujiConfig): AdaptiveParallelismController {
        return AdaptiveParallelismController(
            initialConcurrency = tsujiConfig.translator.adaptive.initialConcurrency,
            minConcurrency = tsujiConfig.translator.adaptive.minConcurrency,
            maxConcurrency = tsujiConfig.translator.adaptive.maxConcurrency,
            rateLimitExceptionClass = net.sharplab.tsuji.core.driver.translator.exception.RateLimitException::class
        )
    }

    @Produces
    @ApplicationScoped
    fun asciidocMarkupValidator(asciidoctor: Asciidoctor): AsciidocMarkupValidator {
        return AsciidocMarkupValidator(asciidoctor)
    }

    @Produces
    @ApplicationScoped
    fun messageTypeNoteGenerator(tsujiConfig: TsujiConfig): MessageTypeNoteGenerator {
        return MessageTypeNoteGenerator(
            headingNote = tsujiConfig.translator.note.heading.orElse(null)
        )
    }

    @Produces
    @ApplicationScoped
    fun translator(
        tsujiConfig: TsujiConfig,
        asciidoctorPreProcessor: AsciidoctorPreProcessor,
        vectorStoreDriver: VectorStoreDriver,
        adaptiveParallelismController: AdaptiveParallelismController,
        asciidocMarkupValidator: AsciidocMarkupValidator,
        messageTypeNoteGenerator: MessageTypeNoteGenerator
    ): Translator {
        return when (tsujiConfig.translator.type.lowercase()) {
            "deepl" -> {
                logger.info("Using DeepL Translator")
                val deeplKey = tsujiConfig.translator.deepl.key.orElseThrow {
                    IllegalStateException("DeepL API key is not configured. Set tsuji.translator.deepl.key.")
                }
                DeepLTranslator(
                    deeplKey,
                    asciidoctorPreProcessor,
                    adaptiveParallelismController,
                    tsujiConfig.translator.adaptive.maxRetries
                )
            }
            "gemini" -> {
                logger.info("Using Gemini Translator")
                val chatModel = createStandardGeminiChatModel(tsujiConfig)
                val escalationChatModel = createEscalationGeminiChatModel(tsujiConfig) ?: chatModel
                val promptPath = tsujiConfig.translator.gemini.prompts.systemPrompt.orElse(null)
                val asciidocRulesPath = tsujiConfig.translator.gemini.prompts.asciidocMarkupRules.orElse(null)
                val htmlRulesPath = tsujiConfig.translator.gemini.prompts.htmlMarkupRules.orElse(null)
                val translationAiService = TranslationAiService(chatModel, escalationChatModel, tsujiConfig, promptPath, asciidocRulesPath, htmlRulesPath)
                val ragTranslationAiService = RAGTranslationAiService(chatModel, escalationChatModel, vectorStoreDriver, tsujiConfig, promptPath, asciidocRulesPath, htmlRulesPath)

                val geminiTranslationProcessor = GeminiTranslationProcessor(
                    translationAiService,
                    ragTranslationAiService,
                    tsujiConfig.translator.gemini.batch.initialTextsPerRequest,
                    tsujiConfig.translator.gemini.batch.maxTextsPerRequest,
                    tsujiConfig.translator.adaptive.maxRetries,
                    tsujiConfig.translator.adaptive.maxMessageValidationRetries,
                    adaptiveParallelismController,
                    asciidocMarkupValidator,
                    messageTypeNoteGenerator
                )
                GeminiTranslator(geminiTranslationProcessor)
            }
            "openai" -> {
                logger.info("Using OpenAI Translator")
                val chatModel = createStandardOpenAiChatModel(tsujiConfig)
                val escalationChatModel = createEscalationOpenAiChatModel(tsujiConfig) ?: chatModel
                val promptPath = tsujiConfig.translator.openai.prompts.systemPrompt.orElse(null)
                val asciidocRulesPath = tsujiConfig.translator.openai.prompts.asciidocMarkupRules.orElse(null)
                val htmlRulesPath = tsujiConfig.translator.openai.prompts.htmlMarkupRules.orElse(null)
                val translationAiService = TranslationAiService(chatModel, escalationChatModel, tsujiConfig, promptPath, asciidocRulesPath, htmlRulesPath)
                val ragTranslationAiService = RAGTranslationAiService(chatModel, escalationChatModel, vectorStoreDriver, tsujiConfig, promptPath, asciidocRulesPath, htmlRulesPath)

                val mtTag = tsujiConfig.translator.openai.mtTag.orElse("openai")
                val openAiTranslationProcessor = OpenAiTranslationProcessor(
                    translationAiService,
                    ragTranslationAiService,
                    mtTag,
                    tsujiConfig.translator.openai.batch.initialTextsPerRequest,
                    tsujiConfig.translator.openai.batch.maxTextsPerRequest,
                    tsujiConfig.translator.adaptive.maxRetries,
                    tsujiConfig.translator.adaptive.maxMessageValidationRetries,
                    adaptiveParallelismController,
                    asciidocMarkupValidator,
                    messageTypeNoteGenerator
                )
                OpenAiTranslator(openAiTranslationProcessor, tsujiConfig.translator.openai.mtTag.orElse(null))
            }
            else -> {
                throw IllegalArgumentException(
                    "Unknown translator type: ${tsujiConfig.translator.type}. " +
                    "Supported types: deepl, gemini, openai"
                )
            }
        }
    }
}
