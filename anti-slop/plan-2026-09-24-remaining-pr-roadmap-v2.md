# Papirus Office — PR Split Strategy v2 (Plans 3B–10 + Plan 1)

**Date:** 2026-09-24 (v2.2 — every ⚑ item re-verified against the samples and the specs; corrections listed in §7.12)
**Supersedes:** `plan-2026-09-24-remaining-pr-roadmap.md` (v1, baseline `e10f956`). v1 is kept as the record of the pre-PR-12 schedule; where this file and v1 conflict, this file wins.
**Baseline:** `main` `55a9a97` (PR #12 merged), branch convention `arena/<session>-papirus-office`, CI is the only compile/test evidence (no local JDK; `./gradlew testDebugUnitTest` + the build job in `.github/workflows/build.yml`).
**Relationship to plan 1:** `plan-01-master-index.md` keeps the WG-chapter mapping, the checklist mapping and the finding registry; this file keeps the PR order and the per-PR scope. Plan 1's update rule (§6) is executed per PR as tabulated in §4.12.
**Evidence base:** every plan and audit in `anti-slop/` re-read; all 12 `tests/inky` files re-unpacked and re-inventoried (§2, raw XML, method stated); the ODF 1.4 Part 3 schema in `docs/html` probed for every element a parser must read; ECMA-376 §17.9.18 (`w:numId=0`) and the LibreOffice Writer Guide Chapter 1 ("Status bar") read online; every file:line citation re-opened in the working tree at `55a9a97` on 2026-09-24. Where a v1 number could not be reproduced, it was replaced by a count whose rule is written next to it — no figure in this file is quoted from another document without re-deriving it.

---

## 0. Binding decisions (carried over; no re-asking)

From v1 §0 (audit-005 §6 / audit-006 §5 answers) and plan-03 §0:

| Decision | Consequence carried into this file |
|---|---|
| Page-count tolerance = **staged windows** (Sample-5 `15..21`, Sample-6 `15..26` at Plan 5; tighten to ±10 % of the M365 refs 18/21 after Plans 7/8, never below) | PR 16 lands the wide windows; the tightening commit sits inside PR 21 (or 22), not in a metrics PR. |
| Font supply = **display the bundled metric-compatible faces** (TNR→Liberation Serif, Calibri→Carlito, Cambria→Caladea, Arial/Helvetica→Liberation Sans, Courier New→Liberation Mono, Symbol/Wingdings→OpenSymbol) in both pagination and painting from the moment `FontRegistry` exists | PR 15 wires the mapping, PR 19 makes the renderer consume it, Plan 10 A1 upgrades loading to real `Typeface`s. |
| ODT TOC = **authored snapshot** (no "Update Index" stub); regeneration is a later feature after a field model exists | PR 19 G-2; PR 21's DOCX TOC handling mirrors it (the DOCX samples carry the same authored snapshots, §2.2). |
| Save with unserialisable images = **refuse with a clear en_US message**, no `[Image: path]` placeholders | PR 17 F-5; PR 22 removes the need. |
| en_US everywhere in the UI; `values-in` frozen at its 27 keys; document content (style names) is data | PR 12 landed the sweep; the guard enforces it mechanically from `55a9a97` on. |
| Disabled ribbon tabs get an honest visible note (not silent decks) | PR 13 3.6. |
| antislop runs **Mode 1 (during the work)** — the standing project default recorded in `plan-2026-09-22-remaining-writer-fixes.md` §5 | Every PR below is written to the rules rather than audited after them; the Delivery Gate report is attached per PR. |

**New in v2, recorded here for the user to strike:** items ⚑3.32 and ⚑3.33 (PR 13: Navigator category honesty, status-bar object information), ⚑G-4b (PR 18/20: bookmark parsing) and ⚑G-7 (PR 19/21: section identity). Each is additive, small, and unblocks an existing checklist item or a verified sample fact rather than inventing a feature. They are numbered **above** plan-03's existing range because 3.13–3.31 are already taken by the ODF/OOXML conformance and documentation rows (§5–§6 of that file).

---

## 1. Where the work stands after PR 12 (verified at `55a9a97`)

### 1.1 What PR 12 landed (record lives in plan-03 §8)

226 `contentDescription` literals → `cd_*` resources; 282 Toast literals → resources; the Indonesian user-visible copy replaced by en_US (`Draft Dokumen Baru` → "Untitled Document", `Inky_Dokumen.*` → `Untitled.*`, font-size/paste toasts, settings-reset popup, Drive button); the four user-visible em dashes removed; `TODO(plan-x)` markers on the four toast-only hub tools, the unimplemented ribbon decks and the Drive placeholder; and **`SourceHygieneGuardTest`** in `app/src/test/java/com/example/SourceHygieneGuardTest.kt` running in the existing `testDebugUnitTest` job. `values/strings.xml` is now **701** keys; `values-in/strings.xml` is still exactly **27**. Every later PR fails CI if it reintroduces a literal.

### 1.2 Guard baseline that PR 13 burns down

`SourceHygieneGuardTest` records the tolerated raw-grey allowances at guard birth. Re-counted at `55a9a97` with the guard's own rule (comment-masked `Color.Gray|DarkGray|LightGray`, occurrences not lines):

| File | Occurrences | Guard allowance |
|---|---|---|
| `modules/slidia/SlidiaModule.kt` | 8 | 8 |
| `ui/components/SwTextFormattingInspectorDialog.kt` | 7 | 7 |
| `modules/cellina/CellinaModule.kt` | 7 | 7 |
| `modules/pagella/PagellaModule.kt` | 4 | 4 |
| `modules/inky/HomeSubpages.kt` (document-colour palette, plan-03 3.11) | 4 | 4 |
| `ui/components/UniversalNavigatorSheet.kt` | 3 | 3 |
| `modules/inky/InkyModule.kt` | 3 | 3 |
| `ui/components/UniversalOdfSheet.kt` / `UniversalEmailSheet.kt` / `UniversalClipboardSheet.kt` / `OfficeUiComponents.kt` | 2 each | 2 each |
| `ui/components/UniversalChartSheet.kt` / `CloudSyncBar.kt` | 1 each | 1 each |
| `modules/inky/LayoutDrivenDocumentRenderer.kt` | **0** | **1 (stale)** |
| **Total** | **46 in code, 13 files** | **47** |

**Correction recorded here:** the "47 across 14 files" quoted by both v1-derived text and plan-03 §8 is the count of *textual* occurrences in the 14-file set. One of them — `LayoutDrivenDocumentRenderer.kt:665` — sits inside the comment that explains why the grey there was already replaced by `outline`/`onSurfaceVariant` in PR #11, and the guard masks comments before counting. The masked truth is **46 occurrences in 13 files**; `LayoutDrivenDocumentRenderer`'s allowance of 1 is stale and is deleted with the rest. The guard's own header assigns this burn-down to 3B; PR 13 scope below includes deleting every map entry as the allowances reach zero.

### 1.3 Still open (re-verified this session; ✅ = confirmed at the cited location at `55a9a97`)

| Still open | Where | Owning PR |
|---|---|---|
| `fontSizeSp * 2.5f` measuring fudge | ✅ `LayoutEngine.kt:154` | 16 (5B) |
| Flat `elementGapDp = 12f` between all elements | ✅ `LayoutEngine.kt:107` | 16 (5B) |
| `StyleResolver` 14 sp default on null/miss (F-21) | ✅ `LayoutEngine.kt:61` | 16 (5B) |
| Text scales with `renderScale` while paper maps with `pageScale` | ✅ `LayoutDrivenDocumentRenderer.kt:149-150,243-245` | 15 (5A) |
| Renderer line height = `(sizeSp + 5f)` magic constant | ✅ `LayoutDrivenDocumentRenderer.kt:474,500,579,596,650` | 16 (5B, E-2) |
| Table height `rows * 35 + 10`, all cells `10.sp` | ✅ `LayoutEngine.kt` | 19 (7B) / 21 (8B) |
| Ribbon: 8 tabs declared, only File+Home have content, literal "…will be implemented soon." | ✅ `InkyModule.kt:2936`, `:3383-3390` (pre-13 numbering) | 13 (3B) |
| Toolbar hub image/table/link/comment toast-only (TODOs landed in 12) | ✅ `InkyModule.kt:2790-2811` | 13 (3B) |
| ⚑ Navigator: 13 categories rendered, ~20 `EmptyCategoryRow` rows, no "hyperlinks" category | ✅ `UniversalNavigatorSheet.kt` (categories: bookmarks, comments, fields, footnotes, frames, headings, images, ole, pages, sections, shapes, tables; each renders `R.string.no_objects_to_navigate` when empty) | 13 (3B) + 15/17/18/19/20/21 fill |
| ⚑ No bookmark parsing anywhere (tokens exist, unconsumed; DOCX `w:bookmarkStart/End` unhandled) | ✅ `OdfXmlToken.kt:39-41` has `XML_BOOKMARK*`, no context arm; no `bookmark` handling in the ODT/DOCX parsers | 18 (ODF) / 20 (DOCX) |
| ⚑ ODT `text:section` identity unparsed (5 sections in Sample-6; children still parse, the name/style do not) | ✅ no section token in `OdfXmlToken.kt` | 19 (7B, ⚑G-7) |
| 1.4 s artificial delay (`delay(500)+delay(500)+delay(400)`) in the open path | ✅ `InkyModule.kt:1105-1109` | 17 (6) |
| `docxExtents` dead state (writes, no reads) | ✅ `InkyModule.kt:212` | 17 (6) |
| DOCX save writes `[Image: <device path>]` | ✅ `DocxDocumentParser.kt:723` | 17 (6) refuses; 22 (9) fixes |
| ODF TOC/list/hyperlink structure unread (G-1…G-4) | ✅ `OdfXmlToken.kt` (no list-style or index tokens; `XML_A` unconsumed), `SvXMLImportContext.kt` (hard-coded `• `/`◦ `) | 18 (7A) / 19 (7B) |
| DOCX style chain names-only (`w:docDefaults`/`w:basedOn` unread) | ✅ `OfficeDocumentParser.kt:218` `extractDocxStyles` → `DocxStyleMeta(styleId, name, outlineLvl, isHeading, headingLevel)` only | 20 (8A) |
| DOCX heading detection is sample-tuned regexes | ✅ `OfficeDocumentParser.kt:55-57` `PARA_STYLE_REGEX=para[1-9]`, `HEADING_STYLE_REGEX=heading[1-9]` | 20 (8A) retires them |
| `currentRuns` declared but never populated (every DOCX paragraph = one flat run) | ✅ `OfficeDocumentParser.kt:800` (dead writes at :873/:1069/:1083) | 20 (8A) |
| Run flags (`w:b/i/u`) leak across the paragraph; `w:val` unread | ✅ run-flag arms in the shared parse block | 20 (8A) |
| `word/numbering.xml` never read; writer hard-codes `w:numId="1"` | ✅ no `numPr`/`numId` handling in the parser; `DocxDocumentParser.kt:740` | 21 (8B) |
| Fields (`w:fldSimple`, `w:fldChar`/`instrText`) land in body text; `w:hyperlink` anchors dropped | ✅ shared TEXT arm appends unconditionally; no `hyperlink`/`anchor`/`fldChar` handling | 21 (8B) |
| Only the last `w:sectPr` read | ✅ `OfficeDocumentParser.kt:144` `extractDocxPageStyleSpec` | 21 (8B) |
| `w:lastRenderedPageBreak` → hard break + `--- Page Break ---` text | ✅ shared parse block (the `w:br` arm at :961 already distinguishes `type="page"` from soft breaks — that part is correct) | 16 (5B) / 21 (8B) |
| Save round trip: ODT written as bare `office:document-content` (`office:version="1.2"`, no styles/manifest), DOCX writer references styles/numbering it does not define | ✅ `DocxDocumentParser.kt:748+` `generateOdtXml`, `saveDocxZip` | 22 (9) |

Plan 4 remains **consumed by PR #11 — do not re-execute.**

---

## 2. Sample matrix (re-inventoried this session from the raw XML)

Method: every count below is `grep -o` with an explicit tag boundary (`<text:list[ />]`, not `<text:list`), run against the unpacked copies of `tests/inky/*`. Nested elements are counted where they occur; where a count needs a qualifier the qualifier is in the cell. This replaces v1's prefix-grep numbers, which inflated Sample-2's TOC count (`<text:table-of-content` also matches `-source` and `-entry-template`) and left the list column ambiguous.

### 2.1 ODT (`content.xml` + `styles.xml`)

| # | `text:h` (levels) | `text:p` | lists / items | list-style defs (content / styles) | tables | TOC | sections | frames / images | `text:a` | bookmark-start/-end | soft-page-break |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 28 (1:4, 2:6, 3:16, 4:2) | 183 | 70 / 84 | 0 / 11 (`WWNum1…11`) | 3 | – | – | 6 / 6 | 0 | 0 / 0 | 8 |
| 2 | 30 (1:6, 2:9, 3:15) | 308 | 41 / 103 | 0 / 26 | 2 | **1** (15 entries / 15 links) | – | 11 / 11 | 15 | 15 / 15 | 10 |
| 3 | 0 | 170 | 0 / 0 | 0 / 0 | 1 | – | – | 0 / 0 | 0 | 0 / 0 | 0 |
| 4 | 7 (all level 1) | 112 | 15 / 18 | 0 / 5 | 0 | 1 (22 / 21) | – | 1 / 1 | 23 | 22 / 22 | 2 |
| 5 | 0 (headings are styled `text:p`) | 218 | 123 / 186 | 0 / 18 | 0 | 1 (15 / 14) | – | 2 / 1 | 14 | 14 / 14 | 2 |
| 6 | 45 (1:6, 2:11, 3:28) | 322 | 115 / 180 | **19 / 1** (`Makalah_20_Default` in `styles.xml`) | 1 | 1 (45 / 45) | 5 | 3 / 3 | 45 | 46 / 46 | 0 |

Cross-format checks that make the pairs provably the same documents: ODT `text:a` counts equal DOCX `w:hyperlink` counts per sample (15/23/14/45, samples 2/4/5/6), ODT `text:h` level distribution equals the DOCX `pStyle para1/2/3` distribution in Sample-6 (6/11/28), and Sample-6's list style is `Makalah_20_Default` (display name "Makalah Default") while its DOCX twin's `abstractNum 15` is `w:name="Makalah Default"` — the same style, spelled per format.

Fidelity references verified in the raw XML: Sample-6's chapter numbering (`style:num-prefix="BAB "`, `display-levels="1"` at level 1, `display-levels="2"` at level 2) lives **only in `styles.xml`**; its 19 sibling list styles are automatic styles in `content.xml`. The `Subbab_*` styles declare levels **2 and 3** with `style:num-format=""` (the "no number" edge). The Sample-6 TOC carries `text:a xlink:href="#_TOC…"` anchors (45 of them) and roman page numbers (ii/iii) for the front matter. Sample-5's page style is A4 with `margin-top 0cm`/`margin-bottom 1cm`; its default paragraph is Aptos 12 pt.

### 2.2 DOCX (`word/document.xml`, `word/styles.xml`, `word/numbering.xml`)

All six samples carry `numbering.xml`. docDefaults: Aptos 12 pt, justified, after 160 twips, line 276–278 (Sample-4: firstLine 720 / line 360).

| # | `w:p` | `w:tbl` | drawings | hyperlinks | `sectPr` | `numPr` | TOC snapshot (toc-styled entries) | `instrText` | `fldSimple` | `w:bookmarkStart` | `w:link` in styles |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 211 | 3 | 6 | 0 | 1 | 54 | – | 9 (SEQ only) | 0 | 0 | 0 |
| 2 | 351 | 2 | 12 | 15 | 5 | 106 | 15 (6+9) | 14 (TOC+SEQ) | 0 | 15 | 0 |
| 3 | 169 | 1 | 0 | 0 | 1 | **0** | – | 0 | 0 | 0 | 0 |
| 4 | 116 | 0 | 1 | 23 | 6 | 17 | 21 (7+14) | 1 | 0 | 22 | **27** |
| 5 | 217 | 0 | 1 | 14 | 5 | 75 | 14 (6+8) | 1 | 0 | 14 | 0 |
| 6 | 371 | 1 | 3 | 45 | 5 | 133 | **45 (6+11+28)** | 1 (TOC) | 0 | 46 | 0 |

The TOC snapshots are authored `toc 1/2/3`-styled paragraphs (the DOCX twins of the ODT `text:table-of-content` bodies, §2.1). Sample-6's 45 entries split 6/11/28 by level — the same distribution as its 45 headings. Parsing gotchas verified in the XML: Sample-4's field instruction arrives XML-entity-escaped (` TOC \o &quot;1 - 2&quot; \z `); Sample-4 defines unused `toc 3…9` + `TOC Heading` styles, so a name-matcher must not treat style *definition* as style *usage*; `w:hyperlink` counts exceed TOC entries in Sample-4 (23 total, 21 in the TOC); and **Sample-4 is the only sample that carries `w:link`** (27 of them), which makes it the precedence test for §2.3 item 2.

No `gridSpan`/`vMerge` in any sample, so merged cells stay deprioritised (parse `tblHeader`, render, but do not build the merge model yet).

### 2.3 ⚑ The Sample-6 DOCX heading architecture (verified this session, line-by-line)

This is the single most important structural fact the DOCX plans must encode. The full picture, each item read out of the file:

1. **The heading *styles* carry the numbering.** `para1…para9` are named `heading 1…heading 9`, `basedOn para0` (Normal: Times New Roman, `w:lang id-id`). Their `w:pPr` contains `w:numPr` → `w:ilvl N-1` / **`w:numId 15`**, plus `jc=center`, `keepNext`, `keepLines`, `outlineLvl=N-1`; their `w:rPr` is only `w:b`/`w:bCs`. `w:num 15` → `abstractNum 15`, `w:name="Makalah Default"`, `lvlText` = `BAB %1`, `%1.%2`, `%1.%2.%3`, … with `start=1`.
2. **The heading *character* styles are linked by naming convention, not by `w:link`.** `char1…char9` are named `Heading 1 Char…Heading 9 Char` (`w:customStyle="1"`, `basedOn char0` = "Default Paragraph Font"). **There is no `w:link` attribute anywhere in Sample-6** (regex over all 37 styles: zero matches), while Sample-4 uses `w:link` 27 times. Word's built-in convention pairs a paragraph style named `X` with a character style named `X Char`; the engine must implement both mechanisms (explicit `w:link` first, then the name convention). Verified metrics: char1 = Aptos Display 20 pt (`w:sz 40`) `#0f4761`; char2 = Aptos Display 16 pt (`sz 32`) `#0f4761`; char3 = 14 pt (`sz 28`) `#0f4761` with **no `w:ascii` `rFonts` of its own** (only `eastAsia`/`cs`), so its family is inherited up the chain.
3. **Paragraphs override the style's numbering with direct `w:numPr`.** All 45 heading paragraphs use `pStyle para1/2/3` (6/11/28). **42** carry a direct `w:numPr` repeating the style's `numId=15`; **3** carry **`w:numId 0`** — KATA PENGANTAR, DAFTAR ISI and one more — which per ECMA-376 §17.9.18 *"shall never be used to point to a numbering definition instance, and shall instead only be used to designate the removal of numbering properties at a particular level in the style hierarchy."* Those three headings must render **without** the `BAB N` prefix; the other 42 must render **with** it, computed from the style-inherited numbering.
4. **Page breaks can live inside the heading paragraph.** KATA PENGANTAR/DAFTAR ISI begin with `w:br w:type="page"` before the bookmark and text (the ODT pair expresses the same break as `fo:break-before=page` on the auto style — the parser must normalise both to one model fact). `w:br w:type="textWrapping"` is a soft line break, not a page break; the shared parse block already distinguishes the two (`OfficeDocumentParser.kt:961-971`).
5. **The DOCX TOC is an authored snapshot** (same decision as ODT). Entries are plain paragraphs styled `para16/17/18` = **`toc 1/2/3`** (basedOn Normal), each a `w:hyperlink w:anchor="_TOCxxxxx"` containing the entry text, a `w:tab`, and the literal page number. **Correction to the first draft of this file: the right-aligned dot-leader tab (`w:tab w:val="right" w:pos="9027" w:leader="dot"`) is a *paragraph-level* `w:tabs` in `document.xml`, not part of the `toc 1/2/3` style definitions** — the three styles carry nothing but `w:ind`/`w:spacing`. A renderer that reads the leader from the style would find nothing; the plan reads it from the entry paragraph. Bookmark `_TOC000009` sits in the real PEMBAHASAN heading; 45 `w:hyperlink w:anchor="_TOC…"` targets exist.
6. **Sections carry page-numbering formats.** The five `w:sectPr` share geometry but differ in `w:pgNumType`: §1 `lowerRoman start=1` + `titlePg` (front matter, roman ii/iii — the same roman pages the ODT TOC shows), §2 `decimal start=1` (body restarts), §3–5 `decimal` (no `start`). The status bar must be able to tell **page number** from **sequence number** (WG Ch.1 status-bar item), so the section model needs `pgNumType` (fmt/start) and `titlePg`, not just geometry.

**Consequences for the plans:** PR 20 must resolve the full style chain *including* naming-convention char links and must surface, per heading paragraph, whether numbering is inherited, explicit, or suppressed (`numId=0`) — as a model fact PR 21 renders. PR 21 renders the `BAB N` labels from `abstractNum 15`, handles the three suppressed headings, and keeps the TOC snapshot intact (toc styles, paragraph-level dot leaders, literal pages, `_TOC` anchors). PR 22's writer must reproduce a `styles.xml` with the heading/char pairs and a `numbering.xml` the generated `document.xml` actually references.

### 2.4 What the matrix dictates for the plans

1. **G-1 (PR 18) reads `text:list-style` from both `content.xml` and `styles.xml`** and resolves through paragraph auto-styles (`P5` → `Makalah_20_Default`), not only `text:list` → `text:list-style-name`.
2. **The numbering model tolerates empty `style:num-format`** (Sample-6 `Subbab_*` levels 2 and 3) as "no number".
3. **H-4 (PR 21) needs complex fields**, not just `w:fldSimple` (Sample-1: nine `SEQ` fields in `w:fldChar` wrappers, zero `fldSimple`); instructions never reach body text.
4. **Sample-3 is the control file** for both structure plans (no lists, numbering, TOC or fields; one table; pure paragraph/run/style-chain behaviour): every structural PR asserts "nothing new appears, nothing old disappears".
5. **The heaviest TOC case is Sample-6** (45 entries/45 links, 19.7 KB of index body), then Sample-4 (22/21), Sample-2 (15/15), Sample-5 (15/14) — not Sample-2 "six instances" as v1 stated.
6. **Sample-5's headings are `text:p` with auto-styles parenting `Judul1`** (zero `text:h`) — the style cascade already handles them; G-1 must not regress it.
7. **⚑ Sample-6 is the cross-format convergence case end to end:** identical 45-heading distribution in both formats, the same chapter numbering expressed two ways (ODT list style ↔ DOCX style-inherited `numId 15`), the same roman front-matter pages (ODT TOC entry pages ↔ DOCX `pgNumType lowerRoman`), the same five sections (ODT `text:section` ↔ DOCX `sectPr`). PR 21's acceptance uses it to prove ODT and DOCX of one file paginate identically.
8. `tests/cellina` and `tests/slidia` stay out of scope (Appendix B rule).

---

## 3. The PR sequence at a glance

Sub-item IDs keep their plan identity (E = Plan 5, F = 6, G = 7, H = 8, I = 9). "Gate" = what must be true before the next PR starts. ⚑ = new in v2.

| PR | Plan | Title | Depends on | Size | Gate to leave |
|---|---|---|---|---|---|
| 12 | 3A | Mechanical strings sweep + CI hygiene guards | — | — | **landed** (`55a9a97`) |
| 13 | 3B | Honesty and dead controls (ribbon, hub, Drive, ⚑ Navigator, ⚑ status bar) + token sweep | 12 | medium | Delivery Gate report at 320 dp; grey allowances at 0 |
| 14 | 3C | Documentation accuracy (DESIGN.md review, CONCEPT/About/nightly notes) | — (docs-only) | small | diffs against cited files |
| 15 | 5A | The measuring stick: per-page dump, `LayoutUnits`, `TextMetrics`, `FontRegistry` seam, one render transform | 12 | medium | dump answers F-25; zero pagination change |
| 16 | 5B | Honest pagination: real metrics, spacing, breaks, widows, windows | 15 | large | Sample-5 `15..21`, Sample-6 `15..26`, both formats |
| 17 | 6 | Image pipeline: extents in both formats, media store, no fake delays, save refusal | 15 | medium | no blank frame; self-heal; refusal dialog |
| 18 | 7A | ODF numbering, heading runs, hyperlinks, ⚑ bookmarks | 16 | large | BAB/2.1 labels render; no link text lost |
| 19 | 7B | ODF TOC snapshot, table geometry, font identity, ⚑ sections | 18 | large | Sample-6 ODT fidelity checklist |
| 20 | 8A | DOCX style chain + run formatting (⚑ char-link convention, numId-suppression flag) | 15 | large | heading/body sizes from the file, no leak |
| 21 | 8B | DOCX numbering, fields, tables, sections, ⚑ TOC snapshot | 20 (+19 shared geometry) | large | Sample-6 DOCX checklist; both formats converge |
| 22 | 9 | Save round-trip integrity (pre-change gate first) | 17, 19, 21 | large | open → save → reopen preserves structure |
| 10 | — | Font engine + design language — **parked** | 21 + user decision | — | resume trigger §4.11 |

Parallelism: 13 and 14 share no code with 15/16 (they touch `InkyModule`'s ribbon/hub regions, not the layout path) and can run in a parallel session. 20's reader scaffolding (the `styles.xml`/`numbering.xml` part readers) starts while 18 is in review — different files. Everything else is sequential.

---

## 4. The PRs in detail

### 4.0 PR 12 — Plan 3A — **landed** (`55a9a97`)

Record: plan-03 §8. The guard is the standing enforcement for 3.1–3.4/3.8; the banned-literal list (`Draft Dokumen Baru`, `Inky_Dokumen`, `Pengaturan aplikasi sukses direset`, `Hubungkan Akun Google`, `Ukuran font diubah ke`, `Menempelkan sebagai`, `Ubah Ukuran Font`) is data inside the test and grows if a later PR regresses copy.

### 4.1 PR 13 — Plan 3B: honesty and dead controls

**Goal:** nothing in the UI pretends. Closes plan-03 3.5, 3.6, 3.7, 3.9, 3.10, 3.11, 3.28, plus the three ⚑ items (3.32, 3.33, and the E-7 split noted below).

**Scope**

1. **3.6 Ribbon.** Rebuild the tab list from `CONCEPT.md` (File, Home, Insert, Layout, Review, View + contextual tabs when their context exists); **drop `References`/`Mailings`** until their features exist — `CONCEPT.md` does not list them for Writer, and a permanently disabled tab is the louder lie. The tab strip keeps all six visible: the two implemented tabs switch the deck, the other four render in a disabled tone, carry an accessible reason, and a press raises the honest note as a string resource (replacing the `InkyModule.kt:3383` literal). The pager must host **only** implemented decks and map page index → tab identity explicitly, not by positional coincidence.
2. **3.5 Hub tools.** The four toast-only tools (image, table, link, comment) get the visible "not available in this build" state (38 % on-surface tint, disabled semantics, `cd_add_*` naming the reason) while still telling the user what happens on press; the `TODO(plan-6/7/8)` markers from PR 12 stay.
3. **3.7/3.28 Drive.** Keep the honest placeholder; correct `AGENTS.md` + `PROJECT_CONTEXT.md` to "placeholder, not yet implemented".
4. **3.9/3.10 Tokens and targets outside Inky.** `CellinaModule.kt:800,811` and `SlidiaModule.kt:904,915` zoom pairs → 48 dp targets (their explicit `Modifier.size(24.dp)` overrides Material 3's 48 dp minimum, which is the actual defect). **Correction:** `PagellaModule.kt:106,110` need no change — those `IconButton`s carry no size modifier, so Material 3's `minimumInteractiveComponentSize` already gives them 48 dp; the claim that they were open came from an audit line count, not from a measurement. The raw greys in those modules, `HomeDashboard`'s neighbourhood, `SwTextFormattingInspectorDialog`, `UniversalNavigatorSheet`, `CloudSyncBar`, `UniversalChartSheet`, `UniversalOdfSheet`, `UniversalEmailSheet`, `UniversalClipboardSheet`, `OfficeUiComponents`, `InkyModule` and `HomeSubpages` move to the token set. **Every one of the 46 recorded grey occurrences reaches zero and all 14 map entries — including the stale `LayoutDrivenDocumentRenderer` one — are deleted from `SourceHygieneGuardTest`** (§1.2).
5. **3.11 Palette.** Keep the Material-2014 hues as document colours; write them as explicit `Color(0xFF…)` document colours (identical values) with one `antislop-code`-compliant comment recording the decision, so the *chrome* grey rule can reach zero without repainting the picker.
6. **⚑ 3.32 Navigator categories (new).** `UniversalNavigatorSheet` renders 13 categories and, when one is empty, the same generic "no objects to navigate" line for every one — including categories whose data **no parser produces at all**. In this PR each empty category becomes one of two honest states, chosen by whether the parsers can *see* that object class:
   * **verified-absent** — "No %s in this document." — only for classes some parser constructs today: headings, tables, images, pages (and the session-only Reminders list). Verified at `55a9a97` by constructor search: `OfficeListItem(`, `OfficeTable(`, `OfficeImage(` etc. are constructed in `data/writer/OdtDocumentParser.kt` and `OfficeDocumentParser.kt`; the index engine's arms for headings/tables/images run on real documents.
   * **not-yet-readable** — a disabled row "Not yet available in this build." plus a `TODO(plan-NN)` marker in source — for **bookmarks, comments, fields, footnotes, frames, OLE objects, sections, shapes, hyperlinks**. The reason is verified, not assumed: no main-source file constructs `OfficeBookmark(`, `OfficeComment(`, `OfficeSection(`, `OfficeShape(`, `OfficeField(`, `OfficeFootnoteElement(` or `OfficeHyperlink(`; `OfficeParagraph.bookmark` is only written by the editor API `DocumentCoreEngines.insertBookmark`, `OfficeTextRun.hyperlink`/`field` are never assigned by a parser, `DocumentIndex.frames` is never appended to, and `OfficeResources.objects` (the OLE source) is never populated by any parser. The index engine *has* arms for all of them — `NavigationEngineTest` exercises those arms with a synthetic document — but on a real opened file those lists are empty for a reason the user cannot see, and a parser that cannot read footnotes may not claim the document has none.
   * The **Hyperlinks** category is added in the not-yet-readable shape. Basis: `plan-01` §3.1's WG sidebar-deck row ("Navigator deck … lacks … Hyperlinks categories") and `AGENTS.md`'s Navigator Deck list; the Navigate-By filter option for it lands with the data (18/20), so no dead filter entry is added.
   * No jump behaviour changes in this PR — the data lands with the owning plans.
7. **⚑ 3.33 Status-bar object information (new, WG Table 1).** WG Ch.1 gives the status bar a "section or object information" field (image/frame → size+position; list item → level+list style; heading → heading level; table → name+cell reference; section → name; index → type). This PR creates the **slot** and fills it only with facts the model already proves, consumed **through the seam that already exists**: the caret-to-element mapping in `InkyModule` (`DocumentTextWindows.elementForOffset`) that the toolbar hub already uses for font tracking. Facts shown: a heading (level + text) resolved by the Navigator index — which also covers Sample-5's paragraph-styled headings, because the index is the one resolver that walks the style parents — the element kinds "Table" and "List item", and a hyphen placeholder (`"-"`, never an em dash, R-02) everywhere else. Table row/column, sections, frames and indexes are **not invented**; they fill the same slot after 19/21.
8. **⚑ E-7 split (recorded here, executed in 15):** the status bar's page-range logic re-implements the renderer's fit-scale computation inline (`InkyModule.kt:2412-2431`). PR 13 does **not** touch it (that region is under PR 15's transform work); PR 15 makes it consume the same single transform.

**Files:** `InkyModule.kt` (ribbon/hub/status-bar regions), `UniversalNavigatorSheet.kt`, the three other module files, `SwTextFormattingInspectorDialog.kt`, `CloudSyncBar.kt`, `UniversalChartSheet.kt`, `UniversalOdfSheet.kt`, `UniversalEmailSheet.kt`, `UniversalClipboardSheet.kt`, `OfficeUiComponents.kt`, `HomeSubpages.kt`, `LayoutDrivenDocumentRenderer.kt` (comment only), `strings.xml`, `SourceHygieneGuardTest.kt` (allowances → 0), `AGENTS.md`, `PROJECT_CONTEXT.md`.
**Tests:** `SourceHygieneGuardTest` green with an empty allowance map; `NavigatorCategoryHonestyTest` — asserts the readable/unreadable split **against the source tree** (a category classified readable must have an element constructor in `app/src/main/java`, a category classified unreadable must not), so a later parser PR cannot leave the Navigator lying; `WriterRibbonTabsTest` — every declared tab either maps to a deck or is marked unavailable, implemented tabs are a subset of the strip, the pager page count equals the deck count. Compose-level click-throughs stay on device (R-35): CI has no display.
**Acceptance:** Delivery Gate report with 320 dp evidence; every visible control either works or says why it does not (R-26/R-27); contrast ≥ 4.5:1 for the swept chrome; guard green with zero allowances.
**Size:** medium. **Risk:** low; it touches chrome only, no layout path.

### 4.2 PR 14 — Plan 3C: documentation accuracy (unchanged from v1)

Closes plan-03 3.12, 3.29, 3.30, 3.31. `DESIGN.md` review against m3.material.io for the four families the screenshots exercise (tonal bottom bar, FAB role, 40 % sheet deck, dialog header) — deviations recorded, not silently fixed; `CONCEPT.md` equation pipeline notes the writer-side gap (MathML/OMML produced, not embedded); About screen states the SIMULATED fallback beside the LOKit credit; nightly release body switches to commit-derived notes or is labelled a standing summary (following the `AGENTS.md` nightly rules: delete-then-recreate, `target_commitish`, `make_latest: false`).
**Tests:** none (docs + workflow). **Acceptance:** diffs only; the workflow change is proven by the next nightly run. **Size:** small. Can run in parallel with 15.

### 4.3 PR 15 — Plan 5A: the measuring stick

**Goal:** infrastructure with zero pagination change, plus the trace that explains the empty pages before anything moves. Closes E-EN-1…E-EN-5, E-7 (including ⚑), the plan-2 render gap, F-25 instrumentation.

1. **E-0 · Per-page element dump (first).** Debug-only, CI-invokable: for each page of a laid-out sample, element indices, kinds, reserved heights, leftover space. Shipped as a unit test on Sample-6 (ODT + DOCX), output asserted *and* printed. The suspected empty-page mechanism — the `OfficePageBreak` arm in `LayoutEngine.kt` adds an extra **empty page** when a break lands at the top of a page (double break → blank) — is confirmed or refuted by evidence here; plan-2 §1 item 7 closes in this PR, not 16.
2. **E-EN-1 · `LayoutUnits`.** One converter: `ptToUnits`, `cmToUnits`, `emuToUnits`, `twipsToUnits` (96/inch). The `* 2.5f` fudge is *not yet deleted* (16 deletes it); every new call site uses the converter; the `OdfFrameContext` 160/in drift dies here too.
3. **E-EN-2 · `TextMetrics(style): Measurable`.** Same resolved style for measure and display; Android `Paint` on device, deterministic JVM advance table in tests.
4. **E-EN-3 · `ParagraphStyle` grows metric fields** (`spaceBefore/AfterUnits`, `lineHeightFactor`, indents, `keepWithNext`, `pageBreakBefore`, `fontFamily`). Parsers don't populate them yet (16/18/20 do); defaults keep today's rendering byte-identical.
5. **E-EN-5 · `FontRegistry` seam.** Substitution order per §0 (exact → bundled metric-compatible → user fonts → system → default), one family per name for both `TextMetrics` and `OfficeRuns.fontFamilyFor`. This PR wires the *mapping*; real `Typeface` loading is Plan 10 A1; the display decision is recorded in `FontRegistry` itself.
6. **E-7 · One render transform (+ ⚑ centralisation).** The renderer derives card size, margins, and text scale from the page's own `widthDp/heightDp` through a single `pageScale`; `PageStackMetrics.BASE_CARD_WIDTH_DP` retires from the render path. ⚑ The status bar's page-range logic consumes the same single transform as the renderer, so the two can never drift again. `PageStackMetrics` survives only as the Go-to-Page/visibility-range input until that range derives from real page heights.
**Files:** new `data/LayoutUnits.kt`, `data/TextMetrics.kt`, `data/FontRegistry.kt`; `data/OfficeDocument.kt`; `LayoutEngine.kt` (dump + seams only); `LayoutDrivenDocumentRenderer.kt` (transform); `InkyModule.kt` (transform inputs; page-range consumers).
**Tests:** `LayoutUnitsTest`, `TextMetricsTest`, `FontRegistrySubstitutionTest` (30-name corpus from the samples resolves identically for metrics and display), `Plan5ElementDumpTest` (Sample-6, both formats), and **the pagination windows must not move** (Sample-5 stays `12..30` until 16).
**Acceptance:** no pagination-window change; at 100 % the on-screen text column equals the layout's content width at any viewport size; the dump names the empty-page mechanism in its output.
**Size:** medium (2–3 days). **Commit plan:** dump → units → metrics + styles → fonts → transform.

### 4.4 PR 16 — Plan 5B: honest pagination

**Goal:** the page count stops lying. Closes E-2…E-6, F-21, the page-count half of finding 6, plan-03 3.17, and the F-24/F-25/F-28 symptoms once the dump's mechanism is confirmed.

1. **E-2:** `layoutParagraph` measures through `TextMetrics` at `ptToUnits(fontSize)`; line height = `max(ascent+descent, fontSizeUnits * lineHeightFactor)` — which also deletes the renderer's `(sizeSp + 5f)` magic line-height constant (⚑ made explicit; the constant dies in favour of style-driven line height, Sample-6 `Normal` = 116 %). The `2.5f` fudge and `fallbackTextSize` are deleted.
2. **E-3:** `spaceBefore/After` from the style fields, populated from `fo:margin-top/bottom`, `fo:line-height`, `fo:text-indent`, `fo:margin-left/right` in `SvXMLImport` and `w:spacing`/`w:ind` in the DOCX branch (including docDefaults `pPr` — §2.2); `elementGapDp` dies.
3. **E-4:** break semantics: honour `fo:break-before/after`, `keep-with-next`, `w:pageBreakBefore`, `w:keepNext`; `text:soft-page-break` and `w:lastRenderedPageBreak` stop being page breaks and the `--- Page Break ---` text pollution goes with them (3.17, O-02 pagination half). The ODT/DOCX difference for chapter breaks (style-level `fo:break-before` on the auto style vs `w:br type=page` inside the heading paragraph, §2.3 item 4) normalises to the same model fact here.
4. **E-5:** widow/orphan floor of 2 where the style declares it (Sample-6 `Normal`).
5. **E-6:** windows: `Sample5UnifiedPaginationTest` tightens from `12..30` to `15..21`; new `Sample6PaginationTest` asserts `15..26` for both formats with the M365 references (18/21) recorded. Staged window per §0; the ±10 % tightening is a later commit after 7/8.
6. **F-21:** one default-size constant: `StyleResolver`'s 14 sp default is replaced by the document's `docDefaults`/default-style size (Sample-6: 12 pt), falling back to 12 pt; toolbar chip, paginator and renderer read the same constant (audit-003 D5 closes with it).
**Files:** `LayoutEngine.kt`, `TextMetrics.kt` (now load-bearing), `data/odf/SvXMLImport.kt`, `OfficeDocumentParser.kt`, `OfficeDocument.kt`, `LayoutDrivenDocumentRenderer.kt` (line heights).
**Tests:** `ParagraphMetricsTest` (Sample-6 `Normal`: 116 % line height, 0.282 cm after), `PaginationFidelityTest` (windows, both formats), the dump test re-run showing no zero-element pages unless the document authors one, caret/selection/undo suites unchanged.
**Acceptance:** Sample-5 in `15..21`, Sample-6 in `15..26` (ODT **and** DOCX), no empty page in any sample, one-page documents stay one page.
**Size:** large. **Risk:** medium-high (upstream assertions move). Mitigations: windows not exact counts; `forceRebuildAll` spot checks; `TextMetrics` reverts alone.

### 4.5 PR 17 — Plan 6: image pipeline and load performance (unchanged from v1)

Closes F-07, F-18, the image half of save integrity (refusal per §0), O-01's minimal guard.
1. **F-1:** extents parsed in the live path for both formats (`wp:extent` EMU → `emuToUnits`; `svg:width/height` through `LayoutUnits`); `OfficeImage` carries units; dead plumbing deleted (`DocxDocumentParser.parseDocxFile`, `imageExtents`, `InkyModule.docxExtents`).
2. **F-2:** media store in `filesDir/media/<sha>/` with manifest + LRU cap + self-heal re-extraction on miss.
3. **F-3:** decode without a blank frame: explicit `size()` from the extent, placeholder, `crossfade(false)`, pre-decode of first pages during layout.
4. **F-4:** the three `delay()` calls (`InkyModule.kt:1105-1109`) die; `loadingProgressStatus` is driven by real stages (the string resources from Plan 2 stay).
5. **F-5:** the save guard per §0: refuse with a clear en_US dialog when the model holds images the writer cannot serialise; the `[Image: path]` branch (`DocxDocumentParser.kt:723`) is made unreachable, then deleted.
**Tests:** `ImageExtentParsingTest` (Sample-6 ODT/DOCX → same dp triple), `MediaStoreTest` (self-heal, cap), `PaginationImageTest` (taller-than-page image still paginates), `OpenLatencyTest` (no artificial delay in the open path).
**Acceptance:** Recents open shows text in one frame and images within one frame of decode; a cache wipe cannot destroy media; both formats render the same picture the same size.
**Size:** medium.

### 4.6 PR 18 — Plan 7A: ODF numbering, heading runs, hyperlinks, ⚑ bookmarks

**Goal:** structure the samples actually contain. Closes G-1, G-3, G-4, ⚑G-4b (new), F-12, F-13, F-15, O-04, O-05 groundwork.

1. **G-1:** tokens + parser for `text:list-style` and its level styles from **both** `content.xml` and `styles.xml` (§2.4.1), including empty `style:num-format` (§2.4.2), `num-prefix/suffix`, `display-levels`, `start-value`, `bullet-char`, per-level text/paragraph properties, `text:continue-numbering` (35 occurrences in Sample-6, §2.1), and resolution via paragraph auto-styles (`P5` → `Makalah_20_Default`). Output through E-EN-4's `NumberingSpec`; the counter renders `BAB 1`, `2.1`, `a.`, `•`. The hard-coded `• `/`◦ ` dies. Sample-5's headings-as-paragraphs keep working (§2.4.6).
2. **G-3:** `XML_A` arm for paragraph/heading contexts (the token exists at `OdfXmlToken.kt:24`, no context consumes it); `OfficeTextRun` gains an optional link; Samples 2/4/5/6 stop losing link text (45 occurrences in Sample-6 alone).
3. **G-4:** `Heading` and `ListItem` carry runs; `toOfficeDocument()` stops writing synthetic/empty run lists.
4. **⚑ G-4b (new) · Bookmarks.** `XML_BOOKMARK`/`-START`/`-END` arms (tokens already exist, unconsumed) into a lightweight `OfficeBookmark` (name, anchor position) feeding the Navigator's bookmarks category (§1.3) and the `_TOCxxxxx` anchors the ODT TOC snapshots reference (Sample-6: 46 start/end pairs, all named). The DOCX arm (`w:bookmarkStart/End`, 46 in Sample-6) lands in PR 20 alongside the style-chain work — same file, same pass — so both formats expose anchors to the TOC snapshot rendering (19/21) and the Navigator stub (13) in the same PR wave.
**Files:** `data/odf/OdfXmlToken.kt`, `data/odf/SvXMLImport.kt`, `data/odf/SvXMLImportContext.kt`, `data/OfficeDocument.kt` (+`NumberingSpec`/`CounterState`), `UniversalNavigatorSheet.kt` (bookmarks + headings data feed).
**Tests:** `OdtListNumberingTest` (Sample-6: `BAB`-shaped labels with per-chapter restarts; `Subbab` levels 2–3 render no number; Sample-1/2/4/5 labels present and Sample-3 unchanged), `HyperlinkFidelityTest` (Samples 2/4/5/6 text counts), `HeadingRunsTest`, `OdtBookmarkTest` (Sample-6: 46 named anchors survive).
**Acceptance:** Sample-6 headings numbered, list labels at the level's own font size, no hyperlink text lost, bookmarks listed in the Navigator; page windows still hold.
**Size:** large.

### 4.7 PR 19 — Plan 7B: ODF TOC snapshot, table geometry, font identity, ⚑ sections

Closes G-2, G-5, G-6, ⚑G-7 (new), F-08, F-09, F-11, F-14, F-23.

1. **G-2:** tokens for `text:table-of-content` / `-source` / `text:index-body` / `text:index-title` / `-entry-template`; the authored snapshot parses into a `TableOfContent` element (level, text, page number, anchor) and renders with the right tab stop and `TOC 1/2/3` styles, roman pages included. Per §0: snapshot only; regeneration is a later feature. **Stress case: Sample-6 (45 entries/45 links), then Sample-4 (22/21), Sample-2 (15/15), Sample-5 (15/14)** (§2.1).
2. **G-5:** column widths from `table:table-column` styles (Sample-6's widths live in the column auto-styles); cell properties (padding, borders, background, v-align, spans); `OfficeTable` carries column weights; height from real cell metrics (the `rows*35+10` dies); rows paginate across pages. **The F-23 parse trace lands first** (header-row-as-paragraphs mechanism), per audit-006 §1.3.
3. **G-6:** renderer consumes `FontRegistry` so Times New Roman body text paints as Liberation Serif with Liberation Serif metrics (per §0; real `Typeface` loading is Plan 10 A1).
4. **⚑ G-7 (new) · Sections.** A `text:section` token + context capturing the section name and its style (Sample-6: five sections). The children already parse through the base context, so this is identity, not content. Model shape is settled at implementation — a lightweight `OfficeSection` (name, styleName) wrapping the child element list, or a named attribute on the first paragraph, whichever the `SvXMLImportContext` nesting makes mechanical; either way it exposes the section name to the Navigator's sections category (PR 13 stub), the status-bar object info (PR 13 slot), and gives PR 21's DOCX `sectPr` handling a structural counterpart (ODT section ↔ DOCX section, §2.4.7).
**Files:** `data/odf/OdfXmlToken.kt`, `data/odf/SvXMLImport*.kt`, `data/OfficeDocument.kt`, `LayoutEngine.kt` (table metrics), `LayoutDrivenDocumentRenderer.kt` (font + TOC render).
**Tests:** `OdtTocTest` (Samples 2/4/5/6: entries present, none empty, page numbers from the file including roman), `OdtTableGeometryTest` (Sample-6 declared widths, header row inside the table), `OdtSectionTest` (Sample-6: 5 named sections), `FontRegistryTest` (TNR body renders through the substitute; heading sizes equal the style sheet), Sample-3 unchanged.
**Acceptance:** Sample-6.odt fidelity checklist green.
**Size:** large.

### 4.8 PR 20 — Plan 8A: DOCX style chain + run formatting (⚑ scope added)

Closes H-1, H-2, F-16, F-20, the DOCX half of F-10.

1. **H-1:** parse `w:docDefaults` (`rPrDefault`/`pPrDefault`) and every `w:style` with its `basedOn` chain and full `w:rPr`/`w:pPr`; `pStyle` keeps the DOCX style id. `para1` resolves to 20 pt Aptos Display `#0f4761` via the chain (§2.3 item 2); body resolves to docDefaults' 12 pt — not the 24/20/16 fallback or 14 sp.
2. **⚑ H-1b (new) · Character-style linkage.** Resolution precedence: explicit `w:link` wins (Sample-4: 27 of them); then Word's **naming convention** (paragraph style `X` ↔ character style `X Char`, case-insensitive, trailing-space tolerant); if neither matches, the paragraph style's own `w:rPr` applies unchanged — Sample-6 has no `w:link` attribute at all (§2.3 item 2), so convention-only files must resolve. The resolved character properties (font, size, colour, bold) merge into the paragraph's default run properties; a direct run `w:rPr` always overrides both.
3. **⚑ H-1c (new) · Numbering state per paragraph.** While numbering *rendering* is PR 21, the style-chain work must record per paragraph one of: **inherited** (`numId`/`ilvl` from the style chain, walked through `w:basedOn` — Sample-6 headings: `numId 15` inside `w:style/w:pPr`), **explicit** (a direct `w:numPr` — 42 paragraphs), or **suppressed** (direct `w:numId 0`, ECMA-376 §17.9.18 — 3 paragraphs). PR 20 resolves; PR 21 renders. This is why H-1 must read `w:numPr` inside styles at all.
4. **H-2:** `TextRun`s built per `w:r` from resolved character properties; `w:val="0"|"false"` is an explicit negative (negative-flag support in `OfficeRuns.mergeRun`); run flags stop leaking past their run (reset at run end, not at `</w:p>`); **`currentRuns` actually reaches the paragraph** (today it is declared at `OfficeDocumentParser.kt:800` and never populated — every DOCX paragraph is one flat run).
5. **⚑ H-2b (new) · Retire the sample-tuned regexes.** `PARA_STYLE_REGEX` (`para[1-9]`), `HEADING_STYLE_REGEX` (`heading[1-9]`) and the `SINGLE_DIGIT` heuristic (`OfficeDocumentParser.kt:55-57` and the `w:pStyle` arm) are replaced by the real style-table lookup (H-1); a styleId absent from the file's own style table resolves to the Normal-based default (not a heading). The regexes were tuned to the samples' styleId shape; the lookup is what the spec says.
**Files:** `OfficeDocumentParser.kt` (new `word/styles.xml` reader + run path), `data/OfficeDocument.kt` (run + numbering-state fields), `data/DocxDocumentParser.kt` (nothing — save path is PR 22).
**Tests:** `DocxStyleChainTest` (Sample-6: `para1` → 20 pt Aptos Display `#0f4761`; `para2` → 16 pt; `para3` → 14 pt with inherited font; docDefaults → 12 pt; Sample-3 as control), `DocxRunFormattingTest` (no leak; explicit unbold via `w:val="0"`), `DocxHeadingNumberingStateTest` (6/11/28 distribution; 42 explicit + 3 suppressed flagged), `DocxCharLinkTest` (Sample-4 resolves via `w:link`; Sample-6 resolves via the name convention), Sample-3.docx unchanged.
**Acceptance:** heading/body sizes and fonts come from the file in every sample; no run-flag leak; Sample-3 control green.
**Size:** large.

### 4.9 PR 21 — Plan 8B: DOCX numbering, fields, tables, sections, ⚑ TOC snapshot

Closes H-3…H-7, ⚑H-3b/⚑H-4b/⚑H-6b (new), F-13 DOCX parity, F-17, F-19, F-26, F-27, F-28 tail, O-03, O-02 remaining.

1. **H-3:** `word/numbering.xml` reader (`abstractNum`/`num`/`lvl` with `start`, `numFmt`, `lvlText`, `lvlJc`, `suff`, `ind`, `isLgl` (45 levels in Sample-4), `lvlOverride`/`startOverride`) resolved per paragraph into `NumberingSpec`, honouring H-1c's precedence — direct `w:numPr` (including `numId=0`) → `pStyle` chain walked through `w:basedOn` → none: Sample-6's 42 headings get `BAB N` from `abstractNum 15` (`BAB %1`, `%1.%2`, …), and its 3 `numId=0` headings (KATA PENGANTAR, DAFTAR ISI, +1) render **without** a prefix. Five of six samples exercise the reader (§2.2).
2. **H-4:** the field model: `w:fldSimple` **and** `w:fldChar` complex fields (begin/separate/end) with `TOC`, `PAGEREF`, `SEQ` handling (§2.4.3); instructions never reach body text (`SEQ` count in parsed Sample-1 = 0).
3. **⚑ H-4b (new) · TOC snapshot, DOCX half.** The authored TOC renders as the ODT half does (G-2): entries styled `toc 1/2/3`, each entry's **paragraph-level** `w:tabs` right-aligned dot leader (Sample-6: `pos 9027`, `leader="dot"` — §2.3 item 5), literal page numbers, and `w:hyperlink w:anchor="_TOCxxxxx"` preserved as anchors (bookmarks from PR 20's DOCX arm). The ` TOC \o "1 - 9" \z ` instruction never appears in output. Entry detection is by **toc-styled paragraph, regardless of whether it sits inside a field-result run** (the samples author the snapshot as plain paragraphs). The test asserts the verified sizes (Sample-2: 15, Sample-4: 21, Sample-5: 14, Sample-6: 45; §2.2), handles the `&quot;`-escaped field instruction (Sample-4), and does not list the unused `toc 3…9`/`TOC Heading` *style definitions* (Sample-4) as entries.
4. **H-5:** `w:tblGrid`/`w:tblW`/`w:tcW` → column weights; `gridSpan`/`vMerge` parsed into the span fields (none in the samples, but the model exists); `tcPr`/`trPr` (`tblHeader` repeat); shared geometry with G-5.
5. **H-6:** per-paragraph governing `w:sectPr` (`w:pPr/w:sectPr` starts a section; body-level closes the last); Sample-6's five sections paginate with their own geometry.
6. **⚑ H-6b (new) · Section page numbering.** The section model carries `w:pgNumType` (fmt/start) and `w:titlePg` (§2.3 item 6): front matter lowerRoman (ii/iii), body decimal restarting at 1. This is the data behind the status bar's page-number-vs-sequence-number field (WG Ch.1) and the Navigator's "Page" category sequence numbers. `w:headerReference`/`w:footerReference` are recorded in the model but **not rendered** — page furniture has no plan item, and PR 21 must not grow it.
7. **H-7:** `w:lastRenderedPageBreak` excluded from pagination and editable text (Sample-2's 22 occurrences are the regression case).
**Files:** `OfficeDocumentParser.kt` (numbering/field/section readers + run path completion), `data/OfficeDocument.kt`, `data/DocxDocumentParser.kt` (TOC rendering only — save is PR 22), `LayoutEngine.kt` (section-aware pagination).
**Tests:** `DocxNumberingTest` (Sample-6: `BAB 1…3` on the 42 numbered headings, none on the 3 suppressed; Sample-4 legal-numbered list; Sample-3: zero labels), `DocxFieldTest` (no `SEQ`/`TOC \`/`PAGEREF` instruction text in parsed output for Samples 1/2/6), `DocxTocTest` (entries, dot leaders, literal pages, `_TOC` anchors), `DocxTableGeometryTest`, `DocxSectionGeometryTest` (5 sections → 5 geometry boxes; pgNumType recorded), and **the convergence test: Sample-6 ODT and DOCX paginate into the same page windows** (§2.4.7).
**Acceptance:** Sample-6.docx fidelity checklist; both formats converge on the same page windows; **the §0 staged-tightening commit lands here** (windows move toward ±10 % of 18/21, never below).
**Size:** large.

### 4.10 PR 22 — Plan 9: save round-trip integrity (unchanged from v1, pre-change gate first)

Closes O-01, the non-destructive-package rule, plan-03 3.13, 3.19, 3.20, 3.26, 3.27, and retires F-2 (`OfficeDocElement`).

Scheduled **after** 17/19/21, with its own pre-change gate (plan-04-to-09 § Plan 9 text stays the seed): a real ODT writer (styles, list styles, TOC, manifest entries per ODF Part 2, `style:font-face`, `office:version` 1.4 — today `generateOdtXml` at `DocxDocumentParser.kt:748+` writes a bare `office:document-content` with `office:version="1.2"` and no styles at all), a real DOCX writer (`styles.xml` consistent with the regenerated `document.xml` — heading/char pairs per §2.3, `numbering.xml` references that exist, `w:tblGrid`, images in the package with rels), the original-package-bytes fallback removed once round trip is proven, and the `OfficeDocElement` wrapper deleted in a mechanical final commit.
**Acceptance:** open → save → reopen preserves text, styles, numbering, tables, images, TOC for Sample-6 in both formats, verified by a CI round-trip test. **Scope is firmed by a short plan document before this PR starts; do not start it from this paragraph alone.**

### 4.11 Plan 10 — stays parked

Thread A (real `Typeface` loading, A2 policy write-down, A3 Font Style UI, A4 SAF/user fonts, A5 metrics-parity test) and B2–B6 resume **after PR 21 converges both formats and the user re-confirms** (the §0 display decision is already binding on `FontRegistry` from PR 15, so A1 is an upgrade of the loader, not a redesign). B1 (the `DESIGN.md`/m3.material.io review) lands early as documentation in PR 14.

### 4.12 Plan 1 — master index updates (living document, per PR)

Per plan-01 §6's update rule, every PR's **final commit** contains:

| PR | plan-01 registry change (§2) | plan file line |
|---|---|---|
| 13 | row 3 → "3A landed (PR 12); 3B landed (PR 13)" | plan-03 §8 gets a 3B record (3.5–3.11, 3.28, ⚑3.32/⚑3.33 as shipped) |
| 14 | row 3 → "3C landed (PR 14)" | plan-03 §8 gets a 3C record |
| 15 | row 5 → "5A landed (PR 15)" | plan-04-to-09 § Plan 5 gets E-0/E-7/⚑centralisation record |
| 16 | row 5 → "landed (PR 15-16)" | § Plan 5 gets the windows actually achieved |
| 17 | row 6 → landed | § Plan 6 record |
| 18 | row 7 → "7A landed (PR 18)" | § Plan 7 gets G-1/G-3/G-4/⚑G-4b record |
| 19 | row 7 → "landed (PR 18-19)" | § Plan 7 gets G-2/G-5/G-6/⚑G-7 record |
| 20 | row 8 → "8A landed (PR 20)" | § Plan 8 gets H-1(+b/c)/H-2(+b) record |
| 21 | row 8 → "landed (PR 20-21)" | § Plan 8 gets H-3…H-7(+b) record + the tightened windows |
| 22 | row 9 → landed | § Plan 9 record; plan-01 §4 checklist items 1/10 close |
| 10 resume | row 10 status change when it starts | plan-10 head note |

One-time plan-1 changes with PR 13's commits (they describe state now): registry row 3 status (3A **landed**, not "in review"); §3 WG-mapping gains three owner lines (Navigator categories → 13/18/19/20/21; status-bar object info → 13 + 19/21; bookmarks → 18/20); §3.1's sidebar-deck row loses its "lacks Images" half and keeps "lacks the Hyperlinks category"; §3.1's menu-bar row's "6 of 8 ribbon tabs are empty" becomes "4 of 6 declared tabs have no deck yet (References/Mailings dropped per CONCEPT.md)"; §6's "Next actions" item 2 is struck through as done.

---

## 5. Sequencing, parallelism, and the device checklist

| Order | PR | Parallel with | Gate to enter the next |
|---|---|---|---|
| 1 | 13 (3B) | 14 (docs-only) | Delivery Gate PASS at 320 dp; grey allowances 0 |
| 2 | 15 (5A) | 13/14 if a second session is free | dump answers F-25; windows unmoved |
| 3 | 16 (5B) | — | both-format windows green |
| 4 | 17 (6) | 20 (8A) scaffolding | no blank frame; refusal proven |
| 5 | 18 (7A) | 20 (8A) scaffolding | ODT numbering fidelity |
| 6 | 19 (7B) | tail of 18 | ODT checklist green |
| 7 | 20 (8A) | — | DOCX style chain green |
| 8 | 21 (8B) | — | both-format convergence + tightening commit |
| 9 | 22 (9) | plan 10 resume decision | round-trip CI test |

**Device checklist resume points** (`docs/InkyC1Checklist.md`, deliberately postponed; section order = item number):

1. **Now (before PR 15):** install the latest nightly (≥ PR 12), run plan-02 §5 acceptance and checklist items 5 (Selection), 8 (Reminder), 9 (Zoom), plus the `[needs run]` device log for the Viewer FCT platform-menu question.
2. **After PR 16:** items 2 (editing stages), 4 (Caret), 6 (Go To with believable counts), 9 re-run, 11 (Session Restore: page 15 at 170 %, migrated zoom semantics).
3. **After PR 19/21:** item 7 (Navigator categories — now including the Images/Hyperlinks/Bookmarks/Sections data that 15/17/18/19/20/21 feed), item 10 (Save Compatibility), and the item 12 stress test after PR 17 (media memory).

---

## 6. Cross-plan invariants (unchanged from plan-01 §5)

One paginator, one unit system, one style resolver, one numbering model, one media store — a plan that adds a second path deletes the first in the same PR. No new hard-coded copy or `contentDescription` literal (the PR 12 guard enforces this mechanically). 48 dp targets, token colours only, verified at 320 dp. No em dash in user-visible strings. Evidence, not claims: every PR body lists the findings it closes, the suite it ran, and, for UI, the Delivery Gate with device evidence. The 12-file sample matrix (§2) is the floor.

---

## 7. Deltas against v1 (what changed and why)

1. **Baseline moved** `e10f956` → `55a9a97` (PR 12 landed); §1.3 re-verified at the new baseline; the PR 12 section is a pointer, not a task.
2. **Sample-2 TOC corrected:** v1's "6 TOC instances" was a prefix-grep artifact; the true count is 1 (15 entries/15 links). The heaviest TOC stress case is **Sample-6** (45/45), not Sample-2. G-2's test list now orders Samples 6/4/2/5.
3. **Sample matrix re-derived with explicit tag boundaries (§2).** Every figure in v1's two tables was re-counted; the ODT list column is now "lists / items" (70/84, 41/103, 0/0, 15/18, 123/186, 115/180) because v1's single "lists" number was not reproducible from one stated rule, and the ODT bookmark column is split into start/end pairs (Sample-6: 46/46 — the ODT half of the DOCX `w:bookmarkStart` count, not "92 bookmarks").
4. **⚑ Sample-6 DOCX heading architecture added (§2.3)** — v1 said the heading runs "lack rFonts/sz" and left the `w:link` question open. Verified reality: the *styles* carry the numbering (`numId 15`, `BAB %1`), the *char styles* link by naming convention (no `w:link` in the file, 27 in Sample-4), 42 headings carry explicit `numPr` and 3 carry `numId=0` suppression, page breaks sit inside heading paragraphs, and the TOC is an authored snapshot of `toc 1/2/3`-styled hyperlink paragraphs with **paragraph-level** dot-leader tabs. This grows PR 20 (H-1b, H-1c, H-2b) and PR 21 (⚑H-3 style-inherited numbering, ⚑H-4b TOC snapshot, ⚑H-6b pgNumType).
5. **⚑ Navigator honesty item (3.32)** — 13 categories, one generic empty message, and no Hyperlinks category were not in v1's PR 13. The classification is evidence-based: the "not yet readable" set is exactly the element classes **no main-source file constructs** (§4.1 item 6), and the PR ships a test that re-derives that split from the source tree.
6. **⚑ Status-bar object information (3.33)** — WG Ch.1's "section or object information" field (Table 1) had no owner; the honest-minimal version lands in 13 through the caret seam the toolbar hub already uses, and the full version follows 19/21.
7. **⚑ Bookmarks (G-4b in 18, DOCX arm in 20)** — tokens exist but are unconsumed; the Navigator bookmarks category and the `_TOC` anchors need them.
8. **⚑ Sections (G-7 in 19, H-6b in 21)** — ODT `text:section` identity had no plan item; DOCX `pgNumType`/`titlePg` is now explicit (WG status-bar page-number-vs-sequence-number).
9. **⚑ E-7 centralisation (15)** — the status bar's duplicated fit-scale computation now consumes the renderer's single transform; PR 13 deliberately does not touch that region.
10. **⚑ E-2 line-height constant (16)** — the renderer's `(sizeSp + 5f)` magic line height dies with the fudge, in favour of style-driven line height.
11. **Guard baseline corrected (§1.2)** — 47 textual occurrences in 14 files, **46 in code across 13** once comments are masked; the `LayoutDrivenDocumentRenderer` allowance is stale. PR 13's scope states that every map entry is deleted.
12. **`w:br` handling corrected in the record** — the shared parse block *does* distinguish `type="page"` from soft breaks (`OfficeDocumentParser.kt:961`); v1's "still open" row implied otherwise. The remaining issue is `w:lastRenderedPageBreak` (ODT soft-page-break pollution), unchanged.
13. **`w:tabs` attribution corrected** — the first draft of this file placed the TOC dot-leader tab in the `toc 1/2/3` styles; it is a paragraph-level property in `document.xml` (§2.3 item 5, §4.9 item 3).
14. **Pagella 3.9 corrected** — the zoom pair there already has 48 dp targets through Material 3's minimum; only Cellina and Slidia override it with `Modifier.size(24.dp)` (§4.1 item 4).
15. **⚑ items re-numbered and tightened (v2.2, 2026-09-24):** the Navigator and status-bar items are now **3.32/3.33**, because 3.14/3.15 in the first draft collided with plan-03's existing ODF-conformance rows (list styles / TOC index dropped, owned by plans 7 and 9). **⚑3.32** distinguishes *verified-absent* from *not-yet-readable* and moves the justification from "the index engine populates only some categories" (it has arms for all of them, exercised by `NavigationEngineTest`) to the verified fact that **no parser constructs the element classes**; **⚑3.33** fixes the data sources (caret→element seam; heading level+text from the Navigator index; table and list-item kinds; hyphen fallback, no invented data); **⚑G-4b** pins the DOCX `w:bookmarkStart/End` arm to PR 20 so both formats expose anchors in the same wave as the TOC snapshot rendering; **⚑G-7** leaves the section model shape (wrapper vs attribute) to implementation, fixed on the exposed name; **⚑H-1b** adds the precedence (explicit `w:link` → naming convention → style's own `rPr`; a direct run always overrides); **⚑H-1c** states the three-state flag and that the `pStyle` chain is walked through `w:basedOn`; **⚑H-2b** fixes the fallback for styleIds missing from the file's own style table (Normal-based, not a heading); **⚑H-3** states the resolution precedence (direct `numPr` incl. `numId=0` → style chain → none); **⚑H-4b** fixes entry detection to "toc-styled paragraph, inside or outside a field-result run"; **⚑H-6b** records `headerReference`/`footerReference` in the model without rendering them (no plan item owns page furniture).
16. **⚑3.32 implementation additions (recorded in PR 13, v2.3):** the Indexes filter (`NavigateBy.INDEX`) was the same class of lie as the twelve categories in the All view — it fell through to the generic "There are no objects to navigate" row although a TOC/index is authored document content that plans 19/21 read — so `indexes` joined the not-yet-readable set in `NavigatorCategories` (key `indexes`, element classes hand-checked like `frames`/`ole`, owner `plan-19/21`). The "Pages" and "Reminders" Navigate-By labels became plural so the verified-absent sentence ("No %1$s in this document.") reads correctly for them; `values-in` is untouched. The Hyperlinks category carries no rows and no `Navigate-By` filter option in this PR: `NavigationEngine` has no hyperlink jump yet, so the rows and their jump land together with the parser in 18/20.

---

## 8. What still needs the user (nothing blocks)

1. The on-device passes at the three resume points in §5; the first one gates only checklist credit, not PR 13/15.
2. A re-check of post-fix screenshots after PR 13 (the R-26 surfaces change).
3. The plan-9 pre-change gate when PR 21 nears (its scope paragraph is a seed, not a commitment).
4. The staged-window tightening decision is already recorded (§0) and executes inside PR 21 — no action needed, listed for completeness.
5. **⚑ The new scope items (3.32, 3.33 in PR 13; ⚑G-4b bookmarks in PR 18/20; ⚑G-7 sections in PR 19/21; ⚑H-1b/H-1c/H-2b and ⚑H-3/H-4b/H-6b in PR 20/21)** are additive and each unblocks an existing checklist item or a verified sample fact; strike any of them and the owning PR simply drops that bullet — nothing else depends on them.

---

## Appendix A — Verification evidence (this session, at `55a9a97`)

**Sample-6 DOCX (verbatim, abridged):**

- Heading style: `<w:style w:type="paragraph" w:styleId="para1"><w:name w:val="heading 1"/><w:basedOn w:val="para0"/><w:pPr><w:numPr><w:ilvl w:val="0"/><w:numId w:val="15"/></w:numPr><w:spacing/><w:jc w:val="center"/><w:keepNext/><w:outlineLvl w:val="0"/><w:keepLines/></w:pPr><w:rPr><w:b/><w:bCs/></w:rPr></w:style>`
- Char style: `<w:style w:type="character" w:styleId="char1" w:customStyle="1"><w:name w:val="Heading 1 Char"/><w:basedOn w:val="char0"/><w:rPr><w:rFonts w:ascii="Aptos Display" …/><w:color w:val="0f4761"/><w:sz w:val="40"/>…` — **no `w:link` attribute exists anywhere in Sample-6** (regex over all 37 styles: zero matches; Sample-4 carries 27).
- Suppressed heading: `<w:p><w:pPr><w:pStyle w:val="para1"/><w:numPr><w:ilvl w:val="0"/><w:numId w:val="0"/></w:numPr>…<w:r><w:br w:type="page"/></w:r><w:bookmarkStart w:name="_TOC000002"/><w:r><w:t>KATA PENGANTAR</w:t>…`
- TOC entry: `<w:p><w:pPr><w:pStyle w:val="para16"/>(= "toc 1")<w:tabs …><w:tab w:val="right" w:pos="9027" w:leader="dot"/></w:tabs>…<w:hyperlink w:anchor="_TOC000009"><w:r><w:t>BAB 2  PEMBAHASAN</w:t><w:tab/><w:t>4</w:t></w:r></w:hyperlink></w:p>` — the `<w:tabs>` sits **in the paragraph**, and `/word/styles.xml` contains no `<w:tabs>` at all.
- Sections: `sectPr1: pgNumType=lowerRoman start=1 titlePg; sectPr2: decimal start=1; sectPr3-5: decimal`.
- `abstractNum 15`: `w:name="Makalah Default"`, `lvlText` = `BAB %1`, `%1.%2`, `%1.%2.%3`, … `start=1`.
- `numId` usage in `document.xml`: `15` × 42, `0` × 3, plus 19 other ids for lists.
- ECMA-376 §17.9.18 (verified online, Open XML SDK `numId` reference): *"A value of 0 for the @val attribute shall never be used to point to a numbering definition instance, and shall instead only be used to designate the removal of numbering properties at a particular level in the style hierarchy."*

**ODF (local `docs/html/OpenDocument-v1.4-part3-schema.html`, authoritative per `AGENTS.md`):** tokens confirmed present in the schema for every element the plans parse — `text:list-style`, `text:list-level-style-number`, `text:display-levels`, `text:table-of-content`, `text:index-body`, `text:continue-numbering`, `style:table-column-properties`, `style:column-width`, `text:numbered-paragraph`, `text:bookmark`, `fo:break-before`, `widows`, `orphans`.

**Writer Guide Ch.1 status bar (read online this session):** "Shows the sequence number of the current page, the total number of pages in the document, and the current page number (if different from the sequence number)"; "If you select a portion of text, the count for that selection will temporarily replace the document total count"; Table 1 = Image or Frame → size and position; List item → level and (if relevant) list style; Heading → heading numbering level and (if relevant) list style; Table → name or number and cell reference of cursor; Section → name of section.

**Engine (file:line at `55a9a97`):** `LayoutEngine.kt` — `elementGapDp` :107, `StyleResolver` 14 sp :61, `2.5f` fudge :154, table `rows*35+10`; `LayoutDrivenDocumentRenderer.kt` — `PageStackMetrics` (320/452) :56-61, `fitScale`/`renderScale` :149-150, `pageScale` :243-245, line height `(sizeSp+5f)` :474/:500/:579/:596/:650, the `Color.DarkGray` mention at :665 is a comment (the grey there is already gone); `InkyModule.kt` — `isWebView` :122, cursor-ratio page estimate :524-533, delays :1105-1109, `docxExtents` :212, hub tools :2790-2811, status bar (page range + fit-scale re-implementation) :2396-2470, caret→element seam :659-707, ribbon tabs :2936, unimplemented-tab literal :3383, `activeRibbonTab` written at :2941/:3125 and never read; `OfficeDocumentParser.kt` — `extractDocxPageStyleSpec` :144, `extractDocxStyles` :218 (names only, `namespaceAware=false`), regexes :55-57, `currentRuns` :800, `w:br` type distinction :961-971, `w:tab` :958; `DocxDocumentParser.kt` — `[Image:]` :723, hard-coded `w:numId 1` :740, `generateOdtXml` (`office:version="1.2"`) :748+; `OdfXmlToken.kt` — `XML_A` :24, `XML_BOOKMARK*` :39-41, no list-style/index tokens; `SvXMLImportContext.kt` — hard-coded bullets, no `XML_A`/bookmark arms, soft-page-break mid-paragraph.

**Navigator (⚑):** `UniversalNavigatorSheet.kt` categories = bookmarks, comments, fields, footnotes, frames, headings, images, ole, pages, sections, shapes, tables; every empty one renders `R.string.no_objects_to_navigate`; no hyperlinks category. **Index reachability (verified):** no main-source file constructs `OfficeBookmark(`, `OfficeComment(`, `OfficeSection(`, `OfficeShape(`, `OfficeField(`, `OfficeFootnoteElement(` or `OfficeHyperlink(`; `OfficeParagraph.bookmark` is written only by `DocumentCoreEngines.insertBookmark`; `OfficeTextRun.hyperlink`/`field` are never assigned by a parser; `DocumentIndexEngine.framesList` is never appended to; `OfficeResources.objects` is never populated. `NavigationEngineTest` exercises the index arms with a synthetic document, which is why the arms exist and still cannot fire on a real file.

**Strings/guard:** `values/strings.xml` 701 keys; `values-in/strings.xml` 27 keys (frozen); `SourceHygieneGuardTest` — 47 recorded allowance (46 measured) across 14/13 files, 7 banned literals, five active rules (literal toasts, literal `contentDescription`, raw greys above allowance, em dashes in literals, resurrected 3.3 literals), comment-masking self-test. A Python port of the guard (same masking, same rules) reports "clean" for every rule except the grey allowances at `55a9a97`.

**CI:** `.github/workflows/build.yml` — job `test` (`./gradlew testDebugUnitTest`, zulu JDK 17) on push/PR/schedule/manual; job `build` (SemVer/nightly debug APK, manual release). The guard runs in `testDebugUnitTest` — no workflow change needed for any PR here.

## Appendix B — Later stage (explicitly out of scope for PRs 13–22)

`tests/cellina/Sample-1.{ods,xlsx}` and `tests/slidia/Sample-1.{odp,pptx}` are unpacked and inventoried (ODS: 12 tables/312 rows/1852 cells, 0 formulas; XLSX: 12 sheets, 48 shared strings, 2 formulas; ODP: 51 pages/506 text boxes; PPTX: 51 slides/996 shapes/55 media) as the floor for the Cellina/Slidia stages. They are referenced here only so the later-stage plans start from verified structure. Nothing in PRs 13–22 reads, tests or changes them.
