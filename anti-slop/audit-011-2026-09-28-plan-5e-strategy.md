# Audit 011 (2026-09-28): Plan 5e Strategy and Implementation Blueprint

**Baseline:** `main` `600394c` (Merge pull request #18 from `arena/01a0e40a-papirus-office`).
**Author:** Arena.ai Coding Agent
**Status:** Plan 5d completed and merged; Plan 5e active implementation blueprint.
**Context:** Preceded by Plan 5a (PR #15, measuring stick), Plan 5b (PR #16, fixture re-baseline), Plan 5c (PR #17, documentation refresh), and Plan 5d (PR #18, breaks and defaults).
**Standards Compliance:** Strict adherence to `antislop.md` Mode 1 (zero em dashes U+2014, exact `file:line` citations, grounded measurements, unambiguous distinction between shipped code and target designs).

---

## 1. Executive Summary & Progression Context

Plan 5 ("Layout metrics and pagination") is organized into five structured sub-plans designed to isolate parsing invariants from layout measurement. With the successful completion and merging of PR #18 ("Plan 5d: breaks, defaults, and metric style chains", commit `600394c`), the parsing half of the document model is fully stabilized.

### 1.1 Status of the Five Plan 5 Sub-Plans

1. **Plan 5a (PR #15 - Merged `1769611`):** The measuring stick. Shipped `LayoutUnits` (96 units/inch), `TextMetrics` interface and advance tables, `FontRegistry` substitution seam, `LayoutDump` per-page element reporting, `PageTransform`, and `Plan5ElementDumpTest`.
2. **Plan 5b (PR #16 - Merged `afb5f77`):** Fixture re-baseline. Re-anchored canonical test corpus to authentic producers (Microsoft Word 16 for all DOCX, Collabora Office 26.04.3.1 for all ODT), establishing authoritative page count references and updating `SampleMatrixTest` and `PageGeometryTest`.
3. **Plan 5c (PR #17 - Merged `3631611`):** Documentation refresh. Delivered `DESIGN.md` v3.0, Material 3 Expressive adoption contract, mobile editor surface briefs, and truth pass on templates, drive, and fonts.
4. **Plan 5d (PR #18 - Merged `600394c`):** Breaks and defaults (parsing side). Eliminated fake breaks (`w:lastRenderedPageBreak`, `text:soft-page-break`), parsed authored breaks (`w:br type=page`, section `w:sectPr`, `fo:break-before=page`), computed body rectangles from margins and header/footer specifications, established the metric-only style cascade (`docDefaults` -> `Normal` -> parent styles -> direct `pPr`), and replaced hardcoded constants with file-declared document defaults.
5. **Plan 5e (Active Step - Target PR #19):** Metrics and windows (measurement side). Integrates `TextMetrics` into the active layout path, deletes the `2.5f` scaling fudge and `fallbackTextSize`, deletes the `(sizeSp + 5f)` renderer magic constant, replaces static `elementGapDp = 12f` with style-driven `spaceBefore` and `spaceAfter` margins, adds widow/orphan floor handling, introduces tab stop line wrapping, and achieves green status across all twelve per-format pagination windows.

Following Plan 5e, the roadmap proceeds directly to **Plan 1** (Master Index synchronization), followed by **Plans 6 through 11**, culminating in a physical device verification pass against `docs/InkyC1Checklist.md`. This solidifies LibreOffice Writer Guide 26.2 Chapter 1 compliance and unblocks **Chapter 2: Working With Text (Basics)**.

---

## 2. CI Verification Baseline: PR #18 Run 36355811273 Analysis

The unit test run `36355811273` on commit `7045a2b` executed 204 unit tests across 36 test classes with 0 failures, 0 errors, and 0 skipped tests.

### 2.1 Authoritative Break Counts Verification

Plan 5d successfully normalized authored page breaks across the corpus, verified by `BreakSemanticsTest.kt:63, 81`:

```
ODT Break Counts (fo:break-before=page):
Sample-1.odt: 0 authored breaks in model
Sample-2.odt: 2 authored breaks in model
Sample-3.odt: 0 authored breaks in model
Sample-4.odt: 2 authored breaks in model
Sample-5.odt: 2 authored breaks in model
Sample-6.odt: 2 authored breaks in model

DOCX Break Counts (w:br type="page" + intermediate w:sectPr):
Sample-1.docx: 0 authored breaks in model
Sample-2.docx: 6 authored breaks in model (2 w:br + 4 w:sectPr)
Sample-3.docx: 0 authored breaks in model
Sample-4.docx: 7 authored breaks in model (2 w:br + 5 w:sectPr)
Sample-5.docx: 6 authored breaks in model (2 w:br + 4 w:sectPr)
Sample-6.docx: 6 authored breaks in model (2 w:br + 4 w:sectPr)
```

Zero ephemeral soft breaks (`w:lastRenderedPageBreak`, `text:soft-page-break`) pollute the editable text or document elements (`BreakSemanticsTest.noPlainTextContainsPageBreakMarkers`).

### 2.2 Fixture Pagination Matrix at Plan 5d Landing

The layout engine element dump (`LayoutDump.kt:40-75`) reported the following page counts against the audit-008 reference windows:

| File | Actual Pages | Producer Reference | Audit-008 Window | Thin Pages | Empty Pages | Model Breaks | Window Status |
|---|---|---|---|---|---|---|---|
| Sample-1.odt | 12 | 15 | 12..18 | 0 | 0 | 0 | **In Window** |
| Sample-1.docx | 12 | 15 | 12..18 | 0 | 0 | 0 | **In Window** |
| Sample-2.odt | 22 | 23 | 18..28 | 0 | 0 | 2 | **In Window** |
| Sample-2.docx | 23 | 23 | 18..28 | 0 | 0 | 6 | **Exact Match** |
| Sample-3.odt | 11 | 22 | 18..26 | 0 | 0 | 0 | Below window (thin metrics) |
| Sample-3.docx | 12 | 22 | 18..26 | 1 | 0 | 0 | Below window (thin metrics) |
| Sample-4.odt | 7 | 11 | 9..13 | 0 | 0 | 2 | Below window (thin metrics) |
| Sample-4.docx | 11 | 10 | 8..12 | 2 | 0 | 7 | **In Window** |
| Sample-5.odt | 14 | 19 | 15..23 | 0 | 0 | 2 | Below window (thin metrics) |
| Sample-5.docx | 16 | 18 | 15..21 | 0 | 0 | 6 | **In Window** |
| Sample-6.odt | 21 | 22 | 17..27 | 1 | 0 | 2 | **In Window** |
| Sample-6.docx | 23 | 21 | 15..26 | 0 | 0 | 6 | **In Window** |

### 2.3 Diagnosis of the Remaining Gaps

8 of the 12 fixtures already land inside or exactly upon their target reference windows. The 4 fixtures that measure slightly below their target windows (Sample-3.odt, Sample-3.docx, Sample-4.odt, and Sample-5.odt) diverge due to specific legacy measurement heuristics that Plan 5e resolves:

1. **Virtual Scaling Fudge (`LayoutEngine.kt:208`):**
   `setPaintTextSize(style.fontSizeSp * 2.5f)` scales font sizes by an arbitrary 2.5 factor, while measuring widths with platform Paint or character length fallbacks (`text.length * 8.0f`), diverging from real layout units (96 DPI).
2. **Hardcoded Line Height Heuristic (`LayoutEngine.kt:246, 274`):**
   Lines use `height = getPaintTextSize() * 1.2f`, ignoring the style's authored `lineHeightFactor` (e.g. Sample-4's 1.5x line spacing) and `lineHeightExactUnits`.
3. **Magic Renderer Line Height (`LayoutDrivenDocumentRenderer.kt:461, 487, 566, 583, 637`):**
   The Compose renderer uses `lineHeight = ((sizeSp + 5f) * zoomScale).sp`, creating a disconnect between what the paginator measures and what the renderer paints.
4. **Static Inter-Element Gap (`LayoutEngine.kt:378`):**
   `currentY += h + elementGapDp` uses a hardcoded 12 dp gap rather than collapsing paragraph `spaceAfter` and `spaceBefore` values.
5. **Absence of Tab Stop Expansion (`LayoutEngine.kt:213`):**
   Tab characters `\t` are treated as generic characters rather than advancing to tab stop intervals (`w:tabs` / `style:tab-stops`), causing table of contents dot leaders in Sample-4 and Sample-6 to wrap too tightly.

---

## 3. Plan 5e Architecture & Implementation Blueprint

Plan 5e converts `TextMetrics` from an auxiliary evaluation class into the authoritative measurement engine across all layout and rendering paths.

### 3.1 Load-Bearing `TextMetrics` Integration (`LayoutEngine.kt`)

`LayoutEngine.layoutParagraph` must measure lines directly in layout units (96 per inch):

1. **Style Metrics Resolution:**
   Obtain `TextMetrics` for the paragraph style:
   ```kotlin
   val style = StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
   val metrics = TextMetrics.forStyle(style)
   ```
2. **Text Measurement:**
   Replace `measureTextWidth(word)` with `metrics.widthOf(word)`:
   - On Android devices with active graphics, `PaintAdvanceSource` executes real glyph measurement.
   - In Robolectric CI environments, `TableAdvanceSource` provides deterministic Liberation Serif/Sans advances matching authentic metrics.
3. **Line Height Calculation:**
   Replace `getPaintTextSize() * 1.2f` with `metrics.lineHeightUnits`:
   ```kotlin
   val lineH = metrics.lineHeightUnits
   val baseline = metrics.naturalLineHeightUnits
   ```
   This honors proportional spacing (`fo:line-height="115%"`, `w:line="276" lineRule="auto"`, Sample-4 `line="360"` 1.5x) and exact heights (`lineRule="exact"`).
4. **Eliminate Fudge Constants:**
   Remove `setPaintTextSize(style.fontSizeSp * 2.5f)` and delete `fallbackTextSize`.

### 3.2 Dynamic Paragraph Spacing (`LayoutEngine.kt:355-385`)

Replace `elementGapDp = 12f` with real margin collapsing:

1. **Inter-Paragraph Spacing Model:**
   For adjacent paragraphs:
   ```kotlin
   val prevSpaceAfter = previousStyle?.spaceAfterUnits ?: 0f
   val currentSpaceBefore = currentStyle.spaceBeforeUnits ?: 0f
   val interElementMargin = maxOf(prevSpaceAfter, currentSpaceBefore)
   ```
   (In Word/ODT additive mode: `spaceAfter + spaceBefore` where configured, or collapsed maximum).
2. **Vertical Cursor Advancement:**
   ```kotlin
   currentY += interElementMargin + elementHeight
   ```
   This directly applies Sample-1's `after="160"` (10.67 units) and Sample-3's `before="360"` (24 units), expanding Sample-3's vertical span to match its 22-page reference.

### 3.3 Renderer Line Height Alignment (`LayoutDrivenDocumentRenderer.kt`)

Eliminate the `(sizeSp + 5f)` magic constant across all text rendering surfaces:

1. In `ParagraphText`, `HeadingText`, and `ListItemText`:
   Replace:
   ```kotlin
   lineHeight = ((sizeSp + 5f) * zoomScale).sp
   ```
   With:
   ```kotlin
   val resolved = StyleResolver.resolveParagraphStyle(styleName, document.styles)
   val factor = resolved.lineHeightFactor.coerceAtLeast(1.0f)
   val computedLineHeightSp = resolved.fontSizeSp * factor
   lineHeight = (computedLineHeightSp * zoomScale).sp
   ```
2. This guarantees that text wrapping in the Compose view precisely matches the line breaks and element heights calculated during layout.

### 3.4 Widow and Orphan Control (`LayoutEngine.kt:250-320`)

When a paragraph is split across a page boundary:

1. **Widow/Orphan Constraint:**
   Apply a minimum of 2 lines (`widows = 2`, `orphans = 2` by default in LibreOffice Writer and Microsoft Word).
2. **Page Break Rules:**
   - If a paragraph has only 2 lines total, the entire paragraph must stay together; it cannot leave 1 line on page N.
   - If a multi-line paragraph splits such that only 1 line remains at the bottom of page N (orphan), the split point shifts up by 1 line so at least 2 lines stay on page N.
   - If only 1 line would be pushed to the top of page N+1 (widow), take 1 line from page N to join it on page N+1, ensuring at least 2 lines appear on page N+1.

### 3.5 Tab Stop Measurement for Line Honesty (`LayoutEngine.kt`)

In Table of Contents and formatted paragraphs containing `\t`:

1. Default tab stop interval:
   - DOCX default: `w:defaultTabStop` (typically 720 twips = 48 units = 0.5 inch).
   - ODT default: `style:tab-stop-distance` (typically 1.25 cm = 47.24 units = 0.492 inch).
2. Measure tabs relative to the line origin:
   ```kotlin
   val nextTabPosition = ceil((currentLineWidth + 1f) / defaultTabIntervalUnits) * defaultTabIntervalUnits
   currentLineWidth = nextTabPosition
   ```
3. This accounts for the tab spacing in Sample-4 (35 tabs) and Sample-6 (167 tabs), preventing TOC entries from prematurely wrapping or under-filling lines.

---

## 4. Alignment with LibreOffice Writer Guide 26.2 Chapter 1

Plan 5e directly implements requirements outlined in LibreOffice Writer Guide 26.2 Chapter 1 ("Introducing Writer"):

| Writer Guide 26.2 Chapter 1 Section | Papirus Surface | Impact of Plan 5e Implementation |
|---|---|---|
| **Status Bar: Page Information** (Table 1: Page number, total pages) | Unified bottom status bar (`InkyModule.kt:850-890`) | Page count stops deviating due to fudge factors; displays authentic counts matching M365 and Collabora references. |
| **Status Bar: Section & Object Info** (Table 1) | Unified bottom status bar | Correct page numbering sequence aligned with section breaks and authored page breaks. |
| **Navigation: Go to Page (Ctrl+G)** | Go to Page dialog (`InkyModule.kt:1560-1590`) | Users can jump to valid target pages up to the genuine document boundary (e.g. Page 22 of 22 in Sample-3). |
| **Using the Navigator: Headings & Objects** | Navigator Deck (`UniversalNavigatorSheet.kt`) | Jumps accurately position the viewport at the real vertical layout coordinate of headings, tables, and sections. |
| **Document Views: Zoom and View Layout** | Zoom controls & layout canvas | Replaces static 320 dp card logic with true 100% fit-to-width/fit-to-page based on physical page geometry (A4 / Letter). |
| **Outline Folding** | `OutlineEngine.kt` | Correctly reflows and paginates document when heading sections are collapsed/expanded. |

### 4.1 Groundwork for Chapter 2: Working With Text (Basics)

Plan 5e lays the architectural foundation for LibreOffice Writer Guide Chapter 2:
- **Font Selection and Size:** Font metrics tied to `FontRegistry` and `TextMetrics`.
- **Line and Paragraph Spacing:** Proportional factors, exact twip measurements, before/after paragraph margins.
- **Indentation and Alignment:** First line indents, hanging indents, margins.
- **Tabs and En-dash/Bullet Lists:** Tab stop progression and list item alignment.

---

## 5. Implementation Phases & Acceptance Gates

### 5.1 Step 1: Implement `TextMetrics` Load-Bearing Path
- Update `LayoutEngine.layoutParagraph` to consume `TextMetrics.forStyle(style)`.
- Remove virtual `2.5f` scale factor; compute line advances and heights in units.
- Remove `(sizeSp + 5f)` magic constant in `LayoutDrivenDocumentRenderer.kt`.
- Run CI: verify existing unit tests pass without regressions.

### 5.2 Step 2: Implement Dynamic Spacing & Tab Stops
- Add paragraph `spaceBefore` and `spaceAfter` vertical spacing in `LayoutEngine.performLayout`.
- Add default tab stop interval calculation.
- Run `Plan5ElementDumpTest` and verify that Sample-3, Sample-4, and Sample-5 page counts advance into their target windows.

### 5.3 Step 3: Implement Widow/Orphan Protection & Pagination Windows Assertion
- Add widow/orphan line floor logic to `LayoutEngine.kt`.
- Create `PaginationFidelityTest.kt` asserting that all 12 fixtures land squarely within their audit-008 windows:
  - DOCX: Sample-1 (12..18), Sample-2 (18..28), Sample-3 (18..26), Sample-4 (8..12), Sample-5 (15..21), Sample-6 (15..26).
  - ODT: Sample-1 (12..18), Sample-2 (18..28), Sample-3 (18..26), Sample-4 (9..13), Sample-5 (15..23), Sample-6 (17..27).
- Verify zero thin pages without authored breaks and zero empty pages.

### 5.4 Acceptance Checklist for Plan 5e PR
- [ ] `TextMetrics` actively drives `LayoutEngine.layoutParagraph` line measurement.
- [ ] `LayoutDrivenDocumentRenderer.kt` consumes style line heights; `(sizeSp + 5f)` is deleted.
- [ ] Paragraph `spaceBefore` and `spaceAfter` replace hardcoded `elementGapDp = 12f`.
- [ ] Widow/orphan floor of 2 implemented.
- [ ] Tab stop intervals advance line widths appropriately.
- [ ] All 12 fixtures satisfy their per-format pagination windows.
- [ ] Zero em dashes (U+2014) in all modified and new files.
- [ ] CI testDebugUnitTest passes completely with 100% green tests.
