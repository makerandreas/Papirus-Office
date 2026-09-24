# Papirus Office — PR Roadmap for the Remaining Plans

**Date:** 2026-09-24
**Status:** **active execution schedule.** It turns plans 3, 5, 6, 7, 8, 9 and parked plan 10 into an ordered PR sequence with scope, tests, acceptance and gates. It changes no code by itself. Plan 4 is **consumed** (its scope landed as PR #11); plan 1 stays the living index and is updated by this document.
**Baseline:** `main` `e10f956` (PR #11, Plan 2 chrome), branch convention `arena/<session>-papirus-office`, CI-verified only (no local JDK; `gh` is the build evidence channel).
**Evidence base for this document:** every plan and audit in `anti-slop/` re-read this session; the cited code locations re-opened and re-verified in the working tree; all 12 files in `tests/inky` unpacked and re-inventoried (§2); WG 24.8 Chapter 1 and the Open XML SDK references re-checked online. Line numbers marked ✅ were verified on 2026-09-24 against `main` `e10f956`; older numbers in previous documents have drifted and are superseded by this file where they conflict.

---

## 0. Decisions taken by the user (2026-09-24)

These close the open questions of `audit-005` §6 and `audit-006` §5 that were still unanswered when this session started. They are binding for every PR below; a fresh session does not re-ask.

| Question | Decision | Consequence |
|---|---|---|
| Page-count tolerance (audit-005 §6 Q2) | **Staged windows.** Plan 5 lands with wide windows (Sample-5 `15..21`, Sample-6 `15..26` for both formats); after Plans 7/8 the windows tighten toward ±10 % of the M365 reference (18 / 21) and never below it. | E-6 stays as written; the tightening step gets its own commit inside Plan 8B/Plan 9, not inside the metrics PR. |
| Font supply (audit-005 §6 Q3) | **Display the bundled faces.** Times New Roman → Liberation Serif, Calibri → Carlito, Cambria → Caladea, Arial/Helvetica → Liberation Sans, Courier New → Liberation Mono, Symbol/Wingdings → OpenSymbol, in **both** pagination and painting, from the moment `FontRegistry` (E-EN-5) exists. | Plan 5 wires the mapping; Plan 7 G-6 makes the renderer consume it; Plan 10 A1 upgrades loading to real `Typeface`s and adds the A2 policy write-down and the A5 metrics-parity test. Plan 5 must not ship a metrics-only seam that display ignores. |
| TOC source (audit-005 §6 Q4) | **Authored snapshot now.** ODT TOC renders from the authored `text:index-body` with the file's own page numbers. Regeneration from the Navigator index is a follow-up feature after a field model exists. | G-2 scope unchanged; the `TableOfContent` element model is still built so Plan 9 can preserve it and a later plan can regenerate. No "Update Index" stub is added. |
| Save with unserialisable images (audit-006 §5 Q4) | **Refuse with a clear message.** A save that would degrade content fails loudly instead of writing `[Image: <device path>]` placeholders. | Plan 6 F-5 implements the refusal (en_US copy, `strings.xml`); Plan 9 removes the need by making images survive the round trip. |

Together with the three decisions already recorded in `plan-03` §0 (en_US everywhere; disabled ribbon tabs with an honest note; Plan 2 before Plan 3), **no decision is outstanding that gates any PR in this file.**

---

## 1. Where the work stands after PR #11 (verified, not assumed)

PR #11 merged Plan 2 (= Plan 4's scope). What it paid down, verified in the tree: the per-card `x / y` counter is gone from `LayoutDrivenDocumentRenderer`; the status bar is a 48 dp three-slot overlay (page range / words+chars / zoom or Edit); the Viewer FAB is deleted; `RendererFocusBridge` exists and the swallowed `catch` is gone; the Viewer selection now drives the FCT via `boundsInWindow`; the page stack renders with one fit scale and tokenised chrome; `Plan2ChromeTest` guards it. Plan-02 §7 is the authoritative record; its acceptance list (§5) is **still open on device** — the nightly built from PR #11 is the install for that pass, and checklist items 5, 8 and 9 resume with it.

What remains, re-verified this session (✅ = confirmed at the cited location today):

| Still open | Where | Owning PR |
|---|---|---|
| `fontSizeSp * 2.5f` measuring fudge | ✅ `LayoutEngine.kt:154` | 16 (5B) |
| Flat `elementGapDp = 12f` between all elements | ✅ `LayoutEngine.kt:107`, applied at `:331` | 16 (5B) |
| `StyleResolver` 14 sp default on null/miss (F-21) | ✅ `LayoutEngine.kt:61` | 16 (5B) |
| Text scales with `renderScale` while paper maps with `pageScale` (plan-2 recorded gap) | ✅ `LayoutDrivenDocumentRenderer.kt:149-150,243-245` | 15 (5A) |
| Table height `rows * 35 + 10`, all cells `10.sp` | ✅ `LayoutEngine.kt` (`rows.size * 35f + 10f`) | 19 (7B) / 21 (8B) |
| Eight ribbon tabs declared, only File and Home have content; literal "…options will be implemented soon." | ✅ `InkyModule.kt:2935`, `:3390` | 13 (3B) |
| Toolbar hub image/table/link/comment toast-only | ✅ `InkyModule.kt:2791,2798` (drifted from the audit's `2695-2704` after Plan 2) | 13 (3B) |
| Indonesian user-visible copy (`Draft Dokumen Baru`, `Inky_Dokumen.*`, `Ukuran font diubah ke …`, `Menempelkan sebagai …`, config-reset message, Drive button) | ✅ `InkyModule.kt:208,3347,3465,3476,3922`; `PapirusConfigManager.kt:382`; `HomeDashboard.kt:1273` | 12 (3A) |
| `contentDescription` literals (231 repo-wide / 72 inky) and `Toast` literals (316 / 57) | audit-006 §2.1 counts, unchanged | 12 (3A) |
| `values-in/strings.xml` has 27 keys; `values/strings.xml` 283 | ✅ counted this session | 12 (3A) keeps `values-in` frozen |
| Four user-visible em dashes (R-02) | `UniversalEmailSheet.kt:461`, `PapirusEmailEngine.kt:139,208`, `LokitEngine.kt:29` | 12 (3A) |
| Zero `TODO`/`FIXME` markers in `app/src/main/java` | audit-006 §2.2 | 12 (3A) |
| 1.4 s of artificial `delay(500)+delay(500)+delay(400)` in the open path | ✅ `InkyModule.kt:1104-1108` | 17 (6) |
| `docxExtents` dead state (5 writes, 0 reads) | ✅ `InkyModule.kt:212` + 5 assignment sites | 17 (6) |
| DOCX save writes `[Image: <device path>]` | ✅ `DocxDocumentParser.kt:723` | 17 (6) refuses; 22 (9) fixes |
| `w:instrText` accumulated as body text | ✅ `OfficeDocumentParser.kt` TEXT-event arm (~`:1027`) | 21 (8B) |
| `text:soft-page-break` → hard break + `--- Page Break ---` text | ✅ `OfficeDocumentParser.kt:907-909` | 16 (5B) |
| No list-style parsing; `• ` hard-coded | ✅ no `text:list-style` token in `OdfXmlToken.kt`; bullet arm at `SvXMLImportContext.kt:375` | 18 (7A) |
| TOC tokens absent; hyperlink text dropped (ODT) | ✅ `OdfXmlToken.kt` carries `XML_A` but no index tokens and no `XML_A` context arm | 18 (7A) / 19 (7B) |
| DOCX style chain empty (`w:docDefaults`/`w:basedOn` names only) | ✅ `OfficeDocumentParser.kt:218` `extractDocxStyles` → `DocxStyleMeta` names only | 20 (8A) |
| Run flags leak across the paragraph; `w:val` unread | `OfficeDocumentParser.kt` run-flag arms (unchanged since audit-005) | 20 (8A) |
| Only the last `w:sectPr` read | ✅ `OfficeDocumentParser.kt:144` `extractDocxPageStyleSpec` | 21 (8B) |

**Plan 4 is done. Do not re-execute it.** `plan-04-to-09` § Plan 4 remains in tree as the design record for what PR #11 shipped; its D-1…D-5 numbering is closed by plan-02 §7.

---

## 2. Sample matrix refresh (verified this session)

All 12 `tests/inky` files were unpacked and counted (opening-tag greps; minor deltas vs older documents are method differences, not regressions). This matrix is the floor every PR below must keep green, and it sharpens four plans:

### ODT

| Sample | text:h (levels) | text:p | lists | list-style defs (content/styles) | tables | TOC instances | sections | frames / images | text:a | spans | soft-page-break |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 28 | 183 | 70 | 0 / 11 (`WWNum1…11`) | 3 | 0 | 0 | 6 / 6 | 0 | 149 | 8 |
| 2 | 30 | 308 | 41 | 0 / 26 | 2 | **6** | 0 | 11 / 10 | 15 | 185 | 10 |
| 3 | 0 | 170 | 0 | 0 / 0 | 1 | 0 | 0 | 0 / 0 | 0 | 423 | 0 |
| 4 | 7 | 112 | 15 | 0 / 5 | 0 | 1 | 0 | 1 / 1 | 23 | 476 | 2 |
| 5 | 0 (headings are styled `text:p`) | 218 | 123 | 0 / 18 | 0 | 1 | 0 | 2 / 1 (+1 object frame) | 14 | ~2.6 k | 2 |
| 6 | 45 (6/11/28 at levels 1/2/3) | 322 | 115 | **19 / 1** | 1 | 1 | 5 | 3 / 3 | 45 | 451 | 0 |

### DOCX

| Sample | w:p | w:tbl | drawings | hyperlinks | sectPr | numPr | page br | lastRendered | styles | abstractNum/num | instrText | fldSimple |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 211 | 3 | 6 | 0 | 1 | 54 | 0 | 0 | 35 | 11/11 | **9 (SEQ only)** | 0 |
| 2 | 351 | 2 | 12 | 15 | 5 | 106 | 2 | **22** | 39 | 25/25 | 14 (TOC + SEQ) | 0 |
| 3 | 169 | 1 | 0 | 0 | 1 | **0** | 0 | 0 | 43 | **0/0** | 0 | 0 |
| 4 | 116 | 0 | 1 | 23 | 6 | 17 | 2 | 0 | 191 | 5/5 | 1 | 0 |
| 5 | 217 | 0 | 1 | 14 | 5 | 75 | 2 | 3 | 36 | 18/18 | 1 | 0 |
| 6 | 371 | 1 | 3 | 45 | 5 | 133 | 2 | 0 | 37 | 21/21 | 1 (TOC) | 0 |

### What the matrix changes in the plans

1. **G-1 (PR 18) must read `text:list-style` from both `content.xml` and `styles.xml`.** Sample-6 proves it: the chapter-numbering style `Makalah_20_Default` (level 1 `num-prefix="BAB "`, level 2 `display-levels="2"`) is defined **only in `styles.xml`**, while its 19 sibling list styles are automatic styles in `content.xml`. A parser that reads only one part loses the BAB numbering. The paragraph auto-styles reference it (`P5` → `style:list-style-name="Makalah_20_Default"`), so resolution must flow paragraph-style → list-style, not only `text:list` → `text:list-style-name`.
2. **The numbering model must tolerate an empty `style:num-format`.** Sample-6's `Subbab` styles declare level 3 with `style:num-format=""` — an edge the `NumberingSpec` (E-EN-4) has to represent as "no number" rather than crashing or rendering `""`.
3. **H-4 (PR 21) needs complex fields, not just `w:fldSimple`.** Sample-1.docx carries nine `SEQ "Gambar"/"Tabel"` caption fields wrapped in `w:fldChar` begin/separate/end and **zero** `fldSimple`. Today those instructions land in body text by the TEXT-event leak. A test must assert no ` SEQ ` instruction text survives parsing for Sample-1.docx.
4. **Sample-3 is the control file for both structure plans.** No lists, no numbering, no TOC, no fields, one table, 170 plain paragraphs, 423 spans (ODT) / 43 styles (DOCX): pure paragraph/run/style-chain behaviour. Every structural PR asserts Sample-3 as "nothing new appears, nothing old disappears".
5. **Sample-2 is the `lastRenderedPageBreak` stress file** (22 occurrences) and holds **six** ODT TOC instances; the O-02 hygiene fix and the TOC snapshot renderer both take their heaviest case there, not in Sample-6.
6. Sample-5's headings are `text:p` with auto-styles parenting `Judul1` (zero `text:h`) — already handled by the style cascade; G-1 must not regress it when list styles join paragraph styles.
7. `tests/cellina` (Sample-1 `.ods`/`.xlsx`) and `tests/slidia` (Sample-1 `.odp`/`.pptx`) exist for the later stage and stay out of scope here (Appendix B rule).

---

## 3. The PR sequence at a glance

Letters are gone: sub-item IDs keep their plan identity (E = Plan 5, F = 6, G = 7, H = 8, I = 9). "Gate" = what must be true before the next PR starts.

| PR | Plan | Title | Depends on | Size | Gate to leave |
|---|---|---|---|---|---|
| 12 | 3A | Mechanical strings sweep + CI hygiene guards | — | large but mechanical | hygiene guard green; counts at zero |
| 13 | 3B | Honesty and dead controls (ribbon, hub, Drive) + token sweep outside Inky | 12 | medium | Delivery Gate report; 320 dp evidence |
| 14 | 3C | Documentation accuracy (DESIGN.md review, CONCEPT/About/nightly notes) | — (docs-only) | small | diffs against cited files |
| 15 | 5A | The measuring stick: per-page dump, `LayoutUnits`, `TextMetrics`, one render transform | 12 | medium | dump answers F-25; zero pagination change |
| 16 | 5B | Honest pagination: real metrics, spacing, breaks, widows, windows | 15 | large | Sample-5 `15..21`, Sample-6 `15..26`, both formats |
| 17 | 6 | Image pipeline: extents in both formats, media store, no fake delays, save refusal | 15 | medium | no blank frame; self-heal; refusal dialog |
| 18 | 7A | ODF numbering, heading runs, hyperlinks | 16 | large | BAB/2.1 labels render; no link text lost |
| 19 | 7B | ODF TOC snapshot, table geometry, font identity | 18 | large | Sample-6 ODT fidelity checklist |
| 20 | 8A | DOCX style chain + run formatting | 15 | large | heading/body sizes from the file, no leak |
| 21 | 8B | DOCX numbering, fields, tables, sections | 20 (+19 for shared geometry) | large | Sample-6 DOCX checklist; both formats converge |
| 22 | 9 | Save round-trip integrity (scope firmed by its own pre-change gate) | 17, 19, 21 | large | open → save → reopen preserves structure |

Parallelism: 13 and 14 share no code with 15/16 and can run in a parallel session (they touch `InkyModule`'s ribbon/hub regions, not the layout path). 20 can start its reader scaffolding while 18 is in review (different files). Everything else is sequential.

---

## 4. The PRs in detail

### PR 12 — Plan 3A: mechanical strings sweep + CI hygiene guards

**Goal:** every later PR lands against working guards, not behind them. Closes plan-03 items 3.1, 3.2, 3.3, 3.4, 3.8.

**Scope**

1. Move every `contentDescription` literal (231 repo-wide) and every `Toast` literal (316 repo-wide, 49 inside `InkyModule`) into `app/src/main/res/values/strings.xml`, en_US. Commits are cut per directory so review is mechanical: (a) `modules/inky`, (b) `ui/components`, (c) `ui/home` + `ui/options`, (d) `modules/cellina` + `modules/slidia` + `modules/pagella`, (e) `data/`.
2. Replace the Indonesian user-visible copy with en_US resources, **without** adding `values-in` siblings (`values-in` keeps exactly its 27 existing keys): `Draft Dokumen Baru`, `Inky_Dokumen.*` default names, the two inky toasts, `PapirusConfigManager` reset message, the Drive button. Document content (style names such as `Judul1`) stays data and is untouched.
3. Kill the four user-visible em dashes (R-02): `UniversalEmailSheet.kt:461`, `PapirusEmailEngine.kt:139,208`, `LokitEngine.kt:29`. The `strings.xml:160` XML comment stays and is named in the PR body as documentation.
4. Add labelled `TODO(plan-xx)` markers to every intentional stub found while sweeping (3.8) — the hub's four toast tools, the six empty ribbon decks, the Drive placeholder.
5. **The guards.** A plain JUnit source-hygiene test (runs in the existing `testDebugUnitTest` job, no workflow change) that scans `app/src/main/java/**/*.kt` and fails, with a file:line list, on: `Toast.makeText(..., "<literal>`, `contentDescription = "<literal>`, raw `Color.Gray/DarkGray/LightGray` outside the tolerated paper-white/black carve-outs, and new em dashes in string arguments. The tolerated carve-outs live in one checked-in list so the guard is data, not vibes. From this PR on, every fidelity PR fails CI if it reintroduces a literal.

**Files:** `res/values/strings.xml`, the Kotlin files listed by the audit counts, new `app/src/test/java/com/example/SourceHygieneGuardTest.kt`.
**Tests:** the guard itself (asserts zero offenders after the sweep; a self-test that seeds one synthetic offender in a temp tree and fails); existing suites untouched.
**Acceptance:** the audit's own grep counts reach zero repo-wide; `values-in` diff is empty; CI green; no UI behaviour change beyond translated copy.
**Size:** large but mechanical (1-2 sessions). **Risk:** low; merge conflicts with the fidelity work are the only hazard, which is why this PR goes first.

### PR 13 — Plan 3B: honesty and dead controls

**Goal:** nothing in the UI pretends. Closes plan-03 items 3.5, 3.6, 3.7, 3.9 (the non-Inky 32 dp pairs), 3.10 (the non-Inky token sweep), 3.28.

**Scope**

1. Rebuild the ribbon tab list from `CONCEPT.md` (File, Home, Insert, Layout, Review, View + conditional tabs Document/Table/Picture/Object/Drawing/Fontwork/Chart when the context exists) and settle `References`/`Mailings`: recommend **dropping both** from the Writer tab set until their features exist, because `CONCEPT.md` does not list them for Writer and a tab that is permanently disabled is the louder lie. Unimplemented tabs render disabled with an accessible reason; a press raises the honest note as a string resource (replacing the `:3390` literal).
2. The four hub tools (image, table, link, comment) get either their visible "coming in a later build" state with the `TODO(plan-6/7)` marker from PR 12, or honest enablement only where the owning plan's path already exists (none do today, so: visible disabled state).
3. Google Drive screen: keep the honest placeholder, localize the button (done in 12), and correct `AGENTS.md` + `PROJECT_CONTEXT.md` to "placeholder, not yet implemented" (3.28).
4. Touch targets and tokens outside Inky: `CellinaModule.kt:800,811`, `SlidiaModule.kt:904,915`, `PagellaModule.kt:106,110` zoom pairs to 48 dp targets; the raw greys in those modules and `HomeDashboard` move to `outlineVariant`/`onSurfaceVariant` (paper stays white/black).
5. The colour-picker palette audit (3.11): keep Material 2014 hues as document colours; record that decision in one code comment per the `antislop-code` skill.

**Files:** the three other module files, `HomeDashboard.kt`, `InkyModule.kt` (ribbon/hub region only), `strings.xml`, the two docs.
**Tests:** Robolectric: disabled tabs expose `isEnabled=false` + semantics content description; hub tools assert their disabled state; the SourceHygieneGuard from PR 12 stays green.
**Acceptance:** Delivery Gate report with 320 dp evidence; every visible control either works or says why it does not (R-26/R-27); contrast ≥ 4.5:1 for the swept chrome.
**Size:** medium.

### PR 14 — Plan 3C: documentation accuracy

**Goal:** the documents describe the app that exists. Closes plan-03 items 3.12, 3.29, 3.30, 3.31.

1. `DESIGN.md` review against m3.material.io for the four families the screenshots exercise (tonal bottom bar, FAB role, 40 % sheet deck, dialog header); deviations recorded, not silently fixed (this is plan 10's B1 landing early as docs).
2. `CONCEPT.md` equation pipeline: note that MathML/OMML are produced by `EquationParser` but not yet embedded by either writer (3.29).
3. About screen copy: keep the LOKit credit, state the SIMULATED fallback in the same screen (3.30; the `LokitEngine.statusLabel` string already says it).
4. Nightly release body: switch the static "Recent Changes" bullets to commit-derived notes generated in `build.yml`, or label them as a standing summary (3.31). Follow the `AGENTS.md` nightly rules exactly (delete-then-recreate, `target_commitish`, `make_latest: false`).

**Files:** `DESIGN.md`, `CONCEPT.md`, About strings, `.github/workflows/build.yml`.
**Acceptance:** diffs only; the workflow change is proven by the next nightly run.
**Size:** small. Can run in parallel with 15.

### PR 15 — Plan 5A: the measuring stick

**Goal:** infrastructure with zero pagination change, plus the trace that explains the empty pages before anything moves. Closes E-0 (new, below), E-EN-1…E-EN-3, the `FontRegistry` seam E-EN-5, the plan-2 render-gap, and the F-25 instrumentation requirement.

**Scope**

1. **E-0 · Per-page element dump (new, goes first).** A debug-only (and CI-invokable) dump: for each page of a laid-out sample, the element indices, kinds, reserved heights and the leftover space. Shipped as a unit test on Sample-6 (ODT + DOCX) whose output is asserted *and* printed, so the empty-page mechanism (the `OfficePageBreak` arm at `LayoutEngine.kt:346-353` vs the renderer's silent `else -> { }`) is identified by evidence before PR 16 changes metrics. The plan-2 §1 item 7 question closes here, not in 16.
2. **E-EN-1 · `LayoutUnits`.** One converter object: `ptToUnits`, `cmToUnits`, `emuToUnits`, `twipsToUnits` (reusing `OdfLength`), 96/inch space. The `* 2.5f` fudge is *not yet deleted* (PR 16 deletes it when the new metrics take over), but every new call site uses the converter. The `OdfFrameContext` 160/in drift (audit-003 D3) dies here too.
3. **E-EN-2 · `TextMetrics(style): Measurable`.** Same resolved style for measure and display; Android `Paint` on device, deterministic JVM advance table in tests. No behaviour change yet.
4. **E-EN-3 · `ParagraphStyle` grows real metric fields** (`spaceBefore/AfterUnits`, `lineHeightFactor`, indents, `keepWithNext`, `pageBreakBefore`, `fontFamily`). Parsers do not populate them yet (PR 16/18/20 do); defaults keep today's rendering byte-identical.
5. **E-EN-5 · `FontRegistry` seam** with the §0 substitution order (exact → bundled metric-compatible → user fonts → system → default) returning one family per name for both `TextMetrics` and `OfficeRuns.fontFamilyFor`. This PR wires the *mapping*; device face loading (real `Typeface` from `assets/fonts`) follows in Plan 10 A1, and the display decision is recorded in `FontRegistry` itself.
6. **E-7 · One render transform (new; closes the plan-2 recorded gap).** The renderer derives card size, margins, and text scale from the page's own `widthDp/heightDp` through a single `pageScale`; `PageStackMetrics.BASE_CARD_WIDTH_DP` retires from the render path (it survives only as the Go-to-Page/visibility-range input until the range derives from real page heights). Acceptance: at 100 % the on-screen text column equals the layout's content width at any viewport size.

**Files:** new `data/LayoutUnits.kt`, `data/TextMetrics.kt`, `data/FontRegistry.kt`; `data/OfficeDocument.kt`; `LayoutEngine.kt` (dump + seams only); `LayoutDrivenDocumentRenderer.kt` (transform); `InkyModule.kt` (wiring of the transform inputs).
**Tests:** `LayoutUnitsTest` (pt/cm/emu/twips round-trips), `TextMetricsTest` (JVM determinism), `FontRegistrySubstitutionTest` (30-name corpus from the samples resolves identically for metrics and display), `Plan5ElementDumpTest` (the Sample-6 dump, both formats), and **the pagination windows must not move** (Sample-5 stays `12..30`).
**Acceptance:** no pagination-window change (Sample-5 stays `12..30`); at 100 % the on-screen text column is the print-faithful one at any viewport size; the dump names the empty-page mechanism in its output.
**Size:** medium (2-3 days). **Commit plan:** dump → units → metrics + styles → fonts → transform.

### PR 16 — Plan 5B: honest pagination

**Goal:** the page count stops lying. Closes E-2…E-6, F-21, the page-count half of finding 6, plan-03 3.17, and the F-24/F-25/F-28 symptoms once the dump's mechanism is confirmed.

1. **E-2:** `layoutParagraph` measures through `TextMetrics` at `ptToUnits(fontSize)`; line height from `max(ascent+descent, fontSizeUnits * lineHeightFactor)`; the `2.5f` fudge and `fallbackTextSize` are deleted.
2. **E-3:** `spaceBefore/After` from the style fields (populated from `fo:margin-top/bottom`, `fo:line-height`, `fo:text-indent`, `fo:margin-left/right` in `SvXMLImport`, and `w:spacing`/`w:ind` in the DOCX branch); `elementGapDp` dies.
3. **E-4:** break semantics: honour `fo:break-before/after`, `keep-with-next`, `w:pageBreakBefore`, `w:keepNext`; `text:soft-page-break` and `w:lastRenderedPageBreak` stop being page breaks, and the `--- Page Break ---` text pollution goes with it (3.17, O-02 pagination half).
4. **E-5:** widow/orphan floor of 2 where the style declares it (Sample-6 `Normal`).
5. **E-6:** windows: `Sample5UnifiedPaginationTest` tightens to `15..21`; new `Sample6PaginationTest` asserts `15..26` for both formats with the M365 references (18/21) recorded. Per §0, this is the *staged* window; ±10 % tightening is a later commit after Plans 7/8.
6. **F-21:** one default-size constant: `StyleResolver`'s 14 sp default is replaced by the document's `docDefaults`/default-style size, falling back to 12 pt; the toolbar chip, the paginator and the renderer read the same constant (audit-003 D5 closes with it).

**Files:** `LayoutEngine.kt`, `TextMetrics.kt` (now load-bearing), `data/odf/SvXMLImport.kt`, `OfficeDocumentParser.kt`, `OfficeDocument.kt`, renderer (line heights).
**Tests:** `ParagraphMetricsTest` (Sample-6 `Normal`: 116 % line height, 0.282 cm after), `PaginationFidelityTest` (windows above, both formats), the dump test re-run showing no zero-element pages unless the document authors one, caret/selection/undo suites unchanged.
**Acceptance:** Sample-5 in `15..21`, Sample-6 in `15..26` (ODT **and** DOCX), no empty page in any sample, one-page documents stay one page.
**Risk:** medium-high (upstream assertions move). Mitigations: windows not exact counts; `forceRebuildAll` spot checks; `TextMetrics` reverts alone.

### PR 17 — Plan 6: image pipeline and load performance

**Goal:** images appear immediately, at the declared size, and survive. Closes F-07, F-18, the image half of save integrity (per §0: refuse), O-01's minimal guard.

1. **F-1:** extents parsed in the live path for both formats (`wp:extent` EMU → `emuToUnits`; `svg:width/height` through `LayoutUnits`); `OfficeImage` carries units; dead plumbing deleted (`DocxDocumentParser.parseDocxFile`, `imageExtents`, `InkyModule.docxExtents`).
2. **F-2:** media store in `filesDir/media/<sha>/` with manifest + LRU cap + self-heal re-extraction on miss.
3. **F-3:** decode without a blank frame: explicit `size()` from the extent, placeholder, `crossfade(false)`, pre-decode of first pages during layout.
4. **F-4:** the three `delay()` calls die; `loadingProgressStatus` is driven by real stages (the string resources from Plan 2 stay).
5. **F-5:** the save guard per §0: refuse with a clear en_US dialog when the model holds images the writer cannot serialise; the `[Image: path]` branch is unreachable, then deleted.

**Tests:** `ImageExtentParsingTest` (Sample-6 ODT/DOCX → same dp triple, 292/165/231), `MediaStoreTest` (self-heal, cap), `PaginationImageTest` (taller-than-page image still paginates), `OpenLatencyTest` (no artificial delay in the open path).
**Acceptance:** Recents open shows text in one frame and images within one frame of decode; a cache wipe cannot destroy media; both formats render the same picture the same size.
**Size:** medium.

### PR 18 — Plan 7A: ODF numbering, heading runs, hyperlinks

**Goal:** structure the samples actually contain. Closes G-1, G-3, G-4 (+ F-12, F-13, F-15, O-04, O-05 groundwork).

1. **G-1:** tokens + parser for `text:list-style` and its level styles from **both** `content.xml` and `styles.xml` (§2 finding 1), including empty `style:num-format` (§2 finding 2), `num-prefix/suffix`, `display-levels`, `start-value`, `bullet-char`, per-level text/list-level properties, `text:continue-numbering`, and resolution via paragraph auto-styles (`P5` → `Makalah_20_Default`). Output through E-EN-4's `NumberingSpec`; `CounterState` renders `BAB I`, `2.1`, `a.`, `•`. The hard-coded `• `/`◦ ` dies. Sample-5's headings-as-paragraphs keep working (§2 finding 6).
2. **G-3:** `XML_A` arm for paragraph/heading contexts; `OfficeTextRun` gains an optional link; Sample-2/4/5/6 stop losing link text (45 occurrences in Sample-6 alone).
3. **G-4:** `Heading` and `ListItem` carry runs; `toOfficeDocument()` stops writing synthetic/empty run lists.

**Tests:** `OdtListNumberingTest` (Sample-6: `BAB`-shaped labels with restarts per chapter; `Subbab` level 3 renders no number), `HyperlinkFidelityTest` (Samples 2/4/5/6 text counts), `HeadingRunsTest`, plus Sample-3 unchanged (§2 finding 4).
**Acceptance:** Sample-6 headings numbered, list labels at the level's own font size, no hyperlink text lost; page windows still hold.
**Size:** large.

### PR 19 — Plan 7B: ODF TOC snapshot, table geometry, font identity

Closes G-2, G-5, G-6 (+ F-08, F-09, F-11, F-14, F-23).

1. **G-2:** tokens for `text:table-of-content` / `-source` / `text:index-body` / `text:index-title`; the authored snapshot parses into a `TableOfContent` element (level, text, page number, anchor) and renders with the right tab stop and `TOC 1/2/3` styles. Per §0: snapshot only; regeneration is a later feature. Sample-2's six instances are the stress case (§2 finding 5).
2. **G-5:** column widths from `table:table-column` styles; cell properties (padding, borders, background, v-align, spans); `OfficeTable` carries column weights; height from real cell metrics; rows paginate across pages. **The F-23 parse trace lands first** (header-row-as-paragraphs mechanism), per audit-006 §1.3.
3. **G-6:** renderer consumes `FontRegistry` so Times New Roman body text paints as Liberation Serif with Liberation Serif metrics (per §0 display decision; real `Typeface` loading is plan 10 A1).

**Tests:** `OdtTocTest` (Samples 2/4/6: entries present, none empty, page numbers from the file), `OdtTableGeometryTest` (Sample-6 declared widths, header row inside the table), `FontRegistryTest` (screenshot: TNR body renders through the substitute; heading sizes equal the style sheet).
**Acceptance:** Sample-6.odt fidelity checklist green.
**Size:** large.

### PR 20 — Plan 8A: DOCX style chain + run formatting

Closes H-1, H-2 (+ F-16, F-20, the DOCX half of F-10).

1. **H-1:** parse `w:docDefaults` (`rPrDefault`/`pPrDefault`) and every `w:style` with its `basedOn` chain and full `w:rPr`/`w:pPr`; `pStyle` keeps the DOCX style id. `para1` resolves to 20 pt Aptos Display; body resolves to docDefaults' 12 pt — not the 24/20/16 fallback or 14 sp.
2. **H-2:** `TextRun`s built per `w:r` from resolved character properties; `w:val="0"|"false"` is an explicit negative (negative-flag support in `OfficeRuns.mergeRun`); run flags stop leaking past their run; `currentRuns` actually reaches the paragraph.

**Tests:** `DocxStyleChainTest` (`para1` → 20 pt; docDefaults → 12 pt), `DocxRunFormattingTest` (no leak; explicit unbold), Sample-3.docx as the control (§2 finding 4).
**Size:** large.

### PR 21 — Plan 8B: DOCX numbering, fields, tables, sections

Closes H-3…H-7 (+ F-13 DOCX parity, F-17, F-19, F-26, F-27, F-28 tail, O-03, O-02 remaining).

1. **H-3:** `word/numbering.xml` reader (`abstractNum`/`num`/`lvl` with `start`, `numFmt`, `lvlText`, `lvlJc`, `suff`, `ind`, `isLgl`, `lvlOverride`/`startOverride`) resolved per paragraph into `NumberingSpec`. Five of six samples exercise it (§2 DOCX table).
2. **H-4:** the field model: `w:fldSimple` **and** `w:fldChar` complex fields (begin/separate/end) with `TOC`, `PAGEREF`, `SEQ` handling (§2 finding 3); instructions never reach body text; TOC entries render with `w:tab` leaders and right-aligned numbers; `w:hyperlink` keeps text + target.
3. **H-5:** `w:tblGrid`/`w:tblW`/`w:tcW` → column weights; `gridSpan`, `vMerge`, `tcPr`, `trPr` (`tblHeader` repeat); shared geometry with G-5.
4. **H-6:** per-paragraph governing `w:sectPr` (`w:pPr/w:sectPr` starts a section; body-level closes the last); Sample-6's five sections paginate with their own geometry.
5. **H-7:** `w:lastRenderedPageBreak` excluded from pagination and editable text (Sample-2's 22 occurrences are the regression case).

**Tests:** `DocxNumberingTest`, `DocxFieldTest` (no ` SEQ `/`TOC \` instruction text in parsed output for Samples 1/2/6), `DocxTableGeometryTest`, `DocxSectionGeometryTest` (5 sections → 5 boxes), `DocxTocTest`.
**Acceptance:** Sample-6.docx fidelity checklist; both formats converge on the same page windows; §0's staged-tightening commit lands here or in PR 22.
**Size:** large.

### PR 22 — Plan 9: save round-trip integrity

Closes O-01, the non-destructive-package rule, plan-03 3.13, 3.19, 3.20, 3.26, 3.27, and retires F-2 (`OfficeDocElement`).

Scheduled **after** 17/19/21, with its own pre-change gate (the plan-04-to-09 § Plan 9 text stays the seed): a real ODT writer (styles, list styles, TOC, manifest entries, `style:font-face`, `office:version`), a real DOCX writer (`styles.xml` consistent with regenerated `document.xml`, `numbering.xml` references that exist, `w:tblGrid`, images in the package), the original-package-bytes fallback removed once round trip is proven, and the `OfficeDocElement` wrapper deleted in a mechanical final commit. Acceptance: open → save → reopen preserves text, styles, numbering, tables, images, TOC for Sample-6 in both formats, verified by a CI round-trip test. **Scope is firmed by a short plan document before this PR starts; do not start it from this paragraph alone.**

### Plan 10 — stays parked

Only 3.12 (its documentation half) lands early, inside PR 14. Thread A (real `Typeface` loading, A2 policy write-down, A3 Font Style UI, A4 SAF/user fonts, A5 metrics-parity test) and B2-B6 resume after the fidelity plans. The §0 display decision is already binding on `FontRegistry` from PR 15 onward, so plan 10 A1 is an upgrade of the loader, not a redesign.

---

## 5. Sequencing, parallelism, and the device checklist

| Order | PR | Parallel with | Gate to enter the next |
|---|---|---|---|
| 1 | 12 (3A) | 14 (docs-only) | hygiene guard green |
| 2 | 13 (3B) | — | Delivery Gate PASS at 320 dp |
| 3 | 15 (5A) | 13/14 if a second session is free | dump answers F-25; windows unmoved |
| 4 | 16 (5B) | — | both-format windows green |
| 5 | 17 (6) | 20 (8A) scaffolding | no blank frame; refusal proven |
| 6 | 18 (7A) | 20 (8A) scaffolding | ODT numbering fidelity |
| 7 | 19 (7B) | tail of 18 | ODT checklist green |
| 8 | 20 (8A) | — | DOCX style chain green |
| 9 | 21 (8B) | — | both-format convergence |
| 10 | 22 (9) | plan 10 resume decision | round-trip CI test |

**Device checklist resume points** (`docs/InkyC1Checklist.md`, deliberately postponed):

1. **Now (before PR 15):** install the PR #11 nightly, run plan-02 §5 acceptance and checklist items 5 (Selection), 8 (Reminder), 9 (Zoom), plus the `[needs run]` device log for the Viewer FCT platform-menu question.
2. **After PR 16:** items 2 (editing stages), 4 (Caret), 6 (Go To with believable counts), 9 re-run, 11 (Session Restore: page 15 at 170 %, migrated zoom semantics).
3. **After PR 19/21:** item 7 (Navigator categories incl. the new Images/Hyperlinks entries once they exist), item 10 (Save Compatibility), and the item 12 stress test after PR 17 (media memory).

---

## 6. Cross-plan invariants (unchanged from plan-01 §5)

One paginator, one unit system, one style resolver, one numbering model, one media store — a plan that adds a second path deletes the first in the same PR. No new hard-coded copy or `contentDescription` literal (the PR 12 guard enforces this mechanically). 48 dp targets, token colours only, verified at 320 dp. No em dash in user-visible strings. Evidence, not claims: every PR body lists the findings it closes, the suite it ran, and, for UI, the Delivery Gate with device evidence. The 12-file sample matrix (§2) is the floor.

---

## 7. Deltas against the older plan documents

So nothing contradicts across sessions:

* `plan-01` §2 registry: Plan 2 = landed (PR #11); Plan 4 = consumed by Plan 2. §6 next actions 1-2 are done; action 3 (the dump) is PR 15 commit 1. The registry's `Closes` column stays authoritative for findings.
* `plan-02` §7 stands as the implementation record; its two recorded gaps (text-vs-paper scale, platform toolbar `[needs run]`) are owned by PR 15 and the first device resume point respectively.
* `plan-03` items map: 3.1-3.4+3.8 → PR 12; 3.5-3.7, 3.9-3.11, 3.28 → PR 13; 3.12, 3.29-3.31 → PR 14; 3.13-3.27 → their owning fidelity PRs unchanged.
* `plan-04-to-09`: Plan 4 closed; E-EN-1…E-EN-5 unchanged but ordered as 15/16; E-0 and E-7 are new items defined here; F-1…F-5, G-1…G-6, H-1…H-7 unchanged with the §2 refinements applied; plan 9's seed unchanged.
* `plan-10`: B1 lands early via PR 14; everything else waits; A1 inherits a binding display policy from §0.
* `audit-005` §6 and `audit-006` §5: all questions now answered (§0 here, `plan-03` §0 for the earlier three).

## 8. What still needs the user (nothing blocks)

1. The on-device passes at the three resume points above; the first one gates only checklist credit, not PR 15.
2. A re-check of post-fix screenshots after PR 13 (the R-26 surfaces change).
3. The plan-9 pre-change gate when PR 21 nears (its scope paragraph is a seed, not a commitment).
4. The eventual staged-window tightening decision after PR 21 (±10 %, never below M365) — recorded in §0, executed then.
