package net.sharplab.tsuji.core.driver.jekyll

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

class JekyllDriverImplTest {

    private val externalProcessDriver: ExternalProcessDriver = mock()
    private val target = JekyllDriverImpl(externalProcessDriver)

    @Test
    fun prepareSource_should_copy_files(@TempDir tempDir: Path) {
        // Given
        val sourceDir = tempDir.resolve("source").createDirectories()
        val workDir = tempDir.resolve("work").createDirectories()

        sourceDir.resolve("file1.txt").writeText("source content 1")
        sourceDir.resolve("subdir").createDirectories().resolve("file3.txt").writeText("source content 3")

        // When
        target.prepareSource(sourceDir, workDir)

        // Then
        assertThat(workDir.resolve("file1.txt").readText()).isEqualTo("source content 1")
        assertThat(workDir.resolve("subdir/file3.txt").readText()).isEqualTo("source content 3")
    }

    @Test
    fun applyOverrides_should_overwrite_files(@TempDir tempDir: Path) {
        // Given
        val overrideDir = tempDir.resolve("override").createDirectories()
        val workDir = tempDir.resolve("work").createDirectories()

        workDir.resolve("file1.txt").writeText("original content 1")
        overrideDir.resolve("file1.txt").writeText("override content 1")
        overrideDir.resolve("new_file.txt").writeText("new content")

        // When
        target.applyOverrides(overrideDir, workDir)

        // Then
        assertThat(workDir.resolve("file1.txt").readText()).isEqualTo("override content 1")
        assertThat(workDir.resolve("new_file.txt").readText()).isEqualTo("new content")
    }
}
