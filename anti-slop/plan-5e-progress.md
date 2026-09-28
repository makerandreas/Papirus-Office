# Plan 5e implementation record

Date: 2026-09-28. Scope and gates: [audit-011](audit-011-2026-09-28-plan-5e-readiness.md), section 6.

**Status: started, not complete.** Plan 1 synchronization and Plans 6-11 have not started.

## Gate 0: baseline repair

- Integrated main commit `683cb78` (updated `Untitled.ods` and `Untitled.odp`) into the fixed session branch.
- All three blanks pass local ZIP CRC and XML well-formedness checks and declare content/manifest version 1.4. The updated ODS has one sheet; ODP has one slide. This supersedes audit-011's observations of the old ODS/ODP, not its historical evidence.
- Corrected exact-case asset paths while preserving lowercase cache filenames. Removed unrelated styled-template fallbacks and host-filesystem fallbacks from blank extraction. Missing assets return failure, not a different document.
- `CreateNewDocumentTest` now checks byte identity, MIME/manifest/body identity, ODS/ODP structure and replacement of stale cache content, in addition to existing UI entry checks.
- Recorded twelve Writer fixture hashes, producer metadata and saved reference page counts in `tests/inky/fixture-identities.properties`, checked against `SampleMatrix` by `FixtureIdentityTest`. No sample bytes or acceptance windows changed.
- Corrected `LayoutDump`'s flow header to body bounds; a low element count is no longer described as proof of inflated metrics. Page-end classifications are still inferred from element order, pending explicit fragment end reasons.

Baseline failure supplied by owner: run `36370092065`, 204 tests, two failures in `CreateNewDocumentTest` (expected `untitled.odt`, received `Default.ott`). Compilation succeeded. This is the actual baseline failure, not a JNI diagnosis. No local JDK/Android SDK is available; Android compilation and tests must be checked through CI. Validation results will be recorded below when available.

## Gate 0 CI evidence

[Run 36372935809](https://github.com/makerandreas/Papirus-Office/actions/runs/36372935809), PR #19 head `961d4a3` (GitHub test merge `52fecdf`): **205 tests, zero failures/errors/skips**. All three `CreateNewDocumentTest` cases and `FixtureIdentityTest` pass. The Unit Tests job completed in 4m59s. The APK build also passed (6m44s).

The twelve-fixture dump retains baseline ODT pages `[12,22,11,7,14,21]` and DOCX `[12,23,12,11,16,23]`. Its backend is explicitly the legacy Paint per-character stub (`MMMM=iiii=4` at 35 px for 14 pt), not device pagination. These results do not pass all planned fidelity windows; those remain unmodified and are not yet enforced.

## First semantics batch (partial stage 1)

- ODF break-after has its own field and is emitted after the paragraph/heading. Break ownership moved from direct body children to paragraph contexts, covering nested sections. Empty ODF paragraphs/headings are retained.
- ODF `auto` clears inherited before/after/keep flags. `normal` and percentages clear inherited absolute line height; absolute declarations clear percentage basis.
- DOCX `atLeast` is a minimum distinct from `exact`, through docDefaults, styles and direct properties. A declared spacing mode replaces the inherited mode; unrelated spacing properties leave it intact.
- DOCX explicit `0`/`false`/`off` reset keep-next and page-break-before, including inherited docDefaults.
- `TextMetrics` distinguishes ODF font-size-based percentage minima from DOCX natural-height multiples. Its mixed-line method accepts maximum descendant font size and natural height. This method is tested as a contract, **not yet wired into the paginator or Compose**.
- Added eight synthetic style/break/reset tests and two additional deterministic line-height tests. The CI results below validate this batch, not the remaining layout gates.

This is not all of stage 1: inline DOCX boundaries/sections and keep/widow/orphan/tab settings remain open. No renderer or editing pipeline changes are included here.

## First semantics batch CI evidence

[Run 36373420922](https://github.com/makerandreas/Papirus-Office/actions/runs/36373420922), PR #19 head `24ae28a` (GitHub test merge `3c80819`): **215 tests, zero failures/errors/skips**. Unit Tests passed in 5m10s; APK build passed in 7m0s. `ParagraphStyleSemanticsTest`: eight cases passed. `TextMetricsTest`: seven cases passed. The full existing regression suite and all twelve fixture dumps ran.

Current dump: ODT pages `[13,24,11,9,16,24]`; DOCX `[12,23,12,11,16,23]`. ODT blank paragraph counts changed from `[0,4,0,0,3,3]` to `[9,42,1,15,30,59]`; total element increases match the newly retained blank paragraphs. Authored-break counts did not change. The backend is still the legacy per-character Paint stub. **Sample-3 remains outside its window in both formats (11/12 pages versus reference 22).** No windows were widened and no hints or spacing inflation were added to force counts.

The implementation commits are pushed to [draft PR #19](https://github.com/makerandreas/Papirus-Office/pull/19). This final evidence update is saved in the working tree; the PR body also records the results, without changing the tested code commit.

## Delivery checkpoint (not release approval)

- PASS, blank entry regression: all three CreateNewDocumentTest cases, including exact package identity and UI entry, pass in both runs.
- PASS, fixture provenance / R-17: SHA-256 identity guard passes; reference counts and producers are recorded; dump backend and actual counts are disclosed above.
- PASS, semantics contract: eight parser cases and seven TextMetrics cases pass; mixed-line metrics are not claimed as integrated rendering.
- PASS, R-02 / R-15 / R-16: new progress/PR copy contains no em dash and names concrete remaining work, not promotional claims.
- PASS, R-36 / C-5: implemented contracts, pending integration, JVM evidence and unperformed device checks are distinguished.
- NOT APPROVED, R-03 / R-25 / R-32 / R-34 / R-35 / C-4: no new Android visual, accessibility, theme or cross-page editing smoke pass. APK compilation and Robolectric tests do not establish these results.
- NOT APPROVED, full R-26 / R-27 / R-38 / C-2: this batch does not repair all existing app capability states or the fabricated gallery; Plan 11 ownership remains explicit.
- N/A to this patch, R-18 / R-23 / R-24 / R-28 / R-37 and Purpose-Gate/Liveliness visual techniques: no new screen, artwork, testimonial, FAQ, navigation structure, theme, motion or design direction was introduced. Existing DESIGN.md direction is unchanged.
- R-33: changes live in Kotlin source and tests; no runtime source-rewriting mechanism is shipped.

**The full Plan 5e delivery gate is open. This checkpoint is not approval to merge or a claim that the app design is complete.**

## Remaining gates

1. Finish break and style semantics: DOCX inline break source ranges and section kinds, tabs, keep-together/widow/orphan settings, DOCX empty paragraphs. The first batch above is CI-verified.
2. Load-bearing measurement: injected metrics, mixed runs, lossless wrapping, indents/tabs, content/style/width/backend-aware cache.
3. Line-fragment pagination with bounded keep/progress rules and stable source/page mappings.
4. Renderer/input/navigation parity, shared line geometry, fragment-aware editing/selection and an honest Web View state.
5. Twelve fixture windows without tuning, stress/incremental tests, Android visual/geometry smoke evidence. Physical-device Chapter 1 checklist remains after Plans 6-11.

No pagination fidelity or visual parity claim is made for this baseline repair. Template gallery replacement is still Plan 11; online discovery is still deferred.

## Continuation: measurement integration (2026-09-28)

Owner decision: real-device testing and visual-fidelity approval are deferred until after Plan 11. They are not blockers for continuing Plan 5e automated implementation. This does not waive source integrity, pagination, input correctness or automated regression gates.

Work now in progress:
- Replaced raw Paint/2.5 sizing and character-count fallback with an injected TextMetrics backend, measured caret advances and lossless source ranges.
- Added hard-newline, grapheme-boundary, emergency long-word, soft/dictionary hyphen, indent, mixed-run and positional-tab measurement.
- Cache keys include text/runs, resolved paragraph/character styles and body width; backend is immutable per engine. The cache is pruned for removed element indices and exposes measured-paragraph counts for incremental tests.
- Imported ODF tabs/keep-together/widows/orphans and DOCX tab settings, positional clear overrides, keep-lines and widow-control through the existing cascade. Tab definitions no longer inject text tabs.
- Paragraph spacing replaces the fixed inter-block gap; DOCX collapses adjacent before/after contributions. Renderer boxes use computed body bounds, not a second margin-padded flow; line height no longer uses point size plus five.

Fragment pagination and source-range-aware input integration are still pending. In particular, these new keep/widow/orphan fields are preserved but not yet enforced by the live paginator. No completion or Compose-measurement parity claim is made at this checkpoint.
