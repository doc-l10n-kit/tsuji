package net.sharplab.tsuji.core.driver.po

import net.sharplab.tsuji.test.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PoDriverImplTest {

    private val target = PoDriverImpl()

    @Test
    fun load_test() {
        // Given
        val path = TestUtil.resolveClasspath("po/sample.adoc.po")

        // When
        val po = target.load(path)

        // Then
        assertThat(po).isNotNull
        assertThat(po.target).isEqualTo("ja_JP") // assuming sample.adoc.po has ja_JP target
        assertThat(po.messages).isNotEmpty
    }

    @Test
    fun save_and_load_test(@TempDir tempDir: Path) {
        // Given
        val path = TestUtil.resolveClasspath("po/sample.adoc.po")
        val originalPo = target.load(path)

        // Modify a message
        val firstMessage = originalPo.messages.first { it.messageId.isNotEmpty() }
        val originalId = firstMessage.messageId
        val testString = "テスト翻訳"
        firstMessage.messageString = testString
        val savePath = tempDir.resolve("test.po")

        // When
        target.save(originalPo, savePath)

        // Then
        assertThat(savePath).exists()

        // Reload and verify
        val loadedPo = target.load(savePath)
        val verifiedMessage = loadedPo.messages.find { it.messageId == originalId }
        assertThat(verifiedMessage).isNotNull
        assertThat(verifiedMessage?.messageString).isEqualTo(testString)
    }
}