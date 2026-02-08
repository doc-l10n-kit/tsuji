package net.sharplab.tsuji.core.driver.adoc

import org.asciidoctor.Asciidoctor
import org.asciidoctor.Attributes
import org.asciidoctor.Options
import org.asciidoctor.ast.Document
import java.nio.file.Path

class AsciidocDriverImpl(private val asciidoctor: Asciidoctor) : AsciidocDriver {

    override fun load(path: Path): Document {
        val attributes = Attributes.builder()
            .attribute("skip-front-matter")
            .build()

        val options = Options.builder()
            .sourcemap(true)
            .catalogAssets(true)
            .attributes(attributes)
            .build()
        return asciidoctor.loadFile(path.toFile(), options)
    }

    override fun loadString(content: String, path: String?): Document {
        val attributesBuilder = Attributes.builder()
            .attribute("skip-front-matter")
        if (path != null) {
            attributesBuilder.attribute("docfile", path)
        }
        val attributes = attributesBuilder.build()

        val options = Options.builder()
            .sourcemap(true)
            .catalogAssets(true)
            .attributes(attributes)
            .build()
        return asciidoctor.load(content, options)
    }
}
