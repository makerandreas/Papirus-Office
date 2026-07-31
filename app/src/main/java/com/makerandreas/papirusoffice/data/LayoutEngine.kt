package com.makerandreas.papirusoffice.data

import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import kotlin.math.max
import kotlin.math.min

// ==========================================
// PHASE 1 & 2: Paragraph & Line Layout Models
// ==========================================

data class LineLayout(
    val text: String,
    val runs: List<OfficeTextRun> = emptyList(),
    val width: Float = 0f,
    val height: Float = 0f,
    val baseline: Float = 0f,
    val startOffset: Int = 0,
    val endOffset: Int = 0
)

data class ParagraphLayout(
    val paragraphIndex: Int,
    val lines: List<LineLayout> = emptyList(),
    val width: Float = 0f,
    val height: Float = 0f,
    val baseline: Float = 0f,
    val boundingBox: OfficeRect = OfficeRect()
)

// ==========================================
// PHASE 5: Pagination Models
// ==========================================

data class PageLayout(
    val pageNumber: Int,
    val widthDp: Float = 816f,  // standard A4 or Letter ratio approx
    val heightDp: Float = 1056f,
    val elements: List<PageElementLayout> = emptyList()
)

data class PageElementLayout(
    val element: OfficeElement,
    val paragraphLayout: ParagraphLayout? = null,
    val bounds: OfficeRect = OfficeRect() // Positioned bounds relative to page top-left
)

data class DocumentLayoutResult(
    val pages: List<PageLayout> = emptyList(),
    val totalHeightDp: Float = 0f
)

// ==========================================
// PHASE 6: Style Resolver
// ==========================================

object StyleResolver {
    fun resolveParagraphStyle(styleName: String?, styles: DocumentStyles): ParagraphStyle {
        if (styleName == null) return ParagraphStyle("Default", fontSizeSp = 14f)
        val style = styles.paragraphStyles[styleName]
        if (style != null) return style

        // Fallback cascades
        return when {
            styleName.contains("Heading 1", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 24f, isBold = true)
            styleName.contains("Heading 2", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 20f, isBold = true)
            styleName.contains("Heading 3", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 16f, isBold = true)
            styleName.contains("Heading", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 18f, isBold = true)
            styleName.contains("Title", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 28f, isBold = true)
            styleName.contains("Subtitle", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 18f, isItalic = true)
            styleName.contains("Quote", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 14f, isItalic = true, colorHex = "#555555")
            styleName.contains("Caption", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = 11f, colorHex = "#777777")
            else -> ParagraphStyle(styleName, fontSizeSp = 14f)
        }
    }

    fun resolveCharacterStyle(styleName: String?, styles: DocumentStyles): CharacterStyle {
        if (styleName == null) return CharacterStyle("Default", fontSizeSp = 14f)
        val style = styles.characterStyles[styleName]
        if (style != null) return style
        return CharacterStyle(styleName, fontSizeSp = 14f)
    }
}

// ==========================================
// PHASE 7: Layout Engine & Incremental Layout
// ==========================================

class LayoutEngine(private val pageWidthDp: Float = 816f, private val pageHeightDp: Float = 1056f) {
    // Cache map for Incremental Layout: paragraph index to its paragraph layout
    private val paragraphLayoutCache = mutableMapOf<Int, ParagraphLayout>()

    // Simple text measurement system using default Android system sizes scaled
    private val textPaint = Paint().apply {
        isAntiAlias = true
    }

    fun clearCache() {
        paragraphLayoutCache.clear()
    }

    /**
     * Compute paragraph layout. Uses caching for incremental updates.
     */
    fun layoutParagraph(
        paragraphIndex: Int,
        paragraph: OfficeParagraph,
        styles: DocumentStyles,
        forceRebuild: Boolean = false
    ): ParagraphLayout {
        if (!forceRebuild && paragraphLayoutCache.containsKey(paragraphIndex)) {
            return paragraphLayoutCache[paragraphIndex]!!
        }

        val style = StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
        textPaint.textSize = style.fontSizeSp * 2.5f // rough dp-to-px scaling factor for virtual measuring

        val words = paragraph.text.split(" ")
        val lines = mutableListOf<LineLayout>()
        var currentLineText = StringBuilder()
        var currentLineWidth = 0f
        var startCharOffset = 0

        val maxLineWidth = pageWidthDp * 2.0f // Measure boundary

        for (word in words) {
            val spaceText = if (currentLineText.isNotEmpty()) " " else ""
            val testWord = spaceText + word
            val wordWidth = textPaint.measureText(testWord)

            if (currentLineWidth + wordWidth > maxLineWidth && currentLineText.isNotEmpty()) {
                val lineStr = currentLineText.toString()
                lines.add(
                    LineLayout(
                        text = lineStr,
                        runs = paragraph.runs,
                        width = currentLineWidth,
                        height = textPaint.textSize * 1.2f,
                        baseline = textPaint.textSize,
                        startOffset = startCharOffset,
                        endOffset = startCharOffset + lineStr.length
                    )
                )
                startCharOffset += lineStr.length + 1
                currentLineText = StringBuilder(word)
                currentLineWidth = textPaint.measureText(word)
            } else {
                currentLineText.append(testWord)
                currentLineWidth += wordWidth
            }
        }

        if (currentLineText.isNotEmpty()) {
            val lineStr = currentLineText.toString()
            lines.add(
                LineLayout(
                    text = lineStr,
                    runs = paragraph.runs,
                    width = currentLineWidth,
                    height = textPaint.textSize * 1.2f,
                    baseline = textPaint.textSize,
                    startOffset = startCharOffset,
                    endOffset = startCharOffset + lineStr.length
                )
            )
        }

        // Calculate total height of lines
        var totalHeight = 0f
        for (line in lines) {
            totalHeight += line.height
        }

        val layout = ParagraphLayout(
            paragraphIndex = paragraphIndex,
            lines = lines,
            width = pageWidthDp - 80f, // minus margin
            height = max(totalHeight, textPaint.textSize * 1.5f),
            boundingBox = OfficeRect(0f, 0f, pageWidthDp - 80f, totalHeight)
        )

        paragraphLayoutCache[paragraphIndex] = layout
        return layout
    }

    /**
     * PAGINATION ENGINE (Phase 5)
     * Fits elements nicely across multiple pages
     */
    fun performLayout(document: OfficeDocument, forceRebuildAll: Boolean = false): DocumentLayoutResult {
        if (forceRebuildAll) {
            clearCache()
        }

        val pages = mutableListOf<PageLayout>()
        var currentPageElements = mutableListOf<PageElementLayout>()
        var currentY = 50f // top margin
        val marginX = 40f
        val maxUsableHeight = pageHeightDp - 60f // reserve space for headers/footers

        val rawElements = document.body.elements

        rawElements.forEachIndexed { index, element ->
            when (element) {
                is OfficeDocElement.ParagraphElement -> {
                    val pLayout = layoutParagraph(index, element.paragraph, document.styles, forceRebuildAll)
                    if (currentY + pLayout.height > maxUsableHeight && currentPageElements.isNotEmpty()) {
                        // Push current page
                        pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
                        currentPageElements = mutableListOf()
                        currentY = 50f
                    }

                    currentPageElements.add(
                        PageElementLayout(
                            element = element,
                            paragraphLayout = pLayout,
                            bounds = OfficeRect(marginX, currentY, marginX + pLayout.width, currentY + pLayout.height)
                        )
                    )
                    currentY += pLayout.height + 12f // spacer between paragraphs
                }
                is OfficeDocElement.TableElement -> {
                    // Approximate table height
                    val tableHeight = element.table.rows.size * 35f + 10f
                    if (currentY + tableHeight > maxUsableHeight && currentPageElements.isNotEmpty()) {
                        pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
                        currentPageElements = mutableListOf()
                        currentY = 50f
                    }

                    currentPageElements.add(
                        PageElementLayout(
                            element = element,
                            bounds = OfficeRect(marginX, currentY, pageWidthDp - marginX, currentY + tableHeight)
                        )
                    )
                    currentY += tableHeight + 16f
                }
                is OfficeDocElement.ImageElement -> {
                    val imgHeight = if (element.image.heightDp > 0) element.image.heightDp else 200f
                    if (currentY + imgHeight > maxUsableHeight && currentPageElements.isNotEmpty()) {
                        pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
                        currentPageElements = mutableListOf()
                        currentY = 50f
                    }

                    currentPageElements.add(
                        PageElementLayout(
                            element = element,
                            bounds = OfficeRect(marginX, currentY, marginX + (if (element.image.widthDp > 0) element.image.widthDp else 250f), currentY + imgHeight)
                        )
                    )
                    currentY += imgHeight + 16f
                }
                else -> {
                    // Fallback element size
                    val itemHeight = 30f
                    if (currentY + itemHeight > maxUsableHeight && currentPageElements.isNotEmpty()) {
                        pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
                        currentPageElements = mutableListOf()
                        currentY = 50f
                    }

                    currentPageElements.add(
                        PageElementLayout(
                            element = element,
                            bounds = OfficeRect(marginX, currentY, pageWidthDp - marginX, currentY + itemHeight)
                        )
                    )
                    currentY += itemHeight + 10f
                }
            }
        }

        if (currentPageElements.isNotEmpty()) {
            pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
        }

        return DocumentLayoutResult(pages, totalHeightDp = pages.size * pageHeightDp)
    }

    // ==========================================
// PHASE 3: Hit Testing
// ==========================================
    fun hitTest(x: Float, y: Float, pages: List<PageLayout>): HitTestResult? {
        val pageIndex = (y / pageHeightDp).toInt()
        if (pageIndex < 0 || pageIndex >= pages.size) return null

        val targetPage = pages[pageIndex]
        val relativeY = y % pageHeightDp

        // Search elements inside page
        for (elemLayout in targetPage.elements) {
            val b = elemLayout.bounds
            if (relativeY >= b.top && relativeY <= b.bottom && x >= b.left && x <= b.right) {
                val element = elemLayout.element
                if (element is OfficeDocElement.ParagraphElement && elemLayout.paragraphLayout != null) {
                    val pLayout = elemLayout.paragraphLayout
                    var lineY = b.top
                    for (lineIdx in pLayout.lines.indices) {
                        val line = pLayout.lines[lineIdx]
                        if (relativeY >= lineY && relativeY <= lineY + line.height) {
                            // Hit this line! Find character offset inside line
                            val lineRelativeX = x - b.left
                            val charRatio = if (line.width > 0) lineRelativeX / line.width else 0f
                            val approxCharOffsetInLine = (line.text.length * charRatio).toInt().coerceIn(0, line.text.length)
                            val totalOffset = line.startOffset + approxCharOffsetInLine

                            return HitTestResult(
                                pageIndex = pageIndex,
                                elementIndex = pLayout.paragraphIndex,
                                paragraphIndex = pLayout.paragraphIndex,
                                lineIndex = lineIdx,
                                characterOffset = totalOffset
                            )
                        }
                        lineY += line.height
                    }
                }
            }
        }
        return null
    }
}

data class HitTestResult(
    val pageIndex: Int,
    val elementIndex: Int,
    val paragraphIndex: Int,
    val lineIndex: Int,
    val characterOffset: Int
)

// ==========================================
// PHASE 4: Caret Engine
// ==========================================

object CaretEngine {
    fun moveLeft(document: OfficeDocument, cursor: DocumentCursor, layoutResult: DocumentLayoutResult): DocumentCursor {
        val elements = document.body.elements
        val pIdx = cursor.paragraphIndex
        if (pIdx < 0 || pIdx >= elements.size) return cursor

        val element = elements[pIdx]
        if (element is OfficeDocElement.ParagraphElement) {
            val text = element.paragraph.text
            if (cursor.offset > 0) {
                return cursor.copy(offset = cursor.offset - 1)
            } else if (pIdx > 0) {
                // Move to end of previous paragraph
                val prevElement = elements[pIdx - 1]
                if (prevElement is OfficeDocElement.ParagraphElement) {
                    return cursor.copy(
                        paragraphIndex = pIdx - 1,
                        elementIndex = pIdx - 1,
                        offset = prevElement.paragraph.text.length
                    )
                }
            }
        }
        return cursor
    }

    fun moveRight(document: OfficeDocument, cursor: DocumentCursor, layoutResult: DocumentLayoutResult): DocumentCursor {
        val elements = document.body.elements
        val pIdx = cursor.paragraphIndex
        if (pIdx < 0 || pIdx >= elements.size) return cursor

        val element = elements[pIdx]
        if (element is OfficeDocElement.ParagraphElement) {
            val text = element.paragraph.text
            if (cursor.offset < text.length) {
                return cursor.copy(offset = cursor.offset + 1)
            } else if (pIdx < elements.size - 1) {
                // Move to start of next paragraph
                return cursor.copy(
                    paragraphIndex = pIdx + 1,
                    elementIndex = pIdx + 1,
                    offset = 0
                )
            }
        }
        return cursor
    }

    fun moveUp(document: OfficeDocument, cursor: DocumentCursor, layoutResult: DocumentLayoutResult): DocumentCursor {
        // Simple multiline layout vertical traversal: move back up a line or back a paragraph
        if (cursor.paragraphIndex > 0) {
            return cursor.copy(
                paragraphIndex = cursor.paragraphIndex - 1,
                elementIndex = cursor.paragraphIndex - 1,
                offset = 0
            )
        }
        return cursor
    }

    fun moveDown(document: OfficeDocument, cursor: DocumentCursor, layoutResult: DocumentLayoutResult): DocumentCursor {
        val elements = document.body.elements
        if (cursor.paragraphIndex < elements.size - 1) {
            return cursor.copy(
                paragraphIndex = cursor.paragraphIndex + 1,
                elementIndex = cursor.paragraphIndex + 1,
                offset = 0
            )
        }
        return cursor
    }

    fun home(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        return cursor.copy(offset = 0)
    }

    fun end(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        val elements = document.body.elements
        val pIdx = cursor.paragraphIndex
        if (pIdx >= 0 && pIdx < elements.size) {
            val element = elements[pIdx]
            if (element is OfficeDocElement.ParagraphElement) {
                return cursor.copy(offset = element.paragraph.text.length)
            }
        }
        return cursor
    }
}
