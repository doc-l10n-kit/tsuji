package net.sharplab.tsuji.core.driver.adoc

import org.asciidoctor.ast.Document
import java.nio.file.Path

/**
 * Driver for AsciiDoc operations using AsciidoctorJ.
 */
interface AsciidocDriver {
    fun load(path: Path): Document
    fun loadString(content: String, path: String? = null): Document
}
