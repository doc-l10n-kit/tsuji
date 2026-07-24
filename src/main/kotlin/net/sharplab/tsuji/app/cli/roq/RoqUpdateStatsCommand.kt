package net.sharplab.tsuji.app.cli.roq

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.RoqAppService
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(name = "update-stats", description = ["Updates all Roq related statistics (PO and overrides)"])
class RoqUpdateStatsCommand(private val roqAppService: RoqAppService) : BaseCommand() {

    override fun execute() {
        roqAppService.updateRoqStats()
    }
}
