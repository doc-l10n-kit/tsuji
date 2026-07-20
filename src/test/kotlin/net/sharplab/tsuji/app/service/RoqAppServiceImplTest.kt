package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.git.GitTimestampDriver
import net.sharplab.tsuji.core.driver.roq.RoqDriver
import net.sharplab.tsuji.core.service.SiteService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories

class RoqAppServiceImplTest {

    private val roqDriver: RoqDriver = mock()
    private val poAppService: PoAppService = mock()
    private val gitTimestampDriver: GitTimestampDriver = mock()
    private val siteService: SiteService = mock()

    private fun createConfig(
        sourceDir: String = "upstream",
        overrideDir: String = "l10n/override/ja_JP",
        destinationDir: String = "target/roq",
        statsDir: String = "l10n/stats",
        poBaseDir: String = "l10n/po/ja_JP",
        quarkusProfile: String? = null,
        statsSections: Map<String, String>? = null,
        htmlInclude: List<String> = emptyList(),
        yamlExclude: List<String> = emptyList()
    ): TsujiConfig {
        val roqYaml: TsujiConfig.Roq.Extract.Yaml = mock()
        whenever(roqYaml.exclude).thenReturn(Optional.of(yamlExclude))
        val roqHtml: TsujiConfig.Roq.Extract.Html = mock()
        whenever(roqHtml.include).thenReturn(Optional.of(htmlInclude))
        val roqExtract: TsujiConfig.Roq.Extract = mock()
        whenever(roqExtract.yaml).thenReturn(roqYaml)
        whenever(roqExtract.html).thenReturn(roqHtml)

        val roq: TsujiConfig.Roq = mock()
        whenever(roq.sourceDir).thenReturn(sourceDir)
        whenever(roq.overrideDir).thenReturn(overrideDir)
        whenever(roq.destinationDir).thenReturn(destinationDir)
        whenever(roq.statsDir).thenReturn(statsDir)
        whenever(roq.quarkusProfile).thenReturn(Optional.ofNullable(quarkusProfile))
        whenever(roq.statsSections).thenReturn(statsSections ?: emptyMap())
        whenever(roq.extract).thenReturn(roqExtract)

        val po: TsujiConfig.Po = mock()
        whenever(po.baseDir).thenReturn(poBaseDir)

        val language: TsujiConfig.Language = mock()
        whenever(language.to).thenReturn("pt")

        val config: TsujiConfig = mock()
        whenever(config.roq).thenReturn(roq)
        whenever(config.po).thenReturn(po)
        whenever(config.language).thenReturn(language)
        return config
    }

    @Test
    fun build_with_translate_should_prepare_apply_and_build(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("upstream").toString()
        val overrideDir = tempDir.resolve("override").toString()
        val destinationDir = tempDir.resolve("output").toString()
        val poBaseDir = tempDir.resolve("po").toString()

        val config = createConfig(
            sourceDir = sourceDir,
            overrideDir = overrideDir,
            destinationDir = destinationDir,
            poBaseDir = poBaseDir
        )

        val target = RoqAppServiceImpl(roqDriver, poAppService, gitTimestampDriver, siteService, config)
        target.build(translate = true)

        val workDirCaptor = argumentCaptor<Path>()
        verify(roqDriver).prepareSource(any(), workDirCaptor.capture())
        verify(roqDriver).applyOverrides(any(), any())
        verify(poAppService).applyPoToDirectory(
            workDir = any(),
            poBaseDir = any(),
            skipAsciidoc = eq(false),
            htmlIncludeList = any(),
            yamlExcludeList = any()
        )
        verify(roqDriver).build(any(), any(), isNull(), any(), any())
    }

    @Test
    fun build_without_translate_should_skip_overrides(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("upstream").toString()
        val destinationDir = tempDir.resolve("output").toString()

        val config = createConfig(
            sourceDir = sourceDir,
            destinationDir = destinationDir
        )

        val target = RoqAppServiceImpl(roqDriver, poAppService, gitTimestampDriver, siteService, config)
        target.build(translate = false)

        verify(roqDriver).prepareSource(any(), any())
        verify(roqDriver, never()).applyOverrides(any(), any())
        verify(poAppService, never()).applyPoToDirectory(any(), any(), any(), any(), any())
        verify(roqDriver).build(any(), any(), isNull(), isNull(), isNull())
    }

    @Test
    fun build_should_pass_profile(@TempDir tempDir: Path) {
        val config = createConfig(
            sourceDir = tempDir.resolve("upstream").toString(),
            destinationDir = tempDir.resolve("output").toString(),
            quarkusProfile = "only-latest-guides"
        )

        val target = RoqAppServiceImpl(roqDriver, poAppService, gitTimestampDriver, siteService, config)
        target.build(translate = false, profile = "custom-profile")

        verify(roqDriver).build(any(), any(), eq("custom-profile"), isNull(), isNull())
    }

    @Test
    fun build_should_use_config_profile_when_not_overridden(@TempDir tempDir: Path) {
        val config = createConfig(
            sourceDir = tempDir.resolve("upstream").toString(),
            destinationDir = tempDir.resolve("output").toString(),
            quarkusProfile = "only-latest-guides"
        )

        val target = RoqAppServiceImpl(roqDriver, poAppService, gitTimestampDriver, siteService, config)
        target.build(translate = false)

        verify(roqDriver).build(any(), any(), eq("only-latest-guides"), isNull(), isNull())
    }

    @Test
    fun serve_with_translate_should_prepare_apply_and_serve(@TempDir tempDir: Path) {
        val config = createConfig(
            sourceDir = tempDir.resolve("upstream").toString(),
            overrideDir = tempDir.resolve("override").toString(),
            poBaseDir = tempDir.resolve("po").toString()
        )

        val target = RoqAppServiceImpl(roqDriver, poAppService, gitTimestampDriver, siteService, config)
        target.serve(translate = true)

        verify(roqDriver).prepareSource(any(), any())
        verify(roqDriver).applyOverrides(any(), any())
        verify(poAppService).applyPoToDirectory(
            workDir = any(),
            poBaseDir = any(),
            skipAsciidoc = eq(false),
            htmlIncludeList = any(),
            yamlExcludeList = any()
        )
        verify(roqDriver).serve(any(), isNull(), any(), any())
    }

    @Test
    fun build_with_skipAsciidoc_should_pass_flag(@TempDir tempDir: Path) {
        val config = createConfig(
            sourceDir = tempDir.resolve("upstream").toString(),
            overrideDir = tempDir.resolve("override").toString(),
            destinationDir = tempDir.resolve("output").toString(),
            poBaseDir = tempDir.resolve("po").toString()
        )

        val target = RoqAppServiceImpl(roqDriver, poAppService, gitTimestampDriver, siteService, config)
        target.build(translate = true, skipAsciidoc = true)

        verify(poAppService).applyPoToDirectory(
            workDir = any(),
            poBaseDir = any(),
            skipAsciidoc = eq(true),
            htmlIncludeList = any(),
            yamlExcludeList = any()
        )
    }

    @Test
    fun updateRoqStats_should_process_configured_sections(@TempDir tempDir: Path) {
        val poBaseDir = tempDir.resolve("po")
        val statsDir = tempDir.resolve("stats")

        val config = createConfig(
            poBaseDir = poBaseDir.toString(),
            statsDir = statsDir.toString(),
            statsSections = mapOf(
                "latest-guides" to "content/guides",
                "posts" to "content/posts"
            ),
            overrideDir = tempDir.resolve("override").toString(),
            sourceDir = tempDir.resolve("upstream").toString()
        )

        poBaseDir.resolve("content/guides").createDirectories()
        poBaseDir.resolve("content/posts").createDirectories()

        val target = RoqAppServiceImpl(roqDriver, poAppService, gitTimestampDriver, siteService, config)
        target.updateRoqStats()

        verify(poAppService).updatePoStats(
            argThat<List<Path>> { size == 1 && first().toString().contains("content/guides") },
            argThat<Path> { toString().contains("latest-guides-translation.csv") }
        )
        verify(poAppService).updatePoStats(
            argThat<List<Path>> { size == 1 && first().toString().contains("content/posts") },
            argThat<Path> { toString().contains("posts-translation.csv") }
        )
    }
}
