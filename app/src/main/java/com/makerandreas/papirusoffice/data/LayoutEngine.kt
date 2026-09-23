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
    val bounds: OfficeRect = OfficeRect(), // Positioned bounds relative to page top-left
    val elementIndex: Int = 0
)

data class DocumentLayoutResult(
    val pages: List<PageLayout> = emptyList(),
    val totalHeightDp: Float = 0f,
    val elementPageIndex: Map<Int, Int> = emptyMap()
)

// ==========================================
// PHASE 6: Style Resolver
// ==========================================

object StyleResolver {
    fun resolveParagraphStyle(styleName: String?, styles: DocumentStyles): ParagraphStyle {
        if (styleName == null) return ParagraphStyle("Default", fontSizeSp = 14f)
        val style = styles.paragraphStyles[styleName]
        if (style != null) return style

        // 24/20/16 only if the named style is absent. Mapped file sizes win.
        val headingLevel = com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog.headingLevelFromStyleName(styleName)
        return when {
            headingLevel == 1 -> ParagraphStyle(styleName, fontSizeSp = 24f, isBold = true)
            headingLevel == 2 -> ParagraphStyle(styleName, fontSizeSp = 20f, isBold = true)
            headingLevel == 3 -> ParagraphStyle(styleName, fontSizeSp = 16f, isBold = true)
            headingLevel > 0 -> ParagraphStyle(styleName, fontSizeSp = 18f, isBold = true)
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

/**
 * Sole paginator for Viewer and Editor. Works in the PageStyleSpec unit space
 * (layout units at 96/inch); the default spec reproduces the historical
 * Letter box for documents that declare no page geometry. [hyphenator] is
 * null by default, which keeps the historical word-only wrapping.
 */
class LayoutEngine(
    private val pageSpec: PageStyleSpec = PageStyleSpec.FALLBACK,
    private val hyphenator: HyphenationEngine? = null
) {
    private val pageWidthDp: Float = pageSpec.widthDp
    private val pageHeightDp: Float = pageSpec.heightDp

    // Vertical rhythm between stacked elements on the page flow.
    private val elementGapDp: Float = 12f

    // Cache map for Incremental Layout: paragraph index to its paragraph layout
    private val paragraphLayoutCache = mutableMapOf<Int, ParagraphLayout>()

    // Simple text measurement system using default Android system sizes scaled
    private val textPaint: Paint? by lazy {
        try {
            Paint().apply { isAntiAlias = true }
        } catch (e: Throwable) {
            null
        }
    }

    private var fallbackTextSize: Float = 14f * 2.5f

    private fun getPaintTextSize(): Float {
        return textPaint?.textSize ?: fallbackTextSize
    }

    private fun setPaintTextSize(size: Float) {
        textPaint?.let { it.textSize = size }
        fallbackTextSize = size
    }

    private fun measureTextWidth(text: String): Float {
        return textPaint?.measureText(text) ?: (text.length * 8.0f)
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
        if (!forceRebuild) {
            paragraphLayoutCache[paragraphIndex]?.let { return it }
        }

        val style = StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
        setPaintTextSize(style.fontSizeSp * 2.5f) // rough dp-to-px scaling factor for virtual measuring

        val words = paragraph.text.split(" ")
        val lines = mutableListOf<LineLayout>()
        var currentLineText = StringBuilder()
        var currentLineWidth = 0f
        var startCharOffset = 0

        val maxLineWidth = pageSpec.contentWidthDp

        for (w in words) {
            var word = w
            while (true) {
                val spaceText = if (currentLineText.isNotEmpty()) " " else ""
                val testWord = spaceText + word
                val wordWidth = measureTextWidth(testWord)
                val overflows = currentLineWidth + wordWidth > maxLineWidth

                if (overflows && currentLineText.isNotEmpty()) {
                    // A word that does not fit: try a dictionary break before
                    // pushing it whole onto the next line (off by default).
                    var broke = false
                    if (hyphenator != null) {
                        val spaceWidth = measureTextWidth(spaceText)
                        val remaining = (maxLineWidth - currentLineWidth - spaceWidth).coerceAtLeast(0f)
                        val breakAt = hyphenator.firstFittingBreak(word, remaining, ::measureTextWidth)
                        if (breakAt != null) {
                            val head = word.substring(0, breakAt)
                            val lineStr = currentLineText.toString() + spaceText + head
                            lines.add(
                                LineLayout(
                                    text = lineStr,
                                    runs = paragraph.runs,
                                    width = currentLineWidth + spaceWidth + measureTextWidth(head),
                                    height = getPaintTextSize() * 1.2f,
                                    baseline = getPaintTextSize(),
                                    startOffset = startCharOffset,
                                    endOffset = startCharOffset + lineStr.length
                                )
                            )
                            startCharOffset += lineStr.length + 1
                            word = word.substring(breakAt)
                            currentLineText = StringBuilder()
                            currentLineWidth = 0f
                            broke = true
                        }
                    }
                    if (broke) {
                        if (word.isEmpty()) break
                        continue
                    }
                    val lineStr = currentLineText.toString()
                    lines.add(
                        LineLayout(
                            text = lineStr,
                            runs = paragraph.runs,
                            width = currentLineWidth,
                            height = getPaintTextSize() * 1.2f,
                            baseline = getPaintTextSize(),
                            startOffset = startCharOffset,
                            endOffset = startCharOffset + lineStr.length
                        )
                    )
                    startCharOffset += lineStr.length + 1
                    currentLineText = StringBuilder(word)
                    currentLineWidth = measureTextWidth(word)
                    break
                }
                if (overflows && currentLineText.isEmpty() && hyphenator != null) {
                    val breakAt = hyphenator.firstFittingBreak(word, maxLineWidth, ::measureTextWidth)
                    if (breakAt != null && breakAt < word.length) {
                        val head = word.substring(0, breakAt)
                        lines.add(
                            LineLayout(
                                text = head,
                                runs = paragraph.runs,
                                width = measureTextWidth(head),
                                height = getPaintTextSize() * 1.2f,
                                baseline = getPaintTextSize(),
                                startOffset = startCharOffset,
                                endOffset = startCharOffset + head.length
                            )
                        )
                        startCharOffset += head.length
                        word = word.substring(breakAt)
                        continue
                    }
                }
                currentLineText.append(testWord)
                currentLineWidth += wordWidth
                break
            }
        }

        if (currentLineText.isNotEmpty()) {
            val lineStr = currentLineText.toString()
            lines.add(
                LineLayout(
                    text = lineStr,
                    runs = paragraph.runs,
                    width = currentLineWidth,
                    height = getPaintTextSize() * 1.2f,
                    baseline = getPaintTextSize(),
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
            width = maxLineWidth,
            height = max(totalHeight, getPaintTextSize() * 1.5f),
            boundingBox = OfficeRect(0f, 0f, maxLineWidth, totalHeight)
        )

        paragraphLayoutCache[paragraphIndex] = layout
        return layout
    }

    /**
     * PAGINATION ENGINE (Phase 5)
     * Fits elements nicely across multiple pages
     */
    fun performLayout(
        document: OfficeDocument,
        outlineEngine: OutlineEngine? = null,
        showImages: Boolean = true,
        showTables: Boolean = true,
        forceRebuildAll: Boolean = false
    ): DocumentLayoutResult {
        if (forceRebuildAll) {
            clearCache()
        }

        val pages = mutableListOf<PageLayout>()
        var currentPageElements = mutableListOf<PageElementLayout>()
        var currentY = pageSpec.marginTopDp
        val maxUsableHeight = pageSpec.contentBottomDp
        val elementPageIndex = mutableMapOf<Int, Int>()

        fun flushPage() {
            if (currentPageElements.isNotEmpty()) {
                pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
                currentPageElements = mutableListOf()
                currentY = pageSpec.marginTopDp
            }
        }

        fun ensureRoom(needed: Float) {
            if (currentY + needed > maxUsableHeight && currentPageElements.isNotEmpty()) {
                flushPage()
            }
        }

        fun place(index: Int, element: OfficeElement, height: Float, width: Float = pageSpec.contentWidthDp, paragraphLayout: ParagraphLayout? = null) {
            val h = height.coerceAtLeast(8f)
            ensureRoom(h)
            val pageNumber = pages.size + 1
            elementPageIndex[index] = pageNumber
            val left = pageSpec.marginStartDp
            val placedWidth = width.coerceAtMost(pageSpec.contentWidthDp)
            currentPageElements.add(
                PageElementLayout(
                    element = element,
                    paragraphLayout = paragraphLayout,
                    bounds = OfficeRect(left, currentY, left + placedWidth, currentY + h),
                    elementIndex = index
                )
            )
            currentY += h + elementGapDp
        }

        val rawElements = document.body.elements

        rawElements.forEachIndexed { index, element ->
            if (outlineEngine != null && outlineEngine.isElementHidden(index)) {
                return@forEachIndexed
            }
            if (!showImages && (element is OfficeDocElement.ImageElement || element is OfficeImage)) {
                return@forEachIndexed
            }
            if (!showTables && (element is OfficeDocElement.TableElement || element is OfficeTable)) {
                return@forEachIndexed
            }

            when (element) {
                is OfficePageBreak -> {
                    elementPageIndex[index] = pages.size + 1
                    flushPage()
                    if (pages.isEmpty()) {
                        pages.add(PageLayout(1, pageWidthDp, pageHeightDp, emptyList()))
                    }
                }
                is OfficeParagraph -> {
                    val pLayout = layoutParagraph(index, element, document.styles, forceRebuildAll)
                    place(index, element, pLayout.height, pLayout.width, pLayout)
                }
                is OfficeHeading -> {
                    val asPara = OfficeParagraph(
                        text = element.text,
                        styleName = element.styleName ?: "Heading ${element.level}",
                        runs = element.runs
                    )
                    val pLayout = layoutParagraph(index, asPara, document.styles, forceRebuildAll)
                    place(index, element, pLayout.height, pLayout.width, pLayout)
                }
                is OfficeListItem -> {
                    val asPara = OfficeParagraph(text = "${element.bullet}${element.text}", runs = element.runs)
                    val pLayout = layoutParagraph(index, asPara, document.styles, forceRebuildAll)
                    place(index, element, pLayout.height, pLayout.width, pLayout)
                }
                is OfficeDocElement.ParagraphElement -> {
                    val pLayout = layoutParagraph(index, element.paragraph, document.styles, forceRebuildAll)
                    place(index, element, pLayout.height, pLayout.width, pLayout)
                }
                is OfficeTable -> {
                    val tableHeight = (element.rows.size * 35f + 10f).coerceAtLeast(40f)
                    place(index, element, tableHeight)
                }
                is OfficeDocElement.TableElement -> {
                    val tableHeight = (element.table.rows.size * 35f + 10f).coerceAtLeast(40f)
                    place(index, element, tableHeight)
                }
                is OfficeImage -> {
                    val imgHeight = if (element.heightDp > 0) element.heightDp else 180f
                    val imgWidth = if (element.widthDp > 0) element.widthDp else pageSpec.contentWidthDp
                    place(index, element, imgHeight, imgWidth)
                }
                is OfficeDocElement.ImageElement -> {
                    val imgHeight = if (element.image.heightDp > 0) element.image.heightDp else 180f
                    val imgWidth = if (element.image.widthDp > 0) element.image.widthDp else pageSpec.contentWidthDp
                    place(index, element, imgHeight, imgWidth)
                }
                else -> {
                    place(index, element, 30f)
                }
            }
        }

        if (currentPageElements.isNotEmpty()) {
            pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
        }
        if (pages.isEmpty()) {
            pages.add(PageLayout(1, pageWidthDp, pageHeightDp, emptyList()))
        }

        return DocumentLayoutResult(
            pages = pages,
            totalHeightDp = pages.size * pageHeightDp,
            elementPageIndex = elementPageIndex
        )
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
