package net.sharplab.tsuji.app.cli.tmx

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.TmxAppService
import net.sharplab.tsuji.core.model.tmx.TmxGenerationMode
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "generate", mixinStandardHelpOptions = true, description = ["Generates TMX from PO files"])
class GenerateTmxCommand(private val tmxAppService: TmxAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--po", "-p"], description = ["The directory containing PO files"], required = true)
    lateinit var po: Path

    @CommandLine.Option(names = ["--tmx", "-t"], description = ["The output TMX file path"], required = true)
    lateinit var tmx: Path

    @CommandLine.Option(names = ["--mode"], description = ["Generation mode: CONFIRMED or FUZZY"], defaultValue = "CONFIRMED")
    lateinit var mode: TmxGenerationMode

    override fun execute() {
        tmxAppService.generateTmx(po, tmx, mode)
    }
}