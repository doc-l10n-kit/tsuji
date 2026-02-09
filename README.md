# tsuji: LangChain4j-Powered Documentation Localization Toolkit

[![CI](https://github.com/doc-l10n-kit/tsuji/actions/workflows/ci.yml/badge.svg)](https://github.com/doc-l10n-kit/tsuji/actions/workflows/ci.yml)

Version: 0.5.4-SNAPSHOT

**tsuji** is a localization toolkit designed to assist in translating technical documentation, specifically for quarkus.io. By leveraging the **LangChain4j** framework, it enables high-quality, consistent translations through LLMs with Retrieval-Augmented Generation (RAG) directly integrated into the gettext PO file workflow.

## What is "tsuji"?

**"Tsuji" (通詞) refers to the official interpreters in Japan from the 17th to the 19th century.**

They were not merely translators of language. They served as a **"Gateway of Knowledge,"** playing a crucial role in introducing the latest Western science and medicine to Japan. Following their legacy, this project aims to bridge the gap between languages and deliver knowledge to developers globally using modern AI technology.

## Key Features

- **LangChain4j Integration**: Built on top of LangChain4j, providing a robust and flexible architecture for LLM interactions. While it defaults to Google Gemini, it is designed with the extensibility of the LangChain4j ecosystem in mind.
- **RAG-Enhanced Translation**: Automatically retrieves relevant translation context from your existing TMX (Translation Memory eXchange) files using vector search, ensuring terminology consistency without manual model training.
- **Markup Protection**: Specifically designed for AsciiDoc; it protects links, images, and inline markup during the LLM translation process using AsciidoctorJ.
- **PO File Management**: Comprehensive tools for normalizing PO files, purging fuzzy messages, and generating word-count based translation statistics.
- **Jekyll Integration**: Seamlessly handles PO extraction, build processes, and previews for translated Jekyll sites.

## Project Structure

This is a multi-module Gradle project consisting of:

- **[tsuji-cli](./tsuji-cli)**: The main CLI application for PO file management, statistical analysis, and machine translation powered by LangChain4j.
- **[tsuji-tmx](./tsuji-tmx)**: A high-performance Kotlin library for handling TMX files, built with Jackson.

## Getting Started

### Prerequisites

- **JDK 21**
- **Gettext**: Required for PO file operations (e.g., `msgcat`).
- **Po4a**: Required for converting between original sources and PO files.
- **Git**: Required for retrieving commit timestamps for synchronization status.
- **LLM API Key**: Default implementation uses Google Gemini (set via `QUARKUS_LANGCHAIN4J_GEMINI_API_KEY`).

### Building

```bash
./gradlew build
```

### Running the CLI

You can run the CLI in development mode using Quarkus:

```bash
./gradlew :tsuji-cli:quarkusDev --quarkus-args='<args>'
```

### Running Tests

```bash
./gradlew test                  # Run unit tests
./gradlew :tsuji-cli:systemTest # Run system tests (CLI behavior)
```

## Documentation

- **[Design Document](./tsuji-cli/design-doc.md)**: Detailed overview of the architecture and core components (Japanese).
- **[CLI Reference](./tsuji-cli/README.md)**: Comprehensive guide on how to use the `tsuji` commands.

## License

This project is licensed under the [Apache License, Version 2.0](./LICENSE.txt).
