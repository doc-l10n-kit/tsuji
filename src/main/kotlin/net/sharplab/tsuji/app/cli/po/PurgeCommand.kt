package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.PoAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "purge", mixinStandardHelpOptions = true, description = ["Purges translations in PO file(s). By default only fuzzy, use --all for all."])
class PurgeCommand(private val poAppService: PoAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--po", "-p"], description = ["The PO file or directory to process"], required = true)
    lateinit var po: Path

    @CommandLine.Option(names = ["--all", "-a"], description = ["Purge all translations including confirmed ones"], defaultValue = "false")
    var all: Boolean = false

    override fun execute() {
        if (all) {
            poAppService.purgeAll(po)
        } else {
            poAppService.purgeFuzzy(po)
        }
    }
}
