package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.PoAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "update-stats", description = ["Updates PO translation statistics"])
class UpdatePoStatsCommand(private val poAppService: PoAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--po", "-p"], description = ["The PO directories to analyze"], required = true)
    lateinit var po: List<Path>

    @CommandLine.Option(names = ["--output", "-o"], description = ["The output CSV file path"], required = true)
    lateinit var output: Path

    override fun execute() {
        poAppService.updatePoStats(po, output)
    }
}