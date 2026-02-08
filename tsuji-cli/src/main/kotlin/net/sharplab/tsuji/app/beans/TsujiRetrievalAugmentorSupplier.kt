package net.sharplab.tsuji.app.beans

import dev.langchain4j.rag.RetrievalAugmentor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.sharplab.tsuji.core.driver.vectorstore.VectorStoreDriver
import java.util.function.Supplier

@ApplicationScoped
class TsujiRetrievalAugmentorSupplier : Supplier<RetrievalAugmentor> {

    @Inject
    lateinit var beans: TsujiBeans

    @Inject
    lateinit var vectorStoreDriver: VectorStoreDriver

    override fun get(): RetrievalAugmentor {
        return beans.retrievalAugmentor(vectorStoreDriver)
    }
}