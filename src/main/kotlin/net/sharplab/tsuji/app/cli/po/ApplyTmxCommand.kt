package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.app.service.TmxAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "apply-tmx", description = ["Translate messages in .po file with tmx file"])
class ApplyTmxCommand(
    private val tmxAppService: TmxAppService,
    private val tsujiConfig: TsujiConfig
) : BaseCommand() {

    @CommandLine.Option(order = 1, names = ["--tmx", "-t"], description = ["tmx"])
    private var tmx: Path? = null

    @CommandLine.Option(order = 2, names = ["--po", "-p"], description = ["po"])
    private var po: Path? = null

    @CommandLine.Option(order = 9, names = ["--help", "-h"], description = ["print help"], usageHelp = true)
    private var help = false

    override fun execute() {
        val tmxPath = tmx ?: Path.of(tsujiConfig.tmx.confirmedPath)
        tmxAppService.applyConfirmedTmx(tmxPath, po)
    }
}