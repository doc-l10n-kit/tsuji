package net.sharplab.tsuji.app.service

import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.gettext.GettextDriver
import net.sharplab.tsuji.core.driver.jekyll.JekyllDriver
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.po4a.Po4aDriver
import net.sharplab.tsuji.core.service.PoNormalizerService
import net.sharplab.tsuji.core.service.PoService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.*
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class PoAppServiceImplRoqTest {

    private val gettextDriver: GettextDriver = mock()
    private val poDriver: PoDriver = mock()
    private val po4aDriver: Po4aDriver = mock()
    private val jekyllDriver: JekyllDriver = mock()
    private val poService: PoService = mock()
    private val poNormalizerService: PoNormalizerService = mock()

    private fun createMockConfig(
        roqSourceDir: String = "upstream",
        roqOverrideDir: String = "l10n/override/ja_JP",
        roqYamlExclude: List<String> = emptyList(),
        roqHtmlInclude: List<String> = emptyList(),
        poBaseDir: String = "l10n/po/ja_JP",
        jekyllYamlExclude: List<String> = emptyList(),
        jekyllHtmlInclude: List<String> = emptyList()
    ): TsujiConfig {
        val roqYaml: TsujiConfig.Roq.Extract.Yaml = mock()
        whenever(roqYaml.exclude).thenReturn(Optional.of(roqYamlExclude))
        val roqHtml: TsujiConfig.Roq.Extract.Html = mock()
        whenever(roqHtml.include).thenReturn(Optional.of(roqHtmlInclude))
        val roqExtract: TsujiConfig.Roq.Extract = mock()
        whenever(roqExtract.yaml).thenReturn(roqYaml)
        whenever(roqExtract.html).thenReturn(roqHtml)
        val roq: TsujiConfig.Roq = mock()
        whenever(roq.sourceDir).thenReturn(roqSourceDir)
        whenever(roq.overrideDir).thenReturn(roqOverrideDir)
        whenever(roq.extract).thenReturn(roqExtract)

        val jekyllYaml: TsujiConfig.Jekyll.Extract.Yaml = mock()
        whenever(jekyllYaml.exclude).thenReturn(Optional.of(jekyllYamlExclude))
        val jekyllHtml: TsujiConfig.Jekyll.Extract.Html = mock()
        whenever(jekyllHtml.include).thenReturn(Optional.of(jekyllHtmlInclude))
        val jekyllExtract: TsujiConfig.Jekyll.Extract = mock()
        whenever(jekyllExtract.yaml).thenReturn(jekyllYaml)
        whenever(jekyllExtract.html).thenReturn(jekyllHtml)
        val jekyll: TsujiConfig.Jekyll = mock()
        whenever(jekyll.extract).thenReturn(jekyllExtract)

        val po: TsujiConfig.Po = mock()
        whenever(po.baseDir).thenReturn(poBaseDir)

        val config: TsujiConfig = mock()
        whenever(config.po).thenReturn(po)
        whenever(config.roq).thenReturn(roq)
        whenever(config.jekyll).thenReturn(jekyll)
        return config
    }

    // --- extractRoq tests ---

    @Test
    fun extractRoq_should_skip_asciidoc_files(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("upstream").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        sourceDir.resolve("content/guides").createDirectories()
            .resolve("getting-started.adoc").writeText("= Getting Started")

        whenever(po4aDriver.determineFormat(any())).thenAnswer { invocation ->
            val path = invocation.getArgument<Path>(0)
            when (path.toString().substringAfterLast('.')) {
                "adoc" -> "asciidoc"
                else -> null
            }
        }

        val config = createMockConfig(
            roqSourceDir = sourceDir.toString(),
            roqOverrideDir = tempDir.resolve("override").toString(),
            poBaseDir = poBaseDir.toString()
        )

        val target = PoAppServiceImpl(gettextDriver, poDriver, po4aDriver, jekyllDriver, poService, poNormalizerService, config)
        target.extractRoq(poBaseDir, sourceDir, tempDir.resolve("override"))

        verify(po4aDriver, never()).updatePo(
            argThat<Path> { toString().contains("getting-started.adoc") },
            any(), eq("asciidoc"), any()
        )
    }

    @Test
    fun extractRoq_should_not_invoke_jekyll_driver(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("upstream").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        sourceDir.resolve("guide.adoc").writeText("= Guide")

        whenever(po4aDriver.determineFormat(any())).thenReturn("asciidoc")

        val config = createMockConfig(
            roqSourceDir = sourceDir.toString(),
            roqOverrideDir = tempDir.resolve("override").toString(),
            poBaseDir = poBaseDir.toString()
        )

        val target = PoAppServiceImpl(gettextDriver, poDriver, po4aDriver, jekyllDriver, poService, poNormalizerService, config)
        target.extractRoq(poBaseDir, sourceDir, tempDir.resolve("override"))

        verify(jekyllDriver, never()).extractPo(any(), any(), any())
    }

    @Test
    fun extractRoq_should_respect_yaml_exclude_filter(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("upstream").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        sourceDir.resolve("_data").createDirectories()
            .resolve("versions.yaml").writeText("versions: []")
        sourceDir.resolve("_data")
            .resolve("guides.yaml").writeText("guides: []")

        whenever(po4aDriver.determineFormat(any())).thenAnswer { invocation ->
            val path = invocation.getArgument<Path>(0)
            when (path.toString().substringAfterLast('.')) {
                "yaml", "yml" -> "yaml"
                else -> null
            }
        }

        val config = createMockConfig(
            roqSourceDir = sourceDir.toString(),
            roqOverrideDir = tempDir.resolve("override").toString(),
            poBaseDir = poBaseDir.toString(),
            roqYamlExclude = listOf("_data/versions.yaml")
        )

        val target = PoAppServiceImpl(gettextDriver, poDriver, po4aDriver, jekyllDriver, poService, poNormalizerService, config)
        target.extractRoq(poBaseDir, sourceDir, tempDir.resolve("override"))

        verify(po4aDriver).updatePo(
            argThat<Path> { toString().contains("guides.yaml") },
            any(), eq("yaml"), any()
        )
        verify(po4aDriver, never()).updatePo(
            argThat<Path> { toString().contains("versions.yaml") },
            any(), any(), any()
        )
    }

    @Test
    fun extractRoq_should_respect_html_include_filter(@TempDir tempDir: Path) {
        val sourceDir = tempDir.resolve("upstream").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        sourceDir.resolve("templates/partials").createDirectories()
        sourceDir.resolve("templates/partials/about.html").writeText("<div>About</div>")
        sourceDir.resolve("templates/partials/nav.html").writeText("<nav>Nav</nav>")

        whenever(po4aDriver.determineFormat(any())).thenAnswer { invocation ->
            val path = invocation.getArgument<Path>(0)
            when (path.toString().substringAfterLast('.')) {
                "html" -> "xhtml"
                else -> null
            }
        }

        val config = createMockConfig(
            roqSourceDir = sourceDir.toString(),
            roqOverrideDir = tempDir.resolve("override").toString(),
            poBaseDir = poBaseDir.toString(),
            roqHtmlInclude = listOf("templates/partials/about.html")
        )

        val target = PoAppServiceImpl(gettextDriver, poDriver, po4aDriver, jekyllDriver, poService, poNormalizerService, config)
        target.extractRoq(poBaseDir, sourceDir, tempDir.resolve("override"))

        verify(po4aDriver).updatePo(
            argThat<Path> { toString().contains("about.html") },
            any(), eq("xhtml"), any()
        )
        verify(po4aDriver, never()).updatePo(
            argThat<Path> { toString().contains("nav.html") },
            any(), any(), any()
        )
    }

    // --- applyPoToDirectory tests ---

    @Test
    fun applyPoToDirectory_should_skip_asciidoc(@TempDir tempDir: Path) {
        val workDir = tempDir.resolve("work").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        workDir.resolve("guide.adoc").writeText("= Guide")
        poBaseDir.resolve("guide.adoc.po").writeText("po content")

        whenever(po4aDriver.determineFormat(any())).thenReturn("asciidoc")
        whenever(poService.isIgnored(any())).thenReturn(false)

        val config = createMockConfig()
        val target = PoAppServiceImpl(gettextDriver, poDriver, po4aDriver, jekyllDriver, poService, poNormalizerService, config)
        target.applyPoToDirectory(workDir, poBaseDir)

        verify(po4aDriver, never()).translate(any(), any(), any(), any(), any())
    }

    @Test
    fun applyPoToDirectory_should_use_explicit_filter_params(@TempDir tempDir: Path) {
        val workDir = tempDir.resolve("work").createDirectories()
        val poBaseDir = tempDir.resolve("po").createDirectories()

        workDir.resolve("page.html").writeText("<div>Page</div>")
        poBaseDir.resolve("page.html.po").writeText("po content")

        whenever(po4aDriver.determineFormat(any())).thenReturn("xhtml")
        whenever(poService.isIgnored(any())).thenReturn(false)

        val config = createMockConfig(jekyllHtmlInclude = emptyList())
        val target = PoAppServiceImpl(gettextDriver, poDriver, po4aDriver, jekyllDriver, poService, poNormalizerService, config)

        // Without explicit param, uses Jekyll config (empty include = skip all HTML)
        target.applyPoToDirectory(workDir, poBaseDir)
        verify(po4aDriver, never()).translate(any(), any(), any(), any(), any())

        // With explicit param including the file, should translate
        target.applyPoToDirectory(workDir, poBaseDir,
            htmlIncludeList = listOf("page.html"))
        verify(po4aDriver).translate(any(), any(), any(), eq("xhtml"), any())
    }
}
