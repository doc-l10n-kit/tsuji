package net.sharplab.tsuji.app.cli.po

import net.sharplab.tsuji.app.service.TranslationAppService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import picocli.CommandLine

class MachineTranslateCommandTest {

    @Test
    fun `default values should be correct`() {
        // Create a command without Quarkus injection for pure picocli testing
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = CommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja")

        // Access via reflection since fields are private
        val asciidocField = command.javaClass.getDeclaredField("asciidoc")
        asciidocField.isAccessible = true
        val ragField = command.javaClass.getDeclaredField("rag")
        ragField.isAccessible = true

        assertThat(asciidocField.getBoolean(command))
            .describedAs("Default asciidoc should be true")
            .isTrue()
        assertThat(ragField.getBoolean(command))
            .describedAs("Default rag should be true")
            .isTrue()
    }

    @Test
    fun `--isAsciidoc should enable asciidoc processing`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = CommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja", "--isAsciidoc")

        val asciidocField = command.javaClass.getDeclaredField("asciidoc")
        asciidocField.isAccessible = true

        assertThat(asciidocField.getBoolean(command))
            .describedAs("--isAsciidoc should enable asciidoc")
            .isTrue()
    }

    @Test
    fun `--no-isAsciidoc should disable asciidoc processing`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = CommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja", "--no-isAsciidoc")

        val asciidocField = command.javaClass.getDeclaredField("asciidoc")
        asciidocField.isAccessible = true

        assertThat(asciidocField.getBoolean(command))
            .describedAs("--no-isAsciidoc should disable asciidoc")
            .isFalse()
    }

    @Test
    fun `--rag should enable RAG`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = CommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja", "--rag")

        val ragField = command.javaClass.getDeclaredField("rag")
        ragField.isAccessible = true

        assertThat(ragField.getBoolean(command))
            .describedAs("--rag should enable RAG")
            .isTrue()
    }

    @Test
    fun `--no-rag should disable RAG`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = CommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja", "--no-rag")

        val ragField = command.javaClass.getDeclaredField("rag")
        ragField.isAccessible = true

        assertThat(ragField.getBoolean(command))
            .describedAs("--no-rag should disable RAG")
            .isFalse()
    }
}
