package net.sharplab.tsuji.test

import java.nio.file.Path

class TestUtil {

    companion object{
        fun resolveClasspath(classpath: String): Path {
            return Path.of(this::class.java.classLoader.getResource(classpath)!!.toURI())
        }
    }
}
