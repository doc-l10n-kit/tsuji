package net.sharplab.tsuji.core.driver.translator.deepl

import org.junit.jupiter.api.Disabled

/**
 * このテストは新しいパイプライン実装に対応するため、以下に移行されました：
 *
 * 1. DeepLTranslationProcessorTest.kt
 *    - メッセージ処理ロジックのテスト（空入力、スキップ、分類等）
 *    - Jekyll Front Matter の翻訳
 *    - バッチ処理のロジック
 *
 * 2. DeepLTranslatorIntegrationTest.kt
 *    - APIキーのバリデーション
 *    - Po全体の翻訳フロー
 *    - 設定の統合テスト
 *
 * 3. PostProcessorsTest.kt (AsciidoctorPreProcessorTest.kt も)
 *    - Asciidoctor 前処理・後処理のテスト
 *
 * 旧実装でテストしていた内容：
 * ✅ 空の入力 → 空のリストを返す → DeepLTranslatorIntegrationTest
 * ✅ APIキーなし → 例外をスロー → DeepLTranslatorIntegrationTest
 * ✅ APIキーが空白 → 例外をスロー → DeepLTranslatorIntegrationTest
 * ✅ APIキーが空文字列 → 例外をスロー → DeepLTranslatorIntegrationTest
 * ✅ 空文字列の入力 → 空文字列を返す → DeepLTranslationProcessorTest
 */
@Disabled("新しいパイプライン実装に移行済み。上記のテストクラスを参照")
class DeepLTranslatorTest
