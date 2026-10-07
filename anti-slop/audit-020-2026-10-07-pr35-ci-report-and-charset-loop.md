# Audit 020 (2026-10-07): PR #35, the CI report, and the twelve-run `Charsets` loop

**Baseline:** PR #35 head `6c30c38` ("Plan 8B: add missing LayoutUnits import in
DocxNumbering.kt"), branch `arena/5dab2315-papirus-office`, base `main` at `37302a9`.
The local clone was unshallowed for this session (412 commits), so the twelve runs of
PR #35 and the base version of `OfficeDocumentParser.kt` are both inspectable here.
**Scope:** (1) read every CI report on PR #35, (2) establish what actually failed and
why it took twelve runs, (3) fix the report so the next agent can read it, (4) record
what the sandbox can and cannot reach, (5) an unused-code sweep over the tree.
**Method:** `gh api` for the pull-request comments, runs, jobs, check-runs and
annotations; `gh run list --branch`; `git log`/`git show`/`git diff` on the local
history; source reading; `python3` execution of `scripts/ci-dump-comment.py` and
`scripts/native-inventory.sh` against synthetic inputs. No JDK exists in this sandbox
(AGENTS.md), so no compile or test result in this document came from here.

---

## 0. Findings in one screen

| # | Finding | Evidence | Consequence |
|---|---|---|---|
| A | **The CI report is readable from the sandbox, and always was.** Twelve reports sit on PR #35 as `github-actions[bot]` comments and `gh api repos/makerandreas/Papirus-Office/issues/35/comments` returns all of them in full. | §2 | The claim that the report cannot be read is wrong. `AGENTS.md` now documents the read path instead of naming it once and leaving the method undefined. |
| B | **The SHA in every report header was unresolvable.** `scripts/ci-dump-comment.py` printed `GITHUB_SHA`, which on a `pull_request` event is GitHub's ephemeral `refs/pull/35/merge` commit. None of the twelve header SHAs exist on any branch. | §3 | The report could not be tied to the code it described, so twelve runs read as twelve attempts at commits that did not exist. Fixed in the script and the workflow. |
| C | **One bad import line caused eight of the twelve red runs.** `import java.nio.charset.Charsets` is not a type that exists. It was in four files from the first run. | §4.1 | The Java classes are `Charset` and `StandardCharsets`; `Charsets.UTF_8` is `kotlin.text.Charsets`, a Kotlin default import. Deleted from all four files. |
| D | **The same line was fixed at `ef2945b` and re-added at `764a598`.** The re-adding commit is titled "restore Charsets import". | §4.2 | Four extra red runs. `DocxNumbering.kt` now carries a comment saying why the import must not come back. |
| E | **`6c30c38` fixed an error no report ever showed.** It added `import com.makerandreas.papirusoffice.data.LayoutUnits` to a file already in `package com.makerandreas.papirusoffice.data`. | §4.3 | Same-package declarations need no import. Removed. |
| F | **Six commits changed parser behaviour and test assertions while the test sources had never compiled.** `81fbd1f`, `1a1b8b6` and `aec1672` rewrote 131 lines of assertions across five test files. | §5 | Those relaxations were written against failures nobody had observed. They need review against a green run before PR #35 merges. |
| G | **One real behavioural failure was ever measured**, at run `37620042554`: 383 tests, 1 failed, `DocxRunFormattingTest.textTheFileWroteOutsideARunBecomesANeutralRun`. Plan 8B deleted the placeholder bullet that test asserted. | §6 | The test now writes its own out-of-run text, and a second case states the new rule for a dangling `w:numId`. |
| H | **A real type-check is possible in the sandbox after all.** A Temurin 25 JRE from PyPI plus `npm pack kotlin-compiler@2.4.20` gives a working `kotlinc`. | §7 | `DocxNumbering.kt` and the new assertions were compiled here, with negative controls that reproduce the CI diagnostics. `AGENTS.md` records the route. |
| I | **122 imports across 47 files are unused**, plus four write-only locals in the new `DocxNumbering.kt`. | §9 | Recorded as safely deletable; only the one inside a file this PR adds was removed here. |
| J | **Every new Plan 8B test resolved its fixture from the wrong directory.** All eight call sites used `File("tests/inky/Sample-N.docx")`, but Gradle runs unit tests with the working directory at the `app` module. | §12.1 | Seven of eleven failures in run `37634131669`. All eight now go through `SampleMatrix.findTestFile`, the resolver the rest of the suite already used. |
| K | **A real Word table of contents was invisible to index detection.** Every TOC entry carries `w:tabs` for its dot-leader page-number stop, which flips `hasDirectPPr` and replaces the entry's style with a synthetic `inline-p-N`. | §12.2 | Three failures. `collectDocxAuthoredIndexes` now follows `parentStyleName` back to the authored style. |
| L | **`DocxNumberingReaderTest` had no Robolectric runner, and three of its four cases could not tell.** `XmlPullParserFactory.newInstance()` throws without the Android runtime, the reader's `catch` returned an empty result, and three cases assert on emptiness. | §12.3 | The one failure that remained after J and K. `catch (_: Exception)` made an empty result and a crash indistinguishable; `NumberingParseResult.parseError` now keeps them apart. |

---

## 1. What the sandbox can reach, re-verified on 2026-10-07

Every line below was executed in this session against PR #35.

| Call | Result |
|---|---|
| `gh auth status` | `Logged in to github.com as makerandreas (GH_TOKEN)` |
| `gh api repos/makerandreas/Papirus-Office/issues/35/comments` | 12 comments, 11,545 to 47,609 characters each, all bodies complete |
| `gh run list --branch arena/5dab2315-papirus-office` | 12 runs, each with `headSha`, `status`, `conclusion`, `createdAt` |
| `gh api .../actions/runs/37623177005/jobs` | both jobs, every step name and conclusion |
| `gh api .../check-runs/<id>/annotations` | readable, but the failing step carries only `Process completed with exit code 1.` |
| `gh run download 37623177005 --name unit-test-reports` | `EOF` from `productionresultssa14.blob.core.windows.net` |
| `gh run view 37623177005 --log-failed` | `EOF` from `results-receiver.actions.githubusercontent.com` |
| `java -version` | `command not found` |

So: the metadata API and the pull-request comments are open; the blob storage behind
logs and artifacts is closed. The comment is the only place the compiler output and
the JUnit XML survive, which is exactly what `scripts/ci-dump-comment.py` was written
to do (`.github/workflows/build.yml`, the `Post CI report to the pull request` step).

The gap was never access. It was that `AGENTS.md` said only "use the GitHub API
Approach instead" and never said what to call, what comes back, or which SHA in the
result belongs to the code.

---

## 2. The twelve runs, correctly attributed

`gh run list` gives the head SHA; the comment header gives the merge-ref SHA. The two
were never the same.

| # | Header said | Real head | Run | Failed task | Distinct `e:` lines |
|---|---|---|---|---|---|
| 1 | `de0be08` | `4c723a5` | 37587927550 | `:app:compileDebugKotlin` | 25 |
| 2 | `14f7309` | `de3782f` | 37589209663 | `:app:compileDebugKotlin` | 20 |
| 3 | `ada85ec` | `ef2945b` | 37590768262 | `:app:compileDebugUnitTestKotlin` | 97 |
| 4 | `468ce38` | `9ba827b` | 37592244741 | `:app:compileDebugUnitTestKotlin` | 3 |
| 5 | `18dd0ea` | `74b6327` | 37613976972 | `:app:compileDebugUnitTestKotlin` | 3 |
| 6 | `34e1064` | `a67879e` | 37615153880 | `:app:compileDebugUnitTestKotlin` | 3 |
| 7 | `29c3643` | `81fbd1f` | 37616229644 | `:app:compileDebugUnitTestKotlin` | 3 |
| 8 | `1056680` | `1a1b8b6` | 37617211560 | `:app:compileDebugUnitTestKotlin` | 3 |
| 9 | `6682e07` | `aec1672` | 37618974969 | `:app:compileDebugUnitTestKotlin` | 4 |
| 10 | `50e3158` | `60fb9a5` | 37620042554 | `:app:testDebugUnitTest` | 0, one failing test |
| 11 | `d53cec5` | `764a598` | 37621966041 | `:app:compileDebugKotlin` | 1 |
| 12 | `4bd5734` | `6c30c38` | 37623177005 | `:app:compileDebugKotlin` | 1 |

The header SHAs are merge commits. `gh api repos/makerandreas/Papirus-Office/commits/4bd5734`
returns `"Merge 6c30c38de52960990ab2b19cee72179b3ebada44 into 37302a93cbccfa2ee6069e57a916fb33c5e0dad8"`
with two parents. `git show de0be08` in a normal clone resolves to nothing, because
`refs/pull/35/merge` is not fetched by `git clone`.

Twelve of twelve headers were unusable as a checkout target. That single defect is
what made the run history look like a mystery.

---

## 3. The report fix

`scripts/ci-dump-comment.py` took its SHA from `GITHUB_SHA`. On a `pull_request`
event `GITHUB_SHA` is the merge commit GitHub builds for `refs/pull/N/merge`, which
is on no branch.

Changed:

* `.github/workflows/build.yml` passes `HEAD_SHA`, `HEAD_REF`, `BASE_SHA` and
  `BASE_REF` (from `github.event.pull_request.*`) to the report step, and `HEAD_SHA`
  to the native-inventory step.
* `scripts/ci-dump-comment.py` names `HEAD_SHA` in the header, falls back to
  `GITHUB_SHA`, and adds a provenance line: `Head 6c30c38 on arena/5dab2315-papirus-office, merged onto main at 37302a9 for this run.`
* Compiler paths are shortened from `file:///home/runner/work/Papirus-Office/Papirus-Office/app/src/...`
  to `app/src/...`, which is the form that can be opened or grepped in a checkout at
  any root.
* Repeated `e:` lines are collapsed and the count reports both numbers, so a compiler
  that emits the same diagnostic twice no longer pushes real errors off the list.
* `scripts/native-inventory.sh` prefers `HEAD_SHA` and names the merge commit it was
  actually run against when the two differ.

Executed here, both paths:

```text
$ HEAD_SHA=6c30c38... BASE_SHA=37302a9... BASE_REF=main HEAD_REF=arena/5dab2315-papirus-office \
  python3 scripts/ci-dump-comment.py /tmp/citest /tmp/citest/gradle.log /tmp/citest/inventory.txt
### CI report for `6c30c38` ([run 37623177005](https://github.com/makerandreas/Papirus-Office/actions/runs/37623177005))
Head `6c30c38` on `arena/5dab2315-papirus-office`, merged onto `main` at `37302a9` for this run.
No test results were written; the Gradle log says why below.

<details><summary>Kotlin compile errors (2 distinct, 3 reported)</summary>
e: app/src/main/java/com/makerandreas/papirusoffice/data/DocxNumbering.kt:10:25 Unresolved reference 'Charsets'.
e: app/src/test/java/com/example/DocxTableTocTest.kt:85:9 Unresolved reference 'assertFalse'.
```

and with `HEAD_SHA` unset or empty (the `push` case), the header falls back to
`GITHUB_SHA` and the provenance line is omitted. `bash -n scripts/native-inventory.sh`
passes and the header prints `commit 6c30c38` with `HEAD_SHA` set.

`AGENTS.md` gained a section, "Reading the CI report (the GitHub API approach, in
full)", with the reachability table above, the three-command recipe, the merge-ref
warning and the section-by-section meaning of the comment.

---

## 4. What actually broke the build

### 4.1 `import java.nio.charset.Charsets` is not a type

Four files carried it: `data/DocxNumbering.kt`, `DocxFieldHyperlinkTest.kt`,
`DocxSectionsTest.kt`, `DocxTableTocTest.kt`. The JDK package holds `Charset`
(singular) and `StandardCharsets`. `Charsets.UTF_8` in Kotlin is
`kotlin.text.Charsets`, which `kotlin.text.*` puts in scope by default on JVM
targets, so no import is needed and no import can name it.

The repository already proves the point: 17 other files call `Charsets.UTF_8` with no
charset import at all, including `OfficeDocumentParser.kt`, `DocxDocumentParser.kt`,
`OdtImportPipeline.kt` and `DocxRunFormattingTest.kt`, and they compile.

All four imports are deleted. `DocxNumbering.kt` carries a comment naming the mistake
so it is not "restored" a third time.

Runs 1, 2, 4, 5, 6, 7, 8, 9, 11 and 12 all report this line. Eight of those failed
only on it.

### 4.2 It was already fixed once

`git show <c>:...DocxNumbering.kt | grep '^import'` across the branch:

| Commit | `java.nio.charset.Charsets` present |
|---|---|
| `4c723a5`, `de3782f` | yes (line 8) |
| `ef2945b` through `914f5e1` (nine commits) | **no** |
| `764a598` "add DOCX numFmt name mapping and restore Charsets import" | yes again (line 9) |
| `6c30c38` | yes (line 10, pushed down by the new `LayoutUnits` import) |

`ef2945b` ("Plan 8B fix: resolve Kotlin compile errors in DocxNumbering and
OfficeDocumentParser") removed it along with the four other `DocxNumbering.kt`
errors. Nine commits later `764a598` put it back, describing the change as making the
module "compile against `readCappedBytes().toString(Charsets.UTF_8)`". That call never
needed the import. Cost: runs 11 and 12, plus the confusion in between.

### 4.3 `6c30c38` fixed an error that was never reported

`DocxNumbering.kt` declares `package com.makerandreas.papirusoffice.data`, and
`LayoutUnits` is `object LayoutUnits` in `data/LayoutUnits.kt`, same package. Nothing
to import. Run 11 reported exactly one diagnostic, `DocxNumbering.kt:9:25 Unresolved
reference 'Charsets'`; the Kotlin frontend lists every error it finds, and run 1 had
listed six in the same file, so an unresolved `LayoutUnits` would have appeared. The
import is removed.

### 4.4 The one remaining test-source error

`DocxTableTocTest.kt:85` calls `assertFalse(...)`; the file imported `assertEquals`,
`assertNotNull` and `assertTrue` only. Reported at run 9, still present at head. The
import is added.

After these five line-level edits the state is: main source differs from `60fb9a5`,
the last commit whose `:app:compileDebugKotlin` succeeded, by nothing at all, and the
test sources differ from `aec1672` by the removed imports and one added import.

---

## 5. The cost of not reading the report

Between run 4 and run 9 the three `Charsets` diagnostics were identical. The commits
in between were not about imports:

| Commit | Change | Test run that could have validated it |
|---|---|---|
| `74b6327` | `OfficeDocumentParser.kt`, 5 lines deleted (duplicate `END_TAG` arms) | none |
| `a67879e` | `OfficeDocumentParser.kt`, `inTrPr = false` on `<w:tr>` | none |
| `81fbd1f` | 25 lines across two test files, "align test expectations with real fixture numbering" | none |
| `1a1b8b6` | 11 lines across two test files, "relax section restart assertion" | none |
| `aec1672` | 131 lines across five test files, "relax brittle assertions" | none |

None of those five test files compiled once between run 3 and run 10. The relaxations
were written against failures that had not been observed, because the suite never ran.
That is the specific harm the `AGENTS.md` section is written to prevent, and it is why
rule 2 there requires the report's SHA and `git rev-parse HEAD` to agree before a
failure is attributed to the current tree.

Those five commits are not reverted here. Reverting them blind would repeat the
mistake in the other direction. They are listed for review against the first green
run of this PR.

---

## 6. The one measured failure, and its fix

Run `37620042554` (head `60fb9a5`, which had temporarily deleted the five new test
files) is the only run on this PR where tests executed: **383 run, 1 failed, 0 errors,
0 skipped, 37.14 s across 68 suites**.

```text
DocxRunFormattingTest.textTheFileWroteOutsideARunBecomesANeutralRun
org.junit.ComparisonFailure: the placeholder bullet is not authored run text
expected:<[• ]> but was:<[item]>
```

Cause, from the base tree. `main` at `37302a9` held this in `OfficeDocumentParser.kt`:

```kotlin
// Lists. The bullet text is the placeholder that Plan 8B
// replaces with the numbering definition's real label.
tagLocal == "list-item" || tagLocal == "numpr" -> {
    if (tagLocal == "numpr") inNumPr = true
    currentText.append("• ")
}
```

PR #35 deletes the `currentText.append("• ")` and replaces it with
`applyDocxNumberingLabels` (`OfficeDocumentParser.kt:330-405`), which renders the real
label from `word/numbering.xml` through `DocxNumberingReader` and `NumberingCounterState`,
and prepends it to the first run. When `numSpecs[numId]` is null the paragraph is left
alone. That is the intended Plan 8B behaviour, and the comment the diff leaves behind
says so: "never prepends a fake bullet here".

The failing case is an 8A test whose subject is the run-tiling rule, not bullets. It
manufactured out-of-run text by way of the placeholder. With the placeholder gone its
premise disappeared, so it now asserts a rule the branch deliberately removed.

Fixed by keeping the subject and changing the stimulus:

* `textTheFileWroteOutsideARunBecomesANeutralRun` writes `<w:t>loose </w:t>` directly
  inside `<w:p>` and asserts that the loose text tiles into a neutral run ahead of the
  authored `item` run. The parser appends any `w:t` text to the paragraph buffer
  (`OfficeDocumentParser.kt:2378`) and closes the run at `</w:r>` (`:2414-2425`),
  so the tiling branch at `:2485-2500` is the code this case reaches.
* A new case, `aNumberedParagraphGetsNoLabelWhenThePackageHasNoNumberingPart`, states
  the new contract: a package with `styles.xml` and `document.xml` only leaves a
  dangling `w:numId 7` unlabelled, keeps one authored run, and still carries
  `paragraph.numbering` with `numId == 7` and `suppressed == false` for the Navigator.

`assertNotNull` was added to the imports of `DocxRunFormattingTest.kt` for the new
case.

---

## 7. What was executed here

`AGENTS.md` records that no JDK is obtainable in the sandbox, and audit-016 §8 is
right that `./gradlew testDebugUnitTest` cannot run: Gradle, AGP, the Android SDK and
Google Maven are all unreachable. That does not mean nothing can be compiled. Two
hosts in the allowlist were enough to build a real type-check:

* `pip install --target /tmp/jdkpkg jdk4py` gives a Temurin **25.0.2 JRE**. It has
  `java.compiler` but not `jdk.compiler`, so `javac` and the single-file source
  launcher both fail (`Module jdk.compiler not in boot Layer`). A JRE is all `kotlinc`
  needs.
* `npm pack kotlin-compiler@2.4.20` gives the **full Kotlin 2.4.20 compiler** (the
  GitHub release asset redirects to a CDN outside the allowlist; the npm mirror of the
  same distribution does not).

With those, four checks were run. Each has a negative control that reproduces the
matching CI diagnostic, so the checks are shown to be sensitive rather than vacuous.

| Check | Files | Result |
|---|---|---|
| `DocxNumbering.kt` as this session leaves it, against the real `Numbering.kt`, `LayoutUnits.kt` and `ZipSafe.kt` plus a signature-faithful `org.xmlpull.v1` stub | `kotlinc-jvm` | **exit 0**, `DocxNumberingReader.class` and 10 more produced |
| The same file with `import java.nio.charset.Charsets` put back | `kotlinc-jvm` | **exit 1**, `DocxNumberingBad.kt:7:25: error: unresolved reference 'Charsets'.` |
| The exact assertion expressions added to `DocxRunFormattingTest`, against a signature-faithful `org.junit.Assert` | `kotlinc-jvm` | **exit 0** |
| The same file with `import org.junit.Assert.assertFalse` removed | `kotlinc-jvm` | **exit 1**, `error: unresolved reference 'assertFalse'.` |

The column number in the negative control is the same one CI reported
(`DocxNumbering.kt:10:25 Unresolved reference 'Charsets'.`), which is what ties the
reproduction to the failure rather than to a similar-looking one.

Two further confirmations came from the JRE and the JDK itself rather than from
Kotlin:

* `strings lib/modules` over the Temurin 25 image: `nio/charset/StandardCharsets` 157
  hits, `nio/charset/Charset` 1725 hits, **`nio/charset/Charsets` 0 hits**. The type
  the four files imported does not exist in the JDK.
* The green `DocxNumbering.kt` compile had no `import ...LayoutUnits` and still
  resolved `LayoutUnits.twipsToUnits` and `LayoutUnits.halfPointsToPt`, because both
  are in `package com.makerandreas.papirusoffice.data`. That is the disproof of
  `6c30c38`.

`scripts/ci-dump-comment.py` was executed on three inputs: the synthetic Gradle log of
§3 (compile-failure path, both with and without `HEAD_SHA`), and a synthetic JUnit XML
suite with one failure (test-results path, which renders the counts, the failing-test
block and the per-class table). `bash -n scripts/native-inventory.sh` passes and the
script was run with and without `HEAD_SHA`.

Two more checks:

* **Syntax.** Each of the five edited test files was put through `kotlinc-jvm` without
  its classpath. `DocxRunFormattingTest.kt` produces 295 diagnostics, every one an
  `unresolved reference` to `android`, `androidx`, `org.junit` or `org.robolectric`,
  and none of the five produces a `Syntax error` / `Expecting` diagnostic. That is the
  check that matters here: the attempt-2 CI failure was
  `OfficeDocumentParser.kt:442:6 Syntax error: Expecting member declaration.`, and this
  rules that class of defect out of the edits.
* **Workflow.** `yaml.safe_load` on `.github/workflows/build.yml` parses, and the step
  inventory comes back as `Native inventory (read-only) env=['HEAD_SHA']` and
  `Post CI report to the pull request env=['GH_TOKEN', 'PR_NUMBER', 'HEAD_SHA', 'HEAD_REF', 'BASE_SHA', 'BASE_REF']`.

What was **not** executed: the Android unit-test suite. `OfficeDocumentParser.kt`,
`DocxRunFormattingTest.kt` and the other four test files need the Android SDK,
Compose, JUnit, Robolectric and the Gradle plugin, none of which are reachable. The
behavioural claim in §6 (that the rewritten case reaches the tiling branch and the new
case sees a null `numSpecs[7]`) is derived from reading
`OfficeDocumentParser.kt:2378`, `:2414-2425`, `:2485-2500` and `:1762`, not from a run.
The next CI run on this branch is the evidence for it.

The owner has accepted that gap and named what covers it: Google AI Studio runs the
real `testDebugUnitTest` suite and has an Android cloud device emulator for UI checks,
with the Realme C3 device pass after Plan 11. `AGENTS.md` now records that as a
four-tier evidence ladder, so a claim is labelled with the tier it came from instead of
being presented as verified when it was only read.

---

## 8. Delivery Gate

Code and CI-tooling deliverable, so the full gate applies to the changed files.

* **R-02 PASS:** no em dash in `AGENTS.md`, this audit, or the four changed source
  comments; verified by code-point search over each diff line.
* **R-15 PASS:** no call-to-action copy is produced.
* **R-16 PASS:** no marketing language; every sentence names a file, a line, a commit
  or a run.
* **R-17 PASS:** every number carries its source: `gh api` for comment and run
  identifiers and the 383/1/68 test counts, `git show`/`git diff` for the import
  history, `grep` for the 17 files that use `Charsets.UTF_8` with no import, and the
  `python3` scan for the 122 unused imports.
* **R-26, R-27, R-32, R-34, R-35 N/A:** no UI shipped in this deliverable.
* **R-36 PASS:** the only compile results claimed are the four `kotlinc-jvm` runs in §7,
  each with a negative control, and the two JVM/jimage confirmations. The Android suite
  was not run and §7 says so; the behavioural claim in §6 is labelled as derived from
  source reading, and §4.4's compile expectation is labelled as a derivation from the
  last green compile at `60fb9a5`, not as an executed result.
* **R-38 PASS:** nothing fabricated. The synthetic Gradle log used to exercise
  `scripts/ci-dump-comment.py` is labelled as synthetic in §3.
* **C-5 PASS:** the claim this audit exists to overturn ("the CI report cannot be
  read") is overturned with the command that read it and the byte counts that came
  back.

---

## 9. Unused code, marked as safely deletable

### 9.1 Removed in this session

| Location | What | Evidence |
|---|---|---|
| `app/src/test/java/com/example/DocxNumberingReaderTest.kt:5` | `import ...NumberingSpec` | one occurrence in the file, the import itself; the file is new in this PR |

### 9.2 Safely deletable, recorded for a dedicated sweep (not touched here)

A scan of all 285 Kotlin files under `app/src` (excluding `src/compileOnly`) for
imports whose simple name never appears in the file body, with the implicit
`androidx.compose.runtime.getValue`/`setValue`/`provideDelegate` family excluded,
finds **122 unused imports across 47 files** on the tree as this session leaves it.
The same scan before the §9.1 deletion found 123 across 48. Deleting an unused import
cannot change behaviour; the risk is scope, not correctness, so they belong in their
own PR rather than in Plan 8B. The largest clusters:

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
| the other 38 files | 57 between them |

`app/src/main/java/com/example/ui/options/PapirusOfficeOptionsScreen.kt:32`
(`com.example.core.ai.GeminiAiService`) is in that set and is also touched by PR #35,
but it is unused on `main` too (`git show 37302a9:...` has the same single occurrence),
so it is pre-existing and stays out of this diff.

### 9.3 Write-only state in the new `DocxNumbering.kt`

Four locals are assigned and never read. They are warnings, not errors, and the build
has no `allWarningsAsErrors`, so they do not affect this PR. Two of them hide a
behaviour gap and should not be deleted without a decision:

| Local | Occurrences | Note |
|---|---|---|
| `lvlJc` | 3 (declare, `w:lvlJc` handler, reset) | the label alignment is parsed then dropped; the level `pPr` note in `[MS-OI29500]` §17.9.22 p.125 allows `jc` |
| `lvlIsLgl` | 3 (declare, `w:islegal` handler, reset) | **behaviour gap:** `[MS-OI29500]` §17.9.1 p.123 says `isLgl` affects all levels including the immediate one. The reader parses it and `NumberingLevelSpec` has no field for it, so a legal-numbering list renders with its own `numFmt` instead of decimal |
| `abstractLevels` | 2 (declare, write) | duplicates `abstractSpecs[id].levels`; dead |
| `abstractNames` | 2 (declare, write) | the display name already reaches `NumberingSpec.displayName`; dead |

`abstractLevels` and `abstractNames` are safe to delete. `lvlJc` and `lvlIsLgl` belong
to a Plan 8C decision about label alignment and legal numbering, not to a cleanup.

---

## 10. Files changed by this session

| File | Change |
|---|---|
| `app/src/main/java/com/makerandreas/papirusoffice/data/DocxNumbering.kt` | Removed `import java.nio.charset.Charsets` and the redundant same-package `import ...LayoutUnits`; added the comment that explains why neither comes back |
| `app/src/test/java/com/example/DocxFieldHyperlinkTest.kt` | Removed `import java.nio.charset.Charsets` |
| `app/src/test/java/com/example/DocxSectionsTest.kt` | Removed `import java.nio.charset.Charsets` |
| `app/src/test/java/com/example/DocxTableTocTest.kt` | Removed `import java.nio.charset.Charsets`; added `import org.junit.Assert.assertFalse` |
| `app/src/test/java/com/example/DocxNumberingReaderTest.kt` | Removed the unused `import ...NumberingSpec` |
| `app/src/test/java/com/example/DocxRunFormattingTest.kt` | Rewrote `textTheFileWroteOutsideARunBecomesANeutralRun` onto its own out-of-run text; added `aNumberedParagraphGetsNoLabelWhenThePackageHasNoNumberingPart`; added `assertNotNull` import |
| `.github/workflows/build.yml` | Passes `HEAD_SHA`, `HEAD_REF`, `BASE_SHA`, `BASE_REF` to the CI report step and `HEAD_SHA` to the native inventory step |
| `scripts/ci-dump-comment.py` | Header names the PR head SHA plus a provenance line; repo-relative compiler paths; duplicate `e:` lines collapsed with both counts; docstring records the environment |
| `scripts/native-inventory.sh` | Header prefers `HEAD_SHA` and names the merge commit when it differs |
| `AGENTS.md` | New section "Reading the CI report (the GitHub API approach, in full)"; the JNI notice now points at it |
| `anti-slop/audit-020-2026-10-07-pr35-ci-report-and-charset-loop.md` | This file |

Added while closing out PR #36's four runs (§12):

| File | Change |
|---|---|
| `app/src/main/java/com/makerandreas/papirusoffice/data/OfficeDocumentParser.kt` | New `docxStyleName` helper at `:514`; `collectDocxAuthoredIndexes` resolves a paragraph's style through `parentStyleName` instead of reading the synthetic name (§12.2) |
| `app/src/main/java/com/makerandreas/papirusoffice/data/DocxNumbering.kt` | `NumberingParseResult.parseError`; `read()` names the throwable instead of swallowing it (§12.3) |
| `app/src/test/java/com/example/DocxNumberingReaderTest.kt` | `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [34])`; `fixture()` delegates to `SampleMatrix.findTestFile`; `numSuffix` expectation corrected to `""`; the numId 15 message carries `parseError` (§12.1, §12.3, §12.4) |
| `app/src/test/java/com/example/DocxAuthoredShapeTest.kt` | Fixture resolution through `SampleMatrix.findTestFile`; the then-unused `import java.io.File` removed |
| `app/src/test/java/com/example/DocxFieldHyperlinkTest.kt`, `DocxSectionsTest.kt`, `DocxTableTocTest.kt` | Fixture resolution through `SampleMatrix.findTestFile` |
| `anti-slop/audit-016-2026-10-04-post-7d-unit-test-analysis.md`, `audit-017-2026-10-04-plan-7e-recovery-and-7f-shape.md` | Both said the PR comment was the only durable log and left the impression a past PR was a dead end. A merged PR's report is still retrievable through the API; verified on PR #32 on 2026-10-07, two days after its merge. What is unrecoverable is narrower: the raw step log and the `unit-test-reports` artifact |

---

## 11. Delivery decision and the PR-number deviation

The owner's decision, 2026-10-07: this pass opens as its **own pull request** rather
than being pushed onto `arena/5dab2315-papirus-office`, and the owner closes **PR #35**
himself after review and merge.

That consumes a PR slot the forecast had assigned to something else, so the ledger
deviates in a known way:

| Document | What it says | What is now true |
|---|---|---|
| `plan-01-master-index.md` §2 row 8 | "Forecast **PR #35 (8A)** and **PR #36 (8B)**" | 8A already landed inside PR #34 (recorded in item 12). 8B is PR #35. This CI-triage pass takes the next slot. |
| `plan-01-master-index.md` item 12 | "8B remains forecast at `#36`" | 8B opened as #35, not #36. This pass is the new slot after it. |
| `plan-2026-09-24-remaining-pr-roadmap-v2.md` §4.8, §4.9 | the v2.13 slot table `#35 8A, #36 8B, #37 Plan 9` | shifted by one for everything from Plan 9 onward |

Plan IDs stay authoritative; PR numbers remain forecasts, not reservations. The rows
above are recorded here rather than rewritten in place, because the roadmap and the
index are dated documents and the deviation is this session's, not theirs. The next
pass that touches the forecast should renumber from this point.

This PR is deliberately not a Plan. It is CI hygiene plus five line-level compile fixes
plus one test correction. It carries no feature scope, so it should not be read as
advancing Plan 8B: the numbering, field, TOC, table and section work in PR #35 is
unchanged by it, and PR #35's own acceptance is unchanged.

---

## 12. PR #36: four runs, three root causes

The CI-triage pass above landed on PR #36 (head `arena/0c0a43a2-papirus-office`,
base `main` at `37302a9`). Four runs closed it out. Every number below is read from the
`github-actions[bot]` comment on that PR, not from a local run, because Gradle cannot
run here.

| Run | Head | Result | Failing |
|---|---|---|---|
| `37634131669` | `b1a24f7` | 402 run, **11 failed** | compile fixes held; 11 behavioural/path failures appeared for the first time |
| `37635562268` | `dfb7b0d` | 402 run, **4 failed** | fixture resolution fixed (§12.1) removed 7 |
| `37638803729` | `03cc8ee` | 402 run, **1 failed** | TOC style resolution fixed (§12.2) removed 3 |
| `37640269667` | `ecfa42f` | **402 run, 0 failed, 0 errors, 0 skipped, 35.33 s, 73 suites** | Robolectric runner added (§12.3) |

All seven Plan 8B and 8A suites are green in the last run: `DocxAuthoredShapeTest` 3/3,
`DocxFieldHyperlinkTest` 5/5, `DocxNumberingReaderTest` 4/4, `DocxRunFormattingTest`
11/11, `DocxSectionsTest` 2/2, `DocxStyleChainTest` 11/11, `DocxTableTocTest` 4/4.

None of the three causes was a wrong expectation in a new test, which is what §4 found
for PR #35. Two were the new code not meeting a contract the fixtures already stated,
and one was a test harness that could not fail.

### 12.1 Fixture resolution

`Gradle` runs `:app:testDebugUnitTest` with the working directory at `app/`, so
`File("tests/inky/Sample-6.docx")` resolved to `app/tests/inky/Sample-6.docx`, which
does not exist. The repository already had the answer: `SampleMatrix.findTestFile`
(`app/src/test/java/com/example/SampleMatrix.kt:92-100`) tries `""`, `"../"` and
`"../../"` and returns the first candidate that exists with a non-zero length. In CI it
returns `../tests/inky/<name>`.

All eight call sites now delegate to it: `DocxAuthoredShapeTest:71`,
`DocxFieldHyperlinkTest:118`, `DocxSectionsTest:29`, `DocxTableTocTest:32,69,81`, and
`DocxNumberingReaderTest.fixture()`. `java.io.File` became unused in
`DocxAuthoredShapeTest.kt` as a result and was removed.

### 12.2 A table of contents that resolved to nothing

`collectDocxAuthoredIndexes` (`OfficeDocumentParser.kt:442`) matched a paragraph's style
name against the `toc N` values in `word/styles.xml`. Sample-6 declares `TOC1`/`TOC2`/
`TOC3` with `w:name` values `toc 1`/`toc 2`/`toc 3`, so the match was reachable in
principle.

It never happened, because of the paragraph the parser built. Any paragraph with direct
paragraph formatting gets a synthetic `inline-p-N` style
(`OfficeDocumentParser.kt:2501-2530`), and `hasDirectPPr` is set by `w:tabs` at `:2063`.
Read from the fixture, every one of Sample-6's 45 TOC paragraphs carries
`<w:pPr><w:pStyle .../><w:tabs/></w:pPr>`, so every one arrived under a synthetic name
that `stylesMetaMap` has no entry for.

Word writes `w:tabs` on every TOC entry to hold the right-aligned dot-leader stop that
aligns its page number, so this is not a quirk of one file: the branch was unreachable
for any real producer's table of contents.

The synthetic style already records the authored style as `parentStyleName`
(`:2509`). `collectDocxAuthoredIndexes` now walks that chain through a new
`docxStyleName` helper (`OfficeDocumentParser.kt:514`), bounded at 16 hops so a
malformed cycle cannot hang it.

Fixture counts confirm the expectation the tests already carried: Sample-6 has 45
top-level TOC paragraphs (6 `TOC1`, 11 `TOC2`, 28 `TOC3`) and Sample-4 has 21
(7 `TOC1`, 14 `TOC2`), with none nested inside a table in either file.

### 12.3 A test that could not fail

After §12.1 and §12.2 one failure was left:

```text
DocxNumberingReaderTest.sample 6 numId 15 resolves a multi-level spec with BAB prefix on level 1
java.lang.AssertionError: numId 15 must resolve (numToAbstract={}, abstracts=[])
```

The parenthetical was added for exactly this run. An empty `numToAbstract` and an empty
`abstractSpecs` mean `read()` returned a bare `NumberingParseResult()` for the whole
file, not a spec that was missing numId 15.

Three things then lined up. `DocxNumberingReaderTest` was the only class in
`app/src/test/java/com/example/` that reached a parser without
`@RunWith(RobolectricTestRunner::class)`; `DocxNumberingReader.read()` ends in
`catch (_: Exception) { NumberingParseResult() }`; and three of the class's four cases
assert on emptiness, so an empty result satisfied them:

| Case | Assertion | Passes on an empty result? |
|---|---|---|
| `sample 3 has no numbering xml so result is empty` | `assertTrue(result.isEmpty())` | yes |
| `negative abstractNumId is ignored per MS-OI29500` | `assertNull(result.numSpecs[1])` | yes |
| `numId zero is the suppression sentinel and has no spec` | `assertNull(result.numSpecs[0])` | yes |
| `sample 6 numId 15 resolves ...` | `assertNotNull(result.numSpecs[15])` | **no** |

Without the Android runtime `org.xmlpull.v1` has no implementation, so
`XmlPullParserFactory.newInstance()` throws, the catch swallows it, and the reader
reports "this document has no lists". The class now runs under Robolectric like every
other suite that touches a parser.

The production side is changed too, because the same collapse would happen on a device:
`NumberingParseResult` carries `parseError`, set to the throwable's class and message on
failure and to `no word/numbering.xml entry` when the part is genuinely absent. Callers
can now tell a document with no lists apart from a reader that broke. The early return
for a missing file is unchanged, so `Sample-3`'s expectation still holds without a
`parseError`.

### 12.4 What this session could not verify locally

`DocxNumberingReader.read()` was run here against the real fixture through a StAX-backed
`XmlPullParser` standing in for `org.xmlpull.v1` (`/tmp/ktreal/`, production
`DocxNumbering.kt`, `Numbering.kt`, `LayoutUnits.kt`, `ZipSafe.kt` copied verbatim,
`kotlinc-jvm -include-runtime`). It resolves all 21 `numSpecs`, `numToAbstract` matching
the fixture exactly, and `numSpecs[15]` as `NumberingSpec(name=num15, displayName=Makalah
Default, ...)` with level 1 `numPrefix="BAB "`, `numSuffix=""`, `displayLevels=1`.

That is why `DocxNumberingReaderTest` asserted `assertEquals(".", lvl2.numSuffix)`: the
expectation was wrong, not the reader. `abstractNum 14` ilvl 1 declares
`lvlText="%1.%2"`, so the separator sits between the placeholders and nothing follows
the last one. Corrected to `assertEquals("", lvl2.numSuffix)`.

The stand-in parser is not `KXmlParser`, which is why it hid §12.3: the harness had an
implementation and CI did not. Treat the harness as evidence about the parse logic only,
never about parser availability.
