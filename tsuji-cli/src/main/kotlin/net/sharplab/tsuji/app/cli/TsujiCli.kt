package net.sharplab.tsuji.app.cli

import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import picocli.CommandLine
import picocli.CommandLine.IFactory
import jakarta.inject.Inject

@QuarkusMain
@CommandLine.Command(name = "tsuji", subcommands = [
    PoCommand::class,
    RagCommand::class,
    TmxCommand::class,
    JekyllCommand::class
])
class TsujiCli : QuarkusApplication {

    @Inject
    lateinit var factory: IFactory

    @Throws(Exception::class)
    override fun run(vararg args: String?): Int {
        val commandLine = CommandLine(this, factory)
        commandLine.isCaseInsensitiveEnumValuesAllowed = true
        return commandLine.execute(*args)
    }

}