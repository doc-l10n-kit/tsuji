package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.tmx.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IndexingServiceImplTest {

    private val target = IndexingServiceImpl()

    @Test
    fun convertToSegments_shouldConvertTmxToTextSegments() {
        // Given
        val header = TmxHeader(
            creationTool = "tsuji",
            creationToolVersion = "1.0",
            segType = "sentence",
            oTmf = "UTF-8",
            adminLang = "en",
            srcLang = "en",
            dataType = "PlainText"
        )
        val tus = listOf(
            TranslationUnit(listOf(
                TranslationUnitVariant("en", "Hello"),
                TranslationUnitVariant("ja", "こんにちは")
            )),
            TranslationUnit(listOf(
                TranslationUnitVariant("en", "World"),
                TranslationUnitVariant("ja", "世界")
            ))
        )
        val tmx = Tmx("1.4", header, TmxBody(tus))

        // When
        val result = target.convertToSegments(tmx)

        // Then
        assertThat(result).hasSize(2)
        
        val first = result[0]
        assertThat(first.text()).isEqualTo("Hello")
        // Check metadata assuming "target" and "lang" keys are used
        // Note: Actual metadata keys depend on implementation
    }

    @Test
    fun convertToSegments_withMultipleTargets_shouldCreateMultipleSegments() {
        // Given
        val header = TmxHeader(
            creationTool = "tsuji",
            creationToolVersion = "1.0",
            segType = "sentence",
            oTmf = "UTF-8",
            adminLang = "en",
            srcLang = "en",
            dataType = "PlainText"
        )
        val tus = listOf(
            TranslationUnit(listOf(
                TranslationUnitVariant("en", "Hello"),
                TranslationUnitVariant("ja", "こんにちは"),
                TranslationUnitVariant("fr", "Bonjour")
            ))
        )
        val tmx = Tmx("1.4", header, TmxBody(tus))

        // When
        val result = target.convertToSegments(tmx)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.text() }).containsOnly("Hello")
        // Further assertions on metadata would be ideal
    }
}