# Audit 025 (2026-10-15): the debris left after the 8B closeout, and whether Plan 9 can start

> **Partly executed, same day.** The owner approved the four steps in §11 and three of them are
> done on `arena/20d3dfd7-papirus-office` (PR #40): the housekeeping deletion, the seven imports,
> and Plan 9's plan document. **Gate: `ci.yml` run `38750210445` on head `ffcb277`, all three
> jobs success, `Unit tests: 435 run, 0 failed, 0 errors, 0 skipped (46.79s across 82 suites)`
> and `lint 9.4.1: 0 errors, 59 warnings across 7 issue ids`.** Both predicted numbers landed
> exactly: the test count held at 435, proving the two deleted files and seven imports were dead
> as measured, and the warning count went 62 to 59 and 8 ids to 7, the three `IconLocation` sites
> leaving with the JPGs. The fourth step, recording the goldens, is blocked on the owner's
> credentials rather than on the repository (§5). This file is left as the analysis of record
> with this banner rather than being rewritten; `plan-01-master-index.md` §6 item 19 carries the
> execution record.

**Baseline:** `main` at `53d61ac` ("Add files via upload", 2026-10-12 18:42 +0900), branch
`arena/20d3dfd7-papirus-office`. `main` carries exactly one commit, so every SHA the earlier
audits cite is unresolvable in a fresh clone (§7). Where this document needed the pre-squash
tree it fetched `59800be`, the PR #39 merge commit, from `origin`.
**Gate:** `ci.yml` run `38231653018` on head `53d61ac` (2026-10-12T09:42:49Z), all three jobs
success: `Unit & Roborazzi Snapshot Tests`, `Lint Analysis`, `Build & Package Debug APK`. That is
the newest verification of `main`, and `main` has not moved since, so **the tree at HEAD is
green**. No later run tests anything: the three runs since are `build.yml` on its 02:00 UTC
schedule, which builds and publishes but does not run the suite.
**Newest lint evidence:** the `### Lint report for 34639ce` comment on PR #39 (run
`37860478309`), read with `gh api repos/makerandreas/Papirus-Office/issues/39/comments`:
`lint 9.4.1: 0 errors, 62 warnings across 8 issue ids`.
**Scope:** (1) inventory what the 8B closeout left behind and sort it by whether it blocks Plan
9, (2) read the 39-pull-request record as a sequence, (3) re-measure the dead-code and warning
ledgers against today's tree, (4) measure the documentation drift, (5) answer the owner's
question: clean first, or go to Plan 9.
**Method:** `git diff` against the pre-squash merge commit, `gh api` and `gh run list` for run
metadata and pull-request comments, `git ls-remote` for branch heads, and four Python scans over
the working tree (unused imports with a Kotlin-aware comment/string/backtick/char-literal
stripper, external-reference counts per declaration, `strings.xml` composition, warning-site
spot checks). No Gradle, no Android SDK, no JDK, no Robolectric, no Compose and no MockK exist in
this sandbox (`AGENTS.md`), so nothing was built or run here. Every number below is either a
tier-1 measurement of files in this checkout or a tier-3 figure from a run id named beside it.

---

## 0. Findings in one screen

| # | Finding | Evidence | Blocks Plan 9? |
|---|---|---|---|
| A | **The 2026-10-12 re-upload is a 4-file delta against the merged PR #39 state, and two of the four are the dead files PR #38 deleted.** | `git diff 59800be HEAD --stat` | No, but it is the cheapest thing on this list and it undoes a decision |
| B | **The dead-code ledger re-measured: 4 dead files, of which 2 were already deleted once.** `StorageProvider.kt`, `KeyboardShortcutHandler.kt`, `FontScanner.kt`, `FontSyncService.kt`, plus `data/writer/SwTextAttr.kt` | §3, external-reference counts | No |
| C | **`audit-023`'s "0 unused imports" does not hold.** 7 unused import lines across 6 live files, plus 1 inside a dead file. The earlier scan appears to have counted comment and string occurrences as uses | §3.2 | No |
| D | **The 62-warning backlog is exactly as `audit-024` left it, and the tree still carries every site.** All 8 ids and their line numbers re-checked | §4 | No |
| E | ~~**Goldens are unblocked and still not recorded.**~~ **CLOSED 2026-10-15.** Run `38754267188` recorded 13 PNGs; PR #41 merged into `main`. What remains open is narrower: `ci.yml` still *records* rather than *verifies*, so the baseline is review-only, not enforced | §5, §5.1 | No |
| F | **The documentation drifted again.** 8 measured stale `file:line` claims, one of them citing line numbers in a file that is 150 lines shorter than the citation | §6 | No, but each one is a future wrong fix |
| G | **The audit trail is severed.** `main` is one commit; `8796828`, `d7541f2`, `aec1672`, `912de8e`, `ef2945b`, `6b2dfea`, `c61e970`, `307f01a` all fail `git cat-file -e` | §7 | No, but it costs every future audit |
| H | **The reminder is still write-only.** `setReminder` has one call site (`InkyModule.kt:3943`); `getReminders`, `nextReminder`, `previousReminder` and `removeReminder` have zero outside `ReminderManager.kt` | §9 | No. Plan 11 owns the UI |
| I | **Plan 9's own pre-change gate has not been written.** `plan-04-to-09-writer-fidelity.md` § Plan 9 says "scope is firmed by a short plan document before this PR starts; do not start it from this paragraph alone", and no such document exists | §10 | **Yes. This is the real blocker** |
| J | **The default template carries styles and no content part.** `Default.ott` (8,729 B) has `mimetype`, `META-INF/manifest.xml`, `meta.xml`, `styles.xml`, `Thumbnails/thumbnail.png` — no `content.xml`, no `settings.xml`. Its manifest declares no missing part, so it is consistent, not corrupt. `InkyModule.kt:3426` uses it as the fallback current path. Every sample file *does* have a `content.xml`, so a round-trip test built only on samples never exercises creating one | §5.1 | No, but it should shape Plan 9's test list |

**The short answer to the owner's question:** the debris is real but almost none of it gates Plan
9. Exactly two things do, and they are both cheap. Recommendation in §11.

---

## 1. The 2026-10-12 re-upload, measured

`git fetch origin 59800befe7ea5cb344d27f9954bedf4e8d6a3583` then `git diff --stat 59800be HEAD`
returns four files and nothing else:

```
 app/src/main/AndroidManifest.xml                   |   2 +
 .../papirusoffice/data/StorageProvider.kt          | 136 +++++++++++++++
 .../data/crash/CrashHandlerManager.kt              |  13 +-
 .../data/undo/KeyboardShortcutHandler.kt           |  55 +++++++++
 4 files changed, 205 insertions(+), 1 deletion(-)
```

Everything else in the tree is byte-identical to the merged PR #39 state. That is the good news:
the re-upload did not silently revert the lint pass, the runner pin, the fragment constraint, the
Compose upgrade or the reminder cap. Each was re-verified in the working tree:

| PR #38 / #39 change | Verified at HEAD |
|---|---|
| `abortOnError = true` | `app/build.gradle.kts:174`, with `warningsAsErrors = false` at `:177` |
| `constraints { implementation(libs.androidx.fragment) }` | `app/build.gradle.kts:254`, version `1.8.9` at `gradle/libs.versions.toml:15` |
| `toast_document_properties` rewrite | `strings.xml:516`, still 746 entries, 0 `<plurals>` |
| `PathSettings(context: Context)`, no `val` | `data/framework/PathSettings.kt:17` |
| `tools:ignore="AppLinkUrlError"` | `AndroidManifest.xml:79`, on the filter with `scheme=file` |
| Compose BOM `2026.09.00`, compileSdk 37 | `libs.versions.toml:24`, `app/build.gradle.kts:20` |
| AGP 9.4.1, Gradle 9.8.1 | `libs.versions.toml:2`, `gradle/wrapper/gradle-wrapper.properties` |
| `runs-on: ubuntu-24.04` on all four CI jobs | `ci.yml:33,117,177`, `build.yml:26`, `goldens.yml:50` |
| `ReminderManager.MAX_REMINDERS` | `DocumentEnginesUnitTest.kt:243` asserts it |

### 1.1 The four files, one at a time

**(a) `data/StorageProvider.kt` (136 lines) is back.** It declares `interface StorageProvider`
and `class SafStorageProvider`. `grep -rn` for both names across `app/src`, excluding the file
itself, returns **0 external references**. This is the file `audit-022` §3.2 marked safely
deletable and `audit-023` §2 deleted.

**(b) `data/undo/KeyboardShortcutHandler.kt` (55 lines) is back.** It declares
`object KeyboardShortcutHandler` and `fun Modifier.undoRedoKeyboardShortcuts(...)`. Both names
have **0 external references**, which is the second half of `audit-022`'s finding: the Ctrl+Z /
Ctrl+Y wiring was never attached to a `Modifier`.

Both files compile, because the last `ci.yml` run on `53d61ac` passed `compileDebugKotlin`. Being
compilable is not the same as being wanted.

**(c) `AndroidManifest.xml` gained two permissions** at lines 5 and 6:
`android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`. Neither was in the
PR #39 tree. Nothing in this audit contradicts them: `PapirusApiClient.kt` builds a Retrofit
client and `TemplateSearchRepository.kt` calls an AI service, so network access is plausible as a
deliberate change. But they arrived inside an "Add files via upload" commit with no plan entry and
no audit note, and an app that ships a document editor is exactly the app where a new permission
should be a decision rather than a side effect.

**(d) `CrashHandlerManager.kt` gained a runtime permission guard.** `sendCrashNotification` now
carries `@SuppressLint("NotificationPermission")` at `:77`, and the `notificationManager.notify(...)`
call at `:166` is wrapped in
`if (Build.VERSION.SDK_INT < TIRAMISU || ContextCompat.checkSelfPermission(...) == GRANTED)`,
logging `Log.w` and dropping the notification otherwise. The guard is correct and the suppression
is the right shape for it. The note for the ledger is that the project's own `AGENTS.md` rule 2
under "Reading the lint report" warns that a `lint { disable }` entry AGP does not know silences
nothing; a `@SuppressLint` is not that, but it is the same class of decision and should be named
somewhere a future audit can find it. It is named here.

---

## 2. The debris inventory, sorted by whether it gates Plan 9

Three tiers. Tier 1 items block Plan 9 or change what Plan 9's tests mean. Tier 2 items are
independent and can run before, during or after. Tier 3 items are cosmetic.

### Tier 1: do these before Plan 9

| # | Item | Size | Why it gates |
|---|---|---|---|
| 1 | **Write Plan 9's own plan document** (§10). `plan-04-to-09` explicitly forbids starting from its paragraph. | one document | Without it, Plan 9 has no acceptance list, no file list, and no gate to enter |
| 2 | **Record the goldens** by dispatching `goldens.yml` once (§5). | one workflow dispatch, owner-only | Group B of the warning backlog is the one that can change what the app draws, and `audit-024` recommends reviewing it against goldens that do not exist yet |

**Status of the tier 1 and tier 2 items on 2026-10-15**, recorded so the next session starts from
here rather than re-deriving it. Items 1, 3, 4 and 6 are done on
`arena/20d3dfd7-papirus-office` and are compiled by CI, not by this sandbox. Item 2 is blocked on
the owner's credentials (§5). Items 5, 7, 8, 9 and 10 are untouched and none of them gates Plan 9.

### Tier 2: independent, safe to defer past Plan 9

| # | Item | Size | Note |
|---|---|---|---|
| 3 | Re-delete the two resurrected dead files (§1.1a, §1.1b) | 191 lines, 2 files | Cheapest item here. Doing it twice is the argument for doing it in the same PR as (4) |
| 4 | Delete the 7 unused imports across 6 live files (§3.2) | 7 lines | Nothing else belongs in the diff; the compiler is the reviewer |
| 5 | The 62-warning backlog, as the three PRs `audit-024` sequenced (§4) | 62 sites | Group B is the only one that can change pixels, which is why it wants the goldens first |
| 6 | The three unreferenced JPGs, 3,040,550 bytes (§4, `IconLocation`) | 3 files | Owner decision: it is a deletion |
| 7 | Reminder Prev/Next and the Navigator Reminder filter (§9) | UI work | Unassigned since `audit-022`. Plan 11 territory |
| 8 | `plan-3d-strings-sweep.md`, 854 literals | one planned PR | No slot chosen since 2026-10-04 |
| 9 | Documentation drift, 8 measured items (§6) | edits to 3 files | `AGENTS.md` is owner-only |
| 10 | Delete the 37 remote `arena/*` branches (§8) | 37 refs | All from merged or closed pull requests |

### Tier 3: cosmetic

| # | Item |
|---|---|
| 11 | The severed audit trail (§7). Not fixable in the branch; fixed by not squashing again, and by recording SHAs as run ids from now on |
| 12 | `docs/` is 215 MB (`ooxml` 138 MB, `lo-guides` 64 MB, `odf` 13 MB). All are cited reference material and none is debris; recorded so the size is a decision rather than a surprise in a clone |

---

## 3. The dead-code ledger, re-measured against HEAD

External references are counted with `grep -rn "\b<name>\b" app/src --include=*.kt`, excluding the
declaring file. A name that appears only in its own declaration, or only inside a KDoc comment,
counts as zero.

| Declaration | File | External refs | Verdict |
|---|---|---|---|
| `interface StorageProvider`, `class SafStorageProvider` | `data/StorageProvider.kt` | **0** | dead, and already deleted once |
| `object KeyboardShortcutHandler`, `Modifier.undoRedoKeyboardShortcuts` | `data/undo/KeyboardShortcutHandler.kt` | **0** | dead, and already deleted once |
| `class FontScanner` | `core/fonts/FontScanner.kt` | **0** | dead, parked on Plan 10 |
| `class FontSyncService` | `core/fonts/FontSyncService.kt` | **0** | dead, parked on Plan 10 |
| `abstract class SwTextAttr` and its ten `SwFormat*` subclasses | `data/writer/SwTextAttr.kt` | **0** | dead, and easy to misread (see below) |
| `class DocumentCoreEngines` holders | `data/DocumentCoreEngines.kt` | `EditingEngine` **13**, `SelectionEngine` **18**, `CaretNavigation` **6**, `DocumentServices` **4**, `TableEngine` **3**, `FieldEngine` **2**, `DocumentCommand` **5** | **not dead.** See the correction below |
| `DocumentTransaction`, the six `*Command` classes, `DrawingLayer`, `FloatingObject`, `DrawingObjectType`, `SearchResult` | `data/DocumentCoreEngines.kt` | **0** each | dead declarations inside a live file |
| `SwTextAttr`, 27 references | `data/SwDocEngine.kt:162` | n/a | a **different type** with the same simple name |

**Correction to `audit-023` §2.** It lists `data/DocumentCoreEngines.kt` among the files that
"belong to parked plans" in the dead-code tier. The file is load-bearing: `EditingEngine` has 13
external references and `SelectionEngine` has 18. It is not a dead file. What is dead is a subset
of its declarations. A future sweep that deletes the file would break the editor.

**The `SwTextAttr` name collision.** `data/SwDocEngine.kt:162` declares
`sealed class SwTextAttr` in package `com.makerandreas.papirusoffice.data`, and
`data/writer/SwTextAttr.kt:6` declares `abstract class SwTextAttr` in package
`com.makerandreas.papirusoffice.data.writer`. The 27 references in `SwDocEngine.kt` and
`SwNodes.kt` all resolve to the first. A reference count on the simple name reports the second as
used when it is not. Any sweep must match on the fully qualified name.

### 3.1 Correction to `audit-023` §2's "0 unused imports"

A re-scan of all **296** `.kt` files under `app/src` finds **7 unused import lines across 6 live
files**, plus 1 inside the resurrected `StorageProvider.kt`:

| File | Line | Import | Checked by |
|---|---|---|---|
| `ui/components/ActionsToUndoSubpage.kt` | 23 | `androidx.compose.material.icons.rounded.Redo` | only occurrences are two comments, `:92` and `:105` |
| `ui/components/ActionsToUndoSubpage.kt` | 24 | `androidx.compose.material.icons.rounded.Undo` | only occurrences are two comments, `:27` and `:40` |
| `ui/options/PapirusOfficeOptionsScreen.kt` | 24 | `androidx.compose.ui.graphics.Color` | the only occurrence is the string `"Dynamic Color (Material You)"` at `:761` |
| `data/PapirusConfigManager.kt` | 6 | `android.content.SharedPreferences` | four occurrences, all comments (`:225`, `:237`, `:303`, `:323`); the live calls are `getSharedPreferences` and `syncToSharedPreferences`, which are different tokens |
| `data/framework/PapirusClipboardEngine.kt` | 8 | `android.net.Uri` | two occurrences, both comments (`:96`, `:130`) |
| `data/framework/PrintingFramework.kt` | 14 | `android.print.PrintManager` | two occurrences, both comments (`:21`, `:263`) |
| `data/StorageProvider.kt` | 7 | `java.io.File` | zero occurrences; the file is dead anyway |

Three classes of false positive had to be removed from the scanner before this list was
trustworthy, and they are recorded because the next person to write this scan will hit them:

1. **Kotlin string templates.** `sb.append("Generated at: ${SimpleDateFormat(...).format(Date())}")`
   puts live code inside a string literal. A scanner that blanks string contents reports
   `SimpleDateFormat` and `Date` as unused in `CrashLogsScreen.kt:284` when they are not.
2. **Char literals.** `raw.trim('\'', '"')` at `SvXMLImport.kt:296` contains a `"` inside a char
   literal, which opens a phantom string and swallows the rest of the file.
3. **Backtick identifiers.** `` fun `sample 6 preface no-man's-land headings...`() `` in
   `DocxAuthoredShapeTest.kt:26` contains an apostrophe, which opens a phantom char literal.

The earlier scans counted a name as used if it appeared anywhere outside the import line,
comments and strings included. That is the most likely explanation for the difference between
this list and `audit-023`'s zero, and it is a correction rather than a contradiction: the two
numbers measure different things.

---

## 4. The 62-warning backlog, re-verified

Read from the PR #39 lint comment (run `37860478309`, head `34639ce`). The tree has not changed
since, so the line numbers were re-checked against HEAD and all of them still land on the
construct the warning names.

| Issue id | Severity | Count | Spot check at HEAD |
|---|---|---|---|
| `AutoboxingStateCreation` | Hint | 34 | `MainActivity.kt:50`, `PagellaModule.kt:59-61`, `UniversalPrintSheet.kt:48,55,57`; `mutableStateOf(0…)` grep returns 14 literal-argument hits |
| `ConfigurationScreenWidthHeight` | Warning | 6 | `InkyModule.kt:147,148`, `LayoutDrivenDocumentRenderer.kt:214`, `MainActivity.kt:220`, `PapirusTextToolbar.kt:127,128`, all present |
| `PluralsCandidate` | Warning | 6 | `strings.xml:130,184,444,600,684,793`; the file still has **0** `<plurals>` entries in 746 `<string>` entries |
| `VectorRaster` | Warning | 6 | the six brand marks, all at `res/drawable/ic_*_logo.xml:3` plus `ic_papirus_foreground.xml:3` |
| `ModifierParameter` | Warning | 4 | `HomeDashboard.kt:1096`, `HomeSubpages.kt:121`, `InkyViewSettingsSubpage.kt:31`, `LayoutDrivenDocumentRenderer.kt:119` |
| `IconLocation` | Warning | 3 | the three `img_pattern_*.jpg`, 1,269,429 + 542,033 + 1,229,088 = **3,040,550 bytes**, `grep -rn img_pattern app/src` returns **0** references. **Deleted since this table was read** (PR #40); the run `38750210445` lint report confirms the backlog is now **59 across 7 ids** |
| `FrequentlyChangingValue` | Warning | 2 | `InkyModule.kt:1499,1810` |
| `UseOfNonLambdaOffsetOverload` | Warning | 1 | `AboutScreen.kt:351` |
| **Total** | | **62** | 34+6+6+6+4+3+2+1 = 62, across 8 ids |

The `audit-024` grouping still holds: **A** 35 mechanical, **B** 12 real Compose 1.12 migrations
that can change what the app draws, **C** 15 content decisions for the owner.

**`AGENTS.md:247` is stale on both of its numbers.** It says "the 58-warning tail, 35 of them
`UseKtx`". The 8-id table above contains no `UseKtx` at all, and the total is 62, not 58. The
`UseKtx` sites went away with the Compose upgrade; the count rose by 47 from five checks new in
lint 9.4.1. (Proposed replacement text is in §6.)

---

## 5. Goldens: unblocked 2026-10-09, recorded 2026-10-15 — see §5.1

| Check | Result |
|---|---|
| `gh run list --workflow goldens.yml --limit 10` | **empty. The workflow has never run** |
| `ls app/src/test/screenshots/` | one file, `greeting.png`, 2,868 bytes |
| `grep -rn verifyRoborazzi` across the repository | three hits, all inside `goldens.yml`'s own comments and two audit files. **No verify task exists** |
| `ci.yml` test command | still records (`roborazzi.test.record=true`), never compares |
| `gh api .../actions/workflows` | `Record Goldens`, id `379134664`, `state: active`. The workflow is enabled and on the default branch, so it is dispatchable today |

**The dispatch was attempted here and failed on credentials, not on the workflow.**
`gh workflow run goldens.yml --ref main -f base_branch=main` returns
`HTTP 403: Resource not accessible by integration`. That is the sandbox token missing the
`workflow` scope; `gh auth status` shows the same token authenticating fine for repository reads
and for `git push`. So this step needs the owner. From a machine with a full token, or from the
Actions tab:

```bash
gh workflow run goldens.yml --ref main -f base_branch=main
```

or open `https://github.com/makerandreas/Papirus-Office/actions/workflows/goldens.yml`, choose
**Run workflow**, leave `base_branch` on `main`, and run it. It records on a pinned
`ubuntu-24.04` runner, then opens a pull request named "Record Roborazzi goldens (run N)" rather
than pushing, so the images are reviewed before they become the baseline. Do not merge that pull
request without looking at the PNGs: whatever it shows becomes what every later
`verifyRoborazziDebug` run is compared against.

Recording from `main` is the right baseline for the Plan 9 branch. This audit's housekeeping
commit deletes two unreachable Kotlin files, seven unused import lines and three unreferenced
JPGs, none of which a composable draws, so it cannot move a pixel.

The consequence is the one `audit-022` §2 named and that is still true: a rendering regression
changes an artifact and no job notices. For Plan 9 specifically, the consequence is narrower and
still real: Plan 9 rewrites the save path, whose visible output is a re-opened document, and
"the re-opened document looks wrong" is precisely the class of failure a recorded golden catches
and a passing unit test does not.

### 5.1 What happened next: the owner dispatched it, and the run is a clean bill of health

The owner dispatched the workflow from `main` on 2026-10-15. This section is the read of that
log, and it is the evidence for Recommendation item 3 being closed and for starting Plan 9.

**Run identity.** Workflow `Record Goldens` (id `379134664`), run `38754267188`, from `53d61ac`,
runner `ubuntu-24.04` (image `20261012.334.1`), `Zulu 17.0.20+8`, Gradle `9.8.1`. Job
`Record Roborazzi goldens` ran `2026-10-15T14:45:41Z` → `14:51:51Z`, `BUILD SUCCESSFUL in 5m 26s`,
35 tasks executed. Artifact `recorded-goldens`, 568,162 B, id `11983850657`, 14-day retention,
SHA256 `bec4953fe5bf994e8120d3ced0698ea894a194d4f37bd67e9e98fac8bc3ac189`. PR **#41** was opened
from branch `goldens/record-1` and is **MERGED** into `main`. Thirteen PNGs, all `create mode
100644` except `greeting.png`, which was re-recorded over the pre-existing file.

**What the workflow actually ran.** `./gradlew testDebugUnitTest -Proborazzi.test.record=true` —
the whole unit suite, the same command `ci.yml`'s test job runs. This matters for reading the log:
it is a full 435-test run with test stdout, not a screenshot-only job, so its printed measurements
are directly comparable with the CI reports on PR #39 and PR #40.

**The headline is that nothing moved.** The plan 5A dump summary printed by
`PaginationFidelityTest` is identical, column for column, in all three runs — `37860478309`
(PR #39, the Compose upgrade head), `38750210445` (PR #40, this branch after the housekeeping
deletions) and `38754267188` (the goldens run on `main`):

| file | pages | ref | window | thin | empty | breaks | blank |
|---|---|---|---|---|---|---|---|
| Sample-1.odt | 14 | 15 | 12..18 | 0 | 0 | 0 | 9 |
| Sample-1.docx | 16 | 15 | 12..18 | 0 | 0 | 0 | 9 |
| Sample-2.odt | 23 | 23 | 18..28 | 1 | 0 | 2 | 27 |
| Sample-2.docx | 26 | 23 | 18..28 | 0 | 0 | 0 | 29 |
| Sample-3.odt | 21 | 22 | 18..26 | 3 | 0 | 0 | 1 |
| Sample-3.docx | 23 | 22 | 18..26 | 2 | 0 | 0 | 1 |
| Sample-4.odt | 10 | 11 | 9..13 | 0 | 0 | 2 | 15 |
| Sample-4.docx | 12 | 10 | 8..12 | 2 | 0 | 0 | 19 |
| Sample-5.odt | 18 | 19 | 15..23 | 0 | 0 | 2 | 16 |
| Sample-5.docx | 20 | 18 | 15..21 | 1 | 0 | 0 | 19 |
| Sample-6.odt | 20 | 22 | 17..27 | 0 | 0 | 2 | 14 |
| Sample-6.docx | 24 | 21 | 15..26 | 1 | **1** | 0 | 18 |

All twelve files stay inside their recorded windows. `Plan6cLoadingProgressTest` agrees:
Sample-6.odt `parse=35 ms layout=75 ms pages=20`, Sample-6.docx `parse=40 ms layout=53 ms pages=24`.
`FragmentPaginationTest`'s stress case is `backend=table pages=587 paragraphs=160 measurements=160
elapsedMs=409`, the same 587 as before.

**The one number that looked new is not new.** `Sample-6.docx` reports `empty 1`: page 2 has
`0` elements, `first -`, `last -`, `reserved 0.0`, `leftover 930.7`, `endedBy BREAK`. That is a
genuinely blank page in the middle of the document. It is also in PR #39's report (`37860478309`)
with the same page count of 24 and the same thin count of 1, which means it predates the Compose
upgrade, predates this branch and predates the goldens run. It is the recorded baseline, not a
regression, and not something Plan 9 introduced. Whether it *should* be there is a layout question
for whoever touches pagination; it is not a blocker.

**The Robolectric SDK warning is a non-issue for this run.** The log prints
`[Robolectric] WARN: Android SDK 36 requires Java 21 (have Java 17). Tests won't be run on SDK 36
unless explicitly requested.` Every Robolectric test class in the project pins the SDK: all 49
files that use `RobolectricTestRunner` carry `@Config`, and all 60 `@Config` occurrences pin
`sdk = [34]`. No test asks for SDK 36, so no test was skipped. It goes live the moment
`targetSdk 36` stops being theoretical or someone bumps a pin, and bumping any one of those 60 pins
invalidates all thirteen goldens at once — they are SDK-34 goldens.

**Goldens are recorded but still not enforced.** `ci.yml` passes
`-Proborazzi.test.record=true`, which overwrites rather than compares, and `verifyRoborazziDebug`
still does not exist in the repository (three grep hits: one in `goldens.yml`'s own comments, two
in audit files). So the thirteen PNGs on `main` are a reference baseline for review, not a gate.
Switching `ci.yml` to verify is a separate, deliberate commit that had to land *after* these
goldens, and it still has not been made. Until it is, a future change that alters a screen will
not fail CI.

**One new thing the run surfaced, which belongs to Plan 9.** `testDefaultOttDiagnostic` prints
`content.xml FAIL` and `settings.xml FAIL`. That is a printer, not a failure — the test only
`println`s PASS/FAIL per part and asserts nothing, which is why the build stayed green. The fact
behind it is real and measurable:
`app/src/main/assets/templates/styles/Default.ott` (8,729 B) contains exactly five parts —
`mimetype` 48 B, `META-INF/manifest.xml` 538 B, `meta.xml` 591 B, `styles.xml` 11,569 B,
`Thumbnails/thumbnail.png` 5,826 B. There is no `content.xml` and no `settings.xml`. The manifest
lists only `Thumbnails/thumbnail.png`, `styles.xml` and `meta.xml`, so it declares no part the
package does not contain — the file is internally consistent, just styles-only.
`InkyModule.kt:3426` is the consumer: it falls back to `templates/styles/Default.ott` as the
current path when no document is open.

The consequence for Plan 9 is concrete. Every one of the six samples has a `content.xml` to
re-save, so a round-trip test built only on the samples never exercises *creating* one. The first
save of a document created from the default template has to. Plan 9's round-trip test should
therefore include a "new document → type something → save → close → reopen" case alongside the
sample-file cases, because that is the path with no `content.xml` to fall back on.

---

## 6. Documentation drift, measured

The project's own third named failure pattern (`audit-022` §6) is that documentation drifts
toward describing the target, and that the `file:line` requirement only works when the numbers get
re-read. They have not been re-read since 2026-10-08, and the 2026-10-12 re-upload moved them.

| Where | Claim | Measured at HEAD |
|---|---|---|
| `AGENTS.md:87` | fabricated resume at `InkyModule.kt:1157` | the string is at **`InkyModule.kt:1190`** (`audit-023` proposed `:1206`) |
| `AGENTS.md:107` | `TextLayoutManager` debug inspector at `InkyModule.kt:3897` | **`InkyModule.kt:3963`** |
| `plan-01-master-index.md:75` | `isWebView = false` at `InkyModule.kt:124` | **`InkyModule.kt:109`** |
| `plan-01-master-index.md:167` | same, corrected to `:125` | **`InkyModule.kt:109`** |
| `plan-01-master-index.md:137` | `InkyModule.kt:3897` | **`InkyModule.kt:3963`** |
| `plan-01-master-index.md:139` | 48 dp targets at `DESIGN.md:379`, token colours at `DESIGN.md:384` | **`DESIGN.md` is 229 lines.** Both citations point past the end of the file. The 48 dp accessibility requirement is now at `DESIGN.md:220` |
| `AGENTS.md:29` | icon mixing, e.g. `CellinaModule.kt:569` | **`:569` is a blank line.** The nearest real `Icons.Default` use is `:571`; the first in the file is `:387`. 148 non-`Rounded` icon references exist across 12 files, so the claim is right and both of its examples are wrong |
| `AGENTS.md:29` | same, e.g. `PagellaModule.kt:147` | `:147` is an `IconButton`; the first `Icons.Default` in that file is `:171` |
| `AGENTS.md:132-133` | consult `app/src/main/sdk-examples` | **the directory does not exist.** The SDK examples live in `sdk-references/` (64 `.java` files) and `app/src/main/assets/sdk_examples` |
| `AGENTS.md:247` | "the 58-warning tail, 35 of them `UseKtx`" | 62 warnings, 8 ids, no `UseKtx` (§4) |

`AGENTS.md`'s header reserves its content to the owner, so none of these were edited. The
corrected values are listed here so the edit is a copy rather than another scan.

**Still accurate, re-checked:** `HomeDashboard.kt:1210-1275` (the Google Drive sub-page really
does start at `:1210`), `PagellaModule.kt:85-120,132-198,202-299` (file is 314 lines),
`InkyModule.kt:3943` is still the only `setReminder` call site.

---

## 7. The audit trail is severed

`git log --oneline main` returns one commit. Every SHA the audits cite is therefore unresolvable:

```
8796828  MISSING      (audit-022 / audit-023 baseline, "Merge pull request #37")
d7541f2  MISSING      (audit-023's PR #38 gate head)
aec1672  MISSING      (the assertion-relaxation commit audit-023 §5 reviews)
912de8e  MISSING      (the commit that deleted testImplementation(libs.mockk))
ef2945b  MISSING      (the Charsets import loop, audit-020 §3)
6b2dfea  MISSING      (PR #38 merge)
c61e970  MISSING      (the 62-warning run head)
307f01a  MISSING      (PR #37's last head)
59800be  present      (PR #39 merge; present here only because this audit fetched it)
```

Two consequences. First, `git show <sha>` and `git merge-base --is-ancestor`, which `audit-022` §5
used to decide whether the five PR #35 commits were on `main`, no longer work at all. Second, the
owner's own rule 2 under "Reading the CI report" in `AGENTS.md` says to confirm that
`git rev-parse HEAD` and the report's SHA agree before attributing a failure to the current tree.
That check can now only be satisfied for commits made after 2026-10-12.

Not fixable from this branch, and not worth a rewrite of 24 audit files. The mitigation is
forward-looking: cite **run ids** rather than SHAs, since `gh api .../actions/runs/<id>` still
resolves and `gh run list --branch <branch>` still maps a branch to its heads.

---

## 8. The 39-pull-request record, read as a sequence

39 pull requests: **37 merged, 2 closed** (#2 superseded by #3, #35 superseded by #36 and closed
by the owner after review). Read as phases rather than as a list:

| Phase | PRs | What it was | What it left behind |
|---|---|---|---|
| Audit-driven hardening | #1 to #10 | security, crash, build and correctness findings; the LOKit seam; the structural Navigator; the unified page stack; page geometry | the `OfficeDocElement` wrapper (F-2), still Plan 9's to retire |
| Writer fidelity, ODF | #15 to #33 | Plan 5a to 5e (metrics, fixtures, transform, pagination), Plan 6A to 6D (images, media store, truthful progress), Plan 7A to 7F (numbering, canonical import, indexes, sections, tables, fonts, tab leaders, hidden sections), Plan 12A (the LOKit JNI seam) | Plans 7 and 8 are complete. Decision D3 (declared font faces on a modified save) was reversed by the owner on 2026-10-04 and deferred to Plan 9 |
| Writer fidelity, OOXML | #34, #36 | Plan 8A (style chain, run model), Plan 8B (numbering, fields, tables, TOC, sections) | landed through the CI triage PR rather than its own |
| CI and honesty | #12, #13, #14, #20, #21, #36, #37 | strings extraction, dead controls, the documentation truth passes, and the last two making the build's own evidence readable | the 62-warning backlog and the unrecorded goldens |
| Closeout, not a Plan | #38, #39 | `audit-023` executed `audit-022`'s six recommendations; `audit-024` recorded the Compose upgrade and the new warning backlog | this audit's subject |

**Three patterns the record itself named, and whether they held this time:**

1. *"Every long red loop was an unreadable report, not a hard bug."* Held, and the fix held: PR #38
   and #39 went green without a loop, because the report now names the head SHA.
2. *"Relaxing an assertion is how a green build is bought."* Held as a risk and `audit-023` §3
   reversed four relaxations against measured values. One was left deliberately un-restored (the
   per-level TOC counts) because the number could not be derived from the fixture, which is the
   right call.
3. *"The documentation drifts toward describing the target."* **Held, and drifted again.** §6 lists
   ten stale claims, three of which were corrected on 2026-10-08 and are stale again because the
   re-upload moved the code without moving the prose. This is the one pattern the project has not
   yet found a guard for. `SourceHygieneGuardTest` catches literals and raw greys; nothing catches
   a `file:line` in a markdown file.

**Also on the remote:** 37 `arena/*` branches, every one from a merged or closed pull request.
They are not debris in the tree, but they make `git branch -r` useless for finding live work.

---

## 9. Writer Guide 26.2 Chapter 1, where it stands

Chapter 1 of `docs/lo-guides/WG262-WriterGuide_compressed.pdf` (PDF pages 17 to 40) is the
behaviour the project is reproducing, and `docs/InkyC1Checklist.md` is the owner's pass/fail list
for it. The mapping lives in `plan-01-master-index.md` §3 and §4 and is not repeated here. What
changed since `audit-022` §4:

**The five-reminder cap is implemented and tested** (`ReminderManager.MAX_REMINDERS`, eviction on
a monotonic `DocumentReminder.sequence`, `DocumentEnginesUnitTest.kt:243-256` covers the sixth,
seventh and replace cases). `plan-01` §3.3 was corrected on 2026-10-08 and now says so.

**The reminder is still write-only.** `docs/InkyC1Checklist.md` §Reminder asks for Prev/Next in
the Navigator with the Reminder filter selected. Re-measured at HEAD: `setReminder` has exactly
one call site (`InkyModule.kt:3943`); `getReminders`, `nextReminder`, `previousReminder` and
`removeReminder` have **zero** call sites outside `ReminderManager.kt` and its own test. The
dialog at `InkyModule.kt:3921-3956` sets a reminder, raises a toast, and the reminder is then
unreachable. Half of that checklist section cannot pass, and it is not a Plan 9 item: it is the
Plan 11 editor package.

**Nothing in Chapter 1 has been run on hardware.** The owner deferred the physical-device pass
until Plan 11 completes, and every row of `plan-01` §4 still says "device-unverified".

---

## 10. Plan 9 readiness

**What Plan 9 is.** `plan-04-to-09-writer-fidelity.md` § Plan 9 (save round-trip integrity, was
PR I). It closes O-01, the non-destructive-package rule, and retires F-2 (`OfficeDocElement`).
Roadmap v2 §4.10 states its acceptance: *open, save, reopen preserves text, styles, numbering,
tables, images and TOC for Sample-6 in both formats, verified by a CI round-trip test.*

**What already exists.** `OdtDocumentWriter.kt` is a real writer, not a stub. It preserves the
original `content.xml` byte-for-byte when the document is unmodified, embeds images as package
entries, writes `mimetype` first as STORED per ODF Part 2, and refuses a modified save through
`saveCapability` / `rejectUnsupportedStructuralRewrite` when the document carries authored
indexes, named sections or source table structure. That fail-closed boundary is Plan 7B's
contribution, and it is in place.

**What Plan 9 still has to build**, from the two design notes in `plan-04-to-09` § Plan 9:

* **ODT:** upgrade the generated roots from `office:version="1.2"` to `1.4`. Confirmed still `1.2`
  at `OdtDocumentWriter.kt:194` (`content.xml`) and at `:331`, `:359` and `:372` (`styles.xml`,
  `meta.xml` and `settings.xml`). ODF 1.4 Part 3 §19.390 is the citation, and
  `docs/odf/OpenDocument-v1.4-part3-schema.html` is in the tree.
* **ODT:** preserve `styles.xml`, `meta.xml`, `settings.xml` and `Thumbnails/*` untouched, and
  append derived automatic styles under collision-safe names rather than re-indexing. **This is
  the largest gap and it is visible in the code today.** `buildStylesXml` at
  `OdtDocumentWriter.kt:328` builds `styles.xml` from a hardcoded triple-quoted template: a
  `Standard` style, a `Body_20_Text` style, a `Heading_1` style at a fixed 18 pt, and a
  `style:default-style` pinned to `Liberation Serif, Times New Roman` at 12 pt. A saved ODT
  therefore loses every style the document declared. That is Plan 9's core job, and it is the
  concrete form of the `audit-006` O-01 finding the plan was written to close.
* **DOCX:** a real `styles.xml` consistent with the regenerated `document.xml`, `numbering.xml`
  references that resolve, heading/character style pairs, `w:tblGrid`, and images registered in
  `word/_rels/document.xml.rels` and `[Content_Types].xml`.
* Remove the original-package-bytes fallback once the round trip is proven.
* Delete the `OfficeDocElement` wrapper in a mechanical final commit (cross-plan invariant 1).
* Retire the last `[Image: …]` residue at `OfficeDocumentParser.kt:2392`. It is inside
  `toPlainText`, not a save path, so it is a candidate rather than a certainty; decide it in the
  plan document.

**The blocker.** §4.10 ends with: *"Scope is firmed by a short plan document before this PR
starts; do not start it from this paragraph alone."* No such document exists in `anti-slop/`,
`docs/`, or anywhere in the tree. That is the only thing standing between the current green tree
and Plan 9, and it is a writing job, not a code job.

**Two things Plan 9 will collide with if they are not decided first:**

1. **The fixture rule.** Cross-plan invariant 6 says re-saving a fixture is a deliberate act that
   updates `tests/inky/fixture-identities.properties`, the `SampleMatrix` references and the
   recorded windows in the same PR. A round-trip test writes files. `FixtureIdentityTest` is in
   place and will catch an accidental rewrite, which is the right outcome, but the plan document
   has to say where the round-trip output goes and how the manifest is updated.
2. **The goldens.** If Plan 9 is the first PR reviewed against recorded pixels, the goldens have
   to exist before it starts, not during it. **Satisfied 2026-10-15**: the 13 PNGs are on `main`
   via PR #41 (§5.1).

**Update 2026-10-15.** Both of the things that were standing between the tree and Plan 9 are now
closed: the plan document exists as `anti-slop/plan-09-save-round-trip-integrity.md`, and the
goldens are recorded and merged. Two notes belong alongside that, and both came out of reading
the goldens run (§5.1):

* Plan 9's round-trip test list should gain a **new-document** case. The default template
  `templates/styles/Default.ott` has no `content.xml` (§5.1, finding J), so "new → type → save →
  close → reopen" is the only path where the writer has to create that part rather than update it.
  The six samples all have one, so a test built only on samples never reaches that branch.
* The goldens are a **baseline, not a gate**. `ci.yml` still records. Plan 9 will change what a
  re-opened document looks like, which is exactly the thing a golden would catch — but only if
  someone runs `verifyRoborazziDebug`. Until `ci.yml` is switched, Plan 9's visual review is a
  manual act: compare the 13 PNGs before and after.

---

## 11. Recommendation

**Continue to Plan 9. Do not stop to clean all of the debris.** Two reasons.

First, almost none of it gates Plan 9. Of the twelve items in §2, two are in tier 1 and one of
those (the plan document) is a prerequisite you would write anyway. The other ten are independent
and several are cheaper later: the warning backlog wants the goldens, and the goldens want the
Compose-1.12 pixel behaviour settled, which is exactly what Plan 9 does not change.

Second, the debris is not decaying. The two resurrected dead files cost 191 lines of dead code and
nothing else; the unused imports are invisible to lint; the 62 warnings are already recorded with
file and line and will still be there in November. What *is* decaying is the memory of why each
one is parked, and `audit-024` plus this document carry that.

**The recommended order, three PRs before Plan 9 and one after:**

| Order | PR | Contents | Why here |
|---|---|---|---|
| 1 | **Housekeeping** | Re-delete `StorageProvider.kt` and `KeyboardShortcutHandler.kt`; delete the 7 unused imports; delete the three unreferenced JPGs (owner's call on the last one); update `plan-01` §6 and this audit | Pure deletion, compiler is the reviewer, and it settles the "deleted twice" problem before anything else touches `data/` |
| 2 | **Plan 9 plan document** | One file in `anti-slop/`, following the shape of `plan-3d-strings-sweep.md`: scope, files, tests, acceptance, and the two decisions in §10 | This is the actual gate. Nothing else in tier 1 is |
| 3 | **Goldens** — **DONE 2026-10-15** | Dispatched from `main`, run `38754267188`, 13 PNGs on `ubuntu-24.04`, opened as PR #41 and merged | Group B and every later visual change are now reviewable against pixels. Two follow-ups are *not* done: switching `ci.yml` from record to verify, and re-recording if any of the 60 `sdk = [34]` pins ever moves |
| 4 | **Plan 9** | As its plan document specifies, plus one test case §5.1 argues for: new document → type → save → close → reopen, which is the only path that has to *create* `content.xml` | The slot after these. Everything in the first three rows is now closed |
| after | **Warning Groups A, then B, then C** | As `audit-024` sequenced them, with Group B reviewed against the goldens | Independent of Plan 9; A and C can be prepared any time |

**Two habits worth adopting, both cheap:**

* **Stop squashing `main`.** The 2026-10-12 re-upload cost the repository its entire commit
  history and, with it, the ability to check any claim the 24 audit files make (§7). If a re-upload
  is unavoidable, fetch the previous head first and diff, the way §1 does.
* **Cite run ids, not SHAs.** `gh api .../actions/runs/<id>` still resolves after a squash; a SHA
  does not.

---

## 12. Honest limits

* **Nothing was built, run, or linted here.** No Gradle, no Android SDK, no JDK, no Robolectric,
  no Compose, no MockK. Every build, test and lint figure is CI's and carries the run id it came
  from: `38231653018` for the gate on HEAD, `37860478309` for the 62 warnings,
  `38750210445` for the PR #40 gate, `38754267188` for the goldens run read in §5.1.
* **The test count is now measured on this branch, not inherited.** PR #40's run `38750210445`
  reports 435 tests, 0 failures, 0 errors, 0 skipped across 82 suites, and lint 9.4.1 at 0 errors
  and 59 warnings across 7 issue ids. The goldens run `38754267188` ran the same
  `testDebugUnitTest` task and finished `BUILD SUCCESSFUL`, so 435 is a measurement twice over.
  No number for `53d61ac` itself exists to read: a push to `main` posts no CI report, which is why
  §5.1 compares `main`'s behaviour through the goldens run rather than a gate on the SHA.
* **The unused-import count is this audit's method, not the project's.** §3.1 names the three
  false-positive classes that had to be removed. A count produced by a different scanner will
  differ, and the difference will be in the method.
* **The dead-declaration counts are `grep`, not a compiler.** Kotlin resolution (operator
  overloads, `by` delegation, extension functions with the same simple name in another package)
  can make a name look used or unused. The `SwTextAttr` collision in §3 is one such case, found by
  reading rather than counting, and there may be others.
* **`AGENTS.md`, `DESIGN.md`, `PROJECT_CONTEXT.md` and `CONCEPT.md` were not edited.** The first
  is owner-only by its own header; the other three have no such reservation but the corrections in
  §6 belong to the owner's truth pass (Plan 1B), which is still open.
* **No pixels were seen.** The Roborazzi artifacts live on storage this sandbox cannot open, so §5
  and §5.1 rest on the run log, the tracked-file list, the workflow's run history and the test
  sources, not on images. Nothing in §5.1 claims a golden *looks* right; it claims the run
  succeeded, which 13 PNGs were written, and that the layout numbers in the same log match the two
  runs before it.
* **No device.** No Chapter 1 item has been run on hardware, and none was checked here.
