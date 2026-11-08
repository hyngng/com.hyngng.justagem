# GemTalk

[English](README.md) | [한국어](README-ko.md)

GemTalk은 언어 모델을 **완전히 온디바이스에서** 실행하는 안드로이드 채팅 앱입니다. AICore 기반의 ML Kit Prompt API(Gemini Nano / Gemma 4)를 사용해 프롬프트·답변·도구 결과가 기기 밖으로 나가지 않으며, 백엔드나 로그인이 없습니다.

UI는 ChatGPT·Claude·Gemini 같은 주요 AI 채팅 클라이언트의 조작 방식을 따라합니다 — 버블 메시지 목록, 입력 도크, 세션 드로어, 설정 페이지 — 단 Jetpack Compose로 처음부터 새로 구현했습니다.

## 특징

- 완전 온디바이스 추론 (AICore에서 실행; 유일한 선택적 네트워크 접근은 명시적으로 트리거되는 `web_fetch` 도구)
- 답변·사용자 입력·생각 전문의 마크다운 렌더링 (스트리밍 중 실시간 갱신)
- LaTeX 수식 (`$...$` 인라인, `$$...$$` 디스플레이) 스트리밍 중에도 동기 렌더링
- Live 사고 미리보기와 전문 읽기 창을 갖춘 Thinking 모드
- 전체 화면 음성 오버레이를 포함한 양방향 음성 대화
- 기기 사진 보관함 대상 시맨틱 사진 검색
- 함수 호출 도구 (`get_datetime`, `search_conversation_history`, `search_images`, `web_fetch`)
- 멀티모달 입력 (카메라 및 사진 선택기 첨부)
- 이름 변경/삭제·본문 검색이 가능한 다중 세션
- 생성 설정 (Temperature, Top-K, 최대 출력 토큰, 시스템 지시, 모델 예열 토글)
- 이중 언어 문구 (한국어/영어 인라인, 시스템 언어 따름)

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
