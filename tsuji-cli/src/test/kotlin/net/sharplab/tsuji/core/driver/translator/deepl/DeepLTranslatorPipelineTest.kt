package net.sharplab.tsuji.core.driver.translator.deepl

import com.deepl.api.TextResult
import net.sharplab.tsuji.app.config.TsujiConfig
import net.sharplab.tsuji.core.driver.translator.processor.AsciidoctorPreProcessor
import net.sharplab.tsuji.core.model.po.MessageType
import net.sharplab.tsuji.core.model.po.Po
import net.sharplab.tsuji.core.model.po.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.asciidoctor.Asciidoctor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.slf4j.LoggerFactory
import java.util.Optional

/**
 * DeepLTranslator のパイプライン統合テスト。
 * 実際のTranslatorを使用し、翻訳API部分のみをモック化して、
 * パイプライン全体（前処理 → 翻訳 → 後処理）が正しく動作することを確認する。
 */
internal class DeepLTranslatorPipelineTest {

    private val logger = LoggerFactory.getLogger(DeepLTranslatorPipelineTest::class.java)

    private lateinit var asciidoctor: Asciidoctor
    private lateinit var preProcessor: AsciidoctorPreProcessor
    private lateinit var mockDeepLApi: com.deepl.api.Translator
    private lateinit var translator: DeepLTranslator

    @BeforeEach
    fun setUp() {
        asciidoctor = Asciidoctor.Factory.create()
        preProcessor = AsciidoctorPreProcessor(asciidoctor)
        mockDeepLApi = mock()

        // TsujiConfigをモック化（ダミーAPIキーを返す）
        val mockConfig = mock<TsujiConfig>()
        val mockTranslator = mock<TsujiConfig.Translator>()
        val mockDeepL = mock<TsujiConfig.Translator.DeepL>()
        whenever(mockConfig.translator).thenReturn(mockTranslator)
        whenever(mockTranslator.deepl).thenReturn(mockDeepL)
        whenever(mockDeepL.apiKey).thenReturn(Optional.of("dummy-api-key"))

        // モックAPIを注入
        translator = DeepLTranslator(mockConfig, preProcessor, mockDeepLApi)
    }

    @AfterEach
    fun tearDown() {
        preProcessor.close()
        asciidoctor.shutdown()
    }

    private fun createMessage(
        messageId: String,
        messageString: String = "",
        type: MessageType = MessageType.PlainText
    ) = PoMessage(
        type = type,
        messageId = messageId,
        messageString = messageString,
        sourceReferences = emptyList()
    )

    @Test
    fun `should process Asciidoctor link tags through full pipeline`() {
        // Given: Asciidoctorフォーマットのリンクを含むメッセージ
        val message = createMessage("This is a link:https://example.com[example site].")
        val po = Po("ja", listOf(message))

        // ArgumentCaptorでモックに渡される引数をキャプチャ
        val textsCaptor = argumentCaptor<List<String>>()

        // Mock翻訳API: HTMLタグをそのまま保持して日本語に翻訳
        whenever(mockDeepLApi.translateText(textsCaptor.capture(), eq("en"), eq("ja"), any()))
            .thenReturn(listOf(
                TextResult("これは<a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://example.com\">サンプルサイト</a>です。", "en")
            ))

        // When: パイプライン実行
        val result = translator.translate(po, "en", "ja", isAsciidoctor = true, useRag = false)

        // Then: AsciidoctorPreProcessorでHTML化されていることを確認
        val capturedTexts = textsCaptor.firstValue
        assertThat(capturedTexts).hasSize(1)
        assertThat(capturedTexts[0])
            .describedAs("AsciidoctorPreProcessor should convert link: syntax to HTML")
            .contains("<a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://example.com\">example site</a>")

        // LinkTagMessageProcessorでAsciidoc構文に戻される
        assertThat(result.messages).hasSize(1)
        assertThat(result.messages[0].messageString).isEqualTo("これは link:https://example.com[サンプルサイト] です。")
        assertThat(result.messages[0].fuzzy).isTrue()
    }

    @Test
    fun `should process Asciidoctor image tags through full pipeline`() {
        // Given: 画像を含むメッセージ
        val message = createMessage("See image:diagram.png[diagram]")
        val po = Po("ja", listOf(message))

        // Mock翻訳API: HTMLタグをそのまま保持
        whenever(mockDeepLApi.translateText(any<List<String>>(), eq("en"), eq("ja"), any()))
            .thenReturn(listOf(
                TextResult("<span class=\"image\"><img src=\"diagram.png\" alt=\"図\"></span>を参照してください", "en")
            ))

        // When
        val result = translator.translate(po, "en", "ja", isAsciidoctor = true, useRag = false)

        // Then: 画像タグがAsciidoctor構文に戻される
        assertThat(result.messages[0].messageString).isEqualTo("image:diagram.png[alt=\"図\"] を参照してください")
    }

    @Test
    fun `should process decoration tags through full pipeline`() {
        // Given: 装飾タグを含むメッセージ
        val message = createMessage("This is *bold* and _italic_ text.")
        val po = Po("ja", listOf(message))

        // Mock翻訳API: HTML形式で返す
        whenever(mockDeepLApi.translateText(any<List<String>>(), eq("en"), eq("ja"), any()))
            .thenReturn(listOf(
                TextResult("これは <strong>太字</strong> と <em>斜体</em> テキストです。", "en")
            ))

        // When
        val result = translator.translate(po, "en", "ja", isAsciidoctor = true, useRag = false)

        // Then: 装飾タグがAsciidoctor構文に戻される
        assertThat(result.messages[0].messageString).isEqualTo("これは *太字* と _斜体_ テキストです。")
    }

    @Test
    fun `should handle complex nested tags`() {
        // Given: 複雑な複合タグ
        val message = createMessage("Click *link:https://example.com[this link]* now.")
        val po = Po("ja", listOf(message))

        // Mock翻訳API
        whenever(mockDeepLApi.translateText(any<List<String>>(), eq("en"), eq("ja"), any()))
            .thenReturn(listOf(
                TextResult("今すぐ <strong><a data-doc-l10n-kit-type=\"link\" data-doc-l10n-kit-target=\"https://example.com\">このリンク</a></strong> をクリックしてください。", "en")
            ))

        // When
        val result = translator.translate(po, "en", "ja", isAsciidoctor = true, useRag = false)

        // Then: すべてのタグが正しく変換される
        assertThat(result.messages[0].messageString).isEqualTo("今すぐ *link:https://example.com[このリンク]* をクリックしてください。")
    }

    @Test
    fun `should unescape HTML entities`() {
        // Given: HTMLエンティティを含むメッセージ
        val message = createMessage("Use `<tag>` and `&amp;` symbols.")
        val po = Po("ja", listOf(message))

        // Mock翻訳API: HTMLエンティティ付きで返す
        whenever(mockDeepLApi.translateText(any<List<String>>(), eq("en"), eq("ja"), any()))
            .thenReturn(listOf(
                TextResult("<code>&lt;tag&gt;</code> と <code>&amp;amp;</code> 記号を使います。", "en")
            ))

        // When
        val result = translator.translate(po, "en", "ja", isAsciidoctor = true, useRag = false)

        // Then: HTMLエンティティがアンエスケープされる
        assertThat(result.messages[0].messageString).isEqualTo("`<tag>` と `&` 記号を使います。")
    }

    @Test
    fun `should skip preprocessing and postprocessing when not Asciidoctor mode`() {
        // Given: Asciidoctorマークアップを含むが、非Asciidoctorモード
        val message = createMessage("This is *bold* text.")
        val po = Po("ja", listOf(message))

        // Mock翻訳API: プレーンテキストとして翻訳
        whenever(mockDeepLApi.translateText(any<List<String>>(), eq("en"), eq("ja"), any()))
            .thenReturn(listOf(
                TextResult("これは*太字*テキストです。", "en")
            ))

        // When: isAsciidoctor = false
        val result = translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)

        // Then: マークアップがそのまま（前処理・後処理がスキップされる）
        assertThat(result.messages[0].messageString).isEqualTo("これは*太字*テキストです。")
    }

    @Test
    fun `should handle multiple messages in batch`() {
        // Given: 複数メッセージ
        val messages = listOf(
            createMessage("First *message*."),
            createMessage("Second _message_."),
            createMessage("Third message.")
        )
        val po = Po("ja", listOf(messages[0], messages[1], messages[2]))

        // Mock翻訳API: バッチで翻訳
        whenever(mockDeepLApi.translateText(any<List<String>>(), eq("en"), eq("ja"), any()))
            .thenReturn(listOf(
                TextResult("最初の <strong>メッセージ</strong> 。", "en"),
                TextResult("2番目の <em>メッセージ</em> 。", "en"),
                TextResult("3番目のメッセージ。", "en")
            ))

        // When
        val result = translator.translate(po, "en", "ja", isAsciidoctor = true, useRag = false)

        // Then: すべてのメッセージが正しく処理される
        assertThat(result.messages).hasSize(3)
        assertThat(result.messages[0].messageString).isEqualTo("最初の *メッセージ* 。")
        assertThat(result.messages[1].messageString).isEqualTo("2番目の _メッセージ_ 。")
        assertThat(result.messages[2].messageString).isEqualTo("3番目のメッセージ。")
    }

    @Test
    fun `should skip already translated messages`() {
        // Given: すでに翻訳済みのメッセージ
        val message = createMessage(messageId = "Hello", messageString = "こんにちは")
        val po = Po("ja", listOf(message))

        // When: 翻訳実行（APIは呼ばれないはず）
        val result = translator.translate(po, "en", "ja", isAsciidoctor = false, useRag = false)

        // Then: 既存の翻訳がそのまま残る
        assertThat(result.messages[0].messageString).isEqualTo("こんにちは")

        // API呼び出しが行われていないことを確認
        verify(mockDeepLApi, never()).translateText(any<List<String>>(), any<String>(), any<String>())
    }
}
