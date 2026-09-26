# Papirus Office Writer — Fix Strategy, Plans 4 to 9 (was PRs D to I)

**Date:** 2026-09-24
**Input:** `anti-slop/audit-005-2026-09-24.md` (findings F-01 … F-20, observations O-01 … O-06) and `anti-slop/audit-006-2026-09-24.md` (screenshot findings F-21 … F-31, compliance sweep)
**Numbering:** this document holds **plans 4 to 9**; it was written as PRs D to I and the letters are kept in parentheses for traceability. Sub-item IDs (`D-1`, `E-EN-2`, `F-3`, `G-1`, `H-4` …) are unchanged, so `letter-n` reads as `plan-n item` (D-1 = Plan 4 item 1, E-2 = Plan 5 item 2, F-3 = Plan 6 item 3, G-1 = Plan 7 item 1, H-4 = Plan 8 item 4). The index is `anti-slop/plan-01-master-index.md`.

**Writer Guide reference:** how these plans serve WG 24.8 Chapter 1 and `docs/InkyC1Checklist.md` is mapped in `plan-01-master-index.md` §3 and §4; the short version is that Plan 5 carries the status-bar page count and the caret/layout guards, Plans 7 and 8 carry the Navigator's categories and the checklist's Save Compatibility item, and Plan 9 carries the lifecycle items.

**Baseline:** post-PR-C nightly, `main` d1105ce (PRs #7 A, #8 B1, #9 B2, #10 C)
**Deliverable of this document:** an ordered, reviewable PR split with scope, root causes closed, files, tests, acceptance criteria, and risk. It changes no code by itself.

> **Read with:** `AGENTS.md` (branch/CI rules, en_US strings, 48 dp targets), `DESIGN.md` (M3 Expressive tokens, dial ENERGY 2 / RHYTHM 1 / MOTION 3), `antislop.md` + the five skills (Modal UI changes must pass the Delivery Gate), `docs/html` ODF 1.4 Part 1 (authoritative for every ODF decision), ECMA-376 §17 (authoritative for every OOXML decision).

---

## Why six plans and not one

The findings are not one bug. They sit in four independent layers that can each be verified on its own:

| Layer | Symptom class | Where the truth lives |
|---|---|---|
| **Chrome & input** | per-page counter, FAB overlap, keyboard dead, Viewer FCT missing, zoom not fitted | `InkyModule.kt`, `LayoutDrivenDocumentRenderer.kt`, `PapirusTextToolbar.kt` |
| **Measurement & pagination** | 21 → 65/88 pages, wrong image extents, "premature" spacing | `LayoutEngine.kt`, `PageStyleSpec`, style models |
| **Structure, ODF** | TOC, BAB/2.1 numbering, tables, heading runs, hyperlink text | `SvXMLImport.kt`, `SvXMLImportContext.kt`, `OdfXmlToken.kt`, `OfficeDocument.kt` |
| **Structure, OOXML** | heading sizes, run formatting, numbering, table grid, sections | `OfficeDocumentParser.kt` (DOCX branch), `DocxDocumentParser.kt`, `StyleResolver` |

Fixing chrome inside the measurement plan, or measurement inside the OOXML plan, would make both unreviewable: the first touches pixels, the second touches the page model, the third and fourth touch file-format parsing. The six plans below follow the layers and the dependency order **4 → 5 → 6 → 7 → 8 → 9**, with 4‖6 and 7‖(independent parts of 8) parallelisable.

---

## Shared enablers (land inside Plan 5, used by Plans 6-8)

These are the seams every later PR plugs into. They belong in E because E is the first PR that needs them.

* **E-EN-1 · `LayoutUnits` (one unit system).** `ptToUnits(pt) = pt * 96f / 72f`, `unitsToPt`, `cmToUnits`, `emuToUnits` (EMU ÷ 9525 = CSS px at 96/in), plus `twipsToUnits` (already in `OdfLength`). Delete the `* 2.5f` fudge (`LayoutEngine.kt:154`) and every other magic scale.
* **E-EN-2 · `TextMetrics(style): Measurable`.** Measurement and display read the *same* resolved style. Backed by an Android `Paint` on device; on JVM (CI) by a deterministic advance-width table so unit tests have stable numbers. No more "measured at 30, drawn at 16".
* **E-EN-3 · `ParagraphStyle` grows real metrics.** `spaceBeforeUnits`, `spaceAfterUnits`, `lineHeightFactor`, `startIndentUnits`, `endIndentUnits`, `firstLineIndentUnits`, `keepWithNext`, `pageBreakBefore`, `fontFamily`. Character styles keep nullable fields (F-3 behaviour preserved).
* **E-EN-4 · `NumberingModel`.** One renderer-side model for list/heading numbering: `NumberingSpec(levels: List<LevelSpec>)`, `LevelSpec(format, prefix, suffix, startValue, displayLevels, indentUnits, bulletChar, fontFamily, fontSizeUnits)`. G-1 (ODF) and H-4 (OOXML) both produce it; the renderer consumes it; `CounterState` resolves the visible label ("BAB II", "2.1", "a.", "•").
* **E-EN-5 · `FontRegistry`.** Family name → bundled `Typeface` (Liberation Serif for Times New Roman, Carlito for Calibri, Caladea for Cambria, Liberation Sans for Arial/Helvetica, Liberation Mono for Courier New, OpenSymbol for Symbol/Wingdings), falling back to system. Used by `TextMetrics` **and** `OfficeRuns.fontFamilyFor` so pagination and painting never disagree. `assets/fonts` already ships all of these; `FontProvider` already extracts them.

---

## Plan 4 — Viewer/Editor chrome and input fixes (was PR D)

**Goal:** the app stops *looking* broken before we touch the document engine. Ship first, it is the fastest win and the lowest risk.
**Closes:** F-01, F-02, F-03, F-04, F-05, F-06.
**Does not touch:** parser, layout, styles.

### Scope

1. **D-1 · One page counter (F-01).** Delete the per-card marker `Box/Text` at `LayoutDrivenDocumentRenderer.kt:174-182`. The unified bar (`InkyModule.kt:2368-2470`) is the only page counter in either mode.
2. **D-2 · Status bar geometry (F-02, F-03).** Rebuild the bar as three `weight(1f)` slots: leading = page range (clickable, Go-to-Page), centre = `N words, M chars`, trailing = zoom cluster (Editor) / a 48 dp Edit action (Viewer, replacing the FAB). The Edit FAB block at `InkyModule.kt:4170-4205` is deleted; Viewer reaches Editor through the bar. Rationale to write into the PR body per `antislop.md` purpose test: the FAB was duplicating an existing action and covering a live tap target.
3. **D-3 · Focus bridge (F-04).** Add `RendererFocusBridge` (an interface with `focusElement(index)` / `focusFirstEditable()`), implemented by `LayoutDrivenDocumentRenderer` over its `focusRequesters` map and exposed upward via a `remember { }` holder. The keyboard hub button and the sheet-close path call the bridge instead of the orphan `FocusRequester` at `InkyModule.kt:117`; delete the empty `catch (e: Exception)`. Tapping a page hands focus to the tapped field (the `onCursorChange` path already knows the element).
4. **D-4 · Viewer FCT (F-05).** In `ParagraphSelectField`, `LaunchedEffect(localValue.selection)`: when the selection is non-collapsed, compute the field bounds (`onGloballyPositioned` → `boundsInWindow`) and call the already-existing `customTextToolbar.show(rect)` (`PapirusTextToolbar.kt:77`); on collapse call `hide()`. This reuses the existing FCT UI, `FctMode` and Compact row rules; no new UI surface.
5. **D-5 · Fit-to-screen (F-06).** Single transform for the page stack: `fitScale = viewportWidth / page.widthDp`; `cardWidth = page.widthDp * fitScale * zoomScale`. 100 % becomes "page width equals viewport width" (M365-Word behaviour); 25–400 % remains a multiplier; horizontal scroll only appears above 100 %. This is F-1 from `plan-2026-09-22` and closes it.

### Files
`app/src/main/java/com/example/modules/inky/InkyModule.kt`, `.../inky/LayoutDrivenDocumentRenderer.kt`, `.../ui/components/PapirusTextToolbar.kt` (expose `show`/`hide` cleanly, no behaviour change), `app/src/main/res/values/strings.xml` (only if new copy is needed; en_US).

### Tests
* Compose/Robolectric: exactly **one** `"n / m"` page counter node exists in Viewer and in Editor; the Edit action is inside the status bar row (overlap assertion fails before the fix, passes after).
* Unit: `RendererFocusBridge` focuses the first editable element; keyboard button path does not swallow exceptions (assert on a fake bridge).
* Unit: viewer selection triggers `show(rect)` on a fake `TextToolbar`, collapse triggers `hide()`.
* Screenshot test (existing `GreetingScreenshotTest` pattern) at 320 dp width: page card width == viewport width at 100 %.

### Acceptance
All four user-visible items reproduced-and-fixed on device: no per-page counter; no overlap; centred counter; keyboard summon works after opening every sheet deck; Viewer FCT appears on long-press **and** on handle drag; 100 % fits the screen. `antislop` Delivery Gate reported PASS with evidence (R-26 dead controls, R-32 keyboard reachability, R-03 no overflow at 320 dp).

### Risk
Low. The only behavioural trade is removing the Viewer FAB (a UX decision the audit argues for); if the user prefers to keep it, D-2 falls back to insetting the bar so the two cannot overlap.

**Size:** small (1–2 days). **Commit plan:** 4 commits (counter, status bar, focus bridge + FCT, fit transform) so each is revertible.

---

## Plan 5 — Layout metrics and pagination (was PR E, the page-count plan)

> **Amended 2026-09-26.** Plan 5 executes as three PRs: **PR 15 (5A)** the measuring stick, **PR 16a (5B)** breaks and defaults, **PR 16b (5C)** metrics and windows; scope per PR is in `plan-2026-09-24-remaining-pr-roadmap-v2.md` §4.3-§4.4, evidence in `audit-007-2026-09-26-sample-matrix.md`. The acceptance lines below that say "for both formats" are superseded: windows are **per format** for all six pairs (audit-007 §11.3; DOCX around the M365 counts 15/23/20/10/18/21, ODT provisional until the Collabora-regenerated fixtures land), and page geometry is honoured as each file declares it. The rest of this section stays as the design record.
>
> **5A implementation record (PR #15, 2026-09-26).** Landed on the branch, in order: `LayoutUnits` (one 96/inch converter; `OdfLength`/`OpenXmlUnits` delegate); `ParagraphStyle` metric fields + `PageStyleSpec.headerHeightDp/footerHeightDp/bodyTopDp/bodyBottomDp` + `DocumentStyles.masterPages/firstMasterPageName` (ODF header/footer heights and master pages read, not yet pagination inputs); `FontRegistry` (exact > metric-compatible > stand-in Aptos -> Martel Sans > user > generic > default) behind `OfficeRuns.fontFamilyFor` and `TextMetrics`; `TextMetrics` (Paint at textSize = units, or the deterministic em table when Paint is a stub) provided but not load-bearing; `LayoutDump` (E-0) with `Plan5ElementDumpTest` over all six pairs; `PageTransform` (E-7: one `pageScale` for card, margins, gap, text, images, taps; system font scale neutralised); `SampleMatrix`/`SampleMatrixTest` pinning all twelve fixtures from the package XML; CI native inventory + test-report artifact + PR report comment (the sandbox cannot read Actions logs). Pagination windows untouched (Sample-5 12..30 green). The baseline dump and its seven findings are audit-007 §12.1; the two that reshape 5B/5C: CI's Paint is a per-character stub (every CI page count so far is paragraph arithmetic), and no default style is read from the file (14 pt, no family, everywhere). Corrections made along the way: Sample-2.odt body is 12 pt (default-style), not 11; OnlyOffice/WPS ODTs name the default font with `fo:font-family`, not `style:font-name`.

**Goal:** one honest measurement of text, one honest page model. This is the PR that moves 65/88 pages toward 21.
**Closes:** the page-count half of the user's finding 6, F-10's paragraph metrics, the table-height part of F-11, plus enables F-18.
**Depends on:** nothing. **Blocks:** G, H acceptance (their page-count tests need E's windows).

### Scope

1. **E-1 · Enablers E-EN-1 … E-EN-5** (section 1 above).
2. **E-2 · Paragraph layout from real metrics.** `layoutParagraph` uses `TextMetrics` at `ptToUnits(fontSizePt)`; line height = `max(fontAscent+fontDescent, fontSizeUnits * lineHeightFactor)`; wrap by real advance widths (keep the existing hyphenation hook, still off by default).
3. **E-3 · Paragraph spacing replaces the flat gap.** `currentY += h + spaceAfter(prev) + spaceBefore(next)`; drop `elementGapDp = 12f` (`LayoutEngine.kt:107/331`). ODF reads `fo:margin-top/bottom`, `fo:text-indent`, `fo:margin-left/right`, `fo:line-height`; OOXML reads `w:spacing` (before/after/line/lineRule) and `w:ind`. Character-level defaults stay out of paragraph metrics.
4. **E-4 · Break semantics.** Honour `fo:break-before="page"` / `fo:break-after`, `style:keep-with-next` (ODF), `w:pageBreakBefore`, `w:keepNext` (OOXML). `text:soft-page-break` and `w:lastRenderedPageBreak` are **not** page breaks by default (fixes O-02's pagination half).
5. **E-5 · Widow/orphan floor.** Minimum 2 lines at the bottom of a page and 2 at the top when the paragraph style declares it (ODF `fo:widows`/`fo:orphans` = 2 in Sample-6's `Normal`).
6. **E-6 · Fidelity windows in CI.** `Sample5UnifiedPaginationTest` tightens from `12..30` to `15..21`; a new `Sample6PaginationTest` asserts `15..26` pages for both `Sample-6.odt` and `Sample-6.docx`, with a comment recording the M365 reference (21) and the expectation to tighten after G/H.

### Files
`data/LayoutEngine.kt`, `data/OfficeDocument.kt` (`ParagraphStyle` fields), `data/odf/SvXMLImport.kt` (margin/line-height/tab/break capture), `data/OfficeDocumentParser.kt` (DOCX `w:spacing`/`w:ind` capture at the paragraph level), `data/OfficeRuns.kt` (single style source), new `data/LayoutUnits.kt`, `data/TextMetrics.kt`.

### Tests
`LayoutUnitsTest` (pt/cm/emu/twips round-trips), `TextMetricsTest` (JVM table is deterministic), `ParagraphMetricsTest` (Sample-6 `Normal` yields 116 % line height and 0.282 cm after), `PaginationFidelityTest` (both formats, windows above), plus the unchanged Sample-1…5 heading/navigation suites.

### Acceptance
* Sample-5 lands in **15..21** (reference 18) for both formats.
* Sample-6 lands in **15..26** (reference 21) for both formats, down from 65/88.
* No regression in `InkyC1Checklist` zoom/caret/selection items; caret geometry tests still green.
* A one-page document stays one page; an empty document stays one page.

### Risk
Medium-high: this is the first change that makes pagination *correct* rather than *stable*, so many upstream assertions will move. Mitigations: windows instead of exact counts in this PR, `forceRebuildAll` is already available for spot checks, and the metric change is a single file (`TextMetrics`) that can be reverted alone.

**Size:** medium (4–6 days). **Commit plan:** units+metrics → spacing → breaks/widows → windows/tests, one commit each.

---

## Plan 6 — Image pipeline and load performance (was PR F)

**Goal:** images appear immediately, at the right size, and stay there.
**Closes:** F-07, F-18, the "never load" half of finding 5.
**Depends on:** E (extents are layout units).

### Scope

1. **F-1 · Extents are parsed for both formats, in the live path.** OOXML: read `wp:extent cx/cy` (and `a:ext` for legacy `v:shape`) inside `OfficeDocumentParser`'s DOCX branch, converted through `emuToUnits`. ODF: keep `svg:width/height` but store layout units (`OdfFrameContext` already does this; align rounding). `ImageElement`/`OfficeImage` carry `widthUnits`/`heightUnits`; `LayoutEngine` reserves exactly that; the renderer scales from it (fixes F-18: 7.735 cm ↔ 2784475 EMU now produce the same 292 dp on both sides). Then **delete** the dead extent plumbing so it cannot mislead again: `DocxDocumentParser.parseDocxFile`, `DocxParseResult.imageExtents`, `InkyModule.docxExtents` (assigned 5 times, read 0 times).
2. **F-2 · Media store instead of `cacheDir`.** Extract into `filesDir/media/<sha256(path:len:mtime)>/` with a small manifest (name → size, hash), an LRU cap (`ZipSafe.MAX_IMAGE_BYTES` already bounds single files), and a **self-heal** path: if a referenced file is missing at render time (cache trim, cleanup worker), re-extract that one entry from the source package instead of printing `[Image]`. This is the fix for "sometimes the image never loads".
3. **F-3 · Decode without a blank frame.** `DocxEmbeddedImage` gets explicit `size()` from the resolved extent, `ContentScale.Fit`, a low-cost placeholder, and `crossfade(false)`; pre-decode the first pages' images on a background dispatcher while the document is being laid out.
4. **F-4 · Honest loading progress.** Delete `delay(500)+delay(500)+delay(400)` from `runDocumentLoading`; drive `loadingProgressStatus` from real stages (zip open → styles → body → media → layout). Target: Recents open shows text in one frame after the parse, images within one frame after decode (no ≥500 ms blank).
5. **F-5 · Save-path guard (O-01, minimal).** Until Plan 9 exists, the DOCX/ODT save must refuse to run when the model contains images it cannot serialise, or must copy original media entries through unchanged. A silent `[Image: path]` replacement is data loss; make it an explicit, logged failure the user sees.

### Files
`data/DocxImageExtractor.kt` (→ `MediaStore`), `data/OfficeDocumentParser.kt` (extent capture, DOCX and ODF), `data/DocxDocumentParser.kt` (delete the dead `parseDocxFile`/`imageExtents` plumbing), `data/OfficeDocument.kt` (`OfficeImage` units), `data/LayoutEngine.kt` (reserve declared size), `modules/inky/LayoutDrivenDocumentRenderer.kt` (`RenderImage`), `ui/components/DocxEmbeddedImage.kt`, `modules/inky/InkyModule.kt` (loading sequence, save guard, drop dead `docxExtents` state).

### Tests
`ImageExtentParsingTest` (Sample-6 `.odt`/`.docx` resolve to the same dp triple, 292/165/231), `MediaStoreTest` (extracts to filesDir; deleting a file self-heals; caps respected), `PaginationImageTest` (image taller than a page still paginates), `OpenLatencyTest` (no artificial `delay` calls in the open path, assertion by source-scan or by fake clock).

### Acceptance
Recents open renders text then images without a blank frame on the Realme C3 class; media survives a cache wipe; a DOCX image and its ODT twin render at the same size; deleting a file mid-session then reopening restores it.

### Risk
Low-medium (Coil behaviour varies with device; the self-heal path is the important part).

**Size:** medium (3–4 days).

---

## Plan 7 — ODF structural fidelity (was PR G)

**Goal:** Sample-6.odt reads like the Writer Guide document it is: numbered headings, real lists, a table with correct columns, a TOC, and hyperlinks that keep their text.
**Closes:** F-08 (font identity), F-09, F-11, F-12, F-13, F-14, F-15, O-03, O-04, O-05.
**Depends on:** E (E-EN-4 numbering model, E-EN-5 font registry).

### Scope

1. **G-1 · List styles become real numbering (F-13, F-12).** New tokens + parser for `text:list-style`, `text:list-level-style-number`, `text:list-level-style-bullet` (`text:num-format`, `style:num-prefix`, `style:num-suffix`, `text:display-levels`, `text:start-value`, `text:bullet-char`, per-level `style:text-properties` and `style:list-level-properties`). Paragraph styles keep `style:list-style-name`; `text:list` keeps `text:style-name`; `text:list-item` keeps `text:start-value`; `text:continue-numbering` is honoured. Output goes through E-EN-4, so Sample-6 renders `BAB I`, `BAB II`, `2.1`, `2.1.1` with the level's own font size, and bullets use the bullet level's font (fixes the size anomaly). `OdfListItemContext` stops hard-coding `"• "` / `"◦ "`.
2. **G-2 · TOC and index (F-14).** Tokens for `text:table-of-content`, `text:index-body`, `text:index-title`, `text:index-source-styles`; parse the authored snapshot into a `TableOfContent` element (entries with level, text, page number, link anchor) and render it with a right-aligned tab stop and the `TOC 1/2/3` styles. Default is the authored snapshot (fast, page numbers match the file); regeneration from the Navigator index is a follow-up feature (open question 4 in the audit).
3. **G-3 · Hyperlinks (F-14 second half, O-04).** Add an `XML_A` arm to `OdfParagraphContext`/`OdfHeadingContext` so `text:a` preserves its spans and its `xlink:href`; `OfficeTextRun` gains an optional link. Sample-2/4/5/6 body hyperlinks stop disappearing (45 occurrences in Sample-6.odt alone).
4. **G-4 · Heading and list-item runs (F-15).** Add `runs: List<TextRun>` to `OfficeDocumentElement.Heading` and `ListItem`; `OdfHeadingContext` collects them like `OdfParagraphContext`; `toOfficeDocument()` stops writing `runs = emptyList()` / a single synthetic run. Bold-italic-coloured heading text survives.
5. **G-5 · Tables (F-11).** Parse `table:table-column` (`table:style-name` → `style:table-column-properties/style:column-width`) and cell properties (`style:table-cell-properties`: padding, borders, background, vertical align, `table:number-columns-spanned`/`table:number-rows-spanned`). `OfficeTable` carries column weights; `LayoutEngine` computes height from the real font metrics of the heaviest cell and paginates rows across pages when needed; `RenderTable` uses widths and per-cell style instead of `10.sp` everywhere. Add an ODT table rendering test on Sample-6 (6 rows × 5 columns, header row repeated).
6. **G-6 · Font identity (F-08, F-09).** `FontRegistry` (E-EN-5) becomes the single mapping for both metrics and display; verify with a screenshot test that Times New Roman body text renders through Liberation Serif and that heading sizes equal the style sheet (Sample-6 headings inherit 12 pt TNR bold, centred, with the 0.884 cm hanging indent on level 2).

### Files
`data/odf/OdfXmlToken.kt`, `data/odf/SvXMLImport.kt`, `data/odf/SvXMLImportContext.kt`, `data/OfficeDocumentModel.kt`, `data/OfficeDocument.kt`, `data/LayoutEngine.kt`, `modules/inky/LayoutDrivenDocumentRenderer.kt`, new `data/Numbering.kt` consumer code, `data/FontRegistry.kt`.

### Tests
`OdtListNumberingTest` (Sample-6 yields `BAB I`…`BAB IV` and `2.1`-shaped labels; numbering restarts per chapter), `OdtTocTest` (Sample-6 and Sample-4 TOC entries present, none empty), `HyperlinkFidelityTest` (no dropped link text in Samples 2/4/5/6; href preserved), `HeadingRunsTest`, `OdtTableGeometryTest`, `FontRegistryTest`.

### Acceptance
* Sample-6.odt: TOC populated, chapter headings numbered `BAB n`, sub-headings numbered `n.m`, table columns match the file's declared widths, bullets and list text share the level's font size, no hyperlink text lost.
* Sample-1/2/4/5.odt: no regressions in the existing heading/navigation suites; page-count windows still hold.

### Risk
Medium. Numbering is the one place where the model must be general (ODF `num-format` covers `1`, `a`, `A`, `i`, `I`, `BAB ` prefixes and `display-levels`); implement it as data, not as branches.

**Size:** large (1.5–2 weeks, 6 commits as above).

---

## Plan 8 — OOXML structural fidelity (was PR H)

**Goal:** close the DOCX fidelity gap that makes the user say the format is "far from perfect" for exactly the right reason: it is proprietary, so our parser must be more careful, not less.
**Closes:** F-16, F-17, F-19, F-20 and the DOCX half of F-10/F-11.
**Depends on:** E (metrics) and G (shared numbering model, shared table geometry).

### Scope

1. **H-1 · Real style chain (F-16).** Parse `w:docDefaults` (`rPrDefault`, `pPrDefault`) and every `w:style`: `w:basedOn` chain, `w:name`, `w:link`, `w:rPr` (`w:rFonts` ascii/hAnsi/cs, `w:sz`/`w:szCs` half-points, `w:b`, `w:i`, `w:u`, `w:color`, `w:highlight`, `w:vanish`), `w:pPr` (`w:jc`, `w:spacing`, `w:ind`, `w:keepNext`, `w:pageBreakBefore`, `w:outlineLvl`, `w:tabs`). `pStyle` keeps the **DOCX style id** as the paragraph `styleName` (no more inventing `"Heading N"`), so `para1` resolves to the real 20 pt Aptos Display heading instead of the legacy 24 pt fallback, and body text resolves to docDefaults' 12 pt instead of 14 sp.
2. **H-2 · Run-level formatting (F-20).** Build `TextRun`s per `w:r` from the resolved character properties; `w:val="0"|"false"` becomes an explicit *negative* (extend `OfficeRuns.mergeRun` to honour negative flags, keeping the F-3 nullable-character-style behaviour); stop leaking a bold run across the rest of the paragraph; supply `currentRuns` to the paragraph that owns them (today they are discarded).
3. **H-3 · Real numbering (F-13 parity for DOCX).** Parse `word/numbering.xml`: `w:abstractNum`/`w:num`, `w:lvl` (`w:start`, `w:numFmt`, `w:lvlText`, `w:lvlJc`, `w:suff`, `w:ind`, `w:isLgl`), `w:lvlOverride`/`w:startOverride`, and resolve `w:numPr` (`w:numId` + `w:ilvl`) per paragraph into E-EN-4. Sample-6's 133 `w:numPr` paragraphs stop rendering as flat `"• "`.
4. **H-4 · Fields, TOC, hyperlinks (F-19).** `w:fldSimple`, `w:instrText`, `w:fldChar` become field metadata (never body text); TOC paragraphs render with `w:tab` leaders and the right indent; `w:hyperlink` keeps text + relationship target.
5. **H-5 · Table geometry (F-17).** `w:tblGrid`/`w:tblW`/`w:tcW` → column weights; `w:gridSpan`, `w:vMerge`, `w:tcPr` (borders, shading, margins, vertical align), `w:trPr` (`w:tblHeader` repeat on page breaks). Rendering and pagination use the same geometry as G-5.
6. **H-6 · Section geometry (O-03).** Pick the `w:sectPr` that actually governs each paragraph (`w:pPr/w:sectPr` starts a new section from that paragraph; the body-level `sectPr` closes the last one), so Sample-6's five sections paginate with their own page size/margins instead of one global box.
7. **H-7 · Page-break hygiene (O-02).** `w:lastRenderedPageBreak` is a hint, not a break: exclude it from pagination and from the editable plain text. Real `w:br w:type="page"` and `w:pageBreakBefore` are the authored breaks.

### Files
`data/OfficeDocumentParser.kt` (DOCX branch + `extractDocxStyles` → a proper `DocxStyles` reader), `data/DocxDocumentParser.kt`, `data/OfficeDocumentModel.kt` (optional link/field metadata), `data/OfficeRuns.kt` (negative flags), `data/LayoutEngine.kt` (sections), `data/Numbering.kt`.

### Tests
`DocxStyleChainTest` (`para1` → 20 pt Aptos Display bold; docDefaults → 12 pt), `DocxRunFormattingTest` (bold run does not leak; `w:val="0"` unbolds), `DocxNumberingTest` (Sample-6 multi-level labels, restart per numId), `DocxTableGeometryTest` (5 columns at declared widths, header row repeats), `DocxSectionGeometryTest` (5 sections → 5 boxes), `DocxTocTest`.

### Acceptance
* Sample-6.docx: heading sizes match the style sheet, body text is 12 pt, tables keep their columns, TOC entries render with page numbers, numbering matches Word, sections paginate separately.
* Sample-1/3/4/5.docx: no regressions; `w:numPr`-heavy Sample-1 keeps its lists.
* Both formats converge on the same page windows as G (Sample-6 in `15..26`, tightening later).

### Risk
Medium-high (biggest parser surface). Mitigation: keep the new reader side-by-side behind the existing entry point, with the old branch deleted in a final commit only after the sample suites are green.

**Size:** large (2 weeks, 7 commits).

---

## Plan 9 (planned, not scheduled here) — save round-trip integrity (was PR I)

O-01 is real and dangerous: `DocxDocumentParser.saveDocument(file, document)` regenerates `word/document.xml` and downgrades images to `"[Image: path]"` text; the ODT side regenerates `content.xml` with a reduced element set, while copying every other zip entry unchanged. Until a format-correct writer exists, a save can silently destroy content. The minimal guard ships **inside Plan 6** (refuse instead of degrade); the full writer (styles, numbering, tables, images, TOC) is a separate PR to plan after G/H, together with F-2's `OfficeDocElement` retirement.

---

## Appendix A — Sequencing, parallelism, verification

| Order | PR | Parallel with | Gate to enter the next |
|---|---|---|---|
| 1 | **Plan 4** chrome & input | **Plan 6** (image pipeline) | device checklist for findings 1–5, 7; Delivery Gate PASS |
| 2 | **Plan 5** metrics & pagination | — (touches every test window) | Sample-5 `15..21`, Sample-6 `15..26`, caret/selection suites green |
| 3 | **Plan 6** images & performance | **Plan 4** | Recents open: no blank frame, media self-heal proven |
| 4 | **Plan 7** ODF structure | **Plan 8** style-reader scaffolding (different files) | Sample-6.odt fidelity checklist; hyperlink text-count test |
| 5 | **Plan 8** OOXML structure | tail of **Plan 7** | Sample-6.docx fidelity checklist; both-format convergence |
| 6 | **Plan 9** save integrity | — | round-trip test: open → save → reopen preserves text, styles, tables, images |

**Verification protocol (per `AGENTS.md`).** No local JDK exists in this environment: every PR is verified through GitHub Actions (`gh run list -L 5`, `gh run watch`), with the unit-test job green before review and the nightly build green before the user retests. Manual device verification uses `docs/InkyC1Checklist.md`; its run has been deliberately postponed and should resume after **Plan 4** (chrome/input items), then again after **E** (zoom, caret, selection, session restore: page 15 at 170 %), and after **G/H** (save compatibility).

**Test matrix every PR must keep green**

| Sample | ODT checks | DOCX checks |
|---|---|---|
| 1 | headings, lists (`WWNum*`), 3 tables, images | headings `para1..`, lists, 3 tables |
| 2 | TOC, 11 frames, multi master pages | 2 tables, 12 drawings, 15 hyperlinks |
| 3 | 186 paragraph styles, footnote, chapters | `style1..` heading sizes, footnote |
| 4 | TOC, 7 master pages, 23 links | 191 styles, heading 6..9, 2 footnotes |
| 5 | 123 lists, 2583 spans, TOC, Object 1 | `Judul1..`, 75 `w:numPr`, 5 sections |
| 6 | 45 headings, 115 lists, 1 table, TOC, 5 sections, 3 images | 371 paragraphs, TOC, 133 `w:numPr`, 5 `sectPr`, 3 images |

---

## Appendix B — What these plans deliberately do not do

* No new third-party dependency (no XML/zip library): `ZipSafe` + XmlPullParser + stdlib stay (`AGENTS.md`).
* No UI copy outside `values/strings.xml`; no Indonesian strings in the app layer (audit-003 P2-2).
* No `.hyph` work: Indonesian hyphenation is out of scope (S6 decision); correctness of wrapping comes from metrics, not from dictionaries.
* No spreadsheet/presentation fidelity (Cellina/Slidia samples stay in a later stage).
* No emoji/Wingdings glyph work beyond mapping Symbol/Wingdings to the bundled `opens___.ttf` (audit-003 residual).
* No change to the FCT interaction model beyond making it appear in Viewer; the Compact/Full rules from PR C stand.

---

## Appendix C — Definition of done

1. Sample-6 renders in Papirus within ±20 % of the M365 page count first, ±10 % after Plan 9, with every fidelity item from the user's list resolved or explicitly deferred with a reason.
2. Sample-1…5 keep their heading, navigation, pagination and image behaviour (windows tightened, never loosened silently).
3. `docs/InkyC1Checklist.md` runs end-to-end on device after Plan 4 and Plan 5, with results recorded in the next audit file.
4. Every UI-touching PR carries an `antislop` Delivery Gate PASS with evidence, and `DESIGN.md` tokens are respected (module accent, 48 dp targets, tonal elevation only, no unbranded colours).
5. Each PR body states which findings (F-xx) it closes and which it defers, so the audit stays the single source of truth.
