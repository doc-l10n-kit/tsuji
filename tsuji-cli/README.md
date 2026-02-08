# tsuji-cli

`tsuji-cli` is the core application of the **tsuji** project, providing a CLI toolkit for documentation localization. It handles PO files and leverages the **LangChain4j** framework (with Google Gemini by default) and **RAG (Retrieval-Augmented Generation)** to provide context-aware translations.

## CLI Usage Reference

Examples below use the `./gradlew :tsuji-cli:quarkusDev --quarkus-args='...'` format.

### 1. Indexing Existing Translations (RAG)
Import your TMX files into the vector store to provide context for the AI.
```bash
tsuji rag index --tmx=path/to/memory.tmx
```

### 2. Machine Translation
Translate PO files using AI + RAG.
```bash
tsuji po machine-translate --po=path/to/file.po --source=en --target=ja
```
*Supports both files and directories. RAG is enabled by default. Multiple paths can be provided.*

### 3. PO File Management
- **Update**: Create or update PO from source (e.g., AsciiDoc).
  ```bash
  tsuji po update --master=master.adoc --po=master.adoc.po --format=asciidoc
  ```
- **Apply**: Generate localized file from PO.
  ```bash
  tsuji po apply --master=master.adoc --po=master.adoc.po --localized=ja.adoc --format=asciidoc
  ```
- **Normalize**: Reformat PO files using internal normalization rules.
  ```bash
  tsuji po normalize --po=path/to/dir
  ```
- **Statistics**: Generate a CSV report of translation progress (word-count based).
  ```bash
  tsuji po update-stats --po=l10n/po/ja_JP --output=progress.csv
  ```

### 4. TMX Utilities
- **Generate**: Create a TMX file from translated PO files.
  ```bash
  tsuji tmx generate --po=l10n/po/ja_JP --tmx=output.tmx
  ```
- **Apply**: Fill PO files with translations from a TMX file.
  ```bash
  tsuji po apply-tmx --tmx=memory.tmx --po=path/to/dir
  ```

### 5. Jekyll Integration
- **Extract**: Automatically extract PO files from Jekyll source directory.
  ```bash
  tsuji jekyll extract
  ```
- **Synchronization Status**: Compare commit timestamps between override and upstream files.
  ```bash
  tsuji jekyll update-override-stats --override-dir=l10n/override/ja_JP --upstream-dir=upstream --output=sync.csv
  ```
- **Build/Serve**: Build or preview the translated site.
  ```bash
  tsuji jekyll build --additional-configs=custom.yml
  tsuji jekyll serve
  ```

## Configuration

Configuration is managed via `src/main/resources/application.yml` or environment variables.

```yaml
tsuji:
  rag:
    index-path: index # Storage for RAG vector index
  po:
    base-dir: l10n/po/ja_JP # Base directory for PO operations
  jekyll:
    source-dir: upstream # Jekyll source files
    override-dir: l10n/override/ja_JP # Localized override files
    destination-dir: docs # Build output
  translator:
    language:
      source: en
      destination: ja
```

For more architectural details, please refer to the **[Design Document](./design-doc.md)** (Japanese).
