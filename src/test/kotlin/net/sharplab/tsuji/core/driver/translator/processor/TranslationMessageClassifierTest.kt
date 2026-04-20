package net.sharplab.tsuji.core.driver.translator.processor

import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.model.MessageType
import net.sharplab.tsuji.po.model.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TranslationMessageClassifierTest {

    private fun createMessage(
        messageId: String,
        messageString: String = "",
        type: MessageType = MessageType.PlainText
    ): TranslationMessage {
        val poMessage = PoMessage(
            messageId = messageId,
            messageString = messageString,
            sourceReferences = emptyList(),
            comments = if (type != MessageType.None) listOf(type.value) else emptyList()
        )
        return TranslationMessage(
            original = poMessage,
            text = if (messageString.isEmpty()) messageId else messageString,
            needsTranslation = messageString.isEmpty()
        )
    }

    @Test
    fun `should classify normal messages`() {
        val messages = listOf(
            createMessage("Hello"),
            createMessage("World")
        )
        val classified = classifyMessages(messages)
        assertThat(classified.normalIndices).containsExactly(0, 1)
        assertThat(classified.jekyllIndices).isEmpty()
        assertThat(classified.fillIndices).isEmpty()
    }

    @Test
    fun `should classify Jekyll Front Matter`() {
        val messages = listOf(
            createMessage("layout: post\ntitle: Hello\ndate: 2026-01-01\ntags: guide\nauthor: me\nsynopsis: World")
        )
        val classified = classifyMessages(messages)
        assertThat(classified.jekyllIndices).containsExactly(0)
        assertThat(classified.normalIndices).isEmpty()
    }

    @Test
    fun `should classify delimited blocks as fill`() {
        val messages = listOf(
            createMessage("----", type = MessageType.DelimitedBlock)
        )
        val classified = classifyMessages(messages)
        assertThat(classified.fillIndices).containsExactly(0)
        assertThat(classified.normalIndices).isEmpty()
    }

    @Test
    fun `should skip already translated messages`() {
        val messages = listOf(
            createMessage("Hello", messageString = "こんにちは")
        )
        val classified = classifyMessages(messages)
        assertThat(classified.normalIndices).isEmpty()
        assertThat(classified.jekyllIndices).isEmpty()
        assertThat(classified.fillIndices).isEmpty()
    }

    @Test
    fun `should skip empty messages`() {
        val messages = listOf(
            createMessage("")
        )
        val classified = classifyMessages(messages)
        assertThat(classified.normalIndices).isEmpty()
        assertThat(classified.jekyllIndices).isEmpty()
        assertThat(classified.fillIndices).isEmpty()
    }

    @Test
    fun `should classify mixed messages correctly`() {
        val messages = listOf(
            createMessage("Normal text"),                                    // 0: normal
            createMessage("Already done", messageString = "翻訳済み"),       // 1: skip
            createMessage("layout: post\ntitle: Hello\ndate: 2026-01-01\ntags: guide\nauthor: me\nsynopsis: World"), // 2: jekyll
            createMessage("----", type = MessageType.DelimitedBlock),        // 3: fill
            createMessage("Another normal")                                  // 4: normal
        )
        val classified = classifyMessages(messages)
        assertThat(classified.normalIndices).containsExactly(0, 4)
        assertThat(classified.jekyllIndices).containsExactly(2)
        assertThat(classified.fillIndices).containsExactly(3)
    }
}
