package net.sharplab.tsuji.app.cli.jekyll

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.JekyllAppService
import picocli.CommandLine
import java.nio.file.Path

@Dependent
@Unremovable
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = ["Builds the translated Jekyll site"])
class JekyllBuildCommand(private val jekyllAppService: JekyllAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--translate"], description = ["Whether to apply translation (default: true)"], negatable = true, defaultValue = "true")
    var translate: Boolean = true

    @CommandLine.Option(names = ["--additional-configs", "-c"], description = ["Additional Jekyll configuration files"])
    var additionalConfigs: List<String>? = null

    override fun execute() {
        jekyllAppService.build(translate = translate, additionalConfigs = additionalConfigs)
    }
}
