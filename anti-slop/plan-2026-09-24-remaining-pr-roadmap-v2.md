# Papirus Office: PR Split Strategy v2 (Plans 3B–10 + Plan 1)

**Date:** 2026-09-24 (v2.2: every ⚑ item re-verified against the samples and the specs; corrections listed in §7.12)
**Amended:** 2026-09-26 (v2.4, baseline `5c99072`, PR #14 merged). PR 16 is split into **PR 16a (Plan 5B)** and **PR 16b (Plan 5C)**; page-count windows are **per format**; PR 15 gains a sample-matrix test, the E-0 dump over all six pairs and header/footer heights in `PageStyleSpec`. Evidence and the user's decisions of 2026-09-26 are in `audit-007-2026-09-26-sample-matrix.md`; deltas in §7.17.
**Amended:** 2026-09-27 (v2.5). Plan 5 is renumbered 5a–5e: **5b** = fixture re-baseline (PR #16), **5c** = documentation refresh, **5d** = the former PR 16a / Plan 5B (breaks and defaults), **5e** = the former PR 16b / Plan 5C (metrics and windows). The regenerated fixtures (all DOCX from M365, all ODT from Collabora 26.04) replace the sample facts of audit-007 §1–§5 and the windows of §11.3; see `audit-008-2026-09-27-fixture-rebaseline.md`, which wins over this file wherever they disagree about the samples.
**Amended:** 2026-10-03 (v2.6). The former combined Plan 7B is split into four plans and exactly four PRs: **7B** canonical semantic model/importer convergence, **7C** authored indexes and named sections, **7D** tables end to end, and **7E** font-face aliases/final calibration. This amendment supersedes every older combined-7B ownership or downstream PR forecast in this file. Evidence and boundaries: `audit-014-2026-10-03-plan-7b-convergence.md`.
**Supersedes:** `plan-2026-09-24-remaining-pr-roadmap.md` (v1, baseline `e10f956`). v1 is kept as the record of the pre-PR-12 schedule; where this file and v1 conflict, this file wins.
**Baseline:** `main` `55a9a97` (PR #12 merged), branch convention `arena/<session>-papirus-office`, CI is the only compile/test evidence (no local JDK; `./gradlew testDebugUnitTest` + the build job in `.github/workflows/build.yml`).
**Relationship to plan 1:** `plan-01-master-index.md` keeps the WG-chapter mapping, the checklist mapping and the finding registry; this file keeps the PR order and the per-PR scope. Plan 1's update rule (§6) is executed per PR as tabulated in §4.12.
**Evidence base:** every plan and audit in `anti-slop/` re-read; all 12 `tests/inky` files re-unpacked and re-inventoried (§2, raw XML, method stated); the ODF 1.4 Part 3 schema in `docs/odf` probed for every element a parser must read (the path was written `docs/html` here and corrected on 2026-09-28); ECMA-376 §17.9.18 (`w:numId=0`) and the LibreOffice Writer Guide Chapter 1 ("Status bar") read online; every file:line citation re-opened in the working tree at `55a9a97` on 2026-09-24. Where a v1 number could not be reproduced, it was replaced by a count whose rule is written next to it. No figure in this file is quoted from another document without re-deriving it.

---

## 0. Binding decisions (carried over; no re-asking)

From v1 §0 (audit-005 §6 / audit-006 §5 answers) and plan-03 §0:

| Decision | Consequence carried into this file |
|---|---|
| Page-count tolerance = **staged windows, per format** (amended 2026-09-26, audit-007 §11.3). DOCX windows around the M365 references 15/23/20/10/18/21: `12..18`, `18..28`, `16..24`, `8..12`, `15..21`, `15..26`. ODT windows are provisional until the user regenerates the six `.odt` fixtures with Collabora Office: `9..18`, `15..22`, `12..20`, `7..12`, `12..21`, `15..26`. Tighten to ±10 % of the references after Plans 7/8, never below. Geometry is honoured **as each file declares it** (no repairing an ODT from its DOCX twin). | Plan 5E landed the windows. Plan 7E performs the ODT post-structure remeasurement; Plan 8B performs cross-format convergence. Tightening requires measured support, not a metrics-only forecast. |
| Font supply = **display the bundled metric-compatible faces** (TNR→Liberation Serif, Calibri→Carlito, Cambria→Caladea, Arial/Helvetica→Liberation Sans, Courier New→Liberation Mono, Symbol/Wingdings→OpenSymbol) in both pagination and painting from the moment `FontRegistry` exists | Plan 5A wired the mapping seam; Plan 7E parses ODF aliases and makes metrics/display agree; Plan 10 A1 upgrades broader loading to real `Typeface`s. |
| ODT TOC = **authored snapshot** (no "Update Index" stub); regeneration is a later feature after a field model exists | Plan 7C owns G-2; Plan 8B's DOCX TOC handling mirrors it (the DOCX samples carry authored snapshots, §2.2). |
| Save with unserialisable images = **refuse with a clear en_US message**, no `[Image: path]` placeholders | PR 17 F-5; PR 22 removes the need. |
| en_US everywhere in the UI; `values-in` frozen at its 27 keys; document content (style names) is data | PR 12 landed the sweep; the guard enforces it mechanically from `55a9a97` on. |
| Disabled ribbon tabs get an honest visible note (not silent decks) | PR 13 3.6. |
| antislop runs **Mode 1 (during the work)**: the standing project default recorded in `plan-2026-09-22-remaining-writer-fixes.md` §5 | Every PR below is written to the rules rather than audited after them; the Delivery Gate report is attached per PR. |

**New in v2, recorded here for the user to strike:** items ⚑3.32 and ⚑3.33 (PR 13: Navigator category honesty, status-bar object information), ⚑G-4b (historically forecast as PR 18/20; ODF half shipped in 7A) and ⚑G-7 (now owned by 7C for ODF and 8B for DOCX). Each is additive, small, and unblocks an existing checklist item or a verified sample fact rather than inventing a feature. They are numbered **above** plan-03's existing range because 3.13–3.31 are already taken by the ODF/OOXML conformance and documentation rows (§5–§6 of that file).

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

**Correction recorded here:** the "47 across 14 files" quoted by both v1-derived text and plan-03 §8 is the count of *textual* occurrences in the 14-file set. One of them (`LayoutDrivenDocumentRenderer.kt:665`) sits inside the comment that explains why the grey there was already replaced by `outline`/`onSurfaceVariant` in PR #11, and the guard masks comments before counting. The masked truth is **46 occurrences in 13 files**; `LayoutDrivenDocumentRenderer`'s allowance of 1 is stale and is deleted with the rest. The guard's own header assigns this burn-down to 3B; PR 13 scope below includes deleting every map entry as the allowances reach zero.

### 1.3 Still open (re-verified this session; ✅ = confirmed at the cited location at `55a9a97`)

| Still open | Where | Owning PR |
|---|---|---|
| `fontSizeSp * 2.5f` measuring fudge | ✅ `LayoutEngine.kt:154` | 16b (5C) |
| Flat `elementGapDp = 12f` between all elements | ✅ `LayoutEngine.kt:107` | 16b (5C) |
| `StyleResolver` 14 sp default on null/miss (F-21) | ✅ `LayoutEngine.kt:61` | 16a (5B) |
| Text scales with `renderScale` while paper maps with `pageScale` | ✅ `LayoutDrivenDocumentRenderer.kt:149-150,243-245` | 15 (5A) |
| Renderer line height = `(sizeSp + 5f)` magic constant | ✅ `LayoutDrivenDocumentRenderer.kt:474,500,579,596,650` | 16b (5C, E-2) |
| Table height `rows * 35 + 10`, all cells `10.sp` | ✅ `LayoutEngine.kt` | **7D** (ODF) / **8B** (DOCX) |
| Ribbon: 8 tabs declared, only File+Home have content, literal "…will be implemented soon." | ✅ `InkyModule.kt:2936`, `:3383-3390` (pre-13 numbering) | 13 (3B) |
| Toolbar hub image/table/link/comment toast-only (TODOs landed in 12) | ✅ `InkyModule.kt:2790-2811` | 13 (3B) |
| ⚑ Navigator: 13 categories rendered, ~20 `EmptyCategoryRow` rows, no "hyperlinks" category | ✅ `UniversalNavigatorSheet.kt` (categories: bookmarks, comments, fields, footnotes, frames, headings, images, ole, pages, sections, shapes, tables; each renders `R.string.no_objects_to_navigate` when empty) | 13 (3B) + 15/17/18/19/20/21 fill |
| ⚑ No bookmark parsing anywhere (tokens exist, unconsumed; DOCX `w:bookmarkStart/End` unhandled) | ✅ `OdfXmlToken.kt:39-41` has `XML_BOOKMARK*`, no context arm; no `bookmark` handling in the ODT/DOCX parsers | 18 (ODF) / 20 (DOCX) |
| ⚑ ODT `text:section` identity unparsed (the current six-fixture corpus has none; synthetic/schema cases are required; children already flow through generic contexts) | ✅ no section token in `OdfXmlToken.kt` | **7C (G-7)** |
| 1.4 s artificial delay (`delay(500)+delay(500)+delay(400)`) in the open path | ✅ `InkyModule.kt:1105-1109` | 17 (6) |
| `docxExtents` dead state (writes, no reads) | ✅ `InkyModule.kt:212` | 17 (6) |
| DOCX save writes `[Image: <device path>]` | ✅ `DocxDocumentParser.kt:723` | 17 (6) refuses; 22 (9) fixes |
| ODF authored index snapshot still unread (G-2); list styles, heading/list runs, hyperlinks, and bookmarks shipped in 7A | ✅ PR #26 / `audit-013`; index source/body still needs a canonical sidecar producer | **7C (G-2)** |
| DOCX style chain names-only (`w:docDefaults`/`w:basedOn` unread) | ✅ `OfficeDocumentParser.kt:218` `extractDocxStyles` → `DocxStyleMeta(styleId, name, outlineLvl, isHeading, headingLevel)` only | 20 (8A) |
| DOCX heading detection is sample-tuned regexes | ✅ `OfficeDocumentParser.kt:55-57` `PARA_STYLE_REGEX=para[1-9]`, `HEADING_STYLE_REGEX=heading[1-9]` | 20 (8A) retires them |
| `currentRuns` declared but never populated (every DOCX paragraph = one flat run) | ✅ `OfficeDocumentParser.kt:800` (dead writes at :873/:1069/:1083) | 20 (8A) |
| Run flags (`w:b/i/u`) leak across the paragraph; `w:val` unread | ✅ run-flag arms in the shared parse block | 20 (8A) |
| `word/numbering.xml` never read; writer hard-codes `w:numId="1"` | ✅ no `numPr`/`numId` handling in the parser; `DocxDocumentParser.kt:740` | 21 (8B) |
| Fields (`w:fldSimple`, `w:fldChar`/`instrText`) land in body text; `w:hyperlink` anchors dropped | ✅ shared TEXT arm appends unconditionally; no `hyperlink`/`anchor`/`fldChar` handling | 21 (8B) |
| Only the last `w:sectPr` read | ✅ `OfficeDocumentParser.kt:144` `extractDocxPageStyleSpec` | 21 (8B) |
| `w:lastRenderedPageBreak` → hard break + `--- Page Break ---` text | ✅ shared parse block (the `w:br` arm at :961 already distinguishes `type="page"` from soft breaks, and that part is correct) | 16a (5B) / 21 (8B) |
| Save round trip: ODT written as bare `office:document-content` (`office:version="1.2"`, no styles/manifest), DOCX writer references styles/numbering it does not define | ✅ `DocxDocumentParser.kt:748+` `generateOdtXml`, `saveDocxZip` | 22 (9) |

Plan 4 remains **consumed by PR #11. Do not re-execute.**

---

## 2. Sample matrix (re-inventoried this session from the raw XML)

Method: every count below is `grep -o` with an explicit tag boundary (`<text:list[ />]`, not `<text:list`), run against the unpacked copies of `tests/inky/*`. Nested elements are counted where they occur; where a count needs a qualifier the qualifier is in the cell. This replaces v1's prefix-grep numbers, which inflated Sample-2's TOC count (`<text:table-of-content` also matches `-source` and `-entry-template`) and left the list column ambiguous.

### 2.1 ODT (`content.xml` + `styles.xml`)

| # | `text:h` (levels) | `text:p` | lists / items | list-style defs (content / styles) | tables | TOC | sections | frames / images | `text:a` | bookmark-start/-end | soft-page-break |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 28 (1:4, 2:6, 3:16, 4:2) | 183 | 12 / 26 | 0 / 10 | 3 | - | - | 6 / 6 | 0 | 0 / 0 | 14 |
| 2 | 30 (1:6, 2:9, 3:15) | 308 | 41 / 103 | 0 / 25 | 2 | **1** (15 entries / 15 links) | - | 11 / 11 | 15 | 15 / 15 | 16 |
| 3 | 27 (1:7, 2:20) | 143 | 0 / 0 | 0 / 1 (`No_20_List`) | 1 | - | - | 0 / 0 | 0 | 0 / 0 | 21 |
| 4 | 25 (1:11, 2:14) | 87 | 15 / 18 | 0 / 6 | 0 | 1 (authored snapshot) | - | 1 / 1 | 2 | 22 / 22 (5 point + 17 ranged) | 3 |
| 5 | 38 (1:6, 2:8, 3:9, 4:15) | 175 | 26 / 72 | 0 / 18 | 0 | 1 (14 links) | - | 2 / 2 | 14 | 14 / 14 | 12 |
| 6 | 45 (1:6, 2:11, 3:28) | 322 | 23 / 88 | 0 / 21 | 1 | 1 (45 entries / 45 links) | - | 3 / 3 | 45 | 46 / 46 (1 point + 45 ranged) | 15 |

Cross-format checks that make the pairs provably the same documents: ODT `text:a` counts equal DOCX `w:hyperlink` counts per sample (15/23/14/45, samples 2/4/5/6), ODT `text:h` level distribution equals the DOCX `pStyle para1/2/3` distribution in Sample-6 (6/11/28), and Sample-6's list style is `Makalah_20_Default` (display name "Makalah Default") while its DOCX twin's `abstractNum 15` is `w:name="Makalah Default"`, the same style spelled per format.

Fidelity references verified in the raw XML, corrected by `audit-013` and rechecked 2026-10-03: Sample-6's chapter numbering (`style:num-prefix="BAB "`, `display-levels="1"` at level 1, `display-levels="2"` at level 2) lives in `styles.xml`; all 21 list-style definitions are there, while `content.xml` contains zero definitions and 23 list instances. Empty `style:num-format` and `style:list-style-name=""` are the no-number cases. The Sample-6 TOC carries 45 `text:a xlink:href="#_TOC…"` anchors and roman page numbers (ii/iii) for the front matter. Sample-5's page style is A4 with `margin-top 0cm`/`margin-bottom 1cm`; its default paragraph is Aptos 12 pt.

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

The TOC snapshots are authored `toc 1/2/3`-styled paragraphs (the DOCX twins of the ODT `text:table-of-content` bodies, §2.1). Sample-6's 45 entries split 6/11/28 by level, the same distribution as its 45 headings. Parsing gotchas verified in the XML: Sample-4's field instruction arrives XML-entity-escaped (` TOC \o &quot;1 - 2&quot; \z `); Sample-4 defines unused `toc 3…9` + `TOC Heading` styles, so a name-matcher must not treat style *definition* as style *usage*; `w:hyperlink` counts exceed TOC entries in Sample-4 (23 total, 21 in the TOC); and **Sample-4 is the only sample that carries `w:link`** (27 of them), which makes it the precedence test for §2.3 item 2.

No `gridSpan`/`vMerge` in any sample, so merged cells stay deprioritised (parse `tblHeader`, render, but do not build the merge model yet).

### 2.3 ⚑ The Sample-6 DOCX heading architecture (verified this session, line-by-line)

This is the single most important structural fact the DOCX plans must encode. The full picture, each item read out of the file:

1. **The heading *styles* carry the numbering.** `para1…para9` are named `heading 1…heading 9`, `basedOn para0` (Normal: Times New Roman, `w:lang id-id`). Their `w:pPr` contains `w:numPr` → `w:ilvl N-1` / **`w:numId 15`**, plus `jc=center`, `keepNext`, `keepLines`, `outlineLvl=N-1`; their `w:rPr` is only `w:b`/`w:bCs`. `w:num 15` → `abstractNum 15`, `w:name="Makalah Default"`, `lvlText` = `BAB %1`, `%1.%2`, `%1.%2.%3`, … with `start=1`.
2. **The heading *character* styles are linked by naming convention, not by `w:link`.** `char1…char9` are named `Heading 1 Char…Heading 9 Char` (`w:customStyle="1"`, `basedOn char0` = "Default Paragraph Font"). **There is no `w:link` attribute anywhere in Sample-6** (regex over all 37 styles: zero matches), while Sample-4 uses `w:link` 27 times. Word's built-in convention pairs a paragraph style named `X` with a character style named `X Char`; the engine must implement both mechanisms (explicit `w:link` first, then the name convention). Verified metrics: char1 = Aptos Display 20 pt (`w:sz 40`) `#0f4761`; char2 = Aptos Display 16 pt (`sz 32`) `#0f4761`; char3 = 14 pt (`sz 28`) `#0f4761` with **no `w:ascii` `rFonts` of its own** (only `eastAsia`/`cs`), so its family is inherited up the chain.
3. **Paragraphs override the style's numbering with direct `w:numPr`.** All 45 heading paragraphs use `pStyle para1/2/3` (6/11/28). **42** carry a direct `w:numPr` repeating the style's `numId=15`; **3** carry **`w:numId 0`** (KATA PENGANTAR, DAFTAR ISI and one more), which per ECMA-376 §17.9.18 *"shall never be used to point to a numbering definition instance, and shall instead only be used to designate the removal of numbering properties at a particular level in the style hierarchy."* Those three headings must render **without** the `BAB N` prefix; the other 42 must render **with** it, computed from the style-inherited numbering.
4. **Page breaks can live inside the heading paragraph.** KATA PENGANTAR/DAFTAR ISI begin with `w:br w:type="page"` before the bookmark and text (the ODT pair expresses the same break as `fo:break-before=page` on the auto style, so the parser must normalise both to one model fact). `w:br w:type="textWrapping"` is a soft line break, not a page break; the shared parse block already distinguishes the two (`OfficeDocumentParser.kt:961-971`).
5. **The DOCX TOC is an authored snapshot** (same decision as ODT). Entries are plain paragraphs styled `para16/17/18` = **`toc 1/2/3`** (basedOn Normal), each a `w:hyperlink w:anchor="_TOCxxxxx"` containing the entry text, a `w:tab`, and the literal page number. **Correction to the first draft of this file: the right-aligned dot-leader tab (`w:tab w:val="right" w:pos="9027" w:leader="dot"`) is a *paragraph-level* `w:tabs` in `document.xml`, not part of the `toc 1/2/3` style definitions**. The three styles carry nothing but `w:ind`/`w:spacing`. A renderer that reads the leader from the style would find nothing; the plan reads it from the entry paragraph. Bookmark `_TOC000009` sits in the real PEMBAHASAN heading; 45 `w:hyperlink w:anchor="_TOC…"` targets exist.
6. **Sections carry page-numbering formats.** The five `w:sectPr` share geometry but differ in `w:pgNumType`: §1 `lowerRoman start=1` + `titlePg` (front matter, roman ii/iii, the same roman pages the ODT TOC shows), §2 `decimal start=1` (body restarts), §3–5 `decimal` (no `start`). The status bar must be able to tell **page number** from **sequence number** (WG Ch.1 status-bar item), so the section model needs `pgNumType` (fmt/start) and `titlePg`, not just geometry.

**Consequences for the plans:** PR 20 must resolve the full style chain *including* naming-convention char links and must surface, per heading paragraph, whether numbering is inherited, explicit, or suppressed (`numId=0`) as a model fact PR 21 renders. PR 21 renders the `BAB N` labels from `abstractNum 15`, handles the three suppressed headings, and keeps the TOC snapshot intact (toc styles, paragraph-level dot leaders, literal pages, `_TOC` anchors). PR 22's writer must reproduce a `styles.xml` with the heading/char pairs and a `numbering.xml` the generated `document.xml` actually references.

### 2.4 What the matrix dictates for the plans

1. **G-1 (PR 18) reads `text:list-style` from both `content.xml` and `styles.xml`** and resolves through paragraph auto-styles (`P5` → `Makalah_20_Default`), not only `text:list` → `text:list-style-name`.
2. **The numbering model tolerates empty `style:num-format`** (Sample-6 `Subbab_*` levels 2 and 3) as "no number".
3. **H-4 (PR 21) needs complex fields**, not just `w:fldSimple` (Sample-1: nine `SEQ` fields in `w:fldChar` wrappers, zero `fldSimple`); instructions never reach body text.
4. **Sample-3 is the control file** for both structure plans (no lists, numbering, TOC or fields; one table; pure paragraph/run/style-chain behaviour): every structural PR asserts "nothing new appears, nothing old disappears".
5. **The heaviest TOC case is Sample-6** (45 entries/45 links, 19.7 KB of index body), then Sample-4 (22/21), Sample-2 (15/15), Sample-5 (15/14), not Sample-2 "six instances" as v1 stated.
6. **Sample-5's headings are `text:p` with auto-styles parenting `Judul1`** (zero `text:h`). The style cascade already handles them; G-1 must not regress it.
7. **⚑ Sample-6 is the cross-format convergence case for headings, numbering, TOC snapshots, and front-matter page labels:** it has the identical 45-heading distribution in both formats, the same chapter numbering expressed two ways (ODT outline/list style ↔ DOCX style-inherited `numId 15`), and the same roman TOC page labels (ODT literal entry pages ↔ DOCX `pgNumType lowerRoman`). The current ODT fixture has **no** `text:section`; DOCX has five `sectPr`. Section identity therefore needs synthetic/schema coverage in 7C and remains a format-semantic difference rather than a fabricated one-to-one fixture claim. Plan 8B owns cross-format pagination convergence.
8. `tests/cellina` and `tests/slidia` stay out of scope (Appendix B rule).

---

## 3. The PR sequence at a glance

Sub-item IDs keep their plan identity (E = Plan 5, F = 6, G = 7, H = 8, I = 9). "Gate" = what must be true before the next PR starts. ⚑ = new in v2.

| PR | Plan | Title | Depends on | Size | Gate to leave |
|---|---|---|---|---|---|
| 12 | 3A | Mechanical strings sweep + CI hygiene guards | none | none | **landed** (`55a9a97`) |
| 13 | 3B | Honesty and dead controls (ribbon, hub, Drive, ⚑ Navigator, ⚑ status bar) + token sweep | 12 | medium | Delivery Gate report at 320 dp; grey allowances at 0 |
| 14 | 3C | Documentation accuracy (DESIGN.md review, CONCEPT/About/nightly notes) | none (docs-only) | small | diffs against cited files |
| 15 | 5A | The measuring stick: per-page dump (all six pairs), `SampleMatrixTest`, `LayoutUnits`, `TextMetrics`, `FontRegistry` seam, header/footer heights in `PageStyleSpec`, one render transform | 12 | medium | dump answers F-25 for every pair; zero pagination change |
| 16a | 5B | Breaks and defaults: fake breaks out, real breaks and section starts in, body rect from margins + header/footer, metric-only style chain, document defaults replace the 14/24/20/16 sp constants | 15 | medium | dump shows no fake breaks; windows only widened |
| 16b | 5C | Metrics and windows: `TextMetrics` load-bearing, fudge and gap constants deleted, widows/orphans, tab stops, per-format windows for all six pairs | 16a | large | audit-007 §11.3 windows green, both formats |
| 17 | 6 | Image pipeline: extents in both formats, media store, no fake delays, save refusal | 15 | medium | no blank frame; self-heal; refusal dialog |
| 26 | 7A | ODF numbering, heading/list runs, hyperlinks, bookmarks | 5E | large | **landed**: BAB/2.1 labels render; no link text lost |
| 27 | 7B | Canonical semantic model and ODT importer convergence | 7A | medium | one package/import path; facade parity; unsupported modified save fails closed |
| 28 | 7C | Authored indexes, named section ranges, Navigator, status context | 7B | large | snapshots/ranges are navigable without duplicate body flow |
| 29 | 7D | Table structure, measurement, pagination, rendering, cell hit-testing | 7C | large | one table geometry from parser through interaction |
| 30 | 7E | ODF font-face aliases and final pagination calibration | 7D | medium | declared face drives metrics/display; ODT fixture evidence refreshed |
| 31 | 8A | DOCX style chain + run formatting (char-link convention, numId-suppression flag) | 7E | large | heading/body sizes from the file, no leak |
| 32 | 8B | DOCX numbering, fields, tables, sections, TOC snapshot | 8A (+7D shared geometry) | large | Sample-6 DOCX checklist; both formats converge; windows hold |
| 33 | 9 | Save round-trip integrity (pre-change gate first) | 6, 7E, 8B | large | open → save → reopen preserves structure |
| 34-35 | 10 | Font engine + design language, **parked** | 8B + user decision | - | resume trigger §4.11 |

Historical parallelism for Plans 3-5 is unchanged. From 7B forward the semantic dependency chain is sequential: **7B -> 7C -> 7D -> 7E -> 8A -> 8B -> 9**. Research may overlap, but implementation PRs do not: 8A must consume the settled canonical model, and 8B must consume 7D's shared geometry. Plan 11's UI packages remain a separate track (chrome only) and need their own Compose BOM bump PR first (BOM `2024.09.00` has no Material 3 Expressive API).

---

## 4. The PRs in detail

### 4.0 PR 12, Plan 3A: **landed** (`55a9a97`)

Record: plan-03 §8. The guard is the standing enforcement for 3.1–3.4/3.8; the banned-literal list (`Draft Dokumen Baru`, `Inky_Dokumen`, `Pengaturan aplikasi sukses direset`, `Hubungkan Akun Google`, `Ukuran font diubah ke`, `Menempelkan sebagai`, `Ubah Ukuran Font`) is data inside the test and grows if a later PR regresses copy.

### 4.1 PR 13, Plan 3B: honesty and dead controls

**Goal:** nothing in the UI pretends. Closes plan-03 3.5, 3.6, 3.7, 3.9, 3.10, 3.11, 3.28, plus the three ⚑ items (3.32, 3.33, and the E-7 split noted below).

**Scope**

1. **3.6 Ribbon.** Rebuild the tab list from `CONCEPT.md` (File, Home, Insert, Layout, Review, View + contextual tabs when their context exists); **drop `References`/`Mailings`** until their features exist. `CONCEPT.md` does not list them for Writer, and a permanently disabled tab is the louder lie. The tab strip keeps all six visible: the two implemented tabs switch the deck, the other four render in a disabled tone, carry an accessible reason, and a press raises the honest note as a string resource (replacing the `InkyModule.kt:3383` literal). The pager must host **only** implemented decks and map page index → tab identity explicitly, not by positional coincidence.
2. **3.5 Hub tools.** The four toast-only tools (image, table, link, comment) get the visible "not available in this build" state (38 % on-surface tint, disabled semantics, `cd_add_*` naming the reason) while still telling the user what happens on press; the `TODO(plan-6/7/8)` markers from PR 12 stay.
3. **3.7/3.28 Drive.** Keep the honest placeholder; correct `AGENTS.md` + `PROJECT_CONTEXT.md` to "placeholder, not yet implemented".
4. **3.9/3.10 Tokens and targets outside Inky.** `CellinaModule.kt:800,811` and `SlidiaModule.kt:904,915` zoom pairs → 48 dp targets (their explicit `Modifier.size(24.dp)` overrides Material 3's 48 dp minimum, which is the actual defect). **Correction:** `PagellaModule.kt:106,110` need no change: those `IconButton`s carry no size modifier, so Material 3's `minimumInteractiveComponentSize` already gives them 48 dp; the claim that they were open came from an audit line count, not from a measurement. The raw greys in those modules, `HomeDashboard`'s neighbourhood, `SwTextFormattingInspectorDialog`, `UniversalNavigatorSheet`, `CloudSyncBar`, `UniversalChartSheet`, `UniversalOdfSheet`, `UniversalEmailSheet`, `UniversalClipboardSheet`, `OfficeUiComponents`, `InkyModule` and `HomeSubpages` move to the token set. **Every one of the 46 recorded grey occurrences reaches zero and all 14 map entries, including the stale `LayoutDrivenDocumentRenderer` one, are deleted from `SourceHygieneGuardTest`** (§1.2).
5. **3.11 Palette.** Keep the Material-2014 hues as document colours; write them as explicit `Color(0xFF…)` document colours (identical values) with one `antislop-code`-compliant comment recording the decision, so the *chrome* grey rule can reach zero without repainting the picker.
6. **⚑ 3.32 Navigator categories (new).** `UniversalNavigatorSheet` renders 13 categories and, when one is empty, the same generic "no objects to navigate" line for every one, including categories whose data **no parser produces at all**. In this PR each empty category becomes one of two honest states, chosen by whether the parsers can *see* that object class:
   * **verified-absent**: "No %s in this document." Only for classes some parser constructs today: headings, tables, images, pages (and the session-only Reminders list). Verified at `55a9a97` by constructor search: `OfficeListItem(`, `OfficeTable(`, `OfficeImage(` etc. are constructed in `data/writer/OdtDocumentParser.kt` and `OfficeDocumentParser.kt`; the index engine's arms for headings/tables/images run on real documents.
   * **not-yet-readable**: a disabled row "Not yet available in this build." plus a `TODO(plan-NN)` marker in source, for **bookmarks, comments, fields, footnotes, frames, OLE objects, sections, shapes, hyperlinks**. The reason is verified, not assumed: no main-source file constructs `OfficeBookmark(`, `OfficeComment(`, `OfficeSection(`, `OfficeShape(`, `OfficeField(`, `OfficeFootnoteElement(` or `OfficeHyperlink(`; `OfficeParagraph.bookmark` is only written by the editor API `DocumentCoreEngines.insertBookmark`, `OfficeTextRun.hyperlink`/`field` are never assigned by a parser, `DocumentIndex.frames` is never appended to, and `OfficeResources.objects` (the OLE source) is never populated by any parser. The index engine *has* arms for all of them, and `NavigationEngineTest` exercises those arms with a synthetic document, but on a real opened file those lists are empty for a reason the user cannot see, and a parser that cannot read footnotes may not claim the document has none.
   * The **Hyperlinks** category is added in the not-yet-readable shape. Basis: `plan-01` §3.1's WG sidebar-deck row ("Navigator deck … lacks … Hyperlinks categories") and `AGENTS.md`'s Navigator Deck list; the Navigate-By filter option for it lands with the data (18/20), so no dead filter entry is added.
   * No jump behaviour changes in this PR; the data lands with the owning plans.
7. **⚑ 3.33 Status-bar object information (new, WG Table 1).** WG Ch.1 gives the status bar a "section or object information" field (image/frame → size+position; list item → level+list style; heading → heading level; table → name+cell reference; section → name; index → type). This PR creates the **slot** and fills it only with facts the model already proves, consumed **through the seam that already exists**: the caret-to-element mapping in `InkyModule` (`DocumentTextWindows.elementForOffset`) that the toolbar hub already uses for font tracking. Facts shown: a heading (level + text) resolved by the Navigator index, which also covers Sample-5's paragraph-styled headings, because the index is the one resolver that walks the style parents; the element kinds "Table" and "List item", and a hyphen placeholder (`"-"`, never an em dash, R-02) everywhere else. Table row/column, sections, frames and indexes are **not invented**; they fill the same slot after 19/21.
8. **⚑ E-7 split (recorded here, executed in 15):** the status bar's page-range logic re-implements the renderer's fit-scale computation inline (`InkyModule.kt:2412-2431`). PR 13 does **not** touch it (that region is under PR 15's transform work); PR 15 makes it consume the same single transform.

**Files:** `InkyModule.kt` (ribbon/hub/status-bar regions), `UniversalNavigatorSheet.kt`, the three other module files, `SwTextFormattingInspectorDialog.kt`, `CloudSyncBar.kt`, `UniversalChartSheet.kt`, `UniversalOdfSheet.kt`, `UniversalEmailSheet.kt`, `UniversalClipboardSheet.kt`, `OfficeUiComponents.kt`, `HomeSubpages.kt`, `LayoutDrivenDocumentRenderer.kt` (comment only), `strings.xml`, `SourceHygieneGuardTest.kt` (allowances → 0), `AGENTS.md`, `PROJECT_CONTEXT.md`.
**Tests:** `SourceHygieneGuardTest` green with an empty allowance map; `NavigatorCategoryHonestyTest`: asserts the readable/unreadable split **against the source tree** (a category classified readable must have an element constructor in `app/src/main/java`, a category classified unreadable must not), so a later parser PR cannot leave the Navigator lying; `WriterRibbonTabsTest`: every declared tab either maps to a deck or is marked unavailable, implemented tabs are a subset of the strip, the pager page count equals the deck count. Compose-level click-throughs stay on device (R-35): CI has no display.
**Acceptance:** Delivery Gate report with 320 dp evidence; every visible control either works or says why it does not (R-26/R-27); contrast ≥ 4.5:1 for the swept chrome; guard green with zero allowances.
**Size:** medium. **Risk:** low; it touches chrome only, no layout path.

### 4.2 PR 14, Plan 3C: documentation accuracy (unchanged from v1)

Closes plan-03 3.12, 3.29, 3.30, 3.31. `DESIGN.md` review against m3.material.io for the four families the screenshots exercise (tonal bottom bar, FAB role, 40 % sheet deck, dialog header), with deviations recorded rather than silently fixed; `CONCEPT.md` equation pipeline notes the writer-side gap (MathML/OMML produced, not embedded); About screen states the SIMULATED fallback beside the LOKit credit; nightly release body switches to commit-derived notes or is labelled a standing summary (following the `AGENTS.md` nightly rules: delete-then-recreate, `target_commitish`, `make_latest: false`).
**Tests:** none (docs + workflow). **Acceptance:** diffs only; the workflow change is proven by the next nightly run. **Size:** small. Can run in parallel with 15.

### 4.3 PR 15, Plan 5A: the measuring stick

**Goal:** infrastructure with zero pagination change, plus the trace that explains the empty pages before anything moves. Closes E-EN-1…E-EN-5, E-7 (including ⚑), the plan-2 render gap, F-25 instrumentation.

1. **E-0 · Per-page element dump (first).** Debug-only, CI-invokable: for each page of a laid-out sample, element indices, kinds, reserved heights, leftover space. Shipped as a unit test on Sample-6 (ODT + DOCX), output asserted *and* printed. The suspected empty-page mechanism, the `OfficePageBreak` arm in `LayoutEngine.kt` adding an extra **empty page** when a break lands at the top of a page (double break → blank), is confirmed or refuted by evidence here; plan-2 §1 item 7 closes in this PR, not 16.
2. **E-EN-1 · `LayoutUnits`.** One converter: `ptToUnits`, `cmToUnits`, `emuToUnits`, `twipsToUnits` (96/inch). The `* 2.5f` fudge is *not yet deleted* (16 deletes it); every new call site uses the converter; the `OdfFrameContext` 160/in drift dies here too.
3. **E-EN-2 · `TextMetrics(style): Measurable`.** Same resolved style for measure and display; Android `Paint` on device, deterministic JVM advance table in tests.
4. **E-EN-3 · `ParagraphStyle` grows metric fields** (`spaceBefore/AfterUnits`, `lineHeightFactor`, indents, `keepWithNext`, `pageBreakBefore`, `fontFamily`). Parsers don't populate them yet (16a/18/20 do); defaults keep today's rendering byte-identical.
5. **E-EN-5 · `FontRegistry` seam.** Substitution order per §0 (exact → bundled metric-compatible → user fonts → system → default), one family per name for both `TextMetrics` and `OfficeRuns.fontFamilyFor`. This PR wires the *mapping*; real `Typeface` loading is Plan 10 A1; the display decision is recorded in `FontRegistry` itself.
6. **E-7 · One render transform (+ ⚑ centralisation).** The renderer derives card size, margins, and text scale from the page's own `widthDp/heightDp` through a single `pageScale`; `PageStackMetrics.BASE_CARD_WIDTH_DP` retires from the render path. ⚑ The status bar's page-range logic consumes the same single transform as the renderer, so the two can never drift again. `PageStackMetrics` survives only as the Go-to-Page/visibility-range input until that range derives from real page heights.
**Amendments of 2026-09-26 (audit-007 §11.1):**
7. **⚑ E-0 runs on all six pairs**, not Sample-6 alone: per file it prints the body rectangle used, the default metrics used, the page count, the reference count (audit-007 §1: M365 15/23/20/10/18/21 for DOCX; ODT pending Collabora) and the number of pages holding fewer than three elements. The assertion is still only "the dump ran and named the mechanism". Code reading already refutes the double-break theory (`flushPage()` emits nothing for an empty page; audit-007 finding G); the dump confirms which files' blank pages come from the fake breaks (audit-007 §5) and which from inflated metrics.
8. **⚑ `SampleMatrixTest` (pure JVM).** Pins, for all twelve files, the declared page size, margins, header/footer heights, first master page (ODT) or section count (DOCX), body font and size, default line height and after-spacing, exactly as tabulated in audit-007 §2 and §3, and the reference counts as named constants with their provenance in a comment. `PageGeometryTest`'s "margins" test is re-worded to "declared margins" and keeps its numbers.
9. **⚑ `PageStyleSpec` gains `headerHeightDp`/`footerHeightDp`** inside E-EN-3 (fixed `svg:height`, else `fo:min-height`; ODF 1.4 Part 3 §20.407.2 / §20.212), populated by `SvXMLImport`, left 0 for DOCX (Word keeps header and footer inside the margin). Defaults keep today's pagination byte-identical; 16a makes the paginator use them. Reason: Sample-4/5 ODT carry Word's 1 in top margin as a 2.54 cm fixed-height header with `fo:margin-top="0cm"` (audit-007 finding B), so today they paginate against a 29.7 cm body.
10. **⚑ `FontRegistry` input corpus** is audit-007 §8 (eleven family names actually present in the six pairs); the Aptos stand-in is an open user choice recorded in the registry as "not metric-compatible".
**Files:** new `data/LayoutUnits.kt`, `data/TextMetrics.kt`, `data/FontRegistry.kt`; `data/OfficeDocument.kt`; `LayoutEngine.kt` (dump + seams only); `LayoutDrivenDocumentRenderer.kt` (transform); `InkyModule.kt` (transform inputs; page-range consumers); `data/odf/SvXMLImport.kt` (header/footer heights only).
**Tests:** `LayoutUnitsTest`, `TextMetricsTest`, `FontRegistrySubstitutionTest` (the §8 corpus resolves identically for metrics and display), `Plan5ElementDumpTest` (all six pairs), `SampleMatrixTest` (twelve files), and **the pagination windows must not move** (Sample-5 stays `12..30` until 16b).
**Acceptance:** no pagination-window change; at 100 % the on-screen text column equals the layout's content width at any viewport size; the dump names the empty-page mechanism per file; `SampleMatrixTest` green against the audit-007 tables.
**Size:** medium (2–3 days). **Commit plan:** audit-007 evidence (landed 2026-09-26) → dump → matrix test → units → metrics + styles (incl. header/footer heights) → fonts → transform.

### 4.4 PR 16: split on 2026-09-26 into PR 16a (Plan 5B) and PR 16b (Plan 5C)

The original single PR 16 ("honest pagination", E-2…E-6 + F-21) was rated large and medium-high risk, and audit-007 showed its two halves fail differently: the parsing half (breaks, defaults, body rectangle, style chain) is visible commit by commit in the E-0 dump, the measurement half (real advances, line heights, widows, tabs) moves every window at once. They are therefore two PRs. Item IDs keep their Plan 5 identity.

#### 4.4a PR 16a, Plan 5B: breaks and defaults (parsing side)

**Goal:** every page break in the model is one the document authored, and every default metric comes from the file. Closes E-4, the parsing half of E-3, F-21, plan-03 3.17, the O-02 pagination half. Each commit re-runs the E-0 dump so its effect is visible alone.

1. **E-4a · Fake breaks out.** `w:lastRenderedPageBreak` (Sample-2: 22, Sample-5: 3) and `text:soft-page-break` (Sample-1: 8, Sample-2: 10, Sample-4/5: 2) stop being page breaks; the `--- Page Break ---` text pollution goes with them (3.17). First commit, on its own, because nothing after it is readable while they are in.
2. **E-4b · Real breaks in.** `fo:break-before/after` on paragraph (auto) styles, `w:br type="page"` inside runs (Sample-2/4/5/6 start chapter headings that way), `w:pageBreakBefore`, and section starts (`w:sectPr` `type=nextPage` or default) become one model fact; `keep-with-next`/`w:keepNext` are recorded on the paragraph for 16b. The ODT/DOCX chapter-break difference (§2.3 item 4) normalises here.
3. **⚑ E-3a · Body rectangle = margins + header/footer heights** (audit-007 finding B), consuming the `PageStyleSpec` fields PR 15 added. Sample-4/5 ODT stop paginating against a 29.7 cm body; Sample-3 ODT keeps its declared 0 cm margins (honoured as declared, user decision 2026-09-26).
4. **⚑ E-3b · Metric-only style chain, both formats.** `docDefaults`/`default-style` → default paragraph style → `w:basedOn`/`style:parent-style-name` → paragraph style → direct `w:pPr`/paragraph properties, for **size, line height (`w:line`/`lineRule`, `fo:line-height`), before/after spacing, indents, keep/break flags only**. Populates the `ParagraphStyle` metric fields from PR 15 (E-EN-3). Run formatting, fonts, colours and the char-link rule stay in PR 20, which extends this reader. Sample-4 is the acceptance case for the order (docDefaults 1.5 lines, Normal `w:line=240`, body 278/360; audit-007 §3.3).
5. **F-21 · Document defaults replace the constants.** `StyleResolver`'s 14 sp default and its 24/20/16 sp heading fallbacks are replaced by the chain's result (Sample-1: 11 pt; Samples 2-6: 12 pt; TextMaker headings are body-size bold, audit-007 finding D), falling back to 12 pt only when a file declares nothing. Toolbar chip, paginator and renderer read the same value (audit-003 D5 closes with it). `Sample5StyleFidelityTest.mappedHeadingStyleBeatsHeuristic` is re-worded to assert the document default instead of the heuristic.
6. **Windows:** only **widened** where the dump shows the current `12..30` (Sample-5) is dishonest; nothing is tightened in this PR.
**Files:** `data/odf/SvXMLImport.kt`, `data/odf/SvXMLImportContext.kt`, `OfficeDocumentParser.kt` (DOCX branch + shared parse block), `OfficeDocument.kt` (`ParagraphStyle` population, break model), `LayoutEngine.kt` (`StyleResolver`, break handling, body rect), the dump test.
**Tests:** `BreakSemanticsTest` (per file: authored breaks in, fake breaks out, counts from audit-007 §5), `StyleChainMetricsTest` (Sample-4 order; Sample-1 11 pt; Sample-6 heading = 12 pt bold; Sample-3 heading 1 = 20 pt), `BodyRectTest` (Sample-4/5 ODT body top 2.54 cm; Sample-3 0 cm; DOCX 6.27 × 9.69 in), `SampleMatrixTest` and the dump re-run, `Sample5UnifiedPaginationTest` unchanged or widened.
**Acceptance:** the dump shows zero non-authored breaks in all twelve files; every paragraph's resolved size equals the audit-007 §3/§4 expectation for the sampled paragraphs; no window tightened.
**Size:** medium. **Risk:** medium (parser changes touch every file, but each is dump-visible and reverts alone).

#### 4.4b PR 16b, Plan 5C: metrics and windows (measurement side)

**Goal:** the page count stops lying. Closes E-2, the measurement half of E-3, E-5, E-6, the page-count half of finding 6, and the F-24/F-25/F-28 symptoms.

1. **E-2:** `layoutParagraph` measures through `TextMetrics` at `ptToUnits(fontSize)` with the family `FontRegistry` resolved; line height = `max(ascent+descent, fontSizeUnits * lineHeightFactor)`, which also deletes the renderer's `(sizeSp + 5f)` magic line-height constant in favour of style-driven line height (Sample-6 `Normal` = 116 %). The `2.5f` fudge and `fallbackTextSize` are deleted.
2. **E-3c:** `currentY += h + spaceAfter(prev) + spaceBefore(next)` from the fields 16a populated; `elementGapDp` dies.
3. **E-5:** widow/orphan floor of 2 where the style declares it (Sample-1/2 `Standard`, Sample-6 `Normal`).
4. **⚑ E-5b · Tab stops enough for line-count honesty:** default tab distance (`style:tab-stop-distance`, `w:defaultTabStop`/`w:tabs defTabSz`) and paragraph-level `w:tabs`/`style:tab-stops` (Sample-6: 167 `w:tab`, Sample-4: 35; the TOC entries wrap differently if tab widths are wrong). Leaders are painted by Plan 7C/8B, not here.
5. **E-6 · Windows, per format, all six pairs** (§0 as amended; audit-007 §11.3): DOCX `12..18`, `18..28`, `16..24`, `8..12`, `15..21`, `15..26`; ODT provisional `9..18`, `15..22`, `12..20`, `7..12`, `12..21`, `15..26`, re-derived from Collabora's page counts when the regenerated `.odt` fixtures land. `Sample5UnifiedPaginationTest` becomes the Sample-5 rows of `PaginationFidelityTest`. The ±10 % tightening is a later commit after 7/8.
**Files:** `LayoutEngine.kt`, `TextMetrics.kt` (now load-bearing), `LayoutDrivenDocumentRenderer.kt` (line heights), `OfficeDocument.kt` (tab stops on `ParagraphStyle`), the parsers for tab stops only.
**Tests:** `ParagraphMetricsTest` (Sample-6 `Normal`: 116 % line height, 0.282 cm after; Sample-1 `Standard`: 115 %, 0.111 in), `PaginationFidelityTest` (twelve rows), the dump re-run showing no page with fewer than three elements unless the document authors one, caret/selection/undo suites unchanged.
**Acceptance:** all twelve windows green; no blank page in any sample; one-page documents stay one page; an empty document stays one page.
**Size:** large. **Risk:** medium-high (upstream assertions move). Mitigations: windows not exact counts; `forceRebuildAll` spot checks; `TextMetrics` reverts alone; 16a's parsing is already in and green before any metric moves.

### 4.5 PR 22–25, Plan 6: image pipeline and load performance (COMPLETE 2026-09-30)

Closes F-07, F-18, the image half of save integrity (refusal per §0), O-01's minimal guard. The single "PR 17, Plan 6" row from v1 shipped as four increments, because F-2 (a new durable media store with caps and self-heal) and F-3/F-4 (decode presentation and real loading stages) each moved a different subsystem and could not be reviewed as one diff.

1. **F-1 (6A, PR #22 `eff150d`):** extents parsed in the live path for both formats (`wp:extent` EMU → `emuToUnits`, legacy `a:ext` fallback; `svg:width/height` through `LayoutUnits`); `OfficeImage`/`ImageElement` carry units; dead plumbing deleted (`DocxParseResult.imageExtents`, `InkyModule.docxExtents`). The dead `DocxDocumentParser.parseDocxFile`/`parseOdtFile`/`imageExtractor` and the `[Image: path]` branch were deleted in 6D (PR #25). CI: run `36578390234`, **252 unit tests**, 0 failures.
2. **F-2 (6B, PR #23 `ac713e4`):** media store in `filesDir/media/<sourceKey>/` with `manifest.properties`, 128 MiB per-document cap, 256 MiB global LRU cap, `ZipSafe.MAX_IMAGE_BYTES` per-image cap, collision-safe package paths, and self-heal re-extraction on a fresh parse or an in-memory parsed-document cache hit. CI: runs `36587725933`/`36588655662`, **260 unit tests**, 0 failures.
3. **F-3 (6C, PR #24 `bdd2724`):** decode without a blank frame. `DocumentImages` is the single resolver for box size, decode size (zoom-independent, capped at 2048 px) and cache key; `ImagePredecoder` decodes the first two pages' images on `Dispatchers.IO` during layout; `DocxEmbeddedImage` uses the explicit reserved box, `ContentScale.Fit` and `crossfade(false)`, with `DocumentImageTags.IMAGE/PENDING/MISSING`.
4. **F-4 (6C, PR #24):** the three `delay()` calls (`InkyModule.kt:1105-1109`) are gone; `loadingProgressStatus` is driven by `LoadingStage` (`OPENING_PACKAGE`, `VALIDATING`, `EXTRACTING_MEDIA`, `READING_STYLES`, `READING_BODY`, `CACHED`, `LAYOUT`). CI: run `36708650093`, **275 unit tests**, 0 failures; `Plan6cLoadingProgressTest` prints open/first-layout timings that PR #25's `scripts/ci-dump-comment.py` change now surfaces in the PR comment.
5. **F-5 (6A PR #22 + 6D PR #25):** the save path refuses instead of silently substituting `[Image: path]` (`EmbeddedImageSaveGuard`, `OdtDocumentWriter` package-media validation, `DocumentSerializer` propagation). 6D made the writer branch itself fail closed (`IllegalStateException`) so no future caller can resurrect the placeholder. CI: run `36708650093` and the PR #25 run, **276 unit tests**, 0 failures.
**Tests:** `Plan6ImageFoundationTest` (6 tests: the Sample-6 ODT/DOCX dp triple, the DrawingML `a:ext` fallback, DOCX/ODT save refusals, package-media reuse, and `tallerThanPageImageStillPaginatesAndUsesDocumentImagesFallback`, which is the `PaginationImageTest` row of this table), `DocumentMediaStoreTest` (8 tests: persistence, self-heal, LRU, source change, duplicate basenames, limits), `DocumentImagesTest` (6) and `DocumentImagePresentationTest` (4), `Plan6cLoadingProgressTest` (5, including `OpenLatencyTest`'s no-artificial-delay source scan).
**Acceptance:** text and images render from real stages with no artificial delay and no blank frame; a cache wipe cannot destroy media (self-heal re-extracts); both formats render the same picture the same size; all twelve fixtures stay inside their windows (ODT `15/23/18/10/18/19`, DOCX `15/25/21/11/19/23`, zero empty pages). **Device-class acceptance (the Realme C3 one-frame target) is not claimed here; it belongs to the owner's post-Plan-11 hardware pass.**

### 4.6 PR 18, Plan 7A: ODF numbering, heading runs, hyperlinks, ⚑ bookmarks

**Goal:** structure the samples actually contain. Closes G-1, G-3, G-4, ⚑G-4b (new), F-12, F-13, F-15, O-04, O-05 groundwork.

1. **G-1:** tokens + parser for `text:list-style` and its level styles from **both** `content.xml` and `styles.xml` (§2.4.1), including empty `style:num-format` (§2.4.2), `num-prefix/suffix`, `display-levels`, `start-value`, `bullet-char`, per-level text/paragraph properties, `text:continue-numbering` (35 occurrences in Sample-6, §2.1), and resolution via paragraph auto-styles (`P5` → `Makalah_20_Default`). Output through E-EN-4's `NumberingSpec`; the counter renders `BAB 1`, `2.1`, `a.`, `•`. The hard-coded `• `/`◦ ` dies. Sample-5's headings-as-paragraphs keep working (§2.4.6).
2. **G-3:** `XML_A` arm for paragraph/heading contexts (the token exists at `OdfXmlToken.kt:24`, no context consumes it); `OfficeTextRun` gains an optional link; Samples 2/4/5/6 stop losing link text (45 occurrences in Sample-6 alone).
3. **G-4:** `Heading` and `ListItem` carry runs; `toOfficeDocument()` stops writing synthetic/empty run lists.
4. **⚑ G-4b (new) · Bookmarks.** `XML_BOOKMARK`/`-START`/`-END` arms (tokens already exist, unconsumed) into a lightweight `OfficeBookmark` (name, anchor position) feeding the Navigator's bookmarks category (§1.3) and the `_TOCxxxxx` anchors the ODT TOC snapshots reference (Sample-6: 46 start/end pairs, all named). The DOCX arm (`w:bookmarkStart/End`, 46 in Sample-6) lands in PR 20 alongside the style-chain work, same file and same pass, so both formats expose anchors to the TOC snapshot rendering (19/21) and the Navigator stub (13) in the same PR wave.
**Files:** `data/odf/OdfXmlToken.kt`, `data/odf/SvXMLImport.kt`, `data/odf/SvXMLImportContext.kt`, `data/OfficeDocument.kt` (+`NumberingSpec`/`CounterState`), `UniversalNavigatorSheet.kt` (bookmarks + headings data feed).
**Tests:** `OdtListNumberingTest` (Sample-6: `BAB`-shaped labels with per-chapter restarts; `Subbab` levels 2–3 render no number; Sample-1/2/4/5 labels present and Sample-3 unchanged), `HyperlinkFidelityTest` (Samples 2/4/5/6 text counts), `HeadingRunsTest`, `OdtBookmarkTest` (Sample-6: 46 named anchors survive).
**Acceptance:** Sample-6 headings numbered, list labels at the level's own font size, no hyperlink text lost, bookmarks listed in the Navigator; page windows still hold.
**Size:** large.

### 4.7a PR #27, Plan 7B: canonical semantic model and ODT importer convergence

**Goal:** establish one semantic boundary and one authoritative ODT import path before adding new semantics. This is G-0 foundation work, not G-2/G-5/G-6/G-7 behavior.

1. Add format-neutral authored-index and named-section range sidecars, richer table value types, font-face metadata storage, and ODT source-feature provenance to the canonical and parsed models.
2. Preserve metadata, styles, bookmarks, runs, images, page breaks, sidecars, and table values in both adapter directions.
3. Replace `SvXMLImport`'s Android `Context` dependency with `OdfImportDiagnostics`, retaining an Android logger adapter and a pure implementation.
4. Add one `OdtImportPipeline` for bounded ZIP reading, styles/content/meta import, media mappings, exact package entries/XML parts, source-feature inventory, and failure state.
5. Route runtime ODT open and serializer/public compatibility entry points through that pipeline. Reduce `writer.OdtDocumentParser` to delegation and prevent a second XML parser from returning.
6. Add `OdtDocumentWriter.saveCapability`; unmodified imported packages preserve exact `content.xml`, while modified authored indexes, named sections, or source/advanced table structure are refused before bytes are written. Plan 9 still owns regeneration.

**Tests:** all six ODT fixtures produce the same normalized semantics through runtime and compatibility entry points; metadata/reference page counts and package entries survive; malformed package failure works without Android `Context`; sidecar/table adapters preserve values; unmodified content is byte-exact; unsupported modified save reports and throws; source assertion proves the facade contains no parser.

**Not in 7B:** no index/section XML producer, Navigator row, status behavior, table layout/rendering, or font alias resolution. Audit and implementation record: `audit-014-2026-10-03-plan-7b-convergence.md`.

### 4.7b PR #28, Plan 7C: authored indexes, named section ranges, Navigator, and status context

**Goal:** populate and consume the sidecars 7B introduced without creating nested duplicate body trees.

1. **G-2 authored snapshots.** Parse ODF table-of-content/index source, title, body, entry level/text/literal page label, tab/leader, and anchor into `DocumentIndexRange` over the ordinary body elements. Preserve roman labels and authored order; no regeneration or Update Index control.
2. **G-7 named sections.** Parse `text:section` name/style/condition/display/protection and nested range depth into `DocumentSectionRange`. Current fixtures have no `text:section`, so synthetic ODF 1.4 cases are mandatory; do not invent sections from DOCX twins.
3. Feed index and section categories to `DocumentIndexEngine`/Navigator with stable jump targets. Fill the existing status context slot from canonical range identity at the caret.
4. Keep index-body paragraphs in normal flow and prove each is represented once. No wrapper subtree may duplicate layout or editable text.

**Tests:** Samples 2/4/5/6 authored TOC entries and page labels; synthetic nested/named/protected sections; exact ranges and jump targets; status context; no duplicate body text; all page windows hold.

**Owner decisions (2026-10-04, audit-015 §7.1):** tab stops and leaders leave item 1 and move to 7E (§4.7d item 5). Hidden sections are listed greyed out and jump to the nearest visible position. TOC `#_TOC` links are grouped under their index, not under Hyperlinks. 7C adds a TOC entry go-to: tap and hold an entry to open FCT Compact, then tap "Go to entry…". The strings sweep becomes Plan 3D (`plan-3d-strings-sweep.md`). Implementation record: audit-015 §9.

### 4.7c PR #29, Plan 7D: table structure, measurement, pagination, rendering, and cell hit-testing

**Goal:** make one format-neutral table geometry load-bearing end to end.

1. **G-5 import.** Parse table columns and repeats, row/header-row/repeats, cell repeats, spans, covered cells/occupancy, widths, row heights, padding, borders, background, vertical alignment, style names, and table identity from ODF styles/content.
2. Resolve a rectangular logical grid with origin/covered occupancy. Reject impossible spans safely; do not duplicate covered-cell text.
3. Measure cell paragraphs with `TextMetrics`; derive intrinsic and declared column widths; compute row heights from the heaviest cell.
4. Paginate by rows with explicit over-height-row behavior and repeated semantic header rows. No `rows * 35 + 10` estimate remains.
5. Render from the same geometry and add cell-aware hit-testing/context so caret/status/table tools identify row and column accurately.

**Tests:** all source tables in Samples 1/2/3/6; synthetic repeats/spans/covered cells/header rows/borders; measurement-render parity; multi-page row pagination; cell hit tests; no regressions for zero-table fixtures.

**Implementation record (branch `arena/01a104f4-papirus-office`, 2026-10-04):** Plan 7D is being delivered as one four-commit change set. Commits 1 and 2 retain compact table declarations and resolve the rectangular occupancy grid; commit 3 adds shared measurement, row grouping, fragment geometry and row-aware pagination; commit 4 adds the geometry-driven Compose renderer, cell-aware hit testing, row/column cursor context and status detail. Synthetic and imported-table layout tests cover declared widths, intrinsic cell paragraphs, borders, merged cells, repeated headers, multi-page body coverage, cell hits and the ODT source-table set. The local sandbox has no JDK; GitHub Actions run `37177575040` passed both the Unit Tests and Build jobs.

**Scope guard:** table creation, insertion, editing, merge/split controls and native LibreOffice rendering remain out of Plan 7D. DOCX table parsing, font aliases and hidden-section pagination remain with their assigned plans. The compatibility renderer is retained only for externally supplied pre-geometry layouts; normal document layout uses `TableFragmentGeometry` from the paginator.

### 4.7d PR #31, Plan 7E: ODF font-face aliases and final pagination calibration

**Goal:** resolve the document's declared font identities through one metrics/display path, then measure the complete ODT structure.

**Status (v2.8, 2026-10-04).** PR #30 is the post-7D repair merge, so the slot 7E held in earlier revisions moves from #30 to **#31** and every later slot shifts by one. The first build of 7E was written in a sandbox whose four commits never reached a ref (audit-017 section 1); the change set was rebuilt from this section and is delivered in PR #31 as three commits: the declaration reader, the resolver plus wiring, and the calibration and records commit. Two owner decisions taken with the rebuild: items 5 and 6 are split out to Plan 7F (`plan-7f-2026-10-04-tab-stops-and-hidden-sections.md`), and the modified-save refusal tied to font-face declarations (decision D3 in audit-016) is deferred to Plan 9, which owns writer regeneration; the resulting loss is recorded in audit-017 section 12.

1. **G-6 declarations.** Parse `office:font-face-decls` from `content.xml` and `styles.xml`, including `style:name`, `svg:font-family`, generic family, pitch, and charset into `DocumentStyles.fontFaces`.
2. Resolve style alias -> declared family -> `FontRegistry` substitution. Metrics and renderer must receive the same final family; no renderer-only alias map.
3. Cover Times New Roman/Liberation Serif and the fixture corpus's generated aliases without confusing style alias names with actual family names.
4. Re-run the full six-ODT element dump/page matrix after 7C/7D are load-bearing. Explain every shift, tighten only with evidence, and record the final windows before 8A starts.
5. **Tab stops and leaders (moved from 7C on 2026-10-04; split out to Plan 7F on 2026-10-04).** Parse ODF `style:tab-stops` (position, type, `style:leader-style`, `style:leader-text`) in the paragraph style chain, lay out tabs against them in `TextMetrics`, and paint leaders. TOC entries in Samples 2/4/5/6 are the first consumers: entry text, dot leader, right-aligned page label. Positions and alignment already ship; `plan-7f-2026-10-04-tab-stops-and-hidden-sections.md` owns the leader field and the painting.
6. **Hidden-section rendering (added 2026-10-04; split out to Plan 7F on 2026-10-04).** Plan 7C lists `text:display="none"` sections and keeps jumps out of them, but the paginator still lays out their content. Exclude hidden ranges from layout. No fixture contains one, so the item cannot move the matrix and its tests are synthetic.

**Tests:** declaration precedence and aliases from both XML parts; metric/display parity; all six ODT font inventories; final pagination matrix and zero unexplained empty pages. The tab-stop, leader and hidden-section tests named in earlier revisions belong to Plan 7F and are listed there.

**Implementation record (branch `arena/01a1077a-papirus-office`, 2026-10-04).** Three commits on top of `9356212`, after the lost build was rebuilt from source:

| Commit | Content | Tests added |
|---|---|---|
| `784e9d3` | `office:font-face-decls` read from `styles.xml` and `content.xml` into `DocumentStyles.fontFaces`, keyed by `style:name`, raw `svg:font-family` kept for the resolver; first declaration of an alias wins; blank name or blank family skipped | `Plan7eFontFaceImportTest` (4) |
| `c363ce3` | `FontFaceResolver` plus one `resolveFontFamily` helper in both text-properties readers, so the resolved family is the single input to `TextMetrics.forStyle` and `OfficeRuns.fontFamilyFor` | `Plan7eFontResolutionTest` (8) |
| records | this section, `plan-01` row 7, sections 4.7d/4.8 to 4.12 here, plan-04's Plan 7 record, `plan-5e-progress.md`, `PROJECT_CONTEXT.md`, `audit-017` and the new 7F document | none |

CI run `37213135047` (fresh PR merge ref `3914079`; it measured the pre-fix head `28a37fb`, and commit 2 in the table is `c363ce3` after the expectation fix) compiled the new code on the first try: **342 tests across 63 suites, 1 failure**, and the Build job green. The failure was the resolution test's own expectation for `FontChoice.requested`, which after resolution is the declared family rather than the alias; the expectation was corrected to assert both values (resolved path gives `Times New Roman`, the raw path keeps `Times New Roman1`). Every other 7E assertion passed, including the fixture inventories and all six alias decisions.

**Calibration, measured.** The page matrix in that run is identical to the PR #30 baseline in every cell: ODT `14/23/21/10/18/20` and DOCX `15/25/25/11/19/24`, with the same thin-page and empty-page columns. No window moved and none widened. The one observable change is the resolution itself, visible in the same dump: Sample-6.odt's body style reads `12.0 pt (Aptos) line factor 1.00` where the baseline read `(Aptos1)`, and the string `Aptos1` occurs **0** times in the new dump against **1** time in the PR #30 dump. `Aptos` resolves to the Martel Sans stand-in (`BUNDLED_STAND_IN`, `metricCompatible = false`), and the identical page count confirms the earlier static prediction that the two advance tables do not cross a page boundary in this file. Decision D3 (save refusal) is deferred to Plan 9 by the owner; until then a regenerated `content.xml` would drop the declaration table, and that loss is recorded rather than blocked. `OdtDocumentWriter.saveCapability` and every writer path are untouched by this PR.

### 4.8 PR #32, Plan 8A: DOCX style chain + run formatting

Closes H-1, H-2, F-16, F-20, the DOCX half of F-10.

1. **H-1:** parse `w:docDefaults` (`rPrDefault`/`pPrDefault`) and every `w:style` with its `basedOn` chain and full `w:rPr`/`w:pPr`; `pStyle` keeps the DOCX style id. `para1` resolves to 20 pt Aptos Display `#0f4761` via the chain (§2.3 item 2); body resolves to docDefaults' 12 pt, not the 24/20/16 fallback or 14 sp.
2. **⚑ H-1b (new) · Character-style linkage.** Resolution precedence: explicit `w:link` wins (Sample-4: 27 of them); then Word's **naming convention** (paragraph style `X` ↔ character style `X Char`, case-insensitive, trailing-space tolerant); if neither matches, the paragraph style's own `w:rPr` applies unchanged; Sample-6 has no `w:link` attribute at all (§2.3 item 2), so convention-only files must resolve. The resolved character properties (font, size, colour, bold) merge into the paragraph's default run properties; a direct run `w:rPr` always overrides both.
3. **⚑ H-1c (new) · Numbering state per paragraph.** While numbering *rendering* is Plan 8B, the style-chain work must record per paragraph one of: **inherited** (`numId`/`ilvl` from the style chain, walked through `w:basedOn` (Sample-6 headings: `numId 15` inside `w:style/w:pPr`), **explicit** (a direct `w:numPr`, 42 paragraphs), or **suppressed** (direct `w:numId 0`, ECMA-376 §17.9.18, 3 paragraphs). Plan 8A resolves; Plan 8B renders. This is why H-1 must read `w:numPr` inside styles at all.
4. **H-2:** `TextRun`s built per `w:r` from resolved character properties; `w:val="0"|"false"` is an explicit negative (negative-flag support in `OfficeRuns.mergeRun`); run flags stop leaking past their run (reset at run end, not at `</w:p>`); **`currentRuns` actually reaches the paragraph** (today it is declared at `OfficeDocumentParser.kt:800` and never populated, so every DOCX paragraph is one flat run).
5. **⚑ H-2b (new) · Retire the sample-tuned regexes.** `PARA_STYLE_REGEX` (`para[1-9]`), `HEADING_STYLE_REGEX` (`heading[1-9]`) and the `SINGLE_DIGIT` heuristic (`OfficeDocumentParser.kt:55-57` and the `w:pStyle` arm) are replaced by the real style-table lookup (H-1); a styleId absent from the file's own style table resolves to the Normal-based default (not a heading). The regexes were tuned to the samples' styleId shape; the lookup is what the spec says.
**Files:** `OfficeDocumentParser.kt` (new `word/styles.xml` reader + run path), `data/OfficeDocument.kt` (run + numbering-state fields), `data/DocxDocumentParser.kt` (nothing, since the save path is Plan 9).
**Tests:** `DocxStyleChainTest` (Sample-6: `para1` → 20 pt Aptos Display `#0f4761`; `para2` → 16 pt; `para3` → 14 pt with inherited font; docDefaults → 12 pt; Sample-3 as control), `DocxRunFormattingTest` (no leak; explicit unbold via `w:val="0"`), `DocxHeadingNumberingStateTest` (6/11/28 distribution; 42 explicit + 3 suppressed flagged), `DocxCharLinkTest` (Sample-4 resolves via `w:link`; Sample-6 resolves via the name convention), Sample-3.docx unchanged.
**Acceptance:** heading/body sizes and fonts come from the file in every sample; no run-flag leak; Sample-3 control green.
**Size:** large.

### 4.9 PR #33, Plan 8B: DOCX numbering, fields, tables, sections, and TOC snapshot

Closes H-3…H-7, ⚑H-3b/⚑H-4b/⚑H-6b (new), F-13 DOCX parity, F-17, F-19, F-26, F-27, F-28 tail, O-03, O-02 remaining.

1. **H-3:** `word/numbering.xml` reader (`abstractNum`/`num`/`lvl` with `start`, `numFmt`, `lvlText`, `lvlJc`, `suff`, `ind`, `isLgl` (45 levels in Sample-4), `lvlOverride`/`startOverride`) resolved per paragraph into `NumberingSpec`, honouring H-1c's precedence: direct `w:numPr` (including `numId=0`) → `pStyle` chain walked through `w:basedOn` → none: Sample-6's 42 headings get `BAB N` from `abstractNum 15` (`BAB %1`, `%1.%2`, …), and its 3 `numId=0` headings (KATA PENGANTAR, DAFTAR ISI, +1) render **without** a prefix. Five of six samples exercise the reader (§2.2).
2. **H-4:** the field model: `w:fldSimple` **and** `w:fldChar` complex fields (begin/separate/end) with `TOC`, `PAGEREF`, `SEQ` handling (§2.4.3); instructions never reach body text (`SEQ` count in parsed Sample-1 = 0).
3. **⚑ H-4b (new) · TOC snapshot, DOCX half.** The authored TOC renders as the ODT half does (G-2): entries styled `toc 1/2/3`, each entry's **paragraph-level** `w:tabs` right-aligned dot leader (Sample-6: `pos 9027`, `leader="dot"`; §2.3 item 5), literal page numbers, and `w:hyperlink w:anchor="_TOCxxxxx"` preserved as anchors (bookmarks from Plan 8A's DOCX arm). The ` TOC \o "1 - 9" \z ` instruction never appears in output. Entry detection is by **toc-styled paragraph, regardless of whether it sits inside a field-result run** (the samples author the snapshot as plain paragraphs). The test asserts the verified sizes (Sample-2: 15, Sample-4: 21, Sample-5: 14, Sample-6: 45; §2.2), handles the `&quot;`-escaped field instruction (Sample-4), and does not list the unused `toc 3…9`/`TOC Heading` *style definitions* (Sample-4) as entries.
4. **H-5:** `w:tblGrid`/`w:tblW`/`w:tcW` → column weights; `gridSpan`/`vMerge` parsed into the span fields (none in the samples, but the model exists); `tcPr`/`trPr` (`tblHeader` repeat); shared geometry with G-5.
5. **H-6:** per-paragraph governing `w:sectPr` (`w:pPr/w:sectPr` starts a section; body-level closes the last); Sample-6's five sections paginate with their own geometry.
6. **⚑ H-6b (new) · Section page numbering.** The section model carries `w:pgNumType` (fmt/start) and `w:titlePg` (§2.3 item 6): front matter lowerRoman (ii/iii), body decimal restarting at 1. This is the data behind the status bar's page-number-vs-sequence-number field (WG Ch.1) and the Navigator's "Page" category sequence numbers. `w:headerReference`/`w:footerReference` are recorded in the model but **not rendered**: page furniture has no plan item, and Plan 8B must not grow it.
7. **H-7:** `w:lastRenderedPageBreak` excluded from pagination and editable text (Sample-2's 22 occurrences are the regression case).
**Files:** `OfficeDocumentParser.kt` (numbering/field/section readers + run path completion), `data/OfficeDocument.kt`, `data/DocxDocumentParser.kt` (TOC rendering only; save is Plan 9), `LayoutEngine.kt` (section-aware pagination).
**Tests:** `DocxNumberingTest` (Sample-6: `BAB 1…3` on the 42 numbered headings, none on the 3 suppressed; Sample-4 legal-numbered list; Sample-3: zero labels), `DocxFieldTest` (no `SEQ`/`TOC \`/`PAGEREF` instruction text in parsed output for Samples 1/2/6), `DocxTocTest` (entries, dot leaders, literal pages, `_TOC` anchors), `DocxTableGeometryTest`, `DocxSectionGeometryTest` (5 sections → 5 geometry boxes; pgNumType recorded), and **the convergence test: Sample-6 ODT and DOCX paginate into the same page windows** (§2.4.7).
**Acceptance:** Sample-6.docx fidelity checklist; Sample-6's two formats (the one metric-identical pair, audit-007 §1) converge on the same page window, the other pairs hold their per-format windows; **the §0 staged-tightening commit lands here** (windows move toward ±10 % of the audit-007 §1 references, never below).
**Size:** large.

### 4.10 PR #34, Plan 9: save round-trip integrity (pre-change gate first)

Closes O-01, the non-destructive-package rule, plan-03 3.13, 3.19, 3.20, 3.26, 3.27, and retires F-2 (`OfficeDocElement`).

Scheduled **after** Plans 6, 7E and 8B land (**forecast PR slot #34** after the 2026-10-04 shift), with its own pre-change gate (plan-04-to-09 § Plan 9 text stays the seed): a real ODT writer (styles, list styles, TOC, manifest entries per ODF Part 2, `style:font-face`, `office:version` 1.4; today `generateOdtXml` at `DocxDocumentParser.kt:748+` writes a bare `office:document-content` with `office:version="1.2"` and no styles at all), a real DOCX writer (`styles.xml` consistent with the regenerated `document.xml`, heading/char pairs per §2.3, `numbering.xml` references that exist, `w:tblGrid`, images in the package with rels), the original-package-bytes fallback removed once round trip is proven, and the `OfficeDocElement` wrapper deleted in a mechanical final commit.
**Acceptance:** open → save → reopen preserves text, styles, numbering, tables, images, TOC for Sample-6 in both formats, verified by a CI round-trip test. **Scope is firmed by a short plan document before this PR starts; do not start it from this paragraph alone.**

### 4.11 Plan 10: stays parked

Thread A (real `Typeface` loading, A2 policy write-down, A3 Font Style UI, A4 SAF/user fonts, A5 metrics-parity test) and B2 to B6 resume **after Plan 8B (forecast #33) converges both formats and the user re-confirms** (forecast slots #35-#36 after the 2026-10-04 shift) (the §0 display decision is already binding on `FontRegistry` from PR 15, so A1 is an upgrade of the loader, not a redesign). B1 (the `DESIGN.md`/m3.material.io review) lands early as documentation in PR 14.

### 4.12 Plan 1: master index updates (living document, per PR)

Per plan-01 §6's update rule, every PR's **final commit** contains the plan-01 registry change and the matching line in the plan's own file.

**Landed 2026-09-24 to 2026-09-28.** The PR column of the original table below predicted 17 to 22 for Plans 6 to 9. Those numbers are void: 5b and 5c merged as one PR, then 5d and 5e each took a number, so **PR #17 is 5c, PR #18 is 5d and PR #19 is 5e**. What actually landed:

| PR | Merged | plan-01 registry change (§2) | plan file line |
|---|---|---|---|
| 13 | 2026-09-24 | row 3 → "3A landed (PR 12); 3B landed (PR 13)" | plan-03 §8 got a 3B record (3.5 to 3.11, 3.28, 3.32/3.33 as shipped) |
| 14 | 2026-09-25 | row 3 → "3C landed (PR 14)"; row 11 → documentation alignment landed | plan-11 keeps the alignment record |
| 15 | 2026-09-26 | row 5 → "5A landed (PR 15)" | plan-04-to-09 § Plan 5 got the 5A record (E-0, E-7, centralisation) |
| 16 | 2026-09-27 | row 5 → "5A, 5B landed" | § Plan 5 got the 5B fixture re-baseline record (audit-008) |
| 17 | 2026-09-27 | row 5 → "5A, 5B, 5C landed" | § Plan 5 got the 5C documentation refresh record (audit-009, DESIGN.md v3.0) |
| 18 | 2026-09-27 | row 5 → "5D landed" | § Plan 5 got the 5D breaks and defaults record (audit-010) |
| 19 | 2026-09-28 | row 5 → "5A to 5E landed" | § Plan 5 got the 5E record; `plan-5e-progress.md` carries the batch-by-batch evidence |

**Landed 2026-10-02 to 2026-10-04** (Plan 1's two documentation PRs #20/#21 and the four Plan 6 increments #22 to #25 are recorded in the forward-schedule paragraph above):

| PR | Merged | plan-01 registry change (§2) | plan file line |
|---|---|---|---|
| 26 | 2026-10-02 | row 7 → "7A landed" | Plan 7 got the 7A record (G-1/G-3/G-4/G-4b); Plan 12A got the LOKit seam record (`audit-013`) |
| 27 | 2026-10-03 | row 7 → "7B landed" | Plan 7 G-0 implementation record; `audit-014` |
| 28 | 2026-10-04 | row 7 → "7C landed" | Plan 7 G-2/G-7 record; `audit-015` section 9 |
| 29 | 2026-10-04 | row 7 → "7D landed" | Plan 7 G-5 implementation record |
| 30 | 2026-10-04 | row 7 → "7D regression repaired on `main`" | `audit-016` (post-7D unit-test analysis) and `audit-017` (recovery state, forecast shift) |

**Forward schedule, updated 2026-09-30 (Plan 6 complete).** Plan 1's documentation PRs took #20 and #21. Plan 6 then took four PRs instead of the one slot v1 predicted: **#22 (6A, image extents and fail-safe image saves, run `36578390234`, 252 unit tests), #23 (6B, durable media storage and recovery, runs `36587725933`/`36588655662`, 260 unit tests), #24 (6C, decode presentation and real loading progress, run `36708650093`, 275 unit tests), and #25 (6D, closeout gate: dead `DocxDocumentParser` plumbing deleted, `LayoutEngine` unified on `DocumentImages.box`, `PaginationImageTest` added, per-suite JUnit timings and `Plan6cLoadingProgressTest` stdout added to the CI PR comment, 276 unit tests across 48 suites in run `36730234004`)**. Local Java remains unavailable in the sandbox, so every number below comes from a GitHub Actions run. Plan IDs remain authoritative; the PR slots below are the reforecast promised when Plan 6 was split, and they are expectations, not reservations.

| Plan | PR slot | plan-01 registry change | plan file line |
|---|---|---|---|
| 6 | **#22/#23/#24/#25 all merged — COMPLETE** | row 6 → "6a to 6d all landed (COMPLETE)" | plan-04-to-09 § Plan 6 carries the four-increment implementation record, the CI run table and the re-confirmed 12-file matrix |

**2026-10-02 insertion.** Plan 12A and Plan 7A landed together in PR #26: the LOKit JNI/process seam and ODF numbering, heading/list runs, hyperlinks, and bookmarks. Evidence: `audit-013-2026-10-02-plan-12a-and-7a.md`.

**2026-10-03 Plan 7 split.** The old combined 7B row is void. Exactly one PR belongs to each of 7B, 7C, 7D, and 7E. The current forecast is:

| Plan | PR slot | plan-01 registry change | plan file line |
|---|---|---|---|
| 7A | **#26 landed** | row 7 -> "7A landed" | Plan 7 records G-1/G-3/G-4/G-4b |
| 7B | **#27 landed** | row 7 -> "7B importer/model convergence" | Plan 7 G-0 implementation record + `audit-014` |
| 7C | **#28 landed** | row 7 -> "7C indexes/sections landed" | Plan 7 records G-2/G-7 and navigation/status |
| 7D | **#29 landed** | row 7 -> "7D tables landed" | Plan 7 records G-5 end to end |
| repair | **#30 landed** | row 7 gets the regression and repair note | `audit-016` post-7D analysis, `audit-017` recovery state |
| 7E | **#31 (this PR)** | row 7 -> "Plan 7 complete" | Plan 7 records G-6 aliases and the final ODT matrix |
| 8A | **#32** | row 8 -> "8A landed" | Plan 8 gets H-1/H-2 |
| 8B | **#33** | row 8 -> "8A, 8B landed" | Plan 8 gets H-3 to H-7 and convergence evidence |
| 9 | **#34** | row 9 -> landed | Plan 9 structural round-trip record |
| 10 resume | **#35-#36** | row 10 status change when it starts | plan-10 head note |
| 11 packages | **#37-#41** | row 11 status change per package | plan-11 §5 |

Plan IDs remain authoritative and slots are forecasts. Plan 12B/12C stay after Plan 9.

One-time plan-1 changes made with PR 13's commits (they described state then): registry row 3 status (3A **landed**, not "in review"); §3 WG-mapping gained three owner lines; §3.1's sidebar-deck row lost its "lacks Images" half; §3.1's menu-bar row's "6 of 8 ribbon tabs are empty" became "4 of 6 declared tabs have no deck yet"; §6's "Next actions" item 2 was struck through as done.

**2026-09-28 synchronization (this PR).** Beyond the re-numbering above: the plan-01 chapter map moved from WG 24.8 to the pinned 26.2 edition; the Plan 5 status line was replaced; the document-views and Go to Page rows were corrected for the retired flow view and the new page counts; §4 rows 2, 3, 4, 6, 9, 11 and 12 were updated; and §5 invariant 1 now names the retired second renderer and the injected measurement backend. Drift inventory: `anti-slop/audit-012-2026-09-28-plan-1-synchronization.md`.

---

## 5. Sequencing, parallelism, and the device checklist

**Landed 2026-09-24 to 2026-09-28:** 13 (3B), 14 (Plan 3C / Plan 11 docs), 15 (5A), 16 (5B), 17 (5C), 18 (5D), 19 (5E). Plan 5 shipped as five PRs rather than the 16a/16b split predicted below.

**Forward schedule, amended 2026-10-03 for the four-plan ODF split** (plan IDs first; PR slots are forecasts):

| Order | Plan | PR slot | Parallel with | Gate to enter the next |
|---|---|---|---|---|
| - | 6 (images and media) | **#22-#25, merged** | - | **done:** media self-heals; unsafe image save refuses |
| - | 7A (numbering, runs, links, bookmarks) | **#26, merged** | - | **done:** ODT numbering fidelity |
| 1 | 7B (canonical model/import convergence) | **#27** | research only | runtime/facade parity; save capability closed |
| 2 | 7C (indexes/sections/navigation/status) | **#28** | research only | sidecars populated and navigable |
| 3 | 7D (tables end to end) | **#29** | research only | shared geometry through cell hit-testing |
| 4 | 7E (font aliases/calibration) | **#31** | research only | metrics/display parity; final ODT matrix |
| 5 | 8A (DOCX style chain/runs) | **#32** | none | DOCX style chain green |
| 6 | 8B (DOCX numbering/fields/tables/sections) | **#33** | none | both-format convergence |
| 7 | 9 (save round trip) | **#34** | Plan 10 resume decision | structural round-trip CI matrix |
| 8 | 10 and 11 packages | **#35-#41** | per package | per-package gate in plan-11 §5 |
| 9 | Owner device pass | none | after Plan 11 | all twelve `InkyC1Checklist` sections on hardware |

**Device checklist resume points** (`docs/InkyC1Checklist.md`, deliberately postponed; section order = item number):

1. **Now (before PR 15):** install the latest nightly (≥ PR 12), run plan-02 §5 acceptance and checklist items 5 (Selection), 8 (Reminder), 9 (Zoom), plus the `[needs run]` device log for the Viewer FCT platform-menu question.
2. **After Plan 5 (merged):** items 2 (editing stages), 4 (Caret), 6 (Go To with believable counts), 9 re-run, 11 (Session Restore: page 15 at 170 %, migrated zoom semantics). All of these are code-complete and await hardware.
3. **After Plans 7C, 7D, and 8B:** item 7 (Navigator categories, including parser-backed indexes, bookmarks, tables, hyperlinks, and sections) and item 12's structural stress portion. **After Plan 9:** item 10 (Save Compatibility). Plan 6 media-memory coverage is already landed.
4. **After Plan 11:** the full twelve-section pass, recorded with APK, commit, device and Android version. This is the gate before Chapter 2 work starts. The owner deferred it to this point on 2026-09-28.

---

## 6. Cross-plan invariants (unchanged from plan-01 §5)

One paginator (`LayoutEngine`), one measurement backend (`TextMetrics`), one unit system (`LayoutUnits`), one style resolver (`StyleResolver` plus `OfficeRuns`), one numbering model, one media store. A plan that adds a second path deletes the first in the same PR; Plan 5e did exactly that when it retired the second `BasicTextField` renderer and replaced raw Paint sizing. No new hard-coded copy or `contentDescription` literal (the PR 12 guard enforces this mechanically). 48 dp targets, token colours only, verified at 320 dp. No em dash in user-visible strings. Evidence, not claims: every PR body lists the findings it closes, the suite it ran, and, for UI, the Delivery Gate with device evidence. The 12-file sample matrix (§2) is the floor.

---

## 7. Deltas against v1 (what changed and why)

1. **Baseline moved** `e10f956` → `55a9a97` (PR 12 landed); §1.3 re-verified at the new baseline; the PR 12 section is a pointer, not a task.
2. **Sample-2 TOC corrected:** v1's "6 TOC instances" was a prefix-grep artifact; the true count is 1 (15 entries/15 links). The heaviest TOC stress case is **Sample-6** (45/45), not Sample-2. G-2's test list now orders Samples 6/4/2/5.
3. **Sample matrix re-derived with explicit tag boundaries (§2).** Every figure in v1's two tables was re-counted; the ODT list column is now "lists / items" (70/84, 41/103, 0/0, 15/18, 123/186, 115/180) because v1's single "lists" number was not reproducible from one stated rule, and the ODT bookmark column is split into start/end pairs (Sample-6: 46/46, the ODT half of the DOCX `w:bookmarkStart` count, not "92 bookmarks").
4. **⚑ Sample-6 DOCX heading architecture added (§2.3)**: v1 said the heading runs "lack rFonts/sz" and left the `w:link` question open. Verified reality: the *styles* carry the numbering (`numId 15`, `BAB %1`), the *char styles* link by naming convention (no `w:link` in the file, 27 in Sample-4), 42 headings carry explicit `numPr` and 3 carry `numId=0` suppression, page breaks sit inside heading paragraphs, and the TOC is an authored snapshot of `toc 1/2/3`-styled hyperlink paragraphs with **paragraph-level** dot-leader tabs. This grows PR 20 (H-1b, H-1c, H-2b) and PR 21 (⚑H-3 style-inherited numbering, ⚑H-4b TOC snapshot, ⚑H-6b pgNumType).
5. **⚑ Navigator honesty item (3.32)**: 13 categories, one generic empty message, and no Hyperlinks category were not in v1's PR 13. The classification is evidence-based: the "not yet readable" set is exactly the element classes **no main-source file constructs** (§4.1 item 6), and the PR ships a test that re-derives that split from the source tree.
6. **⚑ Status-bar object information (3.33)**: WG Ch.1's "section or object information" field (Table 1) had no owner; the honest-minimal version lands in 13 through the caret seam the toolbar hub already uses, and the full version follows 19/21.
7. **⚑ Bookmarks (G-4b in 18, DOCX arm in 20)**: tokens exist but are unconsumed; the Navigator bookmarks category and the `_TOC` anchors need them.
8. **⚑ Sections (G-7 in 19, H-6b in 21)**: ODT `text:section` identity had no plan item; DOCX `pgNumType`/`titlePg` is now explicit (WG status-bar page-number-vs-sequence-number).
9. **⚑ E-7 centralisation (15)**: the status bar's duplicated fit-scale computation now consumes the renderer's single transform; PR 13 deliberately does not touch that region.
10. **⚑ E-2 line-height constant (16b)**: the renderer's `(sizeSp + 5f)` magic line height dies with the fudge, in favour of style-driven line height.
11. **Guard baseline corrected (§1.2)**: 47 textual occurrences in 14 files, **46 in code across 13** once comments are masked; the `LayoutDrivenDocumentRenderer` allowance is stale. PR 13's scope states that every map entry is deleted.
12. **`w:br` handling corrected in the record**: the shared parse block *does* distinguish `type="page"` from soft breaks (`OfficeDocumentParser.kt:961`); v1's "still open" row implied otherwise. The remaining issue is `w:lastRenderedPageBreak` (ODT soft-page-break pollution), unchanged.
13. **`w:tabs` attribution corrected**: the first draft of this file placed the TOC dot-leader tab in the `toc 1/2/3` styles; it is a paragraph-level property in `document.xml` (§2.3 item 5, §4.9 item 3).
14. **Pagella 3.9 corrected**: the zoom pair there already has 48 dp targets through Material 3's minimum; only Cellina and Slidia override it with `Modifier.size(24.dp)` (§4.1 item 4).
15. **⚑ items re-numbered and tightened (v2.2, 2026-09-24):** the Navigator and status-bar items are now **3.32/3.33**, because 3.14/3.15 in the first draft collided with plan-03's existing ODF-conformance rows (list styles / TOC index dropped, owned by plans 7 and 9). **⚑3.32** distinguishes *verified-absent* from *not-yet-readable* and moves the justification from "the index engine populates only some categories" (it has arms for all of them, exercised by `NavigationEngineTest`) to the verified fact that **no parser constructs the element classes**; **⚑3.33** fixes the data sources (caret→element seam; heading level+text from the Navigator index; table and list-item kinds; hyphen fallback, no invented data); **⚑G-4b** pins the DOCX `w:bookmarkStart/End` arm to PR 20 so both formats expose anchors in the same wave as the TOC snapshot rendering; **⚑G-7** leaves the section model shape (wrapper vs attribute) to implementation, fixed on the exposed name; **⚑H-1b** adds the precedence (explicit `w:link` → naming convention → style's own `rPr`; a direct run always overrides); **⚑H-1c** states the three-state flag and that the `pStyle` chain is walked through `w:basedOn`; **⚑H-2b** fixes the fallback for styleIds missing from the file's own style table (Normal-based, not a heading); **⚑H-3** states the resolution precedence (direct `numPr` incl. `numId=0` → style chain → none); **⚑H-4b** fixes entry detection to "toc-styled paragraph, inside or outside a field-result run"; **⚑H-6b** records `headerReference`/`footerReference` in the model without rendering them (no plan item owns page furniture).
16. **⚑3.32 implementation additions (recorded in PR 13, v2.3):** the Indexes filter (`NavigateBy.INDEX`) was the same class of lie as the twelve categories in the All view: it fell through to the generic "There are no objects to navigate" row although a TOC/index is authored document content that plans 19/21 read, so `indexes` joined the not-yet-readable set in `NavigatorCategories` (key `indexes`, element classes hand-checked like `frames`/`ole`, owner `plan-19/21`). The "Pages" and "Reminders" Navigate-By labels became plural so the verified-absent sentence ("No %1$s in this document.") reads correctly for them; `values-in` is untouched. The Hyperlinks category carries no rows and no `Navigate-By` filter option in this PR: `NavigationEngine` has no hyperlink jump yet, so the rows and their jump land together with the parser in 18/20.
17. **⚑ v2.4 (2026-09-26, audit-007):** (a) **PR 16 split** into 16a (Plan 5B, parsing: fake breaks out, real breaks and section starts in, body rect from margins + header/footer heights, metric-only style chain, F-21 defaults) and 16b (Plan 5C, measurement: `TextMetrics` load-bearing, constants deleted, widows/orphans, tab stops, windows); 17 still depends on 15, 18 now depends on 16b. (b) **Windows are per format** and cover all six pairs; the "Sample-5 `15..21` both formats" wording is withdrawn because the OnlyOffice ODT exports of Sample-3/4/5 lost Word's `pPrDefault` (single-spaced, 0 after) and Sample-3.odt declares 0 cm top/bottom margins; geometry is honoured as declared (user decision), and the ODT column is provisional until the user regenerates the `.odt` fixtures with Collabora Office. (c) **DOCX references completed** from M365 Copilot: Sample-1 15, Sample-3 20, Sample-4 10 (Sample-2 23 and Sample-5 18 from `app.xml`, Sample-6 21 as before). (d) **PR 15 gains** the dump over all six pairs, `SampleMatrixTest`, header/footer heights in `PageStyleSpec`, and the §8 font corpus. (e) **Corrections to the record:** §2's counts are confirmed; Sample-4/5 ODT's "margin-top 0" (§2.1's Sample-5 note) is a fixed-height header carrying the margin (equivalent to the DOCX), not a defect, while Sample-3.odt's 0 cm margins are; the "double break makes a blank page" theory (plan-02 item 7) is refuted by `flushPage()`'s guard, so E-0 is confirmation, not diagnosis. (f) **Native libraries:** the checked-in `.so` files are LFS pointers to a 196 MB (arm64) / 135 MB (v7a) `liblo-native-code.so` plus the NSS/NSPR set; the "~60 KB stub" wording in earlier audits is withdrawn (audit-007 §10).

18. **v2.6 (2026-10-03):** the old combined 7B package and its downstream PR forecast are superseded by 7B canonical/import convergence, 7C indexes/sections/navigation/status, 7D tables end to end, and 7E font declarations/aliases/calibration. The ODT matrix was re-read from the current ZIPs: Sample-6 has no `text:section`, list-style definitions are in `styles.xml`, and current list/heading/soft-break counts replace the pre-regeneration rows. `audit-014` records the implementation boundary.
19. **v2.7 (2026-10-04, audit-015):** Plan 7C is implemented as one PR in four commits. Tab stops and leaders move from 7C to 7E (§4.7d item 5), and hidden-section rendering is added to 7E (§4.7d item 6). The strings sweep is Plan 3D in its own file. No forecast PR slot moves, because 3D is scheduled when the owner picks a slot.

20. **v2.8 (2026-10-04, audit-016 and audit-017):** the post-7D refactor regression (`cb89460`) and its repair are recorded; PR #30 is the repair merge `9356212`, so the forecast from 7E onward shifts by one: 7E `#31`, 8A `#32`, 8B `#33`, Plan 9 `#34`, Plan 10 `#35`-`#36`, Plan 11 `#37`-`#41`. Plan 7E is delivered as PR #31; its first two commits compiled in CI run `37213135047` (342 tests, one corrected expectation, Build green) and the calibration is measured: the twelve-file page matrix is unchanged from PR #30, with Sample-6.odt's dump line moving from `(Aptos1)` to `(Aptos)` and no `Aptos1` left in the dump. Items 5 and 6 leave 7E for the new Plan 7F (`plan-7f-2026-10-04-tab-stops-and-hidden-sections.md`), and decision D3 (save refusal for declared font faces) is deferred to Plan 9. Every earlier line that names a 7E slot of `#30` is superseded on that point only.

---

## 8. What still needs the user (nothing blocks)

1. The on-device passes at the three resume points in §5; the first one gates only checklist credit, not PR 13/15.
2. A re-check of post-fix screenshots after PR 13 (the R-26 surfaces change).
3. The Plan 9 pre-change gate when Plan 8B nears completion (its scope paragraph is a seed, not a commitment).
4. The staged-window decision is recorded (§0): Plan 7E remeasures ODT after structure, and Plan 8B owns cross-format convergence; no action is needed now.
5. New ownership is fixed: G-2/G-7 in 7C, G-5 in 7D, G-6 in 7E, and H-1/H-2 in 8A with H-3 to H-7 in 8B. Removing an item requires amending its owning plan rather than silently moving it back into 7B.
6. **⚑ From audit-007 §12 (2026-09-26):** the Aptos stand-in for `FontRegistry` (Carlito, Liberation Sans, or system default; cosmetic for pagination); whether to add a read-only native-library inventory step to the CI `test` job; whether Sample-1's ODT keeps the bridging window `9..18` or has no ODT assertion until the Collabora count exists; and the Collabora regeneration itself (drop the six `.odt` files in place, record the six status-bar page counts, update `SampleMatrixTest` and the ODT window column in one commit).

---

## Appendix A: Verification evidence (this session, at `55a9a97`)

*2026-09-26:* the full twelve-file re-measurement (geometry, defaults, headings, breaks, structure counts, producer quirks, fonts, native-library sizes, and the per-column regexes) lives in `audit-007-2026-09-26-sample-matrix.md`; the evidence below is kept as the PR 12/13-era record.

**Sample-6 DOCX (verbatim, abridged):**

- Heading style: `<w:style w:type="paragraph" w:styleId="para1"><w:name w:val="heading 1"/><w:basedOn w:val="para0"/><w:pPr><w:numPr><w:ilvl w:val="0"/><w:numId w:val="15"/></w:numPr><w:spacing/><w:jc w:val="center"/><w:keepNext/><w:outlineLvl w:val="0"/><w:keepLines/></w:pPr><w:rPr><w:b/><w:bCs/></w:rPr></w:style>`
- Char style: `<w:style w:type="character" w:styleId="char1" w:customStyle="1"><w:name w:val="Heading 1 Char"/><w:basedOn w:val="char0"/><w:rPr><w:rFonts w:ascii="Aptos Display" …/><w:color w:val="0f4761"/><w:sz w:val="40"/>…`. **No `w:link` attribute exists anywhere in Sample-6** (regex over all 37 styles: zero matches; Sample-4 carries 27).
- Suppressed heading: `<w:p><w:pPr><w:pStyle w:val="para1"/><w:numPr><w:ilvl w:val="0"/><w:numId w:val="0"/></w:numPr>…<w:r><w:br w:type="page"/></w:r><w:bookmarkStart w:name="_TOC000002"/><w:r><w:t>KATA PENGANTAR</w:t>…`
- TOC entry: `<w:p><w:pPr><w:pStyle w:val="para16"/>(= "toc 1")<w:tabs …><w:tab w:val="right" w:pos="9027" w:leader="dot"/></w:tabs>…<w:hyperlink w:anchor="_TOC000009"><w:r><w:t>BAB 2  PEMBAHASAN</w:t><w:tab/><w:t>4</w:t></w:r></w:hyperlink></w:p>`. The `<w:tabs>` sits **in the paragraph**, and `/word/styles.xml` contains no `<w:tabs>` at all.
- Sections: `sectPr1: pgNumType=lowerRoman start=1 titlePg; sectPr2: decimal start=1; sectPr3-5: decimal`.
- `abstractNum 15`: `w:name="Makalah Default"`, `lvlText` = `BAB %1`, `%1.%2`, `%1.%2.%3`, … `start=1`.
- `numId` usage in `document.xml`: `15` × 42, `0` × 3, plus 19 other ids for lists.
- ECMA-376 §17.9.18 (verified online, Open XML SDK `numId` reference): *"A value of 0 for the @val attribute shall never be used to point to a numbering definition instance, and shall instead only be used to designate the removal of numbering properties at a particular level in the style hierarchy."*

**ODF (local `docs/odf/OpenDocument-v1.4-part3-schema.html`, authoritative per `AGENTS.md`; the path was written `docs/html` in this appendix and corrected on 2026-09-28):** tokens confirmed present in the schema for every element the plans parse, namely `text:list-style`, `text:list-level-style-number`, `text:display-levels`, `text:table-of-content`, `text:index-body`, `text:continue-numbering`, `style:table-column-properties`, `style:column-width`, `text:numbered-paragraph`, `text:bookmark`, `fo:break-before`, `widows`, `orphans`.

**Writer Guide Ch.1 status bar (read online this session):** "Shows the sequence number of the current page, the total number of pages in the document, and the current page number (if different from the sequence number)"; "If you select a portion of text, the count for that selection will temporarily replace the document total count"; Table 1 = Image or Frame → size and position; List item → level and (if relevant) list style; Heading → heading numbering level and (if relevant) list style; Table → name or number and cell reference of cursor; Section → name of section.

**Engine (file:line at `55a9a97`):** `LayoutEngine.kt`: `elementGapDp` :107, `StyleResolver` 14 sp :61, `2.5f` fudge :154, table `rows*35+10`; `LayoutDrivenDocumentRenderer.kt`: `PageStackMetrics` (320/452) :56-61, `fitScale`/`renderScale` :149-150, `pageScale` :243-245, line height `(sizeSp+5f)` :474/:500/:579/:596/:650, the `Color.DarkGray` mention at :665 is a comment (the grey there is already gone); `InkyModule.kt`: `isWebView` :122, cursor-ratio page estimate :524-533, delays :1105-1109, `docxExtents` :212, hub tools :2790-2811, status bar (page range + fit-scale re-implementation) :2396-2470, caret→element seam :659-707, ribbon tabs :2936, unimplemented-tab literal :3383, `activeRibbonTab` written at :2941/:3125 and never read; `OfficeDocumentParser.kt`: `extractDocxPageStyleSpec` :144, `extractDocxStyles` :218 (names only, `namespaceAware=false`), regexes :55-57, `currentRuns` :800, `w:br` type distinction :961-971, `w:tab` :958; `DocxDocumentParser.kt`: `[Image:]` :723, hard-coded `w:numId 1` :740, `generateOdtXml` (`office:version="1.2"`) :748+; `OdfXmlToken.kt`: `XML_A` :24, `XML_BOOKMARK*` :39-41, no list-style/index tokens; `SvXMLImportContext.kt`: hard-coded bullets, no `XML_A`/bookmark arms, soft-page-break mid-paragraph.

**Navigator (⚑):** `UniversalNavigatorSheet.kt` categories = bookmarks, comments, fields, footnotes, frames, headings, images, ole, pages, sections, shapes, tables; every empty one renders `R.string.no_objects_to_navigate`; no hyperlinks category. **Index reachability (verified):** no main-source file constructs `OfficeBookmark(`, `OfficeComment(`, `OfficeSection(`, `OfficeShape(`, `OfficeField(`, `OfficeFootnoteElement(` or `OfficeHyperlink(`; `OfficeParagraph.bookmark` is written only by `DocumentCoreEngines.insertBookmark`; `OfficeTextRun.hyperlink`/`field` are never assigned by a parser; `DocumentIndexEngine.framesList` is never appended to; `OfficeResources.objects` is never populated. `NavigationEngineTest` exercises the index arms with a synthetic document, which is why the arms exist and still cannot fire on a real file.

**Strings/guard:** `values/strings.xml` 701 keys; `values-in/strings.xml` 27 keys (frozen); `SourceHygieneGuardTest`: 47 recorded allowance (46 measured) across 14/13 files, 7 banned literals, five active rules (literal toasts, literal `contentDescription`, raw greys above allowance, em dashes in literals, resurrected 3.3 literals), comment-masking self-test. A Python port of the guard (same masking, same rules) reports "clean" for every rule except the grey allowances at `55a9a97`.

**CI:** `.github/workflows/build.yml`: job `test` (`./gradlew testDebugUnitTest`, zulu JDK 17) on push/PR/schedule/manual; job `build` (SemVer/nightly debug APK, manual release). The guard runs in `testDebugUnitTest`, so no workflow change is needed for any PR here.

## Appendix B: Later stage (explicitly out of scope for PRs 13–22)

`tests/cellina/Sample-1.{ods,xlsx}` and `tests/slidia/Sample-1.{odp,pptx}` are unpacked and inventoried (ODS: 12 tables/312 rows/1852 cells, 0 formulas; XLSX: 12 sheets, 48 shared strings, 2 formulas; ODP: 51 pages/506 text boxes; PPTX: 51 slides/996 shapes/55 media) as the floor for the Cellina/Slidia stages. They are referenced here only so the later-stage plans start from verified structure. Nothing in PRs 13–22 reads, tests or changes them.
