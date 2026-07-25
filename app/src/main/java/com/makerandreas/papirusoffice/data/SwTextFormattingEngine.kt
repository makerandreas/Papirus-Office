package com.makerandreas.papirusoffice.data

import androidx.compose.ui.graphics.Color

/**
 * OpenOffice / LibreOffice Writer Core Text Formatting Data Structures
 * Based on Writer layout engine concepts (SwTxtFrm, SwParaPortion, SwLineLayout, SwDropPortion, SwScriptInfo, SwAttrSet)
 */
object SwTextFormattingEngine {

    enum class ScriptType {
        LATIN,
        ASIAN, // CJK (Chinese, Japanese, Korean)
        CTL    // Complex Text Layout (Arabic, Hebrew, Devanagari, Thai, etc.)
    }

    data class ScriptAnalysis(
        val totalChars: Int,
        val latinCount: Int,
        val asianCount: Int,
        val ctlCount: Int,
        val scriptChanges: List<Int>, // Indices where script type changes (SwScriptInfo)
        val primaryScript: ScriptType
    ) {
        val latinPercent: Float get() = if (totalChars > 0) (latinCount * 100f / totalChars) else 100f
        val asianPercent: Float get() = if (totalChars > 0) (asianCount * 100f / totalChars) else 0f
        val ctlPercent: Float get() = if (totalChars > 0) (ctlCount * 100f / totalChars) else 0f
    }

    data class DropCapInfo(
        val enabled: Boolean = false,
        val linesCount: Int = 3, // Number of lines the drop cap covers (SwDropPortion::nLines)
        val initialChar: String = "",
        val dropHeightTwips: Int = 720, // Height in twips (1 pt = 20 twips)
        val fontScaleFactor: Float = 2.5f,
        val yOffsetTwips: Int = 0
    )

    data class LineLayoutInfo(
        val lineNumber: Int,
        val startCharIdx: Int,
        val lengthChars: Int,
        val heightTwips: Int,
        val ascentTwips: Int,
        val descentTwips: Int,
        val realHeightTwips: Int,
        val portionCount: Int,
        val textSnippet: String
    )

    data class ParaPortionSummary(
        val paragraphIndex: Int,
        val totalLength: Int,
        val activeStyleName: String,
        val lines: List<LineLayoutInfo>,
        val totalPortions: Int,
        val scriptAnalysis: ScriptAnalysis,
        val dropCap: DropCapInfo,
        val lineSpacingFactor: Float,
        val lineSpacingTwips: Int,
        val asianGridEnabled: Boolean,
        val hangingPunctuation: Boolean
    )

    /**
     * Analyzes string for Script Types according to SwScriptInfo
     */
    fun analyzeScript(text: String): ScriptAnalysis {
        if (text.isEmpty()) {
            return ScriptAnalysis(0, 0, 0, 0, emptyList(), ScriptType.LATIN)
        }

        var latin = 0
        var asian = 0
        var ctl = 0
        val scriptChanges = mutableListOf<Int>()
        var currentScript: ScriptType? = null

        text.forEachIndexed { index, ch ->
            val code = ch.code
            val type = when {
                // CJK / Asian ranges
                code in 0x2E80..0x9FFF || code in 0xF900..0xFAFF || code in 0x3000..0x303F -> {
                    asian++
                    ScriptType.ASIAN
                }
                // CTL (Hebrew, Arabic, Devanagari, Thai, etc.)
                code in 0x0590..0x0E7F || code in 0x1000..0x109F -> {
                    ctl++
                    ScriptType.CTL
                }
                else -> {
                    latin++
                    ScriptType.LATIN
                }
            }

            if (currentScript == null) {
                currentScript = type
            } else if (currentScript != type) {
                scriptChanges.add(index)
                currentScript = type
            }
        }

        val primary = when {
            asian >= latin && asian >= ctl -> ScriptType.ASIAN
            ctl >= latin && ctl >= asian -> ScriptType.CTL
            else -> ScriptType.LATIN
        }

        return ScriptAnalysis(text.length, latin, asian, ctl, scriptChanges, primary)
    }

    /**
     * Computes Writer SwParaPortion & SwLineLayout metrics for a text block
     */
    fun computeFormattingSummary(
        text: String,
        styleName: String,
        fontSizePt: Int,
        lineSpacingFactor: Float,
        dropCapEnabled: Boolean,
        dropCapLines: Int,
        asianGridEnabled: Boolean,
        hangingPunctuation: Boolean
    ): ParaPortionSummary {
        val scriptAnalysis = analyzeScript(text)
        val paragraphs = text.split("\n")
        
        // Base line height in twips (1 pt = 20 twips)
        val baseFontHeightTwips = fontSizePt * 20
        val lineSpacingTwips = (baseFontHeightTwips * lineSpacingFactor).toInt()
        val ascentTwips = (lineSpacingTwips * 0.75f).toInt()
        val descentTwips = lineSpacingTwips - ascentTwips

        val linesList = mutableListOf<LineLayoutInfo>()
        var globalCharOffset = 0
        var lineIndex = 1
        var totalPortions = 0

        paragraphs.forEachIndexed { pIdx, paragraph ->
            val pLen = paragraph.length
            val charsPerLine = maxOf(20, 60 - (fontSizePt / 2))
            val approxLines = maxOf(1, (pLen + charsPerLine - 1) / charsPerLine)

            var lineStart = 0
            for (i in 0 until approxLines) {
                val len = if (i == approxLines - 1) (pLen - lineStart) else minOf(charsPerLine, pLen - lineStart)
                val snippet = if (len > 0) paragraph.substring(lineStart, lineStart + len) else ""
                
                // Portions per line: attribute changes + script changes + kerning
                val scriptChangeCountInLine = scriptAnalysis.scriptChanges.count { 
                    it >= (globalCharOffset + lineStart) && it < (globalCharOffset + lineStart + len) 
                }
                val linePortions = 1 + scriptChangeCountInLine + (if (dropCapEnabled && lineIndex <= dropCapLines) 1 else 0)
                totalPortions += linePortions

                linesList.add(
                    LineLayoutInfo(
                        lineNumber = lineIndex++,
                        startCharIdx = globalCharOffset + lineStart,
                        lengthChars = len,
                        heightTwips = lineSpacingTwips,
                        ascentTwips = ascentTwips,
                        descentTwips = descentTwips,
                        realHeightTwips = (lineSpacingTwips * 1.1f).toInt(),
                        portionCount = linePortions,
                        textSnippet = snippet
                    )
                )
                lineStart += len
            }
            globalCharOffset += pLen + 1 // including newline
        }

        // Initial character for Drop Cap if enabled
        val firstChar = text.trimStart().firstOrNull()?.toString() ?: "A"
        val dropCapInfo = DropCapInfo(
            enabled = dropCapEnabled,
            linesCount = dropCapLines,
            initialChar = firstChar,
            dropHeightTwips = lineSpacingTwips * dropCapLines,
            fontScaleFactor = dropCapLines * 0.9f,
            yOffsetTwips = (lineSpacingTwips * 0.1f).toInt()
        )

        return ParaPortionSummary(
            paragraphIndex = 0,
            totalLength = text.length,
            activeStyleName = styleName,
            lines = linesList,
            totalPortions = totalPortions,
            scriptAnalysis = scriptAnalysis,
            dropCap = dropCapInfo,
            lineSpacingFactor = lineSpacingFactor,
            lineSpacingTwips = lineSpacingTwips,
            asianGridEnabled = asianGridEnabled,
            hangingPunctuation = hangingPunctuation
        )
    }
}
