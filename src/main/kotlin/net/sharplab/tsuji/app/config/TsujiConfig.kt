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

        @get:WithName("max-results")
        @get:WithDefault("3")
        val maxResults: Int

        @get:WithName("min-score")
        @get:WithDefault("0.5")
        val minScore: Double
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

        @get:WithName("cname")
        val cname: Optional<String>

        @get:WithName("surge-domain-suffix")
        val surgeDomainSuffix: Optional<String>

        @get:WithName("jekyll-l10n-branch")
        @get:WithDefault("main")
        val jekyllL10nBranch: String

        @get:WithName("extract")
        val extract: Extract

        interface Extract {
            @get:WithName("yaml")
            val yaml: Yaml

            @get:WithName("html")
            val html: Html

            interface Yaml {
                @get:WithName("exclude")
                val exclude: Optional<List<String>>
            }

            interface Html {
                @get:WithName("include")
                val include: Optional<List<String>>
            }
        }
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

    @get:WithName("translator")
    val translator: Translator

    interface Translator {
        @get:WithName("type")
        @get:WithDefault("deepl")
        val type: String

        @get:WithName("target-directories")
        val targetDirectories: Optional<List<String>>

        @get:WithName("deepl")
        val deepl: DeepL

        interface DeepL {
            @get:WithName("key")
            val key: Optional<String>
        }

        @get:WithName("gemini")
        val gemini: Gemini

        interface Gemini {
            @get:WithName("key")
            val key: Optional<String>

            @get:WithName("model")
            @get:WithDefault("gemini-2.5-flash")
            val model: String

            @get:WithName("batch")
            val batch: Batch

            @get:WithName("adaptive")
            val adaptive: Adaptive

            @get:WithName("prompts")
            val prompts: Prompts

            interface Prompts {
                @get:WithName("batch-system-prompt")
                val batchSystemPrompt: Optional<String>

                @get:WithName("rag-batch-system-prompt")
                val ragBatchSystemPrompt: Optional<String>
            }

            interface Batch {
                @get:WithName("initial-texts-per-request")
                @get:WithDefault("200")
                val initialTextsPerRequest: Int

                @get:WithName("max-texts-per-request")
                @get:WithDefault("200")
                val maxTextsPerRequest: Int

                @get:WithName("max-text-size-bytes")
                @get:WithDefault("700000")
                val maxTextSizeBytes: Int
            }

            interface Adaptive {
                @get:WithName("initial-concurrency")
                @get:WithDefault("40")
                val initialConcurrency: Int

                @get:WithName("min-concurrency")
                @get:WithDefault("1")
                val minConcurrency: Int

                @get:WithName("max-concurrency")
                @get:WithDefault("60")
                val maxConcurrency: Int

                @get:WithName("max-retries")
                @get:WithDefault("3")
                val maxRetries: Int
            }

        }

        @get:WithName("openai")
        val openai: OpenAi

        interface OpenAi {
            @get:WithName("key")
            val key: Optional<String>

            @get:WithName("model")
            @get:WithDefault("gpt-4o-mini")
            val model: String

            @get:WithName("mt-tag")
            val mtTag: Optional<String>

            @get:WithName("batch")
            val batch: Batch

            @get:WithName("adaptive")
            val adaptive: Adaptive

            @get:WithName("prompts")
            val prompts: Prompts

            interface Prompts {
                @get:WithName("batch-system-prompt")
                val batchSystemPrompt: Optional<String>

                @get:WithName("rag-batch-system-prompt")
                val ragBatchSystemPrompt: Optional<String>
            }

            interface Batch {
                @get:WithName("initial-texts-per-request")
                @get:WithDefault("200")
                val initialTextsPerRequest: Int

                @get:WithName("max-texts-per-request")
                @get:WithDefault("200")
                val maxTextsPerRequest: Int

                @get:WithName("max-text-size-bytes")
                @get:WithDefault("700000")
                val maxTextSizeBytes: Int
            }

            interface Adaptive {
                @get:WithName("initial-concurrency")
                @get:WithDefault("40")
                val initialConcurrency: Int

                @get:WithName("min-concurrency")
                @get:WithDefault("1")
                val minConcurrency: Int

                @get:WithName("max-concurrency")
                @get:WithDefault("60")
                val maxConcurrency: Int

                @get:WithName("max-retries")
                @get:WithDefault("3")
                val maxRetries: Int
            }

        }
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

    @get:WithName("glossary")
    val glossary: Glossary

    interface Glossary {
        @get:WithName("enabled")
        @get:WithDefault("false")
        val enabled: Boolean

        @get:WithName("entries")
        @get:WithDefault("")
        val entries: List<GlossaryEntry>
    }

    interface GlossaryEntry {
        @get:WithName("term")
        val term: String

        @get:WithName("translation")
        val translation: String

        @get:WithName("context")
        val context: Optional<String>
    }
}
