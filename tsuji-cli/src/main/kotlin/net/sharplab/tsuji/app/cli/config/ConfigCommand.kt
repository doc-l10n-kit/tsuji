package net.sharplab.tsuji.app.cli.config

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(
    name = "config",
    mixinStandardHelpOptions = true,
    description = ["Configuration management commands"],
    subcommands = [ConfigGetCommand::class]
)
class ConfigCommand : BaseCommand() {
    override fun execute() {
        // This method is not called when subcommands are used
        // picocli will show help if no subcommand is specified
    }
}
