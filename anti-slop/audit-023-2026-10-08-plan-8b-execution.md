# Audit 023 (2026-10-08): Plan 8B owed work executed, and six corrections to audit-022

**Baseline:** `main` at `8796828` ("Merge pull request #37"), branch
`arena/574ad9fd-papirus-office`. All work below is eight commits on that branch, on top of
`audit-022`'s analysis, which was itself written against the same baseline.

**Gate: PR #38, CI run `37824259896` on head `d7541f2`, all three jobs success.** `Unit &
Roborazzi Snapshot Tests`, `Lint Analysis` and `Build & Package Debug APK` all green. The CI
comment reports **435 tests, 0 failed, 0 errors, 0 skipped, 43.42 s across 82 suites**, against
PR #37's 434 across the same 82, the +1 being the new reminder-cap case. The lint comment
reports **`lint 9.1.1: 0 errors, 58 warnings across 11 issue ids`**, against PR #37's `17
errors, 58 warnings across 15 issue ids` on head `307f01a` (run `37719095501`). An earlier run
on this branch, `37822790268` on head `acf9aaf`, reads the same three conclusions and the same
two report shapes. §8 separates what was executed here from what only those runs could confirm,
and §8.2 records where the prediction in §8.1 was wrong.

**Scope:** execute the six steps `audit-022` §11 recommended, plus the owner-approved
deletion of every unused-code item previously marked. Where executing a step contradicted
the recommendation, the contradiction is recorded rather than smoothed over.

**Evidence tiers used.** Tier 1 (this sandbox) for every command quoted below. Tier 3
(GitHub Actions) only for the PR #37 reports already in the repository. No claim in this
document rests on a build, test or lint result this session did not produce or did not
already have a report for.

---

## 1. The lint pass

Two commits: one closing all seventeen errors with `abortOnError` still `false`, one
flipping it. Splitting them makes the toggle bisectable, so the fixes can be reverted
without losing the gate or vice versa.

### 1.1 The Fatal, `InvalidFragmentVersionForActivityResult`

Closed with a dependency constraint, not a lint disable and not a direct dependency.

| | |
|---|---|
| Declared in | `gradle/libs.versions.toml`, `fragment = "1.8.9"` plus an `androidx-fragment` library alias |
| Applied at | `app/build.gradle.kts`, `constraints { implementation(libs.androidx.fragment) }` |
| Why a constraint | The Gradle user guide states a constraint sets a version requirement without adding the module as a dependency, and that if the module is not brought in transitively the constraint is a no-op. Nothing under `app/src/main/java` imports `androidx.fragment`, so this is the narrowest change that moves the resolved coordinate |
| Why the alias exists | `TestDependencyGuardTest.everyCatalogAccessorUsedByTheBuildScriptExistsInTheVersionCatalog` fails the build on a `libs.*` accessor with no catalog alias |

**1.8.9 and not 1.9.1.** Both POMs were read from `dl.google.com`. Fragment 1.8.9 requires
activity 1.8.1, lifecycle 2.6.1, core-ktx 1.2.0, collection 1.1.0 and kotlin-stdlib 1.8.22,
all already superseded by this project (activity 1.10.1, lifecycle 2.8.7, core-ktx 1.18.0,
Kotlin 2.2.10), so it moves fragment alone. Fragment 1.9.1 requires lifecycle 2.10.0 and
collection 1.4.2, which would drag lifecycle 2.8.7 up against Compose BOM 2024.09.00. A
version bump that is supposed to silence one lint id should not carry a framework upgrade.

**Known limit, written into the comment at the call site.** The androidx check compares
versions as Kotlin strings, so a future `1.10.0` sorts below `1.3.0` and re-fires on a
newer version. The comment says the answer then is a `lint { disable }` entry with that
comment as its reason, never a downgrade.

### 1.2 The fifteen `StringFormatMatches`

Fourteen `%1$s` placeholders that receive an `Int`, across eleven `strings.xml` entries. Ten
are a one-character change and are now `%1$d`.

The eleventh was more than a placeholder. `toast_document_properties_room_db_n_file_meta_filename`
carried raw Kotlin concatenation residue, a literal `" +` and 65-space indents, and rendered
513 characters of source code into a `Toast.LENGTH_LONG`. It is rewritten as one line under a
new name, `toast_document_properties`, keeping the eight arguments in the order the call site
at `InkyModule.kt:3452` already passes them, and the "(Room DB)" implementation detail is
dropped from user-visible copy. The call site is updated; the old name has no remaining
reference in `app/src`.

**Measured after the edit:** the file parses as XML, still holds 746 `<string>` entries, and
has no duplicate names.

### 1.3 `StaticFieldLeak`

`PathSettings(private val context: Context)` read `context` only in `init`, at two lines, and
its sole owner is `PapirusAssetEngine`, a process-lifetime `object`. The property is now a
plain constructor parameter, with the reason and a do-not-add-`val`-back note in the KDoc. The
second `StaticFieldLeak`, at `StorageProvider.kt:127`, is closed by §2.

### 1.4 `AppLinkUrlError`

Suppressed at the `<intent-filter>` with `tools:ignore="AppLinkUrlError"` and an eight-line
comment, not fixed. The filter declares `android:scheme="file"` with twelve `pathPattern`
entries and no `data` authority; an intent filter with no authority ignores the URI authority
entirely, which is what lets any file manager match it, including the empty authority of a
`file:///` URI. Adding `android:host="*"` would require a non-empty host and can stop matching
those. The check is written for `http`/`https` App Links.

The suppression is on the filter rather than the `<data>` element so it covers the reported
location whichever element lint attributes it to. Placement verified by parsing the manifest:
four intent filters, and the annotated one is the only filter carrying `scheme=file` plus
twelve `pathPattern` entries.

### 1.5 `abortOnError = true`

Second commit. `warningsAsErrors` stays `false`: the 58-warning tail, 35 of them `UseKtx`, is
a separate and much larger pass.

---

## 2. The dead-code sweep

| | |
|---|---|
| Files deleted | `data/StorageProvider.kt` (136 lines), `data/undo/KeyboardShortcutHandler.kt` (55 lines) |
| Import lines removed | **121 across 46 files**: 103 in the sweep commit across 45 files, and the 18 in `InkyModule.kt` that the lint-pass commit already carried |
| Import lines that left with the deleted files | 17 |
| Re-scan after the sweep | **0 unused imports across 0 files** |

`audit-022` measured 122 across 47 files on the pre-sweep tree. That measurement was correct;
the executed sweep is 121 across 46 because `StorageProvider.kt` took its one unused import
with it. The audit's headline number and the sweep's are different numbers for the same tree
and both are right.

Every removed line in the diff is an import line. Verified rather than assumed: filtering the
`.kt` diff for removed lines that are not `import` lines returns exactly two, and both are
intentional edits (`InkyModule.kt`'s call-site rename and `PathSettings`'s signature).

**Kept deliberately.** `PapirusApplication` and `CrashNotificationReceiver` are flagged by the
same reference scan and are named from `AndroidManifest.xml` at `:14` and `:86`. Tier B stays:
`core/fonts/FontScanner.kt`, `FontSyncService.kt`, `data/DocumentCoreEngines.kt` and
`data/writer/SwTextAttr.kt` belong to parked plans.

---

## 3. The re-tightened assertions

Four test files, one commit. Each restored assertion is backed by a measurement made here, not
by the pre-`aec1672` text.

| File | Restored | Measured from |
|---|---|---|
| `DocxNumberingReaderTest` | `numFormat` as `"1"`, plus the five exact labels `"BAB 1"`, `"1.1"`, `"1.1.1"`, `"BAB 2"`, `"2.1"` | `mapNumFmt` at `DocxNumbering.kt:439`, and the shipped `NumberingCounterState` executed under `kotlinc-jvm` |
| `DocxAuthoredShapeTest` | the three chapters carry 1, 2, 3 in document order; TOC entries keep the cached `BAB` labels | `Sample-6.docx`: 42 paragraphs inherit `numId 15` through Judul1/Judul2/Judul3, exactly three at `ilvl 0`, in the order PENDAHULUAN, PEMBAHASAN, PENUTUP |
| `DocxFieldHyperlinkTest` | the `TOC \o` instruction check alongside `PAGEREF`; the "must remain" text narrowed back to the two front-matter entries | `aec1672` replaced one with the other; the fixture needs both, and the neighbouring case in `DocxAuthoredShapeTest` already asserts the pair |
| `DocxSectionsTest` | the arabic restart requires `pageNumberFormat == null` | `Sample-6.docx` has five `sectPr` blocks: the first `start=1`/`lowerRoman`/`titlePg`, the second `start=1` with no `fmt`, the last three with neither |

**The `"decimal"` correction.** `aec1672` deleted `assertEquals("decimal", lvl1.numFormat)`.
It did not correct it. `mapNumFmt("decimal")` returns `"1"`, so the deleted assertion would
fail on today's code, and deleting it is what hid that. The restored form is
`assertEquals("1", lvl1.numFormat)`.

**Not restored, deliberately.** The per-level TOC entry counts. Which level an entry lands on
is a property of the parser's `authoredIndexes` model, and an attempt to derive the counts
from the fixture did not reproduce that model: a structural scan of `Sample-6.docx` found the
`TOC \o` instruction inside a paragraph that also carries entry text, and no PAGEREF-bearing
paragraph between it and the field end. Pinning a number this sandbox cannot execute would be
a guess in an assertion's clothes. The commit message says so.

---

## 4. The reminder cap

LibreOffice Writer Guide 26.2, Chapter 1, "Setting reminders", PDF page 37: "You can set up to
5 reminders in a document; setting a sixth causes the first to be deleted."

`ReminderManager` had no size check. Its only unit test inserted one reminder and asserted
size 1. `plan-01-master-index.md` §3.3 claimed the checklist already tested the cap; §3.3 is
corrected in the same commit group.

### 4.1 Why the eviction key is not `timestamp`

`audit-022` recommended keying eviction on `DocumentReminder.timestamp`. That is wrong, and
executing the code is what showed it. Two problems:

1. `timestamp` ties. Six reminders set in a loop can land inside one millisecond, and "the
   first to be deleted" then has no defined answer.
2. `getReminders()` is sorted by `paragraphIndex`, so list position is not insertion order
   either.

The implementation adds a monotonic `DocumentReminder.sequence` and evicts on it.

### 4.2 Executed, not asserted

The shipped `ReminderManager.kt` was compiled with `kotlinc-jvm` 2.4.20 on Temurin 25.0.2 and
run against fourteen checks:

```
PASS  starts empty
PASS  five fit
PASS  MAX_REMINDERS is 5
PASS  sorted by paragraph while full
PASS  cap holds after sixth
PASS  the first inserted is gone
PASS  the five newest survive in paragraph order
PASS  cap holds after seventh, second-inserted evicted
PASS  replace caps at five
PASS  replace evicts by insertion order
PASS  re-set of an existing position does not grow the list
PASS  single reminder note
PASS  nextReminder offset
PASS  previousReminder offset
ALL PASS
```

**Two of those fourteen failed on the first run, and the code was right both times.** The
harness notes were named "first", "second", "third" while being inserted in the order third,
first, fifth, second, fourth, so "the oldest" was the reminder named "third"; and a second
hand-computed expectation for the replace case was off by one insertion. Both expectations
were corrected before the JUnit test was written, and `reminderCapIsFiveAndTheSixthEvictsThe
FirstInserted` carries the corrected ones with insertion-order names instead of ordinal ones.
The pre-existing single-reminder and Prev/Next behaviour is asserted unchanged in the same run.

### 4.3 The gap that measuring opened

`getReminders`, `nextReminder`, `previousReminder` and `removeReminder` have **zero call sites
outside `ReminderManager.kt`**. The only production reference is `reminderManager.setReminder`
in `InkyModule.kt`. So Papirus can set a reminder and nothing in the UI can reach it again: no
Reminder filter in the Navigator, no Prev/Next. `plan-01-master-index.md` §3.3 listed both as
present. §3.3 now says the cap is done and the rest is unassigned.

---

## 5. The snapshot claim

`AGENTS.md:297` already told the truth: with no flag at all `RoborazziTaskType.None` makes
`captureRoboImage` return before capturing, and "the five snapshot classes are composition
smoke tests rather than screenshot tests". What was missing was any statement inside the five
classes, where a reader sees thirteen `captureRoboImage` calls and no reason to doubt they
guard pixels. Each class now carries a KDoc naming what it renders, that nothing compares the
PNGs, what a green run does and does not prove, and why the goldens are deferred.

### 5.1 Two parts of the recommendation withdrawn

**`onRoot().assertExists()` is vacuous.** `audit-022` §2.3 item 2 recommended one assertion per
class, "for example `composeTestRule.onRoot().assertExists()`", on the grounds that a snapshot
class passes even if the module renders an empty box. It does not: `captureRoboImage(onRoot())`
calls `fetchSemanticsNode()`, which throws when the composition produced nothing. The assertion
would be a line that cannot fail. Not added, and the reasoning is in the KDoc instead.

**`PROJECT_CONTEXT.md` carries no claim to rename.** `audit-022` named it alongside `AGENTS.md`
as calling the classes screenshot or regression tests. A grep for screenshot, snapshot and
Roborazzi over that file returns nothing. There was no prose claim to rename, only the five
undocumented classes.

Deferring the goldens and the switch to `verifyRoborazziDebug` stands, for the reason
`audit-022` gave: the runner pin landed in the same commit group and the Compose BOM upgrade is
still ahead, and both move pixel rendering.

---

## 6. The runner pin

All four jobs pinned to `ubuntu-24.04`: test, lint and build in `ci.yml`, build in
`build.yml`. Both files parse as YAML with the change and every job reports
`runs-on: ubuntu-24.04`.

**The date, from the source rather than from an earlier audit.** `actions/runner-images#14748`,
"[Ubuntu] `ubuntu-latest` label will use Ubuntu 26.04 in November 2026", opened 2026-09-17, still
open: "This change will be rolled out over a period of several weeks beginning October 19,
2026. We plan to complete the migration by November 19, 2026." The affected images are Ubuntu
24.04 and Ubuntu 26.04; the Java default stays 17 on both. So the earlier audits' "on
2026-10-19" is the start of a month-long window, not a single-day switch, and the pin has to
hold until the migration is finished rather than until one date has passed.

The runner-images table still lists Ubuntu 24.04 as "`ubuntu-latest` or `ubuntu-24.04`", so
the two labels are the same image today and this changes nothing now.

---

## 7. Corrections to audit-022

Six claims in that document did not survive being executed. It is left as the analysis of
record with a banner pointing here rather than being rewritten.

| # | audit-022 said | Measured |
|---|---|---|
| 1 | "the 122 unused imports across 47 files", to be swept | Correct as a measurement of the pre-sweep tree. The executed sweep removes **121 across 46**, because `StorageProvider.kt` took its one unused import with it. 17 further import lines left with the two deleted files |
| 2 | "the fix keys eviction on `DocumentReminder.timestamp`" | `timestamp` ties within a millisecond. Keyed on a monotonic `DocumentReminder.sequence` instead |
| 3 | "add one assertion per snapshot class, for example `onRoot().assertExists()`" | Vacuous. `captureRoboImage(onRoot())` already resolves the root semantics node and throws on a missing one |
| 4 | "`PROJECT_CONTEXT.md` and any plan text that calls them screenshot or regression tests" | `PROJECT_CONTEXT.md` has no screenshot, snapshot or Roborazzi mention. `AGENTS.md:297` was already correct |
| 5 | `81fbd1f` listed among the commits that relaxed assertions | It relaxed nothing. It corrected a wrong roman-numeral expectation against a fixture that stores decimal plus a "BAB " prefix, and kept the exact labels; `aec1672` relaxed them afterwards |
| 6 | `1a1b8b6` described as a relaxation | Half of it was. Its `DocxTableTocTest` change was a correct tightening: all 46 TOC bookmarks in `Sample-6.docx` are `_TOC`, uppercase, so dropping the `_Toc` alternative removes a branch the data cannot take. Only its `DocxSectionsTest` half was a needless relaxation, and that one is re-tightened |

Two smaller ones. `audit-022` §1.3 shows the rewritten resource keeping the old name
`toast_document_properties_room_db_n_file_meta_filename`; it shipped as
`toast_document_properties`, and the "(Room DB)" implementation detail came out of the copy.
And "the 49-file dead-code sweep" is 48 files touched: 46 with import removals and 2 deleted.

---

## 8. What was executed here and what only CI can confirm

**Executed here (tier 1).**

| Check | Result |
|---|---|
| `strings.xml` after 11 edits: XML parse, entry count, duplicate names | parses, 746 entries, no duplicates, old name gone |
| `AndroidManifest.xml` after the suppression: XML parse and filter attribution | parses; the annotated filter is the only one with `scheme=file` plus 12 `pathPattern` entries |
| `gradle/libs.versions.toml` after the alias: TOML parse | parses; `versions.fragment = 1.8.9`; 57 libraries, 46 versions |
| Both workflows after the pin: YAML parse | both parse; all four jobs `ubuntu-24.04` |
| Unused-import re-scan over 292 files | 0 unused imports |
| Shipped `ReminderManager.kt` compiled and run | 14 of 14 checks pass, including the three pre-existing behaviours |
| Real `TestDependencyGuardTest` compiled and run, both working directories | 2 passed / 0 failed from `app/` and from the repo root |
| Negative control: `androidx-fragment` alias removed | the accessor test fails naming `libs.androidx.fragment` and the missing alias |

**Not executable here.** There is no Gradle and no Android SDK in this sandbox, so
`./gradlew` was never run and everything in the list below came from the PR #38 run rather
than from this session:

* `:app:lintDebug` and the counts, confirmed at `0 errors, 58 warnings across 11 issue ids`.
* `compileDebugKotlin` over the 46 files whose imports changed, and `compileDebugUnitTestKotlin`
  over the five edited test files: both green, since the test job and the packaging job passed.
* The re-tightened assertions: `DocxNumberingReaderTest` 4/0/0/0, `DocxAuthoredShapeTest`
  3/0/0/0, `DocxSectionsTest` 2/0/0/0, `DocxFieldHyperlinkTest` 5/0/0/0, and
  `DocumentEnginesUnitTest` 26/0/0/0 including the new cap case.
* `TestDependencyGuardTest` 2/0/0/0 on the runner, which is where the working directory is
  `app` and the defect in §8.1 was already masked.

**Still unverified anywhere.** The resolved fragment coordinate. The lint job going green with
`abortOnError = true` shows the Fatal no longer fires, which is what the constraint was for,
but it does not print the version Gradle settled on. The owner's check remains
`./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep androidx.fragment`.

### 8.2 Where the prediction was wrong

`audit-022` predicted the fix would take the report from `17 errors, 58 warnings` to "about
`2 errors, 62 to 64 warnings`". The actual is **0 errors, 58 warnings**.

| | predicted | actual |
|---|---|---|
| errors | about 2 | **0** |
| warnings | 62 to 64 | **58** |
| issue ids | not predicted | **11**, from 15 |

The mechanism was right and the arithmetic was not. `PluralsCandidate` did rise from 2 to 6,
the predicted +4 from `%1$d` before a noun. `StaticFieldLeak` did fall to zero. `UseKtx` fell
by 2, and the two are identifiable: they were `StorageProvider.kt:21:53` and `:31:54`, both
gone with the deleted file, every other `UseKtx` location in the two reports matching once the
line shifts from the import removals are accounted for. So `-2 -2 +4 = 0` and the total held at
58 rather than rising. The two errors that were predicted to survive did not: the
`tools:ignore` covered `AppLinkUrlError` completely rather than partially, and no second
`StringFormatMatches` was left behind.

### 8.1 One defect found by running the guard

`TestDependencyGuardTest.resolve()` prefers `build.gradle.kts` and falls back to
`app/build.gradle.kts`. Gradle's test task runs with the working directory at `app`, where both
names are the same file, so CI is correct. Run from the repository root, `build.gradle.kts`
exists too and is the root script. The two test methods then fail in opposite and both
unhelpful directions: the mockk guard fails naming a dependency that `app/build.gradle.kts:294`
declares, and the catalog-accessor guard scans the root script, finds no library accessors, and
passes having checked almost nothing. That is the failure mode the guard exists to prevent,
inside the guard.

The first run of the guard in this session produced exactly that false pass, and the reported
"pass" was against the wrong file. Fixed by resolving the build script by content: `appBuildScript()`
takes the first candidate whose text contains an `android {` block. From the repository root the
pair went 1 passed / 1 failed to 2 passed / 0 failed; from `app/` it passes either way.

This was not one of the six approved steps. It is a change to a test, made because executing
the approved work exposed it, and it is called out here rather than folded in silently.

---

## 9. What is still owed

1. ~~The pull-request run.~~ Done: PR #38, run `37824259896`, all three jobs green, both
   comments read, and the prediction checked in §8.2 rather than agreed with.
2. **The 58-warning tail.** 35 `UseKtx`, 6 `VectorRaster`, 3 `IconLocation`, 2 each of
   `AndroidGradlePluginVersion`, `ApplySharedPref`, `DefaultLocale`, `PluralsCandidate` and
   `StaticFieldLeak` (the latter now expected to fall to zero with this branch), and singles.
   A separate pass with its own budget.
3. **The fragment constraint's owner check** (§8) before this is called closed.
4. **Reminder Prev/Next and the Navigator filter** (§4.3). UI work, unassigned.
5. **Goldens and `verifyRoborazziDebug`** after the Compose BOM upgrade lands.
6. **Plan 9**, save round-trip integrity, which is what all of this was clearing the way for.

---

## 10. AGENTS.md

`AGENTS.md`'s header reserves edits to the owner. Two paragraphs in "Reading the lint report"
were factually stale after this branch and are updated, minimally, under the owner's direction
to carry out every recommended step: the one stating that `abortOnError` is `false` and that a
green job therefore proves nothing, and the one listing the seventeen errors as open. Nothing
else in the file is touched, and both edits are restated here so they can be reviewed or
reverted without reading the file.

`scripts/lint-dump-comment.py` had the same staleness in code rather than prose: it asserted
`abortOnError = false` in three hardcoded places, so the report PR #38 first posted told the
reader that a lint error would not fail the job one sentence after reporting zero errors from a
job gated on finding none. The value is now read out of `app/build.gradle.kts` at run time,
scoped to the `lint {` block so an `abortOnError` elsewhere in the DSL is not picked up, and
the sentence follows the flag. Verified against four cases: the current tree returns `true`,
the pre-change script at `8796828` returns `false`, a script with no lint block returns `None`,
and a script whose only `abortOnError` sits in a `packaging` block returns `None`. The report
on head `d7541f2` carries the corrected sentence.
