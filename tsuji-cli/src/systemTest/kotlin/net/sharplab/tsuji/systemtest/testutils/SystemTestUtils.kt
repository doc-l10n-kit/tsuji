package net.sharplab.tsuji.systemtest.testutils

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

object SystemTestUtils {

    private val logger = LoggerFactory.getLogger(SystemTestUtils::class.java)

    fun prepareTestDir(): Path {
        val stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        val caller = stackWalker.walk { frames ->
            frames.skip(1) // skip prepareTestDir
                .findFirst()
                .orElseThrow { RuntimeException("Caller not found") }
        }
        val className = caller.declaringClass.simpleName
        val methodName = caller.methodName

        val buildDir = Paths.get("build/tmp/systemTest/$className/$methodName")
        if (buildDir.exists()) {
            Files.walk(buildDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.delete(it) }
        }
        buildDir.createDirectories()
        logger.info("Test temporary directory: ${buildDir.toAbsolutePath()}")
        return buildDir
    }

    fun copyTestResources(destinationDir: Path, vararg fileNames: String) {
        val stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        val caller = stackWalker.walk { frames ->
            frames.skip(1) // skip copyTestResources
                .findFirst()
                .orElseThrow { RuntimeException("Caller not found") }
        }
        val className = caller.declaringClass.simpleName
        val methodName = caller.methodName

        fileNames.forEach { fileName ->
            val resourcePath = "/$className/$methodName/$fileName"
            val resourceStream = caller.declaringClass.getResourceAsStream(resourcePath)
                ?: throw IllegalArgumentException("Resource not found: $resourcePath")

            val destPath = destinationDir.resolve(fileName)
            Files.copy(resourceStream, destPath)
        }
    }

    fun gitClone(workingDir: Path, url: String, depth: Int? = null) {
        val command = mutableListOf("git", "clone")
        if (depth != null) {
            command.add("--depth")
            command.add(depth.toString())
        }
        command.add(url)
        runCommand(workingDir, *command.toTypedArray())
    }

    fun runCommand(workingDir: Path, vararg command: String, timeoutMinutes: Long = 10) {
        val process = ProcessBuilder(*command)
            .directory(workingDir.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val completed = process.waitFor(timeoutMinutes, TimeUnit.MINUTES)
        if (!completed) {
            process.destroy()
            throw RuntimeException("Command timed out: ${command.joinToString(" ")}")
        }
        if (process.exitValue() != 0) {
            throw RuntimeException("Command failed with exit code ${process.exitValue()}: ${command.joinToString(" ")}")
        }
    }
}
