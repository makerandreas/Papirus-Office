# Papirus Office Writer: Fix Strategy, Plans 4 to 9 (was PRs D to I)

**Date:** 2026-09-24
**Input:** `anti-slop/audit-005-2026-09-24.md` (findings F-01 … F-20, observations O-01 … O-06) and `anti-slop/audit-006-2026-09-24.md` (screenshot findings F-21 … F-31, compliance sweep)
**Numbering:** this document holds **plans 4 to 9**; it was written as PRs D to I and the letters are kept in parentheses for traceability. Sub-item IDs (`D-1`, `E-EN-2`, `F-3`, `G-1`, `H-4` …) are unchanged, so `letter-n` reads as `plan-n item` (D-1 = Plan 4 item 1, E-2 = Plan 5 item 2, F-3 = Plan 6 item 3, G-1 = Plan 7 item 1, H-4 = Plan 8 item 4). The index is `anti-slop/plan-01-master-index.md`.

**Writer Guide reference:** how these plans serve WG 24.8 Chapter 1 and `docs/InkyC1Checklist.md` is mapped in `plan-01-master-index.md` §3 and §4; the short version is that Plan 5 carries the status-bar page count and the caret/layout guards, Plans 7 and 8 carry the Navigator's categories and the checklist's Save Compatibility item, and Plan 9 carries the lifecycle items.

**Baseline:** post-PR-C nightly, `main` d1105ce (PRs #7 A, #8 B1, #9 B2, #10 C)
**Deliverable of this document:** an ordered, reviewable PR split with scope, root causes closed, files, tests, acceptance criteria, and risk. It changes no code by itself.

> **Read with:** `AGENTS.md` (branch/CI rules, en_US strings, 48 dp targets), `DESIGN.md` (M3 Expressive tokens, dial ENERGY 2 / RHYTHM 1 / MOTION 3), `antislop.md` + the five skills (Modal UI changes must pass the Delivery Gate), `docs/odf` ODF 1.4 Part 1 (authoritative for every ODF decision), ECMA-376 §17 (authoritative for every OOXML decision). Path corrected 2026-09-28 (Plan 1, `audit-012` P1-06): the ODF parts are under `docs/odf/`, and the LibreOffice guide PDFs are under `docs/lo-guides/`.

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

## Plan 4: Viewer/Editor chrome and input fixes (was PR D)

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

## Plan 5: Layout metrics and pagination (was PR E, the page-count plan)

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

> **5B implementation record (PR #16, 2026-09-27).** Fixture re-baseline. Every `tests/inky` `.docx` was re-saved by Microsoft 365 and every `.odt` by Collabora Office 26.04, so the reference page counts now come from `docProps/app.xml` `<Pages>` and `meta.xml` `meta:page-count` instead of a third-party estimate. `SampleMatrix` carries both references and the per-format windows (DOCX 12-18, 18-28, 18-26, 8-12, 15-21, 15-26; ODT 12-18, 18-28, 18-26, 9-13, 15-23, 17-27). Evidence: `audit-008`, which supersedes `audit-007` §1 and §11.3 on every regenerated file. The blank `untitled.od{t,s,p}` packages stayed non-conformant at this point; their replacement is recorded under 5e.
>
> **5C implementation record (PR #17, 2026-09-27).** Documentation refresh, no product code. `DESIGN.md` v3.0 with pinned editions in front matter, a Create New truth pass, and the LibreOffice 26.2 re-citation. Evidence: `audit-009`. It assigns two obligations to Plan 1: re-map the historical 24.8 chapter map (`DESIGN.md:227`) and re-map the nine Sidebar decks and the accessibility-check field rather than claiming they exist (`DESIGN.md:175`).
>
> **5D implementation record (PR #18, 2026-09-27).** Breaks and defaults. Fake breaks out, authored breaks in; a body rectangle derived from the declared page and margins; a metric style chain so that spacing, indents and line height resolve through the cascade instead of a constant; F-21's defaults now come from the file. Evidence: `audit-010`. Its Sample-3 zero-margin and Sample-4/5 fixed-header examples are **not authoritative** after the 5b re-baseline (`audit-011` §9.2).
>
> **5E implementation record (PR #19, merge `439ed05`, 2026-09-28).** Load-bearing measurement, fragment pagination and shared input. Executed in five batches, evidence `audit-011` and the record in `plan-5e-progress.md`:
> 1. **Baseline repair.** Exact-case `templates/Untitled.od{t,s,p}` lookup with lowercase cache names; the styled-template and host-filesystem fallbacks removed, so a missing asset fails instead of opening a different document. `CreateNewDocumentTest` asserts byte identity, mimetype, content and manifest version 1.4, one body element, the ODS sheet and ODP slide counts, and cache replacement. Twelve fixture hashes, producers and reference counts recorded in `tests/inky/fixture-identities.properties` and guarded by `FixtureIdentityTest`.
> 2. **Semantics.** ODF `fo:break-after` and master-page assignments become authored boundaries; `auto` clears inherited before/after and keep flags; DOCX `atLeast` is a minimum distinct from `exact`; explicit `0`/`false`/`off` reset `keepNext` and `pageBreakBefore`, including inherited docDefaults; ODF tabs, keep-together, widows and orphans, and DOCX tab settings, keep-lines and widow-control enter the cascade.
> 3. **Measurement.** `TextMetrics` replaces raw Paint sizing and the character-count fallback. Cache keys include text and runs, resolved paragraph and character styles, and body width; the backend is immutable per engine. Tabs, indents, hyphenation, grapheme boundaries and caret advances are measured, and wrapping is lossless with respect to the source range.
> 4. **Fragments.** Lines carry element identity, source and line ranges, and continuation flags; pages end for an explicit reason (`END`, `OVERFLOW`, `AUTHORED`, `KEEP`); widow, orphan and bounded keep chains are enforced; navigation keeps both the first page and every occupied page. DOCX inline page breaks stay as offsets inside one paragraph, and section kinds are retained at the start of the section they describe.
> 5. **Input parity.** Viewer and Editor share one input field per logical paragraph. `ParagraphProjection` is a `VisualTransformation`, so inserted line breaks and page gaps are display-only and never enter the edit value, the undo history or the IME. The alternate flow view is explicitly unavailable (`InkyModule.kt:124`, `R.string.inky_flow_view_unavailable`) instead of bypassing the pipeline.
>
> **Acceptance.** CI run 36377627605: 247 tests, zero failures, errors or skips. `PaginationFidelityTest` puts all twelve fixtures inside their windows **and** asserts per-element source coverage and fragment adjacency. Measured ODT 15/23/18/10/18/19 and DOCX 15/24/21/11/19/22 under the table advance backend, against references 15/23/22/11/19/22 (ODT) and 15/23/22/10/18/21 (DOCX). No window was widened, tightened or added, and no hint or spacing constant was used to reach one. Sample-3 ODT (18 against 22) is the weakest pair and is the structural question Plans 7 and 8 must answer. Device glyph and Compose parity is outside this record by the owner's deferral.

**Amended 2026-09-28 (Plan 1).** Plan 5 is complete. The acceptance lines above that say "for both formats" stay as the design record; the enforced reality is the per-format windows in `SampleMatrix` plus the source-coverage assertion. Real Typefaces in Plan 10 will move these counts again, and windows tighten only with measured support.

---

## Plan 6: Image pipeline and load performance (was PR F) — COMPLETE

**Goal:** images appear immediately, at the right size, and stay there.
**Closes:** F-07, F-18, the "never load" half of finding 5.
**Depends on:** E (extents are layout units).

### Scope

1. **F-1 · Extents are parsed for both formats, in the live path.** OOXML: read `wp:extent cx/cy` (and `a:ext` for legacy `v:shape`) inside `OfficeDocumentParser`'s DOCX branch, converted through `emuToUnits`. ODF: keep `svg:width/height` but store layout units (`OdfFrameContext` already does this; align rounding). `ImageElement`/`OfficeImage` carry `widthUnits`/`heightUnits`; `LayoutEngine` reserves exactly that; the renderer scales from it (fixes F-18: 7.735 cm ↔ 2784475 EMU now produce the same 292 dp on both sides). Then **delete** the dead extent plumbing so it cannot mislead again: `DocxDocumentParser.parseDocxFile`, `DocxParseResult.imageExtents`, `InkyModule.docxExtents` (assigned 5 times, read 0 times).
2. **F-2 · Media store instead of `cacheDir`.** Extract into `filesDir/media/<sha256(path:len:mtime)>/` with a small manifest (package path → size and hash), a 128 MiB per-document media cap, and a 256 MiB global LRU cap; `ZipSafe.MAX_IMAGE_BYTES` remains the per-image limit. If a referenced file is missing when the document is opened or its parsed model is reused, re-extract only that package entry instead of leaving a stale `[Image]` placeholder. This is the fix for "sometimes the image never loads".
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

### Implementation record (2026-09-30, Plan 6 COMPLETE)

Plan 6 shipped across four increments (6A in PR #22, 6B in PR #23, 6C in PR #24, and 6D closeout in PR #25):

1. **Plan 6A (PR #22, source `2ee77e3`, merge `eff150d`):**
   * Implemented F-1 (DOCX `wp:extent` and legacy `a:ext` EMU parsing via `LayoutUnits.emuToUnits` into `OfficeImage` layout units; deleted `DocxParseResult.imageExtents` and `InkyModule.docxExtents`) and F-5 (`EmbeddedImageSaveGuard` refusing DOCX saves with embedded images, `OdtDocumentWriter` failing closed when an image payload is missing or unsafe while reusing verified original package media, and `DocumentSerializer` propagating save failure).
   * Added `Plan6ImageFoundationTest` (5 tests in 6A).
   * CI: PR run `36578390234` passed **252 unit tests, 0 failures** (Unit Tests step 3m 49s / job 4m 21s; Build step 5m 47s / job 6m 53s; artifact `11044382829`). Post-merge `main` run `36579308372` passed Unit Tests (step 5m 48s / job 6m 19s) and Build (step 5m 30s / job 6m 25s; artifact `11044710903`).
2. **Plan 6B (PR #23, commits `042870d`, `63eb3f4`, `1154465`, merge `ac713e4`):**
   * Implemented F-2: durable media storage under `filesDir/media/<sourceKey>/` with `manifest.properties`, 128 MiB per-document cap, 256 MiB global LRU cap, `ZipSafe.MAX_IMAGE_BYTES` per-image cap, collision-safe package-path storage, and self-healing re-extraction on both fresh parse and in-memory `OfficeParsedDocument` cache hits (`OfficeDocumentParser.ensureExtractedMedia`).
   * Added `DocumentMediaStoreTest` (8 tests covering persistence, recovery after file deletion, LRU eviction, source changes, duplicate basenames, and configured limits).
   * CI: intermediate PR run `36587725933` on `63eb3f4` passed **260 unit tests, 0 failures** (Unit Tests step 4m 43s / job 5m 16s; Build step 5m 34s / job 6m 00s; artifact `11047782909`); final PR head `1154465` passed in run `36588655662` (**260 unit tests, 0 failures**; Unit Tests step 5m 43s / job 6m 13s; Build step 4m 37s / job 5m 18s; artifact `11048120534`); post-merge `main` runs `36589704465` (Unit Tests step 3m 50s / job 4m 23s; Build step 5m 35s / job 6m 21s; artifact `11048501207`) and `36589734261` (Unit Tests step 4m 14s / job 4m 51s; Build step 5m 53s / job 6m 43s) passed both jobs.
3. **Plan 6C (PR #24, commits `8c858b6`, `bfd2ee6`, `5a693b4`, `7f0d05f`, merge `bdd2724`):**
   * Implemented F-3 (`DocumentImages` single-source box/decode-size/cache-key resolver, `ImagePredecoder` pre-decoding the first two pages' images on `Dispatchers.IO` during layout, `DocxEmbeddedImage` with explicit reserved box, `ContentScale.Fit`, `crossfade(false)`, zoom-independent `decodeSizePx` capped at `2048 px`, and `DocumentImageTags.IMAGE/PENDING/MISSING`) and F-4 (`LoadingStage` enum `OPENING_PACKAGE`, `VALIDATING`, `EXTRACTING_MEDIA`, `READING_STYLES`, `READING_BODY`, `CACHED`, `LAYOUT`, and removal of the `500+500+400 ms` artificial `delay()` calls in `InkyModule.runDocumentLoading`).
   * Added `DocumentImagesTest` (6 tests), `DocumentImagePresentationTest` (4 tests), and `Plan6cLoadingProgressTest` (5 tests).
   * CI: PR head `7f0d05f` passed in run `36708650093` (**275 unit tests, 0 failures**; Unit Tests step 3m 53s / job 4m 39s; Build step 5m 38s / job 6m 15s; artifact `11093720666`); post-merge `main` run `36710138324` passed Unit Tests (step 4m 47s / job 5m 31s) and Build (step 3m 31s / job 4m 34s; artifact `11094391272`); `main` run `36724913329` on `1d4afcd` passed **275 unit tests, 0 failures** (Unit Tests step 4m 19s / job 5m 06s; Build step 5m 39s / job 6m 27s; artifact `11101629289`).
4. **Plan 6D (PR #25, closeout gate):**
   * Completed the remaining F-1/F-5 dead-code removal in `DocxDocumentParser.kt`: deleted unused private `parseDocxFile`, `parseOdtFile`, and `imageExtractor`, and replaced the unreachable `[Image: path]` branch in `writeDocxElement` with a fail-closed `IllegalStateException`.
   * Unified `LayoutEngine.kt` image box resolution with `DocumentImages.box(widthDp, heightDp)` so the paginator and `RenderImage` share one source of truth (Cross-plan invariant #1).
   * Added `tallerThanPageImageStillPaginatesAndUsesDocumentImagesFallback` to `Plan6ImageFoundationTest` (6 tests in `Plan6ImageFoundationTest`, **276 unit tests** total across 48 suites, covering `PaginationImageTest` from the Plan 6 test table).
   * Updated `scripts/ci-dump-comment.py` to emit per-suite and total JUnit execution times (`time (s)`) and `Plan6cLoadingProgressTest` `<system-out>` open/first-layout timings in PR CI comments.
   * **Re-confirmed 12-file `SampleMatrix` page-count matrix** (identical across 6A run `36578390234`, 6B run `36588655662`, 6C run `36708650093`, and 6D PR #25, with `0` empty pages in all 12 files):
     * **ODT measured:** `Sample-1 = 15` (ref 15, window `12..18`), `Sample-2 = 23` (ref 23, `18..28`), `Sample-3 = 18` (ref 22, `18..26`), `Sample-4 = 10` (ref 11, `9..13`), `Sample-5 = 18` (ref 19, `15..23`), `Sample-6 = 19` (ref 22, `17..27`).
     * **DOCX measured:** `Sample-1 = 15` (ref 15, window `12..18`), `Sample-2 = 25` (ref 23, `18..28`, +1 vs 5E's 24 from 12 parsed `wp:extent` drawings up to `468.1 x 307.1` units), `Sample-3 = 21` (ref 22, `18..26`), `Sample-4 = 11` (ref 10, `8..12`), `Sample-5 = 19` (ref 18, `15..21`), `Sample-6 = 23` (ref 21, `15..26`, +1 vs 5E's 22 from 3 parsed `wp:extent` drawings at `292.3 x 292.3`, `165.0 x 427.0`, `230.9 x 470.9` units).
   * CI: PR #25 head `efa6fd7` passed in run `36730234004`: **276 unit tests, 0 failures, 0 errors, 0 skipped (28.07 s of JUnit time across 48 suites)**; Unit Tests step 4m 55s / job 5m 45s, Build step 4m 41s / job 5m 20s, artifact `11105575967` (`159,088` B). This is the first run whose PR comment carries the per-suite `time (s)` column and the `Plan6cLoadingProgressTest` stdout: `Sample-6.odt parse=62 ms, layout=34 ms, pages=19, predecodeWindow=0..2, predecodeImages=1` and `Sample-6.docx parse=38 ms, layout=32 ms, pages=23, predecodeWindow=0..2, predecodeImages=1` (CI runner timings, not a device measurement). The busiest suites were `BodyRectTest` 7.14 s, `DocumentImagePresentationTest` 3.97 s, `CreateNewDocumentTest` 2.96 s and `Plan5ElementDumpTest` 2.44 s.
   * **Reforecast superseded 2026-10-04:** Plan 7A used `#26`. Plan 7B landed as `#27`, 7C as `#28`, 7D as `#29`, and the post-7D regression repair took `#30`. The current sequence is Plan 7E (`#31`), 7F (one slot after 7E), 8A (`#32`), 8B (`#33`), Plan 9 (`#34`), Plan 10 (`#35`-`#36`), and Plan 11 (`#37`-`#41`). Plan IDs remain authoritative. Physical-device verification remains scheduled after Plan 11 per the owner's deferral.

---

## Plan 7: ODF structural fidelity (was PR G)

**Goal:** Sample-6.odt reads like the Writer Guide document it is: numbered headings, real lists, a table with correct columns, a TOC, and hyperlinks that keep their text.
**Closes:** F-08 (font identity), F-09, F-11, F-12, F-13, F-14, F-15, O-03, O-04, O-05.
**Depends on:** E (E-EN-4 numbering model, E-EN-5 font registry).

### Authoritative four-PR continuation (2026-10-03)

The earlier combined Plan 7B package is superseded. After 7A, this plan proceeds through exactly four plans and four PRs:

| Plan | Item ownership | Gate |
|---|---|---|
| **7B - canonical semantic model and ODT importer convergence** | G-0: format-neutral sidecars/value types; one `OdtImportPipeline`; `writer.OdtDocumentParser` delegation; context-independent diagnostics; metadata/package/source-feature preservation; fail-closed modified-save capability | all ODT entry points produce one semantic result for all six fixtures; no duplicate XML parser remains |
| **7C - authored indexes, named section ranges, Navigator, status** | G-2 and G-7 | authored snapshots and section identities populate sidecars and Navigator/status context without duplicating body flow |
| **7D - tables end to end** | G-5 | table grid, repeats, spans, styles, intrinsic measurement, row pagination, rendering, and cell hit-testing share one geometry model |
| **7E - font faces and final calibration** | G-6 plus ODF font-face declaration/alias parsing and the post-structure pagination remeasurement | metrics and display resolve the same declared face; fixture windows are recalibrated from evidence |

Detailed split evidence and non-goals are in `audit-014-2026-10-03-plan-7b-convergence.md`. Structural regeneration remains Plan 9; 7B refuses a modified save when the current writer would discard indexes, sections, or source table structure.

### Scope (cumulative through 7E; ownership fixed by the table above)

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

### Implementation record (2026-10-02, Plan 7A shipped; 7B-7E follow)

Plan 7A landed on the session branch in five commits, after `audit-013`'s measurements were re-derived from the raw package XML (`styles.xml` list/outline styles, `content.xml` structure). What shipped, item by item:

1. **G-1 · Real numbering (`data/Numbering.kt`, new).** `NumberingLevelSpec`, `NumberingSpec`, `NumberingCounterState` (the roadmap's E-EN-4 `CounterState` type alias) and `NumberingFormatter` implement `1`, `a`, `A`, `i`, `I`, `none`/empty, `num-prefix`, `num-suffix`, `display-levels`, `start-value` and deeper-level resets as data. `SvXMLImport.parseOdfStyles` reads `<text:list-style>`, `<text:outline-style>`, `<text:list-level-style-number>`, `<text:list-level-style-bullet>`, `<text:outline-level-style>`, `<style:list-level-properties>` and `<style:list-level-label-alignment>` (including the per-level `style:name` reference to a `text`-family style that carries the label font), and records `style:list-style-name` on every paragraph style. `DocumentStyles` carries `listStyles` and `outlineStyle`; `ParagraphStyle` carries `listStyleName` (`null` inherits, `""` suppresses).
2. **G-1 resolution paths.** Three paths feed one counter: `text:list` → `text:style-name` (with `text:continue-numbering` and `text:continue-list` sharing one counter per style across sibling lists), a paragraph style's `style:list-style-name` (walked through the auto-style parent chain, so `P5` → `Makalah_20_Default` resolves), and `<text:outline-style>` for `text:h` headings that no list declares (Sample-6's `BAB n` and `n.m`). `text:is-list-header` and an item's first `<text:p>` consume the counter; later blocks in the same item do not.
3. **G-3 · Hyperlinks.** `OdfHyperlinkContext` preserves `xlink:href` across direct characters, nested `<text:span>`, `<text:s>` and `<text:tab/>`, and marks the runs underlined; `OdfSpanContext` gained `inheritedHyperlink`/`fallbackStyleName` so a span inside `text:a` keeps both the link and its own formatting. `OfficeTextRun.hyperlink` is now populated by `toOfficeDocument()` and reaches the Navigator's hyperlinks arm (still classified not-yet-readable in 7A, see item 6).
4. **G-4 · Heading and list-item runs.** `OdfHeadingContext` collects runs (with the same span/hyperlink/bookmark arms as paragraphs); `OfficeDocumentElement.Heading` and `.ListItem` carry `runs`; `toOfficeDocument()` maps them instead of writing `emptyList()`/one synthetic run. `ListItem` also carries `styleName`, `labelFontSizeSp`, `labelFontFamily` and `level`, and the renderer and paginator use the label font for the prefix (`LayoutEngine.paragraph()`, `ParagraphText`, `ParagraphEditField`, `ParagraphSelectField`).
5. **G-4b · Bookmarks.** `text:bookmark`, `-start` and `-end` are consumed at paragraph, heading, list-item, span, hyperlink **and** table-cell depth; names are de-duplicated per element and per document. `OfficeDocument.bookmarks`, `OfficeDocumentElement.*.bookmarks`, `OfficeDocument.bookmarks` and `OdtDocumentParser` (writer path) all expose them. `NavigatorCategories` reclassifies `"bookmarks"` as `PARSED_DOCUMENT_CLASS` and `DocumentIndexEngine` registers one row per unique anchor (including `OfficeParagraph.bookmark`), so the Navigator renders real counts and the existing `NavigatorCategoryHonestyTest` now enforces the new classification (its readable-category arm demands a producer, and the sheet renders the count instead of `null`).
6. **Deliberately not in 7A:** the hyperlinks category stays `NOT_READABLE_YET` because `OfficeHyperlink` is still never constructed by a parser arm (only runs carry links). G-2 moves to 7C, G-5 to 7D, G-6 to 7E, and G-7 to 7C. The TOC snapshot's `<text:table-of-content-source>` template is ignored through a new `OdfIgnoreSubtreeContext` so its placeholder paragraphs cannot leak into the body.
7. **Tests added (pure JVM + Robolectric, no device claims):** `OdtListNumberingTest` (counter arithmetic incl. deeper-level resets; a synthetic auto-style chain producing `BAB I PENDAHULUAN`, `1.1`, `1.2`, `BAB II`, `2.1`, `2.1.1`; Sample-6 normalized headings `BAB 1 PENDAHULUAN`, `1.1`, `1.2`, `BAB 2 PEMBAHASAN`, `2.1.1`, `BAB 3 PENUTUP`, `3.1`, `3.2.1` with the three unnumbered headings intact; Sample-6 ordered labels `1.`-`10.` plus the bare `num-suffix`-less `1`-`4` form, bullet labels without `··` doubling and with the level's declared label family (`Symbol` on the bullet level, `Aptos` on the `ListLabel_*` styles the numbered levels reference, neither declaring a size; corrected in audit-013 after the first reading claimed 10 pt); Sample-4 `A.`/`B.`/`C.` list-wrapped headings), `HyperlinkFidelityTest` (`text:a` with a nested bold span and a tab keeps one href and underlines; Samples 2/4/5/6 expose at least 15/2/14/45 linked elements with run/text synchronization, plus `OdtDocumentParser` parity), `HeadingRunsTest` (Sample-4/5 inline italic heading runs; Sample-6's 12 bold-span list items render through `OfficeRuns`), `OdtBookmarkTest` (all six ODT files: 0/15/0/22/14/46 anchors in `OfficeDocument`, the Navigator index and `OdtDocumentParser`).
8. **CI evidence:** recorded in the PR body and in the `scripts/ci-dump-comment.py` comment on the pull request (unit-test count, per-suite times, the twelve-fixture page-count matrix and the `SampleMatrix` windows). No local JDK exists in the sandbox, so the GitHub Actions run is the compile and test evidence.

**Sample-4 numbering note (recorded so later Plan 7 work does not "fix" it).** Sample-4's list-wrapped headings use `WWNum1` level 1 (`A`, `1`, `a` for levels 1 to 3): the outer `<text:list>` sits at level 1 for the heading even though the heading is `text:outline-level="2"`, and the nested question lists advance level 2. That is the authored structure, so the headings read `A. Latar Belakang`, `B. Rumusan Masalah`, `C. Tujuan` (`OdtListNumberingTest`). Sample-6's sub-headings come from `<text:outline-style>` instead, because they are not inside `<text:list>` at all: `display-levels` on levels 2 and 3 produce `1.1` and `2.1.1` (audit-013 §3).

### Implementation record (2026-10-03, Plan 7B)

Plan 7B establishes the boundary the next three plans consume without implementing their behavior:

1. `DocumentSemantics.kt` defines half-open body ranges for authored indexes and named sections, table geometry/style value types, font-face metadata, and ODT source-feature provenance. `OfficeParsedDocument`, `OfficeDocument`, and both conversion directions preserve those values, metadata, styles, bookmarks, runs, images, and page breaks.
2. `OdfImportDiagnostics` removes Android `Context` from `SvXMLImport`'s semantic contract. `AndroidOdfImportDiagnostics` retains file-backed runtime logging; pure and compatibility callers use the silent or injected implementation.
3. `OdtImportPipeline` is the one ODT ZIP/package and semantic path. It preserves all package entries, original XML parts, metadata/statistics, extracted media mappings, styles, bookmarks, parser failure state, and `OdtSourceFeatures`.
4. Runtime ODT open delegates to the pipeline. ODS/ODP behavior is unchanged. The 741-line duplicate `writer.OdtDocumentParser` becomes a small compatibility facade with no XML semantics.
5. `OdtDocumentWriter.saveCapability` reports and enforces a closed decision for modified unsupported authored indexes, named sections, and source/advanced table structure. Unmodified package preservation remains exact; Plan 9 still owns regeneration.
6. `Plan7bSemanticImportTest` gates all six ODT fixtures for runtime/facade parity, package metadata and feature preservation, pure diagnostics/failure behavior, canonical sidecar/table adaptation, exact unmodified `content.xml`, modified-save refusal, and source architecture. Full CI evidence is recorded in `audit-014` and the PR.

Explicitly deferred: populating/rendering index and section sidecars (7C), populating and consuming table geometry (7D), and parsing/resolving font faces plus pagination calibration (7E).

### Plan 7C record (forecast PR #28, target until merged)

G-2 and G-7 are implemented for ODF. Dedicated contexts record half-open ranges for `text:section` and all seven index elements while their paragraphs stay in normal body flow. Index entries carry their text, a level taken from the entry template through the paragraph style chain, the link anchor and the authored page label (roman `ii`/`iii` stay strings). The Navigator lists indexes with their entries and lists sections nested by depth. Hidden sections are greyed and their jumps land on the nearest visible position. TOC links are grouped under their index. The status bar shows the index type inside an index, and otherwise the innermost section name joined with the existing detail. FCT Compact offers "Go to entry…" on linked TOC entries. Tab stops, leaders and hidden-section rendering move to 7E. The full evidence table and known limits are in `audit-015` §9.

### Plan 7E record (PR #31, in flight 2026-10-04)

G-6 is implemented for ODF on `arena/01a1077a-papirus-office` in two code commits plus a records commit, rebuilt from roadmap v2 §4.7d after the first build was lost with its sandbox (audit-017 section 1).

* `784e9d3` reads `office:font-face-decls` from `styles.xml` and `content.xml` into `DocumentStyles.fontFaces` (ODF 1.4 Part 3 3.14, 19.502.3, 19.532). The raw `svg:font-family` value is stored untouched, because `style:name` is an identifier and not a family; the first declaration of an alias wins and a declaration with a blank name or family is skipped.
* `c363ce3` adds `FontFaceResolver` and one `resolveFontFamily` helper used by both text-properties readers, so the resolved family is the value in `ParagraphStyle.fontFamily` and the single input to `TextMetrics.forStyle` and `OfficeRuns.fontFamilyFor`. No renderer-only alias map exists.
* Tests: `Plan7eFontFaceImportTest` (4) covers both XML parts, per-fixture inventories (9/10/12/12/10/13) and the precedence rule; `Plan7eFontResolutionTest` (8) covers alias versus direct convergence, `Times New Roman1` to Liberation Serif (`BUNDLED_METRIC_COMPATIBLE`, `assetStem = "LiberationSerif"`), the three Aptos aliases to Martel Sans (`BUNDLED_STAND_IN`, `metricCompatible = false`), unusable declarations falling back to the raw name, and the shared measurement/display input.

CI run `37213135047` (which measured the pre-fix head `28a37fb`; commit 2 is `c363ce3` after the expectation fix) compiled the change on the first try: **342 tests across 63 suites, 1 failure**, Build job green. The single failure was the resolution test's own expectation for `FontChoice.requested`; the corrected test asserts the resolved name (`Times New Roman`) for the resolved path and the alias (`Times New Roman1`) for the raw path. The calibration is measured and recorded: the twelve-file page matrix is identical to the PR #30 baseline (ODT `14/23/21/10/18/20`, DOCX `15/25/25/11/19/24`), the only dump change is Sample-6.odt's body style moving from `(Aptos1)` to `(Aptos)`, and `Aptos1` now occurs 0 times in the dump against 1 before. `Plan7dTableLayoutTest` and `PaginationFidelityTest` are green in the same run.

Decision D3 was reversed by the owner on 2026-10-04: the modified-save refusal for declared font faces is deferred to Plan 9, which owns writer regeneration. The recorded loss is that a regenerated `content.xml` would drop the declaration table; no writer path is touched by 7E. Items 5 and 6 of §4.7d (tab leaders, hidden-section layout) are split out to the new Plan 7F (`plan-7f-2026-10-04-tab-stops-and-hidden-sections.md`).

---

## Plan 8: OOXML structural fidelity (was PR H)

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

### Side-quest design note (2026-10-02, ClearPDF analysis for Plans 8A and 8B)

* **Bounded namespace-agnostic XML subtree helper:** When implementing `H-1` (`word/styles.xml`) and `H-3` (`word/numbering.xml`), use a small bounded element reader (`OoxmlNode` / `XmlEl` pattern with local-name lookup and a swappable `XmlPullParser` factory for JVM tests) for `<w:style>`, `<w:docDefaults>`, `<w:abstractNum>`, and `<w:num>` blocks rather than adding more streaming state flags to the 1,900-line `OfficeDocumentParser.kt` loop.
* **Explicit tri-state boolean toggle helper (`H-2`, `E11-05`):** Parse ECMA-376 toggle properties (`w:b`, `w:i`, `w:strike`, `w:vanish`, `w:keepNext`, `w:pageBreakBefore`) as tri-state values (`Boolean?`: `null` when element is absent so parent style inherits; `false` when `w:val="0"` or `"false"`; `true` when `<w:b/>` has no `w:val` or `w:val="1"|"true"`).

---

## Plan 9 (planned, not scheduled here): save round-trip integrity (was PR I)

O-01 is real and dangerous: `DocxDocumentParser.saveDocument(file, document)` regenerates `word/document.xml` and downgrades images to `"[Image: path]"` text; the ODT side regenerates `content.xml` with a reduced element set, while copying every other zip entry unchanged. Until a format-correct writer exists, a save can silently destroy content. The image guard shipped **inside Plan 6**; Plan 7B extends the closed capability boundary to unsupported authored indexes, named sections, and source table structure. The full writer (styles, numbering, tables, images, TOC) is a separate PR to plan after G/H, together with F-2's `OfficeDocElement` retirement.

### Side-quest design note (2026-10-02, ClearPDF analysis for Plan 9 pre-change gate)

* **Non-destructive ZIP preservation + surgical XML section splicing:** Modeled on ClearPDF's `XlsxWriter` and `StyleTable` (MIT License), Plan 9's writers should preserve every untouched ZIP entry byte-for-byte (`word/styles.xml`, `word/numbering.xml`, `word/theme/*`, `word/fontTable.xml`, `docProps/*` for DOCX; `styles.xml`, `meta.xml`, `settings.xml`, `Thumbnails/*` for ODT) and splice only the modified body section while retaining the original terminal `<w:sectPr>` (when section geometry is unchanged) and existing automatic/named style definitions.
* **Append-only style derivation and relationship registration:** Never re-index or overwrite existing style definitions in `styles.xml` or `content.xml` `<office:automatic-styles>`. Append newly derived formatting styles to the tail under collision-safe names (`P_papirus_*`, `T_papirus_*`), upgrade generated ODT roots to `office:version="1.4"` (ODF 1.4 Part 3 section 19.390), and register newly embedded images in `word/_rels/document.xml.rels` + `[Content_Types].xml` (DOCX) or `META-INF/manifest.xml` (ODT) without dropping existing entries.

---

## Appendix A: Sequencing, parallelism, verification

| Order | PR | Parallel with | Gate to enter the next |
|---|---|---|---|
| 1 | **Plan 4** chrome & input | **Plan 6** (image pipeline) | device checklist for findings 1–5, 7; Delivery Gate PASS |
| 2 | **Plan 5** metrics & pagination | none (touches every test window) | Sample-5 `15..21`, Sample-6 `15..26`, caret/selection suites green |
| 3 | **Plan 6** images & performance | **Plan 4** | Recents open: no blank frame, media self-heal proven |
| 4 | **Plan 7A** ODF numbering/runs/links/bookmarks | none | shipped in PR #26 |
| 5 | **Plan 7B** canonical/import convergence | none | every ODT entry point agrees; unsupported modified saves fail closed |
| 6 | **Plan 7C** indexes/sections/navigation/status | none | authored snapshots and named ranges are navigable |
| 7 | **Plan 7D** tables end to end | none | one table geometry reaches layout, rendering, pagination, and hit-testing |
| 8 | **Plan 7E** fonts/calibration | none | declared aliases drive metrics/display; fixtures remeasured |
| 9 | **Plan 7F** leaders/hidden sections | none | leaders painted inside reserved advances; matrix unchanged |
| 10 | **Plan 8** OOXML structure | after **Plan 7E** | Sample-6.docx fidelity checklist; both-format convergence |
| 11 | **Plan 9** save integrity | none | round-trip test: open → save → reopen preserves text, styles, tables, images |

**Verification protocol (per `AGENTS.md`).** No local JDK exists in this environment: every PR is verified through GitHub Actions (`gh run list -L 5`, `gh run watch`), with the unit-test job green before review and the nightly build green before the user retests. Manual device verification uses `docs/InkyC1Checklist.md`; its run has been deliberately postponed and should resume after **Plan 4** (chrome/input items), then again after **E** (zoom, caret, selection, session restore: page 15 at 170 %), and after **G/H** (save compatibility).

**Test matrix every PR must keep green**

| Sample | ODT checks | DOCX checks |
|---|---|---|
| 1 | headings, lists (`WWNum*`), 3 tables, images | headings `para1..`, lists, 3 tables |
| 2 | TOC, 11 frames, multi master pages | 2 tables, 12 drawings, 15 hyperlinks |
| 3 | 186 paragraph styles, footnote, chapters | `style1..` heading sizes, footnote |
| 4 | TOC, 7 master pages, 23 links | 191 styles, heading 6..9, 2 footnotes |
| 5 | 123 lists, 2583 spans, TOC, Object 1 | `Judul1..`, 75 `w:numPr`, 5 sections |
| 6 | 45 headings, 23 `text:list` elements, 1 table, TOC, no `text:section`, 3 images | 371 paragraphs, TOC, 133 `w:numPr`, 5 `sectPr`, 3 images |

---

## Appendix B: What these plans deliberately do not do

* No new third-party dependency (no XML/zip library): `ZipSafe` + XmlPullParser + stdlib stay (`AGENTS.md`).
* No UI copy outside `values/strings.xml`; no Indonesian strings in the app layer (audit-003 P2-2).
* No `.hyph` work: Indonesian hyphenation is out of scope (S6 decision); correctness of wrapping comes from metrics, not from dictionaries.
* No spreadsheet/presentation fidelity (Cellina/Slidia samples stay in a later stage).
* No emoji/Wingdings glyph work beyond mapping Symbol/Wingdings to the bundled `opens___.ttf` (audit-003 residual).
* No change to the FCT interaction model beyond making it appear in Viewer; the Compact/Full rules from PR C stand.

---

## Appendix C: Definition of done

1. Sample-6 renders in Papirus within ±20 % of the M365 page count first, ±10 % after Plan 9, with every fidelity item from the user's list resolved or explicitly deferred with a reason.
2. Sample-1…5 keep their heading, navigation, pagination and image behaviour (windows tightened, never loosened silently).
3. `docs/InkyC1Checklist.md` runs end-to-end on device after Plan 4 and Plan 5, with results recorded in the next audit file.
4. Every UI-touching PR carries an `antislop` Delivery Gate PASS with evidence, and `DESIGN.md` tokens are respected (module accent, 48 dp targets, tonal elevation only, no unbranded colours).
5. Each PR body states which findings (F-xx) it closes and which it defers, so the audit stays the single source of truth.
