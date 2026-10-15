# Plan 9 handoff: what needs a JDK, an Android SDK, or a real ODF reader

**Purpose.** A self-contained brief to hand to `gemini-3.8-flash` in Google AI Studio. Everything
below was done in a sandbox that has **no Gradle, no Android SDK, no JDK, no Robolectric, no
Compose and no MockK** — `AGENTS.md` calls that evidence tier 1. Every build, test and lint number
in this plan therefore came from GitHub Actions (tier 3), which is the only place anything here has
actually executed. This document says what CI already covers, and what it cannot.

**Repository:** `makerandreas/Papirus-Office`
**Branch:** `arena/20d3dfd7-papirus-office`
**PR:** #40 (open, mergeable, base `main`)
**Head at the time of writing:** `ab2ae9d`
**Plan document:** `anti-slop/plan-09-save-round-trip-integrity.md`
**Parent plan:** `plan-04-to-09-writer-fidelity.md` § Plan 9

---

## 1. What Plan 9 changed, in five commits

All five are ODT-only. The Word (.docx) half is deferred to **Plan 9b** by owner decision (D5).

| # | Commit | What changed | Tests | CI run (head) |
|---|---|---|---|---|
| 1 | Pre-change gate | New `app/src/test/java/com/example/Plan9RoundTripTest.kt`, 6 tests recording what today's writer already preserves | 435 → 441 | `38765790493` (`b769276`) |
| 2 | ODT styles | The document keeps its own styles; `office:version` 1.2 → 1.4 | 441 → 445 | `38770782845` (`21b25d3`) |
| 3 | Lists and tables | Consecutive items share one list; cell spans and covered slots | 445 → 450 | `38773891798` (`4191d9f`) |
| 4 | Fonts, sections, links, bookmarks | `font-face-decls`, `text:section`, `text:a`, bookmarks | 450 → 455 | `38778398794` (`20d6724`) |
| 5 | Wrapper retirement (F-2) | `OfficeDocElement` wrappers unwrapped out of the write path | 455 → 456 | `38815868265` (`ab2ae9d`) |

**Final state: 456 tests, 0 failed, 0 errors, 0 skipped.** `Plan9RoundTripTest | 21 | 0 | 0 | 0`.
`FixtureIdentityTest | 1 | 0 | 0 | 0`. Lint `9.4.1: 0 errors, 59 warnings across 7 issue ids`.

**The 12-fixture page-count matrix did not move in any of the five commits.** ODT
`14/23/21/10/18/20`, DOCX `16/26/23/12/20/24`, with the thin, empty, break and blank columns
identical to the four runs that predate this plan. That is the main regression signal, and it is
clean.

**Two commits failed before they passed**, both caught by CI rather than by the sandbox:

* `38764644308` (`f3d2b97`) — 4 × `Unresolved reference 'toOfficeDocument'`; the extension was not
  imported. Fixed in `b769276`.
* `38777197677` (`e8decbf`) — 16 compile errors at `OdtDocumentWriter.kt:640-648`; the
  `OfficeHyperlink` and `OfficeBookmark` branches escaped an attribute's opening quote but not its
  closing one, terminating the Kotlin string early. Fixed in `20d6724`.

Expect the same class of mistake in any future code written without a compiler.

### The design rule (owner's words, recorded as decision D4 in the plan §8)

> If you want to open the document and then edit it, make sure when you save it, the document
> *must* use the paragraph style, character or other style that is the same as before it opened
> (unless there is a paragraph style, character, or other style changed by the user). The
> application *may not* carry out the style override with the default template force that is used
> to create a new document.

Commit 2 implements this by copying the source's `<office:automatic-styles>` block **verbatim**
out of `OdtPackageData` rather than re-deriving it from `DocumentStyles`, because the resolved
style model does not carry every property the source declares. A `StyleNameAllocator` seeded with
the source's `style:name` values gives the writer's eleven hardcoded names a `_papirus_N` suffix
when the document already owns one.

### A finding that shapes everything below

**No fixture can reach the code this plan rewrote.** `detectSourceFeatures`
(`OdtImportPipeline.kt:231`) sets `hasAdvancedTables` on *any* `<table:table>` and
`hasAuthoredIndexes` on *any* `table-of-content`; `saveCapability` (`OdtDocumentWriter.kt:142`)
refuses a modified save for either. Sample-1/2/3/6 contain tables, Sample-2/4/5/6 contain a TOC, so
**all six are refused**. `Plan9RoundTripTest.modifiedOdtSaveIsRefusedForEveryFixture` passes, which
pins this by execution.

Consequence: the roadmap's "open, save, reopen preserves … for Sample-6" is only reachable through
the path that copies the original `content.xml` byte-for-byte, which already worked and cannot tell
a fixed generator from a broken one. Commits 2–4 were therefore proven with **synthetic documents**
that set `isModified = true` with default `OdtSourceFeatures`. That is a real limitation, and
section 2 task 1 is the way to close it.

---

## 2. What a JDK environment can do that CI cannot

### Task 1 — validate the output with an independent ODF reader (highest value)

**Why this matters more than anything else on the list.** Every round-trip assertion in
`Plan9RoundTripTest` re-opens the saved file with **the app's own parser**. If the writer and the
parser share a misunderstanding, both agree and the test passes while the file is wrong. Nothing in
CI can catch that.

**Ask:** generate a saved `.odt`, then open it in LibreOffice (or validate it with an ODF
validator) and confirm it renders as expected. Concretely:

1. Build a small ODT by hand in LibreOffice: a title paragraph, two body paragraphs, a
   three-item bulleted list, a 2×2 table with one merged cell, a red run, and one bookmark.
   Save it — deliberately with **no table of contents and no tables**, so a modified save is
   not refused.
2. Load it in the app, edit one paragraph, save.
3. Open the saved file in LibreOffice and check: the title is still a title, the list still
   numbers 1-2-3, the merged cell is still merged, the run is still red, the bookmark is still
   there.

**Report back:** a screenshot or a description per item, and the `content.xml` if anything is
wrong.

### Task 2 — dump the generated `content.xml` and read it

CI never shows the XML. A one-off test that writes a package and prints the content is cheap and
catches things assertions miss. Drop this into `app/src/test/java/com/example/` and run it:

```kotlin
package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.makerandreas.papirusoffice.data.*
import com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan9DumpXmlTest {
    @Test
    fun dumpGeneratedContentXml() {
        val document = OfficeDocument(
            body = DocumentBody(
                elements = listOf(
                    OfficeParagraph("The title", styleName = "P1"),
                    OfficeParagraph("Plain body"),
                    OfficeListItem("One"), OfficeListItem("Two"), OfficeListItem("Three"),
                    OfficeTable(
                        numColumns = 2,
                        rows = listOf(
                            OfficeTableRow(cells = listOf(
                                OfficeTableCell("wide", columnSpan = 2),
                                OfficeTableCell("", occupancy = TableCellOccupancy.COVERED)
                            )),
                            OfficeTableRow(cells = listOf(
                                OfficeTableCell("left"), OfficeTableCell("right")
                            ))
                        )
                    ),
                    OfficeParagraph("red", runs = listOf(
                        OfficeTextRun("red", colorHex = "#ff0000")
                    ))
                )
            )
        )
        val bytes = OdtDocumentWriter().write(document)
        ZipInputStream(bytes.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name == "content.xml") {
                    val out = ByteArrayOutputStream()
                    zip.copyTo(out)
                    println("===== BEGIN content.xml =====")
                    println(out.toString(Charsets.UTF_8))
                    println("===== END content.xml =====")
                }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
    }
}
```

Run `./gradlew testDebugUnitTest --tests "com.example.Plan9DumpXmlTest"` and paste the output.
Delete the file afterwards; it is a probe, not a test.

### Task 3 — confirm the whole suite and the lint baseline locally

```
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Expected: **456 tests, 0 failures** and **lint 9.4.1, 0 errors, 59 warnings across 7 issue ids**.
If either differs, the sandbox's understanding of the tree is wrong and that is worth knowing
before anything else on this list.

### Task 4 — compiler warnings the CI summary does not print

The CI comment prints lint, not the Kotlin compiler's own warnings. Run:

```
./gradlew compileDebugKotlin --warning-mode all
```

Report anything in `OdtDocumentWriter.kt`. Two things in particular were written blind and are
worth a look: the `unwrap()` helper added in commit 5 uses `@Suppress("DEPRECATION")` over a
`when` covering all seven `OfficeDocElement` subtypes, and the `@Suppress("UNCHECKED_CAST")` on the
list-run slice in `writeElements`.

### Task 5 — resolve the dependency versions

`audit-025` §5 records this as still unverified anywhere:

```
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep androidx.fragment
```

Report the resolved `androidx.fragment` version. `AGENTS.md` and several audits cite a constraint
whose actual resolved value has never been confirmed on any machine.

### Task 6 — measure what the sandbox cannot

* Wall-clock time for `OdtDocumentWriter.write` on Sample-6-sized input. `writeElements` now maps
  the element list through `unwrap` before writing, which is one extra allocation per save. On a
  1,268-element document (`Sample-6.odt` parsed) it should be noise, but it has never been timed.
* `Plan6cLoadingProgressTest` last reported `Sample-6.odt parse=35 ms layout=75 ms pages=20` and
  `Sample-6.docx parse=40 ms layout=53 ms pages=24` in the goldens run `38754267188`. Re-measure
  after these commits.

### Task 7 — a device or emulator pass (tier 2 / tier 4)

Plan 9 changes no UI, so there is nothing new to look at. The only device question it raises is
behavioural: open a document, edit a paragraph, save, reopen, and confirm the document still looks
right. That is task 1 done on hardware rather than in LibreOffice.

---

## 3. Known gaps — do not let these be rediscovered

1. **Page breaks are still not written.** `OfficePageBreak` has no branch in `writeElement` and
   falls through to the empty `else`. It appears in the plan §0.1's dropped table but not in §2.1's
   scope list, so it is still broken by design, and recorded rather than forgotten.
2. **Font *size* is deliberately not written.** `OfficeTextRun.fontSizeSp` is in sp and ODF wants
   pt. Shipping an unverified conversion would resize a user's text, which breaks decision D4 worse
   than omitting it. It belongs with the metrics work. Colour, highlight and hidden *are* written.
3. **`Heading_$level` as a heading fallback is unresolved.** A heading with no `styleName` still
   writes `Heading_$level`, which only resolves if the document happens to declare it. Not fixed.
4. **`detectSourceFeatures` is too coarse.** One trivial two-cell table marks a document as having
   "advanced tables" and blocks a modified save. Not Plan 9's job — widening it would change what
   Plan 7B guarantees — but it is why no fixture exercises the generator.
5. **`office:version="1.4"` with 1.2-era content.** The plan §8 flags this: if a re-opened
   document differs from the original on `office:version` alone, that is expected and should be
   asserted, not treated as a failure. LibreOffice's actual behaviour is unverified.
6. **The sealed `OfficeDocElement` class survives.** The DOCX writer still uses it; retiring it
   completes in Plan 9b.
7. **Goldens are recorded but not enforced.** `ci.yml` still passes
   `-Proborazzi.test.record=true`, which overwrites rather than compares, and `verifyRoborazziDebug`
   does not exist in the repository. The 13 PNGs merged in PR #41 are a review baseline, not a
   gate. Switching `ci.yml` to verify is a separate, deliberate commit that must not land before
   the goldens — it has not been made.

---

## 4. Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/makerandreas/papirusoffice/data/writer/OdtDocumentWriter.kt` | all five commits; the whole plan lives here |
| `app/src/test/java/com/example/Plan9RoundTripTest.kt` | **new**, 21 tests |
| `anti-slop/plan-09-save-round-trip-integrity.md | plan document, §9 carries the per-commit record |
| `anti-slop/plan-01-master-index.md` | ledger items 20–23 |
| `anti-slop/audit-025-…md` | §5.1 goldens read, §10 the two findings this plan surfaced |

No UI, no strings, no resources, no new dependencies, no build files.

---

## 5. What to report back

Per task: the command you ran, the output, and whether it matched. Anything that does **not** match
the numbers in section 1 is more interesting than anything that does — the sandbox's model of this
tree has never been compiled, and two commits already proved it wrong in ways only a compiler could
find.
