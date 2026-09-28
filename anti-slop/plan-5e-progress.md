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

[Run 36372935809](https://github.com/makerandreas/Papirus-Office/actions/runs/36372935809), PR #19 head `961d4a3` (GitHub test merge `52fecdf`): **205 tests, zero failures/errors/skips**. All three `CreateNewDocumentTest` cases and `FixtureIdentityTest` pass. The Unit Tests job completed in 4m59s. The APK job is still pending at this record point.

The twelve-fixture dump retains baseline ODT pages `[12,22,11,7,14,21]` and DOCX `[12,23,12,11,16,23]`. Its backend is explicitly the legacy Paint per-character stub (`MMMM=iiii=4` at 35 px for 14 pt), not device pagination. These results do not pass all planned fidelity windows; those remain unmodified and are not yet enforced.

## First semantics batch (partial stage 1)

- ODF break-after has its own field and is emitted after the paragraph/heading. Break ownership moved from direct body children to paragraph contexts, covering nested sections. Empty ODF paragraphs/headings are retained.
- ODF `auto` clears inherited before/after/keep flags. `normal` and percentages clear inherited absolute line height; absolute declarations clear percentage basis.
- DOCX `atLeast` is a minimum distinct from `exact`, through docDefaults, styles and direct properties. A declared spacing mode replaces the inherited mode; unrelated spacing properties leave it intact.
- DOCX explicit `0`/`false`/`off` reset keep-next and page-break-before, including inherited docDefaults.
- `TextMetrics` distinguishes ODF font-size-based percentage minima from DOCX natural-height multiples. Its mixed-line method accepts maximum descendant font size and natural height. This method is tested as a contract, **not yet wired into the paginator or Compose**.
- Added synthetic style/break/reset tests and deterministic line-height tests. CI for this batch is required before treating it as validated.

This is not all of stage 1: inline DOCX boundaries/sections and keep/widow/orphan/tab settings remain open. No renderer or editing pipeline changes are included here.

## Remaining gates

1. Finish break and style semantics: DOCX inline break source ranges and section kinds, tabs, keep-together/widow/orphan settings, DOCX empty paragraphs; verify the first batch above.
2. Load-bearing measurement: injected metrics, mixed runs, lossless wrapping, indents/tabs, content/style/width/backend-aware cache.
3. Line-fragment pagination with bounded keep/progress rules and stable source/page mappings.
4. Renderer/input/navigation parity, shared line geometry, fragment-aware editing/selection and an honest Web View state.
5. Twelve fixture windows without tuning, stress/incremental tests, Android visual/geometry smoke evidence. Physical-device Chapter 1 checklist remains after Plans 6-11.

No pagination fidelity or visual parity claim is made for this baseline repair. Template gallery replacement is still Plan 11; online discovery is still deferred.
