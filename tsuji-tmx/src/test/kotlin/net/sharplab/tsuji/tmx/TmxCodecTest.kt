package net.sharplab.tsuji.tmx

import net.sharplab.tsuji.test.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TmxCodecTest{

    val target = TmxCodec()

    @Test
    fun load_test(){
        // Given
        val tmxPath = TestUtil.resolveClasspath("tmx/test.tmx")

        // When
        val tmx = target.load(tmxPath)

        // Then
        assertThat(tmx).isNotNull
        assertThat(tmx.tmxHeader).isNotNull
        assertThat(tmx.tmxBody).isNotNull
        assertThat(tmx.tmxBody.translationUnits).isNotEmpty
    }

    @Test
    fun save_and_load_test(@TempDir tempDir: Path) {
        // Given
        val tmxPath = TestUtil.resolveClasspath("tmx/test.tmx")
        val originalTmx = target.load(tmxPath)
        val savePath = tempDir.resolve("saved.tmx")

        // When
        target.save(originalTmx, savePath)

        // Then
        assertThat(savePath).exists()
        val loadedTmx = target.load(savePath)
        assertThat(loadedTmx.version).isEqualTo(originalTmx.version)
        assertThat(loadedTmx.tmxHeader.creationTool).isEqualTo(originalTmx.tmxHeader.creationTool)
        assertThat(loadedTmx.tmxBody.translationUnits?.size).isEqualTo(originalTmx.tmxBody.translationUnits?.size)
    }
}
