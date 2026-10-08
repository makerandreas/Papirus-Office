# Audit 021 (2026-10-08): the MockK regression, the workflow split, and the lint report

**Baseline:** `main` at `323dc94` ("Re-adding OOXML Documentation (batch 9)"), branch
`arena/b85c4d48-papirus-office`, PR #37, head `83428ae`. The local clone was a
shallow one-commit checkout and was deepened by 40 for this session, so the eleven
commits after the PR #36 merge are inspectable here.
**Gate:** CI run `37717832692` on head `b4395b1`, all three jobs green,
**434 tests run, 0 failed, 0 errors, 0 skipped, 45.05 s across 82 suites**, against
PR #36's 402 across 73. Two earlier runs on this branch were green as well
(§10.1). Lint reports 17 errors and 58 warnings behind a green job because
`abortOnError = false`; §6.2 lists them and §13 owns them.
**Scope:** (1) establish why `main` has been red since `891d077`, (2) check the two
Copilot suggestions the owner brought against the MockK source rather than against
plausibility, (3) fix it, (4) stop the same class of regression from being silent,
(5) split the two workflows that had grown into duplicates, (6) make lint readable,
(7) re-run the unused-code scan.
**Method:** `gh api` for runs, jobs, step conclusions, annotations, artifacts and
pull-request comments; `git log`/`git show`/`git diff` on the deepened history;
source reading; `gh api repos/mockk/mockk` and `gh api repos/takahirom/roborazzi`
for the two libraries whose behaviour the fix turns on; `jdk4py` 25.0.2 plus
`kotlin-compiler` 2.4.20 from npm to compile and **run** the new guard test;
`python3` execution of `scripts/lint-dump-comment.py` against four synthetic
inputs. No Gradle and no Android SDK exist in this sandbox (AGENTS.md), so every
build and test number below came from CI run `37714464091`, not from here.

---

## 0. Findings in one screen

| # | Finding | Evidence | Consequence |
|---|---|---|---|
| A | **`main` was green on tests at `891d077`, and red from `912de8e` onward.** Two independent defects, one per commit. `891d077` passed both unit-test jobs and failed both lint jobs; `912de8e` fixed lint and broke the test compile. | §1, runs `37661728786`/`37661728671` and `37673343000`/`37673343359` | The failure was never one thing, and reading only the newest run hides that. |
| B | **`912de8e` deleted three lines of test configuration that lint never touched.** `testImplementation(libs.mockk)`, `unitTests { isReturnDefaultValues = true }` and the Byte Buddy agent `jvmArgs`. | §2 | `:app:compileDebugUnitTestKotlin` failed on 20 of the 22 runs `main` produced after PR #36. |
| C | **Both Copilot suggestions are wrong.** `import io.mockk.any` cannot resolve; `any()` is a member of `io.mockk.MockKMatcherScope`. `mockk-android` is the instrumentation artifact and does not belong on a JVM unit-test classpath. | §3, mockk `API.kt:737` and `:802` | Neither was applied. Applying the import would have added a second red run and a new unresolved reference. |
| D | **Nothing in the build catches a dropped test dependency.** The catalog still declared `mockk = "1.13.16"` and the `mockk` library alias while no module used them, and 30 test methods in four classes imported `io.mockk`. | §4 | `TestDependencyGuardTest` added. It names the dependency line, not the fifty use sites. |
| E | **`ci.yml` and `build.yml` had become the same workflow twice.** Both ran test and lint on every push and pull request, and both posted a CI report comment. | §5 | Split: `ci.yml` verifies, `build.yml` distributes. A pull request now gets one CI report and one lint report. |
| F | **The nightly release has been frozen since PR #36.** `build.yml`'s build job is `needs: [test, lint]`, so all eleven pushes after the merge skipped it. The published nightly still points at `8ff0a378`. | §5.3, `gh release view nightly` | Recorded. After this PR the nightly comes from the 02:00 UTC cron, not from every push. |
| G | **Lint has been reporting 17 errors and 61 warnings the whole time, invisibly.** `abortOnError = false` made the job green without touching a single finding. The report only reached an artifact the sandbox cannot open. | §6, run `37714464091` lint comment | `scripts/lint-dump-comment.py` now mirrors it into a pull-request comment. The 17 errors are listed in §6.2 as a backlog, not fixed here. |
| H | **`"OldTargetSdkVersion"` is not a lint issue id.** It sits in the `disable` set `912de8e` added, lint reports it back as `UnknownIssueId`, and the id that actually fires is `OldTargetApi`. | §6.3 | Corrected to `OldTargetApi`. |
| I | **`roborazzi.test.record` in `gradle.properties` is a real flag with a local side effect.** The Roborazzi plugin forwards `roborazzi.*` Gradle properties into the test JVM, so every local test run would overwrite the tracked `app/src/test/screenshots/greeting.png`. | §7.1 | Moved to the CI command line only. |
| J | **Record mode was off when CI was last green on tests.** With no flag, `RoborazziTaskType.None` makes `captureRoboImage` return before capturing. The five snapshot classes proved composition, not pixels. | §7.2, artifact size 224,245 → 794,754 bytes | Recorded. Run `37714464091` is the first run that rendered them. |
| K | **`actions/upload-artifact@v4` targets Node.js 20** and was being forced onto Node.js 24 in both jobs. | §8 | Bumped to v7. `checkout@v6` and `setup-java@v5` produced no warning and were left alone. |
| L | **The unused-import scan reproduces exactly.** 122 unused imports across 47 of 293 Kotlin files, the same figure `audit-020` §9.2 recorded on 285 files. | §9 | Still marked safely deletable, still a dedicated sweep, still not mixed into a CI recovery. |

---

## 1. What actually broke, and when

Every row below is a step conclusion read through
`gh api repos/makerandreas/Papirus-Office/actions/runs/<id>/jobs`.

| Commit | Pushed | ci.yml run | build.yml run | Unit test job | Lint job | Build job |
|---|---|---|---|---|---|---|
| `8ff0a37` PR #36 merge | 10-07 16:16Z | did not exist | success | success | did not exist | success |
| `891d077` ci: enhance CI pipeline | 10-07 17:46Z | `37661728786` | `37661728671` | **success** (both) | **failure** (both) | skipped |
| `912de8e` build: configure build pipeline | 10-07 19:17Z | `37673343000` | `37673343359` | **failure** (both) | success | skipped |
| `d49580a` .. `323dc94`, nine doc batches | 10-08 00:51Z to 01:08Z | 9 runs | 9 runs | **failure** (all 18) | success | skipped |

Twenty-two runs on `main` after the PR #36 merge. Twenty unit-test job failures,
two lint job failures, zero builds, zero nightly releases.

The two defects never overlapped, which is why the sequence reads as one long
failure and is not one:

* At `891d077` the test compile was fine. `testImplementation(libs.mockk)` was in
  the build script, `isReturnDefaultValues = true` was in `testOptions`, and the
  Byte Buddy agent `jvmArgs` were in `tasks.withType<Test>()`. Both unit-test jobs
  passed: the ci.yml step took 4m14s, the build.yml step 5m16s. What failed was
  `./gradlew lintDebug`, at 6m59s and 6m05s.
* At `912de8e` lint went green and the test compile broke. The same edit did both.

Check-run annotations stay as thin as `audit-020` §1 recorded. For the `891d077`
lint job (check-run `112930675231`) the only failure annotation is
`Process completed with exit code 1.` What lint said at `891d077` is not
recoverable from here: the raw log is on `results-receiver.actions.githubusercontent.com`
and the `lint-reports` artifact is on blob storage, both unreachable. §6 explains
what is known instead.

---

## 2. The three lines `912de8e` removed

`git show 912de8e -- app/build.gradle.kts` has four hunks. One adds the `lint {}`
block, one is the `LoadingStage`-driven reformat of `testOptions`, and two delete
test configuration that has no relationship to lint:

| Removed | Why it mattered |
|---|---|
| `testImplementation(libs.mockk)` | `gradle/libs.versions.toml:46` still declares `mockk = "1.13.16"` and `:104` the library alias. Four test classes (`DocumentCacheRepositoryTest`, `DocumentSessionManagerTest`, `InkyDocumentMetadataRepositoryTest`, `PapirusApiClientMockTest`) import `io.mockk`. Without the dependency, every `mockk`, `every`, `coEvery`, `coVerify`, `mockkStatic` and `unmockkStatic` in them is unresolved, and so is `any()` inside the matcher lambdas. |
| `unitTests { isReturnDefaultValues = true }` | `PapirusApiClientMockTest` carries no `@RunWith(RobolectricTestRunner::class)`. It reaches `android.util.Log` as the `android.jar` stub. The test mocks `Log` statically, so the stub body is not the path it exercises, but the option is what keeps any unmocked stub method from throwing `Stub!`. |
| `jvmArgs("-XX:+EnableDynamicAgentLoading", "-Djdk.attach.allowAttachSelf=true")` plus the `doFirst` that adds `-javaagent:<byte-buddy-agent jar>` | MockK's inline mock maker attaches a Byte Buddy agent at runtime. CI is on Zulu 17, where dynamic attach is permitted, so this is warning suppression rather than a hard requirement. It was in the proven-green configuration and there is no evidence for removing it. |

All three are restored. The result is checked against the version that CI passed:

```
diff <(git show 891d077:app/build.gradle.kts) app/build.gradle.kts
```

gives three hunks: the `lint {}` block `912de8e` added, and two comments naming
what the restored lines are for. Nothing else differs.

`isReturnDefaultValues` and the agent `jvmArgs` are both belt-and-braces on this
suite as it stands. They are restored because they were in the configuration CI
proved, not because a failure without them was measured. That distinction is the
reason the run in §10 matters.

---

## 3. The two Copilot suggestions, checked against the MockK source

The owner brought two suggested fixes. Both were checked against the library
rather than against plausibility, because `AGENTS.md` rule 3 under "Reading the CI
report" exists for exactly this: *fix the line the report names; do not add an
import that looks plausible.* PR #35 lost four runs to that mistake.

### 3.1 `import io.mockk.any` cannot exist

Suggestion: "Add the missing import at the top of the file: `import io.mockk.any`."

`any()` is not a top-level function in package `io.mockk`. Read through
`gh api repos/mockk/mockk/contents/modules/mockk-dsl/src/commonMain/kotlin/io/mockk/API.kt`:

```
737: open class MockKMatcherScope(
802:     inline fun <reified T : Any> any(): T = match(AnyTypedMatcher(T::class), T::class)
```

`:802` is inside the class opened at `:737`. It resolves because `every {}` and
`coEvery {}` take a lambda whose receiver is `MockKMatcherScope`, so `any()` is in
scope inside the braces without any import. There is no `io.mockk.any` to import,
so the suggested line would itself become the next `Unresolved reference`, at the
top of the file instead of at line 87.

The suggestion also reads the symptom backwards. `any()` at
`PapirusApiClientMockTest.kt:87` and `coEvery` at `:98` are two use sites of one
missing library. `coEvery` at `:7` is already imported correctly; the import is
unresolvable because the artifact is not on the classpath. Adding a second import
does not put an artifact on a classpath.

### 3.2 `mockk-android` is the instrumentation artifact

Suggestion: `testImplementation("io.mockk:mockk-android:1.13.16")`, with the note
that "the `mockk-android` module is necessary since your test uses `android.util.Log`."

`mockk-android` is the artifact for `androidTestImplementation`, on a device or
emulator, where the inline mock maker cannot instrument and MockK falls back to a
dexmaker proxy. These are JVM unit tests: `testDebugUnitTest` runs on the host
JDK, and `android.util.Log` is present because AGP puts the mockable
`android.jar` on the unit-test classpath, not because an Android mock artifact is
needed. Putting `mockk-android` on `testImplementation` adds an agent that has no
host JVM to attach to.

The hardcoded `1.13.8` in the first suggestion is also a step backwards: the
catalog already pins `1.13.16`, and every other dependency in
`app/build.gradle.kts` goes through `libs.*`. A version literal in the module
build script is how a catalog stops being the source of truth.

### 3.3 What was applied instead

One line, the one that was deleted:

```kotlin
testImplementation(libs.mockk)
```

---

## 4. The guard

Nothing in the build noticed that the catalog declared a library no module used
while four test classes imported it. Gradle does not warn about an unused catalog
entry, and the compiler's output names fifty use sites instead of the one
dependency line.

`app/src/test/java/com/example/TestDependencyGuardTest.kt` closes that. Two cases:

1. For each of five import prefixes (`io.mockk.`, `org.robolectric.`,
   `com.github.takahirom.roborazzi.`, `kotlinx.coroutines.test.`,
   `androidx.compose.ui.test.`), if any file under the test source set imports it,
   some `testImplementation(...)` line in `app/build.gradle.kts` must name the
   matching catalog accessor.
2. Every `libs.<accessor>` in `app/build.gradle.kts` must exist as a library alias
   in `gradle/libs.versions.toml`, so a deleted catalog entry fails here rather
   than as a Gradle script evaluation error.

The failure message names the dependency line and says not to touch the test file:

```
4 test file(s) import io.mockk.* (DocumentCacheRepositoryTest.kt,
DocumentSessionManagerTest.kt, InkyDocumentMetadataRepositoryTest.kt,
PapirusApiClientMockTest.kt) but no testImplementation line in
app/build.gradle.kts declares libs.mockk. Add it back; do not add an import to
the test file.
```

### 4.1 Executed here, with a negative control

`AGENTS.md` records that `jdk4py` from PyPI plus `kotlin-compiler` from npm gives a
working `kotlinc` without Gradle. This pass went one step further and **ran** the
compiled test, because its only dependencies are `org.junit.Assert` and
`java.io.File` and a two-symbol stub covers both.

| check | result |
|---|---|
| `TestDependencyGuardTest.kt` + a JUnit stub, `kotlinc-jvm 2.4.20` on Temurin 25.0.2 | **exit 0**, `TestDependencyGuardTest.class`, `TestDependencyGuardTest$GuardedLibrary.class` |
| both cases, cwd `app/`, the fixed build script | **PASS**, **PASS** |
| both cases against `git show HEAD:app/build.gradle.kts` in a reconstructed `app/` tree | **FAIL** with the message quoted above; case 2 still PASS |
| all 62 `libs.*` accessors in `app/build.gradle.kts` against the catalog aliases | 0 missing, including the seven commented-out ones |

The negative control is the point: the guard's predicate is false on the exact tree
that produced twenty red unit-test jobs, and true on the fixed one.

---

## 5. The workflow split

### 5.1 What had grown

`891d077` created `.github/workflows/ci.yml` (196 lines) and added a lint job to
`.github/workflows/build.yml` (43 lines). The result was two workflows that both
ran the same Gradle work on the same events:

| | ci.yml before | build.yml before |
|---|---|---|
| name | Papirus Office CI | Papirus Office CI/CD |
| `push` branches | main, develop | main, develop |
| `push` tags | `v*`, `*.*.*` | `v*`, `*.*.*` |
| `pull_request` | main, develop | main, develop |
| `schedule` | no | 02:00 UTC |
| `release` | no | published, created |
| unit tests | `testDebugUnitTest -Proborazzi.test.record=true` | `testDebugUnitTest` |
| lint | `lintDebug` | `:app:lintDebug` |
| CI report comment | yes | yes |
| native inventory | yes, guarded by `if [ -f ... ]` | yes, unguarded |
| build job | assembleDebug or assembleRelease, APK artifact | assembleDebug or assembleRelease, nightly or release assets |

So a pull request ran the suite twice, ran lint twice with two different task
paths, and received two CI report comments that differed only in the flags. The
2026-10-08 documentation pushes each produced two runs of the same four Gradle
invocations.

`build.yml` is the workflow the documentation names: `AGENTS.md` "Handling
`build.yml`", `PROJECT_CONTEXT.md:348`, `KEYSTORE_GUIDE.md:27`, and eight audit
files. `ci.yml` was one commit old and undocumented.

### 5.2 The owner's decision and the resulting split

The owner chose to keep both and split the roles rather than delete one.

**`ci.yml`, verification only.**

* Triggers: `push` to `main`/`develop`, `pull_request` to `main`/`develop`,
  `workflow_dispatch`. The `tags` trigger is gone; distribution owns tags.
* Jobs: `test` (unit + Roborazzi, native inventory, CI report comment), `lint`
  (`:app:lintDebug`, lint report comment), `build` (`assembleDebug` plus the APK
  artifact, `needs: [test, lint]`).
* The release-APK branch of the build job is gone, because no event that reaches
  this workflow can produce a release APK. A dead conditional in a workflow is a
  defect the same way a dead branch in code is.
* The keystore secrets are no longer exported here. `assembleDebug` uses the
  auto-generated debug key, and `app/build.gradle.kts` only reads
  `KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` inside
  `signingConfigs.create("release")` when a keystore file exists.
* The `if [ -f scripts/... ]` guards around the native inventory and the CI report
  are gone. Both scripts are in the repository; a guard that silently skips the
  evidence step when the script is missing is worse than a failure.
* `permissions` on the build job drops from `contents: write` to `contents: read`.
  It uploads an artifact and creates no release.

**`build.yml`, distribution only.**

* Triggers: `schedule` 02:00 UTC, `push` tags `v*`/`*.*.*`, `release`
  published/created, `workflow_dispatch`. No `push: branches`, no `pull_request`.
* One job. The `test` and `lint` jobs and `needs: [test, lint]` are gone.
* New step `Verify the tagged commit before publishing`, gated on
  `github.event_name == 'release' || startsWith(github.ref, 'refs/tags/')`, runs
  `./gradlew testDebugUnitTest`. A tag or a release event has no `ci.yml` run
  behind it, because `ci.yml` no longer triggers on tags, and a published APK
  should not come from a commit nothing verified.
* The three nightly-path conditions drop their now-unreachable
  `github.event_name != 'pull_request'` clause.
* `GEMINI_API_KEY`, `GOOGLE_CSE_CX`, `GOOGLE_CSE_API_KEY` and
  `GOOGLE_FONTS_REST_API` move from the two assemble steps to the job `env`, where
  they were already duplicated verbatim.

Both files parse with `yaml.safe_load`, and the step inventory of each was printed
and read back before committing.

### 5.3 The consequence for the nightly, stated plainly

`gh release view nightly` on 2026-10-08:

```
name              Papirus Office Nightly Build 1.0-nightly_07102026_456
tagName           nightly
targetCommitish   8ff0a378f32cdfc098409b8cfa20e20c24df7f0e
publishedAt       2026-10-07T16:22:52Z
assets            app-arm64-v8a-debug.apk   264,323,341
                  app-armeabi-v7a-debug.apk 199,732,861
```

The published nightly points at the PR #36 merge commit. It has not moved since,
because `build.yml`'s build job was `needs: [test, lint]` and the test job failed
on all eleven pushes after it. No nightly release was dropped nine times on
2026-10-08; nine were skipped.

After this split the nightly comes from the 02:00 UTC cron on `main`'s head, or
from `workflow_dispatch`. A merge to `main` no longer refreshes it within minutes.
That is the trade the split buys, and it is the owner's to reverse by putting
`push: branches` back on `build.yml` if the immediacy is wanted more than the
halved runner time.

---

## 6. Lint

### 6.1 What was decided

The owner chose to keep `abortOnError = false` and make the report readable, not to
flip it and fix every finding in this pass. That is the same shape as PR #36:
make the failure legible first, then close it out against what it says.

`scripts/lint-dump-comment.py` reads `app/build/reports/lint-results-debug.xml`
and falls back to the `.txt` report. It posts one comment with the same
`HEAD_SHA`/`HEAD_REF`/`BASE_SHA`/`BASE_REF` header convention as
`ci-dump-comment.py`, a summary line that says out loud that the job stays green
with errors present, every error as `path:line:col [IssueId] message`, a table of
issue ids with severity and count, and the warnings. Paths are made
repo-relative through `GITHUB_WORKSPACE`, because lint writes some locations as
absolute runner paths (`gradle/wrapper/gradle-wrapper.properties`) and others
relative to the module (`src/main/java/...`).

Executed here against four inputs: a synthetic four-issue XML report, the
text-report fallback, an empty report, and both reports missing. Each produced the
right output, including the empty case, which says "this run reported none"
instead of promising errors below.

### 6.2 What lint actually says, from run `37714464091`

`lint 9.1.1: 17 errors, 61 warnings across 17 issue ids.` The job is green because
of `abortOnError = false`. These are the errors, and they are the backlog for a
lint pass, not this PR's scope:

| Issue id | Severity | Count | Where |
|---|---|---|---|
| `InvalidFragmentVersionForActivityResult` | **Fatal** | 1 | `app/src/main/java/com/example/MainActivity.kt:64:9`, "Upgrade Fragment version to at least 1.3.0" |
| `StringFormatMatches` | Error | 15 | `CellinaModule.kt:1060`, `:1583` (x2); `HomeSubpages.kt:1248`, `:1254`; `InkyModule.kt:2071`, `:3452` (x3), `:3594`, `:3962`; `LoadSaveGeneralSubpage.kt:536`; `SlidiaModule.kt:1120`, `:1786`; `UniversalFormsSheet.kt:1025` |
| `AppLinkUrlError` | Error | 1 | `app/src/main/AndroidManifest.xml:67:17`, "At least one `host` must be specified" |

All fifteen `StringFormatMatches` are the same defect: a `strings.xml` entry
declares `%1$s` and the call site passes an `Int`. Lint's own wording is
"conversion is `s`, received `int` (Did you mean formatting character `d`, 'o' or
`x`?)". The affected strings are `toast_inserted_row_above_row_activecellrow`,
`toast_frozen_pane_at_col_row`, `toast_drop_cap_set_to_lines_lines`,
`toast_found_match_at_character_index`,
`toast_document_properties_room_db_n_file_meta_filename`, `toast_font_size_changed`,
`toast_reminder_set_at_paragraph_cursorpara`,
`toast_auto_recovery_set_to_every_validminutes_minutes`,
`toast_exported_slide_activeslideindex_1_as_png_image`,
`toast_custom_show_customshowname_created_with_parsed_size` and
`toast_built_sdk_form_with_res_totalcontrolscreated`. Each is a Plan 3A
`strings.xml` extraction where the placeholder type did not follow the argument
type. At runtime `String.format` with `%s` and an `Int` does not throw, it prints
the number, so this is a correctness and localization defect rather than a crash.
Two of them (`strings.xml:130` `%d ... minutes`, `:184` `%d ... words`) are also
flagged `PluralsCandidate`, which is the same strings needing a real plural.

The 61 warnings cluster as: `UseKtx` 35, `VectorRaster` 6, `IconLocation` 3,
`AndroidGradlePluginVersion` 2 (Gradle 9.3.1 against 9.8.1, AGP 9.1.1 against
9.4.1), `ApplySharedPref` 2, `DefaultLocale` 2, `PluralsCandidate` 2,
`StaticFieldLeak` 2 (`PapirusAssetEngine.kt:37`, `StorageProvider.kt:127`, both a
`Context` reachable from a static field), `UnknownIssueId` 2, then one each of
`OldTargetApi`, `RedundantLabel`, `SwitchIntDef`, `UnnecessaryArrayInit` and
`UnusedAttribute`.

`StaticFieldLeak` and `InvalidFragmentVersionForActivityResult` are the two with a
crash or leak behind them rather than a string. They are named here so the lint
pass starts from evidence instead of from the count.

### 6.3 One defect in the `disable` set, corrected

The lint report contains:

```
app/build.gradle.kts:183:8 [UnknownIssueId] Unknown issue id "OldTargetSdkVersion"
```

twice, and separately

```
app/build.gradle.kts:19:5 [OldTargetApi] Not targeting the latest versions of Android
```

`912de8e` added a `disable` set of fourteen ids. Thirteen are real. The fourteenth,
`"OldTargetSdkVersion"`, is not an AGP lint id, so lint reports it back as an
unknown id and the check the author meant to silence, `OldTargetApi`, still fires.
Corrected to `"OldTargetApi"` with a one-line comment. This is the only lint
finding fixed in this pass, because it is the only one that is a defect in the
configuration `912de8e` itself added.

The fix was predicted before it was pushed and then measured. Two runs of the same
lint report:

| Run | Head | Report line | `UnknownIssueId` | `OldTargetApi` |
|---|---|---|---|---|
| `37714464091` | `4972c3e`, id still `"OldTargetSdkVersion"` | 17 errors, 61 warnings, 17 issue ids | 2 | 1 |
| `37716347743` | `83428ae`, corrected to `"OldTargetApi"` | 17 errors, 58 warnings, 15 issue ids | absent | absent |
| `37717832692` | `b4395b1`, docs only | 17 errors, 58 warnings, 15 issue ids | absent | absent |

Three warnings and two issue ids, exactly the ones the correction targets, and the
seventeen errors untouched. The third run holds the corrected figures, which is what
a docs-only commit should do to a lint report. That is the whole claim about this fix: it silences one
warning and removes one bogus id, and it does not hide an error.

### 6.4 What is still unknown

Why the lint job failed at `891d077` cannot be read from here. The step conclusion
and the exit code are available; the output is not. The most likely reading is that
`abortOnError` defaulted to true and at least one of the seventeen findings above
was already present, since none of them is in code `912de8e` touched. The
`lint {}` block silences thirteen ids including `HardcodedText`,
`MissingTranslation` and `UnusedResources`, which are large categories in this
repository, and that is consistent with the `lint-reports` artifact shrinking from
144,253 bytes at `891d077` to 63,563 bytes here. Artifact size is the only signal
available and it is an inference, not a measurement of the earlier report. The
`@param:StringRes` change to `LoadingStage.kt` in the same commit does not appear
anywhere in the current report, so whether it fixed a finding or was speculative
cannot be established either.

---

## 7. Roborazzi

### 7.1 `roborazzi.test.record` in `gradle.properties` is a real flag

Worth stating, because the obvious reading is that a Gradle property cannot reach a
test JVM's `System.getProperty`. It can, through the plugin. From
`takahirom/roborazzi` `include-build/roborazzi-gradle-plugin/src/main/java/io/github/takahirom/roborazzi/RoborazziPlugin.kt`:

```
357: val roborazziProperties: Map<String, Any?> =
358:   project.providers.gradlePropertiesPrefixedBy("roborazzi").get()
...
218: return roborazziProperties["roborazzi.test.record"] == "true" || ...
...
561: if (!isTaskPresent) {
562:   test.systemProperties.putAll(roborazziProperties)
```

`gradlePropertiesPrefixedBy` covers both `gradle.properties` entries and `-P`
command-line properties. When no Roborazzi task is in the graph, the plugin copies
them into the test task's system properties in `doFirst`, so
`System.getProperty("roborazzi.test.record")` is `"true"` in the worker and
`roborazziSystemPropertyTaskType()` returns `Record`.

The consequence of putting it in `gradle.properties` is that it also applies to
every local run. `app/src/test/screenshots/greeting.png` is tracked
(`git ls-files` confirms it), so any `./gradlew testDebugUnitTest` would overwrite
a committed binary and leave twelve untracked PNGs in the source tree. Moved to the
CI command line, where `ci.yml` already had it, and taken out of
`gradle.properties`.

### 7.2 Record mode was off when CI was last green on tests

`RoborazziTaskType.of(false, false, false)` returns `None`, `None.isEnabled()` is
false, and the capture entry points in
`roborazzi/src/main/java/com/github/takahirom/roborazzi/Roborazzi.kt` open with
`if (!roborazziOptions.taskType.isEnabled()) return`. Fourteen of them do.

So at `891d077`, where no Roborazzi property was set anywhere, the five snapshot
classes (`GreetingScreenshotTest`, and the four `891d077` added) composed their
content, called `captureRoboImage`, and captured nothing. They were composition
smoke tests. That is not worthless, and it is why they were fast and green, but it
is not a screenshot test.

Run `37714464091` is the first run of this repository with record mode on. The
artifact sizes are the available evidence that images were written:

| Run | Record flag | `unit-test-and-snapshot-reports` |
|---|---|---|
| `37661728786` (`891d077`) | absent | 224,245 bytes |
| `37714464091` (this PR, head `4972c3e`) | `-Proborazzi.test.record=true` | 794,754 bytes |
| `37716347743` (this PR, head `83428ae`) | `-Proborazzi.test.record=true` | 794,660 bytes |
| `37717832692` (this PR, head `b4395b1`) | `-Proborazzi.test.record=true` | 794,859 bytes |

The artifact also carries nine more suites of JUnit XML in the later runs, which is
a few kilobytes. The remaining ~570 KB across thirteen PNGs is consistent with
images that were not there before, and three runs of near-identical trees agree
within 199 bytes while the one run without the flag sits 570 KB lower, so the delta
is reproducible rather than a one-off. The artifact itself cannot be opened from
here, so this stays an inference from size, and the way to confirm it is to download
`unit-test-and-snapshot-reports` from run `37717832692` and list
`app/src/test/screenshots/`.

Per-suite JUnit times for the snapshot classes in the green run:
`CellinaSnapshotTest` 3 tests 6.65s, `InkySnapshotTest` 3 tests 1.48s,
`PagellaSnapshotTest` 3 tests 0.98s, `SlidiaSnapshotTest` 3 tests 0.99s,
`GreetingScreenshotTest` 1 test 0.11s. Those are runner figures.

### 7.3 What this leaves open

Nothing compares the screenshots to anything. Record mode writes files; there is no
golden set and no `verifyRoborazziDebug` in CI, so a rendering regression changes
the artifact and no job notices. That is a deliberate gap for now, not an oversight
to hide: it belongs with the Plan 11 device and visual pass, or with a decision to
commit goldens and switch CI to `verify`. It is recorded here so the snapshot
classes are not described as regression tests.

---

## 8. Actions versions and runner notices

Both jobs carried this annotation on every run:

```
warning: Node.js 20 is deprecated. The following actions target Node.js 20 but are
being forced to run on Node.js 24: actions/upload-artifact@v4.
```

`actions/upload-artifact` v4 to v7. Read from the release notes: v5 is Node 24
support treated as breaking, v6 makes Node 24 the default and needs runner
2.327.1 or newer, v7.0.0 adds an opt-in `archive: false` for single-file direct
uploads and moves the package to ESM, v7.0.2 improves 429 retries. Nothing this
repository uses changes: `name`, `path`, `if-no-files-found` and `retention-days`
behave the same. GitHub-hosted `ubuntu-latest` is well past runner 2.327.1.

Left alone on purpose:

* `actions/checkout@v6` and `actions/setup-java@v5`. Neither produced a warning.
  `checkout` v7.0.1 and `setup-java` v6.0.1 exist, and a major bump with no
  evidence behind it is a change for its own sake.
* `softprops/action-gh-release@v3`, current at v3.0.3.
* `runs-on: ubuntu-latest`. The runner image carries a notice that the label
  migrates to Ubuntu 26 beginning 2026-10-19, eleven days after this session. The
  repository has always used `ubuntu-latest`; pinning `ubuntu-24.04` is a decision
  for the owner, and if the migration breaks anything it will show up as a run
  failure with the CI report attached, which is now readable.

---

## 9. Unused code, marked not deleted

The scan `audit-020` §9.2 describes, re-run on this tree with the same method and
the same exclusions (285 files then, 293 now, `src/compileOnly` excluded, the
implicit `androidx.compose.runtime.getValue`/`setValue`/`provideDelegate` family
excluded, an import counted unused when its simple name never appears in the file
with the import lines removed):

**122 unused imports across 47 files.** Identical to `audit-020`. The eight test
classes `891d077` added carry none, and neither does the guard test added here.

| File | Unused imports |
|---|---|
| `app/src/main/java/com/example/modules/inky/InkyModule.kt` | 18 |
| `app/src/main/java/com/example/ui/components/OfficeUiComponents.kt` | 8 |
| `app/src/main/java/com/example/ui/home/HomeDashboard.kt` | 8 |
| `app/src/main/java/com/example/MainActivity.kt` | 6 |
| `app/src/main/java/com/example/modules/inky/HomeSubpages.kt` | 6 |
| `app/src/main/java/com/example/ui/components/ActionsToUndoSubpage.kt` | 6 |
| `app/src/main/java/com/makerandreas/papirusoffice/data/crash/CrashNotificationReceiver.kt` | 5 |
| `app/src/main/java/com/example/ui/home/AboutScreen.kt` | 4 |
| `app/src/main/java/com/makerandreas/papirusoffice/data/framework/PrintingFramework.kt` | 4 |
| `app/src/main/java/com/example/ui/components/UniversalChartSheet.kt` | 3 |
| `app/src/main/java/com/example/ui/components/UniversalClipboardSheet.kt` | 3 |
| `app/src/main/java/com/example/ui/components/UniversalXmlImportSheet.kt` | 3 |
| `app/src/main/java/com/makerandreas/papirusoffice/data/bridge/MyThesBridge.kt` | 3 |
| `app/src/main/java/com/makerandreas/papirusoffice/data/odf/SvXMLImport.kt` | 3 |
| the other 33 files | 45 between them |

Marked safely deletable. Deleting an unused import cannot change behaviour, so the
risk is scope rather than correctness, and a 47-file diff does not belong inside a
CI recovery. `audit-020` §9.2 remains the record; this section re-confirms the
count a day later on a tree that gained nine files.

Two further items `audit-020` recorded are unchanged and still not this pass's:
`lvlJc` and `lvlIsLgl` in `DocxNumbering.kt` are write-only locals that hide a
numbering-fidelity decision (`[MS-OI29500]` §17.9.1 p.123 on `isLgl`), and the
`"ppr"`/`"rpr"` lowercase-versus-`"pPr"`/`"rPr"` mismatch in the same file makes
every `w:ind`/`w:rFonts`/`w:sz`/`w:b`/`w:i`/`w:color` in `numbering.xml`
silently dropped. Both belong to a Plan 8C numbering pass.

---

## 10. Evidence

### 10.1 CI, three runs, base `main` at `323dc94`

Read from the `github-actions[bot]` comments on PR #37 and from
`gh api .../actions/runs/<id>/jobs`. The gate is the last run that was read before
this file was written.

| Run | Head | Unit & Roborazzi | Lint Analysis | Build & Package Debug APK | Tests |
|---|---|---|---|---|---|
| `37714464091` | `4972c3e` | success, step 5m16s | success, step 7m53s | success, step 5m52s | 434/0/0/0, 44.38s |
| `37716347743` | `83428ae` | success, step 5m06s | success, step 7m52s | success, step 5m54s | 434/0/0/0, 43.41s |
| `37717832692` | `b4395b1` | success, step 5m41s | success, step 7m37s | success, step 4m00s | 434/0/0/0, 45.05s |

**Gate, run `37717832692`: 434 run, 0 failed, 0 errors, 0 skipped, 45.05s across 82
suites.** All three runs read the same four counts across the same 82 suites, and
the wall time moves by 1.6s between the fastest and the slowest, which is the
spread to expect from runner variance rather than from anything this branch did.

Lint: `17 errors, 61 warnings across 17 issue ids` on the first run, then
`17 errors, 58 warnings across 15 issue ids` on both later runs. §6.3.

A note on the regress this creates. This document ships in the commit after its own
gate run, so the run that commit triggers cannot be named here without a further
commit naming that one. The convention adopted is that the audit names the last run
read before it was written, and the pull-request body, which is not part of the
tree, carries the live list. Run `37717832692` is docs-and-config only relative to
`83428ae`'s successor, and the body of PR #37 is where a later run is recorded.

Against the PR #36 baseline of 402 across 73 suites (run `37640269667`), that is
+32 tests and +9 suites, and the arithmetic closes exactly:

| Class | Tests | From |
|---|---|---|
| `CellinaSnapshotTest`, `InkySnapshotTest`, `PagellaSnapshotTest`, `SlidiaSnapshotTest` | 3 each, 12 | `891d077` |
| `DocumentCacheRepositoryTest` | 5 | `891d077` |
| `DocumentSessionManagerTest` | 3 | `891d077` |
| `InkyDocumentMetadataRepositoryTest` | 6 | `891d077` |
| `PapirusApiClientMockTest` | 4 | `891d077` |
| `TestDependencyGuardTest` | 2 | this pass |

The four MockK classes are the ones that could not compile: 18 tests, all green,
including `PapirusApiClientMockTest` 4/4 at 0.90s on the gate run.
`TestDependencyGuardTest` 2/2 at 0.03s.

Artifacts across the three runs, in run order: `apks` 262,589,474, 262,590,327 and
262,589,918 bytes; `unit-test-and-snapshot-reports` 794,754, 794,660 and 794,859;
`lint-reports` 63,563, 61,231 and 61,233. The APK varies by 853 bytes and the
snapshot artifact by 199 across three builds of near-identical trees, which is the
build-stamp and archive-order noise to expect. The lint artifact drops 2,332 bytes
between the first run and the second and holds, matching the three warnings the
`OldTargetApi` correction removed.

One workflow ran per push to this pull request, not two. That is the split,
observed rather than asserted.

### 10.2 Executed in the sandbox

| check | result |
|---|---|
| `TestDependencyGuardTest.kt` compiled with `kotlinc-jvm 2.4.20` on Temurin 25.0.2 | exit 0 |
| the same, run against the fixed `app/build.gradle.kts` from cwd `app/` | 2 PASS |
| the same, run against `git show HEAD:app/build.gradle.kts` | 1 FAIL naming `libs.mockk`, 1 PASS |
| all 62 `libs.*` accessors against `gradle/libs.versions.toml` | 0 missing |
| `scripts/lint-dump-comment.py` on a synthetic four-issue XML report | header, summary, errors, id table, warnings all correct |
| the same on the `.txt` fallback | correct, with `(no summary in the text report)` in the id table |
| the same on an empty report | `0 errors, 0 warnings`, "this run reported none" |
| the same with both reports missing | "No lint report was written", no invented findings |
| the same with and without `GITHUB_WORKSPACE` | runner-absolute, module-relative and repo-relative paths all normalize with it |
| `mockk` `API.kt` read through `gh api` | `any()` at `:802`, inside `MockKMatcherScope` opened at `:737` |
| `roborazzi` `RoborazziPlugin.kt`, `RoborazziProperties.kt`, `RoborazziTaskType.kt`, `Roborazzi.kt`, `SystemProperty.commonJvm.kt` read through `gh api` | §7.1 and §7.2 |
| both workflow files | `yaml.safe_load` parses; job and step inventory printed and read back |
| unused-import scan over 293 Kotlin files | 122 across 47, matching `audit-020` §9.2 |

---

## 11. Honest limits

* **No Android suite ran here.** No Gradle, no Android SDK, no Robolectric, no
  Compose, no MockK artifact in the sandbox. The 434-test green is CI's, from run
  `37714464091`, and the owner's Google AI Studio session is the tier above it.
* **`kotlinc-jvm` here is 2.4.20; the project is on Kotlin 2.2.10.** Adequate to
  type-check and run a two-dependency file against a stub; not a substitute for
  `:app:compileDebugUnitTestKotlin`.
* **The guard test ran against a two-symbol JUnit stub.** `org.junit.Assert` and
  `org.junit.Test` only. Its assertions are file reads and string matching, so the
  stub is faithful for what it does, but it is not JUnit.
* **`lint-dump-comment.py` was executed against synthetic input**, not a real AGP
  9.1.1 report. Its first real execution is the lint comment on PR #37, which is
  reproduced in §6.2, so that limit is closed for the XML path and still open for
  the `.txt` fallback.
* **Why lint failed at `891d077` is not established.** §6.4 gives the inference and
  names it as one.
* **The screenshots were not seen.** §7.2 infers they were written from a ~570 KB
  artifact delta that three runs reproduce within 199 bytes. The artifacts
  themselves are on storage this sandbox cannot open, so the PNGs are inferred, not
  inspected.
* **Nothing compares screenshots to anything.** Record mode with no goldens and no
  verify task is not a visual regression gate (§7.3).
* **`isReturnDefaultValues` and the Byte Buddy `jvmArgs` were restored on the
  strength of the `891d077` configuration, not on a measured failure without
  them.** Run `37714464091` confirms the restored set is green; it does not
  isolate which of the three lines was load-bearing. Only `libs.mockk` is provably
  load-bearing, from the compiler diagnostics.
* **The nightly cadence change is a behaviour change the owner may want back.**
  §5.3 states the trade and how to reverse it.

---

## 12. Files changed by this session

| File | Change |
|---|---|
| `app/build.gradle.kts` | `testImplementation(libs.mockk)` restored; `unitTests { isReturnDefaultValues = true }` restored; Byte Buddy agent `jvmArgs` restored; `"OldTargetSdkVersion"` corrected to `"OldTargetApi"`; three comments naming what each restored line is for |
| `gradle.properties` | `roborazzi.test.record=true` removed |
| `app/src/test/java/com/example/TestDependencyGuardTest.kt` | new, 2 cases |
| `scripts/lint-dump-comment.py` | new, mirrors the lint report into a pull-request comment |
| `.github/workflows/ci.yml` | verification only; lint report step added; `upload-artifact` v7; dead release branch and secret exports removed |
| `.github/workflows/build.yml` | distribution only; test and lint jobs removed; tagged-commit verification step added; `upload-artifact` v7 nowhere needed since it uploads no artifact |
| `AGENTS.md` | the workflow split, the lint report read path, the MockK regression and the guard |
| `PROJECT_CONTEXT.md` | the CI reference at `:348` points at `ci.yml` |
| `anti-slop/audit-021-2026-10-08-ci-regression-workflow-split-and-lint-report.md` | this file |
| `anti-slop/plan-01-master-index.md` | item 14 |

---

## 13. What is owed next

1. **A lint pass against §6.2.** Seventeen errors, starting with the Fatal
   `InvalidFragmentVersionForActivityResult` at `MainActivity.kt:64` and the two
   `StaticFieldLeak` warnings, then the fifteen `StringFormatMatches` placeholder
   types, which are one mechanical sweep over eleven `strings.xml` entries and
   their call sites. Only after that is `abortOnError = true` worth restoring.
2. **A decision on screenshot verification.** Commit goldens and move CI to
   `verifyRoborazziDebug`, or accept the snapshot classes as composition smoke
   tests and say so wherever they are described. §7.3.
3. **The unused-import sweep.** 122 across 47 files, unchanged from `audit-020`
   §9.2, still its own PR.
4. **`ubuntu-latest` migrates to Ubuntu 26 on 2026-10-19.** Eleven days out. Either
   pin `ubuntu-24.04` before then or let it move and read the CI report.
5. **Plan 9.** `plan-01-master-index.md` forecasts it at PR #36; PR #36 was the CI
   triage pass and this is #37, so Plan 9 is the slot after this one. The forecast
   shift is recorded here and in item 14 of the master index rather than edited
   into the dated roadmap, following the `audit-020` §11 precedent.
6. **The five PR #35 commits `audit-020` §5 flagged for review.** `74b6327`,
   `a67879e`, `81fbd1f`, `1a1b8b6` and `aec1672` changed parser behaviour and
   relaxed 131 lines of assertions while the test sources had never compiled. PR
   #36 recorded them as owed and did not review them. Run `37714464091` is the
   first green run that includes them, so the review is now possible and still not
   done.
