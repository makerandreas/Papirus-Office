# Plan 9b: DOCX save round-trip integrity

**Status:** planned 2026-10-16. This is the document `plan-04-to-09-writer-fidelity.md` requires
before a plan starts, applied to the half that Plan 9 deferred. No PR slot is claimed: plan IDs are
authoritative and PR numbers are forecasts.
**Parent:** `anti-slop/plan-09-save-round-trip-integrity.md` §0.2 and §2.2, which measured this
defect and deferred it by owner decision **D5** on 2026-10-15.
**Closes:** the DOCX half of roadmap v2 §4.10, and completes **F-2** (retiring the
`OfficeDocElement` sealed class, which Plan 9 retired from the *write* path but left in place
because the DOCX writer still uses it).
**Depends on:** Plan 9 (landed), Plan 6D (the image refusal), Plan 8A (the `OfficeTextRun`
tri-state model), Plan 8B (numbering). All landed.
**Evidence:** every number below was measured in the working tree on 2026-10-16 by unzipping the
six `tests/inky/*.docx` fixtures and by reading `DocxDocumentParser.kt`. Line numbers are that
tree's. Re-check them before using this document: the 2026-10-12 re-upload moved code without
moving the prose that cites it (`audit-025` §6), and Plan 9's own line numbers were already stale
once.
**Reference:** `docs/ooxml/[MS-DOCX].pdf`, page-indexed in `docs/ooxml/README.md`; ECMA-376 Part 1.

---

## 0. Why this plan exists, measured rather than asserted

### 0.1 What the DOCX save path does today

`DocxDocumentParser.saveDocument` (`DocxDocumentParser.kt:54`) is the whole DOCX writer. There is
no `DocxDocumentWriter` class; `DocxDocumentSerializer.write` calls this parser method. It does
four things, and three of them lose data.

1. **Refuses images** (`:56`–`:62`). This is Plan 6D's fail-closed guard and it is correct; it
   stays until item 6 lands and the round trip proves it.
2. **Throws away every run.** The element mapping at `:64`–`:74` builds
   `OfficeDocumentElement.Paragraph(text = element.paragraph.text)` with no `runs`,
   `Heading(text, level)` with no runs and **no `styleName`**, and `ListItem(text, bullet)` with no
   runs. Every bold, italic, colour, size and font is gone before a byte is written. The
   `OfficeTextRun` tri-state model Plan 8A built, and the eleven tests in `DocxRunFormattingTest`
   that cover it, never reach the save path.
3. **Rebuilds `word/document.xml` from scratch** via `generateDocxXmlFromElements` (`:440`). See
   §0.2.
4. **Rebuilds or drops the package** via `saveDocxZip` (`:314`). See §1 and §0.4.

### 0.2 The page-size defect: this is decision D4's rule, on the DOCX side

`generateDocxXmlFromElements` (`:440`) appends a hardcoded section:

```
<w:sectPr>
  <w:pgSz w:w="12240" w:h="15840"/>
  <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>
</w:sectPr>
```

`12240 × 15840` twips is **US Letter**. Every one of the six fixtures is **A4**:

| fixture | source `w:pgSz` | source `w:pgMar` (top/right/bottom/left, header, footer) |
|---|---|---|
| Sample-1.docx | `11906 × 16838` | 1440/1440/1440/1440, header 0, footer 567 |
| Sample-2.docx | `11907 × 16840` | 1440/1440/1440/1440, header 0, footer 567 |
| Sample-3.docx | `11906 × 16838` | 1440/1440/**2203**/1440, header 0, footer **1440** |
| Sample-4.docx | `11906 × 16838` | — |
| Sample-5.docx | `11907 × 16840` | — |
| Sample-6.docx | `11907 × 16840` | — |

So a DOCX save silently changes the page from A4 to Letter and the bottom margin and footer
distance with it. That reflows the entire document and moves the page count. It is the clearest
possible instance of the rule the owner stated for Plan 9 (decision D4), recorded verbatim in
`plan-09` §8:

> The application *may not* carry out the style override with the default template force that is
> used to create a new document.

**D4 applies to this plan unchanged.** Preserving the source's `w:sectPr` is therefore not a
polish item; it is the same defect Plan 9's commit 2 fixed for ODT, and it is arguably worse here
because getting it wrong moves pages rather than just changing a font.

### 0.3 What the six fixtures contain

Measured by unzipping each package and counting elements in `word/document.xml`:

| fixture | `w:r` | `w:pStyle` | `w:numPr` | `w:tbl` | `w:tblGrid` | `w:hyperlink` | `w:bookmarkStart` | `w:sectPr` | `numbering.xml` | styles | media |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Sample-1.docx | 1348 | 73 | 27 | 3 | 3 | 0 | 0 | 1 | yes | 41 | 6 |
| Sample-2.docx | 736 | 174 | 106 | 2 | 2 | 15 | 15 | 5 | yes | 39 | 10 |
| Sample-3.docx | 436 | 32 | 0 | 1 | 1 | 0 | 0 | 1 | **no** | 58 | 0 |
| Sample-4.docx | 172 | 51 | 14 | 0 | 0 | 23 | 22 | 6 | yes | 191 | 2 |
| Sample-5.docx | 2570 | 123 | 75 | 0 | 0 | 14 | 14 | 5 | yes | 36 | 1 |
| Sample-6.docx | 732 | 202 | 91 | 1 | 1 | 45 | 46 | 5 | yes | 38 | 3 |

Five consequences for the test design:

1. **Unlike ODT, the generated path is reachable through fixtures.** `saveDocument` refuses only
   for images; there is no `saveCapability` gate. **Sample-3.docx has 0 media**, so it can be
   round-tripped today, before any of this plan is implemented. That makes Sample-3 the pre-change
   gate's subject, and the other five join as images land.
2. **`w:gridSpan` and `w:vMerge` are 0 in all six.** No fixture has a merged cell, so item 4 cannot
   be proven with a fixture and needs synthetic documents. This is the same shape as Plan 9's
   §2.3.1 finding, and it is recorded here rather than discovered at the gate.
3. **Sample-3 has no `word/numbering.xml`.** Numbering assertions must tolerate a missing
   numbering part; `DocxNumberingReaderTest`'s `sample 3 has no numbering xml so result is empty`
   already pins the read side.
4. **Every fixture has 1–6 `w:sectPr` elements.** The writer emits exactly one. Sections are not an
   edge case here.
5. **Hyperlinks and bookmarks are substantial** (up to 45 and 46 in Sample-6) and currently vanish.

### 0.4 Saving to a new file loses the package entirely

`saveDocxZip` (`:314`) branches on whether the target file already exists:

* **Target exists** (`:332`–`:356`): every entry is copied through unchanged except
  `word/document.xml`, which is replaced, with a fallback at `:350` that appends it if the source
  package had none. This is real package preservation and it already works — see §1.
* **Target does not exist** (`:317`–`:331`): exactly four parts are written —
  `[Content_Types].xml`, `_rels/.rels`, `word/_rels/document.xml.rels`, `word/document.xml`.
  **No `word/styles.xml`, no `word/numbering.xml`, no `word/media/*`, no `docProps/*`.**

So a "Save As" to a new path produces a DOCX whose `w:pStyle` references point at a styles part
that does not exist. Every one of Sample-5's 123 `w:pStyle` references dangles. Word will fall
back to Normal; the formatting is gone. `generateDocxContentTypesXml` (`:387`) reinforces this: it
declares a single `Override`, for `word/document.xml`.

---

## 1. What already exists. Do not rebuild it

1. **Package preservation when the target exists.** `saveDocxZip:332`–`:356` copies every entry
   except `word/document.xml` byte-for-byte. `word/styles.xml`, `word/numbering.xml`,
   `word/theme/*`, `word/fontTable.xml` and `docProps/*` already survive a save in place. Item 1
   is mostly about *preserving that while adding* rather than building it.
2. **The `OfficeTextRun` tri-state model and its eleven tests.** `DocxRunFormattingTest` covers
   run-per-authored-run, explicit negatives, paragraph-mark properties not leaking into runs,
   `w:rStyle` resolution through the character-style table, direct formatting beating the run
   style, colour/highlight/vanish, tabs and breaks staying in their run, `aNumberedParagraphGets
   NoLabelWhenThePackageHasNoNumberingPart`, heading runs, and `mergeRunSubtracts`. Item 2's job
   is to let this already-tested model reach the writer, not to re-test it.
3. **Numbering state on the model.** `DocxNumberingRef(numId, ilvl, suppressed, fromStyle)`
   (`OfficeDocumentModel.kt:141`) is populated on `OfficeParagraph.numbering` and
   `OfficeHeading.numbering` by Plan 8A/8B, and `DocxNumberingReader` resolves `numId` → spec with
   its own tests. Item 5 writes what the model already carries.
4. **The image refusal, and the reason it exists.** `saveDocument:56` plus
   `Plan6ImageFoundationTest`. Removed only when item 6 is proven.
5. **`DocumentFormatWriter`** (`DocumentFormatWriter.kt:5`), the one-method interface
   `OdtDocumentWriter` implements. The new `DocxDocumentWriter` implements the same interface.
6. **Plan 9's own reusable pieces, if they can be shared honestly.** `StyleNameAllocator` solves
   the same collision problem for ODT, but it is currently `private` inside `OdtDocumentWriter`.
   Item 3 decides whether to lift it to a shared internal helper or write a DOCX-shaped equivalent.
   Do not share it by copy-paste.

---

## 2. Scope

Ordered by how much data each item recovers.

1. **Preserve the source section properties.** Write the document's own `w:sectPr` — page size,
   margins, header/footer distances, gutter — instead of the hardcoded Letter block. Where the
   model does not carry a value, fall back to the current constants and say so. This is §0.2 and it
   is item 1, not item 7, because it changes page counts.
2. **Stop dropping runs.** Carry `OfficeTextRun` through to `w:r` with `w:rPr`, including the
   tri-state "subtract" behaviour Plan 8A implemented. `w:rStyle` when the run carries a character
   style; direct `w:b` / `w:i` / `w:u` / `w:color` / `w:highlight` / `w:vanish` / `w:sz` only when
   the tri-state says so. This is the single highest-value change in the plan: 2,570 runs are lost
   on Sample-5 today.
3. **Style linkage.** Heading and character style pairs per `plan-04-to-09` §2.3, with `w:link`
   and the `X` / `X Char` convention. Because `word/styles.xml` is preserved from the source,
   generated styles must be **appended, never re-indexed** — the same append-only rule the ClearPDF
   design note gave Plan 9, and the same collision rule as decision D4: a style name the document
   declares is the document's.
4. **Tables.** `w:tblGrid` on every regenerated table, plus `w:gridSpan` and `w:vMerge` for cell
   spans. No fixture exercises spans (§0.3 consequence 2), so this is proven synthetically.
5. **Numbering.** `w:numPr` written from `DocxNumberingRef`, with `w:ilvl` and `w:numId`. Assert
   every `numId` written resolves in the preserved `numbering.xml`, and tolerate a package with no
   numbering part (Sample-3).
6. **Images.** Write `word/media/*`, register each in `[Content_Types].xml` and
   `word/_rels/document.xml.rels`, then remove the refusal at `DocxDocumentParser:56`. This is
   what unblocks five of the six fixtures for round-tripping.
7. **Make a new file a real package.** When the target does not exist, write `word/styles.xml`,
   `word/numbering.xml` and a correct `[Content_Types].xml` rather than four bare parts (§0.4).
8. **Then and only then** delete `generateDocxXmlFromElements`, the `saveDocument` flattening at
   `:64`–`:74`, and the `OfficeDocElement` sealed class (F-2 completes).

### 2.1 The round-trip test

Extend `app/src/test/java/com/example/Plan9RoundTripTest.kt`, which already has 21 tests and the
fixture-directory guard. The shape is unchanged from Plan 9 §2.3: read fixture → write to
`context.cacheDir` → re-read → assert → delete in a `finally`. Never write into `tests/inky`;
`FixtureIdentityTest` is the guard.

The one existing DOCX test, `docxSaveStillRefusesWhenTheDocumentCarriesImages`, currently asserts
the refusal is **still in place**. Each item flips one assertion:

| Property | Assertable today | After item |
|---|---|---|
| element count and order | yes | — |
| plain text equality | yes | — |
| run count and per-run flags | no (Sample-3: 436 runs lost) | 2 |
| every `w:pStyle` / `w:rStyle` written resolves in the package | no (Save-As: all dangle) | 3, 7 |
| page size and margins equal the source | no (Letter vs A4) | 1 |
| `w:tblGrid` on every table, spans preserved | no | 4 |
| every `w:numId` resolves, or numbering part absent | no (hardcoded `numId 1`) | 5 |
| image count survives | no (refused) | 6 |

---

## 3. Out of scope

* Fields, footnotes and endnotes, headers and footers content, tracked changes, comments, TOC
  field regeneration. All remain unwritten.
* Cellina (`.xlsx`) and Slidia (`.pptx`) writers. `saveXlsxDocument` and `savePptxDocument` in
  `OfficeDocumentParser.kt` are untouched.
* Any new third-party dependency. Appendix B holds: `ZipSafe` plus XmlPullParser plus stdlib.
* New translations and any Indonesian string. `values-in` is not extended (`plan-03` §0).
* UI work of any kind. Plan 9b has no screen changes, so the `antislop` Delivery Gate runs its
  reduced documentation form (`AGENTS.md` addendum 3): R-02, R-15, R-16, R-17, R-36, R-38, C-5 with
  evidence; R-26, R-27, R-32, R-34, R-35 reported N/A with the reason.
* The `[Image: …]` string at `OfficeDocumentParser.kt:2392`. It is inside `toPlainText`, which is
  diagnostics, not a save path.

---

## 4. The pre-change gate

Before item 1, one commit that changes no behaviour, extending the existing test file:

1. Add a DOCX round trip for **Sample-3** (0 media, so it is not refused) asserting element count,
   plain text, and that the current writer *loses* the runs — asserted as a measurement, printed
   with the count, so the number is on the record before it changes.
2. Assert the current page size is Letter against Sample-3's A4, again as a recorded measurement
   rather than a failure, with a comment that item 1 will flip it.
3. Assert a Save-As to a **new** path writes no `word/styles.xml`, so §0.4 is on the record.
4. Assert the image refusal for the five fixtures that carry media.

Gate: CI green, `FixtureIdentityTest` green, and the recorded baseline written into this file's §9.

---

## 5. Files

| File | Change |
|---|---|
| `data/writer/DocxDocumentWriter.kt` | **new**, implements `DocumentFormatWriter`. The largest diff |
| `data/DocxDocumentParser.kt` | remove `saveDocument`'s flattening (`:64`–`:74`), the image refusal (`:56`), `generateDocxXmlFromElements` (`:440`) and `writeDocxElement`, once the writer replaces them |
| `data/DocumentSerializer.kt` | `DocxDocumentSerializer.write` calls `DocxDocumentWriter`; `serializeToFormat` unchanged |
| `data/OfficeDocument.kt` | retire the `OfficeDocElement` sealed class (F-2) in the final commit, once the DOCX writer no longer uses it |
| `data/DocumentTextMerger.kt` | its `OfficeDocElement.ParagraphElement` arm (`:120`) goes with the sealed class |
| `app/src/test/java/com/example/Plan9RoundTripTest.kt` | extend |
| `anti-slop/plan-09b-docx-save-round-trip-integrity.md` | this file, §9 per commit |
| `anti-slop/plan-01-master-index.md` | registry and a ledger item per commit |

---

## 6. Delivery

Seven commits, each with its own gate.

| # | Commit | Gate |
|---|---|---|
| 1 | **Pre-change gate** (§4): Sample-3 DOCX round trip, the three recorded measurements, the refusal assertions | CI green; `FixtureIdentityTest` green |
| 2 | **Sections** (item 1): the source's own `w:sectPr` | Sample-3's saved page size equals its source, A4 not Letter; **all six DOCX page counts still inside their `SampleMatrix` windows** |
| 3 | **Runs** (item 2) | `DocxRunFormattingTest`'s eleven tests still green, and the round-tripped Sample-3 keeps its 436 runs with their flags |
| 4 | **Styles and a real new package** (items 3, 7) | every `w:pStyle` written resolves in the package for a Save-As; the source's style names are unchanged |
| 5 | **Tables and numbering** (items 4, 5) | `w:tblGrid` present on every regenerated table; every `w:numId` resolves or the numbering part is genuinely absent |
| 6 | **Images** (item 6) | the refusal is gone; all six fixtures round-trip with their media; `Plan6ImageFoundationTest` still green |
| 7 | **Retire the old paths** (F-2): delete `generateDocxXmlFromElements`, the flattening, and `OfficeDocElement` | mechanical in the sense that no assertion changes; **see the warning below** |

**Warning carried from Plan 9 commit 5.** That commit's plan entry said "mechanical; the compiler
is the reviewer" and it was wrong. Deleting `OfficeDocElement` looked safe until
`DocumentTextMerger.replaceText` turned out to still construct one, and an unhandled wrapper fell
through an empty `else` and vanished from the saved file. Commit 7 must re-grep every construction
site before deleting anything, and must not trust the plan's own word that it is mechanical.

---

## 7. Acceptance

1. `Plan9RoundTripTest` passes for **Sample-6 in DOCX** on every property in the §2.1 table, in CI.
   With Plan 9's ODT half, that closes roadmap §4.10's "Sample-6 in both formats" in full.
2. All twelve fixtures stay inside their recorded `SampleMatrix` windows, for **re-opened**
   documents as well as originals. Record the re-opened counts in the PR body; they are a new
   measurement and become the baseline.
3. `FixtureIdentityTest` passes unchanged, proving no fixture was rewritten.
4. `DocxRunFormattingTest`'s eleven tests still pass.
5. A saved DOCX keeps its source page size and margins.
6. `DocxDocumentParser.saveDocument`'s image refusal is gone and the six fixtures round-trip with
   their media intact.
7. `OfficeDocElement` no longer exists, and the compiler proves it.
8. The PR body lists which findings it closes and which it defers (§3), so the audit stays the
   single source of truth.

---

## 8. Risks and decisions

**Risk: page counts will move, and this time they are supposed to.** Item 1 restores A4, which
reflows documents the app has been saving as Letter. `plan-04-to-09` Appendix C item 2 says windows
are "tightened, never loosened silently": if a window has to move, the PR must say which sample, by
how much, and why. Measure before and after. A page count that *returns to* the recorded reference
is a fix, not a regression, and the PR should say so explicitly.

**Risk: no compiler in the agent sandbox.** Plan 9 shipped two compile failures that only CI
caught — a missing `toOfficeDocument` import in commit 1, and an unescaped closing quote in two
`sb.append` strings in commit 4. Expect more. Every claim in this document is tier 1 until a CI
run says otherwise, and the §9 record must carry run ids rather than intentions.

**Decision D4 carries over unchanged.** The document owns its names and its formatting; the
writer's defaults may not override them. Item 1 (page size), item 3 (style names) and item 2
(run formatting) are all applications of it.

**Decision D5 is what created this plan.** The owner deferred the DOCX half on 2026-10-15 so each
PR stays reviewable; `plan-04-to-09` Appendix A treats 8A and 8B the same way.

**Open, not blocking:** whether commits 3 and 4 fit as written or need splitting on diff size. If
either passes roughly 600 lines, split and give each half its own CI gate, the way Plan 9's §6
originally proposed for the DOCX half as a whole.

**Open, not blocking:** whether `StyleNameAllocator` is lifted out of `OdtDocumentWriter` into a
shared internal helper or reimplemented for DOCX. Lifting it is preferred; decide when item 3's
shape is clear.

---

## 9. Implementation record

* **2026-10-16, plan written.** No code changed. The measurements in §0 and the line numbers
  throughout are this tree's, taken by unzipping the six fixtures and reading
  `DocxDocumentParser.kt`. Nothing here has been compiled: no Gradle, Android SDK or JDK in the
  agent sandbox (`AGENTS.md` evidence ladder, tier 1).
* **Predecessor, for the baseline.** Plan 9's ODT half landed as five commits on
  `arena/20d3dfd7-papirus-office` (PR #40), ending at `Unit tests: 456 run, 0 failed, 0 errors, 0
  skipped`, `Plan9RoundTripTest | 21 | 0 | 0 | 0`, `lint 9.4.1: 0 errors, 59 warnings across 7
  issue ids`, with the 12-file matrix unmoved across five consecutive runs (CI run `38815868265`,
  head `ab2ae9d`). Those are the numbers this plan starts from. **If plan-09b begins on a tree
  where any of them differ, re-measure this document before using it.**
* **Carried forward from Plan 9.** A JDK handoff brief exists at
  `anti-slop/plan-09-jdk-handoff.md`. Its central caution applies here with more force, because
  DOCX round-trip assertions re-open the saved file with the app's own parser: a misunderstanding
  shared by the writer and the parser passes both sides of the test. Only an independent reader —
  Microsoft Word, or a validator — can catch that. Re-run that brief's task 1 against DOCX output
  before calling this plan done.
