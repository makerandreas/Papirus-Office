# Audit 019 (2026-10-06): Plan 8 readiness, the OOXML shelf, and a repository sweep

**Baseline:** `main` at `f6ce775` ("Add OOXML Documentation (batch 11)"). Work branch
`arena/5f73bf06-papirus-office`. The clone was unshallowed for this session, so the full 372-commit
history and the merge commits of PR #26 to PR #33 are inspectable locally.
**Scope:** (1) the twenty `[MS-*].pdf` documents added in batches 1 to 11, (2) the PR history as the
`anti-slop` plans and audits record it, (3) Plan 8A and 8B re-derived against the tree and the fixtures
instead of against the plan text, (4) an unused-code sweep, (5) a delivery strategy for 8A and 8B.
**Method:** `git log` and `git show --stat` on the local history; `gh pr list` and `gh run list`;
source reading of the DOCX path in `OfficeDocumentParser.kt`, `OfficeDocument.kt`,
`OfficeDocumentModel.kt`, `OfficeRuns.kt` and `Numbering.kt`; raw-package inspection of the six DOCX
fixtures with ElementTree; keyword page indexing of `[MS-OI29500]`, `[MS-OE376]`, `[MS-DOCX]` and
`[MS-OODF14]` with pypdf 6.19.0. Fixture numbers were re-measured here; earlier documents are cited as
sources, never as substitutes for a measurement.
**Supersedes:** the stale fixture facts reused by roadmap v2 §4.8, §4.9 and plan-04-to-09 § Plan 8
(first written 2026-09-24, before the 2026-09-27 fixture re-baseline recorded in audit-008). It does
not change PR or plan ownership.

---

## 0. Findings in one screen

| # | Finding | Evidence | Consequence |
|---|---|---|---|
| A | The twenty PDFs are now analysed and indexed, and are moved to `docs/ooxml/` with a shelf README. | §2, `docs/ooxml/README.md` | Plans 8A and 8B get page-cited answers for Word behaviour. |
| B | Three assumptions carried by the 8A/8B scope text no longer match the tree: "docDefaults and basedOn are unread", "para1 resolves to 20 pt Aptos Display", and several sample counts. | §3 | 8A has to be re-scoped before code starts, or its tests would encode facts the fixtures contradict. |
| C | Plan 5d already shipped a large part of H-1: `extractDocxStyles` reads `w:docDefaults` (`rPrDefault` and `pPrDefault`), the `w:basedOn` chain, runs and paragraph properties, and `StyleChainMetricsTest` asserts the results on four fixtures. | §3.1, `OfficeDocumentParser.kt:312-760`, `StyleChainMetricsTest.kt` | What remains in 8A is narrower and more precise: character properties and linkage, the run model, numbering state, regex retirement. |
| D | The DOCX run path still produces one flat run per paragraph: `currentRuns` is declared and cleared but never appended, and `w:b`/`w:i`/`w:u` are document-level flags reset only at `</w:p>`. | `OfficeDocumentParser.kt:1281-1282`, `:1519-1527`, `:1714`, `:1730` | H-2 is fully open and is the visible defect (bold leaks across a paragraph). |
| E | The fixtures carry localised style ids (`Judul1`, `DaftarParagraf`) with English `w:name` values; the `heading[1-9]` and `para[1-9]` regexes match no fixture style id at all. | §3.2, audit-008 finding E | Retiring the regexes (H-2b) is proven safe on the checked-in corpus rather than assumed. |
| F | Numbering state, measured: 42 of Sample-6's paragraphs inherit numbering from a heading style (`numId 15`, which resolves to `abstractNum 14` with `lvlText` "BAB %1"), 91 carry a direct `w:numPr`, and 3 suppress it with `w:numId 0`. Sample-3 has no `word/numbering.xml`. | §3.3 | 8A records and 8B renders, exactly as the plan intends, with the numbers corrected. |
| G | Five private members and one duplicate constant are provably unused; all six were deleted in this session, and the two scan hits that were false positives are recorded so the check can be repeated. | §5 | The repository carries less dead weight without touching any behaviour; compile verification is CI's when the next push runs. |
| H | The twenty-page `[MS-OI29500]` WordprocessingML map (sections 17.3 to 17.15) is now recorded with page numbers. | §2.3 | Any later "what does Word do" question has a first-stop answer. |

---

## 1. Repository and PR state, verified 2026-10-06

* **Every planned PR has merged.** `gh pr list --state all` re-read on 2026-10-06 shows #33 (Plan 7F)
  merged 2026-10-05 17:54 UTC, #32 (selection projection) merged 2026-10-05 06:33 UTC, #31 (Plan 7E)
  merged 2026-10-04 22:23 UTC, and #26 to #30 before them. Plan 7F closed out inside its own PR; the
  forward schedule therefore starts at the slot forecast for 8A.
* **PR #34 (this pass) is open and green.** The preparation commit `cf82081` was pushed to
  `arena/5f73bf06-papirus-office`; run `37430436955` on merge ref `435f26d` passed 358 unit tests across
  66 suites with zero failures, errors or skips, plus the Build (SemVer & Nightly) job. Details in §6.
* **The earlier ledger, from the same call and the plans that own it:** #1 2026-09-18 audit-fix
  round (security, crash, build, correctness); #2 opened and closed without merging, superseded by
  #3 2026-09-20 (LOKit simulated seam, streaming zip guards, undo race); #4 2026-09-21 structural
  Navigator; #5 2026-09-21 CI APK uploads and `OdtSampleHeadingTest`; #6 2026-09-22 Viewer bar,
  Navigator locale, image hash; #7 2026-09-22 declared page geometry; #8 to #10 2026-09-23 the B1,
  B2 and C presentation runs; #11 2026-09-24 Plan 2; #12 Plan 3A and #13 Plan 3B, 2026-09-24;
  #14 2026-09-25 Plan 3C/Plan 11 documentation; #15 2026-09-26 Plan 5A; #16 to #18 2026-09-27
  Plans 5B to 5D; #19 and #20 2026-09-28 Plans 5E and 1A; #21 2026-09-28 Plan 1B; #22 to #25
  2026-09-29 to 2026-09-30 Plan 6A to 6D. PR #33 is the thirty-third merged change and the
  seventeenth planned PR since the plan ledger began.
* **CI is green on `main`.** The four most recent runs (`37427251839`, `37423907849`, `37423490947`,
  `37423225975`) all passed in about six minutes each; they are the OOXML documentation batches 8 to 11.
* **Fixtures.** Six ODT/DOCX pairs under `tests/inky`, pinned by `fixture-identities.properties`
  (SHA-256, producer, saved page count) and asserted by `FixtureIdentityTest`. Producers: Microsoft
  Office Word 16 for the DOCX, Collabora Office 26.04.3.1 for the ODT.
* **Page matrix, from the Plan 7F closeout CI run** (`37311218794`, 358 tests, 66 suites, all green):
  ODT 14/23/21/10/18/20, DOCX 15/25/25/11/19/24, every count inside its `SampleMatrix` window, no empty
  pages. References are DOCX 15/23/22/10/18/21 and ODT 15/23/22/11/19/22.
* **Verification path.** No JDK and no Gradle distribution exist in this sandbox (AGENTS.md, audit-016
  §8). GitHub Actions is the compile and test authority; the owner also runs the same suite in the
  Google AI Studio environment. Statements below that come from reading source are labelled as such.

### 1.1 PR history in one table

| PR | Plan | Delivered (as the plans and audits record) | Verification |
|---|---|---|---|
| #26 | 12A + 7A | LibreOfficeKit JNI seam; ODF numbering, runs, links, bookmarks | merged 2026-10-02 |
| #27 | 7B | Canonical ODT semantic importer convergence; unsupported modified saves fail closed | merged 2026-10-03 |
| #28 | 7C | Authored indexes, section ranges, Navigator rows and status | merged 2026-10-04 |
| #29 | 7D | Tables end to end: geometry, layout, rendering, hit testing | merged 2026-10-04 |
| #30 | repair | Reverted the post-7D regression, kept the verified-good parts | merged 2026-10-04 |
| #31 | 7E | ODF font-face declarations, alias resolution, calibration | 348 tests, 0 failures, runs `37213135047`, `37215310814` |
| #32 | correction | Shared `DocumentTextProjection` for editor, selection and merge offsets | 350 tests, run `37259378329`; merge run `37259905276` |
| #33 | 7F | ODF textual tab leaders; body-level hidden-section layout | leading hand 355 tests (65 suites), second hand 358 tests (66 suites); matrix unchanged; merged 2026-10-05 17:54 UTC |

The pattern the history establishes, and which 8A should keep: one PR per plan, feature commits with
their own test gates, a focused test suite per feature, and a page matrix that must not move unless the
plan says it may.

---

## 2. The OOXML shelf

### 2.1 What was added

Twenty Microsoft Open Specifications, 8,453 pages in total, added to the repository root in eleven
commits and moved to `docs/ooxml/` in this session:

| Family | Documents | Pages |
|---|---|---|
| OOXML implementation notes | `[MS-OI29500]` (936), `[MS-OE376]` (1027) | 1963 |
| Word extensions | `[MS-DOCX]` (119) | 119 |
| ODF implementation notes | `[MS-OODF14]` (848), `[MS-OODF13]` (835), `[MS-OODF3]` (823), `[MS-OODF2]` (953), `[MS-OODF]` (927) | 4386 |
| Module extensions | `[MS-PPTX]` (170), `[MS-XLSX]` (437) | 607 |
| Drawing extensions | `[MS-ODRAWXML]` (419) | 419 |
| Custom UI | `[MS-CUSTOMUI]` (553), `[MS-CUSTOMUI2]` (205) | 758 |
| Macro and extensibility | `[MS-OFFMACRO]` (43), `[MS-OFFMACRO2]` (33), `[MS-OEXTXML]` (14) | 90 |
| Feature extensions | `[MS-OINTXML]` (40), `[MS-OREACTXML]` (19), `[MS-OTASKXML]` (28), `[MS-OWEXML]` (24) | 111 |

`docs/ooxml/README.md` holds the per-document inventory, the reading order, the pypdf recipe for
re-finding a page, and the topic-to-page maps below. Every release date is on page 1 of its file; the
shelf spans v20160914 (`[MS-OFFMACRO]`, `[MS-OODF]`) to v20260818 (`[MS-OI29500]`, `[MS-DOCX]`,
`[MS-OREACTXML]`).

### 2.2 Why this matters for Plan 8

The repository already cites ECMA-376 as the normative OOXML source (AGENTS.md, CONCEPT.md). ECMA-376
says what a package must contain. The `[MS-OI29500]` and `[MS-OE376]` documents say what Word does
with the corner cases the standard leaves open, which is exactly the class of question Plan 8 keeps
asking: how a toggle resolves through several style levels, whether a duplicate `w:link` is honoured,
which children of a numbering `pPr` Word accepts, how page parity is computed for headers.

### 2.3 The WordprocessingML page map (from `[MS-OI29500]`, v20260818)

| Standard part | Topic | Pages |
|---|---|---|
| 17.3 | Paragraphs and paragraph properties (`pPr`, `spacing`, `ind`, `jc`, `tabs`, `keepNext`, `keepLines`, `pageBreakBefore`, `outlineLvl`) | 62 to 80 |
| 17.4 | Tables (`tblGrid`, `gridSpan`, `vMerge`, `tcPr`, `trPr`) | 80 to 95 |
| 17.5 | Custom XML and structured document tags | 95 to 101 |
| 17.6 | Sections (`sectPr`, `sectPrChange`) | 101 to 104 |
| 17.7 | Styles (`style`, `basedOn`, `link`, `aliases`, `latentStyles`, toggle properties, table-style conditionals) | 104 to 120 |
| 17.8 | Fonts and substitution | 120 to 122 |
| 17.9 | Numbering (`abstractNum`, `num`, `lvl`, `lvlOverride`, `numId`, `numStyleLink`) | 123 to 126 |
| 17.10 | Headers and footers (`evenAndOddHeaders`, `footerReference`) | 126 to 127 |
| 17.11 | Footnotes and endnotes | 127 to 129 |
| 17.12 | Glossary document | 129 |
| 17.13 | Comments, revisions, move ranges | 129 to 140 |
| 17.14 | Mail merge | 141 to 145 |
| 17.15 | `settings.xml` | 147 to 164 |
| 17.16 | Fields and hyperlinks (instruction syntax, `fldSimple`, form fields) | 164 to 226 |
| 17.17 to 17.18 | Subdocuments and imported content; shared simple types (`ST_*`) | 226 to 236 |

Six behaviours from the shelf that Plan 8 should encode, each with its citation:

1. **Toggle resolution (17.7.3, p.105).** In Word the effective value of a toggle is true if and only
   if it is false at an even number of levels, and a level that does not set it takes the document
   default. This is the rule H-2's tri-state helper has to implement.
2. **`w:link` duplicates (17.7.4.6, p.106).** Word keeps only the last link for a given style, so a
   reader may follow `w:link` but must never require it as the only path. Four of six fixtures declare
   no `w:link` at all, which is why H-1b's name-convention fallback is load-bearing.
3. **`NoList`, `DefaultParagraphFont`, `TableNormal` (17.7.4.17, p.116).** Word ignores any child
   elements of these styles, and ignores a `styleId` longer than 253 characters. Fixtures declare all
   three; the reader must not let their children leak into resolution.
4. **Numbering bounds (17.9.1 to 17.9.18, p.123 to 125).** `abstractNumId` and `numId` at least 0,
   `ilvl` 0 to 255, at most nine `%[1-9]` sequences, `lvlText` at most 31 characters after
   substitution. `numId 0` is the suppression value, which the fixtures use.
5. **Numbering `pPr` (17.9.22, p.125).** Word allows only `jc`, `ind` and `tabs` there.
6. **Header inheritance and page parity (17.10.1, p.126).** Page parity comes from the section's
   `pgNumType` start value, and a missing header in a later section inherits the previous section's
   header rather than becoming blank.

### 2.4 The ODF half of the shelf

`[MS-OODF14]` and its four predecessors describe how Microsoft Office reads and writes ODF. They are
the mirror image of the OOXML notes and are the natural reference for Plan 9 (writing ODF) and for the
cross-format convergence acceptance in 8B, where the two representations of one document have to agree.
No plan currently cites them; this audit records them as available rather than as owned work.

### 2.5 Online sources checked for the linked-style question

* The .NET **Open XML SDK** reference for the `Style` class (`documentformat.openxml.wordprocessing.style`,
  `openxml-3.0.1`) confirms the `w:style` children this plan needs: `StyleName`, `Aliases`, `BasedOn`,
  `NextParagraphStyle`, **`LinkedStyle`** (the `w:link` element), plus `StyleRunProperties` and
  `StyleParagraphProperties`. It is the SDK-side view of the same element `[MS-OI29500]` §17.7.4.17
  constrains, and it is why the reader can treat `w:link` as an ordinary child element rather than a
  special case. URL: `https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.style`.
* The LibreOffice API (`api.libreoffice.org`, 26.8 SDK reference, `style::CharacterStyle`) and the
  LibreOffice Writer help were opened for the linked-style semantics and returned navigation text
  only in extraction, so they are not cited for a behaviour here. The ODF-side evidence stays with
  `docs/odf` and the LibreOffice 26.2 guides in `docs/lo-guides`, which are already pinned by
  `AGENTS.md` and are readable offline.

---

## 3. Where the documented Plan 8 assumptions stand against the tree

### 3.1 The "names-only style chain" is already gone (Plan 5d)

Roadmap v2 §4.8 and plan-04-to-09 § Plan 8 were written before Plan 5d. `extractDocxStyles`
(`OfficeDocumentParser.kt:293-755`) already:

* reads `w:docDefaults` `rPrDefault` (`rFonts` ascii/hAnsi, `sz`, later `szCs` ignored) and `pPrDefault`
  (`spacing` before/after/line/lineRule, `ind` left/right/firstLine/hanging, `tabs`, `keepNext`,
  `keepLines`, `widowControl`, `pageBreakBefore`, `suppressAutoHyphens`);
* reads per-style `w:name`, `w:basedOn`, `w:outlineLvl`, `w:rPr` (`rFonts`, `sz`, `b`, `i`, `u`) and
  `w:pPr` (`spacing`, `ind`, `jc`, `tabs`, `keepNext`, `keepLines`, `widowControl`,
  `pageBreakBefore`);
* cascades the `w:basedOn` chain (`cascadeDocxStyle`, `:611`) and exposes `paragraphStyles` plus a
  `defaultParagraphStyle` on `DocxStylesParseResult`.

`StyleChainMetricsTest` then asserts the results on real files: Sample-1 DOCX `Normal` = 11 pt with
`after 160` and a 1.15 line factor; Sample-4's cascade from `pPrDefault` to `Normal` to
`TidakAdaSpasi`; Sample-6 `Judul1` = 12 pt bold through `StyleResolver.resolveParagraphStyle`;
Sample-3's Heading 1 = 20 pt in both formats. The audit-008 finding H requirement ("read the default
paragraph style's own `pPr`, not only `docDefaults`") is implemented.

What is **not** read: `w:link`, `w:rStyle` resolution, `w:color`, `w:highlight`, `w:vanish`, `w:szCs`,
`w:kern`, character styles as a table (only `paragraph` type entries are stored), `w:numPr` inside
styles, and `w:name`-based linkage between a paragraph style and its character twin. That is the real
H-1 remainder.

### 3.2 The "para1 resolves to 20 pt Aptos Display" acceptance target cannot pass

The acceptance text in roadmap v2 §4.8 and the test names it proposes (`DocxStyleChainTest`: `para1` to
20 pt Aptos Display `#0f4761`, `para2` 16 pt, `para3` 14 pt, `docDefaults` 12 pt, Sample-3 as control)
describe the fixtures as they were before 2026-09-27. audit-008 finding E re-baselined that: M365 on an
Indonesian locale writes localised style ids. Measured here on the checked-in files:

| Fact | Sample-1 | Sample-2 | Sample-3 | Sample-4 | Sample-5 | Sample-6 |
|---|---|---|---|---|---|---|
| Paragraph style ids seen in `w:pStyle` | `Judul`, `Judul1` to `Judul4`, `DaftarParagraf`, `Keterangan`, `Kutipan` | plus `TOC1`, `TOC2` | `Judul`, `Judul1`, `Judul2`, `Kutipan` | plus `Bibliografi`, `TOC1`, `TOC2` | `Judul1` to `Judul4`, `TOC1`, `TOC2`, `DaftarParagraf` | plus `Subjudul`, `TOC3` |
| Any style id matching `para[1-9]` or `heading[1-9]` | none | none | none | none | none | none |
| `w:name` of `Judul1` | heading 1 | heading 1 | heading 1 | heading 1 | heading 1 | heading 1 |

The current, measured heading sizes are: Sample-1 body 11 pt (from `Normal`), everything else 12 pt from
`docDefaults` (Aptos), Sample-6 `Judul1` 12 pt bold, Sample-3 Heading 1 20 pt, Sample-4 `Judul` (Title)
28 pt from `w:sz 56`. The closest thing to the old "Aptos Display heading" claim in the corpus is
`Judul4` to `Judul9`, which set `Aptos Display` only on the `eastAsia` and `cs` font slots while the
`ascii`/`hAnsi` slot stays Times New Roman.

Two more measured facts belong to the same correction:

* **The link rule has a discriminating case.** `Sample-2.docx` and `Sample-5.docx` put `sz 28`
  (14 pt) on the `Judul1` paragraph style while their `Heading1Char` character style still says
  `sz 40` (20 pt). A property set in both places cannot be resolved by an "either" rule; the
  fixtures force a decision. Recommended: the paragraph style's own `w:rPr` wins for the properties
  it sets and the linked character style supplies the rest, with `Sample-2`/`Sample-5` as the
  regression case for that choice.
* **The run path is not pagination-neutral.** Samples 1, 4 and 6 have no size on their `Judul1`
  paragraph style, so once the linked character style is honoured the level-1 headings grow from
  12 pt to 20 pt. The page counts of those three DOCX fixtures will move; their windows (12..18,
  8..12, 15..26) absorb some movement, but only a CI run can show how much. 8A must measure the
  matrix rather than assume it is unchanged, and the plan should say so before the code lands.

Consequence: 8A's tests must be written against the list above, and the acceptance sentence in the
roadmap should be corrected when the plan document is revised. Encoding the old target would produce a
test that cannot pass on the repository's own fixtures.

### 3.3 Numbering state, measured

| Sample | Style-inherited numbered paragraphs | Direct `w:numPr` | Direct `numId 0` | `numbering.xml` |
|---|---|---|---|---|
| 1 | 27 | 27 | 1 (`Judul1` "References") | 10 abstractNum, 10 num |
| 2 | 27 | 106 | 3 (`Kata Pengantar`, `Daftar Isi`, `Daftar Pustaka`) | 25 abstractNum, 25 num |
| 3 | 0 | 0 | 0 | absent |
| 4 | 3 | 14 | 0 | 5 abstractNum, 5 num |
| 5 | 35 | 75 | 3 | 18 abstractNum, 18 num |
| 6 | 42 | 91 | 3 (`KATA PENGANTAR`, `DAFTAR ISI`, `DAFTAR PUSTAKA`) | 21 abstractNum, 21 num |

Sample-6's inheritance resolves through `Judul1` (`numId 15` with no `ilvl`), `Judul2` (`ilvl 1`),
`Judul3` (`ilvl 2`); `numId 15` maps to `abstractNum 14`, whose levels are `BAB %1` and `%1.%2` to
`%1.%2.%3.%4.%5.%6.%7.%8.%9.`. The roadmap's "42 explicit + 3 suppressed" describes the right shape but
the wrong split: 42 is the inherited group and 91 is the direct group. The "133 `w:numPr` paragraphs"
figure in the sample matrix is the sum of both, which is correct as a raw element count.

Every one of the six files exercises the numbering reader except Sample-3 (audit-008 said "five of six";
that is confirmed).

### 3.4 The run path, read line by line

* `val currentRuns = mutableListOf<TextRun>()` and `val currentRunText = StringBuilder()` are declared
  at `OfficeDocumentParser.kt:1281-1282`, cleared at `:1386` (paragraph start) and `:1730` (paragraph
  end), and never appended to. The paragraph falls back to
  `listOf(TextRun(paraText, isBold, isItalic, isUnderline))` at `:1714`, so every DOCX paragraph is one
  flat run whose flags are the paragraph-wide flags.
* `tagLocal == "b"`, `"i"`, `"u"` at `:1519-1527` set those paragraph-wide flags and ignore `w:val`
  entirely, so `w:b w:val="0"` cannot switch bold off and a bold run makes the rest of the paragraph
  bold until `</w:p>`.
* The text is appended only when `tagLocal == "t"` (`:1497` opens, `:1624` appends, `:1649` closes),
  which already keeps `w:instrText` out of the fallback path used for DOCX. `w:fldSimple`, `w:fldChar`
  and `w:hyperlink` are not handled at all (8B scope), and `w:tab` in a run becomes a literal `\t`
  (`:1560`).
* `OfficeRuns.mergeRun` (`OfficeRuns.kt:68-83`) ORs flags: `charHit?.isBold == true || run.isBold ||
  base.isBold`. A run can therefore add bold but never remove it, which is the display-side mirror of
  the parser defect. `w:val="0"` needs a representable negative before either side can be fixed.

### 3.5 What this means for the 8A size estimate

The plan rates 8A "large". With H-1 largely shipped by Plan 5d, the remaining work is:

1. character-property completeness and linkage (`w:link`, `X` to `X Char`, `w:rStyle`, `color`,
   `highlight`, `vanish`, `szCs`, and the `NoList`/`DefaultParagraphFont`/`TableNormal` rule);
2. the run model end to end, including the tri-state negative and the paragraph's run list;
3. numbering state per paragraph (resolve in 8A, render in 8B);
4. regex retirement and the tests that prove it.

That is still a substantial PR, but it is closer to the 7F shape (two gated feature commits) than to a
rewrite, and most of the risk sits in step 2 because layout and display both consume the run list.

---

## 4. Proposed Plan 8A shape (for owner approval)

Two delivery options, both keeping the roadmap's plan identity and PR forecast (#34 for 8A, #35 for 8B):

**Option 1 (recommended): one PR, two gated feature commits, plus a records commit.**
Commit 1, character properties and linkage: tri-state toggle helper; read `w:link` and resolve the
`X` to `X Char` convention; read character styles into `DocumentStyles.characterStyles`; read `w:rStyle`
on runs; add `color`, `highlight`, `vanish`, `szCs`; apply the `NoList`/`DefaultParagraphFont`/
`TableNormal` rule; record per-paragraph numbering state; retire `PARA_STYLE_REGEX`,
`HEADING_STYLE_REGEX` and the single-digit heuristic. Its gate is a new `DocxStyleChainTest` plus an
extended `StyleChainMetricsTest`, with the page matrix unchanged.
Commit 2, the run model: build `TextRun`s per `w:r` from resolved properties, represent explicit
negatives, reset flags per run, wire `currentRuns` into the paragraph and into `OfficeTextRun`
conversion, keep `OfficeRuns.mergeRun` able to subtract. Its gate is a new `DocxRunFormattingTest`
(no leak, `w:val="0"` unbold, mixed runs in one paragraph) and the matrix unchanged.

**Option 2: three PRs.** 8A-1 character properties and linkage, 8A-2 run model, 8B numbering and the
rest. Smaller reviews, two extra merge cycles, and the PR forecast shifts by one.

Either way, 8B keeps its scope (numbering reader and rendering, fields, TOC snapshot, table geometry,
per-paragraph `sectPr`, `pgNumType`, `lastRenderedPageBreak`) and gains its page citations from §2.3.

### 4.1 Owner decisions, 2026-10-06

1. **PR #34 stays open for the owner's review.** It is not merged by the agent; its CI is green and the
   branch is pushed.
2. **Option 1 is approved.** 8A is one PR with two gated feature commits (character properties and
   linkage first, the run model second) plus the records commit.
3. **The paragraph style wins over the linked character style for properties it sets**, with the linked
   character style supplying only the properties the paragraph style leaves unset. Samples 2 and 5
   therefore render their level-1 headings at 14 pt, not 20 pt, and the new `DocxCharLinkTest` carries
   this case as a named assertion rather than a comment. This decision is scoped to the DOCX reader;
   it does not change the ODF side, which already resolves `style:parent-style-name` by cascade.
   Rationale for the record: the paragraph style's `w:rPr` is the more specific statement about that
   style's own run properties, and treating a linked style as an override would make the effective size
   of a heading depend on whether a file happened to be authored with links (Sample 2 and 5) or through
   the name convention only (Samples 1 and 6), which is one document behaving two ways for no reason
   the file states.

### 4.2 Standing generalisation requirement, 2026-10-06

Owner instruction, recorded before commit 1 of 8A is written: the fidelity work must not be
fixture-specific. A rule may use only declarations inside the file being read (the style chain, `w:link`,
`w:numPr`, `w:rPr` toggles, `w:outlineLvl`, `w:docDefaults`), so the same rule is correct for documents
the repository has never seen:

1. **Newly created blank documents.** A document created in the app starts from the untitled template
   and has no Word style table beyond what the app itself writes. The DOCX reader falls back to
   `w:docDefaults` and then to the app default `Normal`; it must never need a fixture style id to exist.
2. **Documents created from templates later (Plan 9 and beyond).** The save path writes the styles it
   creates, and the reader resolves them from their own declarations, including `w:link` pairs it has
   never seen and `w:name`/`w:outlineLvl` rather than the shape of an id.
3. **Any other document the owner opens, including the Realme C3 pass after Plan 11.** Localised ids
   (`Judul1`, `DaftarParagraf`) resolve through their `w:name`, their `w:basedOn` chain and their
   `w:outlineLvl`. Neither fixture style ids nor fixture numbers may appear in behaviour.

Enforcement, binding on both 8A commits:

- **Behaviour reads declarations only.** Fixture names may appear in comments as rationale (for example
  the Sample-4 note on `resolveNumbering`), but no branch may test a fixture id, name or `numId`. The
  retired `PARA_STYLE_REGEX`, `HEADING_STYLE_REGEX` and single-digit heuristic are the pattern this
  requirement exists to prevent (section 3.2).
- **Every new rule is asserted with a synthetic style table built inside the test** (short XML strings
  fed to the reader), so the assertion is about the rule. The fixture tests stay regression evidence for
  the six samples and are not the proof of the rule; this is also what makes the deferred device pass
  meaningful on unseen files.
- **Unmatched input keeps a defined fallback:** a style id the file's own style table does not contain
  resolves to the Normal-based default, never to a heading and never to a fixture value.
- **Numbering state follows the file's own declaration:** the nearest `w:numPr` in the chain wins,
  `w:numId` 0 suppresses ([MS-OI29500] section 17.9.18, p.125), and a direct `w:numPr` beats the style
  chain. No `numId` value seen in a fixture is special-cased.
- **The ODF reader keeps the same bar** through the `style:parent-style-name` cascade
  (`SvXMLImport.kt`), and commit 1 must not regress `Sample5StyleFidelityTest`.

**Recorded deviation from two ECMA-376 tier rules (commit 1).** Inside one `w:basedOn` chain the
reader takes the nearest definition, which is the standard rule. Across tiers, §17.7.2 orders the
character style above the paragraph style and §17.7.3 combines the twelve toggle run properties
(`b`, `i`, `caps`, `smallCaps`, `strike`, `vanish`, and the rest) by XOR (`[MS-OI29500]` §2.1.230,
p.105), while this reader lets the paragraph style's own `w:rPr` win and its linked character style
fill only the gaps. That is the owner decision in section 4.1 and it is a deliberate deviation: a
linked pair is one logical Word style written twice, the decision is what keeps Samples 2 and 5 at
14 pt, and un-modelled cross-tier XOR must not be claimed as support. Direct run formatting (commit 2)
still overrides both tiers.

**Test naming, reconciled.** Option 1 above and section 4.1 name the commit-1 gate differently. The
commit-1 gate is one new class, `DocxStyleChainTest`, holding the linkage cases from section 4.1 item 3
(`w:link` beats the name convention; the name convention when no link exists; a back link declared on
the character style still pairs; the paragraph style's own `w:rPr` beats the linked character style, so
Samples 2 and 5 stay at 14 pt) plus the toggle, ignored-child and numbering-state cases. The extended
`StyleChainMetricsTest` keeps the fixture-chain numbers, and the twelve-fixture page matrix is
re-measured after commit 1 because three DOCX counts move. `DocxRunFormattingTest` and the measured
matrix remain commit 2.

### 4.3 Plan 8A delivery record, 2026-10-06

Commit 1 (character properties and linkage) is `b328da7`, with `5880a8d` as the
compile fix CI found (`OfficeDocument` exposes `body.elements`; the six errors of
run `37454077585` were that single mistype in the new test). Commit 2 (the run
model) is `e9c0ada`, with `e7ce0c7` and `698bc7d` correcting test expectations
that runs `37475988884` and `37477229929` caught. The records commit is `639414c`
and this one.

Test evidence, from the CI reports rather than from this sandbox (there is still
no JDK here):

| run | head | result |
|---|---|---|
| `37454077585` | `b328da7` | compile failure, 6 errors, one mistyped receiver |
| `37454972514` | `5880a8d` | 373 tests / 67 suites, 0 failed, Build green |
| `37455734065` | `639414c` | records commit, both jobs green |
| `37475988884` | `e9c0ada` | 383 tests, 3 failed, all in the new run test |
| `37477229929` | `e7ce0c7` | 383 tests, 2 failed, all in the new run test |
| `37478258709` | `698bc7d` | 383 tests / 68 suites, 0 failed, Build green |

`DocxStyleChainTest` 11/11, `DocxRunFormattingTest` 10/10,
`StyleChainMetricsTest` 8/8, `OfficeRunsTest` 11/11,
`ParagraphStyleSemanticsTest` 13/13, `Sample5StyleFidelityTest` 4/4,
`HeadingRunsTest` 2/2, `HyperlinkFidelityTest` 2/2, and
`PaginationFidelityTest` green across all twelve windows with Sample-6.docx at
24 pages inside its 15..26 window and zero empty pages.

Every intermediate failure was a wrong expectation about which layer declares a
value (a base span read where a run override was meant, a run asserted to hold a
style's value, a heading asked of a paragraph-only helper) and each fix kept the
assertion, moved it to the layer that holds the value, or turned the
no-override case into an assertion of its own. No assertion was weakened.

---

## 5. Unused code sweep

Method: declarations whose only occurrence in `app/src` (main plus test) is the declaration itself,
plus private members with no in-file use. Two hits of the first scan were false positives and are
recorded so the rule is auditable: `DocumentSerializer.toParsedRun` is used by its receiver's own
file, and `PrintingFramework`'s `context` is a constructor property, not a function. The `data/framework`, `data/calc`, `data/impress`, `data/api`,
`data/bridge`, `data/db`, `data/crash` and `org/libreoffice` trees are excluded: they mirror external
APIs on purpose and are not evidence of dead code. `data/util/OfficeDocumentComparator` and
`OpenXmlUnits` are used by tests and stay.

### 5.1 Tier A, deleted in this session (provably unused, no API surface)

| Location | What | Proof |
|---|---|---|
| `MainActivity.kt:89` | private `copyCapped` and the `MAX_INCOMING_FILE_BYTES` constant that existed only for it | the live equivalent is `OpenedDocumentStore.copyCapped` and `ZipSafe.copyCappedTo`; the constant had no other reader |
| `OfficeUiComponents.kt:302` | private `FormatButton` | no reference in the file or elsewhere |
| `OfficeUiComponents.kt:309` | private `VerticalSeparator` | same |
| `LayoutDump.kt:198` | private `nextElementIsBreak` | same |
| `OfficeDocumentParser.kt:210` | private `extractOdtStylesXml` | same; the live ODT style path is `SvXMLImport` |

### 5.2 Tier B, unused but owned or reserved by a future plan (left in place, listed for a decision)

| Location | What | Why it is not deleted here |
|---|---|---|
| `core/fonts/FontScanner.kt`, `FontSyncService.kt` | font scanning and sync services with no caller | the font engine thread is parked until Plan 10 resumes |
| `data/DocumentCoreEngines.kt` (`DocumentTransaction`, `InsertTextCommand`, `DeleteTextCommand`, `SplitParagraphCommand`, `MergeParagraphsCommand`, `InsertTableCommand`, `InsertImageCommand`, `DrawingLayer`) | a second command set that no caller uses | the live commands live in `data/writer/commands`; deleting the duplicate is a code-ownership decision |
| `data/writer/SwTextAttr.kt` (`SwFormatBold` to `SwFormatMeta`) | attribute classes with no reference | the base class is used; the subclasses may be the seed of the Plan 9 writer model |
| `ui/components/PapirusEngineLoadingIndicator.kt:219` | `PapirusEngineLoadingOverlay` | loading UI; Plan 11 owns UI packages |
| `ui/components/OfficeUiComponents.kt:199`, `:242` | `OfficeDialogSheet`, `OfficeSidebar` | declared as dialog and sidebar wrappers for the Plan 11 packages |
| `ui/home/HomeDashboard.kt:182`, `:192`, `:207` | `getDirectoryShortcut`, `getExternalStorageShortcut`, `getFileTypeForFile` | the module-type mapping duplicates live logic elsewhere; removal touches the Start screen |

### 5.3 Tier C, deliberately kept

`data/framework/*` (the UNO-style API mirror), `data/calc/*`, `data/impress/*`, `org/libreoffice/kit/*`
(the LibreOfficeKit seam), `OfficeDocElement` (retired by Plan 9), and every test-only helper.

---

## 6. Delivery Gate

Documentation-only deliverable (this audit, the shelf README, the moved PDFs, the plan notes) plus a
small set of deletions of provably unused private members (section 5.1). The reduced gate from
AGENTS.md applies to the documentation, and the deletions are covered by their own evidence line. No
UI ships in this deliverable.

* **R-02 PASS:** no em dash in any file written here; verified with a code-point search over each new
  file. Existing repository documents keep their own punctuation.
* **R-15 PASS:** no call-to-action copy is produced.
* **R-16 PASS:** no marketing language; the text names files, line numbers, sections and measurements.
* **R-17 PASS:** every number carries its source: audit-008 for the fixture re-baseline, `SampleMatrix`
  for references and windows, the Plan 7F closeout for the CI matrix, `gh` for PR and run identifiers,
  and this session's own raw-package measurements where the number is new.
* **R-36 PASS:** the audit states plainly that no build, test or Gradle run happened in this sandbox,
  and labels every claim that comes from reading source. The deletions are reference-verified here,
  and the compile and test evidence came from CI once the branch was pushed.
* **CI PASS (executed evidence):** the pass opened as **PR #34** (`Plan 8 preparation: shelve the OOXML references and re-derive the 8A/8B scope`) and run
  [`37430436955`](https://github.com/makerandreas/Papirus-Office/actions/runs/37430436955) on merge ref `435f26d` passed
  **358 unit tests across 66 suites, 0 failed, 0 errors, 0 skipped** (32.08 s of JUnit time) and the Build (SemVer & Nightly) job, in 5m57s and 5m23s.
  That is the same test and suite count as the Plan 7F closeout, so the removals cost no coverage.
* **R-38 PASS:** nothing fabricated; the only new prose describes the repository.
* **C-5 PASS:** the two stale claims in earlier plan text are corrected in place rather than repeated.
* **R-26, R-27, R-32, R-34, R-35 N/A:** no UI shipped in this deliverable.

---

## 7. Files changed by this session

| File | Change |
|---|---|
| `docs/ooxml/README.md` | New: the twenty-document inventory, reading order, pypdf recipe, topic-to-page maps, Word behaviours. |
| `docs/ooxml/*.pdf` | The twenty PDFs moved from the repository root (`git mv`, no content change). |
| `app/src/main/java/com/example/MainActivity.kt` | Deleted the unused private `copyCapped` and its constant. |
| `app/src/main/java/com/example/ui/components/OfficeUiComponents.kt` | Deleted the unused private `FormatButton` and `VerticalSeparator`. |
| `app/src/main/java/com/makerandreas/papirusoffice/data/LayoutDump.kt` | Deleted the unused private `nextElementIsBreak`. |
| `app/src/main/java/com/makerandreas/papirusoffice/data/OfficeDocumentParser.kt` | Deleted the unused private `extractOdtStylesXml`. |
| `anti-slop/audit-019-2026-10-06-plan-8-readiness-and-repo-sweep.md` | This file. |
| `anti-slop/plan-01-master-index.md` | A 2026-10-06 entry recording the shelf move, the 8A re-scope and the deletions, plus the registry rows and the §2 forecast refresh after PR #34. |
| `anti-slop/plan-2026-09-24-remaining-pr-roadmap-v2.md` | v2.12 and v2.13 amendment lines, correction blocks in Plan 8A and 8B, and §7 changelog items 24 and 25. |
| `AGENTS.md` | The reference-editions line now names `docs/ooxml` for the Microsoft implementation notes. |
