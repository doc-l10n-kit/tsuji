package net.sharplab.tsuji.core.driver.tmx

import net.sharplab.tsuji.tmx.TmxCodec
import net.sharplab.tsuji.tmx.model.Tmx
import net.sharplab.tsuji.tmx.model.TmxBody
import net.sharplab.tsuji.tmx.model.TmxHeader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists

class TmxDriverImplTest {

    private val target = TmxDriverImpl(TmxCodec())

    @Test
    fun save_and_load_should_work(@TempDir tempDir: Path) {
        // Given
        val tmxPath = tempDir.resolve("test.tmx")
        val header = TmxHeader("tool", "1.0", "seg", "tmf", "en", "en", "plaintext")
        val body = TmxBody(emptyList())
        val tmx = Tmx("1.4", header, body)

        // When
        target.save(tmx, tmxPath)

        // Then
        assertThat(tmxPath).exists()
        val loaded = target.load(tmxPath)
        assertThat(loaded.version).isEqualTo("1.4")
        assertThat(loaded.tmxHeader.creationTool).isEqualTo("tool")
    }
}