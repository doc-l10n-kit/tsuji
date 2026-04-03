package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.exists

@QuarkusMainTest
class PurgeFuzzyCommandTest {

    @Test
    fun testPurgeFuzzy(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        SystemTestUtils.copyTestResources(buildDir, "sample.po")
        val tempPo = buildDir.resolve("sample.po")

        // When
        val result = launcher.launch("po", "purge-fuzzy", "--po", tempPo.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        
        val poDriver = PoDriverImpl()
        val po = poDriver.load(tempPo)

        // Verify fuzzy message is purged
        val fuzzyMsg = po.messages.find { it.messageId == "Fuzzy message" }
        assertThat(fuzzyMsg).isNotNull
        assertThat(fuzzyMsg?.messageString).isEmpty()
        assertThat(fuzzyMsg?.fuzzy).isFalse()

        // Verify confirmed message is kept
        val confirmedMsg = po.messages.find { it.messageId == "Confirmed message" }
        assertThat(confirmedMsg).isNotNull
        assertThat(confirmedMsg?.messageString).isEqualTo("確定した翻訳")
        assertThat(confirmedMsg?.fuzzy).isFalse()
    }
}