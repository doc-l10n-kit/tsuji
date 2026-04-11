# tsuji: LangChain4j-Powered Documentation Localization Toolkit

[![CI](https://github.com/doc-l10n-kit/tsuji/actions/workflows/ci.yml/badge.svg)](https://github.com/doc-l10n-kit/tsuji/actions/workflows/ci.yml)

**tsuji** is a localization toolkit designed to assist in translating technical documentation, specifically for quarkus.io. By leveraging the **LangChain4j** framework, it enables high-quality, consistent translations through LLMs with Retrieval-Augmented Generation (RAG) directly integrated into the gettext PO file workflow.

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
