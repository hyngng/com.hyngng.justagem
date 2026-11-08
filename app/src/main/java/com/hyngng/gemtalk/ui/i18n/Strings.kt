package com.hyngng.gemtalk.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Languages the UI can switch to. The stored tag is what [SessionStore] keeps,
 * so adding a language only needs a new enum entry and its translations.
 */
enum class AppLanguage(val tag: String, val label: String) {
    KOREAN("ko", "한국어"),
    ENGLISH("en", "English"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: device()

        /** The UI language follows the system locale; the app has no language setting. */
        fun device(): AppLanguage =
            if (java.util.Locale.getDefault().language.equals("ko", ignoreCase = true)) KOREAN else ENGLISH
    }
}

/**
 * Inline translator. Keeping the Korean source next to the English text avoids
 * a key catalogue that needs its own maintenance, and makes missing strings
 * obvious while reading the UI code.
 */
class Strings(val language: AppLanguage) {
    fun t(korean: String, english: String): String =
        if (language == AppLanguage.ENGLISH) english else korean
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
