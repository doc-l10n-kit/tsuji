package net.sharplab.tsuji.app.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import java.util.Optional

@ConfigMapping(prefix = "tsuji")
interface TsujiConfig {

    @get:WithName("rag")
    val rag: Rag

    interface Rag {
        @get:WithName("index-path")
        @get:WithDefault("index")
        val indexPath: String
    }

    @get:WithName("po")
    val po: Po

    interface Po {
        @get:WithName("base-dir")
        @get:WithDefault("l10n/po/ja_JP")
        val baseDir: String
    }

    @get:WithName("jekyll")
    val jekyll: Jekyll

    interface Jekyll {
        @get:WithName("source-dir")
        @get:WithDefault("upstream")
        val sourceDir: String

        @get:WithName("override-dir")
        @get:WithDefault("l10n/override/ja_JP")
        val overrideDir: String

        @get:WithName("destination-dir")
        @get:WithDefault("docs")
        val destinationDir: String

        @get:WithName("additional-configs")
        val additionalConfigs: Optional<List<String>>

        @get:WithName("language")
        val language: Optional<String>
    }

    @get:WithName("translator")
    val translator: Translator

    interface Translator {
        @get:WithName("language")
        val language: Language

        interface Language {
            @get:WithName("source")
            @get:WithDefault("en")
            val source: String

            @get:WithName("destination")
            @get:WithDefault("ja")
            val destination: String
        }
    }
}