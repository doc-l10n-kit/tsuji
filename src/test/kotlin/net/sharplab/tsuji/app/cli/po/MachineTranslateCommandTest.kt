package net.sharplab.tsuji.app.cli.po

import net.sharplab.tsuji.app.service.AsciidocMode
import net.sharplab.tsuji.app.service.TranslationAppService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import picocli.CommandLine

class MachineTranslateCommandTest {

    private fun createCommandLine(command: MachineTranslateCommand): CommandLine {
        val cmd = CommandLine(command)
        cmd.isCaseInsensitiveEnumValuesAllowed = true
        return cmd
    }

    @Test
    fun `default values should be correct`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = createCommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja")

        val asciidocField = command.javaClass.getDeclaredField("asciidoc")
        asciidocField.isAccessible = true
        val ragField = command.javaClass.getDeclaredField("rag")
        ragField.isAccessible = true

        assertThat(asciidocField.get(command))
            .describedAs("Default asciidoc should be AUTO")
            .isEqualTo(AsciidocMode.AUTO)
        assertThat(ragField.getBoolean(command))
            .describedAs("Default rag should be true")
            .isTrue()
    }

    @Test
    fun `--asciidoc always should enable asciidoc processing`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = createCommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja", "--asciidoc", "always")

        val asciidocField = command.javaClass.getDeclaredField("asciidoc")
        asciidocField.isAccessible = true

        assertThat(asciidocField.get(command))
            .describedAs("--asciidoc always should set ALWAYS")
            .isEqualTo(AsciidocMode.ALWAYS)
    }

    @Test
    fun `--asciidoc never should disable asciidoc processing`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = createCommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja", "--asciidoc", "never")

        val asciidocField = command.javaClass.getDeclaredField("asciidoc")
        asciidocField.isAccessible = true

        assertThat(asciidocField.get(command))
            .describedAs("--asciidoc never should set NEVER")
            .isEqualTo(AsciidocMode.NEVER)
    }

    @Test
    fun `--rag should enable RAG`() {
        val mockService = mock(TranslationAppService::class.java)
        val command = MachineTranslateCommand(mockService)

        val cmd = createCommandLine(command)
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

        val cmd = createCommandLine(command)
        cmd.parseArgs("-p", "test.po", "--source", "en", "--target", "ja", "--no-rag")

        val ragField = command.javaClass.getDeclaredField("rag")
        ragField.isAccessible = true

        assertThat(ragField.getBoolean(command))
            .describedAs("--no-rag should disable RAG")
            .isFalse()
    }
}
