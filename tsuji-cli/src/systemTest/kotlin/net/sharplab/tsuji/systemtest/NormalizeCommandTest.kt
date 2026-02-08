package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.exists

@QuarkusMainTest
class NormalizeCommandTest {

    @Test
    fun testNormalize(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        SystemTestUtils.copyTestResources(buildDir, "sample.po")
        val tempPo = buildDir.resolve("sample.po")

        // When
        val result = launcher.launch("po", "normalize", "--po", tempPo.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(tempPo.exists()).isTrue()

        // 1. Verify content via PoDriver
        val poDriver = PoDriverImpl()
        val po = poDriver.load(tempPo)
        val messageId = "This is a long message that is intentionally wrapped into multiple lines within the PO file to test the normalization process which should unwrap it."
        val message = po.messages.find { it.messageId == messageId }

        assertThat(message).isNotNull
        assertThat(message?.messageString).isEqualTo("これは、正規化プロセスをテストするためにPOファイル内で意図的に複数行に折り返された長いメッセージです。正規化によって1行にまとめられるはずです。")

        // 2. Verify that it is NOT wrapped (no-wrap)
        val rawContent = Files.readString(tempPo)
        assertThat(rawContent).contains("msgid \"$messageId\"")
        assertThat(rawContent).doesNotContain("msgid \"\"\n\"This is a long message")
    }
}