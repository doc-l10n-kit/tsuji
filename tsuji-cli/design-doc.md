# tsuji 設計ドキュメント

## 概要
`tsuji` は、quarkus.ioのサイト翻訳を補助するために開発されたツールキットです。
gettext PO ファイルを介して、LangChain4j フレームワークを活用した AI (Google Gemini 等) と、既存の翻訳メモリ (TMX) を組み合わせた RAG (検索拡張生成) 翻訳を実現し、一貫性と品質の高い翻訳プロセスを提供します。

## アーキテクチャ
本アプリケーションは **Quarkus** フレームワーク上で **Kotlin** を使用して実装されており、クリーンな責務分離を目指した階層構造を採用しています。

### レイヤー構成
1. **App Service レイヤー (`net.sharplab.tsuji.app.service`)**
   - アプリケーションのユースケース（「翻訳を実行する」「統計を更新する」など）を制御します。
   - ワークフローの順序制御を担当します。
2. **Core Service レイヤー (`net.sharplab.tsuji.core.service`)**
   - 純粋なドメインロジックやビジネスルールを保持します。
   - 例: POファイルの統計計算、翻訳対象の判定、TMXモデルの構築など。
3. **Core Driver レイヤー (`net.sharplab.tsuji.core.driver`)**
   - 外部ライブラリ（jgettext, AsciidoctorJ）や外部API（LangChain4j）、外部プロセス（Git, po4a）とのインターフェースです。
4. **Core Model レイヤー (`net.sharplab.tsuji.core.model`)**
   - PO, TMX などのデータ構造を表現する純粋な Kotlin `data class` です。

## 主要コンポーネント

### PoService & PoDriver
- **PoService**: POファイル内のメッセージが翻訳対象かどうかの判定、原文ファイルのパス解決、翻訳進捗の統計計算（メッセージ数/単語数ベース）などのドメイン知識を持ちます。
- **PoDriver**: `jgettext` ライブラリを使用して物理的なファイルの読み書きを行い、ライブラリ固有のオブジェクトと `PoMessage` ドメインモデルを相互に変換します。

### TmxService & TmxDriver
- **TmxService**: POファイル群から TMX (Translation Memory eXchange) モデルを生成する際のフィルタリングや、重複排除のロジックを持ちます。
- **TmxDriver**: TMXファイルのパースと保存を担当します。

### Translator & RAG (Retrieval-Augmented Generation)
- **Translator**: 翻訳エンジンの抽象化インターフェースです。現在の実装は `GeminiTranslator` と `DeepLTranslator` の2つです。
- **GeminiTranslator**: LangChain4j を通じて Google Gemini API を呼び出します。RAG（検索拡張生成）に対応し、個別メッセージごとに翻訳を実行します。
- **DeepLTranslator**: DeepL API を使用し、バッチ処理による効率的な翻訳を行います。RAGには対応していません。
- **RAG**: 既存の翻訳資産 (TMX) をベクトルインデックス化し、翻訳時に類似した過去の翻訳ペアを検索してプロンプトに注入します。これにより、プロジェクト特有の用語やスタイルを AI が学習なしで反映できます。

両Translatorは共通のパイプラインアーキテクチャを採用しており、前処理・翻訳・後処理を11個の `MessageProcessor` で構成しています（詳細は次セクション参照）。

### メッセージ処理パイプライン
翻訳処理は、統一的な `MessageProcessor` インターフェースとパイプラインパターンで実装されています。

#### MessageProcessor インターフェース
各プロセッサは以下のインターフェースを実装します：
```kotlin
interface MessageProcessor {
    fun process(
        messages: List<TranslationMessage>,
        context: TranslationContext
    ): List<TranslationMessage>
}
```

- **責務**: メッセージリストを一括で変換します。個別処理（LLM呼び出し）とバッチ処理（DeepL API）を統一的に扱えます。
- **不変性**: 元のリストを変更せず、新しいリストを返します。

#### TranslationMessage ワーキングモデル
`PoMessage`（永続化ドメインモデル）とは分離された、パイプライン処理専用のワーキングモデルです：

- **original**: 元の `PoMessage`（参照用、不変）
- **text**: パイプライン処理中の作業テキスト（各プロセッサが読み書き）
- **fuzzy**: Fuzzyフラグ（プロセッサが更新可能）
- **needsTranslation**: 翻訳が必要かどうか（初期化時に判定）

主要メソッド：
- `withText(newText)`: テキストを更新した新しいインスタンスを返す
- `withFuzzy(fuzzy)`: Fuzzyフラグを更新した新しいインスタンスを返す
- `toPoMessage()`: 処理結果を `PoMessage` に戻す

#### TranslationContext
パイプライン全体で共有されるコンテキスト情報です：

- **po**: PO全体（メタデータ参照用）
- **srcLang / dstLang**: ソース言語 / ターゲット言語
- **isAsciidoctor**: Asciidoctorフォーマットかどうか
- **useRag**: RAGを使用するかどうか

#### パイプライン実行フロー
プロセッサは `fold` パターンで逐次適用されます：

```kotlin
val processedMessages = processors.fold(translationMessages) { msgs, processor ->
    processor.process(msgs, context)
}
```

**11個のプロセッサの順序と役割：**

1. **AsciidoctorPreProcessor**: AsciiDoc → HTML変換（Asciidoctorフォーマット時のみ）
2. **TranslationProcessor**: 翻訳実行（`GeminiTranslationProcessor` または `DeepLTranslationProcessor`）
3. **LinkTagMessageProcessor**: HTML `<a>` タグ → Asciidoctor `xref` / `link` 構文
4. **ImageTagMessageProcessor**: HTML `<img>` タグ → Asciidoctor `image` 構文
5-10. **DecorationTagMessageProcessor** (6種類):
   - `<em>` → `_text_`
   - `<strong>` → `*text*`
   - `<monospace>` → `` `text` ``
   - `<superscript>` → `^text^`
   - `<subscript>` → `~text~`
   - `<code>` → `` `text` ``
11. **CharacterReferenceUnescaper**: HTML文字参照（`&amp;` 等）をアンエスケープ

このパイプライン設計により、前処理・翻訳・後処理が明確に分離され、各プロセッサは単一責任を持ちます。

### MessageClassifier
メッセージ分類を行うユーティリティで、特別な処理が必要なメッセージを判定します。

**主要メソッド：**

- **shouldFillWithMessageId(message)**: 技術識別子やパスなど、翻訳せずに `messageId` をそのまま使用すべきメッセージを判定します（約30種類のパターンをサポート）。
- **isJekyllFrontMatter(text)**: Jekyll YAMLメタデータ（`layout:`, `title:`, `date:` 等）を含むメッセージを検出します。

これらの判定により、翻訳プロセッサは適切にメッセージをスキップまたは特別処理できます。

## データフロー

### 既存訳のインデックス作成
1. `index` コマンドにより TMX をロード。
2. `IndexingService` が翻訳ユニットを `TextSegment` に変換。
3. `VectorStoreDriver` がベクトル化して保存。

### PO ファイルの翻訳
1. `machine-translate` コマンドにより PO をロード。
2. `MessageTranslationService` が各メッセージの翻訳要否を判定。
3. **PoMessage → TranslationMessage 変換**: 各メッセージをパイプライン用のワーキングモデルに変換（`needsTranslation` フラグを設定）。
4. **パイプライン実行**: 11個の `MessageProcessor` を順次適用：
   - **AsciidoctorPreProcessor**: AsciiDoc → HTML変換（マークアップ保護）
   - **TranslationProcessor**: LLM/DeepL API による翻訳実行（RAG検索含む）
   - **各種ポストプロセッサ**: HTML → Asciidoctor構文復元、文字参照アンエスケープ
5. **TranslationMessage → PoMessage 復元**: 処理結果を永続化ドメインモデルに戻す。
6. 翻訳された PO を保存。

## 設計上の決定事項
- **1ファイル1クラス**: コードの可読性とナビゲーションを向上させるため、厳格に適用しています。
- **不変性の重視**: ドメインモデルや設定クラス (`TsujiConfig`) は、可能な限り不変（`val`）としています。`TranslationMessage` も不変データ構造を採用し、`withText()` などのメソッドで新しいインスタンスを返します。
- **DI (Contexts and Dependency Injection)**: Quarkus Arc を使用し、インターフェースによる疎結合を実現しています。Bean の生成ロジックは `TsujiBeans` クラスに集約されています。
- **共通コマンド基底クラス**: CLIコマンドは `BaseCommand` を継承し、例外ハンドリングや終了ステータス管理を統一しています。
- **パイプラインパターンの採用**: メッセージ処理を前処理・翻訳・後処理に明確に分離するため、`MessageProcessor` インターフェースとパイプラインパターンを採用しました。
  - **利点**: テスト容易性（各プロセッサを独立してテスト可能）、拡張性（新しいプロセッサを容易に追加）、保守性（各プロセッサが単一責任）
  - **実装**: Kotlin の `fold` 関数による関数型合成で、プロセッサを逐次適用します。
- **関数型プログラミングの思想**: パイプライン全体をステートレスに保つため、以下の原則を適用しています。
  - 各プロセッサは純粋関数（副作用なし）として実装
  - 状態変更は `copy()` による不変データ構造で実現
  - パイプライン実行は関数合成（`fold`）で表現
- **ワーキングモデル（TranslationMessage）の導入**: `PoMessage`（永続化ドメインモデル）とパイプライン処理中の作業状態を分離しました。
  - **理由**: パイプライン処理中の中間状態（`text`, `fuzzy`, `needsTranslation`）を明確に管理し、ドメインモデルの責務を純粋に保つため
  - **メモリ効率**: 元の `PoMessage` を保持せず、必要な情報のみを `TranslationMessage` に保持することで、大量メッセージ処理時のメモリ使用量を最適化
