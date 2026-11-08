package com.hyngng.gemtalk.data

import kotlin.math.sqrt

/** Small, dependency-free text similarity used by local note search. */
object TextSimilarity {

    private const val VECTOR_SIZE = 256

    fun bigrams(text: String): List<String> {
        val normalized = text.lowercase()
            .filter { it.isLetterOrDigit() || it == ' ' }
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.length < 2) return listOf(normalized).filter { it.isNotBlank() }
        return (0 until normalized.length - 1).map { normalized.substring(it, it + 2) }
    }

    fun hashedVector(text: String): FloatArray {
        val vector = FloatArray(VECTOR_SIZE)
        bigrams(text).forEach { gram ->
            val index = (gram.hashCode() and Int.MAX_VALUE) % VECTOR_SIZE
            vector[index] += 1f
        }
        val length = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        if (length > 0f) {
            for (i in vector.indices) vector[i] /= length
        }
        return vector
    }

    fun cosine(a: String, b: String): Float {
        val va = hashedVector(a)
        val vb = hashedVector(b)
        var dot = 0f
        for (i in va.indices) dot += va[i] * vb[i]
        return dot.coerceIn(0f, 1f)
    }

    fun keywordOverlap(query: String, text: String): Float {
        val queryGrams = bigrams(query).distinct()
        if (queryGrams.isEmpty()) return 0f
        val textGrams = bigrams(text).toHashSet()
        val matched = queryGrams.count { it in textGrams }
        return matched.toFloat() / queryGrams.size
    }
}
