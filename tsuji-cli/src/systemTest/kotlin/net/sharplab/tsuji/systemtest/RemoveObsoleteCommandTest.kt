package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

@QuarkusMainTest
class RemoveObsoleteCommandTest {

    @Test
    fun testRemoveObsolete(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val upstreamDir = buildDir.resolve("upstream")
        val poDir = buildDir.resolve("po")
        
        upstreamDir.createDirectories()
        poDir.createDirectories()

        upstreamDir.resolve("guide1.adoc").writeText("content")
        upstreamDir.resolve("sub/guide2.adoc").parent.createDirectories()
        upstreamDir.resolve("sub/guide2.adoc").writeText("content")

        poDir.resolve("guide1.adoc.po").writeText("po content")
        poDir.resolve("sub/guide2.adoc.po").parent.createDirectories()
        poDir.resolve("sub/guide2.adoc.po").writeText("po content")
        
        val obsoletePo = poDir.resolve("obsolete.adoc.po")
        obsoletePo.writeText("obsolete po content")
        val obsoletePoInSub = poDir.resolve("sub/deleted.adoc.po")
        obsoletePoInSub.writeText("obsolete po content")

        // When
        val result = launcher.launch("po", "remove-obsolete", "--po", poDir.toString(), "--upstream", upstreamDir.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(poDir.resolve("guide1.adoc.po")).exists()
        assertThat(poDir.resolve("sub/guide2.adoc.po")).exists()
        assertThat(obsoletePo).doesNotExist()
        assertThat(obsoletePoInSub).doesNotExist()
    }
}