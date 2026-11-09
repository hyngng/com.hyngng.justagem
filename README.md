# Justagem

[English](README.md) | [한국어](README-ko.md)

Justagem is an Android chat app that runs a language model **entirely on-device**. It uses the AICore-backed ML Kit Prompt API (Gemini Nano / Gemma 4), so prompts, answers, and tool results never leave the device. There is no backend and no sign-in.

It follows the interaction experience of major AI chat apps — ChatGPT, Claude, Gemini, DeepSeek, Kimi, Meta AI and more — with a bubble message list, a composer dock, a session drawer, and a settings page, re-implemented from scratch with Jetpack Compose.

Co-Engineered with:
- Gemini 3.8 Flash
- ChatGPT-6 Luna

## Features

- Fully on-device inference
- Markdown and LaTeX rendering
- Thinking mode
- Voice chat mode
- Image semantic search
- Image input
- Tool calling
    - `get_datetime`
    - `web_fetch`
    - `search_images`
    - `search_conversations`
- Session-based conversation management
- Model settings
- Multilingual support

## Requirements

- An Android device with **AICore** support (minSdk 26, targetSdk 37). On emulators or unsupported devices the model reports as unavailable.
- The model itself is downloaded through AICore on-device; the app never connects to a server for inference.
- Building requires JDK 17 and Android Gradle Plugin 9.4.1 (see `gradle/libs.versions.toml`).

## Dependencies

| Purpose | Library |
|---|---|
| UI language | Kotlin 2.4.x, Jetpack Compose (BOM 2026.02.01), Material 3 |
| Android | androidx.core-ktx, lifecycle-runtime (compose/ktx/viewmodel) |
| On-device models (AICore) | `com.google.mlkit:genai-prompt` 1.0.0-beta5, `genai-schema`, `genai-speech-recognition` alpha1, `genai-text-to-speech` alpha1, `genai-embedding` alpha1 |
| Markdown | Markwon 4.6.2 (core, tables, tasklist, strikethrough, inline-parser) |
| LaTeX | RaTeX (`io.github.erweixin:ratex-android` 0.1.15) |
| HTML extraction (`web_fetch` tool) | jsoup 1.23.2 |

## Build

```bash
./gradlew :app:assembleDebug   # debug APK: app/build/outputs/apk/debug/
./gradlew :app:assembleRelease # release build is R8-optimized but unsigned
```

## Verified running environments

- Snapdragon 8 Elite gen 5 (12GB RAM)
- Snapdragon 8 Elite (12GB RAM)