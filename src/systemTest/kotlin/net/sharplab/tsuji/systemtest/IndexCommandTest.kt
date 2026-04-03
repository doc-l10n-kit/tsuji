package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.exists

@QuarkusMainTest
class IndexCommandTest {

    @Test
    fun testIndex(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        SystemTestUtils.copyTestResources(buildDir, "sample.tmx")
        
        val tmxPath = buildDir.resolve("sample.tmx")
        val indexDir = buildDir.resolve("index")

        // When
        val result = launcher.launch("rag", "index", "--tmx", tmxPath.toString(), "--index-dir", indexDir.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(indexDir.resolve("embeddings.json")).exists()
    }
}
