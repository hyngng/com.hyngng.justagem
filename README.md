# GemTalk

[English](README.md) | [한국어](README-ko.md)

GemTalk is an Android chat app that runs a language model **entirely on-device**. It uses the ML Kit Prompt API backed by AICore (Gemini Nano / Gemma 4), so prompts, answers, and tool results never leave the device. There is no backend and no sign-in.

The UI follows the interaction patterns familiar from mainstream AI chat clients (ChatGPT, Claude, Gemini) — a message list with bubbles, a composer dock, a session drawer, and a settings page — re-implemented from scratch with Jetpack Compose.

## Features

- **Fully on-device inference** — chat runs through AICore on the phone. The only optional network access is the `web_fetch` tool, which is explicitly triggered and only fetches user-approved public HTTPS pages.
- **Markdown rendering** — assistant answers, user prompts, and thought transcripts are rendered with Markwon: tables, checklists, strikethrough, code blocks, live during streaming.
- **LaTeX math** — `$...$` and `$$...$$` formulas are parsed by a C++ based engine (RaTeX) and rendered synchronously, including mid-stream.
- **Thinking mode** — optional thought stream shown in a live preview panel with a full-transcript reader, kept out of the model transcript and the TTS queue.
- **Voice chat** — two-way conversation using the platform `SpeechRecognizer` for input and a sentence-streamed TTS queue for output, with a full-screen voice overlay (captions, live meters, haptic-only feedback while speaking).
- **Semantic photo search** — the device photo library is indexed into 768-dim cross-modal vectors with EmbeddingGemma 2 (stored locally in an append-only journal) so the model can search your photos by meaning and describe them.
- **Function calling tools** — `get_datetime`, `search_conversation_history`, `search_images`, and `web_fetch`; tool output is size-capped and xml-wrapped to resist injection.
- **Multimodal input** — attach images from the camera or the photo picker; attachments are downsampled before being sent to the model.
- **Conversation management** — multiple sessions with rename/delete and full-text search, always stored locally with atomic writes (`allowBackup=false`).
- **Generation settings** — temperature, Top-K, max output tokens, and a plain system instruction, with a model warm-up toggle for faster first responses.
- **Bilingual** — every user-facing string ships inline as Korean/English pair, following the system language.

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
./gradlew :app:assembleDebug   # debug APK at app/build/outputs/apk/debug/
./gradlew :app:assembleRelease # release build is R8-optimized but unsigned
```

Documentation on architecture and design decisions lives in `docs/README.md` (Korean).
