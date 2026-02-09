package net.sharplab.tsuji.app.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import java.util.Optional

@ConfigMapping(prefix = "tsuji")
interface TsujiConfig {

    @get:WithName("version")
    @get:WithDefault("0.1.0")
    val version: String

    @get:WithName("rag")
    val rag: Rag

    interface Rag {
        @get:WithName("index-path")
        @get:WithDefault("l10n/rag/index")
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

        @get:WithName("stats-dir")
        @get:WithDefault("l10n/stats")
        val statsDir: String

        @get:WithName("additional-configs")
        val additionalConfigs: Optional<List<String>>
    }

    @get:WithName("language")
    val language: Language

    interface Language {
        @get:WithName("from")
        @get:WithDefault("en")
        val from: String

        @get:WithName("to")
        @get:WithDefault("ja")
        val to: String
    }

    @get:WithName("git")
    val git: Git

    interface Git {
        @get:WithName("user")
        val user: User

        interface User {
            @get:WithName("name")
            val name: Optional<String>

            @get:WithName("email")
            val email: Optional<String>
        }
    }
}
