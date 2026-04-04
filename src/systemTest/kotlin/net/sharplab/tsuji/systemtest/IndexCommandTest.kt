package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

@QuarkusMainTest
@TestProfile(IndexCommandTest.Profile::class)
class IndexCommandTest {

    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides() = mapOf(
            "tsuji.rag.index-path" to "build/tmp/systemTest/IndexCommandTest/rag-index"
        )
    }

    @Test
    fun testIndex(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        SystemTestUtils.copyTestResources(buildDir, "sample.tmx")

        val tmxPath = buildDir.resolve("sample.tmx")

        // Use test-specific index path (configured in IndexCommandTestProfile)
        val expectedIndexDir = Path.of("build/tmp/systemTest/IndexCommandTest/rag-index")

        // Clean up any existing index to ensure we're testing actual creation
        if (expectedIndexDir.exists()) {
            expectedIndexDir.toFile().deleteRecursively()
        }

        // When
        val result = launcher.launch("rag", "index", "--tmx", tmxPath.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)

        // Verify Lucene index was created
        assertThat(expectedIndexDir).exists()

        // Check for Lucene index files (segments file is always present)
        val segmentsFiles = expectedIndexDir.toFile().listFiles { file ->
            file.name.startsWith("segments_")
        }
        assertThat(segmentsFiles).isNotNull()
        assertThat(segmentsFiles).describedAs("Lucene index should contain segments files").isNotEmpty()
    }
}
