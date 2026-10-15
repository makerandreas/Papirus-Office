# Plan 9: save round-trip integrity (was PR I)

**Status:** planned 2026-10-15. This is the document `plan-2026-09-24-remaining-pr-roadmap-v2.md`
§4.10 requires before the PR starts ("scope is firmed by a short plan document before this PR
starts; do not start it from this paragraph alone"). No PR slot is claimed: plan IDs are
authoritative and PR numbers are forecasts.
**Parent:** `plan-04-to-09-writer-fidelity.md` § Plan 9, including the 2026-10-02 ClearPDF
design note (MIT License) for non-destructive ZIP preservation and append-only style derivation.
**Closes:** O-01; the "non-destructive package preservation" rule; `plan-03-compliance-sweep.md`
items 3.13, 3.19, 3.20, 3.26, 3.27; and F-2 (retiring the `OfficeDocElement` wrapper).
**Depends on:** Plans 6, 7 and 8, all landed. Plan 7B supplies the fail-closed capability
boundary; Plan 9 owns structural regeneration.
**Evidence:** every claim below was measured in the working tree at `main` `53d61ac` on
2026-10-15. File and line numbers are that tree's. Re-check them before using this document,
because the 2026-10-12 re-upload moved code without moving the prose that cites it
(`audit-025` §6).

---

## 0. Why this plan exists, measured rather than asserted

`plan-01-master-index.md` §3.2 says "the written content loses structure (dangling style refs on
ODT, `[Image: path]` on DOCX)". Both halves need an update, because the image half was fixed by
Plan 6 and the ODT half is worse than the sentence says.

### 0.1 The ODT writer

`OdtDocumentWriter.write` (`data/writer/OdtDocumentWriter.kt:31`) has two modes.

**Preserved mode** (`:80`, when `odtPackageData.entries` is non-empty) copies every zip entry
except `content.xml` straight through (`:85` to `:88`), and writes the byte-exact original
`content.xml` when the document is unmodified (`:39`, `:50`). This is already non-destructive in
the sense the rule asks for, and it is the behaviour Plan 7B and Plan 6C built.

**Fresh-generation mode** (`:101`) writes `META-INF/manifest.xml`, `styles.xml`, `meta.xml` and
`settings.xml` from hardcoded triple-quoted templates (`:379`, `:328`, `:354`, `:369`). All four
declare `office:version="1.2"` (`:331`, `:359`, `:372`). `buildStylesXml` emits a `Standard`
style, a `Body_20_Text` style, a `Heading_1` at a fixed 18 pt, and a `style:default-style` pinned
to `Liberation Serif, Times New Roman` at 12 pt. A document saved this way loses every style it
declared.

**The modified path is the defect.** When `isModified` is true, `buildContentXml` (`:183`)
regenerates `content.xml`. It writes a fixed `<office:automatic-styles>` block of eight text
styles and three table styles (`:196` to `:208`), then walks the body. What it drops, read from
`writeElement` (`:223` to `:282`):

| Dropped | Where you can see it |
|---|---|
| **Dangling style references.** A paragraph is written with `text:style-name="{element.styleName}"` (`:226`), but the only automatic styles in the file are the fixed eleven. A paragraph whose style is `P37` comes back referencing a style that does not exist, and LibreOffice falls back to Default | `:226`, against `:196`-`:208` |
| **List grouping.** Every `OfficeListItem` becomes its own `<text:list><text:list-item>` (`:238`). A five-item list is written as five one-item lists, so numbering restarts at 1 on every item, and `text:list-style-name` is never emitted, so the list's own numbering style is lost | `:238` to `:240` |
| **Sections.** `OfficeSection` is not a branch in `writeElement`; it falls through to `else -> { }` at `:280` and disappears | `:280` |
| **Cell spans and cell styles.** `writeElement` emits `<table:table-cell office:value-type="string">` with no `table:number-columns-spanned`, `table:number-rows-spanned` or `table:style-name` | `:242` to `:263` |
| **Bookmarks, hyperlinks, footnotes, fields, page breaks.** No branch writes them | `:223` to `:282` |
| **Headers and footers.** `OfficeDocument.header` and `.footer` (`OfficeDocument.kt:25`, `:26`) are never read by the writer | `OfficeDocument.kt:25`-`:26` |
| **Font face declarations.** No `<office:font-face-decls>` is written. This is decision D3, reversed by the owner on 2026-10-04 and deferred here | `plan-04-to-09` § 7E record |
| **`office:version`** is `1.2`, where ODF 1.4 Part 3 §19.390 and the Plan 9 design note ask for `1.4` | `:194` |

`saveCapability` (`:142`) refuses a **modified** save for exactly three things: authored indexes,
named sections, and source table structure. It does not refuse for lost list numbering, lost cell
spans, lost bookmarks or dangling style references, because those are not detected. Every one of
those is silent data loss today.

### 0.2 The DOCX writer

There is no DOCX writer. `DocxDocumentParser.saveDocument` (`DocxDocumentParser.kt:54`) does three
things:

1. Refuses when the document contains embedded images (`:56` to `:62`). This is the Plan 6
   fail-closed guard and it is correct; Plan 9 replaces the refusal with a real image path.
2. **Throws away every run.** The element mapping at `:64` to `:74` builds
   `OfficeDocumentElement.Paragraph(text = element.paragraph.text)` with no `runs`,
   `Heading(text, level)` with no runs and no `styleName`, and `ListItem(text, bullet)` with no
   runs. Every bold, italic, colour, size and font on those runs is gone before a byte is written.
   The `OfficeTextRun` tri-state model that Plan 8A built never reaches the save path.
3. Writes `saveDocxZip(file, generateDocxXmlFromElements(document.body.elements))` (`:104`), a
   from-scratch `word/document.xml` with no `styles.xml`, no `numbering.xml`, no
   `word/_rels/document.xml.rels` entry for anything, and no images.

`plan-01` §3.2's "[Image: path] on DOCX" is no longer accurate: Plan 6D removed that plumbing and
replaced it with the refusal above. The current defect is quieter and larger: a DOCX save keeps
the words and drops the formatting.

### 0.3 What the round trip must prove

Roadmap §4.10: *open, save, reopen preserves text, styles, numbering, tables, images and TOC for
Sample-6 in both formats, verified by a CI round-trip test.* Measured against §0.1 and §0.2, that
acceptance line is reachable for ODT with the work in §2.1, and is a much larger job for DOCX
(§2.2). This plan says so up front rather than discovering it at the gate.

---

## 1. What already exists. Do not rebuild it

Five things are done and must not be re-done or duplicated:

1. **Non-destructive package preservation, ODT.** `OdtDocumentWriter:80`-`:88` copies untouched
   entries through byte-for-byte. `OdtPackageData` (`OfficeDocument.kt:8`) already carries
   `originalStylesXml`, `originalManifestXml`, `originalMetaXml`, `originalSettingsXml` and the
   raw `entries` map, so the writer does not have to re-read the package.
2. **The fail-closed boundary.** `saveCapability` (`:142`) and
   `rejectUnsupportedStructuralRewrite` (`:158`), with `Plan7bSemanticImportTest` covering it.
3. **`mimetype` first and STORED**, per ODF 1.4 Part 2. `OdtDocumentWriter:64`-`:71`.
4. **Image embedding.** `collectImages` and the `ImageEmbed` path, with
   `validatePreservedPackageImages` (`:395`) refusing to reuse an original `content.xml` whose
   image entries are missing. Covered by `Plan6ImageFoundationTest:167`.
5. **The fail-safe image refusal on DOCX.** `DocxDocumentParser:56`. Removed only when §2.2's
   image path lands and the round-trip test proves it.

---

## 2. Scope

### 2.1 ODT: make the regenerated `content.xml` structurally faithful

Ordered by how much data each item recovers.

1. **Emit the document's own automatic styles.** Walk `OfficeDocument.styles`
   (`DocumentStyles`) and emit `<style:style>` entries for every named and automatic style the
   body references, before the fixed eleven. This is what closes the dangling-reference defect.
   **Collision rule: decision D4 in §8** (the document owns its names; the generator steps aside).
   The rule this paragraph originally proposed was reversed after measurement — see D4.
2. **Append-only derivation for formatting with no style.** Names follow the ClearPDF design note:
   `P_papirus_*` for paragraph styles and `T_papirus_*` for text styles, appended to the tail of
   `<office:automatic-styles>`, never re-indexed.
3. **Group list items into one `<text:list>` per run of consecutive items at the same level**, and
   carry `text:list-style-name` from the parsed list style. Sample-6 ODT carries 23
   `text:list` elements (`plan-04-to-09` Appendix A matrix), so the fixture can check this
   directly.
4. **Write cell spans and cell styles**: `table:number-columns-spanned`,
   `table:number-rows-spanned`, `table:style-name`, and skip `OfficeCellOccupancy` entries that
   are not `ORIGIN` rather than writing them as empty cells.
5. **Emit `<office:font-face-decls>`** from the parsed declarations. This is decision D3, deferred
   here by the owner on 2026-10-04.
6. **Bump `office:version` to `1.4`** on all four generated roots (`:194`, `:331`, `:359`, `:372`).
   Citation: ODF 1.4 Part 3 §19.390, checked in at `docs/odf/OpenDocument-v1.4-part3-schema.html`.
7. **Write bookmarks and hyperlinks.** `text:bookmark` / `text:bookmark-start` / `-end` and
   `text:a` with `xlink:href`. Plan 7A parses all of them.
8. **Write sections** (`OfficeSection`) as `text:section`, recovering the `else -> { }` fallthrough
   at `:280`.

**Still deferred after this plan** (say so in the PR body rather than leaving it implied): TOC and
authored-index regeneration, footnotes and endnotes, fields, headers and footers, table-of-*
`text:section` protection, and Change Tracking. `saveCapability` keeps refusing a modified save
for authored indexes, and the round-trip test asserts the refusal rather than the content.

### 2.2 DOCX: a real writer, and it is the larger half — **DEFERRED to Plan 9b (decision D5)**

The owner deferred this section on 2026-10-15, so Plan 9 ships the ODT half only and §0.2's
defects stand until Plan 9b. Everything below this paragraph is Plan 9b's scope, carried here so
the measurement is not lost. Plan 9b needs its own plan document before it starts, for the same
reason this one did.

Add `DocxDocumentWriter : DocumentFormatWriter` next to `OdtDocumentWriter`, and make
`DocxDocumentSerializer.write` call it instead of `DocxDocumentParser.saveDocument`.

1. **Preserve the package.** Copy every entry from the original DOCX except the ones being
   regenerated: `word/styles.xml`, `word/numbering.xml`, `word/theme/*`, `word/fontTable.xml`,
   `docProps/*` and `word/media/*` go through untouched. Only `word/document.xml`,
   `[Content_Types].xml` and `word/_rels/document.xml.rels` are rewritten.
2. **Stop dropping runs.** This is the single highest-value change in the plan. Carry
   `OfficeTextRun` through to `w:r` with `w:rPr`, including the tri-state "subtract" behaviour Plan
   8A implemented and `DocxRunFormattingTest` covers.
3. **Style linkage.** Heading and character style pairs per `plan-04-to-09` §2.3, with `w:link`
   and the `X` / `X Char` convention. `word/styles.xml` is preserved from the source, so generated
   styles must be appended, never re-indexed.
4. **`w:tblGrid`** on every regenerated table, plus `w:gridSpan` and `w:vMerge` for cell spans.
5. **Numbering.** `w:numPr` references into the preserved `numbering.xml`; assert every `numId`
   written resolves.
6. **Images.** Write `word/media/*`, register each in `[Content_Types].xml` and
   `word/_rels/document.xml.rels`, then remove the refusal at `DocxDocumentParser:56`.
7. **Then and only then** delete the `generateDocxXmlFromElements` path.

Reference: `docs/ooxml/[MS-DOCX].pdf`, page-indexed in `docs/ooxml/README.md`; ECMA-376 Part 1.

### 2.3 The round-trip test

New file: `app/src/test/java/com/example/Plan9RoundTripTest.kt`.

The shape, one method per format per sample:

```
read tests/inky/Sample-N.odt  ->  OfficeDocument
write to a temp file in context.cacheDir   (never over a fixture)
re-read the temp file          ->  OfficeDocument
assert the two agree on the properties in the table below
delete the temp file
```

**Fixture rule (cross-plan invariant 6 applies).** A round-trip test writes files, so it must
write to `context.cacheDir` and clean up in a `finally`. It must never write into `tests/inky`,
and it must not touch `tests/inky/fixture-identities.properties`. `FixtureIdentityTest` is the
guard that catches a mistake here; if it fires, the test is wrong, not the manifest.

### 2.3.1 No fixture can reach the generated `content.xml` path

**Measured, and it changes the test design.** `saveCapability`
(`OdtDocumentWriter.kt:142`) refuses a **modified** save when
`OdtSourceFeatures.hasAuthoredIndexes`, `.hasNamedSections` or `.hasAdvancedTables` is set. Those
three flags come from `detectSourceFeatures` (`OdtImportPipeline.kt:231`), which scans
`content.xml` and `styles.xml` and sets:

* `indexes` on any `table-of-content`, `alphabetical-index`, `table-index`, `illustration-index`,
  `object-index`, `user-index` or `bibliography` element — note **any**, not "a complex one";
* `sections` on any `section` element;
* `tables` on any `table` element — again **any** `<table:table>`, however simple.

Counting those elements in the six fixtures:

| fixture | `<text:table-of-content>` | `<table:table>` | refused for a modified save |
|---|---|---|---|
| Sample-1.odt | 0 | 85 | yes — source table structure |
| Sample-2.odt | 1 | 86 | yes — both |
| Sample-3.odt | 0 | 55 | yes — source table structure |
| Sample-4.odt | 1 | 0 | yes — authored indexes |
| Sample-5.odt | 1 | 0 | yes — authored indexes |
| Sample-6.odt | 1 | 43 | yes — both |

**All six are refused, so `buildContentXml` — the function §2.1 rewrites — is unreachable through
any fixture.** (The flag is the coarse one here; the finer element walk at
`OdtDocumentWriter.kt:164-179` would also refuse Sample-1 and Sample-3, whose tables carry cell,
row and column styles and header rows.)

Three consequences, all of them shaping this plan:

1. **The roadmap's Sample-6 acceptance is reachable only through the preserved path**, the one
   that copies the original `content.xml` byte-for-byte. That path already works. So "open, save,
   reopen preserves … for Sample-6" will pass *before* §2.1's work and cannot distinguish it from
   a broken §2.1. The acceptance has to be restated: Sample-6 proves the package survives, and
   synthetic documents prove the generated content is faithful.
2. **§2.1 is proven with synthetic documents, not samples.** That is arguably better: a synthetic
   document can isolate one property at a time, where a fixture bundles eighty of them.
3. **`detectSourceFeatures` is too coarse to be a useful gate long-term.** A document with one
   trivial two-cell table is treated the same as one with nested spanned tables. Narrowing it is
   not Plan 9's job — it would widen the refusal and change what Plan 7B guarantees — but it is
   the reason §7 gains an item requiring the refusal list to be measured per fixture rather than
   assumed.

`Plan9RoundTripTest.modifiedOdtSaveIsRefusedForEveryFixture` pins this: when Plan 9 or a later
plan narrows the refusal, that test is the thing that fails and says so.

### 2.3.2 The new-document case

`audit-025` §5.1 finding J: `templates/styles/Default.ott` carries `mimetype`,
`META-INF/manifest.xml`, `meta.xml`, `styles.xml` and `Thumbnails/thumbnail.png` and **no
`content.xml`**. `InkyModule.kt:3426` uses it as the fallback current path. Every fixture has a
`content.xml`, so a round-trip test built only on fixtures never exercises *creating* one.

`Plan9RoundTripTest.freshDocumentRoundTripProducesAReadablePackage` covers it with a synthetic
`OfficeDocument` that has no `odtPackageData`, which forces the fresh-generation branch.

What the first version asserts, and why each is assertable today:

| Property | ODT | DOCX | Note |
|---|---|---|---|
| element count and order | yes | yes | the cheapest catch-all |
| plain text equality | yes | yes | `OfficeDocument.toPlainText()` |
| run count and per-run flags | yes | after §2.2 item 2 | this is the assertion that fails today on DOCX |
| every `text:style-name` / `w:pStyle` written resolves to a style in the written package | yes | after §2.2 item 3 | closes the dangling-reference defect |
| list grouping: consecutive items share one `<text:list>` | after §2.1 item 3 | after §2.2 item 5 | Sample-6 ODT has 23 `text:list` elements to check against |
| table shape: rows, columns, spans | after §2.1 item 4 | after §2.2 item 4/5 | |
| image count | yes | after §2.2 item 6 | `Plan6ImageFoundationTest` already covers the ODT half |
| `office:version` is `1.4` | after §2.1 item 6 | n/a | |
| `saveCapability` still refuses a modified save with authored indexes | yes | n/a | guards the boundary this plan does not open |

The test file lands **first** (commit 1 below) with only the assertions that pass on today's code,
and grows one assertion group per feature commit. That is the ratchet Plan 3A used, and it means
every later commit has a failing-then-passing test behind it rather than a claim.

---

## 3. Out of scope

* TOC and authored-index regeneration, footnotes, fields, headers and footers, tracked changes.
  All remain behind `saveCapability`'s refusal, and the test asserts the refusal.
* Cellina (`.ods`, `.xlsx`) and Slidia (`.odp`, `.pptx`) writers. `plan-04-to-09` Appendix B puts
  spreadsheet and presentation fidelity at a later stage. `saveOdsDocument`, `saveXlsxDocument`,
  `saveOdpDocument` and `savePptxDocument` in `OfficeDocumentParser.kt` are untouched.
* Any new third-party dependency. Appendix B holds: `ZipSafe` plus XmlPullParser plus stdlib.
* The `[Image: …]` string at `OfficeDocumentParser.kt:2392`. It is inside `toPlainText`, which is
  diagnostics, not a save path. Look at it while §2.2 item 6 lands and decide then; it is not a
  Plan 9 acceptance item.
* New translations, and any Indonesian string. `values-in` is not extended (`plan-03` §0).
* UI work of any kind. Plan 9 has no screen changes, so the `antislop` Delivery Gate runs its
  reduced documentation form (`AGENTS.md` addendum 3): R-02, R-15, R-16, R-17, R-36, R-38, C-5
  with evidence; R-26, R-27, R-32, R-34, R-35 reported N/A with the reason.

---

## 4. The pre-change gate

Before commit 2, one commit that changes no behaviour:

1. Add `Plan9RoundTripTest` with the assertions marked "yes" in the §2.3 table, and confirm CI is
   green with them. This records what today's writer already preserves, which is the baseline the
   rest of the plan is measured against.
2. Add `Plan9RoundTripTest.roundTripOutputStaysOutOfTheFixtureDirectory`, asserting the test's own
   output path is under `cacheDir` **and** that the fixture's SHA-256 and length are unchanged
   afterwards. Cheap, and it is the guard for invariant 6.
3. Record the ODT side of the current loss in a comment at the top of the test file, citing
   §0.1's table, so a reader knows why each assertion exists.
4. Add the two measurements §2.3.1 and §2.3.2 call for, as pins rather than as features:
   `modifiedOdtSaveIsRefusedForEveryFixture`, which proves no fixture reaches `buildContentXml`,
   and `freshDocumentRoundTripProducesAReadablePackage` / `smallestFreshDocumentStillRoundTrips`,
   which are the only tests that exercise the generated path at all.
5. Pin the DOCX refusal with `docxSaveStillRefusesWhenTheDocumentCarriesImages` so Plan 9b's
   removal of it is a measured change (D5).

Gate: CI green, `FixtureIdentityTest` green, and the recorded baseline written into this
document's §9 implementation record.

---

## 5. Files

| File | Change |
|---|---|
| `data/writer/OdtDocumentWriter.kt` | §2.1. The largest diff |
| `data/writer/DocxDocumentWriter.kt` | **not in this plan.** New file in Plan 9b (D5) |
| `data/DocxDocumentParser.kt` | **not in this plan.** The element-flattening path (`:64`-`:74`) and the image refusal (`:56`) are Plan 9b's (D5) |
| `data/DocumentSerializer.kt` | unchanged in this plan; `DocxDocumentSerializer.write` starts calling `DocxDocumentWriter` in Plan 9b |
| `data/OfficeDocumentParser.kt` | retire the `OfficeDocElement` wrapper (F-2) in the final commit; `toPlainText`'s `[Image:]` line only if §3 says so |
| `app/src/test/java/com/example/Plan9RoundTripTest.kt` | **new** |
| `app/src/test/java/com/example/DocumentEnginesUnitTest.kt` | extend, since it already drives `OdtDocumentWriter` at twelve call sites |
| `anti-slop/plan-09-save-round-trip-integrity.md` | this file, §9 per commit |
| `anti-slop/plan-01-master-index.md` | registry row 9 and a §6 item per commit |

---

## 6. Delivery

Five commits, each with its own gate, ordered so a regression is attributable to one commit.
**ODT only** — decision D5 moved §2.2 to Plan 9b.

| # | Commit | Gate |
|---|---|---|
| 1 | **Pre-change gate** (§4): the round-trip test with today's passing assertions, the fixture-directory guard, and the recorded baseline | CI green; `FixtureIdentityTest` green |
| 2 | **ODT styles** (§2.1 items 1, 2, 6): emit the document's own automatic styles with D4's collision rule, append-only `P_papirus_*` / `T_papirus_*`, `office:version` to `1.4` | every `text:style-name` in a round-tripped synthetic document resolves to a style the package defines; the document's own `P1` (title) and `Table1` survive as the document defined them, not as the writer's fixed ones do; all six ODT pairs still inside their `SampleMatrix` windows |
| 3 | **ODT lists and tables** (§2.1 items 3, 4): one `<text:list>` per run, `text:list-style-name`, cell spans and cell styles | a synthetic five-item list produces one `<text:list>` with five `<text:list-item>` children, not five one-item lists; a synthetic spanned round-trip keeps its spans |
| 4 | **ODT font faces, bookmarks, hyperlinks, sections** (§2.1 items 5, 7, 8) | `saveCapability` no longer needs to refuse for font faces alone; bookmark and hyperlink counts survive the round trip |
| 5 | **Retire the ODT-side old paths**: delete the `OfficeDocElement` wrapper uses on the ODT save path | mechanical; no assertion changes; the compiler is the reviewer |

Commits 2 to 4 are ODT-only and cannot change a DOCX page count. Keep them separate so the
12-file matrix tells you which one moved a number.

**Not in this plan (Plan 9b):** the DOCX writer, `DocxDocumentWriter.kt`, the
`DocxDocumentParser.kt` flattening removal, `w:tblGrid` / numbering / image registration, and the
removal of the image refusal at `DocxDocumentParser:56`. Plan 9b needs its own plan document
before it starts, for the same reason this one did.

**Note on how §2.1's gates are written.** Because §2.3.1 shows no fixture can reach
`buildContentXml`, the gates for commits 2 to 4 are stated against *synthetic* documents, not
Sample-6. Sample-6's job in every one of those commits is narrower and still real: prove the
preserved path still round-trips and the page counts have not moved.

---

## 7. Acceptance

1. `Plan9RoundTripTest` passes for **Sample-6 ODT** on the properties in the §2.3 table **that are
   marked ODT**, in CI, and for the synthetic documents that §2.3.1 and §2.3.2 require. The DOCX
   half is Plan 9b (D5).
2. All twelve fixtures stay inside their recorded `SampleMatrix` windows, for **re-opened**
   documents as well as originals. Record the re-opened counts in the PR body; they are a new
   measurement and become the baseline.
3. `FixtureIdentityTest` passes unchanged, proving no fixture was rewritten.
4. `OdtDocumentWriter.saveCapability` still refuses a modified save for authored indexes, named
   sections and source table structure, and the test asserts it.
5. Every generated ODT root declares `office:version="1.4"`; `mimetype` is still first and STORED.
6. `DocxDocumentParser.saveDocument`'s image refusal is **still in place** and
   `Plan9RoundTripTest.docxSaveStillRefusesWhenTheDocumentCarriesImages` proves it. Removing it is
   Plan 9b's acceptance, not this plan's (D5).
7. The fixture round trips still land on the preserved path for all six ODT samples, and
   `modifiedOdtSaveIsRefusedForEveryFixture` prints the blocking features for each one. If Plan 9
   narrows the refusal, the PR must list which sample moved and why.
8. The PR body lists which findings it closes and which it defers (§3 and D5), so the audit stays
   the single source of truth.

---

## 8. Risks and decisions

**Risk: page counts will move.** Commit 3 changes list grouping, which changes how many lines a
list occupies. `plan-04-to-09` Appendix C item 2 says windows are "tightened, never loosened
silently": if a window has to move, the PR must say which sample, by how much, and why. Measure
before and after, do not adjust the window to make the build green.

**Risk: `office:version` 1.4 with 1.2-era content.** LibreOffice is tolerant and ODF 1.4 is a
superset for the elements this writer emits, but if the round-trip test shows a re-opened document
differing from the original on `office:version` alone, that is expected and should be asserted
rather than treated as a failure.

**Decision D3, carried here.** The owner reversed it on 2026-10-04 and deferred declared font
faces to Plan 9. §2.1 item 5 implements it.

### Decision D4: the collision rule. **Decided 2026-10-15: the document owns its names; the
generator steps aside.**

The question: `buildContentXml` hardcodes eleven style names (`P1`, `T_Bold`, `T_Italic`,
`T_BoldItalic`, `T_Underline`, `T_BoldUnderline`, `T_ItalicUnderline`, `T_BoldItalicUnderline`,
`Table1`, `Table1.Col`). Once §2.1 item 1 starts emitting the document's *own* styles, what
happens when the document already uses one of those names?

**The collision is not hypothetical. It happens on every fixture.** Measured by unzipping the six
`tests/inky/*.odt` and listing the `style:name` values inside `<office:automatic-styles>`:

| fixture | automatic styles | collides with the fixed eleven |
|---|---|---|
| Sample-1.odt | 90 | `P1`, `Table1` |
| Sample-2.odt | 132 | `P1`, `Table1` |
| Sample-3.odt | 53 | `P1`, `Table1` |
| Sample-4.odt | 33 | `P1` |
| Sample-5.odt | 45 | `P1` |
| Sample-6.odt | 75 | `P1`, `Table1` |

**And the two names mean different things.** In Sample-6.odt:

| name | the document's definition | the writer's hardcoded definition |
|---|---|---|
| `P1` | `style:parent-style-name="Title"`, `style:master-page-name="Standard"`, `style:page-number="1"` — **the title paragraph** | `style:parent-style-name="Standard"` — a body paragraph |
| `Table1` | `style:width="15.905cm"`, `table:align="left"` | `style:width="100%"`, `table:align="margins"` |

So the rule this plan originally proposed — *keep the fixed one and log it* — would, on all six
fixtures, silently downgrade the document's title paragraph to body text and resize every table to
full width. "Log it" means the user never sees it. That is precisely the class of silent data loss
Plan 9 exists to close, so the recommendation is reversed.

**The rule:**

1. Collect every style name the document already declares, from `DocumentStyles` plus every
   `style:name` the body references.
2. The writer asks for names through one helper, `reserve(baseName)`. If the name is free it is
   used unchanged; if it is taken the writer gets `baseName_papirus_1`, then `_2`, and so on.
3. The writer never emits a `<style:style>` under a name the document already uses.
4. Log at debug, not warn. A rename is normal bookkeeping, not a defect.
5. If the *source* declares the same name twice, that is a malformed document (ODF forbids
   duplicate style names): keep the first, log a warning, carry on.

**Why this one and not the alternatives.** The fixed eleven are names *the writer invents*; the
document's names are the ones the body's `text:style-name` attributes point at. Moving the
generator costs roughly twenty lines and loses nothing, because the writer controls both sides of
its own references. Moving the document instead would break a reference we did not create.
Refusing the save was rejected: the collision is the writer's fault, not the document's, and
failing closed here would refuse to save real LibreOffice files.

This also keeps the ClearPDF design note's append-only rule intact — we append, and the *generator*
is what moves rather than the document's styles being re-indexed.

**Decision D5: the DOCX half is deferred. Decided by the owner 2026-10-15.** Plan 9 ships the ODT
half only. §2.2 moves to a following plan, **Plan 9b**, alongside the roadmap acceptance line that
depends on it.

What this changes:

* §6's commit 5 (DOCX writer) and the DOCX halves of the §2.3 assertion table leave this plan.
* Roadmap §4.10 asks for "Sample-6 in both formats". This plan therefore meets **the ODT half** of
  that acceptance and says so explicitly rather than implying both. Plan 9b closes the DOCX half.
* Acceptance item 6 (removing `DocxDocumentParser.saveDocument`'s image refusal) moves to Plan 9b.
  `Plan9RoundTripTest.docxSaveStillRefusesWhenTheDocumentCarriesImages` pins the refusal today so
  its removal is a measured change rather than a claim.
* Acceptance item 7's `OfficeDocElement` retirement (F-2) stays in Plan 9: it is a mechanical
  deletion gated on the ODT path, and `generateDocxXmlFromElements` is still the DOCX writer
  Plan 9b replaces, so F-2 completes there instead. Plan 9 deletes the *ODT-side* uses only.
* §5's file table loses `DocxDocumentWriter.kt` (new) and the `DocxDocumentParser.kt` edit; both
  reappear in Plan 9b's own plan document, which must be written before that plan starts, exactly
  as this one was.

The reasoning the owner gave is sound and worth recording: §2.2 is the larger half, and shipping
the ODT half alone keeps each PR reviewable. `plan-04-to-09` Appendix A already treats 8A and 8B
as separate PRs on the same grounds.

---

## 9. Implementation record

* **2026-10-15, plan written.** No code changed. Baseline `main` at `53d61ac`, green at `ci.yml`
  run `38231653018`. The defect measurements in §0 and the line numbers throughout are that
  tree's; re-check them before the first commit, because nothing in this plan has been compiled
  (no Gradle, Android SDK or JDK in the agent sandbox, `AGENTS.md` evidence ladder tier 1).
* **2026-10-15, housekeeping landed first** (`audit-025` tier-2 items 3, 4 and 6): the two dead
  files the 2026-10-12 re-upload brought back were deleted again, 3,040,550 bytes of unreferenced
  `img_pattern_*.jpg` were removed, and 7 unused imports across 6 files were dropped. None of it
  touches the save path; it was done first so Plan 9's diff contains Plan 9.
* **2026-10-15, decisions D4 and D5 closed by the owner.** D4 (§8): the collision rule is *the
  document owns its names; the generator steps aside*, reversing this plan's original
  recommendation after the six fixtures were measured — all six declare `P1` and four declare
  `Table1`, and in Sample-6 the document's `P1` is the title paragraph while the writer's is a
  body paragraph. D5 (§8): the DOCX half is deferred to Plan 9b; Plan 9 ships ODT only.
* **2026-10-15, commit 1 (the pre-change gate) written and then run.** `Plan9RoundTripTest.kt`,
  six tests: the preserved-path round trip over all six ODT fixtures; the per-fixture
  modified-save refusal measurement; two fresh-document round trips; the DOCX image-refusal pin;
  and the fixture-directory guard.
  **CI run `38765790493`, head `b769276`: all six pass.** `Unit tests: 441 run, 0 failed, 0
  errors, 0 skipped (43.57s across 83 suites)`, with `Plan9RoundTripTest | 6 | 0 | 0 | 0 | 0.69`
  and `FixtureIdentityTest | 1 | 0 | 0 | 0`. The count moved **435 → 441**, exactly the six new
  tests, and 82 → 83 suites.
  One correction on the way: run `38764644308` failed to **compile**, with four
  `Unresolved reference 'toOfficeDocument'` errors (the extension was not imported). That is the
  gate doing its job on the first attempt and cost one commit; fixed in `b769276`.
  **What the green run establishes.** The preserved path round-trips all six ODT fixtures with
  element count and plain text unchanged, so the rebuilt package is a package the parser accepts
  (§2.3 table, first two rows). The refusal measurement passed, which **confirms §2.3.1 by
  execution rather than by reading**: every one of the six fixtures is refused for a modified
  save, so `buildContentXml` really is unreachable through a fixture. The fresh-document tests
  pass, so today's generated package is at least readable and text-preserving — the defects §2.1
  fixes (dangling style references, per-item lists, lost spans) are *not* yet asserted, which is
  what commits 2 to 4 add. The DOCX refusal is confirmed still in place.
  The same run's lint job reports `lint 9.4.1: 0 errors, 59 warnings across 7 issue ids`,
  unchanged from the three runs before it, so the new test file added no warning.
  Evidence tier 3 (GitHub Actions): no Gradle, Android SDK or JDK in the agent sandbox, so every
  statement above comes from the CI report and not from a local run (`AGENTS.md` ladder, tier 1
  for everything else in this document).
* **2026-10-15, §2.3.1 added.** No fixture can reach `buildContentXml`: `detectSourceFeatures`
  sets `hasAdvancedTables` on *any* `<table:table>` and `hasAuthoredIndexes` on *any*
  `table-of-content`, and between them the six samples cover both. This is why the §6 gates for
  commits 2 to 4 are written against synthetic documents.
