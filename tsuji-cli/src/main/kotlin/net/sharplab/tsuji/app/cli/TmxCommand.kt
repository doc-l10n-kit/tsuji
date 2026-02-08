package net.sharplab.tsuji.app.cli

import io.quarkus.arc.Unremovable
import picocli.CommandLine
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.tmx.GenerateTmxCommand

@Dependent
@Unremovable
@CommandLine.Command(
    name = "tmx",
    mixinStandardHelpOptions = true,
    description = ["Commands for TMX file operations"],
    subcommands = [
        GenerateTmxCommand::class
    ]
)
class TmxCommand