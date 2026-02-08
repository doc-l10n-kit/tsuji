# tsuji-tmx

A Kotlin library for handling TMX (Translation Memory eXchange) files.

## Features

- **TMX 1.4 Support**: Load and save TMX files with a focus on core structure (Header, Body, Translation Units).
- **Fast Indexing**: Build an in-memory search index (`TranslationIndex`) for efficient translation lookups.
- **Jackson 3 Integration**: Built on top of the latest Jackson 3 (Next Generation) for high-performance XML processing.
- **Kotlin Native Compatibility**: Designed with Kotlin data classes and immutable builders.

## Usage

### Decoding TMX
```kotlin
val codec = TmxCodec()
val tmx = codec.load(Path.of("memory.tmx"))
```

### Searching Translations
```kotlin
val index = TranslationIndex.create(tmx, "ja_JP")
val translation = index["Hello World"] // returns "こんにちは世界"
```

## Dependencies

- Jackson 3.0 (dataformat-xml, module-kotlin)
- SLF4J for logging
- JUnit 5 & AssertJ for testing
