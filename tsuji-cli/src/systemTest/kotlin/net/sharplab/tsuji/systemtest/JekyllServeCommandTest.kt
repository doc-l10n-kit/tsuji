package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@QuarkusMainTest
class JekyllServeCommandTest {

    @Test
    fun testServeHelp(launcher: QuarkusMainLauncher) {
        // When
        val result = launcher.launch("jekyll", "serve", "--help")

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(result.output).contains("Serves the translated Jekyll site")
    }
}