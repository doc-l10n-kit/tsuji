package net.sharplab.tsuji.app.cli.jekyll

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.JekyllAppService
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(name = "serve", mixinStandardHelpOptions = true, description = ["Serves the translated Jekyll site"])
class JekyllServeCommand(private val jekyllAppService: JekyllAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--translate"], description = ["Whether to apply translation (default: true)"], negatable = true, defaultValue = "true")
    var translate: Boolean = true

    @CommandLine.Option(names = ["--additional-configs", "-c"], description = ["Additional Jekyll configuration files"])
    var additionalConfigs: List<String>? = null

    override fun execute() {
        jekyllAppService.serve(translate = translate, additionalConfigs = additionalConfigs)
    }
}