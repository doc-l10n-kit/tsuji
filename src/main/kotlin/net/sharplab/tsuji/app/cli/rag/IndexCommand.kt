package net.sharplab.tsuji.app.cli.rag

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.app.service.IndexingAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "index", description = ["Index Tmx file into RAG store"])
class IndexCommand(
    private val indexingAppService: IndexingAppService,
    private val tsujiConfig: TsujiConfig
) : BaseCommand() {

    @CommandLine.Option(order = 1, names = ["--tmx"], description = ["tmx file path"])
    private var tmx: Path? = null

    override fun execute() {
        val tmxPath = tmx ?: Path.of(tsujiConfig.tmx.confirmedPath)
        indexingAppService.indexTmx(tmxPath)
    }
}
