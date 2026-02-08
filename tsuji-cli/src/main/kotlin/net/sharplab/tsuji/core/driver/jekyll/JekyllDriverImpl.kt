package net.sharplab.tsuji.core.driver.jekyll

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class JekyllDriverImpl(private val externalProcessDriver: ExternalProcessDriver) : JekyllDriver {

    private val asciidoctorL10nRepo = "https://github.com/doc-l10n-kit/asciidoctor-l10n"
    private val jekyllL10nRepo = "https://github.com/doc-l10n-kit/jekyll-l10n"

    override fun prepareSource(sourceDir: Path, workDir: Path) {
        copyDirectory(sourceDir, workDir)
    }

    override fun applyOverrides(overrideDir: Path, workDir: Path) {
        if (!overrideDir.exists()) return
        overrideDir.walk().forEach { path ->
            if (path == overrideDir || path.isDirectory()) return@forEach
            val relative = overrideDir.relativize(path)
            val dest = workDir.resolve(relative)
            dest.parent?.createDirectories()
            path.copyTo(dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        }
    }

    override fun extractPo(jekyllSourceDir: Path, poBaseDir: Path) {
        ensureJekyllL10nPlugin(jekyllSourceDir, asciidoctorL10nRepo)

        externalProcessDriver.execute(
            command = listOf("bundle", "exec", "jekyll", "build", "--config", "_config.yml"),
            directory = jekyllSourceDir,
            env = mapOf(
                "L10N_MODE" to "update_po",
                "L10N_PO_BASE_DIR" to poBaseDir.toAbsolutePath().toString()
            ),
            timeoutValue = 60,
            timeoutUnit = TimeUnit.MINUTES
        )
    }

    override fun build(jekyllSourceDir: Path, poBaseDir: Path, destinationDir: Path, siteLanguageCode: String, additionalConfigs: List<String>, translate: Boolean) {
        val configsList = mutableListOf("_config.yml")
        val env = mutableMapOf<String, String>()
        if (translate) {
            ensureJekyllL10nPlugin(jekyllSourceDir, jekyllL10nRepo)
            createLanguageConfig(jekyllSourceDir, siteLanguageCode)
            configsList.add("language_config.yml")

            env["L10N_MODE"] = "translate"
            env["L10N_PO_BASE_DIR"] = poBaseDir.toAbsolutePath().toString()
        }

        configsList.addAll(additionalConfigs)
        val configs = configsList.joinToString(",")

        externalProcessDriver.execute(
            command = listOf("bundle", "exec", "jekyll", "build", "-d", destinationDir.toAbsolutePath().toString(), "--config", configs),
            directory = jekyllSourceDir,
            env = env,
            timeoutValue = 30,
            timeoutUnit = TimeUnit.MINUTES
        )

        deleteCNAMEFile(destinationDir)
    }

    override fun serve(jekyllSourceDir: Path, poBaseDir: Path, destinationDir: Path, siteLanguageCode: String, additionalConfigs: List<String>, translate: Boolean) {
        val configsList = mutableListOf("_config.yml")
        val env = mutableMapOf<String, String>()
        if (translate) {
            ensureJekyllL10nPlugin(jekyllSourceDir, jekyllL10nRepo)
            createLanguageConfig(jekyllSourceDir, siteLanguageCode)
            configsList.add("language_config.yml")

            env["L10N_MODE"] = "translate"
            env["L10N_PO_BASE_DIR"] = poBaseDir.toAbsolutePath().toString()
        }

        configsList.addAll(additionalConfigs)
        val configs = configsList.joinToString(",")

        // Serve command usually runs forever, but here we keep it for consistency
        externalProcessDriver.execute(
            command = listOf("bundle", "exec", "jekyll", "serve", "-d", destinationDir.toAbsolutePath().toString(), "--config", configs),
            directory = jekyllSourceDir,
            env = env,
            timeoutValue = 24, // High timeout for serve
            timeoutUnit = TimeUnit.HOURS
        )
    }

    private fun ensureJekyllL10nPlugin(jekyllSourceDir: Path, gitRepo: String) {
        val listProcess = ProcessBuilder("bundle", "list")
            .directory(jekyllSourceDir.toFile())
            .start()
        
        val output = listProcess.inputStream.bufferedReader().readText()
        listProcess.waitFor()

        if (!output.contains("jekyll-l10n")) {
            externalProcessDriver.execute(
                command = listOf("bundle", "config", "set", "path", "vendor/bundle"),
                directory = jekyllSourceDir
            )

            externalProcessDriver.execute(
                command = listOf("bundle", "add", "jekyll-l10n", "--group", "jekyll_plugins", "--git", gitRepo, "--branch", "main"),
                directory = jekyllSourceDir,
                timeoutValue = 10,
                timeoutUnit = TimeUnit.MINUTES
            )
        }
    }

    private fun createLanguageConfig(jekyllSourceDir: Path, siteLanguageCode: String) {
        val configPath = jekyllSourceDir.resolve("language_config.yml")
        configPath.writeText("language: $siteLanguageCode\n")
    }

    private fun deleteCNAMEFile(destinationDir: Path) {
        val cnameFile = destinationDir.resolve("CNAME")
        if (cnameFile.exists()) {
            cnameFile.deleteExisting()
        }
    }

    private fun copyDirectory(source: Path, target: Path) {
        source.walk().forEach { path ->
            if (path == source) return@forEach
            val relative = source.relativize(path)
            val dest = target.resolve(relative)
            if (path.isDirectory()) {
                dest.createDirectories()
            } else {
                dest.parent?.createDirectories()
                path.copyTo(dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
            }
        }
    }
}