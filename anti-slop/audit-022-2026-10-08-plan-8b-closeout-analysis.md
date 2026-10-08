# Audit 022 (2026-10-08): Plan 8B closeout, the 17 lint errors root-caused, and the dead-code ledger

> **Executed.** Every step this document recommends was carried out on the same day and is
> recorded in `audit-023`. Six of this document's claims did not survive being executed and
> are corrected there: the sweep is 121 imports across 46 files rather than 122 across 47,
> the eviction key is a monotonic sequence rather than `timestamp`, the
> `assertExists()` recommendation is vacuous, `PROJECT_CONTEXT.md` carries no screenshot
> claim to rename, `81fbd1f` relaxed nothing, and `1a1b8b6`'s `DocxTableTocTest` half was a
> correct tightening rather than a relaxation. This file is left as the analysis of record
> and is not rewritten.

**Baseline:** `main` at `8796828` ("Merge pull request #37"), branch
`arena/574ad9fd-papirus-office`. PR #37 merged 2026-10-08T09:34:02Z, so Plan 8B and the
CI recovery are both on `main` and there is no open pull request.
**Gate:** none of this session's own. The newest CI evidence in the repository is PR #37's
lint report on head `307f01a` (run `37719095501`), `lint 9.1.1: 17 errors, 58 warnings
across 15 issue ids`, and its CI report on the same head. Nothing was pushed here, so no
run belongs to this document.
**Scope:** (1) read the anti-slop PR record end to end and state where the project
actually stands, (2) root-cause the 17 lint errors instead of listing them again,
(3) settle the screenshot question with a recommendation, (4) re-run the unused-code
scan and extend it, (5) cross-check Writer Guide 26.2 Chapter 1 against the code,
(6) close the `audit-021` §13.6 review of the five PR #35 commits.
**Method:** `gh api` for pull-request comments, run metadata and the androidx and Firebase
POMs; `git log`/`git show`/`git merge-base` on a clone deepened by 80; source reading;
`pypdf` 6.19.0 over `docs/lo-guides/WG262-WriterGuide_compressed.pdf`; Python execution of
three scans (unused imports, dead declarations, string-resource damage); and
`jdk4py` 25.0.2 plus `kotlin-compiler` 2.4.20 from npm to compile and **run** the shipped
`Numbering.kt`. No Gradle, no Android SDK, no Robolectric, no Compose and no MockK exist
in this sandbox (`AGENTS.md`), so every build, test and lint number below is CI's and is
labelled with the run it came from.

---

## 0. Findings in one screen

| # | Finding | Evidence | Consequence |
|---|---|---|---|
| A | **The Fatal lint error is a real stale dependency, not a lint bug.** `com.google.firebase:firebase-auth:24.1.0` declares `androidx.fragment:fragment:1.1.0`, and the androidx detector compares that string against `"1.3.0"`. | §1.1, the detector source read from `androidx/androidx`, the Firebase BoM and firebase-auth POMs read from `dl.google.com` | One Gradle constraint closes it and drops a 2019 artifact from the APK. |
| B | **One of the 15 `StringFormatMatches` sits on a string resource that is itself broken.** `strings.xml:516` contains raw Kotlin concatenation syntax (`\n\" +` and 65-space indents) and renders 513 characters including literal `" +` lines. | §1.3, rendered here through aapt's escape rules | Fixing the placeholder alone leaves a garbage toast on screen. |
| C | **The two `StaticFieldLeak` warnings are not the same problem.** `PathSettings` retains a `Context` it only uses in `init`; `StorageManager` already stores `applicationContext` and its whole file is dead. | §1.4, §3.2 | One is a three-token fix, the other disappears when the dead file goes. |
| D | **`AppLinkUrlError` is the only one of the 17 that should not be fixed blind.** The filter is a `file`-scheme deep link that deliberately declares no host so any authority matches. | §1.2 | Adding `android:host="*"` could break "Open with Papirus" from file managers. Needs a suppression with a reason, or a measured intent-matching test. |
| E | **Record mode with no goldens is confirmed, not inferred.** One tracked PNG, thirteen rendered per run, `verifyRoborazziDebug` appears nowhere in the repository. | §2 | A rendering regression changes an artifact and no job notices. §2.3 recommends a path. |
| F | **The unused-import scan reproduces at 122 across 47 files**, and two whole files are dead that no previous audit listed. | §3 | `data/StorageProvider.kt` (137 lines) and `data/undo/KeyboardShortcutHandler.kt` (56 lines) are marked safely deletable. |
| G | **Writer Guide 26.2 Chapter 1's five-reminder cap is not implemented, and the ledger says it is.** `ReminderManager.setReminder` has no size check; the only test inserts one reminder. `plan-01-master-index.md` §3.3 calls the row a guard and says "the checklist already tests the cap". | §4, the guide text extracted from PDF page 37 | A three-line fix plus one test, and one correction to the ledger. |
| H | **The `aec1672` relaxations were not needed.** The shipped `NumberingCounterState.advance` returns exactly `"BAB 1"`, `"1.1"`, `"1.1.1"`, `"BAB 2"`, `"2.1"` for the ilvl sequence the deleted assertions used. | §5, compiled and executed here with a negative control | The exact-label and per-level TOC assertions can be restored to measured values. |

---

## 1. The 17 lint errors, root-caused

Source of the list: the `### Lint report for `307f01a`` comment on PR #37, read with
`gh api repos/makerandreas/Papirus-Office/issues/37/comments`. `307f01a` is the last commit
of the merged branch, so this is the state of `main`. `audit-021` §6.2 listed the same
findings from an earlier head; this section explains them rather than repeating them.

### 1.1 Fatal `InvalidFragmentVersionForActivityResult` at `MainActivity.kt:64`

The call site is `notificationPermissionLauncher`, a `registerForActivityResult` on a
`ComponentActivity`. The check is not an AGP check. It ships inside androidx, at
`activity/activity-lint/src/main/java/androidx/activity/lint/ActivityResultFragmentVersionDetector.kt`,
read here through `gh api repos/androidx/androidx/contents/...`. Its whole decision is:

```kotlin
val currentVersion = library.substringAfter("androidx.fragment:fragment:").substringBeforeLast("-")
if (library != currentVersion && currentVersion < FRAGMENT_VERSION) { /* report */ }
```

over `context.project.buildVariant.mainArtifact.dependencies.getAll()`, that is, over
every resolved coordinate of the main artifact, transitive ones included. Two things
follow. The comparison is a **string** comparison, and it applies to transitive
dependencies, not only to declared ones.

Where the coordinate comes from. `gradle/libs.versions.toml` declares no fragment version
and `app/build.gradle.kts` declares no fragment dependency; `grep -rln androidx.fragment
app/src/main/java` returns nothing. So it is transitive. Read from `dl.google.com`:

| POM | What it declares |
|---|---|
| `firebase-bom/34.15.0` | `firebase-auth` **24.1.0**, `firebase-firestore` 26.4.0, `firebase-ai` 17.13.0, `firebase-appcheck-recaptcha` 19.0.0 |
| `firebase-auth/24.1.0` | `androidx.browser:1.4.0`, `androidx.credentials:1.2.0-rc01`, `androidx.credentials:credentials-play-services-auth:1.2.0-rc01`, **`androidx.fragment:fragment:1.1.0`**, `androidx.localbroadcastmanager:1.0.0`, `play-services-auth-api-phone:18.0.2`, `recaptcha:18.6.1` |
| `credentials/1.2.0-rc01` | `credentials-play-services-auth`, `annotation 1.5.0`, `kotlin-stdlib`, `coroutines-core`. No fragment. |
| `recaptcha/18.6.1` | `play-services-basement`, `play-services-tasks`, `play-integrity`, `kotlin-stdlib`, coroutines. No fragment. |
| `androidx/fragment/fragment/maven-metadata.xml` | newest published version **1.9.1**. No `1.1x` exists. |

So `firebase-auth` is the only fragment declarer in the visible graph, at 1.1.0, and since
no 1.1x release exists and the check fired, the resolved version sorts below `"1.3.0"`.
`androidx.fragment:fragment:1.1.0` is a 2019 release and it is in the APK.

What the finding does and does not mean. The message is about old `FragmentActivity` not
calling `super.onRequestPermissionsResult()` and using invalid request codes. `MainActivity`
extends `ComponentActivity` and no source file touches `androidx.fragment`, so the runtime
hazard the message describes does not apply to this call site. The dependency is still
real, still stale, and still worth lifting.

The fix, one Gradle constraint rather than a lint suppression:

```kotlin
// app/build.gradle.kts, inside dependencies {}
implementation(platform(libs.firebase.bom))
// firebase-auth 24.1.0 pulls androidx.fragment:fragment:1.1.0 (2019). Nothing in this
// module imports androidx.fragment; the constraint lifts the resolved version so the
// androidx.activity lint check sees a current one and the APK stops shipping 1.1.0.
constraints {
  implementation("androidx.fragment:fragment:1.9.1")
}
```

The version belongs in `gradle/libs.versions.toml` as `androidx-fragment` and is
referenced as `libs.androidx.fragment`, matching how every other dependency in the module
is declared.

**Caveat to record now so nobody misreads it later.** Because the detector compares
strings, a future `androidx.fragment:fragment:1.10.0` sorts *below* `"1.3.0"` and would
re-fire the Fatal on a newer version than the one that is correct. If that day comes the
answer is a `lint { disable }` entry with this paragraph as its comment, not a downgrade.

**Verification only the owner can run:**
`./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep androidx.fragment`
before and after. This sandbox has no Gradle, so 1.1.0 is inferred from the POMs plus the
detector's own rule, not resolved here.

### 1.2 `AppLinkUrlError` at `AndroidManifest.xml:67:17`

The flagged element is `<data android:scheme="file" />` inside the third `intent-filter`
(lines 63 to 82), which declares `VIEW` plus `EDIT`, `category.DEFAULT`, one scheme and
eleven `pathPattern`s (`.odt`, `.ott`, `.ods`, `.ots`, `.odp`, `.otp`, `.docx`, `.doc`,
`.xlsx`, `.xls`, `.pptx`) and no host. The sibling filter at lines 44 to 62 carries the
MIME-based `content://` matching and is not flagged, and its own comment says why the two
filters exist.

The check's implementation is `AppLinksValidDetector` in AGP `tools/base`, per the check's
published documentation page. Its subject is deep links and App Links, and its complaint is
that a `<data>` with a path and no host is not a well-formed URL pattern.

Two options, and only one of them is safe to apply from this sandbox:

| Option | What it does | Risk |
|---|---|---|
| Add `android:host="*"` to the same `<data>` element | silences the error, the quickfix the IDE offers | for `file:///sdcard/x.odt` the authority is empty. Whether `host="*"` still matches an empty host is an `IntentFilter`/`PatternMatcher` detail that cannot be tested here. If it does not, "Open with Papirus" from any file manager that emits a `file:///` URI stops working, and no unit test in this repository would notice. |
| `tools:ignore="AppLinkUrlError"` on that one filter, with a comment | keeps today's matching exactly | the finding stays visible as suppressed, which is the point of a suppression with a written reason |

Recommendation: the suppression, with the reason inline, and an instrumented test in
Google AI Studio that builds the two `file:///` and `content://` URIs a file manager
actually sends and asserts `MainActivity`'s filter matches both. That test is worth having
regardless of which option is taken, because it is the only thing standing between this
filter and a silent break.

### 1.3 The fifteen `StringFormatMatches`, and one broken string behind three of them

All fifteen call sites were opened and read. Every one passes an `Int` where the resource
declares `%1$s`. Lint's own wording is the evidence; the argument names below are the
call sites.

| Call site | String resource | Argument |
|---|---|---|
| `CellinaModule.kt:1060` | `toast_inserted_row_above_row_activecellrow` (`strings.xml:726`) | `activeCellRow` |
| `CellinaModule.kt:1583` (2 errors) | `toast_frozen_pane_at_col_row` (`:803`) | `activeCellCol`, `activeCellRow` |
| `HomeSubpages.kt:1248` and `:1254` (2 errors, 1 resource) | `toast_drop_cap_set_to_lines_lines` (`:444`) | `lines` at both sites |
| `InkyModule.kt:2071` | `toast_found_match_at_character_index` (`:500`) | `index` |
| `InkyModule.kt:3452` (3 errors) | `toast_document_properties_room_db_n_file_meta_filename` (`:516`) | `meta.wordCount`, `meta.characterCount`, `meta.paragraphCount` |
| `InkyModule.kt:3594` | `toast_font_size_changed` (`:524`) | `size` |
| `InkyModule.kt:3962` | `toast_reminder_set_at_paragraph_cursorpara` (`:534`) | `cursorPara` |
| `LoadSaveGeneralSubpage.kt:536` | `toast_auto_recovery_set_to_every_validminutes_minutes` (`:689`) | `validMinutes` |
| `SlidiaModule.kt:1120` | `toast_exported_slide_activeslideindex_1_as_png_image` (`:789`) | `activeSlideIndex + 1` |
| `SlidiaModule.kt:1786` | `toast_custom_show_customshowname_created_with_parsed_size` (`:798`) | `parsed.size` only; `customShowName` is a `String` and keeps `%2$s` |
| `UniversalFormsSheet.kt:1025` | `toast_built_sdk_form_with_res_totalcontrolscreated` (`:605`) | `res.totalControlsCreated` |

Fifteen errors, eleven resources, **fourteen placeholder edits**. The count closes because
`toast_drop_cap_set_to_lines_lines` is used at two call sites and needs one edit.

At runtime `String.format` with `%s` and an `Int` prints the number rather than throwing,
so this is a correctness and localization defect, not a crash. It matters for translation:
a translator sees `%1$s` and writes a sentence for a string, and the plural rules of the
target language never apply.

**The finding lint does not report.** `strings.xml:516` is not a well-formed piece of copy.
Its raw XML is:

```
Document Properties (Room DB):\n\" +
                                                                 \"File: %1$s (%2$s)\n\" +
                                                                 \"Author: %3$s\n\" +
                                                                 \"Created: %7$s\n\" +
                                                                 \"Modified: %8$s\n\" +
                                                                 \"Words: %4$s | Chars: %5$s | Paragraphs: %6$s
```

That is a multi-line Kotlin string built with `+`, pasted into the resource verbatim by
the Plan 3A extraction, escapes and all. Applying aapt's unescaping (`\n` to a newline,
`\"` to a quote) and substituting sample arguments gives 513 characters:

```
Document Properties (Room DB):|
" +|
                                                                 "File: Sample.odt (odt)|
" +|
                                                                 "Author: Andreas|
...
```

rendered into a `Toast.LENGTH_LONG` at `InkyModule.kt:3452`, its only use site. So the
user sees literal `" +` and 65-space indents. Changing `%4$s`/`%5$s`/`%6$s` to `%d` fixes
three lint errors and leaves the toast unreadable.

The resource has to be rewritten as one line, for example:

```xml
<string name="toast_document_properties_room_db_n_file_meta_filename">Document properties. File: %1$s (%2$s). Author: %3$s. Created: %7$s. Modified: %8$s. Words: %4$d, characters: %5$d, paragraphs: %6$d.</string>
```

and the honest answer to 513 characters of metadata is a dialog, not a toast. That belongs
to the Plan 11 editor package or the Plan 3 backlog, not to a lint pass; the lint pass
should only stop the resource from printing Kotlin syntax.

**How bounded the sweep is.** All 746 `<string>` entries were scanned for concatenation
residue (`" +`, a trailing `+`), escaped quotes, literal newlines and interpolation
residue. Exactly one entry carries concatenation damage: this one. `save_failed_msg` and
`doc_open_failed_msg` contain `\"%1$s\"`, which is legitimate Android escaping of a
literal quote mark, and `toast_saved_crash_report_to_n_savedlocation` contains a
legitimate `\n`. So the string work is fourteen placeholder edits and one rewrite, not an
open-ended audit.

**Predicted, and checkable.** Turning `%1$s` into `%1$d` creates new `PluralsCandidate`
warnings, for the same reason `strings.xml:130` (`%1$d minutes`) and `:184`
(`%1$d words, %2$d chars`) already carry two. Roughly four to six of the edited strings put
a noun directly after the placeholder (`minutes`, `lines`, `controls`, `slides`, `pt`). The
prediction is that the next lint report reads about **2 errors and 62 to 64 warnings**.
That is a prediction, not a measurement, and the run that follows the fix is what settles
it. It is written down first so the delta can be read against it, the way `audit-021` §6.3
read the `OldTargetApi` correction.

### 1.4 The two `StaticFieldLeak` warnings

**`PapirusAssetEngine.kt:37` is real.** `PapirusAssetEngine` is an `object`, so
`private var pathSettings: PathSettings? = null` is a static field, and
`PathSettings(private val context: Context)` (`data/framework/PathSettings.kt:10`) holds a
`Context` for the life of the process. `context` is read at lines 16 and 17 only, both
inside `init`, in a 75-line file. Dropping the `val` removes the retained field:

```kotlin
class PathSettings(context: Context) {
```

That is the fix at the root rather than a suppression. Whether the retained context is an
`Activity` context depends on the callers, `LokitRuntime.kt:97` and
`PapirusConfigManager.kt:41`, and neither call site's context origin was traced in this
pass. That is unchecked here.

**`StorageProvider.kt:127` is not a leak, and its file is dead.** `getInstance` already
stores `StorageManager(context.applicationContext)`, which is the correct pattern; lint
flags it because the field's type has a `Context` field and it cannot prove which context
was passed. Deleting the file (§3.2) removes the warning without touching behaviour.

### 1.5 What the sweep costs, and when `abortOnError` can go back to true

The `lint {}` block already sets `warningsAsErrors = false` and `ignoreWarnings = false`,
so `abortOnError = true` aborts on errors only. With the 17 errors closed it is safe to
restore in the same PR, as a final commit, so a red lint job means something again. The
58 warnings stay warnings and need their own pass, which is a much larger and much less
urgent job (`UseKtx` alone is 35 of them).

---

## 2. Screenshots: what is actually there

### 2.1 The verified state

| Check | Command | Result |
|---|---|---|
| Tracked goldens | `git ls-files app/src/test/screenshots` | exactly one file, `greeting.png`, 2868 bytes |
| Snapshot classes | `ls app/src/test/java/com/example` | `GreetingScreenshotTest` (1 test), `InkySnapshotTest`, `CellinaSnapshotTest`, `PagellaSnapshotTest`, `SlidiaSnapshotTest` (3 each). 13 tests. |
| A verify task anywhere | `grep -rn "verifyRoborazzi\|roborazzi.test.verify" .github app/build.gradle.kts gradle.properties scripts` | **no matches** |
| What CI runs | `.github/workflows/ci.yml:68` | `./gradlew testDebugUnitTest -Proborazzi.test.record=true` |
| Where the PNGs go | the test sources | `filePath = "src/test/screenshots/..."`, that is `app/src/test/screenshots/` |

So record mode writes thirteen PNGs into the `unit-test-and-snapshot-reports` artifact,
nothing compares them to anything, and the one golden that is committed (`greeting.png`,
written by `GreetingScreenshotTest`) is overwritten on every CI run in the runner's working
tree without a commit, so a change to it is invisible.

`audit-021` §7.3 called this "a deliberate gap for now, not an oversight to hide". This
section turns it into a decision.

### 2.2 Why not to commit goldens today

Roborazzi's verify is a pixel comparison. Two things in this repository are about to move
under it:

* **`runs-on: ubuntu-latest` migrates to Ubuntu 26 on 2026-10-19**, eleven days out
  (`audit-021` §8). A runner image change moves system font rendering, and Compose text
  rendered through Robolectric's native graphics mode moves with it.
* **The BOM upgrade to Material 3 Expressive is a Plan 11 prerequisite** (`AGENTS.md`).
  Every themed colour and type token changes when it lands.

Committing goldens now means rebaselining twice for reasons that have nothing to do with
product regressions, and each rebaseline is a commit of thirteen binaries that a reviewer
cannot read.

### 2.3 The recommendation

1. **Now, in the lint PR or beside it:** rename what the classes claim. `AGENTS.md`,
   `PROJECT_CONTEXT.md` and any plan text that calls them screenshot or regression tests
   should call them composition smoke tests, which is what they are. This is a
   documentation change and it costs nothing.
2. **Also now:** give each class one assertion that fails on a composition break, for
   example `composeTestRule.onRoot().assertExists()` plus a semantic-node or text
   assertion on the module's own chrome. Today a snapshot class passes even if the module
   renders an empty box, as long as `captureRoboImage` is reached.
3. **After Plan 11 pins `runs-on: ubuntu-24.04` and the BOM upgrade lands:** commit the
   thirteen PNGs as goldens, switch the CI command to `verifyRoborazziDebug`, and keep
   `recordRoborazziDebug` behind a `workflow_dispatch` input so rebaselining is a
   deliberate act with a name. That ordering puts the two known rendering changes behind
   the gate instead of in front of it.

---

## 3. Unused code, marked not deleted

### 3.1 The 122 imports, reproduced

Same method as `audit-020` §9.2 and `audit-021` §9: an import counts as unused when its
simple name (or its `as` alias) never appears in the file with the import lines removed,
with the implicit `androidx.compose.runtime.getValue`/`setValue`/`provideDelegate` and
`componentN` family excluded and `src/compileOnly` out of scope.

| | count |
|---|---|
| Kotlin files under `app/src`, excluding `compileOnly` | **294** |
| Kotlin files tracked under `app/src` in total | 296, the extra two being `app/src/compileOnly/java/com/sun/star/{comp/helper/Bootstrap,uno/XComponentContext}.kt` |
| Unused imports | **122** |
| Files carrying them | **47** |

The per-file table matches `audit-021` §9 line for line: `InkyModule.kt` 18,
`OfficeUiComponents.kt` 8, `HomeDashboard.kt` 8, `MainActivity.kt` 6, `HomeSubpages.kt` 6,
`ActionsToUndoSubpage.kt` 6, `CrashNotificationReceiver.kt` 5, `AboutScreen.kt` 4,
`PrintingFramework.kt` 4, then 3, 3, 3, 3, 3 and 45 across the remaining 33 files.

One note on the denominator. `audit-021` recorded 293 files and `audit-020` 285; this
scan sees 294 and the tracked total is 296. The difference is which walk produced the
number, not a change in the tree: no Kotlin file was added between `83428ae` and `8796828`
(`git ls-tree -r 83428ae -- app/src | grep -c '\.kt$'` returns 296 too). The 122 and the 47
are the figures that reproduce, and they reproduce exactly.

Method note, because a first attempt at this scan returned 401 across 125 files and was
wrong: excluding a preceding `.` from the word-boundary test marks every extension
property and function (`12.dp`, `Modifier.clip`) as unused. The simple-name test must be a
plain word boundary.

### 3.2 Two whole files that no previous audit listed

Tighter rule than `audit-019` §5 used: a file is dead when **every** top-level declaration
in it, functions included, has no reference from any other file in `app/src`, and the file
is not in one of the deliberately excluded trees (`data/framework`, `data/calc`,
`data/impress`, `data/api`, `data/bridge`, `data/db`, `data/crash`, `org/libreoffice`).
The results were then read by hand, because the rule has two known false-positive shapes.

| File | Lines | Declarations | Verdict |
|---|---|---|---|
| `app/src/main/java/com/makerandreas/papirusoffice/data/StorageProvider.kt` | 137 | `interface StorageProvider`, `SafStorageProvider`, `AssetStorageProvider`, `MemoryStorageProvider`, `CloudStorageProvider`, `StorageManager` | **Safely deletable.** No reference from any other file. `DocumentReference.kt:32` builds a `"memory://$id"` string and `TemplateManager.kt` builds `"asset://..."` URLs, but nothing routes either through these providers, and `TemplateManager` does its own extraction. Deleting the file also removes the `StaticFieldLeak` warning at `:127` (§1.4). |
| `app/src/main/java/com/makerandreas/papirusoffice/data/undo/KeyboardShortcutHandler.kt` | 56 | `object KeyboardShortcutHandler`, top-level `fun Modifier.undoRedoKeyboardShortcuts` | **Safely deletable, or wire it up.** Each name appears exactly once in all of `app/src`. It is Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z handling that was never attached to a `Modifier`. Wiring it is a Plan 11 product decision, not a hygiene one; `docs/InkyC1Checklist.md` §3 exercises undo through the UI, not through this object. |

Ruled out by hand, and named so the rule stays auditable: `PapirusApplication` and
`CrashNotificationReceiver` have zero `.kt` references, and both are declared in
`AndroidManifest.xml` at `:14` and `:86`. **Any deletion sweep must grep the manifest as
well as the sources.** `PagellaModule.kt`, `SlidiaModule.kt`, `CrashLogsScreen.kt`,
`FilesSubPage.kt` and `PapirusOfficeOptionsScreen.kt` also surface under a
declarations-only rule and are all live files whose `@Composable` functions are used
elsewhere.

Re-confirmed from `audit-019` §5.2, unchanged and still a decision rather than a deletion:
`core/fonts/FontScanner.kt` (17 lines), `core/fonts/FontSyncService.kt` (47 lines),
`data/DocumentCoreEngines.kt` (8 command classes), `data/writer/SwTextAttr.kt` (9
`SwFormat*` classes).

Not scanned and deliberately kept: the UNO-style API mirror in `data/framework`,
`data/calc` and `data/impress` (`audit-019` §5.3). A declarations-only scan over those
trees returns hundreds of hits by design.

### 3.3 The shape of the sweep PR

122 imports across 47 files plus 193 lines across 2 files. Deleting an unused import
cannot change behaviour and neither can deleting a file nothing references, so the risk is
scope, not correctness: a 49-file diff that no reviewer can read line by line, in a
repository where the compiler is the only checker available. One PR, nothing else in it,
and the CI report is the review.

---

## 4. Writer Guide 26.2 Chapter 1 against the code

`docs/lo-guides/WG262-WriterGuide_compressed.pdf` is 526 pages. Pages 17 to 40 are Chapter
1, matching `audit-011` §8. Text was extracted with `pypdf` 6.19.0 and read against the
code and against `plan-01-master-index.md` §3.

### 4.1 The five-reminder cap is missing, and the ledger says it is there

The guide, "Setting reminders", PDF page 37:

> You can set up to 5 reminders in a document; setting a sixth causes the first to be
> deleted. ... Reminders are not saved with the document.

The code, `data/ReminderManager.kt`:

```kotlin
fun setReminder(paragraphIndex: Int, offset: Int, note: String): Boolean {
    reminders.removeAll { it.paragraphIndex == paragraphIndex && it.offset == offset }
    reminders.add(DocumentReminder(paragraphIndex, offset, note))
    reminders.sortBy { it.paragraphIndex }
    return true
}
```

No size check. `grep -n "setReminder\|ReminderManager("` over `app/src` returns the
declaration, one UI call site at `InkyModule.kt:3959`, and one test call site. The only
test, `DocumentEnginesUnitTest.testOutlineFoldingAndReminders` at `:213-228`, inserts one
reminder and asserts `assertEquals(1, reminders.size)`, then exercises `nextReminder` and
`previousReminder`. Nothing tests a cap.

Meanwhile `docs/InkyC1Checklist.md:97-98` is the acceptance test for exactly this:

> Create 5 random reminders (do not have to be in order).
> Create a sixth reminder. Make sure the first reminders is automatically deleted after
> the creation of this sixth reminder.

and `plan-01-master-index.md` §3.3 marks the row **guard** with the words "the checklist
already tests the cap". Both halves of that are wrong. The cap is not implemented, and no
test covers it. This is the C-5 class of defect the project's own addendum defines, and it
sat in a row labelled "already implemented and must simply keep working".

The fix is small and has one subtlety worth writing down before it is coded. The guide
says the *first* reminder is deleted, which is insertion order. The list is sorted by
`paragraphIndex`, so position in the list is not insertion order. `DocumentReminder`
already carries `timestamp`, so eviction must key on that:

```kotlin
fun setReminder(paragraphIndex: Int, offset: Int, note: String): Boolean {
    reminders.removeAll { it.paragraphIndex == paragraphIndex && it.offset == offset }
    reminders.add(DocumentReminder(paragraphIndex, offset, note))
    while (reminders.size > MAX_REMINDERS) {
        reminders.remove(reminders.minByOrNull { it.timestamp } ?: break)
    }
    reminders.sortBy { it.paragraphIndex }
    return true
}
```

with `MAX_REMINDERS = 5` and a test that inserts six at out-of-order paragraph indexes and
asserts which one survived. "Reminders are not saved with the document" is already true:
`ReminderManager` holds an in-memory list and nothing serialises it.

### 4.2 Line drift and two claims re-verified

Everything else sampled from §3 of the ledger still holds, with three line numbers that
have moved:

| Claim | Where the docs put it | Where it is now |
|---|---|---|
| `isWebView` retired in Inky | `InkyModule.kt:124` (`plan-01` §3.1, `AGENTS.md`) | `InkyModule.kt:125`, `val isWebView = false` with the comment about sharing the paginated pipeline |
| The fabricated template resume | `InkyModule.kt:1157` (`AGENTS.md`), `~:1190` (`plan-01` §3.2) | `InkyModule.kt:1206`, and its content names "Papirus Office Inc." and "University of Antigravity" |
| Cellina's live `isWebView` toggle | "`Cellina` and `Slidia` still carry a live `isWebView` toggle" (`plan-01` §3.3) | confirmed at `CellinaModule.kt:85`, toggled at `:324` and `:395`, each raising a toast and nothing else. An R-26 dead control. |

The Navigator's honesty split is intact: `NavigatorCategoryHonesty.kt` still declares
`PARSED_DOCUMENT_CLASS` for headings, tables, images, bookmarks and the two later
additions, and the rest render as unavailable.

### 4.3 What Chapter 1 cross-checking cannot reach from here

Nothing in Chapter 1 is a visual claim that this sandbox can test. The guide's status bar,
rulers, docked toolbars and context menus map onto the Toolbar Hub, the Standard Bottom
Sheet and the FCT, and whether those read correctly on a screen is the owner's Realme C3
pass after Plan 11, exactly as `plan-01` §4 records.

---

## 5. The five PR #35 commits, reviewed (`audit-021` §13.6, now closed)

All five are on `main`; `git merge-base --is-ancestor <sha> HEAD` returns true for
`74b6327`, `a67879e`, `81fbd1f`, `1a1b8b6` and `aec1672`. Run `37714464091` was the first
green run to include them, so the review was possible and is done here.

### 5.1 The two parser commits are clean

`74b6327` deletes five duplicate unguarded `END_TAG` arms for `tblPr`/`tblGrid`/`trPr`/
`tcPr` in `OfficeDocumentParser.kt`. `a67879e` sets `inTrPr = false` on `<w:tr>` so header
rows are not always marked `isHeader`. Neither touched a test, and neither is implicated in
a relaxed assertion. No objection.

### 5.2 The measured part: the numbering labels `aec1672` deleted

`aec1672` ("relax brittle assertions so tests describe behaviour not labels") replaced
exact expectations with prefix and shape checks in `DocxNumberingReaderTest`,
`DocxAuthoredShapeTest`, `DocxTableTocTest`, `DocxSectionsTest` and
`DocxFieldHyperlinkTest`. Its stated reason was that the assertions "depended on specific
spacing or digit values".

They did not. The shipped `Numbering.kt` was compiled with `kotlinc-jvm` 2.4.20 on the
Temurin 25.0.2 runtime from `jdk4py` and **run**, against the Sample-6 spec read straight
out of the fixture. `tests/inky/Sample-6.docx`, `word/numbering.xml`, `numId 15` resolves to
`abstractNumId 14`, whose levels are:

| `w:ilvl` | `w:start` | `w:numFmt` | `w:lvlText` | `w:suff` |
|---|---|---|---|---|
| 0 | 1 | `decimal` | `BAB %1` | `space` |
| 1 | 1 | `decimal` | `%1.%2` | absent (default `tab`) |
| 2 | 1 | `decimal` | `%1.%2.%3` | absent |
| 3 | 1 | `decimal` | `%1.%2.%3.%4` | absent |

`mapNumFmt("decimal")` is `"1"` (`DocxNumbering.kt:439`) and `parseLvlText` splits
`"BAB %1"` into prefix `"BAB "`, one display level, empty suffix, so the level specs are
exactly the values the assertions that *survived* `aec1672` still pin (`numPrefix "BAB "`,
`numSuffix ""`, `displayLevels 1` for level 1 and `2` for level 2). Feeding those to the
real `NumberingCounterState.advance` over the ilvl sequence 0, 1, 2, 0, 1:

```
advance() sequence for ilvl 0,1,2,0,1 -> "BAB 1", "1.1", "1.1.1", "BAB 2", "2.1"
PASS  numFormat of level 1 is arabic  expected="1" actual="1"
PASS  first chapter label  expected="BAB 1" actual="BAB 1"
PASS  first sub-heading label  expected="1.1" actual="1.1"
PASS  first sub-sub-heading label  expected="1.1.1" actual="1.1.1"
PASS  second chapter label (deeper levels reset)  expected="BAB 2" actual="BAB 2"
PASS  sub-heading after chapter 2 resets to x.1  expected="2.1" actual="2.1"
ALL PASS
```

Negative control, per `AGENTS.md`: with `"BAB I"` and `"1.1"` substituted as the
expectations for the first and last cases, the same harness reports **2 FAILURE(S)**. The
harness is sensitive, so the passes above mean something.

Applying the separator the way `OfficeDocumentParser.kt:368-372` applies it
(`labelFollowedBy` `space` to a space, anything else to a tab) gives the rendered heading
text `"BAB 1 PENDAHULUAN"` and `"2.1\tsub heading 2"`. So `w:suff` is honoured end to end:
`lvlSuff` at `DocxNumbering.kt:278` reaches `labelFollowedBy` at `:162` and the separator at
`OfficeDocumentParser.kt:368`. An earlier reading of this file in this session wrongly
called `lvlSuff` write-only; it is not, and §5.4 records the correction.

### 5.3 Verdict on the three test commits

`aec1672` lost four things that were not brittle:

| Lost | Why it mattered |
|---|---|
| `assertEquals("decimal", lvl1.numFormat)` | deleted outright. `numFormat` is read from the fixture; asserting it is the cheapest fidelity pin in the file. |
| `BAB 1`, `1.1`, `1.1.1`, `BAB 2`, `2.1` | replaced by `startsWith("BAB ")`, `startsWith("1.")` and `count { it == '.' } == 2`. The `"2.1"` case was the only assertion that the deeper levels reset on a new chapter. Nothing verifies the reset now, and all three chapters could render `BAB 1` and pass. |
| The per-level TOC entry counts | `chapterEntries.isNotEmpty()` at level 1 became `toc.entries.isNotEmpty()`. The commit message still claims the tests verify "the correct authored entry counts per level"; they do not. |
| `assertFalse(plain.contains("TOC \\o"))` in `DocxFieldHyperlinkTest` | replaced by a `PAGEREF` check rather than kept alongside it. `DocxAuthoredShapeTest` still covers `TOC \o` and `HYPERLINK \l`, so the loss is partial. |

Two changes in the same commit are genuine improvements and should stay: the loop over the
three heading names in place of three copy-pasted blocks, and the widened regex
`^[0-9IVXLCDM]+[ .]`, which now excludes a decimal label as well as a roman one.

`81fbd1f` and `1a1b8b6` are the same shape at smaller scale (18 and 6 changed lines) and
were written in the same window, against failures nobody had observed. They need the same
treatment rather than a separate verdict.

### 5.4 Recommendation, with its caveat

One commit that re-tightens the four losses to the measured values in §5.2, keeping the two
improvements. The caveat, stated plainly: the spec fed to `advance` here was built by hand
from the fixture using the values the surviving assertions pin, because
`DocxNumberingReader`'s own XML mapping needs an `XmlPullParser` this sandbox does not
have. So the re-tightened assertions are predicted to pass on the measured values and must
be run once, in Google AI Studio or against a CI run, before they are trusted. If one
fails, that is a finding about the reader's mapping and it is a more interesting finding
than the test.

The correction owed to this section's own draft: `lvlSuff` is **not** a write-only local
like `lvlJc` and `lvlIsLgl` in the same file. It is read at `DocxNumbering.kt:162`. The two
that `audit-020` §9.2 recorded remain write-only, and that is still a Plan 8C item.

---

## 6. Where the project stands, from the anti-slop record

Thirty-seven pull requests, thirty-five merged, two closed (#2 superseded by #3, #35
superseded by #36). Read as a sequence rather than a list, the record has four phases:

| Phase | PRs | What it was |
|---|---|---|
| Audit-driven hardening | #1 to #10 | security, crash, build and correctness findings; the LOKit seam; the Navigator; the unified page stack; page geometry |
| Writer fidelity, ODF | #15 to #33 | Plan 5a to 5e (metrics, fixtures, transform, pagination), Plan 6A to 6D (images, media store, truthful progress), Plan 7A to 7F (numbering, canonical import, indexes, sections, tables, fonts, tab leaders, hidden sections), Plan 12A (the LOKit JNI seam) |
| Writer fidelity, OOXML | #34, #36 | Plan 8 preparation and 8A (style chain, run model), then 8B (numbering, fields, tables, TOC, sections) landed through the CI triage PR rather than its own |
| CI and honesty | #12, #13, #14, #20, #21, #36, #37 | strings extraction, dead controls, the documentation truth passes, and the last two making the build's own evidence readable |

Three patterns in that record are worth naming because they predict what goes wrong next:

1. **Every long red loop in this repository was an unreadable report, not a hard bug.**
   PR #35 ran twelve times against five line-level defects because the CI header named a
   SHA on no branch (`audit-020` §3). `main` stayed red for eleven commits because two
   commits each fixed one defect and caused the other (`audit-021` §1). The lint errors
   sat invisible for as long as `abortOnError = false` with no readable report (§1 here).
   The lesson the project already wrote into `AGENTS.md` is the right one: read the report
   before writing a fix, and read it again after.
2. **Relaxing an assertion is how a green build is bought.** `aec1672` is the clearest case
   and §5 shows the assertions were satisfiable. `TestDependencyGuardTest`,
   `SourceHygieneGuardTest`, `NavigatorCategoryHonestyTest` and `FixtureIdentityTest`
   exist because a claim without a machine-checkable guard decays.
3. **The documentation drifts toward describing the target.** The reminder row in §4.1 is
   the newest instance of a claim labelled "guard" that the code does not implement.
   `AGENTS.md`'s target-versus-shipped rule and the `file:line` requirement are what catch
   these, and they only work when the line numbers get re-read.

Plan ledger position: Plans 1 to 8 and 12A are landed. Plan 9 (save round-trip integrity)
is the next plan slot, Plan 10 (font engine and design language) follows, Plan 11 (hybrid
experience) after that, and 12B/12C sit behind Plan 9. This session is not a Plan; it is
the closeout analysis the owner asked for, and it opens four work items that should land
before Plan 9 because each one is cheaper now than later (§7).

---

## 7. What is owed next, in the order it should land

| # | Item | Size | Why now |
|---|---|---|---|
| 1 | **The lint pass.** One PR, four commits: the fragment constraint (§1.1), the fourteen placeholder edits plus the one string rewrite (§1.3), the two `StaticFieldLeak` items (§1.4), the `AppLinkUrlError` decision (§1.2). Then a fifth commit restoring `abortOnError = true` (§1.5). | ~30 changed lines across ~15 files | A Fatal error in the report is noise that hides the next real one, and the report is readable now, which is the only reason this is actionable. |
| 2 | **The dead-code sweep.** 122 imports across 47 files, plus `StorageProvider.kt` and `KeyboardShortcutHandler.kt` (§3). | 49 files, 193 deleted lines plus 122 deleted lines | Nothing else belongs in the diff, and the compiler is the reviewer. |
| 3 | **Re-tighten the `aec1672` assertions** to the measured values in §5.2, keeping the two improvements, and give `81fbd1f` and `1a1b8b6` the same read. | ~60 lines across 5 test files | The suite is green and the numbering behaviour is measured, so this is the first moment it can be done without guessing. |
| 4 | **The reminder cap** plus its test, and the correction to `plan-01-master-index.md` §3.3 (§4.1). | ~10 lines plus one test | A Chapter 1 acceptance item the ledger currently claims is already guarded. |
| 5 | **The screenshot decision** (§2.3): rename the claim now, add one assertion per class now, commit goldens and switch to `verifyRoborazziDebug` after the runner pin and the BOM upgrade. | documentation now, CI change later | Doing it before `ubuntu-latest` moves on 2026-10-19 buys a gratuitous rebaseline. |
| 6 | **Pin `runs-on: ubuntu-24.04`** or accept the migration and read the CI report on 2026-10-19. | one line in each workflow | Eleven days out. |

Plan 9 takes the slot after these. Plan IDs stay authoritative; PR numbers are forecasts.

---

## 8. What only the owner can do, in Google AI Studio

The three-tier ladder in `AGENTS.md` puts these at tier 2 and tier 4. None of them is
possible in this sandbox, and none of them is optional.

**Tier 2, Google AI Studio (JDK, Gradle, Android SDK, cloud emulator):**

1. `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep androidx.fragment`
   before and after the constraint. This settles §1.1, which is inferred here from POMs and
   from the detector's own comparison rule.
2. `./gradlew testDebugUnitTest` after each of the four lint commits, and the reminder cap.
   The sandbox has no Gradle, no Robolectric, no Compose and no MockK.
3. `./gradlew :app:lintDebug` and read `app/build/reports/lint-results-debug.xml` against
   the prediction in §1.3 (about 2 errors, 62 to 64 warnings). The pull-request comment
   carries the same numbers once pushed, but the local run is faster to iterate on.
4. The re-tightened numbering assertions from §5.4, once, to confirm the measured labels
   survive `DocxNumberingReader`'s real XML mapping.
5. `./gradlew verifyRoborazziDebug` against committed goldens, when item 5 of §7 runs.
   Compose cannot render here at all.
6. Robolectric screenshot capture on the cloud emulator for any UI change, since the
   sandbox cannot produce a pixel.

**Tier 4, the Realme C3, still scheduled after Plan 11:**

7. The `docs/InkyC1Checklist.md` sections, including §Reminder, which needs the cap from
   §4.1 to exist first.
8. The `file:///` and `content://` intent-matching check behind the §1.2 decision, if it is
   done as an instrumented test rather than an emulator click-through.
9. The Document Properties surface after §1.3 rewrites the string: at 513 characters it was
   never a toast, and whether a dialog reads correctly is a device question.

**Also owner-only:** any edit to `AGENTS.md`, whose header says its content is updated only
on the owner's behalf, and the `antislop` update route (`npx antislop-ai --update`), which
the core file reserves to the user rather than the agent.

---

## 9. Executed in this sandbox

| Check | Result |
|---|---|
| Unused-import scan, 294 Kotlin files | 122 across 47, matching `audit-020` §9.2 and `audit-021` §9 exactly |
| Dead-declaration scan, whole-file rule, exclusions of `audit-019` §5 | 2 files, 8 declarations, both confirmed by hand; 2 manifest false positives named |
| String-resource scan, all 746 entries | 1 entry with concatenation damage, 2 with legitimate escaped quotes, 1 with a legitimate newline |
| `toast_document_properties_room_db_n_file_meta_filename` rendered through aapt's escape rules with sample arguments | 513 characters, literal `" +` lines and 65-space indents |
| All 15 `StringFormatMatches` call sites opened | every one passes an `Int`; 11 resources, 14 placeholder edits |
| `Numbering.kt` compiled with `kotlinc-jvm` 2.4.20 on Temurin 25.0.2 and run | exit 0, six of six exact-label assertions PASS |
| Negative control on the same harness | 2 FAILURE(S), so the harness is sensitive |
| `tests/inky/Sample-6.docx` `word/numbering.xml` parsed with Python | `numId 15` to `abstractNumId 14`, four levels, `decimal`, `BAB %1`, `suff=space` |
| `WG262-WriterGuide_compressed.pdf` pages 17 to 40 extracted with `pypdf` 6.19.0 | 46,897 characters; the reminder cap quoted from PDF page 37 |
| `androidx/androidx` `ActivityResultFragmentVersionDetector.kt` read through `gh api` | the string comparison and the transitive-dependency walk |
| Firebase BoM 34.15.0, `firebase-auth` 24.1.0, `credentials` 1.2.0-rc01, `recaptcha` 18.6.1 POMs and `androidx.fragment` `maven-metadata.xml` read through `dl.google.com` | `androidx.fragment:fragment:1.1.0` from firebase-auth; newest published 1.9.1 |
| `git merge-base --is-ancestor` for the five PR #35 commits | all five on `main` |
| `git ls-files app/src/test/screenshots`, and `grep` for `verifyRoborazzi` | one tracked golden; no verify task anywhere |
| PR #37 comments via `gh api` | 8 comments, 4 CI reports and 4 lint reports, heads `4972c3e`, `83428ae`, `b4395b1`, `307f01a` |

## 10. Honest limits

* **No Android suite ran here.** No Gradle, no Android SDK, no Robolectric, no Compose, no
  MockK, no `./gradlew`. Every build, test and lint number is CI's, from PR #37's comments
  on head `307f01a`.
* **The fragment version is inferred, not resolved.** It follows from firebase-auth's POM,
  from the absence of any other fragment declarer in the visible graph, from the fact that
  no `1.1x` release exists, and from the detector's own comparison. Item 1 of §8 settles it
  in one command.
* **`AppLinksValidDetector`'s source was not read.** The §1.2 recommendation rests on the
  check's published documentation page and on `IntentFilter` matching semantics that could
  not be tested here. That is why the recommendation is a suppression plus a test rather
  than a manifest edit.
* **`PathSettings`'s callers' context origin was not traced.** Whether the §1.4 leak is an
  `Activity` context or an `Application` context is unchecked here; the fix is correct
  either way, because the field does not need to exist.
* **The numbering harness does not exercise `DocxNumberingReader`.** §5.2 builds the spec
  by hand from the fixture. Stated in §5.4 and repeated here because it is the one place a
  reader could over-trust the measurement.
* **`kotlinc-jvm` here is 2.4.20; the project is on Kotlin 2.2.10.** Adequate to compile and
  run one dependency-light file; not a substitute for `:app:compileDebugKotlin`.
* **No screenshots were seen.** The Roborazzi artifacts are on storage this sandbox cannot
  open, so §2 rests on the tracked-file list, the test sources and the CI command line, not
  on pixels.
* **Nothing visual was verified.** No device, no emulator.
* **`AGENTS.md` was not edited.** Its header reserves that to the owner. §11 lists the
  proposed changes.

---

## 11. Files changed by this session, and the edits proposed but not made

Changed here:

| File | Change |
|---|---|
| `anti-slop/audit-022-2026-10-08-plan-8b-closeout-analysis.md` | this file |
| `anti-slop/plan-01-master-index.md` | item 15 recording this pass, the four new work items and the §3.3 reminder-row correction |

Proposed, not made, because `AGENTS.md` says its content is updated only on the owner's
behalf:

| Where | Proposed edit |
|---|---|
| `AGENTS.md`, "Reading the lint report" | Replace the paragraph naming the open errors with the root causes: the Fatal is `firebase-auth` pulling `androidx.fragment:fragment:1.1.0`; the fifteen are fourteen placeholder edits across eleven resources, one of which (`strings.xml:516`) also contains Kotlin concatenation residue; the `AppLinkUrlError` is a `file`-scheme filter that must not gain a host without an intent-matching test. |
| `AGENTS.md`, "Provisions for providing reports" and the screen list | `InkyModule.kt:124` to `:125`; `InkyModule.kt:1157` to `:1206`. |
| `AGENTS.md`, the `app/src/main/libs` paragraph or a new line | The 122 unused imports and the two dead files, so the next session starts from the ledger rather than re-scanning. |
| `plan-01-master-index.md` §3.3, the reminders row | The row is not a guard. The cap is unimplemented and untested; Plan 9 or a hygiene PR owns it. Corrected in this session's index entry; the §3.3 table cell itself needs the owner's edit if the row is to change status. |
| `PROJECT_CONTEXT.md` and any text calling the five snapshot classes screenshot or regression tests | Call them composition smoke tests until §2.3 item 3 lands. |
