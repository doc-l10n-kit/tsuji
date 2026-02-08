package net.sharplab.tsuji.app.beans

import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.RetrievalAugmentor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import net.sharplab.tsuji.app.config.TsujiConfig
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
import net.sharplab.tsuji.core.driver.vectorstore.InMemoryVectorStoreDriver
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import net.sharplab.tsuji.core.processor.AsciidoctorMessageProcessor
import net.sharplab.tsuji.core.service.IndexingService
import net.sharplab.tsuji.core.service.IndexingServiceImpl
import net.sharplab.tsuji.core.service.MessageTranslationService
import net.sharplab.tsuji.core.service.MessageTranslationServiceImpl
import net.sharplab.tsuji.core.service.PoNormalizerService
import net.sharplab.tsuji.core.service.PoNormalizerServiceImpl
import net.sharplab.tsuji.core.service.PoService
import net.sharplab.tsuji.core.service.PoServiceImpl
import net.sharplab.tsuji.core.service.TmxService
import net.sharplab.tsuji.core.service.TmxServiceImpl
import net.sharplab.tsuji.tmx.TmxCodec
import org.asciidoctor.Asciidoctor
import org.asciidoctor.log.Severity
import org.slf4j.LoggerFactory

@Dependent
class TsujiBeans() {

    private val logger = LoggerFactory.getLogger(TsujiBeans::class.java)

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
    fun messageTranslationService(): MessageTranslationService {
        return MessageTranslationServiceImpl()
    }

    @Produces
    @Singleton
    fun asciidoctor(): Asciidoctor {
        val asciidoctor = Asciidoctor.Factory.create()
        asciidoctor.registerLogHandler { logRecord ->
            @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
            when (logRecord.severity) {
                Severity.DEBUG -> logger.debug("{}: {}", logRecord.cursor, logRecord.message)
                Severity.INFO -> logger.info("{}: {}", logRecord.cursor, logRecord.message)
                Severity.WARN -> logger.warn("Asciidoc WARN: {}: {}", logRecord.cursor, logRecord.message)
                Severity.ERROR -> logger.warn("Asciidoc ERROR: {}: {}", logRecord.cursor, logRecord.message)
                Severity.FATAL -> logger.warn("Asciidoc FATAL: {}: {}", logRecord.cursor, logRecord.message)
                Severity.UNKNOWN -> logger.warn("Asciidoc UNKNOWN: {}: {}", logRecord.cursor, logRecord.message)
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
    fun asciidoctorMessageProcessor(asciidoctor: Asciidoctor): AsciidoctorMessageProcessor {
        return AsciidoctorMessageProcessor(asciidoctor)
    }

    fun shutdownAsciidoctorMessageProcessor(@Disposes processor: AsciidoctorMessageProcessor) {
        processor.close()
    }

    @Produces
    fun poNormalizerService(asciidoctor: Asciidoctor): PoNormalizerService {
        return PoNormalizerServiceImpl(asciidoctor)
    }

    @Produces
    fun externalProcessDriver(): ExternalProcessDriver {
        return ExternalProcessDriverImpl()
    }

    @Produces
    fun jekyllDriver(externalProcessDriver: ExternalProcessDriver) : JekyllDriver {
        return JekyllDriverImpl(externalProcessDriver)
    }

    @Produces
    fun po4aDriver(externalProcessDriver: ExternalProcessDriver) : Po4aDriver {
        return Po4aDriverImpl(externalProcessDriver)
    }

    @Produces
    fun poDriver() : PoDriver {
        return PoDriverImpl()
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
    fun vectorStoreDriver(embeddingModel: EmbeddingModel, tsujiConfig: TsujiConfig): VectorStoreDriver {
        return InMemoryVectorStoreDriver(embeddingModel, tsujiConfig.rag.indexPath)
    }

    @Produces
    fun retrievalAugmentor(vectorStoreDriver: VectorStoreDriver): RetrievalAugmentor {
        return DefaultRetrievalAugmentor.builder()
            .contentRetriever(vectorStoreDriver.asContentRetriever())
            .build()
    }
}