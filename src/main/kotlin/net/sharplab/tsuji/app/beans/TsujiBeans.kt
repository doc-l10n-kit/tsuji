package net.sharplab.tsuji.app.beans

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel
import dev.langchain4j.model.googleai.GeminiThinkingConfig
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import net.sharplab.tsuji.app.config.TsujiConfig
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import java.util.Optional
import net.sharplab.tsuji.core.driver.adoc.AsciidocDriver
import net.sharplab.tsuji.core.driver.adoc.AsciidocDriverImpl
import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import net.sharplab.tsuji.core.driver.common.ExternalProcessDriverImpl
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
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiTranslationAiService
import net.sharplab.tsuji.core.driver.translator.gemini.GeminiRAGTranslationAiService
import net.sharplab.tsuji.core.driver.translator.validator.AsciidocMarkupValidator
import net.sharplab.tsuji.core.driver.vectorstore.LuceneVectorStoreDriver
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.core.driver.translator.processor.AsciidoctorPreProcessor
import net.sharplab.tsuji.core.driver.translator.adaptive.AdaptiveParallelismController
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
class TsujiBeans() {

    private val logger = LoggerFactory.getLogger(TsujiBeans::class.java)

    @Produces
    @ApplicationScoped
    fun chatModel(
        tsujiConfig: TsujiConfig,
        @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.api-key") apiKey: String,
        @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.chat-model.model-id") modelId: String,
        @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.chat-model.max-output-tokens") maxOutputTokens: Int,
        @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.timeout") timeout: Duration,
        @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.log-requests") logRequests: Boolean,
        @ConfigProperty(name = "quarkus.langchain4j.ai.gemini.log-responses") logResponses: Boolean
    ): ChatModel {
        val builder = GoogleAiGeminiChatModel.builder()
            .apiKey(apiKey)
            .modelName(modelId)
            .maxOutputTokens(maxOutputTokens)
            .timeout(timeout)
            .logRequests(logRequests)
            .logResponses(logResponses)

        tsujiConfig.translator.gemini.thinkingLevel.ifPresent { level ->
            logger.info("Using Gemini thinking level: $level")
            builder.thinkingConfig(
                GeminiThinkingConfig.builder()
                    .thinkingLevel(level)
                    .includeThoughts(false)
                    .build()
            )
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
    fun tmxService(): TmxService {
        return TmxServiceImpl()
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
    fun asciidocDriver(asciidoctor: Asciidoctor): AsciidocDriver {
        return AsciidocDriverImpl(asciidoctor)
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
            initialConcurrency = tsujiConfig.translator.gemini.adaptive.initialConcurrency,
            minConcurrency = tsujiConfig.translator.gemini.adaptive.minConcurrency,
            maxConcurrency = tsujiConfig.translator.gemini.adaptive.maxConcurrency,
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
    fun translator(
        tsujiConfig: TsujiConfig,
        asciidoctorPreProcessor: AsciidoctorPreProcessor,
        geminiTranslationAiService: GeminiTranslationAiService,
        geminiRAGTranslationAiService: GeminiRAGTranslationAiService,
        adaptiveParallelismController: AdaptiveParallelismController,
        asciidocMarkupValidator: AsciidocMarkupValidator
    ): Translator {
        return when (tsujiConfig.translator.type.lowercase()) {
            "deepl" -> {
                logger.info("Using DeepL Translator")
                DeepLTranslator(
                    tsujiConfig.translator.deepl.key.get(),
                    asciidoctorPreProcessor,
                    adaptiveParallelismController,
                    tsujiConfig.translator.gemini.adaptive.maxRetries
                )
            }
            "gemini" -> {
                logger.info("Using Gemini Translator")
                GeminiTranslator(
                    geminiTranslationAiService,
                    geminiRAGTranslationAiService,
                    tsujiConfig.translator.gemini.batch.initialTextsPerRequest,
                    tsujiConfig.translator.gemini.batch.maxTextsPerRequest,

                    tsujiConfig.translator.gemini.adaptive.maxRetries,
                    adaptiveParallelismController,
                    asciidocMarkupValidator
                )
            }
            else -> {
                logger.warn("Unknown translator type: ${tsujiConfig.translator.type}, defaulting to Gemini")
                GeminiTranslator(
                    geminiTranslationAiService,
                    geminiRAGTranslationAiService,
                    tsujiConfig.translator.gemini.batch.initialTextsPerRequest,
                    tsujiConfig.translator.gemini.batch.maxTextsPerRequest,

                    tsujiConfig.translator.gemini.adaptive.maxRetries,
                    adaptiveParallelismController,
                    asciidocMarkupValidator
                )
            }
        }
    }
}
