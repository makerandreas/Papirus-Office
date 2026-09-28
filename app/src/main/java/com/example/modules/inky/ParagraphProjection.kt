package com.example.modules.inky

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.makerandreas.papirusoffice.data.*

/**
 * Display-only wrapping/page gaps for ONE logical input connection. Neither
 * inserted line breaks nor page gaps enter the edit value, undo history or IME.
 */
internal class ParagraphProjection(
    private val lines: List<LineLayout>,
    private val gapsBefore: List<Float>,
    private val unitSp: Float,
    private val paragraph: OfficeParagraph,
    private val styles: DocumentStyles
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // A transient edit may arrive before the new layout. Never map it using stale offsets.
        if (lines.isEmpty() || lines.last().endOffset != text.length) return TransformedText(text, OffsetMapping.Identity)
        val out = AnnotatedString.Builder()
        val original = IntArray(text.length + 1)
        val transformed = mutableListOf<Int>()
        val base = OfficeRuns.baseStyle(paragraph, styles)
        fun append(char: Char, source: Int) { transformed += source; out.append(char) }
        fun lineStyle(start: Int, end: Int, height: Float, left: Float) {
            if (end <= start) return
            out.addStyle(ParagraphStyle(lineHeight = (height * unitSp).sp,
                textAlign = TextAlign.Left,
                textIndent = TextIndent((left * unitSp).sp, (left * unitSp).sp)), start, end)
        }
        for ((lineIndex, line) in lines.withIndex()) {
            val gap = gapsBefore.getOrElse(lineIndex) { 0f }
            if (gap > 0f) {
                val gapStart = out.length
                append('\n', line.startOffset)
                lineStyle(gapStart, out.length, gap, 0f)
                // The spacer must not inherit a tall font from a preceding run.
                out.addStyle(SpanStyle(fontSize = 0.01f.sp), gapStart, out.length)
            }
            val start = out.length
            for (source in line.startOffset until line.endOffset) {
                original[source] = out.length
                val char = text[source]
                when (char) {
                    '\r', '\n', '\u00ad' -> Unit
                    '\t' -> {
                        val tabStart = out.length
                        append('\u00a0', source)
                        val caret = line.caretOffsets.indexOf(source)
                        val advance = if (caret >= 0 && caret + 1 < line.caretAdvances.size)
                            line.caretAdvances[caret + 1] - line.caretAdvances[caret] else 0f
                        var runOffset = 0
                        val run = paragraph.runs.firstOrNull {
                            val hit = source in runOffset until runOffset + it.text.length
                            runOffset += it.text.length
                            hit
                        }
                        val resolved = run?.let { OfficeRuns.mergeRun(it, base, styles) } ?: base
                        val spaceWidth = TextMetrics.forStyle(resolved).widthOf(" ").coerceAtLeast(0.01f)
                        out.addStyle(SpanStyle(textGeometricTransform = TextGeometricTransform(
                            scaleX = (advance / spaceWidth).coerceAtLeast(0.001f))), tabStart, out.length)
                    }
                    else -> append(char, source)
                }
            }
            if (line.discretionaryHyphen) append('-', line.endOffset)
            original[line.endOffset] = out.length
            if (lineIndex < lines.lastIndex) append('\n', line.endOffset)
            // An empty final line still has the preceding newline's paragraph style.
            lineStyle(start, out.length, line.height, line.left)
        }
        original[text.length] = out.length
        transformed += text.length
        for (span in text.spanStyles) {
            val start = original[span.start]
            val end = original[span.end]
            if (start < end) out.addStyle(span.item, start, end)
        }
        return TransformedText(out.toAnnotatedString(), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = original[offset.coerceIn(0, original.lastIndex)]
            override fun transformedToOriginal(offset: Int): Int = transformed[offset.coerceIn(0, transformed.lastIndex)]
        })
    }
}
