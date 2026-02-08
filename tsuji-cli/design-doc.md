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
- **Translator**: 翻訳エンジンの抽象化です。`GeminiTranslator` は LangChain4j を通じて LLM API を呼び出します。
- **RAG**: 既存の翻訳資産 (TMX) をインデックス化し、翻訳時に類似した過去の翻訳ペアを検索してプロンプトに注入します。これにより、プロジェクト特有の用語やスタイルを AI が学習なしで反映できます。

### AsciidoctorMessageProcessor
- AIが AsciiDoc 特有の構文（リンク、画像、インラインマークアップ）を破壊しないよう、翻訳前に一時的なタグに置換し、翻訳後に安全に復元するプリプロセス・ポストプロセスを行います。

## データフロー

### 既存訳のインデックス作成
1. `index` コマンドにより TMX をロード。
2. `IndexingService` が翻訳ユニットを `TextSegment` に変換。
3. `VectorStoreDriver` がベクトル化して保存。

### PO ファイルの翻訳
1. `machine-translate` コマンドにより PO をロード。
2. `MessageTranslationService` が各メッセージの形式（BlogHeader か通常文か等）や翻訳要否を判定。
3. 各メッセージに対し、RAG によるコンテキスト検索と LLM による翻訳を実行。
4. `AsciidoctorMessageProcessor` がマークアップを保護。
5. 翻訳された PO を保存。

## 設計上の決定事項
- **1ファイル1クラス**: コードの可読性とナビゲーションを向上させるため、厳格に適用しています。
- **不変性の重視**: ドメインモデルや設定クラス (`TsujiConfig`) は、可能な限り不変（`val`）としています。
- **DI (Contexts and Dependency Injection)**: Quarkus Arc を使用し、インターフェースによる疎結合を実現しています。Bean の生成ロジックは `TsujiBeans` クラスに集約されています。
- **共通コマンド基底クラス**: CLIコマンドは `BaseCommand` を継承し、例外ハンドリングや終了ステータス管理を統一しています。
