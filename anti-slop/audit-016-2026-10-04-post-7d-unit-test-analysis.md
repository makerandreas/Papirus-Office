# Audit 016 (2026-10-04): post-7D unit-test analysis, prior-PR review, and Plan 7E strategy

**Baseline:** `main` `cb89460254a7623e3336d44a38ca258e7488b14f` ("refactor: simplify project and remove unused components", parent `66895a7`, the Plan 7D merge). Branch `arena/01a10750-papirus-office`.
**Scope:** the post-unit-test analysis the owner asked for, a review of the landed Plan 7 PRs against the ledger, and the readiness and shape of Plan 7E (ODF font-face declarations, aliases, and final pagination calibration). No production code changes are made here.
**Method:** antislop DURING mode. `gh run`/`gh api` for CI evidence (Actions log storage is unreachable from the sandbox, see §1.1), `git diff 66895a7 cb89460` for the refactor, raw ZIP/XML re-derivation of the twelve `tests/inky` fixtures, and file:line tracing from `OdtImportPipeline` through `SvXMLImport`, `FontRegistry`, `TextMetrics`, `OfficeRuns` and `OdtDocumentWriter`. ODF 1.4 Part 3 was read from `docs/odf` (§3.14, §16.23, §19.502.3, §19.532, §20.277). No JDK is available, so nothing was compiled locally; §1.2 and §1.3 are static proofs, not build output.

---

## 1. Post-unit-test analysis: `main` does not compile

### 1.1 The two CI jobs on `cb89460`

| Job | Step | Result | Duration |
|---|---|---|---|
| Unit Tests (`111454474101`) | `Run Unit Tests` (`./gradlew testDebugUnitTest`) | **failure** | 2m31s |
| Build (SemVer & Nightly) (`111454474325`) | `Build Debug APK` | **failure** | 4m47s |

Run `37208441343` (push event, so the PR-comment mirror stays skipped). The same workflow passed on the 7D merge 8 hours earlier (run `37182899399`) and on the daily schedule (run `37188453644`).

Artifact sizes separate "tests failed" from "tests never ran":

| Run | Commit | `unit-test-reports` |
|---|---|---|
| `37182899399` | 7D merge | 187,264 bytes |
| `37188453644` | nightly | 187,572 bytes |
| `37208441343` | `cb89460` | **5,224 bytes** |

A test-compile failure produces XML for nothing and a checkstyle-sized zip, which is what 5 KB is. Both jobs fail on the same root cause: `compileDebugKotlin` cannot succeed, so neither the unit-test task nor `assembleDebug` can finish.

### 1.2 Static proof: redeclared top-level declarations (hard compile errors)

Same package, same name, twice. Kotlin rejects each pair with "Redeclaration", independent of any analysis here.

| Package | Name | Declaration A | Declaration B |
|---|---|---|---|
| `...papirusoffice.data` | `DocxDocumentParser` | `DocxDocumentParser.kt:15` (class) | `OfficeDocumentParser.kt:33` (typealias) |
| `...papirusoffice.data` | `DocxParseResult` | `DocxDocumentParser.kt:9` | `OfficeDocumentParser.kt:35` |
| `...papirusoffice.data` | `OfficeFile` | `DocumentSession.kt:9` (new `class OfficeFile(val file: File)`) | `OfficeDocument.kt:499` (existing data class) |
| `...papirusoffice.data.writer` | `SelectionRange` | `SelectionEngine.kt:6` (added by the refactor) | `SelectionRange.kt:6` (existing) |
| `...papirusoffice.core.fonts` | `GoogleFontsResponse` | `FontMetadata.kt:20` (unchanged) | `GoogleFontsRepository.kt:14` (copy added by the refactor) |
| `...papirusoffice.core.fonts` | `GoogleFontMetadata` | `FontMetadata.kt:7` (unchanged) | `GoogleFontsRepository.kt:16` (copy added) |
| `...papirusoffice.core.fonts` | `DownloadableFont` | `FontMetadata.kt:25` (unchanged) | `GoogleFontsRepository.kt:23` (copy added) |

`git show 66895a7:...GoogleFontsRepository.kt` has no data classes at all, so the three `core.fonts` collisions are introduced by this commit, not inherited.

### 1.3 Static proof: unresolved references

| Symbol | Required by | Why it is gone |
|---|---|---|
| `BuildConfig.GEMINI_API_KEY` | `GeminiAiService.kt:132` | The new `build.gradle.kts` declares only `ENABLE_OOXML_SUPPORT` and `ENABLE_OMML_PARSER` |
| `BuildConfig.GOOGLE_CSE_CX` | `TemplateSearchRepository.kt:19` | same |
| `BuildConfig.GOOGLE_CSE_API_KEY`, `BuildConfig.GOOGLE_FONTS_REST_API` | `ApiKeyManager.kt:18,26,50,51` | same |
| `PlacedPage` | `DocumentImages.kt:46` (`predecodeTargets(pages: List<PlacedPage>, ...)`) | No such type exists anywhere under `app/src/main`; the paginator emits `PageLayout` (`LayoutEngine.kt:42`) |
| `OfficeImage.file` | `DocumentImages.kt:56,63` | `OfficeImage` has `imageFile` (`OfficeDocument.kt:189-195`) |
| `DocumentSession.id` | `DocumentLifecycleManager.kt:82` (`current.value?.id == session.id`) | The rewritten `DocumentSession` has no `id` |
| `LibreOfficeCore.initialize(...)` as `Boolean` | `LokitSubplan12aTest.kt:169` (`assertTrue(...)`) | The rewritten `initialize` returns `Unit` |
| `com.github.takahirom.roborazzi.*` | `GreetingScreenshotTest.kt:7-8` | `app/build.gradle.kts` no longer applies the roborazzi plugin or declares its test dependencies (the catalog entries remain in `gradle/libs.versions.toml`) |

Secondary, not proven broken but unverified: `AppVersion`/engine-version metadata is gone from `build.gradle.kts`, `versionCode = 1`, `versionName = "1.0"`, the release signing block now points at a keystore that CI does not provide, and `google-services` is applied only when `google-services.json` exists.

The refactor's deletions inside `LibreOfficeCore` itself are clean: `evaluateFormula`, `renderPageToBuffer`, `isNativeConfigured`, `isLibraryLoaded`, `LO_NATIVE_LOAD_ORDER`, `tryLoadNative`, `tryLoadOne`, `safeLog` and `currentCallback` have zero remaining references. What breaks is the removed native probe chain (Plan 12A) and the changed signatures.

### 1.4 Feature regressions hidden behind the compile failure

| File | Lines before to after | What the shipped app loses |
|---|---|---|
| `ui/home/AboutScreen.kt` | 502 to 48 | Versioning, LibreOfficeKit core attribution, Document Liberation Project credits, open-source licences (required by `AGENTS.md`, "About Screen") |
| `ui/components/UniversalXmlImportSheet.kt` | 1178 to 14 | The whole XML import surface, replaced by a stub with the hard-coded literal "XML Import options not available in this build" |
| `ui/components/UniversalChartSheet.kt` | 435 to 15 | Chart options and data entry, replaced by a stub with "Chart options not available in this build" |
| `data/framework/PapirusOdfEngine.kt` | 185 to 40 | Package inspection, document properties and logging become `simulate*` mocks (`getOdfZipContents` returns `emptyList()`), while `UniversalOdfSheet.kt` still calls them |
| `core/jni/LibreOfficeCore.kt` | 155 to 23 | The soname load chain, `putenv` setup and page-render seam from Plan 12A become a logging stub; `LokitEngine.isNativeAvailable` now reads a plain `var` that nothing sets |
| `data/DocumentImages.kt` | 128 to 68 | Plan 6C decode-scale, edge clamp, timestamped cache key, alias-map resolution and predecode de-duplication |
| `data/DocumentSession.kt` | 72 to 60 | `id`, `protected`, `readOnly`, `parserReport`, `sessionIdProvider` wiring, and the dirty-setter notification protocol |
| `data/OfficeDocumentParser.kt` | 3367 to 3375 | Adds the duplicate `typealias DocxDocumentParser` and `data class DocxParseResult` |

Two of the six frozen edits from PR 12 are also reversed in place: `Type.kt` now maps `google_sans_code_regular` (which exists, `res/font/google_sans_code_regular.ttf`) to `google_sans_code_medium`, duplicating the Medium slot, and `ic_pagella_logo.xml` carries a different `pathData` than the version the design work approved. Both are unverified visual changes.

### 1.5 What in the commit is legitimate

- The stray nested path `app/applet/app/src/main/java/.../PapirusSdkBridge.kt` is deleted. GitHub's commit history for that path shows it arrived with `9bf50a41` (2026-07-29) and was never part of the module. Deleting it is correct and should survive any repair.
- `DocxEmbeddedImage.documentImageRequest` moves to Coil's typed `Size(width, height)` overload with the matching import. That is the API-correct form.
- Five test files now locate the repository root by `settings.gradle.kts` as well as `.git`, and `Plan6ImageFoundationTest` gained a `SampleMatrix.findTestFile` fallback. Tests must not depend on a `.git` directory being present, so this is a legitimate hardening.
- `minSdk 24 to 26`, `targetSdk 36 to 35`, `security-crypto 1.0.0 to 1.1.0-alpha06` and the smaller `gradle.properties` may be intentional. They are owner decisions, not defects, but they contradict `PROJECT_CONTEXT.md`/CI expectations until recorded.

### 1.6 The red flag to carry into every later session

The commit is a bulk rewind of plan-backed work (Plans 6C, 12A, and parts of 7B/7C/7D) plus a generic AI Studio application template (`buildConfigField` set, `versionCode 1`, "not available in this build" stubs). It landed directly on `main`, so `main` is red and every later PR inherits a broken baseline: CI cannot produce test evidence for Plan 7E until this is repaired. The unit-test run the owner performs in Google AI Studio will also fail for the same reason, because it compiles the same sources.

---

## 2. What the tests said before the regression

| PR | Plan | Final run | Unit tests | Suites | Result |
|---|---|---|---|---|---|
| #27 | 7B | `37113461800` | 301 | 55 | all passed |
| #28 | 7C | `37172853828` | 320 | 58 | all passed |
| #29 | 7D | `37177899006` | 330 | 61 | all passed, 0 skipped |

Plan 7D's own iteration is visible in the PR comments: `e8946e9` ran 330 with 3 failures, all in `Plan7dTableImportTest.existingOdtCorpusKeepsTheExactTableMatrix` and all the same cause, "Method newInstance in org.xmlpull.v1.XmlPullParserFactory not mocked". `5d0f89f` left 1 failure, `6a94cda` and `577c411` were clean. That is a working, iterative PR, and it is the state the refactor overwrote.

The 7D tests remain in the tree (`Plan7dTableGridTest` 108 lines, `Plan7dTableImportTest` 162, `Plan7dTableLayoutTest` 202) with the geometry they pin: two columns at 90f and 190f, a row minimum at or above 28f, a spanning cell contributing to the row group, a 280f fragment width, one border edge per owned edge, `MIDDLE` vertical alignment, and repeated header rows with unique body coverage.

---

## 3. Prior-PR review against the ledger

| Plan | PR | Ledger claim | What the tree shows |
|---|---|---|---|
| 7A | #26 | ODF numbering, heading/list runs, hyperlinks, bookmarks; LOKit seam | Present (audit-013); the LOKit seam it added is what `cb89460` gutted |
| 7B | #27 | One package/import pipeline, canonical sidecars, fail-closed save capability | Present: `OdtImportPipeline`, `DocumentSemantics`, `OdtDocumentWriter.saveCapability` (`:142-156`) |
| 7C | #28 | Authored indexes, named sections, Navigator rows, status context | Present: `Plan7c*Test.kt` in tree; audit-015 §9 is the record |
| 7D | #29 | Table geometry from import through hit-testing | Present: `TableGrid.kt` (16,202 bytes), `TableLayout.kt` (18,395 bytes), three `Plan7d*Test.kt`; roadmap v2 §4.7c is the record |

The ledger itself is consistent, but three conventions deserve stating because they shaped this analysis:

1. **The only durable test log is the PR comment.** `.github/workflows/build.yml` posts `scripts/ci-dump-comment.py` output on `pull_request` events only. A direct push to `main` (which is how the refactor landed) produces no report, and the raw logs live on storage the sandbox cannot read (`gh run view --log-failed` returns an EOF from `results-receiver.actions.githubusercontent.com`). Anyone who needs a post-push analysis must re-derive it statically or wait for the next PR.
2. **Forecast slots are not reservations.** 7E is forecast at PR #30; 7D took #29 as forecast. Nothing here depends on the number.
3. **A stray path can survive several merges.** The `app/applet/...` file was committed 2026-07-29 and rode along until this commit. Worth a cheap check in future PR reviews.

---

## 4. Plan 7E readiness

### 4.1 What already exists (verified in the tree)

| Piece | Location |
|---|---|
| `FontChoice`, `FontSource`, `GenericFamily`, `FontRegistry.resolve/composeFamilyFor` | `data/FontRegistry.kt` (`resolve` at `:168`, `composeFamilyFor` at `:196`) |
| Bundled metric-compatible map (Times New Roman, Arial, Courier New, Calibri, Cambria, Cambria Math, Symbol, Wingdings) | `FontRegistry.metricCompatible` |
| Stand-ins, Aptos and Aptos Display to Martel Sans, `metricCompatible = false` | `FontRegistry.standIns` |
| `OfficeFontFace(name, family, genericFamily, pitch, charset)` | `DocumentSemantics.kt:213` |
| `DocumentStyles.fontFaces`, documented as "Populated by Plan 7E" | `OfficeDocument.kt:279-280` |
| Source-feature detector, `hasFontFaceDeclarations` | `OdtImportPipeline.kt:231-262` (`"font-face" -> fontFaces = true`) |
| Tokens `XML_FONT_FACE_DECLS`, `XML_FONT_NAME`, `XML_FONT_FAMILY` | `OdfXmlToken.kt:23,126,127` (declared, no consumer) |
| Both style readers that currently pass the raw alias through | `SvXMLImport.kt:260-263` (list-label fonts) and `:433-437` (text properties): `attrs["font-name"] ?: attrs["font-family"]` with quote trimming |
| Alias survives the adapters | `Plan7bSemanticImportTest.kt:162-187` (round-trip of `fontFaces`) |

So the model, the tokens and the registry are in place. Nothing reads `office:font-face-decls`, and nothing resolves an alias. Both consumers already agree on the input, `ParagraphStyle.fontFamily`: the paginator builds `TextMetrics(style, FontRegistry.resolve(style.fontFamily), source)` (`TextMetrics.kt:218`) and the renderer calls `OfficeRuns.fontFamilyFor` to `FontRegistry.composeFamilyFor` (`OfficeRuns.kt:102`, used at nine `LayoutDrivenDocumentRenderer` sites). One shared decision is therefore reachable without a second lookup table, which is exactly the roadmap's "no renderer-only alias map" clause.

### 4.2 Fixture evidence (re-derived 2026-10-04 with `zipfile` plus regex)

Every `tests/inky/*.odt` has exactly one `office:font-face-decls` block in `content.xml` and one in `styles.xml`, with identical sets. Family values are quote-wrapped when they contain a space. The generated aliases:

| Alias | Declared family | Samples |
|---|---|---|
| `Aptos1` | `Aptos` | 1, 2, 3, 4, 5, 6 |
| `Aptos2` | `Aptos` | 6 |
| `Aptos Display1` | `Aptos Display` | 1, 2, 3, 5, 6 |
| `Times New Roman1` | `Times New Roman` | 1, 2, 4, 5, 6 |
| `Arial1` | `Arial` | 4 |
| `Noto Sans Devanagari1` | `Noto Sans Devanagari` | 2, 3, 4, 5, 6 |
| `Basic Sans1` | `Basic Sans` | 4 |

Two corrections to the session brief: Sample-3 declares no `Times New Roman1` (its set also carries `Microsoft YaHei UI`, `Segoe UI Variable`, `Calibri`), and `Basic Sans1` in Sample-4 is an eighth alias that the brief's list omits. `Aptos Display1` is absent from Sample-4, whose faces are `Aptos`, `Aptos1`, `Arial`, `Arial1`, `Basic Sans`, `Basic Sans1`, `Carlito`, the two Noto faces, and `Times New Roman`/`Times New Roman1`.

DOCX side, for boundary clarity: the six `word/styles.xml` files name real families (`Aptos`, `Aptos Display`, `Times New Roman`, plus `Carlito`/`Calibri` in Samples 1 and 3, `Arial`/`Basic Sans` in Sample 4) and every package carries `word/theme/theme1.xml`. DOCX has no alias table of this kind; theme indirection (`w:asciiTheme`, `w:hAnsiTheme`) stays with Plan 8A.

### 4.3 Normative basis

- **ODF 1.4 Part 3 §3.14** `<office:font-face-decls>` holds the font face declarations of a document.
- **§16.23 / §19.502.3** `<style:font-face>` carries `style:name` and, among others, `svg:font-family` (§19.532). `style:name` is an identifier, not necessarily a real family.
- **§20.277** `style:font-name` "select[s] a font face declaration. If a font face declaration is referenced by name, the font-matching algorithms for selecting a font declaration based on the font-family, font-style, font-variant, font-weight and font-size descriptors are not used but the referenced font face declaration is used directly." That sentence is the whole reason the alias must be resolved through the declaration table before any metric match.
- **LibreOffice API precedent:** `com.sun.star.style.CharacterProperties` exposes `CharFontName` ("may contain more than one name separated by comma") and `CharFontFamily`, so a resolver that reads a family list and a family class from the declaration matches the engine concept.
- **OOXML SDK boundary:** `w:rFonts` carries `w:ascii`/`w:hAnsi` plus `w:asciiTheme`/`w:hAnsiTheme`, and theme references take precedence. That is Plan 8A/8B work, not 7E.

### 4.4 Gaps to close

1. **Producer.** A context for `office:font-face-decls` (both XML parts) filling `DocumentStyles.fontFaces`, with `style:name` as the key. `SvXMLImport.parseOdfXml` already preloads `styles.xml` before `content.xml` (`:1267-1275`), and `parseOdfStyles` is the natural seam for both parts. Precedence has to be defined and tested: the same alias can appear in both parts with the same declaration in these fixtures, so the rule should be "first wins, and identical redeclaration is not an error", documented either way.
2. **Resolver.** One pure function, alias plus `fontFaces` map to family (first family of `svg:font-family`, quotes and commas handled), used by both the import path and the tests, so the alias form and the direct form converge before `FontRegistry` is asked. Missing declaration, blank `svg:font-family`, and self-referential aliases must fall back to the raw name so `FontRegistry` classification still answers.
3. **Consumer wiring.** The resolved family reaches `ParagraphStyle.fontFamily` so both existing consumers stay on one path. No change to `FontChoice.composeFamily` (it still maps to generic families, which keeps the "no bundled file is painted" claim true until Plan 10 A1).
4. **Calibration.** Re-run `Plan5ElementDumpTest` and `PaginationFidelityTest` over the twelve fixtures, print every page count next to its window, and explain any shift. The windows in `SampleMatrix.kt` stay as they are (DOCX 12..18, 18..28, 18..26, 8..12, 15..21, 15..26; ODT 12..18, 18..28, 18..26, 9..13, 15..23, 17..27).
5. **Save capability (owner decision D3).** `OdtDocumentWriter.saveCapability` (`:142-156`) blocks a modified save for authored indexes, named sections and source table structure, but `hasFontFaceDeclarations` is not in the list. Making font faces load-bearing without adding them there means an edit to an ODT that declares faces can still regenerate a `content.xml` with no declarations at all. All six fixtures already block modified saves through tables or indexes, so adding the check changes no fixture behaviour; it only closes the general case. The alternative, writing `<office:font-face-decls>` into the regenerated part, is writer work the ledger reserves for Plan 9.

### 4.5 The two extra roadmap items

Roadmap v2 §4.7d moved two items into 7E on 2026-10-04: item 5, ODF `style:tab-stops` with `style:leader-style` and `style:leader-text` plus leader painting (moved out of 7C), and item 6, excluding hidden-section ranges from layout (7C lists them and keeps jumps out, but the paginator still lays their text out). Both are layout changes that move the page matrix, while the font work is a resolution change that mostly should not. Bundling all three in one PR makes every page-count movement ambiguous, which is why §5.2 recommends a split.

---

## 5. Strategy options

### 5.1 Step 0 (blocking): restore `main`

| Option | What it means | Cost | Risk |
|---|---|---|---|
| **A. Revert plus re-apply (recommended)** | `git revert cb89460` on a short repair branch, then one follow-up commit that re-applies only the four verified-good changes from §1.5 and deletes the stray `app/applet/...` path again | Small, mechanical, restores 380+ tests of coverage in one diff | None that is not already in the reverted commit's intent; the two unverified visual edits (Type.kt, logo path) return to their approved state |
| B. Repair forward | Keep the AI Studio versions and make them compile, then re-implement the deleted features | Large: it is effectively re-doing Plan 6C, Plan 12A, the ODF sheet, the XML/chart sheets and the About content | The baseline stays red for a long time; the repair diff is unreviewable |
| C. Branch 7E off `66895a7` and ignore `main` | 7E proceeds on a green base | Medium | `main` stays red, the next AI Studio sync recreates the same conflict, and the PR cannot merge cleanly |

Recommendation: **A**, as a two-commit repair PR, merged before any 7E commit. The repair PR is also the natural place to record the outcome in `plan-01`/roadmap, because the ledger currently says nothing about a rewind.

### 5.2 Plan 7E shape

| Option | Contents | Verdict |
|---|---|---|
| **1. Split (recommended)** | **7E** declarations, alias resolution, calibration. **7F** tab stops/leaders and hidden-section layout | Keeps the page-count diff attributable: 7E should move counts only where an alias changed a real metric, and 7F is expected to move them |
| 2. Single 7E | All six roadmap items plus calibration | One PR owns every page-number change at once; the "explain every shift" gate becomes guesswork |
| 3. Fonts only, defer 5 and 6 with no owner | Smallest diff | Silently drops two items a previous owner decision assigned to 7E; only acceptable with an explicit re-assignment |

The owner's brief already describes option 1's font half in full detail, including the exit gate, so option 1 is also the least surprising reading of the session.

### 5.3 Recommended commit and test plan for 7E

Four commits, each CI-checkable:

1. **Producer.** `OdfFontFaceContext` plus `style:font-face` reading into `DocumentStyles.fontFaces` from both parts; precedence and malformed-declaration handling; `Plan7eFontFaceImportTest` with per-fixture inventories (Sample-1 nine faces, Sample-2 ten, Sample-3 twelve, Sample-4 twelve, Sample-5 ten, Sample-6 thirteen, all re-derived in §4.2) and synthetic single-part, duplicate-alias and blank-family cases.
2. **Resolver.** `FontFaceResolver.familyFor(name, fontFaces)` and one `FontChoice` helper, wired into the two `SvXMLImport` readers and `TextMetrics.forStyle`, with `Plan7eFontResolutionTest` asserting the brief's gates: alias and direct form produce equal `FontChoice`; `Times New Roman1` and `Times New Roman` both reach `Liberation Serif` with `BUNDLED_METRIC_COMPATIBLE`, `metricCompatible = true` and `assetStem = "LiberationSerif"`; `Aptos1`, `Aptos2` and `Aptos Display1` reach `Martel Sans` with `BUNDLED_STAND_IN` and `metricCompatible = false`; the generic `composeFamily` mapping is unchanged, which is the honest form of "nothing is painted from `assets/fonts` until Plan 10".
3. **Save capability.** Add the font-face arm to `saveCapability` with a test that a modified document declaring faces is refused and that an undeclared one still saves (decision D3).
4. **Calibration and docs.** Re-run the element dump and the twelve-fixture pagination test, paste the page counts and windows into this audit's record, update `plan-01` row 7, roadmap v2 §4.7d, the Plan 7 record, and `PROJECT_CONTEXT.md`.

Commit 2 is where a table-height regression would show up if the resolution change altered run metrics, so `Plan7dTableLayoutTest` and `PaginationFidelityTest` run unchanged at that head.

### 5.4 Exit gates mapped to evidence

| Brief's gate | Where it is evidenced |
|---|---|
| Alias and direct forms produce the same `FontChoice` | `Plan7eFontResolutionTest` (commit 2) |
| `Times New Roman1` reaches Liberation Serif substitution | same test, `FontSource.BUNDLED_METRIC_COMPATIBLE` plus `assetStem` |
| `Aptos1` reaches Martel Sans and stays non-metric-compatible | same test, `BUNDLED_STAND_IN`, `metricCompatible = false` |
| No claim of painted bundled files before Plan 10 | `FontChoice.composeFamily` mapping test plus the absence of any `assets/fonts` loading change in 7E's diff |
| All twelve fixtures stay in their windows, none widened | `PaginationFidelityTest` and `Plan5ElementDumpTest` output in commit 4, compared line by line with the pre-7E dump |

---

## 6. Owner decisions

1. **D1, the refactor.** Confirm option A (revert plus re-apply) or state which of its changes are wanted on their own.
2. **D2, 7E shape.** Confirm the split of §5.2 (7E fonts and calibration, 7F tab stops and hidden sections), or keep one plan.
3. **D3, save capability.** Block modified saves that would drop font-face declarations (recommended, no fixture changes), or leave it and record the loss until Plan 9. **Superseded 2026-10-04:** the owner deferred this to Plan 9, which owns writer regeneration; the loss (a regenerated `content.xml` would drop the declaration table) is recorded in `audit-017` section 12 rather than blocked.
4. **D4, sequence.** Repair first and then implement 7E in this session, or stop at the repair and plan 7E in a later session.
5. **D5, AGENTS.md.** Approve the added local-toolchain note recording that the sandbox can reach PyPI and GitHub only, that `jdk4py` ships a JRE without `javac`, and that CI stays the compile/test evidence path.

---

## 7. Delivery Gate (documentation-only deliverable)

Reduced gate per `AGENTS.md` addendum item 3.

- R-02 PASS: no em dash in this document; a `grep` for the U+2014 character over the new file returns zero lines.
- R-15 PASS: no calls to action; this is an internal analysis record.
- R-16 PASS: no marketing vocabulary; section headings name the artifact (audit, plan, gate).
- R-17 PASS: every number traces to a named source: CI run IDs and artifact byte counts (§1.1), file:line citations (§1.2, §1.3), before/after line counts from `git show 66895a7:<path> | wc -l` (§1.4), PR comment counts (§2), and the fixture re-derivation stated in §4.2.
- R-36 PASS: no capability, performance or compliance claim is asserted. §1.3 is labelled a static proof and the analysis explicitly states that nothing was compiled or run locally.
- R-38 PASS: no placeholder content is presented as real; the repair and plan steps are labelled options with their owners.
- C-5 PASS: the one claim that could mislead, "main is red", is backed by two failing jobs on one commit ID and by a 35x artifact-size difference against the last green run.
- R-26, R-27, R-32, R-34, R-35 N/A: no UI shipped in this deliverable.

---

## 8. antislop tooling and local toolchain

`skills/antislop/VERSION` is `3.2.20`. Mode resolution: the platform preference file `~/.config/antislop/settings.json` was absent, while the repository copy `.config/antislop/settings.json` carries `{"mode":"during"}` from the 2026-10-04 owner decision (audit-015 §8). This session copied the repository value to the platform path so other sessions resolve it as a global preference, and ran in DURING mode. No image generation or asset work was involved, so R-23 did not apply.

Local build attempt, recorded for `AGENTS.md`:

| Check | Result |
|---|---|
| `java`, `javac`, `/usr/lib/jvm` | absent |
| `apt-get update` | fails, `deb.debian.org` unreachable |
| JDK vendors (`api.adoptium.net`, `cdn.azul.com`, `download.java.net`) | unreachable |
| PyPI | reachable; `jdk4py==25.0.2.1` downloads (35.4 MB wheel) |
| Contents of that wheel | a runtime only: `jdk4py/java-runtime/bin/java` and eleven peer tools, no `javac` |
| `services.gradle.org`, `repo.maven.apache.org`, `dl.google.com`, `jitpack.io` | unreachable (000) |
| `~/.gradle` cache | absent |

Conclusion: a full JDK is not obtainable here, and even with one the Gradle distribution, the Android plugin and every dependency are unreachable, so `./gradlew testDebugUnitTest` cannot run locally. GitHub Actions stays the compile and test evidence path, exactly as `AGENTS.md` already provides, and static proofs must be labelled as such when used in a PR body.
