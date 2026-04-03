package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.exists

@QuarkusMainTest
class ApplyTmxCommandTest {

    @Test
    fun testApplyTmx(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        SystemTestUtils.copyTestResources(buildDir, "test.tmx", "sample.po")
        
        val tempTmx = buildDir.resolve("test.tmx")
        val tempPo = buildDir.resolve("sample.po")

        // When
        val result = launcher.launch("po", "apply-tmx", "--tmx", tempTmx.toString(), "--po", tempPo.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(tempPo.exists()).isTrue()
        
        val poDriver = PoDriverImpl()
        val po = poDriver.load(tempPo)
        val message = po.messages.find { it.messageId == "This is a pen." }
        assertThat(message).isNotNull
        assertThat(message?.messageString).isEqualTo("これはペンです。")
        assertThat(message?.fuzzy).isFalse()
    }
}
