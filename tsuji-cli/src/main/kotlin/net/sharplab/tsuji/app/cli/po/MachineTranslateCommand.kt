package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.TranslationAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "machine-translate")
class MachineTranslateCommand(private val translationAppService: TranslationAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--po", "-p"], description = ["file or directory path"])
    private var po: List<Path>? = null

    @CommandLine.Option(order = 2, names = ["--source"], description = ["source language"])
    private var source: String? = null

    @CommandLine.Option(order = 3, names = ["--target"], description = ["target language"])
    private var target: String? = null

    @CommandLine.Option(order = 4, names = ["--isAsciidoc"], description = ["enable or disable asciidoc inline markup processing"])
    private var asciidoc = true

    @CommandLine.Option(order = 5, names = ["--rag"], description = ["enable or disable RAG (Retrieval-Augmented Generation)"], defaultValue = "true")
    private var rag = true

    @CommandLine.Option(order = 9, names = ["--help", "-h"], description = ["print help"], usageHelp = true)
    private var help = false

    override fun execute() {
        translationAppService.machineTranslatePoFiles(po, source, target, asciidoc, rag)
    }
}
