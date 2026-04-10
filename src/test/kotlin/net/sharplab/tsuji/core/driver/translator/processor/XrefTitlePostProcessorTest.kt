package net.sharplab.tsuji.core.driver.translator.processor

import kotlinx.coroutines.runBlocking
import net.sharplab.tsuji.core.model.translation.TranslationContext
import net.sharplab.tsuji.core.model.translation.TranslationMessage
import net.sharplab.tsuji.po.model.Po
import net.sharplab.tsuji.po.model.PoMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XrefTitlePostProcessorTest {

    private val processor = XrefTitlePostProcessor()

    private fun titleMsg(original: String, translated: String) = TranslationMessage(
        original = PoMessage(
            messageId = original,
            messageString = "",
            sourceReferences = emptyList(),
            comments = listOf("type: Title ==")
        ),
        text = translated,
        needsTranslation = true
    )

    private fun bodyMsg(original: String, translated: String) = TranslationMessage(
        original = PoMessage(
            messageId = original,
            messageString = "",
            sourceReferences = emptyList()
        ),
        text = translated,
        needsTranslation = true
    )

    private fun context(isAsciidoctor: Boolean = true) = TranslationContext(
        po = Po("ja", emptyList()),
        srcLang = "en",
        dstLang = "ja",
        isAsciidoctor = isAsciidoctor,
        useRag = false
    )

    @Test
    fun `should replace xref with section id and translated display text`() {
        runBlocking {
            val messages = listOf(
                titleMsg("WebAuthn endpoints", "WebAuthnエンドポイント"),
                bodyMsg(
                    "See <<WebAuthn endpoints>> for details.",
                    "詳細は <<WebAuthn endpoints>> を参照してください。"
                )
            )

            val result = processor.process(messages, context())

            assertThat(result[1].text).isEqualTo("詳細は <<webauthn-endpoints,WebAuthnエンドポイント>> を参照してください。")
        }
    }

    @Test
    fun `should handle multiple xrefs`() {
        runBlocking {
            val messages = listOf(
                titleMsg("Obtain a registration challenge", "登録チャレンジの取得"),
                titleMsg("Trigger a registration", "登録のトリガー"),
                bodyMsg(
                    "First <<Obtain a registration challenge>>, then <<Trigger a registration>>.",
                    "まず <<Obtain a registration challenge>>、次に <<Trigger a registration>>。"
                )
            )

            val result = processor.process(messages, context())

            assertThat(result[2].text).isEqualTo("まず <<obtain-a-registration-challenge,登録チャレンジの取得>>、次に <<trigger-a-registration,登録のトリガー>>。")
        }
    }

    @Test
    fun `should not modify xref that already has display text`() {
        runBlocking {
            val messages = listOf(
                titleMsg("configuration-reference", "設定リファレンス"),
                bodyMsg(
                    "See <<configuration-reference,session cookie>>.",
                    "<<configuration-reference,セッションCookie>> を参照。"
                )
            )

            val result = processor.process(messages, context())

            assertThat(result[1].text).isEqualTo("<<configuration-reference,セッションCookie>> を参照。")
        }
    }

    @Test
    fun `should handle title with special characters`() {
        runBlocking {
            val messages = listOf(
                titleMsg("Wiley & Sons, Inc.", "ワイリー＆サンズ社"),
                bodyMsg(
                    "See <<Wiley & Sons, Inc.>>.",
                    "<<Wiley & Sons, Inc.>> を参照。"
                )
            )

            val result = processor.process(messages, context())

            assertThat(result[1].text).isEqualTo("<<wiley-sons-inc,ワイリー＆サンズ社>> を参照。")
        }
    }

    @Test
    fun `should skip non-asciidoctor context`() {
        runBlocking {
            val messages = listOf(
                titleMsg("WebAuthn endpoints", "WebAuthnエンドポイント"),
                bodyMsg("See <<WebAuthn endpoints>>.", "See <<WebAuthn endpoints>>.")
            )

            val result = processor.process(messages, context(isAsciidoctor = false))

            assertThat(result[1].text).isEqualTo("See <<WebAuthn endpoints>>.")
        }
    }

    @Test
    fun `should not modify when title is not translated`() {
        runBlocking {
            val messages = listOf(
                titleMsg("Logout", "Logout"),
                bodyMsg("Press <<Logout>>.", "<<Logout>> を押してください。")
            )

            val result = processor.process(messages, context())

            assertThat(result[1].text).isEqualTo("<<Logout>> を押してください。")
        }
    }
}
