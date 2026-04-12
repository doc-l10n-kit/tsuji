# tsuji 設計ドキュメント

## 1. プロジェクト概要

tsuji は、[quarkus.io](https://quarkus.io) ドキュメントサイトの翻訳のために設計されたローカリゼーションツールキットです。gettext PO ファイルを介して、LLM（Google Gemini）または機械翻訳 API（DeepL）による翻訳と、既存の翻訳メモリ（TMX）を活用した RAG（検索拡張生成）を組み合わせ、一貫性の高い翻訳プロセスを提供します。

**技術スタック**: Kotlin / Quarkus / LangChain4j / PicocLI / JDK 21

## 2. システム構成

### マルチモジュール構成

```
tsuji (root)         … CLI アプリケーション本体
├── tsuji-po         … PO ファイルのドメインモデルと I/O（jgettext）
└── tsuji-tmx        … TMX ファイルのドメインモデルと I/O（Jackson XML）
```

### レイヤー構成（ルートモジュール）

ルートモジュールは 3 層で構成されています。各層は上位から下位への一方向の依存を持ちます。

```
CLI (PicocLI)
  ↓
App Service レイヤー (net.sharplab.tsuji.app.service)
  ユースケースの制御とワークフローの順序管理を担当します。
  例: TranslationAppService, PoAppService, TmxAppService, JekyllAppService
  ↓
Core Service レイヤー (net.sharplab.tsuji.core.service)
  純粋なドメインロジックを保持します。外部依存はありません。
  例: PoService（統計計算、翻訳対象判定）, TmxService（TMX 生成ロジック）
  ↓
Core Driver レイヤー (net.sharplab.tsuji.core.driver)
  外部システムとのインテグレーションを担当します。
  例: Translator, VectorStoreDriver, PoDriver, TmxDriver, Po4aDriver
```

DI は Quarkus Arc を使用し、Bean の生成ロジックは `TsujiBeans` クラスに集約しています。

## 3. 翻訳エンジン

翻訳エンジンは tsuji の中核です。本セクションでその設計を詳述します。

### 3.1 Translator インターフェース

```kotlin
interface Translator {
    suspend fun translate(
        po: Po, srcLang: String, dstLang: String,
        isAsciidoctor: Boolean, useRag: Boolean
    ): Po
}
```

入力は PO ファイル全体（`Po`）で、翻訳済みの `Po` を返します。実装は以下の 2 つです：

| 実装 | 翻訳エンジン | RAG 対応 | マークアップ保護戦略 |
|---|---|---|---|
| `GeminiTranslator` | Google Gemini (LangChain4j) | 対応 | AsciiDoc ネイティブ + 事後バリデーション |
| `DeepLTranslator` | DeepL API | 非対応 | AsciiDoc → HTML ラウンドトリップ |

使用する実装は設定値 `tsuji.translator.type` で切り替わります（`TsujiBeans.translator()` で分岐）。

### 3.2 パイプラインアーキテクチャ

両 Translator は共通の `MessageProcessor` インターフェースによるパイプラインで翻訳を行いますが、**パイプラインの構成は大きく異なります**。

#### MessageProcessor インターフェース

```kotlin
interface MessageProcessor {
    suspend fun process(
        messages: List<TranslationMessage>,
        context: TranslationContext
    ): List<TranslationMessage>
}
```

各プロセッサはメッセージリスト全体を受け取り、処理済みの新しいリストを返します。元のリストは変更しません。

#### GeminiTranslator のパイプライン（2 プロセッサ）

Gemini は AsciiDoc マークアップを直接理解できるため、HTML 変換は不要です。

```
PoMessage → TranslationMessage 変換
  ↓
1. GeminiTranslationProcessor   … バッチ翻訳 + メッセージ分類
2. XrefTitlePostProcessor       … <<Title>> → <<section-id,翻訳タイトル>> 変換
  ↓
TranslationMessage → PoMessage 復元
```

翻訳結果の AsciiDoc マークアップ整合性は `AsciidocMarkupValidator` で事後検証し、破損があればリトライします（後述）。

#### DeepLTranslator のパイプライン（11 プロセッサ）

DeepL はプレーンテキスト API のため、マークアップを保護するための前処理・後処理が必要です。

```
PoMessage → TranslationMessage 変換
  ↓
1.  AsciidoctorPreProcessor       … AsciiDoc → HTML 変換
2.  DeepLTranslationProcessor     … DeepL API による翻訳
3.  LinkTagMessageProcessor       … <a href="..."> → xref/link 構文
4.  ImageTagMessageProcessor      … <img src="..."> → image:path[] 構文
5.  DecorationTagMessageProcessor … <em> → _text_
6.  DecorationTagMessageProcessor … <strong> → *text*
7.  DecorationTagMessageProcessor … <monospace> → `text`
8.  DecorationTagMessageProcessor … <superscript> → ^text^
9.  DecorationTagMessageProcessor … <subscript> → ~text~
10. DecorationTagMessageProcessor … <code> → `text`
11. CharacterReferenceUnescaper   … HTML 文字参照のアンエスケープ
  ↓
TranslationMessage → PoMessage 復元
```

この違いが生じる理由は、Gemini がプロンプトで AsciiDoc のマークアップ保持を指示できるのに対し、DeepL は単純なテキスト翻訳 API であるため、マークアップを一旦 HTML タグに変換して保護し、翻訳後に AsciiDoc に復元する必要があるからです。

### 3.3 TranslationMessage ワーキングモデル

パイプライン処理中は `PoMessage`（永続化ドメインモデル）ではなく、専用のワーキングモデル `TranslationMessage` を使用します。

```kotlin
data class TranslationMessage(
    val original: PoMessage,        // 元の PoMessage（参照用、不変）
    val text: String,               // パイプライン処理中の作業テキスト
    val fuzzy: Boolean,             // Fuzzy フラグ
    val needsTranslation: Boolean,  // 翻訳が必要かどうか（初期化時に判定）
    val additionalComments: List<String>
)
```

- `from(PoMessage)`: messageString が空のメッセージを翻訳対象として初期化します。作業テキストには `messageId`（原文）を設定します。
- `withText()` / `withFuzzy()`: イミュータブルな更新です。新しいインスタンスを返します。
- `toPoMessage()`: 処理結果を `PoMessage` に戻します。

### 3.4 メッセージ分類

`GeminiTranslationProcessor` は翻訳実行前にメッセージを 3 種類に分類します：

| 分類 | 判定 | 処理 |
|---|---|---|
| **fill** | `MessageClassifier.shouldFillWithMessageId()` | messageId をそのまま使用します（技術識別子、パス等。約 30 パターン） |
| **Jekyll Front Matter** | `MessageClassifier.isJekyllFrontMatter()` | title / synopsis フィールドのみ個別翻訳し、fuzzy フラグを付与します |
| **normal** | 上記以外 | バッチ翻訳します（後述） |

### 3.5 バッチ翻訳と構造化出力

normal メッセージは LLM にバッチで送信されます。

#### リクエスト形式

複数のテキストを JSON 配列としてまとめて送信します：

```json
[
  {"index": 0, "text": "First text to translate"},
  {"index": 1, "text": "Second text"}
]
```

#### レスポンス形式（JSON Schema による制約）

LangChain4j の `ResponseFormat` で JSON Schema を指定し、LLM の出力を構造化します：

```json
[
  {"index": 0, "translation": "翻訳されたテキスト"},
  {"index": 1, "translation": "2番目のテキスト"}
]
```

`GeminiTranslationAiService` がレスポンスをパースし、index の一致を検証します。index が不一致の場合は例外を投げます。

#### RAG 対応バッチ翻訳

RAG 有効時は `GeminiRAGTranslationAiService` を使用します。各テキストに対してベクトル検索で類似翻訳を取得し、`tm` フィールドとしてリクエストに含めます：

```json
[
  {
    "index": 0,
    "text": "First text to translate",
    "tm": [
      {"original": "similar source", "translation": "類似翻訳"}
    ]
  }
]
```

システムプロンプトでは、`tm` はあくまで用語の参照であり、文構造のコピーは禁止する旨を指示しています。

#### 用語集（Glossary）

設定で用語集を定義でき、有効時はシステムプロンプトに用語対応表を注入します。

### 3.6 適応的並行制御（二層 AIMD）

大量の PO ファイルを効率的に翻訳するため、**並行度**と**バッチサイズ**の 2 つの軸で適応制御を行います。

#### 第 1 層: 並行度制御 — AdaptiveParallelismController

API レート制限に適応してリクエスト並行度を動的に調整します。

**核心的アイデア: Semaphore の予約パーミット方式**

Semaphore を最大並行度（例: 60）で初期化し、使用しないパーミットを「予約」として保持することで、Semaphore の再生成なしに実効並行度を動的に変更します。

```
最大 60 パーミット、初期並行度 40 の場合:
  → 起動時に 20 パーミットを予約（acquire）
  → 並行度を下げる: パーミットを 1 つ追加予約（keep）
  → 並行度を上げる: 予約パーミットを 1 つ解放（release）
```

**AIMD アルゴリズム**:
- **Additive Increase**: 連続 10 回成功で並行度 +1（予約パーミットを 1 つ解放）
- **Multiplicative Decrease**: レート制限エラー（429）で並行度 -1（パーミットを予約として保持）

レート制限エラー時はパーミットを解放せずそのまま保持するため、自然に実効並行度が下がります。

#### 第 2 層: バッチサイズ制御 — GeminiBatchProvider

1 リクエストあたりのテキスト数を適応的に調整します。

**AIMD アルゴリズム**:
- **Additive Increase**: バッチ成功ごとにサイズ上限 +1
- **Multiplicative Decrease**: バリデーションエラー時にサイズ上限 ×0.5

初期サイズ・最大サイズはともに設定可能です（デフォルト: 200）。

#### BatchedExecutor

`BatchedExecutor` が `BatchProvider` と連携し、以下を制御します：

- `peekNext()` で次のバッチを取得します（位置は進めません）
- 成功時: `consumeNext()` で位置を進め、`notifySuccess()` でサイズを増加させます
- バリデーションエラー時: `notifyValidationError()` でサイズを縮小し、同じ位置からリトライします
- 一般エラー時: 同じバッチをリトライします（指数バックオフ）

二層の制御が連携して動作する全体像は以下の通りです：

```
TranslationAppServiceImpl
  flatMapMerge(concurrency=30) で PO ファイルを並列処理
    ↓
GeminiTranslationProcessor
  BatchedExecutor がバッチ分割とリトライを管理
    ↓
  AdaptiveParallelismController.execute { バッチ翻訳 }
    Semaphore でグローバル並行度を制御
```

### 3.7 マークアップ保護

#### Gemini: 事後バリデーション方式

Gemini にはプロンプトで AsciiDoc マークアップの保持を指示しますが、LLM の出力は確定的でないため、`AsciidocMarkupValidator` で翻訳後に検証します。

**検証手順**:
1. 原文と翻訳文をそれぞれ AsciidoctorJ で HTML に変換します
2. jsoup で DOM を解析し、以下のマークアップ特徴量を抽出します：
   - リンクの `href` 集合
   - 画像の `src` 集合
   - `<em>`, `<strong>`, `<code>` の出現回数
3. 原文と翻訳文の特徴量を比較し、不一致があれば `AsciidocMarkupValidationException` を投げます

**リトライ**: マークアップ破損が検出された場合、破損メッセージのみを再翻訳します（最大 2 回）。それでも修復できない場合はコメントを付与して続行します。

#### DeepL: HTML ラウンドトリップ方式

1. `AsciidoctorPreProcessor`: AsciiDoc → HTML に変換します（AsciidoctorJ + カスタムテンプレート）
2. `DeepLTranslationProcessor`: HTML のまま DeepL API で翻訳します（タグ構造は保持されます）
3. 各種ポストプロセッサ: HTML タグ → AsciiDoc 構文に復元します

### 3.8 ファイルレベル並列処理

`TranslationAppServiceImpl` は Kotlin Flow の `flatMapMerge(concurrency = 30)` で PO ファイルを並列に翻訳します。各ファイル内のバッチ翻訳は `AdaptiveParallelismController` の Semaphore でグローバルに並行度制限されるため、ファイルレベルの並列度と API リクエストレベルの並行度は独立して制御されます。

## 4. RAG（検索拡張生成）

既存の翻訳資産（TMX）をベクトルインデックス化し、翻訳時に類似した過去の翻訳ペアを検索してプロンプトに注入します。

### インデックス構築

```
TMX ファイル
  ↓ TmxDriver.load()
Tmx モデル
  ↓ IndexingService.convertToSegments()
List<TextSegment>   … 原文をテキスト、訳文をメタデータ("target")として保持
  ↓ VectorStoreDriver.addAll() or updateIndexWithDiff()
Lucene ベクトルインデックス (l10n/rag/index/)
```

- **エンベディングモデル**: AllMiniLmL6V2QuantizedEmbeddingModel（ONNX、ローカル実行）
- **ストレージ**: Apache Lucene（`LuceneVectorStoreDriver`）
- **重複排除**: セグメントの内容（原文 + 訳文 + 言語）から SHA-256 ハッシュを生成し、コンテンツベースの ID として使用します。同一内容のセグメントは同じ ID になるため、`updateDocument()` による upsert で重複を防ぎます。
- **差分更新**: `updateIndexWithDiff()` で既存インデックスの ID 集合と新しいセグメントの ID 集合を比較し、追加・削除のみを実行します。

### 翻訳時の検索

`GeminiRAGTranslationAiService` がテキストごとに `EmbeddingStoreContentRetriever` で類似翻訳を検索します。
- **最大結果数**: 設定可能です（デフォルト: 3）
- **最小スコア**: 設定可能です（デフォルト: 0.5）

## 5. PO/TMX ファイル管理

### tsuji-po モジュール

PO ファイルのドメインモデルと I/O を提供します。

- **Po**: PO ファイル全体を表すイミュータブルデータクラスです（target, messages, header）
- **PoMessage**: メッセージ単位のモデルです（messageId, messageString, flags, comments, sourceReferences）
- **PoCodec**: jgettext ライブラリによる PO ファイルの読み書きを行います
- **MessageClassifier**: 技術識別子やパスなど、翻訳不要なメッセージを判定するユーティリティです（約 30 パターン）

### tsuji-tmx モジュール

TMX ファイルのドメインモデルと I/O を提供します。

- **Tmx**: TMX 1.4 ドキュメントモデルです（header, body）
- **TmxCodec**: Jackson XML による TMX ファイルのパースと保存を行います
- **TranslationIndex**: HashMap ベースの高速翻訳検索です。TMX の翻訳ペアを原文 → 訳文のマップとして保持し、TMX の PO への適用時に使用します。

### PO 操作

| コマンド | 機能 |
|---|---|
| `po normalize` | msgcat による PO 構文の正規化 |
| `po update` | po4a による原文からの PO 更新 |
| `po apply` | po4a による翻訳済みドキュメントの生成 |
| `po apply-tmx` | TMX の翻訳を PO に適用（確定訳） |
| `po apply-fuzzy-tmx` | TMX の翻訳を PO に適用（fuzzy 付き） |
| `po machine-translate` | LLM / DeepL による機械翻訳 |
| `po remove-obsolete` | 廃止メッセージ（#~）の除去 |
| `po purge-fuzzy` | fuzzy メッセージの訳文クリア |
| `po purge-all` | 全メッセージの訳文クリア |
| `po update-po-stats` | 翻訳進捗の統計出力（メッセージ数・単語数ベース） |

### TMX 操作

| コマンド | 機能 |
|---|---|
| `tmx generate` | PO ファイル群から TMX を生成（CONFIRMED / FUZZY モード） |

## 6. Jekyll 連携

Jekyll ベースのドキュメントサイト（quarkus.io 等）との統合機能を提供します。

| コマンド | 機能 |
|---|---|
| `jekyll extract` | Jekyll ソースから PO ファイルを抽出（po4a 経由） |
| `jekyll build` | 翻訳済み Jekyll サイトをビルド |
| `jekyll serve` | 翻訳済みサイトのローカルプレビュー |
| `jekyll update-stats` | Jekyll サイト全体の翻訳統計を更新 |

ビルド時は `upstream/` ディレクトリの原文に対して `l10n/override/` のオーバーライドを適用した後、PO ファイルの翻訳を反映します。

## 7. 設計原則

### パイプラインパターンと関数型合成

メッセージ処理を前処理・翻訳・後処理に明確に分離するため、`MessageProcessor` インターフェースとパイプラインパターンを採用しています。各プロセッサは純粋関数として実装し、パイプラインは `fold` による関数合成で表現します。

- **テスト容易性**: 各プロセッサを独立してテストできます
- **拡張性**: 新しいプロセッサを容易に追加できます
- **保守性**: 各プロセッサが単一責任を持ちます

### 不変データ構造とワーキングモデル分離

ドメインモデル（`Po`, `PoMessage`）は不変（`val`）を基本とし、変更は `copy()` で新しいインスタンスを返します。パイプライン処理中は `TranslationMessage` ワーキングモデルを使用し、ドメインモデルの責務を純粋に保ちます。

### インターフェースベースの疎結合

Quarkus Arc による DI で、インターフェースと実装を分離しています。`Translator`, `VectorStoreDriver`, `PoDriver` 等はすべてインターフェースで定義し、`TsujiBeans` で実装を注入します。

### 1 ファイル 1 クラス

コードの可読性とナビゲーションのため、1 ファイルに 1 クラスの原則を適用しています。
