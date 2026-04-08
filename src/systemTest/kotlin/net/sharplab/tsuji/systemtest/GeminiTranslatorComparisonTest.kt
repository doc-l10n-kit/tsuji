package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.main.QuarkusMainLauncher
import io.quarkus.test.junit.main.QuarkusMainTest
import io.quarkus.test.junit.TestProfile
import net.sharplab.tsuji.core.driver.po.PoDriverImpl
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Gemini translator comparison system test.
 *
 * Tests machine translation of multiple PO files using Gemini translator via CLI.
 * Uses real PO files from the ja.quarkus.io project (10 files, ~320 messages total).
 */
@QuarkusMainTest
@TestProfile(GeminiTranslatorProfile::class)
class GeminiTranslatorComparisonTest {

    private val logger = LoggerFactory.getLogger(GeminiTranslatorComparisonTest::class.java)
    private val poDriver = PoDriverImpl()
    private val outputDir = Path.of("build/translation-comparison/gemini")

    @BeforeEach
    fun checkApiKey() {
        val config = ConfigProvider.getConfig()
        val apiKey = config.getOptionalValue("quarkus.langchain4j.ai.gemini.api-key", String::class.java)
        Assumptions.assumeTrue(
            apiKey.isPresent && apiKey.get().isNotBlank(),
            "Gemini API Key is not configured, skipping test."
        )
    }

    @Test
    fun `translate multiple PO files - should translate all messages successfully`(launcher: QuarkusMainLauncher) {
        // Given: Copy test PO files to temp directory
        val testResourceDir = Path.of("src/systemTest/resources/po/translator-comparison")
        val poFiles = Files.walk(testResourceDir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "po" }
                .toList()
        }

        assertThat(poFiles).isNotEmpty()
        logger.info("Found ${poFiles.size} PO files to translate")

        val testDir = outputDir.resolve("test-po-files")
        Files.createDirectories(testDir)

        val copiedFiles = poFiles.map { sourceFile ->
            val targetFile = testDir.resolve(sourceFile.fileName)
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
            targetFile
        }

        // Count untranslated messages before translation
        val beforeStats = copiedFiles.map { poFile ->
            val po = poDriver.load(poFile)
            poFile.name to TranslationStats(
                totalMessages = po.messages.size,
                untranslatedBefore = po.messages.count { it.messageString.isBlank() }
            )
        }.toMap()

        // When: Translate all files using CLI
        val startTime = System.currentTimeMillis()
        val result = launcher.launch(
            "po", "machine-translate",
            "--po", testDir.toString(),
            "--source", "en",
            "--target", "ja",
            "--rag=false"
        )
        val elapsedTime = System.currentTimeMillis() - startTime

        // Then: Verify translation results
        assertThat(result.exitCode()).isEqualTo(0)

        val results = copiedFiles.map { poFile ->
            val stats = beforeStats[poFile.name]!!
            val translatedPo = poDriver.load(poFile)
            val untranslatedAfter = translatedPo.messages.count { it.messageString.isBlank() }
            val translatedCount = stats.untranslatedBefore - untranslatedAfter

            TranslationResult(
                fileName = poFile.name,
                totalMessages = stats.totalMessages,
                translatedCount = translatedCount,
                untranslatedAfter = untranslatedAfter
            )
        }

        val totalMessages = results.sumOf { it.totalMessages }
        val totalTranslated = results.sumOf { it.translatedCount }
        val totalUntranslated = results.sumOf { it.untranslatedAfter }
        val completionRate = (totalTranslated.toDouble() / totalMessages * 100)

        logger.info("=" * 80)
        logger.info("Gemini Translation Results")
        logger.info("=" * 80)
        results.forEach { result ->
            logger.info("  ${result.fileName}: ${result.translatedCount}/${result.totalMessages} translated " +
                    "(${result.untranslatedAfter} untranslated)")
        }
        logger.info("-" * 80)
        logger.info("Total: $totalTranslated/$totalMessages translated (${String.format("%.1f", completionRate)}%)")
        logger.info("Untranslated: $totalUntranslated")
        logger.info("Time: ${elapsedTime}ms (${elapsedTime / 1000}s)")
        logger.info("=" * 80)

        // Assertions
        assertThat(results).hasSizeGreaterThanOrEqualTo(10)
        assertThat(totalMessages).isGreaterThanOrEqualTo(180) // Should have at least 180 messages total (10 files)
        assertThat(totalTranslated).isGreaterThan(0) // At least some messages should be translated

        // Gemini should achieve high completion rate (>95%)
        assertThat(completionRate)
            .describedAs("Gemini should translate most messages successfully")
            .isGreaterThan(95.0)
    }

    @Test
    fun `translate single large PO file - should handle large files correctly`(launcher: QuarkusMainLauncher) {
        // Given: Find the largest PO file
        val testResourceDir = Path.of("src/systemTest/resources/po/translator-comparison")
        val poFiles = Files.walk(testResourceDir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "po" }
                .toList()
        }

        val largestFile = poFiles.maxByOrNull { Files.size(it) }

        assertThat(largestFile).isNotNull()
        logger.info("Testing with largest file: ${largestFile!!.fileName} (${Files.size(largestFile)} bytes)")

        val testFile = outputDir.resolve(largestFile.fileName)
        Files.copy(largestFile, testFile, StandardCopyOption.REPLACE_EXISTING)

        // Count untranslated messages before translation
        val po = poDriver.load(testFile)
        val untranslatedBefore = po.messages.count { it.messageString.isBlank() }

        // When: Translate the file using CLI
        val startTime = System.currentTimeMillis()
        val result = launcher.launch(
            "po", "machine-translate",
            "--po", testFile.toString(),
            "--source", "en",
            "--target", "ja",
            "--rag=false"
        )
        val elapsedTime = System.currentTimeMillis() - startTime

        // Then: Verify translation
        assertThat(result.exitCode()).isEqualTo(0)

        val translatedPo = poDriver.load(testFile)
        val untranslatedAfter = translatedPo.messages.count { it.messageString.isBlank() }
        val translatedCount = untranslatedBefore - untranslatedAfter
        val completionRate = (translatedCount.toDouble() / po.messages.size * 100)

        logger.info("Gemini large file translation result:")
        logger.info("  File: ${testFile.name}")
        logger.info("  Total messages: ${po.messages.size}")
        logger.info("  Translated: $translatedCount")
        logger.info("  Untranslated: $untranslatedAfter")
        logger.info("  Completion rate: ${String.format("%.1f", completionRate)}%")
        logger.info("  Time: ${elapsedTime}ms")

        // Assertions
        // Gemini should handle large files well
        assertThat(completionRate)
            .describedAs("Gemini should handle large files with high completion rate")
            .isGreaterThan(90.0)
    }

    private data class TranslationStats(
        val totalMessages: Int,
        val untranslatedBefore: Int
    )

    private data class TranslationResult(
        val fileName: String,
        val totalMessages: Int,
        val translatedCount: Int,
        val untranslatedAfter: Int
    )

    private operator fun String.times(count: Int): String = this.repeat(count)
}
