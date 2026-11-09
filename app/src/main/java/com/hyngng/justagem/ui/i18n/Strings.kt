package com.hyngng.justagem.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Languages the UI can switch to. The stored tag is what [SessionStore] keeps,
 * so adding a language only needs a new enum entry and its translations.
 */
enum class AppLanguage(val tag: String, val label: String) {
    KOREAN("ko", "한국어"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh", "中文"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    ;

    /**
     * BCP-47 언어 태그([tag])를 기반으로 도출된 시스템 로케일.
     * 새로운 언어가 enum에 추가되어도 별도 분기 없이 STT/TTS 오디오 엔진에 자동 바인딩된다.
     */
    val locale: java.util.Locale
        get() = if (tag == "zh") java.util.Locale.SIMPLIFIED_CHINESE else java.util.Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: device()

        /** Blank/unknown tags (an unset setting) fall back to the system locale. */
        fun resolve(tag: String?): AppLanguage = fromTag(tag?.takeIf { it.isNotBlank() })

        /** Default when no explicit language choice is stored. */
        fun device(): AppLanguage {
            val lang = java.util.Locale.getDefault().language.lowercase()
            return when (lang) {
                "ko" -> KOREAN
                "ja" -> JAPANESE
                "zh" -> CHINESE
                "es" -> SPANISH
                "de" -> GERMAN
                "fr" -> FRENCH
                else -> ENGLISH
            }
        }
    }
}

/**
 * Inline translator. Keeping the Korean source next to the English text avoids
 * a key catalogue that needs its own maintenance, and makes missing strings
 * obvious while reading the UI code.
 */
class Strings(val language: AppLanguage) {
    fun t(korean: String, english: String): String = when (language) {
        AppLanguage.KOREAN -> korean
        AppLanguage.ENGLISH -> english
        else -> Translations.translate(english, language)
    }
}

val LocalStrings = staticCompositionLocalOf { Strings(AppLanguage.device()) }

@Volatile
private var processLanguage = AppLanguage.device()

/**
 * Non-composable code (ViewModels, managers) cannot read a CompositionLocal.
 * The active language is mirrored here whenever the setting changes so their
 * messages use the same language as the screen.
 */
fun updateProcessLanguage(language: AppLanguage) {
    processLanguage = language
}

fun tr(korean: String, english: String): String = Strings(processLanguage).t(korean, english)
