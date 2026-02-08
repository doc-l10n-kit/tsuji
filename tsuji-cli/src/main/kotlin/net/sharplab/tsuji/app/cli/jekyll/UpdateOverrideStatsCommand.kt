package net.sharplab.tsuji.app.cli.jekyll

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.JekyllAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "update-override-stats", description = ["Updates override files synchronization status"])
class UpdateOverrideStatsCommand(private val jekyllAppService: JekyllAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--override-dir"], description = ["The override directory"], required = true)
    lateinit var overrideDir: Path

    @CommandLine.Option(names = ["--upstream-dir"], description = ["The upstream directory"], required = true)
    lateinit var upstreamDir: Path

    @CommandLine.Option(names = ["--output", "-o"], description = ["The output CSV file path"], required = true)
    lateinit var output: Path

    override fun execute() {
        jekyllAppService.updateOverrideFilesStats(overrideDir, upstreamDir, output)
    }
}