package net.sharplab.tsuji.app.cli

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.rag.IndexCommand
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(
    name = "rag",
    description = ["RAG (Retrieval-Augmented Generation) related commands"],
    subcommands = [
        IndexCommand::class
    ],
    mixinStandardHelpOptions = true
)
class RagCommand