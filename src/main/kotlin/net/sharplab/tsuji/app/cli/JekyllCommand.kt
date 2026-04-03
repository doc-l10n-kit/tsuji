package net.sharplab.tsuji.app.cli

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.jekyll.JekyllBuildCommand
import net.sharplab.tsuji.app.cli.jekyll.JekyllExtractCommand
import net.sharplab.tsuji.app.cli.jekyll.JekyllServeCommand
import net.sharplab.tsuji.app.cli.jekyll.JekyllUpdateStatsCommand
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(
    name = "jekyll",
    mixinStandardHelpOptions = true,
    description = ["Commands for Jekyll site building and serving"],
    subcommands = [
        JekyllBuildCommand::class,
        JekyllExtractCommand::class,
        JekyllServeCommand::class,
        JekyllUpdateStatsCommand::class
    ]
)
class JekyllCommand