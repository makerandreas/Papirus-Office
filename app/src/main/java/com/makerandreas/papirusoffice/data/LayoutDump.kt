package com.makerandreas.papirusoffice.data

import java.util.Locale

/**
 * Per-page trace of a [DocumentLayoutResult] (roadmap E-0, plan 5A). It is a
 * pure function over the layout output and the document that produced it,
 * so a unit test can print it for every sample and CI can read it back.
 *
 * What it explains, per page: which elements landed there, how much of the
 * body they reserved, how much was left, and why the page ended (an
 * [OfficePageBreak] element followed the last placed element, the next
 * element did not fit, or the document ended). Pages ended by a break with
 * few elements on them are the "blank page" symptom; the parse-level counts
 * of fake breaks live in audit-007 §5 and in the break inventory below.
 */
object LayoutDump {

    enum class PageEnd { BREAK, OVERFLOW, END }

    data class PageRow(
        val pageNumber: Int,
        val elementCount: Int,
        val firstElementIndex: Int?,
        val lastElementIndex: Int?,
        /** Counts of P(aragraph) H(eading) L(ist) T(able) I(mage) O(ther) placed on the page. */
        val kinds: String,
        /** Paragraph-like elements with blank text on this page. */
        val blankParagraphs: Int,
        /** Height from the flow top to the bottom of the last element, layout units. */
        val reservedHeight: Float,
        /** Space between the last element's bottom and the flow bottom, layout units. */
        val leftover: Float,
        val endedBy: PageEnd
    )

    data class Report(
        val label: String,
        val pageSpec: PageStyleSpec,
        val pages: List<PageRow>,
        val elementTotal: Int,
        val placedTotal: Int,
        val breakElements: Int,
        val blankParagraphs: Int,
        val pageBreakMarkerParagraphs: Int,
        val referencePageCount: Int?,
        val measurementProbe: String,
        val defaultBodyStyle: ParagraphStyle,
        /** ODF: master page the body starts on and its layout, when the file names one (null for DOCX or "Standard"). */
        val firstMasterPageName: String? = null,
        val firstMasterPageSpec: PageStyleSpec? = null
    ) {
        val pageCount: Int get() = pages.size
        val emptyPages: Int get() = pages.count { it.elementCount == 0 }
        val thinPages: Int get() = pages.count { it.elementCount < THIN_PAGE_ELEMENTS }
        val pagesEndedByBreak: Int get() = pages.count { it.endedBy == PageEnd.BREAK }
        val pagesEndedByOverflow: Int get() = pages.count { it.endedBy == PageEnd.OVERFLOW }
        val thinPagesEndedByBreak: Int get() = pages.count { it.elementCount < THIN_PAGE_ELEMENTS && it.endedBy == PageEnd.BREAK }

        /**
         * The mechanism behind this file's page count, named from the rows:
         * break-driven when most thin pages end at a break element, metric-driven
         * when pages overflow with few elements, or neither when no page is thin.
         */
        val mechanism: String
            get() = when {
                thinPages == 0 -> "no thin pages"
                thinPagesEndedByBreak * 2 >= thinPages -> "break elements ($thinPagesEndedByBreak of $thinPages thin pages end at a break element; $breakElements break elements in the model)"
                else -> "inflated metrics (${thinPages - thinPagesEndedByBreak} of $thinPages thin pages overflow with fewer than $THIN_PAGE_ELEMENTS elements)"
            }

        fun toText(): String {
            val sb = StringBuilder()
            val spec = pageSpec
            sb.append("== ").append(label).append('\n')
            sb.append(
                String.format(
                    Locale.ROOT,
                    "page box %.1f x %.1f  margins t/b/s/e %.1f/%.1f/%.1f/%.1f  header/footer %.1f/%.1f%n",
                    spec.widthDp, spec.heightDp, spec.marginTopDp, spec.marginBottomDp,
                    spec.marginStartDp, spec.marginEndDp, spec.headerHeightDp, spec.footerHeightDp
                )
            )
            sb.append(
                String.format(
                    Locale.ROOT,
                    "flow used by paginator: top %.1f bottom %.1f width %.1f | declared body: top %.1f bottom %.1f%n",
                    spec.marginTopDp, spec.contentBottomDp, spec.contentWidthDp, spec.bodyTopDp, spec.bodyBottomDp
                )
            )
            if (firstMasterPageName != null) {
                val master = firstMasterPageSpec
                if (master != null) {
                    sb.append(
                        String.format(
                            Locale.ROOT,
                            "first body master page: %s -> %s: body top %.1f bottom %.1f (header/footer %.1f/%.1f); paginator uses %s%n",
                            firstMasterPageName, master.name, master.bodyTopDp, master.bodyBottomDp,
                            master.headerHeightDp, master.footerHeightDp, spec.name
                        )
                    )
                } else {
                    sb.append("first body master page: ").append(firstMasterPageName).append(" (layout not read)\n")
                }
            }
            sb.append(
                String.format(
                    Locale.ROOT,
                    "default body style: %.1f pt (%s) line factor %.2f | measurement: %s%n",
                    defaultBodyStyle.fontSizeSp, defaultBodyStyle.fontFamily ?: "no family",
                    defaultBodyStyle.lineHeightFactor, measurementProbe
                )
            )
            sb.append(
                "elements $elementTotal placed $placedTotal breakElements $breakElements blankParagraphs $blankParagraphs " +
                    "pageBreakMarkerParagraphs $pageBreakMarkerParagraphs\n"
            )
            sb.append(
                "pages $pageCount reference ${referencePageCount ?: "n/a"} empty $emptyPages thin(<$THIN_PAGE_ELEMENTS) $thinPages " +
                    "endedByBreak $pagesEndedByBreak endedByOverflow $pagesEndedByOverflow\n"
            )
            sb.append("mechanism: ").append(mechanism).append('\n')
            sb.append(String.format(Locale.ROOT, "%4s %5s %6s %6s %-22s %5s %9s %9s %s%n", "page", "elems", "first", "last", "kinds", "blank", "reserved", "leftover", "endedBy"))
            for (row in pages) {
                sb.append(
                    String.format(
                        Locale.ROOT,
                        "%4d %5d %6s %6s %-22s %5d %9.1f %9.1f %s%n",
                        row.pageNumber, row.elementCount,
                        row.firstElementIndex?.toString() ?: "-", row.lastElementIndex?.toString() ?: "-",
                        row.kinds, row.blankParagraphs, row.reservedHeight, row.leftover, row.endedBy
                    )
                )
            }
            return sb.toString()
        }
    }

    const val THIN_PAGE_ELEMENTS = 3
    const val PAGE_BREAK_MARKER = "--- Page Break ---"

    fun build(
        label: String,
        document: OfficeDocument,
        pageSpec: PageStyleSpec,
        result: DocumentLayoutResult,
        referencePageCount: Int? = null,
        measurementProbe: String = "unknown"
    ): Report {
        val elements = document.body.elements
        val breakElements = elements.count { it is OfficePageBreak }
        val rows = ArrayList<PageRow>(result.pages.size)
        for (page in result.pages) {
            val placed = page.elements
            val first = placed.firstOrNull()?.elementIndex
            val last = placed.lastOrNull()?.elementIndex
            val lastBottom = placed.maxOfOrNull { it.bounds.bottom } ?: pageSpec.marginTopDp
            val kinds = kindsSummary(placed)
            val blanks = placed.count { isBlankParagraph(it.element) }
            val endedBy = when {
                page.pageNumber == result.pages.size -> PageEnd.END
                last == null -> PageEnd.BREAK
                nextElementIsBreak(elements, last) -> PageEnd.BREAK
                else -> PageEnd.OVERFLOW
            }
            rows.add(
                PageRow(
                    pageNumber = page.pageNumber,
                    elementCount = placed.size,
                    firstElementIndex = first,
                    lastElementIndex = last,
                    kinds = kinds,
                    blankParagraphs = blanks,
                    reservedHeight = (lastBottom - pageSpec.marginTopDp).coerceAtLeast(0f),
                    leftover = (pageSpec.contentBottomDp - lastBottom).coerceAtLeast(0f),
                    endedBy = endedBy
                )
            )
        }
        return Report(
            label = label,
            pageSpec = pageSpec,
            pages = rows,
            elementTotal = elements.size,
            placedTotal = result.pages.sumOf { it.elements.size },
            breakElements = breakElements,
            blankParagraphs = elements.count { isBlankParagraph(it) },
            pageBreakMarkerParagraphs = elements.count { textOf(it)?.contains(PAGE_BREAK_MARKER) == true },
            referencePageCount = referencePageCount,
            measurementProbe = measurementProbe,
            defaultBodyStyle = StyleResolver.resolveParagraphStyle(null, document.styles),
            firstMasterPageName = document.styles.firstMasterPageName,
            firstMasterPageSpec = document.styles.pageStyleForMaster(document.styles.firstMasterPageName)
        )
    }

    /** True when the element after [lastPlacedIndex] (in document order) is a page break. */
    private fun nextElementIsBreak(elements: List<OfficeElement>, lastPlacedIndex: Int): Boolean {
        val next = lastPlacedIndex + 1
        return next < elements.size && elements[next] is OfficePageBreak
    }

    private fun kindsSummary(placed: List<PageElementLayout>): String {
        var p = 0; var h = 0; var l = 0; var t = 0; var i = 0; var o = 0
        for (item in placed) {
            when (item.element) {
                is OfficeParagraph, is OfficeDocElement.ParagraphElement -> p++
                is OfficeHeading -> h++
                is OfficeListItem -> l++
                is OfficeTable, is OfficeDocElement.TableElement -> t++
                is OfficeImage, is OfficeDocElement.ImageElement -> i++
                else -> o++
            }
        }
        val parts = ArrayList<String>(6)
        if (p > 0) parts.add("P$p")
        if (h > 0) parts.add("H$h")
        if (l > 0) parts.add("L$l")
        if (t > 0) parts.add("T$t")
        if (i > 0) parts.add("I$i")
        if (o > 0) parts.add("O$o")
        return if (parts.isEmpty()) "-" else parts.joinToString(" ")
    }

    private fun textOf(element: OfficeElement): String? = when (element) {
        is OfficeParagraph -> element.text
        is OfficeHeading -> element.text
        is OfficeListItem -> element.text
        is OfficeDocElement.ParagraphElement -> element.paragraph.text
        else -> null
    }

    private fun isBlankParagraph(element: OfficeElement): Boolean = textOf(element)?.isBlank() == true
}
