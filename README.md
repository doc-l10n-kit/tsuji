# tsuji: LangChain4j-Powered Documentation Localization Toolkit

[![CI](https://github.com/doc-l10n-kit/tsuji/actions/workflows/ci.yml/badge.svg)](https://github.com/doc-l10n-kit/tsuji/actions/workflows/ci.yml)

**tsuji** is a localization toolkit designed for translating the [quarkus.io](https://quarkus.io) documentation site. By leveraging the **LangChain4j** framework, it enables high-quality, consistent translations through LLMs with Retrieval-Augmented Generation (RAG) directly integrated into the gettext PO file workflow.

## What is "tsuji"?

**"Tsuji" (通詞) refers to the official interpreters in Japan from the 17th to the 19th century.**

They were not merely translators of language. They served as a **"Gateway of Knowledge,"** playing a crucial role in introducing the latest Western science and medicine to Japan. Following their legacy, this project aims to bridge the gap between languages and deliver knowledge to developers globally using modern AI technology.

## Key Features

- **Dual Translator Support**: Choose between **Google Gemini** (via LangChain4j) and **DeepL** as the translation engine. Each uses a different markup protection strategy optimized for its capabilities.
- **RAG-Enhanced Translation**: Automatically retrieves relevant translation context from your existing TMX (Translation Memory eXchange) files using Lucene vector search, ensuring terminology consistency without manual model training.
- **Adaptive Parallelism**: Two-level AIMD (Additive Increase / Multiplicative Decrease) control automatically adjusts both API request concurrency and batch size in response to rate limits, maximizing throughput while respecting API constraints.
- **AsciiDoc Markup Protection**: Gemini translates AsciiDoc natively with post-translation validation and retry via AsciidoctorJ + jsoup. DeepL uses an HTML round-trip pipeline (AsciiDoc → HTML → translate → HTML → AsciiDoc) with 11 specialized message processors.
- **Structured Batch Translation**: Sends multiple texts per LLM request using JSON Schema-constrained output, with index-based validation to ensure correct mapping between source and translated texts.
- **Glossary Support**: Define terminology mappings in configuration to inject into translation prompts for consistent term usage.
- **PO File Management**: Comprehensive tools for normalizing, purging, updating, and applying PO files, plus word-count-based translation statistics.
- **TMX Operations**: Generate Translation Memory from PO files (confirmed or fuzzy translations) and apply TMX translations back to PO files.
- **Jekyll Integration**: Seamlessly handles PO extraction, build processes, and previews for translated Jekyll sites.

## Project Structure

This is a multi-module Gradle project:

```
tsuji (root)         — Main CLI application (Kotlin / Quarkus / PicocLI)
├── tsuji-po         — PO file domain model and I/O library (jgettext)
└── tsuji-tmx        — TMX file domain model and I/O library (Jackson XML)
```

The root module follows a 3-layer architecture:

- **App Service Layer** — Use case orchestration and workflow control
- **Core Service Layer** — Pure domain logic (statistics, translation eligibility, TMX generation)
- **Core Driver Layer** — External integrations (LLM APIs, vector store, file I/O, external processes)

## CLI Commands

### PO File Operations (`po`)

| Command | Description |
|---|---|
| `po machine-translate` | Translate PO files using LLM or DeepL |
| `po normalize` | Normalize PO file syntax via `msgcat` |
| `po update` | Update PO files from source documents via `po4a` |
| `po apply` | Generate translated documents from PO via `po4a` |
| `po apply-tmx` | Apply TMX translations to PO files (confirmed) |
| `po apply-fuzzy-tmx` | Apply TMX translations to PO files (fuzzy) |
| `po remove-obsolete` | Remove obsolete (#~) entries |
| `po purge-fuzzy` | Clear translations of fuzzy messages |
| `po purge-all` | Clear all translations |
| `po update-po-stats` | Calculate translation progress statistics |

### TMX Operations (`tmx`)

| Command | Description |
|---|---|
| `tmx generate` | Generate TMX from PO files (CONFIRMED or FUZZY mode) |

### RAG Operations (`rag`)

| Command | Description |
|---|---|
| `rag index` | Build or update vector index from TMX files |

### Jekyll Operations (`jekyll`)

| Command | Description |
|---|---|
| `jekyll extract` | Extract PO files from Jekyll source |
| `jekyll build` | Build translated Jekyll site |
| `jekyll serve` | Preview translated site locally |
| `jekyll update-stats` | Update site-wide translation statistics |

### Configuration (`config`)

| Command | Description |
|---|---|
| `config get` | Display current configuration |

## Configuration

tsuji uses [Quarkus SmallRye Config](https://quarkus.io/guides/config-reference) for configuration. All properties can be set via:

- **`application.yml`** in the working directory or classpath
- **Environment variables** (e.g., `tsuji.translator.type` → `TSUJI_TRANSLATOR_TYPE`)
- **System properties** (e.g., `-Dtsuji.translator.type=gemini`)

### Language

| Property | Default | Description |
|---|---|---|
| `tsuji.language.from` | `en` | Source language code |
| `tsuji.language.to` | `ja` | Target language code |

### Translator

| Property | Default | Description |
|---|---|---|
| `tsuji.translator.type` | `deepl` | Translation engine to use: `gemini` or `deepl` |
| `tsuji.translator.target-directories` | *(none)* | List of subdirectories under `tsuji.po.base-dir` to translate. If omitted, the entire base directory is processed |
| `tsuji.translator.deepl.key` | *(none)* | DeepL API key. Can also be set via `TSUJI_TRANSLATOR_DEEPL_KEY` |
| `tsuji.translator.gemini.key` | *(none)* | Gemini API key. Can also be set via `QUARKUS_LANGCHAIN4J_GEMINI_API_KEY` |
| `tsuji.translator.gemini.model` | `gemini-2.5-flash` | Gemini model ID |

#### Gemini Batch Settings

Controls how many texts are sent per LLM request.

| Property | Default | Description |
|---|---|---|
| `tsuji.translator.gemini.batch.initial-texts-per-request` | `200` | Initial number of texts per batch request |
| `tsuji.translator.gemini.batch.max-texts-per-request` | `200` | Maximum number of texts per batch request |
| `tsuji.translator.gemini.batch.max-text-size-bytes` | `700000` | Maximum total text size in bytes per batch request |

#### Gemini Adaptive Concurrency

Controls the adaptive parallelism for API requests (AIMD algorithm).

| Property | Default | Description |
|---|---|---|
| `tsuji.translator.gemini.adaptive.enabled` | `true` | Enable adaptive concurrency control |
| `tsuji.translator.gemini.adaptive.initial-concurrency` | `40` | Initial number of parallel API requests |
| `tsuji.translator.gemini.adaptive.min-concurrency` | `1` | Minimum concurrency (floor for AIMD decrease) |
| `tsuji.translator.gemini.adaptive.max-concurrency` | `60` | Maximum concurrency (ceiling for AIMD increase) |
| `tsuji.translator.gemini.adaptive.max-retries` | `3` | Maximum retry attempts per batch on error |

### RAG (Retrieval-Augmented Generation)

| Property | Default | Description |
|---|---|---|
| `tsuji.rag.index-path` | `l10n/rag/index` | Path to the Lucene vector index directory |
| `tsuji.rag.max-results` | `3` | Maximum number of similar translations to retrieve per text |
| `tsuji.rag.min-score` | `0.5` | Minimum similarity score threshold for retrieval (0.0–1.0) |

### PO Files

| Property | Default | Description |
|---|---|---|
| `tsuji.po.base-dir` | `l10n/po/ja_JP` | Base directory for PO files |

### Jekyll

| Property | Default | Description |
|---|---|---|
| `tsuji.jekyll.source-dir` | `upstream` | Directory containing the original Jekyll source |
| `tsuji.jekyll.override-dir` | `l10n/override/ja_JP` | Directory with locale-specific overrides applied on top of the source |
| `tsuji.jekyll.destination-dir` | `docs` | Output directory for the built Jekyll site |
| `tsuji.jekyll.stats-dir` | `l10n/stats` | Directory for translation statistics output |
| `tsuji.jekyll.additional-configs` | *(none)* | Additional Jekyll config files to merge (comma-separated) |
| `tsuji.jekyll.cname` | *(none)* | CNAME value for the built site. Not used by tsuji itself; exposed via `config get` for external CI/CD scripts |
| `tsuji.jekyll.surge-domain-suffix` | *(none)* | Surge.sh domain suffix for preview deployments. Not used by tsuji itself; exposed via `config get` for external CI/CD scripts |
| `tsuji.jekyll.jekyll-l10n-branch` | `main` | Git branch (or tag) of the jekyll-l10n plugin to install |
| `tsuji.jekyll.extract.yaml.exclude` | *(none)* | YAML front matter keys to exclude from PO extraction |
| `tsuji.jekyll.extract.html.include` | *(none)* | HTML file patterns to include in PO extraction |

### Git

| Property | Default | Description |
|---|---|---|
| `tsuji.git.user.name` | *(none)* | Git user name. Not used by tsuji itself; exposed via `config get` for external CI/CD scripts |
| `tsuji.git.user.email` | *(none)* | Git user email. Not used by tsuji itself; exposed via `config get` for external CI/CD scripts |

### Glossary

| Property | Default | Description |
|---|---|---|
| `tsuji.glossary.enabled` | `false` | Enable glossary injection into translation prompts |
| `tsuji.glossary.entries` | *(none)* | List of term-translation pairs |

Glossary entries are defined as a list in `application.yml`:

```yaml
tsuji:
  glossary:
    enabled: true
    entries:
      - term: "dependency injection"
        translation: "依存性注入"
      - term: "build time"
        translation: "ビルド時"
```

### Example Configuration

```yaml
tsuji:
  language:
    from: "en"
    to: "ja"

  translator:
    type: "gemini"
    gemini:
      batch:
        initial-texts-per-request: 200
        max-texts-per-request: 200
      adaptive:
        initial-concurrency: 40
        max-concurrency: 60
        max-retries: 3

  rag:
    index-path: "l10n/rag/index"
    max-results: 3
    min-score: 0.5

  po:
    base-dir: "l10n/po/ja_JP"

  jekyll:
    source-dir: "upstream"
    destination-dir: "docs"
```

## Getting Started

### Prerequisites

- **JDK 21**
- **Gettext**: Required for PO file operations (e.g., `msgcat`).
- **Po4a**: Required for converting between original sources and PO files.
- **Git**: Required for retrieving commit timestamps for synchronization status.
- **LLM API Key**: Default implementation uses Google Gemini (set via `QUARKUS_LANGCHAIN4J_GEMINI_API_KEY`). For DeepL, set `TSUJI_TRANSLATOR_DEEPL_API_KEY`.

### Building

```bash
./gradlew build
```

### Running the CLI

You can run the CLI in development mode using Quarkus:

```bash
./gradlew quarkusDev --quarkus-args='<args>'
```

Or run the built JAR:

```bash
java -jar build/tsuji.jar <command> [options]
```

### Running Tests

```bash
./gradlew test       # Run all unit tests
./gradlew systemTest # Run system tests (CLI behavior)
```

## Documentation

- **[Design Document](./design-doc.md)**: Detailed overview of the architecture and core components (Japanese).

## License

This project is licensed under the [Apache License, Version 2.0](./LICENSE.txt).
