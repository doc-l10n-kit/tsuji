package net.sharplab.tsuji.core.driver.roq

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import net.sharplab.tsuji.core.util.applyOverlayFiles
import net.sharplab.tsuji.core.util.copyDirectory
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class RoqDriverImpl(
    private val externalProcessDriver: ExternalProcessDriver
) : RoqDriver {

    override fun prepareSource(sourceDir: Path, workDir: Path) {
        copyDirectory(sourceDir, workDir)
    }

    override fun applyOverrides(overrideDir: Path, workDir: Path) {
        if (!overrideDir.exists()) return
        applyOverlayFiles(overrideDir, workDir)
    }

    override fun build(roqSourceDir: Path, destinationDir: Path, profile: String?) {
        val env = mutableMapOf("QUARKUS_ROQ_GENERATOR_BATCH" to "true")
        if (profile != null) {
            env["QUARKUS_PROFILE"] = profile
        }

        externalProcessDriver.execute(
            command = listOf("mvn", "-B", "package", "quarkus:run", "-DskipTests"),
            directory = roqSourceDir,
            env = env,
            timeoutValue = 30,
            timeoutUnit = TimeUnit.MINUTES
        )
    }

    override fun serve(roqSourceDir: Path, profile: String?) {
        val env = mutableMapOf<String, String>()
        if (profile != null) {
            env["QUARKUS_PROFILE"] = profile
        }

        externalProcessDriver.execute(
            command = listOf("mvn", "quarkus:dev"),
            directory = roqSourceDir,
            env = env,
            timeoutValue = 24,
            timeoutUnit = TimeUnit.HOURS
        )
    }

}
