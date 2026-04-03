package net.sharplab.tsuji.core.driver.common

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

class ExternalProcessDriverImpl : ExternalProcessDriver {

    private val logger = LoggerFactory.getLogger(ExternalProcessDriverImpl::class.java)

    override fun execute(
        command: List<String>,
        directory: Path?,
        env: Map<String, String>,
        timeoutValue: Long,
        timeoutUnit: TimeUnit
    ) {
        logger.debug("Executing command: {}", command.joinToString(" "))

        val processBuilder = ProcessBuilder(command).apply {
            if (directory != null) {
                directory(directory.toFile())
            }
            environment().putAll(env)
            inheritIO()
        }

        val process = processBuilder.start()
        val finished = process.waitFor(timeoutValue, timeoutUnit)

        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            throw RuntimeException("Command failed with exit code $exitCode: ${command.joinToString(" ")}")
        }
    }

    override fun executeAndGetOutput(
        command: List<String>,
        directory: Path?,
        env: Map<String, String>,
        timeoutValue: Long,
        timeoutUnit: TimeUnit
    ): String {
        logger.debug("Executing command and capturing output: {}", command.joinToString(" "))

        val processBuilder = ProcessBuilder(command).apply {
            if (directory != null) {
                directory(directory.toFile())
            }
            environment().putAll(env)
            redirectError(ProcessBuilder.Redirect.PIPE)
            redirectOutput(ProcessBuilder.Redirect.PIPE)
        }

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val errorOutput = process.errorStream.bufferedReader().readText()
        val finished = process.waitFor(timeoutValue, timeoutUnit)

        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            throw RuntimeException("Command failed with exit code $exitCode: ${command.joinToString(" ")}\nError: $errorOutput")
        }

        return output.trim()
    }
}
