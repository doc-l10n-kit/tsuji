package net.sharplab.tsuji.app.cli.jekyll

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.JekyllAppService
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(name = "update-stats", description = ["Updates all Jekyll related statistics (PO and overrides)"])
class JekyllUpdateStatsCommand(private val jekyllAppService: JekyllAppService) : BaseCommand() {

    override fun execute() {
        jekyllAppService.updateJekyllStats()
    }
}
