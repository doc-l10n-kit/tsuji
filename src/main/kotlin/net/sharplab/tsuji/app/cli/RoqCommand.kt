package net.sharplab.tsuji.app.cli

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.roq.RoqBuildCommand
import net.sharplab.tsuji.app.cli.roq.RoqExtractCommand
import net.sharplab.tsuji.app.cli.roq.RoqServeCommand
import net.sharplab.tsuji.app.cli.roq.RoqUpdateStatsCommand
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(
    name = "roq",
    mixinStandardHelpOptions = true,
    description = ["Commands for Roq site building and serving"],
    subcommands = [
        RoqBuildCommand::class,
        RoqExtractCommand::class,
        RoqServeCommand::class,
        RoqUpdateStatsCommand::class
    ]
)
class RoqCommand
