package net.sharplab.tsuji.test

import net.sharplab.tsuji.core.driver.adoc.ext.StringExt.Companion.normalizeLineBreak
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.readText

class TestUtil {

    companion object{
        fun loadFromClasspath(classpath: String): String{
            return resolveClasspath(classpath).readText(StandardCharsets.UTF_8).normalizeLineBreak()
        }

        fun resolveClasspath(classpath: String): Path {
            return Path.of(this::class.java.classLoader.getResource(classpath).toURI())
        }
    }
}