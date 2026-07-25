package com.makerandreas.papirusoffice.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize

/**
 * TextLayoutManager - Papirus Engine implementation emulating LibreOffice Writer's `SwTxtFrm`
 *
 * Writer Architecture Reference:
 * - SwTxtFrm: Represents a physical frame containing paragraph text. Responsible for converting
 *   document coordinates (twips/pixels) to character offsets (paragraph string index) and vice versa.
 * - SwTxtNode: Holds the actual string content and attribute array.
 * - SwParaPortion: Main cached layout data structure containing SwLineLayout objects.
 * - SwCharRange & nDelta: Incremental invalidation trackers for efficient re-formatting.
 * - SwScriptInfo: Script type classification boundaries (Latin, CJK, CTL).
 */
class TextLayoutManager(
    private var paragraphText: String = "",
    var fontSizePt: Int = 12,
    var lineSpacingFactor: Float = 1.0f,
    var activeStyleName: String = "Normal"
) {
    // Invalidation state tracking (Writer SwParaPortion::aReformat & nDelta)
    var isLayoutValid: Boolean = false
        private set
    var invalidStartOffset: Int = 0
        private set
    var invalidEndOffset: Int = 0
        private set
    var nDelta: Int = 0 // Sum of added (+) or removed (-) characters since last full format
        private set

    // Calculated layout caching (SwParaPortion)
    private var cachedSummary: SwTextFormattingEngine.ParaPortionSummary? = null

    init {
        reformatLayout()
    }

    /**
     * Updates the underlying paragraph text content (SwTxtNode::XubString).
     * Triggers incremental invalidation tracking (SwCharRange & nDelta).
     */
    fun updateText(newText: String) {
        if (newText == paragraphText) return

        val oldLen = paragraphText.length
        val newLen = newText.length
        val delta = newLen - oldLen

        // Find change start position
        var changeStart = 0
        val minLen = minOf(oldLen, newLen)
        while (changeStart < minLen && paragraphText[changeStart] == newText[changeStart]) {
            changeStart++
        }

        paragraphText = newText
        invalidStartOffset = changeStart
        invalidEndOffset = maxOf(changeStart, changeStart + kotlin.math.abs(delta))
        nDelta += delta
        isLayoutValid = false

        reformatLayout()
    }

    /**
     * Re-formats the paragraph layout, re-building lines and portions (SwTxtFormatter::FormatLine)
     */
    fun reformatLayout(
        dropCapEnabled: Boolean = false,
        dropCapLines: Int = 3,
        asianGridEnabled: Boolean = false,
        hangingPunctuation: Boolean = true
    ): SwTextFormattingEngine.ParaPortionSummary {
        val summary = SwTextFormattingEngine.computeFormattingSummary(
            text = paragraphText,
            styleName = activeStyleName,
            fontSizePt = fontSizePt,
            lineSpacingFactor = lineSpacingFactor,
            dropCapEnabled = dropCapEnabled,
            dropCapLines = dropCapLines,
            asianGridEnabled = asianGridEnabled,
            hangingPunctuation = hangingPunctuation
        )
        cachedSummary = summary
        isLayoutValid = true
        nDelta = 0
        return summary
    }

    /**
     * Writer `SwTxtFrm::GetCharRect`
     * Converts a character offset in the text string into bounding physical rectangle coordinates (in twips/pixels).
     */
    fun getCharRect(charIndex: Int, viewportWidthPx: Float = 600f): Rect {
        val summary = cachedSummary ?: reformatLayout()
        val clampedIndex = charIndex.coerceIn(0, paragraphText.length)

        // Find line containing clampedIndex
        val line = summary.lines.find { line ->
            clampedIndex >= line.startCharIdx && clampedIndex < (line.startCharIdx + line.lengthChars)
        } ?: summary.lines.lastOrNull() ?: return Rect(0f, 0f, 10f, 20f)

        val charOffsetInLine = clampedIndex - line.startCharIdx
        val approxCharWidthPx = (fontSizePt * 1.33f)
        val lineTopPx = (line.lineNumber - 1) * (line.heightTwips / 20f * 1.33f)

        val x = (charOffsetInLine * approxCharWidthPx).coerceAtMost(viewportWidthPx - approxCharWidthPx)
        val y = lineTopPx
        val width = approxCharWidthPx
        val height = line.heightTwips / 20f * 1.33f

        return Rect(x, y, x + width, y + height)
    }

    /**
     * Writer `SwTxtFrm::GetCrsrOfst`
     * Converts physical viewport coordinates (x, y) back into a string character offset index.
     * Used for touch/mouse cursor positioning in Papirus Office editor.
     */
    fun getCrsrOffset(point: Offset, viewportWidthPx: Float = 600f): Int {
        val summary = cachedSummary ?: reformatLayout()
        if (summary.lines.isEmpty()) return 0

        val lineHeightPx = (summary.lineSpacingTwips / 20f * 1.33f)
        val approxCharWidthPx = (fontSizePt * 1.33f)

        val estimatedLineIndex = ((point.y / lineHeightPx).toInt() + 1).coerceIn(1, summary.lines.size)
        val line = summary.lines.getOrNull(estimatedLineIndex - 1) ?: summary.lines.last()

        val charCol = (point.x / approxCharWidthPx).toInt().coerceIn(0, line.lengthChars)
        return (line.startCharIdx + charCol).coerceIn(0, paragraphText.length)
    }

    /**
     * Synchronizes selection state with Jetpack Compose TextFieldValue
     */
    fun createTextFieldValue(selectionStart: Int = 0, selectionEnd: Int = selectionStart): TextFieldValue {
        val validStart = selectionStart.coerceIn(0, paragraphText.length)
        val validEnd = selectionEnd.coerceIn(0, paragraphText.length)
        return TextFieldValue(
            text = paragraphText,
            selection = TextRange(validStart, validEnd)
        )
    }

    /**
     * Returns current cached summary or generates a new one
     */
    fun getLayoutSummary(): SwTextFormattingEngine.ParaPortionSummary {
        return cachedSummary ?: reformatLayout()
    }
}
