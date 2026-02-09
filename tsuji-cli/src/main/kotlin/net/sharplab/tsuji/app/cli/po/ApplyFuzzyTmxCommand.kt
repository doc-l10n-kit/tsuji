package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.TmxAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "apply-fuzzy-tmx", description = ["Translate messages in .po file with fuzzy tmx file"])
class ApplyFuzzyTmxCommand(private val tmxAppService: TmxAppService) : BaseCommand() {

    @CommandLine.Option(order = 1, names = ["--tmx", "-t"], description = ["fuzzy tmx"], required = true)
    private lateinit var tmx: Path

    @CommandLine.Option(order = 2, names = ["--po", "-p"], description = ["po"])
    private var po: Path? = null

    @CommandLine.Option(order = 9, names = ["--help", "-h"], description = ["print help"], usageHelp = true)
    private var help = false

    override fun execute() {
        tmxAppService.applyFuzzyTmx(tmx, po)
    }
}