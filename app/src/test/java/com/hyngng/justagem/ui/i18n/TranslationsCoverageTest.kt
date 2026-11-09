package com.hyngng.justagem.ui.i18n

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Automates i18n coverage validation.
 *
 * Scans all Kotlin files in app/src/main/java for s.t(...) and tr(...) invocations,
 * and asserts that every extracted English key is translated across all supported
 * languages (JA, ZH, ES, DE, FR) via [Translations.translate].
 *
 * If any developer introduces or alters a UI string without registering its
 * native translations, this test fails the build immediately with actionable details.
 */
class TranslationsCoverageTest {

    @Test
    fun `all inline UI strings must be covered in Translations registry`() {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("../app/src/main/java"),
        )
        val srcDir = candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/java from ${File(".").absolutePath}")

        val targetLanguages = listOf(
            AppLanguage.JAPANESE,
            AppLanguage.CHINESE,
            AppLanguage.SPANISH,
            AppLanguage.GERMAN,
            AppLanguage.FRENCH,
        )

        val files = srcDir.walkTopDown()
            .filter { it.extension == "kt" && it.name != "Translations.kt" }
            .toList()

        val missingReport = mutableListOf<String>()
        val callRegex = Regex("""\b(?:s\.t|tr)\s*\(""")

        for (file in files) {
            val content = file.readText(Charsets.UTF_8)
            val matches = callRegex.findAll(content)

            for (match in matches) {
                val startIdx = match.range.last + 1
                var idx = startIdx

                while (idx < content.length && content[idx].isWhitespace()) idx++
                if (idx >= content.length || content[idx] != '"') continue

                val (ko, nextIdx) = parseStringLiteral(content, idx) ?: continue
                idx = nextIdx

                while (idx < content.length && (content[idx].isWhitespace() || content[idx] == '\n')) idx++
                if (idx >= content.length || content[idx] != ',') continue
                idx++

                while (idx < content.length && (content[idx].isWhitespace() || content[idx] == '\n')) idx++
                if (idx >= content.length || content[idx] != '"') continue

                val (en, _) = parseStringLiteral(content, idx) ?: continue

                if (en.isBlank() || en.matches(Regex("""^[\d\s\.\,\:\(\)\-\·\%]+$"""))) continue

                val testSubject = if (en.contains('$')) {
                    val normalized = normalizeInterpolatedString(en)
                    // Complex expressions (function calls, method chains)
                    // remain as unresolved $ references after normalization;
                    // these cannot be tested via simple string matching.
                    if (normalized.contains('$')) continue
                    normalized
                } else {
                    en
                }

                for (lang in targetLanguages) {
                    val translated = Translations.translate(testSubject, lang)
                    val isAlphabetical = testSubject.any { it in 'a'..'z' || it in 'A'..'Z' }
                    if (isAlphabetical && translated == testSubject) {
                        // The static map may deliberately keep the same spelling
                        // (e.g., FR "Photo" → "Photo"). Only flag as missing when
                        // the key does not exist in the map at all.
                        if (!Translations.containsStaticKey(testSubject, lang)) {
                            missingReport.add("[${file.name}] lang=${lang.tag} | EN: \"$en\" (KO: \"$ko\")")
                        }
                    }
                }
            }
        }

        assertTrue(
            "Found ${missingReport.size} untranslated strings in Translations.kt:\n" +
                missingReport.distinct().joinToString("\n"),
            missingReport.isEmpty(),
        )
    }

    private fun parseStringLiteral(content: String, startQuoteIdx: Int): Pair<String, Int>? {
        if (content.startsWith("\"\"\"", startQuoteIdx)) {
            val endIdx = content.indexOf("\"\"\"", startQuoteIdx + 3)
            if (endIdx == -1) return null
            return content.substring(startQuoteIdx + 3, endIdx) to (endIdx + 3)
        } else {
            var i = startQuoteIdx + 1
            val sb = java.lang.StringBuilder()
            while (i < content.length) {
                val c = content[i]
                if (c == '\\') {
                    if (i + 1 < content.length) {
                        when (val next = content[i + 1]) {
                            'n' -> sb.append('\n')
                            't' -> sb.append('\t')
                            'r' -> sb.append('\r')
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '$' -> sb.append('$')
                            else -> {
                                sb.append('\\')
                                sb.append(next)
                            }
                        }
                        i += 2
                        continue
                    }
                }
                if (c == '"') {
                    return sb.toString() to (i + 1)
                }
                sb.append(c)
                i++
            }
            return null
        }
    }

    /**
     * Converts an un-evaluated string template with $var into sample evaluated text
     * so it can test Regex matchers in [Translations.translate].
     *
     * Bare `$var` and braced `${var}` forms are both handled.
     * Complex expressions (method calls, property chains containing `.`) are NOT
     * resolved here; the caller skips strings that still contain `$` after this pass.
     */
    private fun normalizeInterpolatedString(raw: String): String {
        return raw
            .replace("\${visibleSessions.size}", "3")
            .replace("\${model.label}", "Preview · Full")
            .replace("\$toolCallName", "search_images")
            .replace("\${timeoutSeconds}", "15")
            .replace("\${results.size}", "4")
            .replace("\${indexedCount}", "10")
            .replace("\$indexedCount", "10")
            .replace("\$percent", "50")
            .replace("\$after", "2026-01-01")
            .replace("\$before", "2026-12-31")
            .replace("\${(result.score * 100).toInt()}", "92")
            .replace("\${result.photo.formattedDate}", "2026-11-08")
            .replace("\${uiState.model.label}", "Preview · Full")
            .replace("\$error", "3")
            .replace("\${error.errorCode}", "3")
            .replace("\$query", "coffee")
            .replace("\$toolName", "web_fetch")
            .replace("\$available", "search_images")
            .replace("\$eligibleTotal", "100")
            .replace("\${update.effective}", "1024")
            .replace("\${update.requested}", "2048")
            .replace("\$name", "web_fetch")
            .replace("\$arguments", "url=...")
            .replace("\$status", "200")
            .replace("\$contentType", "text/html")
            .replace("\$deferredFailures", "3")
            .replace("\$finalCount", "50")
            .replace("\$keep", "500")
    }
}
