package net.sharplab.tsuji.core.driver.translator.validator

import net.sharplab.tsuji.core.driver.translator.exception.AsciidocMarkupValidationException
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.model.PoMessage
import org.asciidoctor.Asciidoctor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsciidocMarkupValidatorTest {

    private lateinit var asciidoctor: Asciidoctor
    private lateinit var validator: AsciidocMarkupValidator

    @BeforeAll
    fun setup() {
        asciidoctor = Asciidoctor.Factory.create()
        validator = AsciidocMarkupValidator(asciidoctor)
    }

    @AfterAll
    fun teardown() {
        asciidoctor.shutdown()
    }

    private fun msg(source: String, translated: String) = TranslationMessage(
        original = PoMessage(messageId = source, messageString = "", sourceReferences = emptyList()),
        text = translated,
        needsTranslation = true
    )

    // --- extractMarkupFeatures tests ---

    @Test
    fun `extractMarkupFeatures should extract links`() {
        val features = validator.extractMarkupFeatures("Visit link:https://quarkus.io[Quarkus] and https://github.com for more.")
        assertThat(features.linkHrefs).containsExactlyInAnyOrder("https://quarkus.io", "https://github.com")
    }

    @Test
    fun `extractMarkupFeatures should extract images`() {
        val features = validator.extractMarkupFeatures("See image:logo.png[Logo] for details.")
        assertThat(features.imageSrcs).contains("logo.png")
    }

    @Test
    fun `extractMarkupFeatures should count emphasis and strong`() {
        val features = validator.extractMarkupFeatures("This is *bold* and _italic_ and *another bold*.")
        assertThat(features.strongCount).isEqualTo(2)
        assertThat(features.emphasisCount).isEqualTo(1)
    }

    @Test
    fun `extractMarkupFeatures should count code`() {
        val features = validator.extractMarkupFeatures("Use `+foo()+` and `+bar()+` methods.")
        assertThat(features.codeCount).isEqualTo(2)
    }

    @Test
    fun `extractMarkupFeatures should return empty for plain text`() {
        val features = validator.extractMarkupFeatures("Plain text without markup.")
        assertThat(features.linkHrefs).isEmpty()
        assertThat(features.imageSrcs).isEmpty()
        assertThat(features.emphasisCount).isEqualTo(0)
        assertThat(features.strongCount).isEqualTo(0)
        assertThat(features.codeCount).isEqualTo(0)
    }

    // --- validate tests ---

    @Test
    fun `validate should pass when markup is preserved`() {
        assertThatCode {
            validator.validate(listOf(
                msg("Visit link:https://quarkus.io[Quarkus].", "link:https://quarkus.io[Quarkus] をご覧ください。")
            ))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `validate should throw when link is broken`() {
        assertThatThrownBy {
            validator.validate(listOf(
                msg("Visit link:https://quarkus.io[Quarkus].", "Quarkusをご覧ください。")
            ))
        }.isInstanceOf(AsciidocMarkupValidationException::class.java)
            .satisfies({ e ->
                assertThat((e as AsciidocMarkupValidationException).brokenTranslations).hasSize(1)
            })
    }

    @Test
    fun `validate should throw when emphasis is dropped`() {
        assertThatThrownBy {
            validator.validate(listOf(
                msg("This is *important*.", "これは重要です。")
            ))
        }.isInstanceOf(AsciidocMarkupValidationException::class.java)
    }

    @Test
    fun `validate should pass for messages without markup`() {
        assertThatCode {
            validator.validate(listOf(
                msg("Plain text.", "プレーンテキスト。")
            ))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `validate should report only broken messages`() {
        assertThatThrownBy {
            validator.validate(listOf(
                msg("link:https://quarkus.io[OK].", "link:https://quarkus.io[OK]。"),
                msg("link:https://broken.com[Broken].", "壊れた。")
            ))
        }.isInstanceOf(AsciidocMarkupValidationException::class.java)
            .satisfies({ e ->
                val ex = e as AsciidocMarkupValidationException
                assertThat(ex.brokenTranslations).hasSize(1)
                assertThat(ex.brokenTranslations[0].message.original.messageId).contains("broken.com")
            })
    }

    // --- Backtick spacing tests (Issue #1160) ---

    @Test
    fun `validate should detect missing space after backtick with Japanese text`() {
        // When backtick is immediately followed by Japanese text without space,
        // Asciidoc parser treats it as literal backtick, not inline code
        assertThatThrownBy {
            validator.validate(listOf(
                msg(
                    "`curl` command for `/secured/roles-allowed`",
                    "`/secured/roles-allowed`に対する`curl`コマンド"
                )
            ))
        }.isInstanceOf(AsciidocMarkupValidationException::class.java)
            .satisfies({ e ->
                val ex = e as AsciidocMarkupValidationException
                assertThat(ex.brokenTranslations).hasSize(1)
            })
    }

    @Test
    fun `validate should pass when backtick has proper spacing with Japanese text`() {
        assertThatCode {
            validator.validate(listOf(
                msg(
                    "`curl` command for `/secured/roles-allowed`",
                    "`/secured/roles-allowed` に対する `curl` コマンド"
                )
            ))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `extractMarkupFeatures should detect code blocks with proper spacing`() {
        val goodTranslation = "`/secured/roles-allowed` に対する `curl` コマンド"
        val features = validator.extractMarkupFeatures(goodTranslation)
        assertThat(features.codeCount).isEqualTo(2)
    }

    @Test
    fun `extractMarkupFeatures should not recognize backticks without spacing as code`() {
        val badTranslation = "`/secured/roles-allowed`に対する`curl`コマンド"
        val features = validator.extractMarkupFeatures(badTranslation)
        // If spacing is missing, Asciidoc parser treats backticks as literal characters
        // So code count should be 0 or less than expected
        assertThat(features.codeCount).isLessThan(2)
    }

    // --- xref tests ---

    @Test
    fun `extractMarkupFeatures should extract xref links`() {
        val features = validator.extractMarkupFeatures("See xref:telemetry-micrometer.adoc[Micrometer] for details.")
        assertThat(features.linkHrefs).isNotEmpty()
    }

    @Test
    fun `validate should pass when xref is preserved with proper spacing`() {
        assertThatCode {
            validator.validate(listOf(
                msg(
                    "Implemented with the xref:telemetry-micrometer.adoc[Micrometer] extension.",
                    "xref:telemetry-micrometer.adoc[Micrometer] エクステンションで実装されています。"
                )
            ))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `validate should throw when xref is removed`() {
        assertThatThrownBy {
            validator.validate(listOf(
                msg(
                    "Implemented with the xref:telemetry-micrometer.adoc[Micrometer] extension.",
                    "Micrometer エクステンションで実装されています。"
                )
            ))
        }.isInstanceOf(AsciidocMarkupValidationException::class.java)
    }

    @Test
    fun `validate should pass when inline xref anchors are unchanged`() {
        assertThatCode {
            validator.validate(listOf(
                msg(
                    "See <<getting-started>> for details.",
                    "詳細は <<getting-started>> を参照してください。"
                )
            ))
        }.doesNotThrowAnyException()
    }
}
