package net.sharplab.tsuji.app.cli.po

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.PoAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "unfuzzy", mixinStandardHelpOptions = true, description = ["Removes fuzzy flags from PO file(s) while preserving translations."])
class UnfuzzyCommand(private val poAppService: PoAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--po", "-p"], description = ["The PO file or directory to process"], required = true)
    lateinit var po: Path

    override fun execute() {
        poAppService.unfuzzy(po)
    }
}
