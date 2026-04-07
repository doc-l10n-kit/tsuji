# Translator Comparison Test Resources

This directory contains PO files used for translator comparison system tests.

## Files

10 PO files from the ja.quarkus.io project, with all translation strings cleared (msgstr = ""):

1. funqy-aws-lambda-http.adoc.po (15 messages)
2. funqy-gcp-functions-http.adoc.po (15 messages)
3. grpc-kubernetes.adoc.po (17 messages)
4. grpc-virtual-threads.adoc.po (16 messages)
5. grpc-xds.adoc.po (20 messages)
6. grpc.adoc.po (19 messages)
7. podman.adoc.po (25 messages)
8. quarkus-runtime-base-image.adoc.po (16 messages)
9. security-vulnerability-detection.adoc.po (19 messages)
10. tooling.adoc.po (18 messages)

**Total**: ~320 messages across 10 files

## Purpose

These files are used to test and compare:

- **Translation completion rate**: How many messages are successfully translated
- **Error handling**: How translators handle problematic input (e.g., complex AsciiDoc markup)
- **Performance**: Translation speed for batch processing
- **Large file handling**: Ability to process larger PO files without errors

## Usage

Run system tests with different translator configurations:

```bash
# Test with Gemini (default)
./gradlew systemTest --tests TranslatorComparisonTest

# Test with DeepL
./gradlew systemTest --tests TranslatorComparisonTest \
  -Dtsuji.translator.type=deepl \
  -Dtsuji.translator.deepl.api-key=$DEEPL_API_KEY
```

## Expected Results

### Gemini
- **Completion rate**: >95% (should translate almost all messages)
- **Error handling**: Robust handling of AsciiDoc markup
- **Large files**: Successfully processes files with 100+ messages

### DeepL
- **Completion rate**: ~40-50% (may fail on files with complex markup)
- **Error handling**: Tag handling errors on some AsciiDoc constructs
- **Large files**: May fail completely on larger files

## Origin

These files were extracted from:
- Source: `/home/ynojima/workspace/ja.quarkus.io/l10n/po/ja_JP/_guides`
- Date: 2026-04-06
- Cleared using: `clear-translations.py` script
