package net.sharplab.tsuji.systemtest

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import net.sharplab.tsuji.core.driver.po.PoDriver
import net.sharplab.tsuji.core.driver.translator.Translator
import net.sharplab.tsuji.core.service.PoTranslatorService
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Translator comparison system test.
 *
 * Tests machine translation of multiple PO files using both DeepL and Gemini translators.
 * Uses real PO files from the ja.quarkus.io project (10 files, ~320 messages total).
 */
@QuarkusTest
class TranslatorComparisonTest {

    private val logger = LoggerFactory.getLogger(TranslatorComparisonTest::class.java)

    @Inject
    lateinit var translator: Translator

    @Inject
    lateinit var poTranslatorService: PoTranslatorService

    @Inject
    lateinit var poDriver: PoDriver

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun checkApiKey() {
        val config = ConfigProvider.getConfig()
        val translatorType = config.getValue("tsuji.translator.type", String::class.java)

        when (translatorType.lowercase()) {
            "deepl" -> {
                val apiKey = config.getOptionalValue("tsuji.translator.deepl.api-key", String::class.java)
                Assumptions.assumeTrue(
                    apiKey.isPresent && apiKey.get().isNotBlank(),
                    "DeepL API Key is not configured, skipping test."
                )
            }
            "gemini" -> {
                val apiKey = config.getOptionalValue("quarkus.langchain4j.ai.gemini.api-key", String::class.java)
                Assumptions.assumeTrue(
                    apiKey.isPresent && apiKey.get().isNotBlank(),
                    "Gemini API Key is not configured, skipping test."
                )
            }
        }
    }

    @Test
    fun `translate multiple PO files - should translate all messages successfully`() {
        // Given: Copy test PO files to temp directory
        val testResourceDir = Path.of("src/systemTest/resources/po/translator-comparison")
        val poFiles = Files.walk(testResourceDir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "po" }
                .toList()
        }

        assertThat(poFiles).isNotEmpty()
        logger.info("Found ${poFiles.size} PO files to translate")

        val testDir = tempDir.resolve("test-po-files")
        Files.createDirectories(testDir)

        val copiedFiles = poFiles.map { sourceFile ->
            val targetFile = testDir.resolve(sourceFile.fileName)
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
            targetFile
        }

        // When: Translate all files
        val startTime = System.currentTimeMillis()
        val results = copiedFiles.map { poFile ->
            logger.info("Translating: ${poFile.name}")

            val po = poDriver.load(poFile)
            val originalMessageCount = po.messages.size

            // Count untranslated messages before translation
            val untranslatedBefore = po.messages.count { it.messageString.isBlank() }

            val translated = poTranslatorService.translate(
                po,
                source = "en",
                target = "ja",
                isAsciidoctor = true,
                useRag = false
            )

            poDriver.save(translated, poFile)

            // Count untranslated messages after translation
            val translatedPo = poDriver.load(poFile)
            val untranslatedAfter = translatedPo.messages.count { it.messageString.isBlank() }
            val translatedCount = untranslatedBefore - untranslatedAfter

            TranslationResult(
                fileName = poFile.name,
                totalMessages = originalMessageCount,
                translatedCount = translatedCount,
                untranslatedAfter = untranslatedAfter
            )
        }
        val elapsedTime = System.currentTimeMillis() - startTime

        // Then: Verify translation results
        val totalMessages = results.sumOf { it.totalMessages }
        val totalTranslated = results.sumOf { it.translatedCount }
        val totalUntranslated = results.sumOf { it.untranslatedAfter }
        val completionRate = (totalTranslated.toDouble() / totalMessages * 100)

        logger.info("=" * 80)
        logger.info("Translation Results")
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

        // Allow some tolerance for translation failures
        // Gemini should achieve close to 100%, DeepL may have some failures
        val config = ConfigProvider.getConfig()
        val translatorType = config.getValue("tsuji.translator.type", String::class.java)

        when (translatorType.lowercase()) {
            "gemini" -> {
                // Gemini should achieve high completion rate (>95%)
                assertThat(completionRate)
                    .describedAs("Gemini should translate most messages successfully")
                    .isGreaterThan(95.0)
            }
            "deepl" -> {
                // DeepL may have tag handling issues, so we allow lower completion rate
                // but it should still translate at least some messages
                assertThat(completionRate)
                    .describedAs("DeepL should translate at least 30% of messages")
                    .isGreaterThan(30.0)
            }
        }
    }

    @Test
    fun `translate single large PO file - should handle large files correctly`() {
        // Given: Find the largest PO file (security-webauthn.adoc.po or similar)
        val testResourceDir = Path.of("src/systemTest/resources/po/translator-comparison")
        val poFiles = Files.walk(testResourceDir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "po" }
                .toList()
        }

        val largestFile = poFiles.maxByOrNull { Files.size(it) }

        assertThat(largestFile).isNotNull()
        logger.info("Testing with largest file: ${largestFile!!.fileName} (${Files.size(largestFile)} bytes)")

        val testFile = tempDir.resolve(largestFile.fileName)
        Files.copy(largestFile, testFile, StandardCopyOption.REPLACE_EXISTING)

        // When: Translate the file
        val po = poDriver.load(testFile)
        val untranslatedBefore = po.messages.count { it.messageString.isBlank() }

        val startTime = System.currentTimeMillis()
        val translated = poTranslatorService.translate(
            po,
            source = "en",
            target = "ja",
            isAsciidoctor = true,
            useRag = false
        )
        val elapsedTime = System.currentTimeMillis() - startTime

        poDriver.save(translated, testFile)

        // Then: Verify translation
        val translatedPo = poDriver.load(testFile)
        val untranslatedAfter = translatedPo.messages.count { it.messageString.isBlank() }
        val translatedCount = untranslatedBefore - untranslatedAfter
        val completionRate = (translatedCount.toDouble() / po.messages.size * 100)

        logger.info("Large file translation result:")
        logger.info("  File: ${testFile.name}")
        logger.info("  Total messages: ${po.messages.size}")
        logger.info("  Translated: $translatedCount")
        logger.info("  Untranslated: $untranslatedAfter")
        logger.info("  Completion rate: ${String.format("%.1f", completionRate)}%")
        logger.info("  Time: ${elapsedTime}ms")

        // Assertions
        assertThat(translatedCount).isGreaterThan(0)

        // Check translator-specific expectations
        val config = ConfigProvider.getConfig()
        val translatorType = config.getValue("tsuji.translator.type", String::class.java)

        when (translatorType.lowercase()) {
            "gemini" -> {
                // Gemini should handle large files well
                assertThat(completionRate)
                    .describedAs("Gemini should handle large files with high completion rate")
                    .isGreaterThan(90.0)
            }
            "deepl" -> {
                // DeepL may struggle with large files due to tag handling
                // We just verify it attempts translation
                assertThat(translatedCount)
                    .describedAs("DeepL should translate at least some messages in large files")
                    .isGreaterThan(0)
            }
        }
    }

    private data class TranslationResult(
        val fileName: String,
        val totalMessages: Int,
        val translatedCount: Int,
        val untranslatedAfter: Int
    )

    private operator fun String.times(count: Int): String = this.repeat(count)
}
