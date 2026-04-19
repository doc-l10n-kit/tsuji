package net.sharplab.tsuji.core.driver.po4a

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension

class Po4aDriverImpl(private val externalProcessDriver: ExternalProcessDriver) : Po4aDriver {

    override fun determineFormat(path: Path): String? {
        val ext = path.extension.lowercase()
        return when (ext) {
            "md", "markdown" -> "text"
            "yml", "yaml" -> "yaml"
            "html" -> "xhtml"
            "adoc", "asciidoc" -> "asciidoc"
            else -> null
        }
    }

    override fun updatePo(masterFile: Path, poFile: Path, format: String, workingDirectory: Path) {
        val command = "po4a-updatepo"
        val args = mutableListOf(
            command,
            "--no-deprecation",
            "--msgmerge-opt", "--no-fuzzy-matching",
            "--master-charset", "UTF-8",
            "-f", format
        )
        if (format == "text") {
            args.add("-o")
            args.add("markdown")
            args.add("-o")
            args.add("neverwrap")
        }
        args.add("--master")
        args.add(workingDirectory.toAbsolutePath().normalize().relativize(masterFile.toAbsolutePath().normalize()).toString())
        args.add("--po")
        args.add(poFile.toAbsolutePath().toString())

        externalProcessDriver.execute(
            command = args,
            directory = workingDirectory.toAbsolutePath().normalize(),
            timeoutValue = 5,
            timeoutUnit = TimeUnit.MINUTES
        )
    }

    override fun translate(masterFile: Path, poFile: Path, localizedFile: Path, format: String, workingDirectory: Path) {
        val command = "po4a-translate"
        val args = mutableListOf(
            command,
            "--no-deprecation",
            "--master-charset", "UTF-8",
            "--localized-charset", "UTF-8",
            "-f", format,
            "--keep", "0"
        )
        if (format == "text") {
            args.add("-o")
            args.add("markdown")
            args.add("-o")
            args.add("neverwrap")
        }
        args.add("--master")
        args.add(workingDirectory.toAbsolutePath().normalize().relativize(masterFile.toAbsolutePath().normalize()).toString())
        args.add("--localized")
        args.add(workingDirectory.toAbsolutePath().normalize().relativize(localizedFile.toAbsolutePath().normalize()).toString())
        args.add("--po")
        args.add(poFile.toAbsolutePath().toString())

        externalProcessDriver.execute(
            command = args,
            directory = workingDirectory.toAbsolutePath().normalize(),
            timeoutValue = 5,
            timeoutUnit = TimeUnit.MINUTES
        )
    }
}

    