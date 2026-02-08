package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.PoAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "apply", mixinStandardHelpOptions = true, description = ["Applies PO file to master file to generate localized file"])
class ApplyCommand(private val poAppService: PoAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--master", "-m"], description = ["The master file"], required = true)
    lateinit var master: Path

    @CommandLine.Option(names = ["--po", "-p"], description = ["The PO file"], required = true)
    lateinit var po: Path

    @CommandLine.Option(names = ["--localized", "-l"], description = ["The output localized file path"], required = true)
    lateinit var localized: Path

    @CommandLine.Option(names = ["--format", "-f"], description = ["File format (markdown, yaml, xhtml, etc.)"], defaultValue = "text")
    lateinit var format: String

    override fun execute() {
        poAppService.apply(master, po, localized, format)
    }
}