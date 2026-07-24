package net.sharplab.tsuji.core.driver.roq

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class RoqDriverImplTest {

    private val externalProcessDriver: ExternalProcessDriver = mock()
    private val target = RoqDriverImpl(externalProcessDriver)

    @Test
    fun prepareSource_should_copy_files(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("source").createDirectories()
        val workDir = tempDir.resolve("work").createDirectories()

        sourceDir.resolve("file1.adoc").writeText("= Guide Title")
        sourceDir.resolve("content").createDirectories()
            .resolve("guide.adoc").writeText("= Nested Guide")

        target.prepareSource(sourceDir, workDir)

        assertThat(workDir.resolve("file1.adoc").readText()).isEqualTo("= Guide Title")
        assertThat(workDir.resolve("content/guide.adoc").readText()).isEqualTo("= Nested Guide")
    }

    @Test
    fun prepareSource_should_preserve_directory_structure(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("source").createDirectories()
        val workDir = tempDir.resolve("work").createDirectories()

        sourceDir.resolve("content/guides").createDirectories()
            .resolve("getting-started.adoc").writeText("guide content")
        sourceDir.resolve("_data").createDirectories()
            .resolve("versions.yaml").writeText("versions: []")

        target.prepareSource(sourceDir, workDir)

        assertThat(workDir.resolve("content/guides/getting-started.adoc").readText()).isEqualTo("guide content")
        assertThat(workDir.resolve("_data/versions.yaml").readText()).isEqualTo("versions: []")
    }

    @Test
    fun applyOverrides_should_overwrite_existing_files(@TempDir tempDir: Path) {
        val overrideDir = tempDir.resolve("override").createDirectories()
        val workDir = tempDir.resolve("work").createDirectories()

        workDir.resolve("file1.adoc").writeText("original content")
        overrideDir.resolve("file1.adoc").writeText("translated content")

        target.applyOverrides(overrideDir, workDir)

        assertThat(workDir.resolve("file1.adoc").readText()).isEqualTo("translated content")
    }

    @Test
    fun applyOverrides_should_add_new_files(@TempDir tempDir: Path) {
        val overrideDir = tempDir.resolve("override").createDirectories()
        val workDir = tempDir.resolve("work").createDirectories()

        overrideDir.resolve("new_file.html").writeText("new content")

        target.applyOverrides(overrideDir, workDir)

        assertThat(workDir.resolve("new_file.html").readText()).isEqualTo("new content")
    }

    @Test
    fun applyOverrides_should_skip_when_dir_does_not_exist(@TempDir tempDir: Path) {
        val overrideDir = tempDir.resolve("nonexistent")
        val workDir = tempDir.resolve("work").createDirectories()
        workDir.resolve("existing.txt").writeText("should remain")

        target.applyOverrides(overrideDir, workDir)

        assertThat(workDir.resolve("existing.txt").readText()).isEqualTo("should remain")
    }

    @Test
    fun build_should_invoke_maven_with_batch_env(@TempDir tempDir: Path) {
        val roqSourceDir = tempDir.resolve("roq-source").createDirectories()
        val destinationDir = tempDir.resolve("output").createDirectories()

        target.build(roqSourceDir, destinationDir, null)

        verify(externalProcessDriver).execute(
            command = argThat<List<String>> {
                contains("mvn") && contains("quarkus:run")
            },
            directory = eq(roqSourceDir),
            env = argThat<Map<String, String>> {
                this["QUARKUS_ROQ_GENERATOR_BATCH"] == "true"
            },
            timeoutValue = any(),
            timeoutUnit = any()
        )
    }

    @Test
    fun build_should_set_quarkus_profile_when_provided(@TempDir tempDir: Path) {
        val roqSourceDir = tempDir.resolve("roq-source").createDirectories()
        val destinationDir = tempDir.resolve("output").createDirectories()

        target.build(roqSourceDir, destinationDir, "only-latest-guides")

        verify(externalProcessDriver).execute(
            command = any(),
            directory = eq(roqSourceDir),
            env = argThat<Map<String, String>> {
                this["QUARKUS_PROFILE"] == "only-latest-guides" &&
                this["QUARKUS_ROQ_GENERATOR_BATCH"] == "true"
            },
            timeoutValue = any(),
            timeoutUnit = any()
        )
    }

    @Test
    fun serve_should_invoke_maven_quarkus_dev(@TempDir tempDir: Path) {
        val roqSourceDir = tempDir.resolve("roq-source").createDirectories()

        target.serve(roqSourceDir, null)

        verify(externalProcessDriver).execute(
            command = argThat<List<String>> {
                contains("mvn") && contains("quarkus:dev")
            },
            directory = eq(roqSourceDir),
            env = any(),
            timeoutValue = any(),
            timeoutUnit = any()
        )
    }

    @Test
    fun serve_should_set_quarkus_profile_when_provided(@TempDir tempDir: Path) {
        val roqSourceDir = tempDir.resolve("roq-source").createDirectories()

        target.serve(roqSourceDir, "dev-mode")

        verify(externalProcessDriver).execute(
            command = any(),
            directory = eq(roqSourceDir),
            env = argThat<Map<String, String>> {
                this["QUARKUS_PROFILE"] == "dev-mode"
            },
            timeoutValue = any(),
            timeoutUnit = any()
        )
    }

    @Test
    fun build_should_pass_l10n_env_vars_when_provided(@TempDir tempDir: Path) {
        val roqSourceDir = tempDir.resolve("roq-source").createDirectories()
        val destinationDir = tempDir.resolve("output").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        target.build(roqSourceDir, destinationDir, null, poBaseDir = poBaseDir, language = "pt")

        verify(externalProcessDriver).execute(
            command = any(),
            directory = eq(roqSourceDir),
            env = argThat<Map<String, String>> {
                this["L10N_PO_BASE_DIR"] == poBaseDir.toAbsolutePath().toString() &&
                this["L10N_LANGUAGE"] == "pt"
            },
            timeoutValue = any(),
            timeoutUnit = any()
        )
    }

    @Test
    fun build_should_not_pass_l10n_env_vars_when_null(@TempDir tempDir: Path) {
        val roqSourceDir = tempDir.resolve("roq-source").createDirectories()
        val destinationDir = tempDir.resolve("output").createDirectories()

        target.build(roqSourceDir, destinationDir, null, poBaseDir = null, language = null)

        verify(externalProcessDriver).execute(
            command = any(),
            directory = eq(roqSourceDir),
            env = argThat<Map<String, String>> {
                !containsKey("L10N_PO_BASE_DIR") && !containsKey("L10N_LANGUAGE")
            },
            timeoutValue = any(),
            timeoutUnit = any()
        )
    }

    @Test
    fun serve_should_pass_l10n_env_vars_when_provided(@TempDir tempDir: Path) {
        val roqSourceDir = tempDir.resolve("roq-source").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        target.serve(roqSourceDir, null, poBaseDir = poBaseDir, language = "ja")

        verify(externalProcessDriver).execute(
            command = any(),
            directory = eq(roqSourceDir),
            env = argThat<Map<String, String>> {
                this["L10N_PO_BASE_DIR"] == poBaseDir.toAbsolutePath().toString() &&
                this["L10N_LANGUAGE"] == "ja"
            },
            timeoutValue = any(),
            timeoutUnit = any()
        )
    }
}
