# セッション草稿：Quarkus AIを触ってみた：Quarkusドキュメント翻訳での活用

**想定聴衆：** エンジニアリングチーム内（ローカライゼーションチームではない）

---

最近、空いた時間で新しい技術に触れてみようと思い、今回は **Quarkus AI** を試してみました。

## Quarkus AIとは

**Quarkus AI** とは、ChatGPT、Gemini、OSSのローカルLLMなど、各種LLMのAPIをQuarkusアプリケーションから統一的に扱えるようにするライブラリです。

このライブラリは **LangChain4j** がベースとなっており、Quarkusらしい流儀に沿ってLLMを扱えるようになる便利なツールです。このツールを利用することで、各LLMのAPIの差異に煩わされずに迅速なアプリケーション開発が可能になります。

例えば、渡されたテキストを英日翻訳するクライアントクラスを必要とする場合、Quarkus AIライブラリへの依存関係を定義した上で、以下のようにアノテーションを付けたインタフェースを用意することで、 **実装クラスがコンパイル時に自動生成** され、呼び出し側に対してDI（依存性注入）されるようになります。

```kotlin
@RegisterAiService
interface GeminiTranslationService {

    @SystemMessage(
        "You are a professional translator for open-source project documentation.",
        "Translate the given text from {{srcLang}} to {{dstLang}}."
    )
    fun translate(@UserMessage text: String, srcLang: String, dstLang: String): String
}
```

### 呼び出し側の実装イメージ（CDIによるDI）

利用する側では、通常のBeanと同じように `@Inject` するだけで、自動生成された実装クラスが注入されます。

```kotlin
@ApplicationScoped
class TranslationTask(@Inject private val translationService: GeminiTranslationService) {

    fun execute() {
        val originalText = "Quarkus is a cloud-native Java framework."
        // 翻訳メソッドを呼び出すだけでLLMとの通信が行われる
        val translatedText = translationService.translate(originalText, "en", "ja")
        
        println(translatedText) // 「QuarkusはクラウドネイティブなJavaフレームワークです。」
    }
}
```

---

さらに、このライブラリはLLM向けに高度な機能を提供しており、例えば以下のようなことが可能です：

- 複数のLLMをシームレスに切替
- RAG（Retrieval-Augmented Generation）構築
- GuardRailによる入出力保護
- カスタム命令による柔軟な翻訳や生成ルールの指定

### 複数のLLMをシームレスに切替

Quarkus AIの大きな強みは、LLMとの通信やモデルへのリクエストが完全に抽象化されている点です。アプリケーションコードは特定のベンダーのAPI（OpenAIやGoogle Gemini、Claudeなど）に直接依存しません。

これにより、例えば「初期開発はGemini APIでスピーディに検証し、本番移行後は社外へのキャッシュアウト（支出）削減やセキュリティの観点からIBMのモデルやローカルLLM（Ollamaなど）に切り替える」といった柔軟な運用が可能になります。

#### 設定と依存関係だけでLLMを切り替える例

アプリケーション側の実装（先ほどのインターフェースなど）は一切変更せず、プロジェクトの依存関係（`pom.xml` や `build.gradle`）と `application.properties` （Quarkusの設定ファイル）を変更するだけで、接続先のLLMを切り替えることができます。

Quarkusでは **BOM (Bill of Materials)** を利用することで、個別の拡張機能のバージョン管理を簡素化できます。

**Google Geminiを利用する場合（クラウド）:**

1. 依存関係の追加（Gradleの例）:
   ```kotlin
   // build.gradle.kts
   dependencies {
       implementation("io.quarkiverse.langchain4j:quarkus-langchain4j-ai-gemini")
   }
   ```

2. プロパティの設定:
   ```properties
   # application.properties
   quarkus.langchain4j.ai.gemini.api-key=AIza...
   quarkus.langchain4j.ai.gemini.chat-model.model-id=gemini-2.5-flash
   ```

**Ollamaを利用する場合（ローカルLLM）:**

1. 依存関係の切り替え:
   ```kotlin
   // build.gradle.kts
   dependencies {
       // Geminiを外してOllamaを入れる
       implementation("io.quarkiverse.langchain4j:quarkus-langchain4j-ollama")
   }
   ```

2. プロパティの設定:
   ```properties
   # application.properties
   quarkus.langchain4j.ollama.chat-model.model-id=llama3
   quarkus.langchain4j.ollama.base-url=http://localhost:11434/api
   ```

このように、LLMをベンダー固有のAPIに縛られることなく組み込めるため、技術の進化スピードが速いAI領域においても、柔軟なシステムを構築できます。

### RAG（Retrieval-Augmented Generation）構築

#### RAGとは

RAG（検索拡張生成）は、LLMが元々持っている知識だけでなく、**外部のデータベースや文書（社内のドキュメントやナレッジベース）を検索し、その結果を「コンテキスト（文脈・前提知識）」としてプロンプトに含めてLLMに回答させる技術**です。

この「検索とコンテキストの組み立て」を実現する上で中核となるのが、**ベクトル化（Embedding）**、**ベクトル化モデル（Embedding Model）**、そして**ベクトルデータベース**という技術です。

- **ベクトル化（Embedding）とは:** テキストを「意味」や「文脈」を捉えた多次元の数値の配列（ベクトル）に変換する技術のことです。
- **ベクトル化モデル（Embedding Model）とは:** ベクトル化を実行するための特化型AIモデルです。文章を生成する**LLMとは別のモデル**であり、軽量かつ高速に動作します。
- **ベクトルデータベースとは:** ベクトル化されたデータを保存し、高速な「類似度検索」に特化したデータベースです。数値の配列同士の数学的な距離を計算することで、意味的に関連性の高い情報を瞬時に見つけ出します。

**RAGにおける処理のフロー（コンテキストの組み立て方）:**

これらを組み合わせることで、以下のような流れでLLMへの入力（プロンプト）が動的に組み立てられます。

1. **事前準備（ナレッジの保存）:** あらかじめ社内文書や過去の翻訳データを「ベクトル化モデル」に通して数値化し、「ベクトルデータベース」に保存しておきます。
2. **質問のベクトル化:** ユーザーから新しい質問（または翻訳対象のテキスト）が来ると、アプリケーションはそれ自体をLLMへ送る前に、まず**同じベクトル化モデル**を使って質問文をベクトル（数値）に変換します。
3. **類似度検索:** そのベクトルを「ベクトルデータベース」に渡し、意味的に近い（距離が近い）過去のドキュメントや参考情報を検索・抽出します。
4. **プロンプトの組み立て（Augment）:** 抽出された関連テキストを、「以下の情報を参考にして回答してください：[抽出したテキスト]」という形で、ユーザーの元の質問文の裏側（システムプロンプト等）に結合します。
5. **LLMによる生成:** この「検索（Retrieval）によって拡張（Augmented）されたプロンプト」をLLMに送信することで、LLMは与えられた知識を前提とした精度の高い回答を生成（Generation）します。

RAGを活用することで、以下の課題を解決できます。

1. **ハルシネーション（もっともらしい嘘）の抑制:** 事実に基づく情報をコンテキストとして与え、正確な生成を促します。
2. **最新・非公開情報の活用:** LLMが事前学習していない社内知識を回答に反映できます。
3. **コスト削減・パフォーマンス向上:** ファインチューニングなしで、特定のドメインに特化した応答を実現できます。

#### Quarkus AIにおけるRAGのサポート

通常、RAGを実装するには「ソースの読み込み・分割」「ベクトル化」「保存」「類似度検索」といった複雑なパイプラインが必要ですが、Quarkus AI（LangChain4jベース）はこのプロセスを強力にサポートします。

- **データの取り込み（Ingestion）:** ドキュメントパーサーやローダーにより、多様なファイルを簡単に読み込み、適切なサイズに分割（チャンキング）できます。
- **多様なベクトルストアへの対応:** Redis、Chroma、Elasticsearch、PgVectorなど、主要なデータベースとの連携機能が標準で用意されています。
- **アノテーションによる統合:** 設定アノテーションを活用することで、検索処理とLLMへのコンテキスト付与を自動的に連動させ、少ないコード量で本格的なRAGシステムを組み込めます。

### GuardRailによる入出力保護

#### 入出力保護とは

LLMの出力は確率的であり、予期せぬ結果や悪意のある入力を受けるリスクがあります。これらを制御・防御するための仕組みが、GuardRail（ガードレール）です。

ガードレールは、LLMが生成したデータが「プログラムが期待する形式や制約（スキーマ）」に合致しているかを検証する形式的なチェック（JSONフォーマットや数値の範囲など）や、入出力の「意味的な安全性やビジネスルール」の検証（個人情報保護、不適切な発言のブロック、ハルシネーション検知など）を担います。

#### Quarkus AIにおける入出力保護のサポート

Quarkus AIは、エンタープライズ品質のAIアプリケーションを構築するために、これらの保護メカニズムをシームレスに組み込めるように設計されています。

QuarkusのDI（CDI）を活用し、独自のガードレールロジックを簡単に組み込むことができます。インターフェースに `@InputGuardrails` や `@OutputGuardrails` といったアノテーションを付与するだけで、リクエストの送信前やレスポンスの受信後に、自作の検証処理を透過的に実行させることができます。チェックに引っ掛かった場合、Quarkus AIは自動的にLLMに対して「バリデーションエラーが発生したので修正してください」とフィードバックを行い、**自動リトライ（自己修正）** を促すことができます。

**具体的な実装例（Positive/Negativeの判定）:**

テキストを分析して「Positive」か「Negative」のいずれかのみを答えさせるAIサービスに対し、出力が期待する値から逸脱していないかをチェックするガードレールは以下のように実装できます。

```kotlin
// 1. ガードレールの実装（CDI Beanとして定義）
@ApplicationScoped
class SentimentGuardrail : OutputGuardrail {
    override fun validate(context: OutputGuardrailContext): GuardrailResult {
        val text = context.response().text()
        // 出力が「Positive」か「Negative」の完全一致かチェック
        if (text == "Positive" || text == "Negative") {
            return GuardrailResult.success()
        }
        // 逸脱している場合は失敗とし、LLMへ自己修正を促すフィードバックを返す
        return GuardrailResult.failure("Output must be either 'Positive' or 'Negative'. Do not include any other text.")
    }
}

// 2. AIサービスへのガードレールの適用
@RegisterAiService
interface SentimentAnalyzer {
    @SystemMessage("Analyze the sentiment of the provided text and respond with only one word: 'Positive' or 'Negative'.")
    @OutputGuardrails(SentimentGuardrail::class)
    fun analyze(@UserMessage text: String): String
}
```

このように、AIの不確実性をコントロールし、安全で堅牢なアプリケーションを少ないコードで実現できます。

---

## 既存のQuarkusドキュメント翻訳システムにおける課題

ここからは、**Quarkusドキュメント翻訳** を題材に、このQuarkus AIがどのように活用できるかを解説していきます。

**Quarkusドキュメント翻訳** では従来は、DeepL APIを使用してドキュメントの下訳を自動化していました。

### DeepL APIの特徴

DeepL APIは非常に自然で読みやすい日本語の訳文を生成できるのが特徴です。「XMLマークアップテキスト対応」や「用語集対応」などの機能を備えており、数年前の構築当時は技術ドキュメント翻訳において非常にモダンで扱いやすいAPIでした。

### 主な課題

一方で、以下のような課題が残っていたのも事実でした。

1. **翻訳ルールのカスタマイズが効かない**
2. 括弧や句読点の扱いなど、手作業での修正が必要な癖がある
3. **既存の翻訳を考慮できない**（ドキュメント間での用語法やスタイルの統一が困難）
4. **Asciidocなど、軽量マークアップのハンドリングが困難**
5. **用語集の適用が漏れる場合がある**
6. コストが高い

---

## LLMベースの翻訳への切替により期待される改善

LLMおよびQuarkus AIを適用することで、以下のメリットが期待されます。

1. **翻訳ルールのカスタマイズ:** 翻訳のスタイルをプロンプトで詳細に指示可能。
2. **既存訳の考慮:** 既存の人間がレビュー済の翻訳をRAGを活用してコンテキストとして組み込み、翻訳品質を向上。
3. **軽量マークアップのハンドリング:** マークアップを維持した翻訳が可能（モデル次第）。タグ破損時はガードレール機能で自動リトライ。
4. **用語集対応:** プロンプトに埋め込むことで実装可能
5. **コスト:** DeepL比較で全体的に安価なモデルが多い。

注：DeepL APIも最近カスタムインストラクションをサポートしましたが、
汎用的なLLMのAPIではないためQuarkus AIから扱えず、RAGやガードレール機能を活用できない点や
コスト面から、LLMの方が優位です。

---

## Quarkus AIを活用した機械翻訳の実装テクニック

### 翻訳ルールのカスタマイズ（プロンプトエンジニアリングと前処理）

`@SystemMessage` を用いて、厳格なルールを自然言語で指示します。以下のような外部プロンプトファイルを活用しています。

```text
You are a professional translator for open-source project documentation.
Translate the given text from {{srcLang}} to {{dstLang}}.

Rules:
- Maintain the technical context of the documentation.
- Preserve all HTML/Asciidoc tags exactly as they are.
- Do not translate the content inside <code> tags.
```

### RAGを活用した既存訳の考慮（Translation Memoryの活用）

過去の翻訳資産（原文：訳文の対訳）を「参考訳」として動的に注入します。

1. **ベクトル化:** ローカルで高速に動作する `AllMiniLmL6V2QuantizedEmbeddingModel` を採用。
2. **ベクトルストア:** メモリ上で類似度検索を行い、JSONファイルに永続化する `InMemoryVectorStoreDriver` を使用
3. **統合:** `@RegisterAiService(retrievalAugmentor = ...)` により、原文に類似した過去の翻訳ペアを自動検索し、プロンプトに注入します。

**実装例:**

まず、検索用ベクトルストア（`InMemoryVectorStoreDriver`）から `RetrievalAugmentor` を提供するSupplierを用意します。ここで使用するベクトルストアは、テストデータと検索処理が分離しないよう `@ApplicationScoped` で単一のインスタンスを共有することが重要です。

```kotlin
@ApplicationScoped
class TsujiRetrievalAugmentorSupplier : Supplier<RetrievalAugmentor> {

    @Inject
    lateinit var vectorStoreDriver: VectorStoreDriver

    override fun get(): RetrievalAugmentor {
        // ContentRetriever には検索しきい値(minScore)等を設定して精度を調整できます
        return DefaultRetrievalAugmentor.builder()
            .contentRetriever(vectorStoreDriver.asContentRetriever())
            .build()
    }
}
```

その後、`@RegisterAiService` のアノテーションに上記のSupplierを指定するだけで、LangChain4j が自動的にRAG（Retrieval-Augmented Generation）をパイプラインに組み込み、LLMのプロンプト末尾に類似する過去の翻訳結果を追加してくれます。

```kotlin
@RegisterAiService(retrievalAugmentor = TsujiRetrievalAugmentorSupplier::class)
interface GeminiRAGTranslationService {

    @SystemMessage(fromResource = "prompts/translation-rag-system-prompt.txt")
    fun translate(@UserMessage text: String, @V("srcLang") srcLang: String, @V("dstLang") dstLang: String): String
}
```

**プロンプトの組み立て例:**

RAGが有効な状態で `translate("WebAuthn is a standard...", "en", "ja")` を呼び出すと、内部でベクトル検索が行われ、以下のようなプロンプトが組み立てられてLLMに送信されます。

```text
[System Message]
You are a professional translator for open-source project documentation.
Translate the given text from en to ja.

Use the provided information (retrieved from previous translations) as a reference for terminology and style consistency.

Rules: ... (省略)

[User Message]
WebAuthn is a standard for secure web authentication.

Reference Translation Memory (previous translations):
Source: WebAuthn is a standard for secure web authentication. Target: WebAuthn はセキュアなWeb認証のための標準規格です。
```

このように、ユーザーが入力した原文（`User Message`）の末尾に、`Reference Translation Memory` という指示と共に検索された過去の対訳ペアが自動的に付加されます。
これにより、LLMは過去の翻訳例を「参考情報」として参照しながら、一貫性のある翻訳を生成できるようになります。

### ガードレールを活用した軽量マークアップのハンドリング

Asciidocなどのマークアップタグが翻訳時に欠落したり、階層構造が壊れたりする問題に対しては、ガードレール機能を用いた「構文解析による検証」が非常に有効です。

単なる文字列のパターンマッチングではなく、**Asciidocパーサー（Asciidoctor）を使用して原文と訳文をそれぞれ読み込み、構文木（AST）の構造が一致しているか**を厳密にチェックします。

**AST構造を比較するガードレールの実装例:**

```kotlin
@ApplicationScoped
class AsciidocStructureGuardrail(@Inject private val asciidoctor: Asciidoctor) : OutputGuardrail {
    override fun validate(context: OutputGuardrailContext): GuardrailResult {
        val originalText = context.userMessage().contents().filterIsInstance<UserMessage>().first().text()
        val translatedText = context.response().text()

        // 原文と訳文をそれぞれパースして構文木（AST）を取得
        val originalAst = asciidoctor.load(originalText, Options.builder().build())
        val translatedAst = asciidoctor.load(translatedText, Options.builder().build())

        // ブロック数や階層構造が一致するかを検証
        return if (compareAstStructure(originalAst, translatedAst)) {
            GuardrailResult.success()
        } else {
            // 構造が壊れている場合、LLMに具体的なエラーをフィードバックして再生成（自己修正）させる
            GuardrailResult.failure("The translated Asciidoc structure does not match the original. Please ensure all sections and blocks are preserved.")
        }
    }
}
```

このように、パース結果の構造を比較することで、「タグの閉じ忘れ」や「セクション階層の取り違え」といったLLM特有のミスを確実に検知できます。構造異常を検知した場合は、Quarkus AIが自動的にエラーメッセージをLLMへフィードバックし、期待する構造になるまで自動リトライを実行します。これにより、出力の構造的な完全性を100%に近い精度で担保することが可能になります。

### 用語集対応（動的なプロンプト変数の注入）

特定の単語（例：Hibernate、Quarkusなど）の訳語を統一することは、技術ドキュメントの品質において非常に重要です。Quarkus AIの**プロンプトテンプレート機能**を活用することで、アプリケーション側で管理している用語集を動的にプロンプトへ組み込むことが可能です。

メソッド引数に `@V` アノテーション（LangChain4jの変数バインディング）を付けることで、プログラムから動的な文字列（用語集のリストなど）を渡し、それを `@SystemMessage` や `@UserMessage` 内のプレースホルダー（`{{...}}`）に展開することができます。

```kotlin
@RegisterAiService
interface GeminiTranslationService {

    @SystemMessage(
        "You are a professional translator for open-source project documentation.",
        "Translate the given text from {{srcLang}} to {{dstLang}}.",
        "",
        "Use the following glossary to ensure consistent terminology:",
        "{{glossary}}" // ここに @V("glossary") の値が展開される
    )
    fun translate(
        @UserMessage text: String,
        @V("srcLang") srcLang: String,
        @V("dstLang") dstLang: String,
        @V("glossary") glossary: String // 呼び出し元で用意した用語集テキストを渡す
    ): String
}
```

このアプローチにより、LLMに対して「この用語はこのように訳す」というリストをコンテキストとして直接与えるため、翻訳のブレを大きく減らすことができます。また、システム全体の用語集が更新された場合でも、プロンプトファイルやモデル自体を修正することなく、呼び出し側のJava/Kotlinコードから常に最新の用語集を動的に注入できるという利点があります。


---

## まとめ

Quarkus AI（LangChain4j）は単に「LLMのAPIを叩くためのラッパー」にとどまらず、**エンタープライズ品質のAIアプリケーションを構築するための強力なフレームワーク**です。

開発中のQuarkusのドキュメント翻訳の新システムに適用したところ、従来のDeepL APIを用いたドキュメント翻訳で課題となっていた「柔軟なルール適用」や「既存訳の考慮」「マークアップの完全な保持」といった複雑な要件に対して、Quarkus AIは以下のアプローチで明確な解決策を提供してくれます。

1. **LLMの抽象化による柔軟な運用:**
   インターフェースベースの設計により、ビジネスロジックを変更することなく、設定と依存関係の切り替えだけでGeminiやOllamaなど複数のLLMをシームレスに移行・評価できます。
2. **宣言的なプロンプト:**
   `@SystemMessage` や `@V` アノテーションを用いて、翻訳ルールや用語集を組み込んだプロンプトを動的かつクリーンに組み立ててLLMに対し送信することが出来ます。
3. **RAGを活用した「過去の知識」の動的注入:**
   過去の翻訳資産（原文：訳文の対訳）をベクトルデータベース化し、RAG（検索拡張生成）として統合することで、過去の翻訳資産を「参考訳」として動的にプロンプトへ組み込めるようになりました。これにより、ドキュメント全体での用語法やスタイルの一貫性を向上させています。
4. **GuardRailによる「不確実性」の統制:**
   LLM特有のマークアップタグの破壊やフォーマット崩れに対して、Asciidocパーサー等を用いた「GuardRail（ガードレール）」による構文検証と自動リトライ機構を組み込みました。これにより、LLMの不確実な出力を強力に統制し、システムとして安定稼働させることを可能にしています。

**結論として:**
Quarkus AIの真の価値は、これらの高度なAIアーキテクチャを、Quarkusの得意とするDI（依存性注入）やConfig（設定管理）に自然に馴染む形で、非常に少ないコード量で宣言的に実装できる点にあります。AIを「不確実な外部サービス」としてではなく、「堅牢な内部機能」として既存のシステムに組み込みたい場合に極めて有用なフレームワークと言えます。

---
