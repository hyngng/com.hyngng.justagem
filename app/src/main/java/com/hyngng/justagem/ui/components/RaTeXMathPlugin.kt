package com.hyngng.justagem.ui.components

import android.content.Context
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.Layout
import androidx.annotation.ColorInt
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.inlineparser.InlineProcessor
import io.ratex.RaTeXSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.commonmark.node.CustomNode
import org.commonmark.node.Node
import java.util.LinkedHashMap

private class MathNode(
    val latex: String,
    val displayMode: Boolean,
    val source: String,
) : CustomNode()

internal class MathInlineProcessor : InlineProcessor() {
    override fun specialCharacter(): Char = '$'

    override fun parse(): Node? {
        val start = index
        if (start >= input.length || input[start] != '$') return null

        val displayMode = start + 1 < input.length && input[start + 1] == '$'
        val delimiter = if (displayMode) "\$\$" else "\$"
        val contentStart = start + delimiter.length
        var cursor = contentStart

        while (cursor <= input.length - delimiter.length) {
            // Inline math must close before the line ends; only $$ display
            // mode may span rows.
            if (!displayMode && input[cursor] == '\n') return null
            if (input.startsWith(delimiter, cursor) && !isEscaped(cursor)) {
                val latex = input.substring(contentStart, cursor)
                if (latex.isBlank()) return null
                index = cursor + delimiter.length
                return MathNode(latex, displayMode, input.substring(start, index))
            }
            cursor++
        }

        return null
    }

    private fun isEscaped(position: Int): Boolean {
        var slashes = 0
        var cursor = position - 1
        while (cursor >= 0 && input[cursor] == '\\') {
            slashes++
            cursor--
        }
        return slashes % 2 == 1
    }
}

private data class MathSpanKey(
    val latex: String,
    val displayMode: Boolean,
)

internal class RaTeXSpanCache(
    private val context: Context,
    private val fontSizeDp: Float,
    @ColorInt private val color: Int,
) {
    private val spans = object : LinkedHashMap<MathSpanKey, RaTeXSpan>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<MathSpanKey, RaTeXSpan>?): Boolean =
            size > MAX_CACHED_SPANS
    }
    private val failed = object : LinkedHashMap<MathSpanKey, Boolean>(32, 0.75f, false) {
        // Failure entries age out in insertion order like the span cache, so
        // one persistently failing formula cannot crowd out fresh failures.
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<MathSpanKey, Boolean>?): Boolean =
            size > MAX_CACHED_SPANS
    }

    fun getOrRender(latex: String, displayMode: Boolean): RaTeXSpan? {
        val key = MathSpanKey(latex, displayMode)
        synchronized(this) {
            spans[key]?.let { return it }
            if (failed.containsKey(key)) return null
        }

        val rendered = try {
            runBlocking(Dispatchers.Default) {
                RaTeXSpan.create(
                    context = context,
                    latex = latex,
                    fontSize = fontSizeDp,
                    color = color,
                    displayMode = displayMode,
                )
            }
        } catch (_: Exception) {
            null
        }

        synchronized(this) {
            if (rendered == null) {
                failed[key] = true
            } else {
                spans[key] = rendered
            }
        }
        return rendered
    }

    private companion object {
        const val MAX_CACHED_SPANS = 64
    }
}

internal class RaTeXMarkwonPlugin(
    private val spanCache: RaTeXSpanCache,
) : AbstractMarkwonPlugin() {
    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(MathNode::class.java) { visitor, node ->
            if (node.displayMode) visitor.ensureNewLine()
            val start = visitor.length()
            val span = spanCache.getOrRender(node.latex, node.displayMode)
            if (span == null) {
                visitor.builder().append(node.source)
            } else {
                visitor.builder().append('\u200B')
                visitor.builder().setSpan(span, start, visitor.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (node.displayMode) {
                    visitor.builder().setSpan(
                        AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                        start,
                        visitor.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }
            if (node.displayMode) visitor.ensureNewLine()
        }
    }
}
