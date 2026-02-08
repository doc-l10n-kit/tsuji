package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import net.sharplab.tsuji.systemtest.testutils.SystemTestUtils
import net.sharplab.tsuji.tmx.TmxCodec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

@QuarkusMainTest
class TmxGenerateCommandTest {

    @Test
    fun testTmxGenerate(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val poDir = buildDir.resolve("po").createDirectories()
        SystemTestUtils.copyTestResources(poDir, "sample.po")
        val outputTmx = buildDir.resolve("output.tmx")

        // When
        val result = launcher.launch("tmx", "generate", "--po", poDir.toString(), "--tmx", outputTmx.toString())

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        assertThat(outputTmx.exists()).isTrue()
        val tmxCodec = TmxCodec()
        val tmx = tmxCodec.load(outputTmx)
        val tus = tmx.tmxBody.translationUnits ?: emptyList()
        assertThat(tus).hasSize(1)
        val tu = tus[0]
        assertThat(tu.variants.find { it.lang == "en" }?.seg).isEqualTo("Hello")
        assertThat(tu.variants.find { it.lang == "ja_JP" }?.seg).isEqualTo("こんにちは")
    }

    @Test
    fun testTmxGenerateFuzzy(launcher: QuarkusMainLauncher) {
        // Given
        val buildDir = SystemTestUtils.prepareTestDir()
        val poDir = buildDir.resolve("po").createDirectories()
        SystemTestUtils.copyTestResources(poDir, "sample.po")
        val outputTmx = buildDir.resolve("output_fuzzy.tmx")

        // When
        val result = launcher.launch("tmx", "generate", "--po", poDir.toString(), "--tmx", outputTmx.toString(), "--mode", "FUZZY")

        // Then
        assertThat(result.exitCode()).isEqualTo(0)
        val tmxCodec = TmxCodec()
        val tmx = tmxCodec.load(outputTmx)
        val tus = tmx.tmxBody.translationUnits ?: emptyList()
        assertThat(tus).hasSize(1)
        val tu = tus[0]
        assertThat(tu.variants.find { it.lang == "en" }?.seg).isEqualTo("Fuzzy Hello")
        assertThat(tu.variants.find { it.lang == "ja_JP" }?.seg).isEqualTo("ふわっとこんにちは")
    }
}