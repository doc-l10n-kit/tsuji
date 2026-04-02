package net.sharplab.tsuji.core.driver.po

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoFlag
import net.sharplab.tsuji.core.model.po.PoMessage
import net.sharplab.tsuji.test.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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

    @Test
    fun `should preserve type comments on save and load`(@TempDir tempDir: Path) {
        // Given
        val messages = listOf(
            PoMessage(
                type = MessageType("type: Hash Value: footer_text"),
                messageId = "test message",
                messageString = "test translation",
                sourceReferences = listOf(PoMessage.SourceReference(File("test.yaml"), 1)),
                _flags = mutableSetOf<PoFlag>(),
                comments = emptyList()
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("type-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then - header message + actual message
        assertThat(loadedPo.messages).hasSize(2)
        val loadedMessage = loadedPo.messages.find { it.messageId == "test message" }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.type).isEqualTo(MessageType("type: Hash Value: footer_text"))
    }

    @Test
    fun `should preserve non-type extracted comments on save and load`(@TempDir tempDir: Path) {
        // Given
        val messages = listOf(
            PoMessage(
                type = MessageType.PlainText,
                messageId = "test message",
                messageString = "test translation",
                sourceReferences = listOf(PoMessage.SourceReference(File("test.yaml"), 1)),
                _flags = mutableSetOf<PoFlag>(),
                comments = listOf("translator comment", "another comment")
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("comments-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        val loadedMessage = loadedPo.messages.find { it.messageId == "test message" }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.comments).containsExactlyInAnyOrder("translator comment", "another comment")
    }

    @Test
    fun `should preserve both type and non-type comments`(@TempDir tempDir: Path) {
        // Given
        val messages = listOf(
            PoMessage(
                type = MessageType("type: Custom Type"),
                messageId = "test message",
                messageString = "test translation",
                sourceReferences = listOf(PoMessage.SourceReference(File("test.yaml"), 1)),
                _flags = mutableSetOf(PoFlag.Fuzzy),
                comments = listOf("translator note", "context note")
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("both-comments-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        val loadedMessage = loadedPo.messages.find { it.messageId == "test message" }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.type).isEqualTo(MessageType("type: Custom Type"))
        assertThat(loadedMessage?.comments).containsExactlyInAnyOrder("translator note", "context note")
        assertThat(loadedMessage?.fuzzy).isTrue
    }

    @Test
    fun `should handle unknown type comments gracefully`(@TempDir tempDir: Path) {
        // Given
        val messages = listOf(
            PoMessage(
                type = MessageType("type: Unknown Future Type"),
                messageId = "test message",
                messageString = "test translation",
                sourceReferences = listOf(PoMessage.SourceReference(File("test.yaml"), 1)),
                _flags = mutableSetOf<PoFlag>(),
                comments = emptyList()
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("unknown-type-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        val loadedMessage = loadedPo.messages.find { it.messageId == "test message" }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.type?.value).isEqualTo("type: Unknown Future Type")
    }

    @Test
    fun `should preserve MessageType None when no type comment exists`(@TempDir tempDir: Path) {
        // Given
        val messages = listOf(
            PoMessage(
                type = MessageType.None,
                messageId = "test message",
                messageString = "test translation",
                sourceReferences = emptyList(),
                _flags = mutableSetOf<PoFlag>(),
                comments = emptyList()
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("no-type-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        val loadedMessage = loadedPo.messages.find { it.messageId == "test message" }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.type).isEqualTo(MessageType.None)
    }

    @Test
    fun `should preserve no-wrap flag on save and load`(@TempDir tempDir: Path) {
        // Given
        val messages = listOf(
            PoMessage(
                type = MessageType.PlainText,
                messageId = "test message",
                messageString = "test translation",
                sourceReferences = listOf(PoMessage.SourceReference(File("test.yaml"), 1)),
                _flags = mutableSetOf(PoFlag.NoWrap),
                comments = emptyList()
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("no-wrap-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        val loadedMessage = loadedPo.messages.find { it.messageId == "test message" }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.flags).contains(PoFlag.NoWrap)
    }

    @Test
    fun `should preserve multiple flags including fuzzy and no-wrap`(@TempDir tempDir: Path) {
        // Given
        val messages = listOf(
            PoMessage(
                type = MessageType.PlainText,
                messageId = "test message",
                messageString = "test translation",
                sourceReferences = listOf(PoMessage.SourceReference(File("test.yaml"), 1)),
                _flags = mutableSetOf(PoFlag.Fuzzy, PoFlag.NoWrap),
                comments = emptyList()
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("multiple-flags-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        val loadedMessage = loadedPo.messages.find { it.messageId == "test message" }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.flags).containsExactlyInAnyOrder(PoFlag.Fuzzy, PoFlag.NoWrap)
        assertThat(loadedMessage?.fuzzy).isTrue // Convenience property should work
    }

    @Test
    fun `should preserve POT-Creation-Date header on save and load`(@TempDir tempDir: Path) {
        // Given
        val header = mapOf(
            "POT-Creation-Date" to "2026-01-01 12:00+0000",
            "Language" to "ja_JP",
            "MIME-Version" to "1.0",
            "Content-Type" to "text/plain; charset=UTF-8"
        )
        val messages = listOf(
            PoMessage(
                type = MessageType.PlainText,
                messageId = "test",
                messageString = "translation",
                sourceReferences = emptyList(),
                _flags = mutableSetOf<PoFlag>(),
                comments = emptyList()
            )
        )
        val po = Po("ja_JP", messages, header)
        val savePath = tempDir.resolve("header-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        assertThat(loadedPo.header["POT-Creation-Date"]).isEqualTo("2026-01-01 12:00+0000")
        assertThat(loadedPo.header["Language"]).isEqualTo("ja_JP")
    }
}