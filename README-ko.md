# Justagem

[English](README.md) | [한국어](README-ko.md)

Justagem은 언어 모델을 **완전히 온디바이스에서** 실행하는 안드로이드 채팅 앱입니다. AICore 기반의 ML Kit Prompt API(Gemini Nano / Gemma 4)를 사용해 프롬프트, 답변, 도구 결과가 기기 밖으로 나가지 않으며, 백엔드나 로그인이 없습니다.

버블 메시지 목록, 입력 도크, 세션 드로어, 설정 페이지 등 ChatGPT, Claude, Gemini, DeepSeek, Kimi, Meta AI 등 주요 AI 채팅 앱의 조작 경험을 따라하면서도, Jetpack Compose로 처음부터 새로 구현했습니다.

Co-Engineered with:
- Gemini 3.8 Flash
- ChatGPT-6 Luna

## 특징

- 완전 온디바이스 추론
- 마크다운, LaTeX 렌더링
- Thinking 모드
- 음성 대화 모드
- 이미지 시멘틱 검색
- 이미지 입력
- 도구 호출
    - `get_datetime`
    - `web_fetch`
    - `search_images`
    - `search_conversations`
- 세션 단위의 대화 내역 관리
- 모델 설정 지원
- 다국어 지원

## 요구 사항

- **AICore 지원 Android 기기**(minSdk 26, targetSdk 37). 에뮬레이터나 미지원 기기에서는 모델 상태가 "사용 불가"로 표시됩니다.
- 모델 자체는 AICore를 통해 기기에서 다운로드되며, 추론을 위해 앱이 서버에 연결하지 않습니다.
- 빌드에는 JDK 17과 Android Gradle Plugin 9.4.1이 필요합니다(`gradle/libs.versions.toml` 참고).

## 의존성

| 용도 | 라이브러리 |
|---|---|
| UI 언어 | Kotlin 2.4.x, Jetpack Compose(BOM 2026.02.01), Material 3 |
| Android | androidx.core-ktx, lifecycle-runtime(compose/ktx/viewmodel) |
| 온디바이스 모델(AICore) | `com.google.mlkit:genai-prompt` 1.0.0-beta5, `genai-schema`, `genai-speech-recognition` alpha1, `genai-text-to-speech` alpha1, `genai-embedding` alpha1 |
| 마크다운 | Markwon 4.6.2(core, tables, tasklist, strikethrough, inline-parser) |
| LaTeX | RaTeX(`io.github.erweixin:ratex-android` 0.1.15) |
| HTML 추출(`web_fetch` 도구) | jsoup 1.23.2 |

## 빌드

```bash
./gradlew :app:assembleDebug   # debug APK: app/build/outputs/apk/debug/
./gradlew :app:assembleRelease # release 빌드는 R8 최적화되지만 서명 없음
```

## 구동이 확인된 환경

- 스냅드래곤 8 엘리트 gen 5 (12GB RAM)
- 스냅드래곤 8 엘리트 (12GB RAM)