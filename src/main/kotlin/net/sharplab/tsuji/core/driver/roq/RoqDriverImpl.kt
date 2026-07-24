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

    override fun build(roqSourceDir: Path, destinationDir: Path, profile: String?, poBaseDir: Path?, language: String?) {
        val env = mutableMapOf("QUARKUS_ROQ_GENERATOR_BATCH" to "true")
        if (profile != null) {
            env["QUARKUS_PROFILE"] = profile
        }
        addL10nEnv(env, poBaseDir, language)

        externalProcessDriver.execute(
            command = listOf("mvn", "-B", "package", "quarkus:run", "-DskipTests"),
            directory = roqSourceDir,
            env = env,
            timeoutValue = 30,
            timeoutUnit = TimeUnit.MINUTES
        )

        // "target/roq" is the default output path for Roq (quarkus.roq.generator.output-dir=roq),
        // and quarkus.io uses this default.
        val generatedDir = roqSourceDir.resolve("target/roq")
        if (generatedDir.exists()) {
            destinationDir.toFile().deleteRecursively()
            copyDirectory(generatedDir, destinationDir)
        }
    }

    override fun serve(roqSourceDir: Path, profile: String?, poBaseDir: Path?, language: String?) {
        val env = mutableMapOf<String, String>()
        if (profile != null) {
            env["QUARKUS_PROFILE"] = profile
        }
        addL10nEnv(env, poBaseDir, language)

        externalProcessDriver.execute(
            command = listOf("mvn", "quarkus:dev"),
            directory = roqSourceDir,
            env = env,
            timeoutValue = 24,
            timeoutUnit = TimeUnit.HOURS
        )
    }

    private fun addL10nEnv(env: MutableMap<String, String>, poBaseDir: Path?, language: String?) {
        if (poBaseDir != null) {
            env["L10N_PO_BASE_DIR"] = poBaseDir.toAbsolutePath().toString()
        }
        if (language != null) {
            env["L10N_LANGUAGE"] = language
        }
    }
}
