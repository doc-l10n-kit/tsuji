package net.sharplab.tsuji.app.cli.roq

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.PoAppService
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(name = "extract", mixinStandardHelpOptions = true, description = ["Extract PO files from Roq project structure"])
class RoqExtractCommand(private val poAppService: PoAppService) : BaseCommand() {

    override fun execute() {
        poAppService.extractRoq()
    }
}
