package net.sharplab.tsuji.app.cli.config

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.config.TsujiConfig
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(
    name = "get",
    mixinStandardHelpOptions = true,
    description = ["Get a configuration value"]
)
class ConfigGetCommand(
    private val tsujiConfig: TsujiConfig
) : BaseCommand() {

    @CommandLine.Parameters(
        index = "0",
        description = ["Configuration key (e.g., tsuji.version, tsuji.git.user.name)"]
    )
    lateinit var key: String

    override fun execute() {
        val value = getConfigValue(key)
        println(value)
    }

    private fun getConfigValue(key: String): String {
        return when (key) {
            // Version
            "tsuji.version" -> tsujiConfig.version

            // RAG
            "tsuji.rag.index-path" -> tsujiConfig.rag.indexPath

            // PO
            "tsuji.po.base-dir" -> tsujiConfig.po.baseDir

            // Jekyll
            "tsuji.jekyll.source-dir" -> tsujiConfig.jekyll.sourceDir
            "tsuji.jekyll.override-dir" -> tsujiConfig.jekyll.overrideDir
            "tsuji.jekyll.destination-dir" -> tsujiConfig.jekyll.destinationDir
            "tsuji.jekyll.stats-dir" -> tsujiConfig.jekyll.statsDir
            "tsuji.jekyll.cname" -> tsujiConfig.jekyll.cname.orElse("")
            "tsuji.jekyll.surge-domain-suffix" -> tsujiConfig.jekyll.surgeDomainSuffix.orElse("")

            // Language
            "tsuji.language.from" -> tsujiConfig.language.from
            "tsuji.language.to" -> tsujiConfig.language.to

            // Translator
            "tsuji.translator.type" -> tsujiConfig.translator.type

            // Git
            "tsuji.git.user.name" -> tsujiConfig.git.user.name.orElse("")
            "tsuji.git.user.email" -> tsujiConfig.git.user.email.orElse("")

            else -> throw IllegalArgumentException("Unknown configuration key: $key")
        }
    }
}
