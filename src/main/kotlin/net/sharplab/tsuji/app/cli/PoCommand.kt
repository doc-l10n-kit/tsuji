package net.sharplab.tsuji.app.cli

import io.quarkus.arc.Unremovable
import picocli.CommandLine
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.po.ApplyCommand
import net.sharplab.tsuji.app.cli.po.ApplyFuzzyTmxCommand
import net.sharplab.tsuji.app.cli.po.ApplyTmxCommand
import net.sharplab.tsuji.app.cli.po.MachineTranslateCommand
import net.sharplab.tsuji.app.cli.po.NormalizeCommand
import net.sharplab.tsuji.app.cli.po.PurgeCommand
import net.sharplab.tsuji.app.cli.po.RemoveObsoleteCommand
import net.sharplab.tsuji.app.cli.po.UpdateCommand
import net.sharplab.tsuji.app.cli.po.UpdatePoStatsCommand

@Dependent
@Unremovable
@CommandLine.Command(
    name = "po",
    mixinStandardHelpOptions = true,
    description = ["Commands for PO file operations"],
    subcommands = [
        NormalizeCommand::class,
        PurgeCommand::class,
        RemoveObsoleteCommand::class,
        UpdateCommand::class,
        ApplyCommand::class,
        ApplyTmxCommand::class,
        ApplyFuzzyTmxCommand::class,
        MachineTranslateCommand::class,
        UpdatePoStatsCommand::class
    ]
)
class PoCommand
