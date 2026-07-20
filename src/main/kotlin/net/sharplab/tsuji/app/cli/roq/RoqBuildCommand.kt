package net.sharplab.tsuji.app.cli.roq

import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import net.sharplab.tsuji.app.cli.BaseCommand
import net.sharplab.tsuji.app.service.RoqAppService
import picocli.CommandLine

@Dependent
@Unremovable
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = ["Builds the translated Roq site"])
class RoqBuildCommand(private val roqAppService: RoqAppService) : BaseCommand() {

    @CommandLine.Option(names = ["--translate"], description = ["Whether to apply translation (default: true)"], negatable = true, defaultValue = "true")
    var translate: Boolean = true

    @CommandLine.Option(names = ["--profile", "-p"], description = ["Quarkus profile to use for the build"])
    var profile: String? = null

    @CommandLine.Option(names = ["--skip-asciidoc"], description = ["Skip AsciiDoc po4a translation (use when a Roq i18n extension handles AsciiDoc in-AST)"], defaultValue = "false")
    var skipAsciidoc: Boolean = false

    override fun execute() {
        roqAppService.build(translate = translate, profile = profile, skipAsciidoc = skipAsciidoc)
    }
}
