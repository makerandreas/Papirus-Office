package com.makerandreas.papirusoffice.data

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// ==========================================
// PHASE 1 & 2: Paragraph & Line Layout Models
// ==========================================

data class TabLeaderLayout(
    /** Source offset of the tab whose already-reserved gap receives this text. */
    val sourceOffset: Int,
    /** Character requested by `style:leader-text`. */
    val requestedText: String,
    /** Repeated display text; may use the supported-character fallback. */
    val renderedText: String,
    /** Width reserved for [renderedText] within the tab's measured advance. */
    val widthUnits: Float,
    val fallbackApplied: Boolean = false
)

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
    val discretionaryHyphen: Boolean = false,
    /** Display-only text leaders; source text and measured tab advances are unchanged. */
    val tabLeaders: List<TabLeaderLayout> = emptyList()
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

enum class PageEndReason { END, OVERFLOW, AUTHORED, KEEP }

data class PageLayout(
    val pageNumber: Int,
    val widthDp: Float = 816f,  // standard A4 or Letter ratio approx
    val heightDp: Float = 1056f,
    val elements: List<PageElementLayout> = emptyList(),
    val endReason: PageEndReason = PageEndReason.END
)

data class PageElementLayout(
    val element: OfficeElement,
    val paragraphLayout: ParagraphLayout? = null,
    val bounds: OfficeRect = OfficeRect(), // Positioned bounds relative to page top-left
    val elementIndex: Int = 0,
    val firstLineIndex: Int = 0,
    val sourceStart: Int = 0,
    val sourceEnd: Int = 0,
    val continuesBefore: Boolean = false,
    val continuesAfter: Boolean = false,
    val tableFragment: TableFragmentGeometry? = null
)

data class DocumentLayoutResult(
    val pages: List<PageLayout> = emptyList(),
    val totalHeightDp: Float = 0f,
    val elementPageIndex: Map<Int, Int> = emptyMap(),
    val elementPages: Map<Int, List<Int>> = emptyMap()
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
        val baseStyle = StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
        val style = baseStyle.copy(indentStartUnits = baseStyle.indentStartUnits + paragraph.indent)
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
        var placed = mutableListOf<PageElementLayout>()
        var y = pageSpec.bodyTopDp
        val top = pageSpec.bodyTopDp
        val bottom = pageSpec.bodyBottomDp
        val bodyHeight = (bottom - top).coerceAtLeast(1f)
        val firstPages = mutableMapOf<Int, Int>()
        val elementPages = mutableMapOf<Int, MutableList<Int>>()
        var previousAfter = 0f
        var previousCollapses = false
        var trailingAuthoredBreak = false
        fun flush(reason: PageEndReason, force: Boolean = false) {
            if (placed.isNotEmpty() || force) {
                pages += PageLayout(pages.size + 1, pageWidthDp, pageHeightDp, placed, reason)
                placed = mutableListOf()
            }
            y = top
            previousAfter = 0f
        }
        fun register(index: Int) {
            val page = pages.size + 1
            firstPages.putIfAbsent(index, page)
            val range = elementPages.getOrPut(index) { mutableListOf() }
            if (range.lastOrNull() != page) range += page
        }
        fun paragraph(element: OfficeElement): OfficeParagraph? = when (element) {
            is OfficeParagraph -> element
            is OfficeHeading -> OfficeParagraph(element.text, element.styleName ?: "Heading ${element.level}", runs = element.runs, pageBreakOffsets = element.pageBreakOffsets)
            is OfficeListItem -> {
                val baseStyle = StyleResolver.resolveParagraphStyle(null, document.styles)
                val labelStyle = if (element.labelFontSizeSp != null || element.labelFontFamily != null) {
                    baseStyle.copy(
                        fontSizeSp = element.labelFontSizeSp ?: baseStyle.fontSizeSp,
                        fontFamily = element.labelFontFamily ?: baseStyle.fontFamily
                    )
                } else {
                    baseStyle
                }
                OfficeParagraph(
                    text = element.text,
                    runs = element.runs,
                    indent = TextMetrics.forStyle(labelStyle, advanceSource).widthOf(element.bullet)
                )
            }
            is OfficeDocElement.ParagraphElement -> element.paragraph
            else -> null
        }
        val elements = document.body.elements
        val hiddenSectionRanges = document.namedSectionRanges
            .asSequence()
            .filter { it.display == SectionDisplay.HIDDEN && it.bodyRange.endExclusive <= elements.size }
            .map { it.bodyRange }
            .toList()
        paragraphLayoutCache.keys.retainAll(elements.indices.toSet())
        fun visible(index: Int): Boolean {
            val e = elements[index]
            return hiddenSectionRanges.none { index in it } &&
                outlineEngine?.isElementHidden(index) != true &&
                (showImages || e !is OfficeImage && e !is OfficeDocElement.ImageElement) &&
                (showTables || e !is OfficeTable && e !is OfficeDocElement.TableElement)
        }

        fun tableOf(element: OfficeElement): OfficeTable? = when (element) {
            is OfficeTable -> element
            is OfficeDocElement.TableElement -> element.table
            else -> null
        }

        fun placeTable(element: OfficeElement, index: Int, table: OfficeTable) {
            if (previousAfter > 0f) {
                y += previousAfter
                previousAfter = 0f
            }
            val plan = TableLayoutEngine.measure(
                table = table,
                styles = document.styles,
                availableWidth = pageSpec.contentWidthDp,
                advanceSource = advanceSource,
                hyphenator = hyphenator
            )
            val leadingHeaders = (0 until plan.headerRowCount).toList()
            val crossingHeaderGroup = plan.rowGroups.firstOrNull {
                it.startInclusive < plan.headerRowCount && it.endExclusive > plan.headerRowCount
            }
            val bodyGroups = plan.rowGroups.filter { group ->
                group.startInclusive >= plan.headerRowCount || crossingHeaderGroup === group
            }
            var groupCursor = 0
            var firstFragment = true
            var headersEnabled = leadingHeaders.isNotEmpty() && crossingHeaderGroup == null
            var emittedEmptyTable = false
            var pendingDiagnostics = plan.diagnostics

            while (groupCursor < bodyGroups.size || (firstFragment && leadingHeaders.isNotEmpty()) || (!emittedEmptyTable && plan.grid.rows.isEmpty())) {
                val headerRows = if (headersEnabled && (firstFragment || groupCursor < bodyGroups.size)) leadingHeaders else emptyList()
                val headerHeight = plan.heightOfRows(headerRows)
                val firstGroup = bodyGroups.getOrNull(groupCursor)
                val firstGroupHeight = firstGroup?.let(plan::heightOf) ?: 0f

                if (firstGroup != null && firstGroupHeight > bodyHeight && placed.isNotEmpty()) {
                    flush(PageEndReason.OVERFLOW)
                    continue
                }
                if (firstGroup != null && firstGroupHeight > bodyHeight && headersEnabled && headerHeight + firstGroupHeight > bodyHeight) {
                    pendingDiagnostics = pendingDiagnostics + TableDiagnostic(
                        TableDiagnosticCode.OVER_HEIGHT,
                        "An oversized table row or span group was placed alone; the repeated header was omitted"
                    )
                    headersEnabled = false
                    continue
                }
                if (firstGroup != null && y + headerHeight + firstGroupHeight > bottom + 0.001f && placed.isNotEmpty()) {
                    flush(PageEndReason.OVERFLOW)
                    continue
                }
                if (firstGroup != null && headerHeight > bodyHeight && headersEnabled) {
                    pendingDiagnostics = pendingDiagnostics + TableDiagnostic(
                        TableDiagnosticCode.OVER_HEIGHT,
                        "The table header is taller than the usable page body"
                    )
                    headersEnabled = false
                    continue
                }

                val selectedGroups = mutableListOf<TableRowGroup>()
                var selectedHeight = 0f
                while (groupCursor + selectedGroups.size < bodyGroups.size) {
                    val candidate = bodyGroups[groupCursor + selectedGroups.size]
                    val candidateHeight = plan.heightOf(candidate)
                    val candidateTotal = headerHeight + selectedHeight + candidateHeight
                    if (selectedGroups.isNotEmpty() && y + candidateTotal > bottom + 0.001f) break
                    if (selectedGroups.isEmpty() && candidateHeight > bodyHeight) {
                        // Empty-page placement is the explicit forward-progress escape hatch.
                        pendingDiagnostics = pendingDiagnostics + TableDiagnostic(
                            TableDiagnosticCode.OVER_HEIGHT,
                            "An oversized table row or vertical-span group was placed alone",
                            logicalRow = candidate.startInclusive
                        )
                    }
                    selectedGroups += candidate
                    selectedHeight += candidateHeight
                    if (candidateHeight > bodyHeight) break
                }

                if (selectedGroups.isEmpty() && leadingHeaders.isNotEmpty() && firstFragment) {
                    // A table containing only headers still gets one visible fragment.
                    emittedEmptyTable = true
                } else if (selectedGroups.isEmpty() && firstGroup == null) {
                    emittedEmptyTable = true
                }

                val bodyRows = selectedGroups.flatMap { group -> group.startInclusive until group.endExclusive }
                val rowRefs = buildList {
                    headerRows.forEach { add(TableFragmentRowRef(it, isRepeatedHeader = !firstFragment, isBodyCoverage = false)) }
                    bodyRows.forEach { add(TableFragmentRowRef(it, isRepeatedHeader = false, isBodyCoverage = true)) }
                }
                var fragment = plan.fragment(
                    elementIndex = index,
                    left = pageSpec.marginStartDp,
                    top = y,
                    rowRefs = rowRefs,
                    extraDiagnostics = pendingDiagnostics
                )
                if (fragment.bounds.bottom <= fragment.bounds.top) {
                    fragment = fragment.copy(bounds = fragment.bounds.copy(bottom = fragment.bounds.top + 1f))
                }
                register(index)
                placed += PageElementLayout(
                    element = element,
                    bounds = fragment.bounds,
                    elementIndex = index,
                    tableFragment = fragment
                )
                y = fragment.bounds.bottom
                previousAfter = 0f
                previousCollapses = false
                emittedEmptyTable = true
                groupCursor += selectedGroups.size
                firstFragment = false
                pendingDiagnostics = emptyList()
                if (groupCursor < bodyGroups.size && y >= bottom - 0.001f) {
                    flush(PageEndReason.OVERFLOW)
                }
                if (selectedGroups.isEmpty() && groupCursor >= bodyGroups.size) break
            }
        }

        for ((index, element) in elements.withIndex()) {
            if (!visible(index)) continue
            if (element is OfficePageBreak) {
                firstPages[index] = pages.size + 1
                flush(PageEndReason.AUTHORED, force = true)
                trailingAuthoredBreak = true
                continue
            }
            trailingAuthoredBreak = false
            val section = document.sectionStarts.firstOrNull { it.elementIndex == index && index > 0 }
            if (section != null && section.kind != SectionStartKind.CONTINUOUS) {
                // NEXT_COLUMN advances the single supported column; retain its kind for Plan 8B.
                flush(PageEndReason.AUTHORED)
                val nextPage = pages.size + 1
                if ((section.kind == SectionStartKind.ODD_PAGE && nextPage % 2 == 0) ||
                    (section.kind == SectionStartKind.EVEN_PAGE && nextPage % 2 != 0)) flush(PageEndReason.AUTHORED, force = true)
            }
            val p = paragraph(element)
            if (p != null) {
                val style = StyleResolver.resolveParagraphStyle(p.styleName, document.styles)
                val layout = layoutParagraph(index, p, document.styles, forceRebuildAll)
                if ((style.pageBreakBefore || !style.masterPageName.isNullOrBlank()) && placed.isNotEmpty()) flush(PageEndReason.AUTHORED)
                val before = style.spaceBeforeUnits.coerceAtLeast(0f)
                var gap = if (placed.isEmpty()) 0f else if (style.collapseSpacing || previousCollapses)
                    maxOf(previousAfter, before) else previousAfter + before
                // Bounded lookahead. An over-page chain relaxes instead of flushing forever.
                if (style.keepWithNext || style.keepTogether) {
                    var needed = layout.height
                    var currentStyle = style
                    var next = index + 1
                    while (currentStyle.keepWithNext && next < elements.size && needed <= bodyHeight) {
                        if (!visible(next)) { next++; continue }
                        val following = paragraph(elements[next]) ?: break
                        val followingStyle = StyleResolver.resolveParagraphStyle(following.styleName, document.styles)
                        if (followingStyle.pageBreakBefore || !followingStyle.masterPageName.isNullOrBlank()) break
                        needed += if (currentStyle.collapseSpacing || followingStyle.collapseSpacing)
                            maxOf(currentStyle.spaceAfterUnits, followingStyle.spaceBeforeUnits)
                        else currentStyle.spaceAfterUnits + followingStyle.spaceBeforeUnits
                        needed += layoutParagraph(next, following, document.styles).height
                        currentStyle = followingStyle
                        next++
                    }
                    if (needed <= bodyHeight && y + gap + needed > bottom && placed.isNotEmpty()) {
                        flush(PageEndReason.KEEP)
                        gap = 0f
                    }
                }
                if (y + gap + layout.lines.first().height > bottom && placed.isNotEmpty()) {
                    flush(PageEndReason.OVERFLOW)
                    gap = 0f
                }
                y += gap
                val breaks = p.pageBreakOffsets.filter { it in 0..p.text.length }.groupingBy { it }.eachCount().toMutableMap()
                var lineStart = 0
                while (lineStart < layout.lines.size) {
                    val source = layout.lines[lineStart].startOffset
                    repeat(breaks.remove(source) ?: 0) { flush(PageEndReason.AUTHORED, force = true) }
                    var lineEnd = lineStart
                    var height = 0f
                    while (lineEnd < layout.lines.size && y + height + layout.lines[lineEnd].height <= bottom + 0.001f) {
                        if (lineEnd > lineStart && layout.lines[lineEnd].startOffset in breaks) break
                        height += layout.lines[lineEnd++].height
                    }
                    val remaining = layout.lines.size - lineEnd
                    if (remaining > 0 && layout.lines[lineEnd].startOffset !in breaks) {
                        val available = lineEnd - lineStart
                        if (available < style.orphans && placed.isNotEmpty()) {
                            flush(PageEndReason.OVERFLOW)
                            continue
                        }
                        if (remaining < style.widows) {
                            val reducedEnd = layout.lines.size - style.widows
                            if (reducedEnd - lineStart >= style.orphans) lineEnd = reducedEnd
                            else if (layout.height <= bodyHeight && placed.isNotEmpty()) {
                                flush(PageEndReason.OVERFLOW)
                                continue
                            }
                        }
                    }
                    // One over-height line is consumed even on a too-small page.
                    if (lineEnd == lineStart) lineEnd++
                    val lines = layout.lines.subList(lineStart, lineEnd)
                    height = lines.sumOf { it.height.toDouble() }.toFloat()
                    val fragment = layout.copy(lines = lines, height = height,
                        boundingBox = OfficeRect(0f, 0f, layout.width, height))
                    register(index)
                    placed += PageElementLayout(element, fragment,
                        OfficeRect(pageSpec.marginStartDp, y, pageSpec.marginStartDp + layout.width, y + height),
                        index, lineStart, lines.first().startOffset, lines.last().endOffset,
                        lineStart > 0, lineEnd < layout.lines.size)
                    y += height
                    lineStart = lineEnd
                    if (lineStart < layout.lines.size && layout.lines[lineStart].startOffset !in breaks) flush(PageEndReason.OVERFLOW)
                }
                repeat(breaks.remove(p.text.length) ?: 0) {
                    flush(PageEndReason.AUTHORED, force = true)
                    trailingAuthoredBreak = true
                }
                previousAfter = style.spaceAfterUnits.coerceAtLeast(0f)
                previousCollapses = style.collapseSpacing
                if (style.pageBreakAfter && elements.getOrNull(index + 1) !is OfficePageBreak) {
                    flush(PageEndReason.AUTHORED)
                }
            } else if (tableOf(element) != null) {
                placeTable(element, index, tableOf(element)!!)
            } else {
                val imageBox = when (element) {
                    is OfficeImage -> DocumentImages.box(element.widthDp, element.heightDp)
                    is OfficeDocElement.ImageElement -> DocumentImages.box(element.image.widthDp, element.image.heightDp)
                    else -> null
                }
                val height = if (imageBox != null) imageBox.heightUnits else 30f
                val width = imageBox?.widthUnits?.coerceAtMost(pageSpec.contentWidthDp) ?: pageSpec.contentWidthDp
                if (y + previousAfter + height > bottom && placed.isNotEmpty()) flush(PageEndReason.OVERFLOW)
                y += previousAfter
                previousAfter = 0f
                register(index)
                placed += PageElementLayout(element, bounds = OfficeRect(pageSpec.marginStartDp, y, pageSpec.marginStartDp + width, y + height), elementIndex = index)
                y += height
            }
        }
        if (placed.isNotEmpty() || pages.isEmpty() || trailingAuthoredBreak) flush(PageEndReason.END, force = true)
        return DocumentLayoutResult(pages, pages.size * pageHeightDp, firstPages, elementPages)
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
                elemLayout.tableFragment?.let { fragment ->
                    val cell = fragment.cells.firstOrNull { geometry ->
                        x >= geometry.bounds.left && x <= geometry.bounds.right &&
                            relativeY >= geometry.bounds.top && relativeY <= geometry.bounds.bottom
                    }
                    if (cell != null) {
                        val paragraph = cell.paragraphs.firstOrNull { geometry ->
                            relativeY >= geometry.bounds.top && relativeY <= geometry.bounds.bottom
                        }
                        if (paragraph != null) {
                            var lineY = paragraph.bounds.top
                            paragraph.layout.lines.forEachIndexed { lineIndex, line ->
                                if (relativeY >= lineY && relativeY <= lineY + line.height) {
                                    val lineRelativeX = x - paragraph.bounds.left - line.left
                                    val caret = line.caretAdvances.indices.minByOrNull {
                                        kotlin.math.abs(line.caretAdvances[it] - lineRelativeX)
                                    } ?: 0
                                    return HitTestResult(
                                        pageIndex = pageIndex,
                                        elementIndex = elemLayout.elementIndex,
                                        paragraphIndex = elemLayout.elementIndex,
                                        lineIndex = lineIndex,
                                        characterOffset = line.caretOffsets.getOrElse(caret) { line.startOffset },
                                        tableRow = cell.cell.logicalRow,
                                        tableColumn = cell.cell.logicalColumn,
                                        tableCell = cell.cell.key
                                    )
                                }
                                lineY += line.height
                            }
                        }
                        return HitTestResult(
                            pageIndex = pageIndex,
                            elementIndex = elemLayout.elementIndex,
                            paragraphIndex = elemLayout.elementIndex,
                            lineIndex = -1,
                            characterOffset = 0,
                            tableRow = cell.cell.logicalRow,
                            tableColumn = cell.cell.logicalColumn,
                            tableCell = cell.cell.key
                        )
                    }
                }
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
                                lineIndex = elemLayout.firstLineIndex + lineIdx,
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
    val characterOffset: Int,
    val tableRow: Int? = null,
    val tableColumn: Int? = null,
    val tableCell: TableCellSourceKey? = null
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
