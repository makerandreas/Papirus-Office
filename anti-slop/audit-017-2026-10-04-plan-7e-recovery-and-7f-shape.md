# Audit 017 (2026-10-04): Plan 7E recovery state, prior-PR review, and the Plan 7F shape

**Baseline:** `main` `9356212` ("Merge pull request #30", the post-7D repair), verified against GitHub in this session.
**Scope:** a read-only state verification, a review of the landed Plan 7 PRs against the ledger, a fresh re-derivation of the twelve `tests/inky` fixtures for the two plans still open, and a recommended shape for finishing 7E and running 7F. No production code changes.
**Method:** `git`/`gh` for refs, pull requests, runs and comments; `python3` (`zipfile` + `re`) over the raw package XML for every fixture number below; file:line tracing in the working tree; ODF 1.4 Part 3 read from `docs/odf/OpenDocument-v1.4-part3-schema.html`. No JDK exists in this sandbox (`AGENTS.md`, "Local toolchain check"), so nothing here was compiled or executed as a test; every statement that is not raw command output is labelled static.

---

## 1. Verified state at session start

The handoff this session inherited (`finish Plan 7E`, commit `c136346` at the branch head, four unpushed commits, a patch backup in `/home/user/patches-7e`, a saved CI dump in `/home/user/pr30-cidump.md`) does not match this workspace. What exists:

| Item | Handoff said | Verified this session |
|---|---|---|
| Local head | `c136346`, four 7E commits on top of `b7c195c` | `9356212`; the repository is a shallow, grafted clone whose only local commit is the PR #30 merge |
| 7E commits | `1a51970`, `8d7ea6e`, `07bdb85`, `c136346` | absent: `git log --all --oneline` returns `9356212` only |
| Remote `arena/01a10750-papirus-office` | still `b7c195c` | `b7c195c` ("docs: record the post-7D unit-test analysis and the local toolchain check"), branch head confirmed through `gh api` |
| Patch backup | `/home/user/patches-7e/0001..0004-*.patch` | the directory does not exist |
| `pr30-cidump.md` | present | absent; re-fetched here as issue comment `5981186452` (43,610 characters) |
| Open pull requests | none expected | none: `gh pr list --state open` returns `[]`; the newest PR of any state is #30, merged 2026-10-04T15:03:04Z |

**Finding 1.** Plan 7E has no code on any ref. `main` is the repair merge, the session branch is its parent, and no branch contains a font-face reader, a resolver, or a Plan 7E test. Grepping the tree for the 7E surface (`font-face-decls`, `XML_FONT_FACE_DECLS`, `fontFaces`, `OfficeFontFace`, `FontFaceResolver`) returns only the Plan 7B placeholders: the token `OdfXmlToken.kt:23` (no consumer), the model fields `OfficeDocument.kt:279-280` ("Populated by Plan 7E") and `DocumentSemantics.kt:213`, the detector `OdtImportPipeline.kt:250` (`"font-face" -> fontFaces = true`), and the round-trip test `Plan7bSemanticImportTest.kt:162-187`. Items 1 to 3 and decision D3 of roadmap v2 section 4.7d are therefore unstarted, not partially delivered.

**Finding 2.** The pre-7E calibration baseline is re-established. CI run `37211551672` (push to `main` at `9356212`) is green. The PR #30 comment records run `37209964838` at head `6238676`: **330 tests, 61 suites, 0 failed, 0 errors, 0 skipped, 22.48 s** of JUnit time. Its page matrix is the number set the 7E calibration commit must diff against:

| file | pages | reference | window | file | pages | reference | window |
|---|---|---|---|---|---|---|---|
| Sample-1.odt | 14 | 15 | 12..18 | Sample-1.docx | 15 | 15 | 12..18 |
| Sample-2.odt | 23 | 23 | 18..28 | Sample-2.docx | 25 | 23 | 18..28 |
| Sample-3.odt | 21 | 22 | 18..26 | Sample-3.docx | 25 | 22 | 18..26 |
| Sample-4.odt | 10 | 11 | 9..13 | Sample-4.docx | 11 | 10 | 8..12 |
| Sample-5.odt | 18 | 19 | 15..23 | Sample-5.docx | 19 | 18 | 15..21 |
| Sample-6.odt | 20 | 22 | 17..27 | Sample-6.docx | 24 | 21 | 15..26 |

**Finding 3.** The one alias that reaches layout today is visible in that comment. The dump line for Sample-6.odt reads `default body style: 12.0 pt (Aptos1) line factor 1.00` (`LayoutDump.kt:106-112` prints the style's `fontFamily` verbatim). So an unresolved declaration alias is already a live pagination input for that file, and the fix has a single line in the same CI output that proves the change.

**Finding 4.** The 7E exit gate is entirely unproven: no test asserts alias and direct forms converge, `Times New Roman1` never reaches the declaration table, `Aptos1` never reaches the Martel Sans stand-in through an alias, and the modified-save capability (`OdtDocumentWriter.kt:142-156`) still lists only authored indexes, named sections and source table structure.

**Finding 5.** The repair is intact. Spot-checked line counts match the pre-refactor values in audit-016 section 1.4: `AboutScreen.kt` 502, `UniversalXmlImportSheet.kt` 1178, `UniversalChartSheet.kt` 435, `PapirusOdfEngine.kt` 185, `LibreOfficeCore.kt` 155, `DocumentImages.kt` 128, `DocumentSession.kt` 72. A declaration scan over `app/src` (`package` + type name, all Kotlin files) finds **0** duplicate package-and-name pairs, so the seven redeclarations of audit-016 section 1.2 are gone. The stray `app/applet/...` path is absent. The 12 remaining `TODO` markers are plan-tagged (`TODO(plan-6)`, `TODO(plan-7)`, `TODO(plan-8)`, `TODO(plan-8B)`, `TODO(unassigned)`), not repair leftovers.

**Finding 6.** The `docs` shelf and the spec copy needed by 7E and 7F are present: `docs/odf` holds Parts 1 to 4 of ODF 1.4, `docs/lo-guides` holds the 26.2 guides. The normative sentences were re-read here, not copied from the handoff: ODF 1.4 Part 3 section 3.14 defines `<office:font-face-decls>`; section 19.502.3 is `<style:font-face>`; section 19.532 is `svg:font-family`; section 20.277 is `style:font-name`, and its clause body says a declaration referenced by name is "used directly" instead of through font matching; sections 17.7 and 17.8 are `<style:tab-stops>` and `<style:tab-stop>`; 19.489 and 19.490 are `style:leader-style` and `style:leader-text`.

---

## 2. Ledger drift the repair created

The repair shipped with audit-016 and an `AGENTS.md` toolchain note, but no plan document records the regression itself. Three concrete drifts follow:

1. **The 7E forecast slot is consumed.** `plan-2026-09-24-remaining-pr-roadmap-v2.md` section 4.7d is titled "PR #30, Plan 7E" and section 4.12 forecasts 7E at `#30`. PR #30 is the repair. From 7E onward the forecast column shifts by one (7E `#31`, 8A `#32`, 8B `#33`, Plan 9 `#34`), while plan IDs stay authoritative. `plan-01-master-index.md` section 2 row 7 carries the same `#30` and still describes 7D as a branch with a green run rather than a merge.
2. **The regression is unwritten.** No plan or audit records `cb89460`, the two failing jobs on it, or the repair commit chain `d227125` / `47097bb` / `b7c195c`. A reader of roadmap v2 alone cannot tell why 7D (`66895a7`) is not the parent of 7E.
3. **The 7F document named in the handoff does not exist.** `anti-slop/plan-7f-2026-10-04-tab-stops-and-hidden-sections.md` was created in the lost commit `c136346` and never reached a remote. Section 7 below restates its scope from evidence.

---

## 3. Prior-PR review against the ledger

| Plan | PR | Merge | Ledger claim | Tree evidence (this session) |
|---|---|---|---|---|
| 7A | #26 | `9243813` | numbering, heading/list runs, hyperlinks, bookmarks; LOKit seam | present: `data/Numbering.kt`, `OdtListNumberingTest`, `HyperlinkFidelityTest`, `HeadingRunsTest`, `OdtBookmarkTest` |
| 7B | #27 | `1c98e81` | one package/import pipeline, canonical sidecars, fail-closed save capability | present: `odf/OdtImportPipeline.kt`, `data/DocumentSemantics.kt`, `writer/OdtDocumentWriter.kt:142-156`, `Plan7bSemanticImportTest` (7 tests) |
| 7C | #28 | `ca0afaf` | authored indexes, named sections, Navigator rows, status context | present: `Plan7cAuthoredIndexTest` (7), `Plan7cNavigationStatusTest` (8), `Plan7cSectionRangeTest` (4) |
| 7D | #29 | `66895a7` | table geometry from import through hit-testing | present: `data/TableGrid.kt` (16,202 bytes), `data/TableLayout.kt` (18,395 bytes), `Plan7dTableGridTest` (4), `Plan7dTableImportTest` (2), `Plan7dTableLayoutTest` (4) |
| repair | #30 | `9356212` | revert the refactor, keep three verified-good parts | present and green; see section 1 |

The ledger is consistent with the tree except for the drift in section 2. Three conventions still shape any review here, restated because they cost time when forgotten:

1. **The only durable test log is the PR comment.** `.github/workflows/build.yml` posts `scripts/ci-dump-comment.py` output on `pull_request` events only (60,000-character cap). A push to `main` produces no mirror.
2. **Forecast slots are not reservations.** 7D took `#29` as forecast; the repair took `#30` unplanned. Numbers move, plan IDs do not.
3. **Absence of evidence is not evidence of delivery.** The 7E commits were real in their sandbox and still failed every check that matters here: a ref, a PR, a CI comment.

---

## 4. Fixture re-derivation (this session, raw package XML)

### 4.1 Font-face declarations

Every `.odt` carries exactly one `office:font-face-decls` block in `content.xml` and one in `styles.xml`, with identical sets. Face counts: **Sample-1 nine, Sample-2 ten, Sample-3 twelve, Sample-4 twelve, Sample-5 ten, Sample-6 thirteen**. That matches the counts audit-016 section 5.3 recorded, so the recovered plan's test expectations can be written straight into the fixture table. Details the reader must handle:

* Families that contain a space arrive entity-quoted (`svg:font-family="&apos;Times New Roman&apos;"`), so quote stripping has to happen after XML decoding, which `FontRegistry.key` already does (`FontRegistry.kt:199`).
* `style:font-family-generic` is `swiss`, `roman` or `system`; `system` is the value on every generated alias, so it cannot be used to classify the alias. Classification has to come from the declared family.
* `style:font-pitch` is `variable` throughout; `style:font-charset` appears only in Sample-6 (`x-symbol`, on `Aptos` and `Wingdings`).

The alias catalogue per file: Sample-1 `Aptos Display1`, `Aptos1`, `Times New Roman1`; Sample-2 adds `Noto Sans Devanagari1`; Sample-3 has no `Times New Roman1` and adds `Calibri`, `Microsoft YaHei UI`, `Segoe UI Variable` as declared families; Sample-4 adds `Arial1` and `Basic Sans1`; Sample-5 like Sample-2; Sample-6 adds `Aptos2` and declares `Courier New` and `Wingdings`.

### 4.2 Which aliases reach layout (the number that bounds 7E's page movement)

A scan of every `style:font-name`, `style:font-name-asian`, `style:font-name-complex` and `fo:font-family` value for the seven aliases shows:

* **Sample-6.odt is the only fixture that references an alias in the latin `style:font-name` slot** (`styles.xml`, `Aptos1`, twice: the paragraph default style and `Standard`).
* Every other alias reference in all six files is in `font-name-asian` or `font-name-complex`, or in `fo:font-family` (Sample-1's `content.xml` has one `font-name-complex="Times New Roman1"`). The import path reads `attrs["font-name"] ?: attrs["font-family"]` (`SvXMLImport.kt:434` and `:261`) and never the asian or complex slots, so those references do not reach `ParagraphStyle.fontFamily`.

**Static prediction for the calibration commit:** resolving aliases through the declaration table can move **Sample-6.odt only**. All six DOCX files have no declaration table at all (their `word/styles.xml` names real families; theme indirection is Plan 8A), so **no DOCX count may move in 7E**. Any other movement is a signal to investigate, not to accept.

The direction of Sample-6's movement is also predictable in kind, though not in size: today `Aptos1` falls through `FontRegistry.classify` to `GenericFamily.DEFAULT` (serif advance table, natural line height 1.15 em). After 7E it resolves to `Aptos`, which is a `BUNDLED_STAND_IN` on Martel Sans with `GenericFamily.SANS_SERIF` (advances 1.08 times the serif table, natural line height 1.117 em, `FontRegistry.kt:120-123`, `TextMetrics.kt:73-85`). Text gets wider and lines get shorter, so the count can move either way and the PR body must report the measured value rather than a forecast.

### 4.3 Tab stops and leaders

| file | `<text:tab>` in body | `<style:tab-stops>` blocks | `<style:tab-stop>` | leader-style / leader-text |
|---|---|---|---|---|
| Sample-1.odt | 0 | 2 | 2 | 0 / 0 |
| Sample-2.odt | 49 | 7 | 7 | 2 / 2 |
| Sample-3.odt | 0 | 3 | 4 | 0 / 0 |
| Sample-4.odt | 35 | 5 | 7 | 2 / 2 |
| Sample-5.odt | 43 | 4 | 4 | 2 / 2 |
| Sample-6.odt | 167 | 8 | 10 | 3 / 3 |

Positions and alignments are already load-bearing. `SvXMLImport` parses `style:tab-stops` and `style:tab-stop` into `ParagraphStyle.tabStops` including `right`, `center` and `char` (`SvXMLImport.kt:739-748`), the model carries them (`OfficeDocument.kt:345-346`), and `ParagraphMeasurer.tabAdvance` walks the sorted stops, applies RIGHT/CENTER/DECIMAL offsets, honours `CLEAR`, and falls back to `defaultTabIntervalUnits` (`ParagraphMeasurer.kt:59-95`, consumed at `:92` and `:120`). **What is missing is the leader:** `ParagraphTabStop` has no leader field, `style:leader-style` and `style:leader-text` are not read anywhere, and nothing paints dots.

### 4.4 Hidden sections

All six `.odt` fixtures contain **zero** `text:display="none"` attributes and **zero** `text:section` body elements. The only matches for "section" are `style:family="section"` declarations (Sample-2/4/5/6) and one body word ("cross-section") in Sample-3. Plan 7C's section suite is therefore synthetic, and excluding hidden ranges from layout **cannot move the fixture matrix**; it is a correctness item with synthetic tests only. That is the opposite of item 5, whose two leader-bearing samples (2 and 6) have 49 and 167 body tabs.

---

## 5. The post-unit-test leftovers, as far as the tree shows

The episode the owner describes is audit-016's subject: a Google AI Studio session ran the unit tests after Plan 7D, then committed `cb89460` to `main`, which rewrote `build.gradle.kts`, replaced whole screens with "not available in this build" stubs, gutted the LOKit probe, and introduced seven same-package redeclarations plus four missing `BuildConfig` fields. Both CI jobs failed on that commit (run `37208441343`), and the unit-test artifact shrank from 187,264 bytes to 5,224 bytes, which is a compile failure rather than failing assertions.

PR #30 repaired it. What is still detectable in the tree, in priority order:

1. **Plan 7E itself, unshipped.** This is the largest outstanding item and the one that blocks Plan 8A.
2. **Two test classes named `SampleFilesCompatibilityTest`** in different packages (`com.example`, 138 lines; `com.makerandreas.papirusoffice.data`, 131 lines), testing overlapping surface. Not a compile error and not a test-count problem, but two class names that differ only by package make CI output ambiguous.
3. **`UndoManager.kt` appears three times** (`data/` typealias facade, `data/undo/`, `data/writer/`, the last being a different concern, `SwUndo` records). Checked and intentional; listed so no later session "fixes" it.
4. **Ledger drift** (section 2), which is documentation, not code.
5. **Unverified visual restorations**: the repair returned `ui/theme/Type.kt`'s `google_sans_code_regular` mapping and `ic_pagella_logo.xml`'s path data to their approved values. audit-016 recorded both as unverified visual changes in the refactor; the restore is correct by precedent but has no device evidence, and `AGENTS.md` already records that the bundled Google Sans files have malformed name tables. That belongs to Plan 11, not 7E or 7F.

**Open question for the owner:** if the "crap" you saw is something else (for example files or behaviour produced by the AI Studio test run on your machine rather than committed to `main`), name it and it gets its own line here, because nothing matching that description is present in this checkout. The sweep above is exhaustive for tracked content: `git ls-files` shows no build outputs, no scratch directories and no AI Studio export artifacts, and `.gitignore` covers the usual local ones.

---

## 6. Plan 7E, restated as a four-commit completion plan

Rebuilt from roadmap v2 section 4.7d, the 7E exit gate in the handoff, and the seams verified above. Nothing in this section is implemented yet.

| Commit | Content | Files | Tests |
|---|---|---|---|
| 1 | Read `<office:font-face-decls>` from both parts into `DocumentStyles.fontFaces`, first-wins on duplicate alias, skip declarations with a blank name or blank family, and keep `hasFontFaceDeclarations` consistent with what was actually read | `data/odf/SvXMLImport.kt` (`parseOdfStyles`, `toDocumentStyles`), `data/odf/OdfXmlToken.kt` if tokens are used | `Plan7eFontFaceImportTest`: per-fixture inventories (9/10/12/12/10/13), duplicate-alias precedence, blank-family and one-part cases |
| 2 | `FontFaceResolver.familyFor(name, fontFaces)`: first family of `svg:font-family`, quotes and whitespace trimmed, comma lists split, missing or self-referential declarations fall back to the raw name; wire it into the two text-properties readers so the resolved family lands in `ParagraphStyle.fontFamily` (and the list-label path), leaving `TextMetrics.forStyle` and `OfficeRuns.fontFamilyFor` on one input | `data/odf/SvXMLImport.kt:246-265`, `:411-438`, new resolver file | `Plan7eFontResolutionTest`: alias and direct form produce equal `FontChoice`; `Times New Roman1` and `Times New Roman` both reach `Liberation Serif` with `BUNDLED_METRIC_COMPATIBLE`, `metricCompatible = true`, `assetStem = "LiberationSerif"`; `Aptos1`, `Aptos2`, `AptosDisplay1` reach `Martel Sans` with `BUNDLED_STAND_IN`, `metricCompatible = false`; unresolved names still classify as today; `FontChoice.composeFamily` mapping unchanged so no bundled file is claimed as painted |
| 3 | ~~Add the font-face arm to `OdtDocumentWriter.saveCapability` (decision D3)~~ **Dropped 2026-10-04:** the owner deferred D3 to Plan 9, which owns writer regeneration; the loss (a regenerated `content.xml` drops the declaration table) is recorded in section 12 instead of blocked | ~~`data/writer/OdtDocumentWriter.kt:142-156`~~ untouched | ~~`Plan7eSaveCapabilityTest`~~ not written |
| 4 | Calibration and records: re-run the dump and `PaginationFidelityTest`, paste the twelve counts next to their windows and the pre-7E set in section 1, explain every movement, and update `plan-01` row 7, roadmap v2 (sections 4.7d header, 4.12 table, and a repair record), plan-04's Plan 7 record, `PROJECT_CONTEXT.md` where it names the engine's font handling, and this audit | docs only | the CI comment from the PR head is the evidence |

**Exit gates and where they are evidenced (unchanged from the brief):** alias and direct forms converge (commit 2 test); `Times New Roman1` reaches a bundled metric-compatible face (commit 2 test plus `assetStem`); `Aptos1` reaches the stand-in and stays non-metric-compatible (commit 2 test); one resolved family string feeds measurement and display (the wiring, asserted by the same test through both entry points); all twelve fixtures inside their windows, none widened (commit 4, diffed against section 1); no claim that bundled files are painted before Plan 10 (the `composeFamily` test plus the absence of any asset-loading change in the diff).

**D3 is a re-decision, not a carry-over.** The lost commit `07bdb85` implemented it; this session has no evidence of that code, so the owner should confirm the decision stands.

**Rebuild risks, stated up front.** The first CI run is again the first compile. The seams most likely to surprise are the `OfficeFontFace` constructor names, `DocumentStyles.defaultParagraphStyle` versus `paragraphStyles`, `FontChoice.assetStem` and `composeFamily`, `FontSource.SYSTEM_GENERIC`, `OfficeRuns.fontFamilyFor`, the `saveCapability` field names, and `OdtSourceFeatures.hasFontFaceDeclarations`. All were re-checked against the tree while writing this audit and are quoted here with the names the tree actually uses.

---

## 7. Plan 7F, scoped from evidence

The handoff names 7F as tab stops/leaders plus hidden-section layout, and PR #30's body records the owner approving that split. The fixture work in section 4 sharpens it:

* **Item 5, leaders.** Parse `style:leader-style` (ODF 1.4 Part 3 section 19.489) and `style:leader-text` (19.490) into `ParagraphTabStop` through the paragraph style cascade, then paint the leader character in the gap that `ParagraphMeasurer.tabAdvance` already resolves. Positions, alignment and `CLEAR` are done; the leader is not. Consumers: the TOC entries in Samples 2, 4, 5 and 6 (2, 2, 2 and 3 leader-bearing stops respectively).
* **Item 6, hidden sections.** Exclude `SectionDisplay.HIDDEN` ranges (and their nested content) from layout so `Paginator` stops placing text that the Navigator deliberately greys out. Synthetic tests only: no fixture contains one.
* **Not in 7F:** numbering, fields, tables, save regeneration, DOCX parity (Plans 8A/8B), any bundled-typeface painting (Plan 10 A1). 7F is expected to be **matrix-neutral**: leader glyphs are painted inside an already reserved advance, and no fixture has a hidden section. If a count moves in 7F, that is a defect to explain, not a calibration.

---

## 8. Strategy options

| Option | Contents | Verdict |
|---|---|---|
| **A. Two PRs, 7E then 7F (recommended)** | 7E as section 6 (`#31`), 7F as section 7 (`#32`), each with its own CI comment and page-matrix diff | Keeps attribution exact. 7E may move one ODT count for a stated reason; 7F should move none. Plan 8A can start the moment 7E merges, as the roadmap orders it, without waiting for 7F |
| B. One combined 7E + 7F PR | Both sections in one branch | Rejected: two different kinds of change (resolution versus layout) land under one page-matrix diff, which is exactly the ambiguity audit-016 section 5.2 removed when the owner approved the split |
| C. 7F only | Treat 7E as delivered elsewhere and skip it | Rejected: no ref, no PR, no comment, no commit contains it (section 1). The exit gate and D3 would be permanently unproven, and 8A would consume an unresolved font identity |
| D. Fold 7E's font work into 7F | One PR, fonts first then leaders, with the matrix explained in two labelled steps | Acceptable fallback only if the owner prefers fewer PRs; the cost is that the 7E exit gate and the 7F neutrality claim share one comment, and the ledger's four-plan split needs amending rather than using |

Recommendation: **A**, with a short ledger-repair commit inside 7E commit 4 (or a separate docs PR if the owner prefers) that records PR #30, shifts the forecast from 7E onward by one, and corrects `plan-01` row 7.

---

## 9. Owner decisions

1. **D1, 7E recovery.** Rebuild the four commits here from section 6 (recommended), or does the AI Studio session still hold the code and should it be pushed for verification first?
2. **D2, the split.** Keep 7E and 7F as two PRs (recommended), or take option D?
3. **D3, save capability.** Confirm that a modified ODT declaring font faces must be refused (`"font-face declarations"`). No fixture behaviour changes either way, because all six already refuse through tables or indexes.
4. **D4, the leftovers.** Confirm that section 5 covers what you meant, or name the specific junk; the duplicate `SampleFilesCompatibilityTest` pair is the only naming candidate in the tree.
5. **D5, ledger repair.** Fold the PR #30 record and the forecast shift into 7E commit 4, or take it as its own small documentation PR first?

**Resolved 2026-10-04.** D1: rebuild here (section 8 option A), which is what happened. D2: two PRs, 7E then 7F. D3: deferred to Plan 9 (see section 12). D4: the section 5 sweep stands; the duplicate `SampleFilesCompatibilityTest` simple name is the only naming candidate in the tree and no code change is proposed for it, so the owner should name any other junk they saw on the AI Studio machine. D5: the PR #30 record and the forecast shift are folded into the 7E records commit.

---

## 10. Delivery Gate (documentation-only deliverable)

Reduced gate per `AGENTS.md` addendum item 3.

* **R-02 PASS:** no em dash in this document; a `grep` for the em dash code point (U+2014) over the file returns 0 lines.
* **R-15 PASS:** no call to action; this is an internal analysis record.
* **R-16 PASS:** no marketing vocabulary; headings name the artifact (audit, plan, gate).
* **R-17 PASS:** every number traces to a named source: git/`gh` refs and IDs in section 1; run IDs `37211551672`, `37209964838`, `37208441343` and comment `5981186452`; test and suite counts from the PR #30 comment; page counts from the same comment's matrix; fixture counts re-derived in section 4 from `tests/inky/*.odt` with the stated method; file:line citations for every seam claim. Section 12 adds run `37213135047` and its PR comment, naming `784e9d3` and `c363ce3` as the delivered commits and `28a37fb` as the pre-fix head that run measured. Section 13 adds the owner-supplied diff file (`docs/01a10750-1f33-7425-b811-fda994620228.txt`, commit `0311c4a`), its `coding-numstat.txt`, and runs `37213815272` and `37215310814`.
* **R-36 PASS:** no capability, performance or compliance claim is asserted. Nothing was compiled or run locally; section 4.2 is labelled a static prediction, and section 5 states what could not be found rather than claiming a clean bill of health. The compile and test numbers added in section 12 come from the CI run named there, not from this sandbox, which still has no JDK.
* **R-38 PASS:** no placeholder content is presented as real; the 7E plan and 7F scope are labelled as not implemented.
* **C-5 PASS:** the claim that 7E is unshipped rests on three independent checks (local refs, remote branch head, absence of code), and the claim that the repair is intact rests on line counts and a declaration scan.
* **R-26, R-27, R-32, R-34, R-35 N/A:** no UI shipped in this deliverable.
---

## 11. antislop and toolchain note

`skills/antislop/VERSION` is `3.2.20`. The repository preference file `.config/antislop/settings.json` carries `{"mode":"during"}` (owner decision 2026-10-04, audit-015 section 8). The platform path `~/.config/antislop/settings.json` was absent in this fresh sandbox, so this session provisioned it from the repository copy, as audit-016 section 8 did, and ran in DURING mode. No image generation or asset work was involved, so R-23 does not apply.

Local build remains impossible by the same checks `AGENTS.md` records: no `java`, `javac` or `/usr/lib/jvm`, unreachable Debian and JDK vendor hosts, unreachable Gradle/Maven hosts, no `~/.gradle` cache. CI is the compile and test evidence path, and this session produced no build claim.

---

## 12. Implementation record (added after the rebuild, 2026-10-04)

Delivered on `arena/01a1077a-papirus-office` as PR #31, the slot the section 2 shift predicted. Three commits:

| Commit | Content | Evidence |
|---|---|---|
| `784e9d3` | Declarations reader plus `Plan7eFontFaceImportTest` (4 tests): both XML parts, per-fixture inventories, first-wins precedence, blank declarations skipped | CI run `37213135047`: suite green |
| `c363ce3` | `FontFaceResolver` plus one `resolveFontFamily` helper in both text-properties readers, so the resolved family is the single input to `TextMetrics.forStyle` and `OfficeRuns.fontFamilyFor`; `Plan7eFontResolutionTest` (8 tests) | same run: 7 green, 1 expectation failure |
| `6585327` | Alignment with the recovered original diff (section 13): `familyFor` answers the reference itself when no declaration is usable, the list form resolves through the same call, `fontFaces` stores unquoted families, and the original test corpus is merged in | run `37215310814`: 348 tests, 0 failures, `Plan7eFontFaceImportTest` 7/7, `Plan7eFontResolutionTest` 11/11 |
| records | Roadmap v2.8, `plan-01`, plan-04, `plan-5e-progress.md`, `PROJECT_CONTEXT.md`, the section 6 correction, this section and the new Plan 7F document | this file |

**First compile.** Run `37213135047` (PR merge ref `3914079`, measuring the pre-fix head `28a37fb`) built green and ran **342 tests across 63 suites with 1 failure, 0 errors, 0 skipped, 32.64 s** of JUnit time against the 330-test baseline. The failure was this session's own expectation in `aliasAndDirectNameProduceTheSameFontChoice` (`Plan7eFontResolutionTest.kt:74`): `FontChoice.requested` is the resolved family after `FontFaceResolver` runs, so the alias string only survives on the raw registry path. The test now asserts both, and the remaining 11 new tests passed unmodified, including every fixture inventory and every alias decision.

**Final tree.** After the alignment commit the suites hold 7 and 11 tests respectively, 348 in total across 63 suites on run `37215310814`, with the same matrix and dump line as the intermediate run. Section 13 records the comparison that produced the alignment and confirms that no fixture declares a family list, which is why the contract change could not move a page count.

**Calibration, measured, not predicted.** The page matrix in the new comment is identical to the PR #30 baseline in every cell: ODT `14/23/21/10/18/20` and DOCX `15/25/25/11/19/24`, with the same reference, window, thin-page and empty-page columns. The dump changed exactly where section 4 required it to be observable: Sample-6.odt's body style prints `12.0 pt (Aptos) line factor 1.00` where the baseline printed `(Aptos1)`, and the string `Aptos1` occurs **0** times in the new comment against **1** time in the baseline comment. Resolution reaches layout, and the count did not move, so the earlier static prediction that the two advance tables do not cross a page boundary in this file is confirmed rather than overridden.

**Decisions and remaining losses.** D3 is deferred to Plan 9; until writer regeneration exists, a modified save of an ODT that declares font faces would drop `office:font-face-decls`, and no writer path is touched by this PR. Items 5 and 6 move to `plan-7f-2026-10-04-tab-stops-and-hidden-sections.md`, whose scope section states the verified missing pieces (leader parsing and painting; no layout consumer of `SectionDisplay.HIDDEN`) and the synthetic-only test situation. The exit gate of section 6 is met on the evidence above, except the deliberately deferred save arm.

---

## 13. The recovered original diff, compared (added 2026-10-04)

After this branch had already been rebuilt and verified, the owner supplied the previous session's diff from their Google AI Studio machine. It is stored in the repository as `docs/01a10750-1f33-7425-b811-fda994620228.txt` (271,182 bytes, 32 files) and was pushed by the owner as commit `0311c4a`, on top of the records commit. This section records what it contains, how it compares with the rebuild, and what was adopted.

### 13.1 What the file is

* It is the whole previous session, not only Plan 7E: the PR #30 repair (the seven restored screen/engine files, `build.gradle.kts`, `gradle.properties`, `libs.versions.toml`, the `Type.kt` and logo restorations) plus the 7E change set.
* The 7E half matches the handoff's four-commit story: the declaration reader, `FontFaceResolver`, the save-capability arm, the records. That is the strongest available confirmation of the handoff description; a plain diff carries no commit identifiers, so the four SHAs it named remain unverified as object ids, and only the branch content is now recoverable.
* The session's own `coding-numstat.txt` (12 files: the 7E docs, `FontFaceResolver`, `SvXMLImport`, and the two tests) shows the state after the owner reversed decision D3: no `OdtDocumentWriter` and no `Plan7eSaveCapabilityTest`. The diff file itself predates that reversal and still carries both. This branch follows the numstat: D3 stays deferred to Plan 9, and no writer path is touched.
* The repair half is restoration, not new work: it puts back the full `UniversalXmlImportSheet`, `UniversalChartSheet`, `AboutScreen`, `DocxEmbeddedImage`, `DocumentImages`, `DocumentSession`, `PapirusOdfEngine`, `OfficeDocumentParser`, `SelectionEngine`, `SwNodes`, the LOKit probe, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `Type.kt` and the Pagella logo that the `cb89460` refactor had replaced or gutted. Nothing in the diff introduces a new screen, module or artifact, which answers the section 5 open question for this file: the "crap" the owner saw is the refactor already repaired by PR #30, and no separate junk is present here.

### 13.2 The two places where the original was stronger, and were adopted

| Item | Rebuild before this section | Original | Adopted |
|---|---|---|---|
| `familyFor` on an unresolvable name | returned null; each caller kept the raw string | answered the reference itself, null only for a blank reference | yes (`familyFor` is total, and the direct `fo:font-family` list form now resolves through the same call as an alias) |
| A reference that is itself a family list (`fo:font-family="'Times New Roman', serif"`) | fell back to the raw string with the list tail attached | first family of the list | yes |
| `DocumentStyles.fontFaces` values | the raw `svg:font-family` attribute, quotes included | the first family, unquoted | yes |
| `firstFamily` on a list whose first entry is blank (`" , 'Liberation Serif'"`) | skipped the blank entry | answered null | no: skipping is strictly more forgiving, no fixture contains a list, and the test asserting it is kept |
| Save refusal for declared font faces (D3) | absent | present | no: the owner deferred it to Plan 9 on 2026-10-04, and the original session's own numstat reverts it |

The alignment is commit `6585327` ("align the resolver contract with the recovered original"). It also merges in the original's test corpus, which is stronger than the rebuild's in two ways: it checks every declaration of every fixture against the family it answers, and it proves with a synthetic package that a `style:font-name` alias and a `fo:font-family` list reach one family and one `FontChoice`. Both are now in `Plan7eFontResolutionTest` (11 tests) and `Plan7eFontFaceImportTest` (7 tests).

### 13.3 The original's fixture claims, re-verified here

Every corpus assertion in the recovered tests was re-derived from the six ODT ZIPs before adoption, and all of them hold:

* Declaration counts 9/10/12/12/10/13, identical in `content.xml` and `styles.xml` in all six files.
* `Aptos1` in all six; `Aptos2` only in Sample-6; `Aptos Display1` in 1/2/3/5/6; `Times New Roman1` in 1/2/4/5/6; `Noto Sans Devanagari1` in 2/3/4/5/6; `Arial1` and `Basic Sans1` only in Sample-4.
* `style:font-charset` appears only in Sample-6 (`x-symbol`), `style:font-pitch` is `variable` throughout, and every alias declares `style:font-family-generic="system"`.
* No fixture declares a comma-separated `svg:font-family` or `fo:font-family`, so the list handling affects no fixture page count. That is why the alignment commit cannot move the matrix, and the run confirms it.

### 13.4 One claim in the recovered draft that is wrong

The recovered Plan 7F document says Sample-6 "declares the only `text:display` range" and expects pagination to move there. That is false: a scan of all XML parts of all six `.odt` files finds **zero** `text:display` attributes and **zero** `<text:section>` body elements. The 7F document in this branch states the verified position (0 occurrences, synthetic tests only) and carries a note so the withdrawn expectation is not reintroduced. Its other content is good and was folded into that document: the scheduling freedom relative to Plan 8A, the non-goals, and the DOCX `w:tabs` boundary.

### 13.5 Verification of the alignment

CI run `37215310814` at commit `6585327` (PR merge ref for that tree): **348 tests across 63 suites, 0 failures, 0 errors, 0 skipped, 29.98 s** of JUnit time, Build job green, `Plan7eFontFaceImportTest` 7/7 and `Plan7eFontResolutionTest` 11/11. The twelve-file page matrix is unchanged from every earlier run in this PR (ODT `14/23/21/10/18/20`, DOCX `15/25/25/11/19/24`), and the dump still prints `(Aptos)` with zero `Aptos1` occurrences.

---
