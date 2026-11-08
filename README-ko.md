# GemTalk

[English](README.md) | [한국어](README-ko.md)

GemTalk은 언어 모델을 **완전히 온디바이스에서** 실행하는 안드로이드 채팅 앱입니다. AICore 기반의 ML Kit Prompt API(Gemini Nano / Gemma 4)를 사용해 프롬프트·답변·도구 결과가 기기 밖으로 나가지 않으며, 백엔드나 로그인이 없습니다.

UI는 ChatGPT·Claude·Gemini 같은 주요 AI 채팅 클라이언트의 조작 방식을 따라합니다 — 버블 메시지 목록, 입력 도크, 세션 드로어, 설정 페이지 — 단 Jetpack Compose로 처음부터 새로 구현했습니다.

## 특징

- **완전 온디바이스 추론** — 대화가 기기 내 AICore에서 실행됩니다. 유일한 선택적 네트워크 접근은 `web_fetch` 도구로, 사용자가 명시적으로 트리거한 공개 HTTPS 페이지만 불러옵니다.
- **마크다운 렌더링** — 답변·사용자 입력·생각 전문을 Markwon으로 렌더링합니다: 표, 체크리스트, 취소선, 코드 블록, 스트리밍 중 실시간 갱신.
- **LaTeX 수식** — `$...$`/`$$...$$` 수식을 C++ 기반 엔진(RaTeX)으로 파싱해 동기 렌더링하며, 스트리밍 중에도 즉시 표시됩니다.
- **Thinking 모드** — 선택적 사고 스트림을 라이브 미리보기 패널과 전문 읽기 창으로 제공하고, 모델 transcript와 TTS 큐에는 포함하지 않습니다.
- **음성 대화** — 입력은 플랫폼 `SpeechRecognizer`, 출력은 문장 단위 스트리밍 TTS 큐로 양방향 대화를 지원하며, 전체 화면 음성 오버레이(캡션, 실시간 미터, 발화 중 햅틱만 피드백)를 제공합니다.
- **사진 시맨틱 검색** — 기기 사진 보관함을 EmbeddingGemma 2의 768차원 교차 모달 벡터로 색인(Append-only journal로 로컬 저장)해, 모델이 의미 기준으로 사진을 찾고 분석합니다.
- **함수 호출 도구** — `get_datetime`, `search_conversation_history`, `search_images`, `web_fetch`; 도구 결과는 크기 상한과 XML 래핑으로 프롬프트 주입에 대비합니다.
- **멀티모달 입력** — 카메라 또는 사진 선택기로 이미지를 첨부하며, 모델 전송 전에 다운샘플링합니다.
- **대화 관리** — 세션별 이름 변경/삭제·본문 검색을 지원하고, 원자적 쓰기(`allowBackup=false`)로 로컬만 저장합니다.
- **생성 설정** — Temperature, Top-K, 최대 출력 토큰, 평문 시스템 지시, 첫 응답을 빠르게 하는 모델 예열 토글.
- **이중 언어** — 모든 사용자 노출 문구가 한국어/영어 쌍으로 소스에 나란히 있으며 시스템 언어를 따릅니다.

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

아키텍처와 설계 결정 문서는 `docs/README.md`(한국어)에 정리되어 있습니다.
