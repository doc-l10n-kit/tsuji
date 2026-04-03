package net.sharplab.tsuji.core.driver.rag

import dev.langchain4j.model.input.PromptTemplate
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.rag.content.injector.DefaultContentInjector
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import java.util.function.Supplier

@ApplicationScoped
class TsujiRetrievalAugmentorSupplier : Supplier<RetrievalAugmentor> {

    @Inject
    lateinit var vectorStoreDriver: VectorStoreDriver

    override fun get(): RetrievalAugmentor {
        val promptTemplate = PromptTemplate.from(
            "{{userMessage}}\n\n" +
            "Reference Translation Memory (previous translations):\n" +
            "{{contents}}"
        )

        val contentInjector = DefaultContentInjector.builder()
            .promptTemplate(promptTemplate)
            .build()

        return DefaultRetrievalAugmentor.builder()
            .contentRetriever(vectorStoreDriver.asContentRetriever())
            .contentInjector(contentInjector)
            .build()
    }
}
