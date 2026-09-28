package com.makerandreas.papirusoffice.data

import java.util.Locale
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
    val endOffset: Int = 0,
    /** Horizontal line origin relative to the paragraph's body-width box. */
    val left: Float = 0f,
    /** Source UTF-16 caret boundaries and their measured positions, relative to left. */
    val caretOffsets: List<Int> = emptyList(),
    val caretAdvances: List<Float> = emptyList(),
    val discretionaryHyphen: Boolean = false
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
        val defaultStyle = styles.defaultParagraphStyle
            ?: styles.paragraphStyles["Normal"]
            ?: styles.paragraphStyles["normal"]
            ?: styles.paragraphStyles["Standard"]
            ?: styles.paragraphStyles["standard"]
            ?: styles.paragraphStyles["Default"]
            ?: styles.paragraphStyles.values.firstOrNull {
                it.name.equals("Normal", ignoreCase = true) || it.name.equals("Standard", ignoreCase = true)
            }

        val fallbackSize = defaultStyle?.fontSizeSp ?: 12f
        val fallbackFont = defaultStyle?.fontFamily

        if (styleName.isNullOrBlank()) {
            return defaultStyle ?: ParagraphStyle("Default", fontSizeSp = 12f)
        }
        val style = styles.paragraphStyles[styleName] ?: styles.paragraphStyles[styleName.lowercase(Locale.ROOT)]
        if (style != null) return style

        val headingLevel = com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog.headingLevelFromStyleName(styleName)
        if (headingLevel > 0) {
            return ParagraphStyle(
                name = styleName,
                fontSizeSp = fallbackSize,
                isBold = true,
                fontFamily = fallbackFont,
                parentStyleName = defaultStyle?.name
            )
        }

        return when {
            styleName.contains("Title", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = fallbackSize * 1.5f, isBold = true, fontFamily = fallbackFont)
            styleName.contains("Subtitle", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = fallbackSize * 1.2f, isItalic = true, fontFamily = fallbackFont)
            styleName.contains("Quote", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = fallbackSize, isItalic = true, colorHex = "#555555", fontFamily = fallbackFont)
            styleName.contains("Caption", ignoreCase = true) -> ParagraphStyle(styleName, fontSizeSp = (fallbackSize - 1f).coerceAtLeast(8f), colorHex = "#777777", fontFamily = fallbackFont)
            else -> defaultStyle?.copy(name = styleName) ?: ParagraphStyle(styleName, fontSizeSp = fallbackSize, fontFamily = fallbackFont)
        }
    }

    fun resolveCharacterStyle(styleName: String?, styles: DocumentStyles): CharacterStyle {
        val fallbackSize = styles.defaultParagraphStyle?.fontSizeSp ?: 12f
        if (styleName == null) return CharacterStyle("Default", fontSizeSp = fallbackSize)
        val style = styles.characterStyles[styleName] ?: styles.characterStyles[styleName.lowercase(Locale.ROOT)]
        if (style != null) return style
        return CharacterStyle(styleName, fontSizeSp = fallbackSize)
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
    private val hyphenator: HyphenationEngine? = null,
    private val advanceSource: AdvanceSource = TextMetrics.defaultSource()
) {
    private val pageWidthDp: Float = pageSpec.widthDp
    private val pageHeightDp: Float = pageSpec.heightDp

    // A bounded one-entry-per-element cache. Index alone is never an identity.
    private data class CacheKey(val paragraph: OfficeParagraph, val style: ParagraphStyle,
        val characterStyles: Map<String, CharacterStyle>, val width: Float)
    private val paragraphLayoutCache = mutableMapOf<Int, Pair<CacheKey, ParagraphLayout>>()
    var measuredParagraphCount: Int = 0
        private set

    fun measurementProbe(): String {
        val metrics = TextMetrics.forStyle(ParagraphStyle("probe", fontSizeSp = 14f), advanceSource)
        return "${metrics.sourceName} at ${metrics.fontSizeUnits} units for 14 pt: " +
            "MMMM=${metrics.widthOf("MMMM")} iiii=${metrics.widthOf("iiii")} (${metrics.choice.generic})"
    }

    fun clearCache() { paragraphLayoutCache.clear() }

    fun layoutParagraph(
        paragraphIndex: Int,
        paragraph: OfficeParagraph,
        styles: DocumentStyles,
        forceRebuild: Boolean = false
    ): ParagraphLayout {
        val style = StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
        val key = CacheKey(paragraph.copy(runs = paragraph.runs.toList()), style.copy(tabStops = style.tabStops.toList()),
            styles.characterStyles.toMap(), pageSpec.contentWidthDp)
        if (!forceRebuild) paragraphLayoutCache[paragraphIndex]?.takeIf { it.first == key }?.let { return it.second }
        val result = ParagraphMeasurer(advanceSource, hyphenator).measure(
            paragraphIndex, paragraph, style, styles, pageSpec.contentWidthDp)
        measuredParagraphCount++
        paragraphLayoutCache[paragraphIndex] = key to result
        return result
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
        var currentY = pageSpec.bodyTopDp
        val maxUsableHeight = pageSpec.bodyBottomDp
        val elementPageIndex = mutableMapOf<Int, Int>()

        fun flushPage() {
            if (currentPageElements.isNotEmpty()) {
                pages.add(PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, currentPageElements))
                currentPageElements = mutableListOf()
                currentY = pageSpec.bodyTopDp
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
            currentY += h
        }

        var previousAfter = 0f
        var previousCollapses = false
        fun placeParagraph(index: Int, element: OfficeElement, paragraph: OfficeParagraph) {
            val style = StyleResolver.resolveParagraphStyle(paragraph.styleName, document.styles)
            if (style.pageBreakBefore && currentPageElements.isNotEmpty()) flushPage()
            val layout = layoutParagraph(index, paragraph, document.styles, forceRebuildAll)
            val before = style.spaceBeforeUnits.coerceAtLeast(0f)
            var gap = if (currentPageElements.isEmpty()) 0f else
                if (style.collapseSpacing || previousCollapses) maxOf(previousAfter, before) else previousAfter + before
            if (currentY + gap + layout.height > maxUsableHeight && currentPageElements.isNotEmpty()) {
                flushPage()
                gap = 0f
            }
            currentY += gap
            place(index, element, layout.height, layout.width, layout)
            previousAfter = style.spaceAfterUnits.coerceAtLeast(0f)
            previousCollapses = style.collapseSpacing
        }
        val rawElements = document.body.elements
        paragraphLayoutCache.keys.retainAll(rawElements.indices.toSet())

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
                is OfficeParagraph -> placeParagraph(index, element, element)
                is OfficeHeading -> placeParagraph(index, element, OfficeParagraph(
                    text = element.text, styleName = element.styleName ?: "Heading ${element.level}", runs = element.runs))
                is OfficeListItem -> placeParagraph(index, element, OfficeParagraph(
                    text = "${element.bullet}${element.text}"))
                is OfficeDocElement.ParagraphElement -> placeParagraph(index, element, element.paragraph)
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
                if (elemLayout.paragraphLayout != null) {
                    val pLayout = elemLayout.paragraphLayout
                    var lineY = b.top
                    for (lineIdx in pLayout.lines.indices) {
                        val line = pLayout.lines[lineIdx]
                        if (relativeY >= lineY && relativeY <= lineY + line.height) {
                            val lineRelativeX = x - b.left - line.left
                            val caret = line.caretAdvances.indices.minByOrNull {
                                kotlin.math.abs(line.caretAdvances[it] - lineRelativeX)
                            } ?: 0
                            val totalOffset = line.caretOffsets.getOrElse(caret) { line.startOffset }

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
