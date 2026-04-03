package net.sharplab.tsuji.core.driver.po4a

import net.sharplab.tsuji.core.driver.common.ExternalProcessDriver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.nio.file.Paths

class Po4aDriverImplTest {

    private val externalProcessDriver: ExternalProcessDriver = mock()
    private val target = Po4aDriverImpl(externalProcessDriver)

    @Test
    fun determineFormat_test() {
        // Given & When & Then
        assertThat(target.determineFormat(Paths.get("test.md"))).isEqualTo("text")
        assertThat(target.determineFormat(Paths.get("test.markdown"))).isEqualTo("text")
        assertThat(target.determineFormat(Paths.get("test.yml"))).isEqualTo("yaml")
        assertThat(target.determineFormat(Paths.get("test.yaml"))).isEqualTo("yaml")
        assertThat(target.determineFormat(Paths.get("test.html"))).isEqualTo("xhtml")
        assertThat(target.determineFormat(Paths.get("test.adoc"))).isEqualTo("asciidoc")
        assertThat(target.determineFormat(Paths.get("test.unknown"))).isNull()
    }
}