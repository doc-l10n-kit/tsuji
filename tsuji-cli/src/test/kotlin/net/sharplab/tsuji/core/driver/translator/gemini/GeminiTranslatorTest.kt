package net.sharplab.tsuji.core.driver.translator.gemini

import org.junit.jupiter.api.Disabled

/**
 * このテストは新しいパイプライン実装に対応するため、以下に移行されました：
 *
 * 1. GeminiTranslationProcessorTest.kt
 *    - 波括弧のエスケープ（LangChain4j対応）
 *    - メッセージ処理ロジック（空入力、スキップ、分類等）
 *    - Jekyll Front Matter の翻訳
 *    - RAG使用時の動作
 *
 * 2. PostProcessorsTest.kt (AsciidoctorPreProcessorTest.kt も)
 *    - Asciidoctor 前処理・後処理のテスト
 *
 * 旧実装でテストしていた内容：
 * ✅ 波括弧のエスケープ → GeminiTranslationProcessorTest
 * ✅ RAG使用時の動作 → GeminiTranslationProcessorTest
 * ✅ メッセージのスキップ処理 → GeminiTranslationProcessorTest
 * ✅ 複数メッセージの処理 → GeminiTranslationProcessorTest
 */
@Disabled("新しいパイプライン実装に移行済み。上記のテストクラスを参照")
class GeminiTranslatorTest
