package net.sharplab.tsuji.app.cli.rag

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.IndexingAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "index", description = ["Index Tmx file into RAG store"])
class IndexCommand(private val indexingAppService: IndexingAppService) : BaseCommand() {

    @CommandLine.Option(order = 1, names = ["--tmx"], description = ["tmx file path"], required = true)
    private lateinit var tmx: Path

    @CommandLine.Option(order = 2, names = ["--index-dir"], description = ["index directory path"])
    private var indexDir: Path? = null

    override fun execute() {
        indexingAppService.indexTmx(tmx, indexDir)
    }
}
