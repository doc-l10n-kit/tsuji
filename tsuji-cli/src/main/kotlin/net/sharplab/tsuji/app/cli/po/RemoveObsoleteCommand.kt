package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.PoAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "remove-obsolete", mixinStandardHelpOptions = true, description = ["Removes obsolete PO files"])
class RemoveObsoleteCommand(private val poAppService: PoAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--po", "-p"], description = ["The PO directory to clean up"], required = true)
    lateinit var po: Path

    @CommandLine.Option(names = ["--upstream", "-u"], description = ["The upstream directory for reference"], required = true)
    lateinit var upstreamDir: Path

    override fun execute() {
        poAppService.removeObsolete(po, upstreamDir)
    }
}
