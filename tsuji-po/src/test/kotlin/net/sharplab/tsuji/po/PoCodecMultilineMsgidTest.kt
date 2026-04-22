package net.sharplab.tsuji.po

import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoFlag
import net.sharplab.tsuji.po.model.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class PoCodecMultilineMsgidTest {

    private val target = PoCodec()

    @Test
    fun `should preserve multiline msgid with newlines`(@TempDir tempDir: Path) {
        // Given
        val multilineMsgid = """This guide is not going to go into the specifics of using either
xref:hibernate-orm.adoc[Hibernate], or its underlying
link:{jakarta-persistence-spec-url}[Jakarta Persistence specification],
because both already have great documentation that you can and should use for more in-depth knowledge."""

        val messages = listOf(
            PoMessage(
                messageId = multilineMsgid,
                messageString = "テスト翻訳",
                sourceReferences = listOf(PoMessage.SourceReference(File("test.adoc"), 33)),
                _flags = mutableSetOf<PoFlag>(),
                comments = listOf("type: Plain text")
            )
        )
        val po = Po("ja_JP", messages)
        val savePath = tempDir.resolve("multiline-test.po")

        // When
        target.save(po, savePath)
        val loadedPo = target.load(savePath)

        // Then
        val loadedMessage = loadedPo.messages.find { it.messageId.contains("This guide is not going") }
        assertThat(loadedMessage).isNotNull
        assertThat(loadedMessage?.messageId).isEqualTo(multilineMsgid)
        assertThat(loadedMessage?.messageId).contains("\n")

        // Verify the actual file format
        val fileContent = savePath.toFile().readText()

        // Save to a known location for inspection
        val inspectionPath = File("/tmp/pocodec-multiline-test.po")
        inspectionPath.writeText(fileContent)
        println("Saved inspection file to: ${inspectionPath.absolutePath}")
        println("msgid contains newline: ${loadedMessage?.messageId?.contains("\n")}")
    }
}
