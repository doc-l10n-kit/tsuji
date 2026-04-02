package net.sharplab.tsuji.core.service

import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.test.createPoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MessageTranslationServiceImplTest {

    private val target = MessageTranslationServiceImpl()

    @Test
    fun shouldTranslate_shouldReturnTrueForNormalText() {
        // Given
        val msg = createPoMessage("Hello")

        // When
        val result = target.shouldTranslate(msg)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun shouldTranslate_shouldReturnFalseForIgnoredTypes() {
        // Given
        val msg = createPoMessage("some-url", type = MessageType.YAML_HASH_VALUE_LINKS_URL)

        // When
        val result = target.shouldTranslate(msg)

        // Then
        assertThat(result).isFalse()
    }

    /**
     * Jekyll Front Matter の検出と翻訳は、新しいパイプライン実装では
     * DeepLTranslationProcessor と GeminiTranslationProcessor で処理されます。
     *
     * 対応するテスト：
     * - DeepLTranslationProcessorTest.kt (統合テスト内)
     * - GeminiTranslationProcessorTest.kt (統合テスト内)
     *
     * 旧実装でテストしていた内容：
     * ✅ Jekyll Front Matter の検出（layout, title, date, tags, author）
     * ✅ title と synopsis のみ翻訳
     * ✅ 他のフィールドは保持
     */
    @Disabled("新しいパイプライン実装に移行済み。DeepLTranslationProcessorTest と GeminiTranslationProcessorTest を参照")
    @Test
    fun isSpecialFormat_shouldDetectBlogHeader() {
        // 移行先: DeepLTranslationProcessorTest, GeminiTranslationProcessorTest
    }

    @Disabled("新しいパイプライン実装に移行済み。DeepLTranslationProcessorTest と GeminiTranslationProcessorTest を参照")
    @Test
    fun translateSpecialFormat_shouldTranslateOnlyTitleAndSynopsis() {
        // 移行先: DeepLTranslationProcessorTest, GeminiTranslationProcessorTest
    }
}