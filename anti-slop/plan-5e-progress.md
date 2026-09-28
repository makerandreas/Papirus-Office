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

## Remaining gates

1. Break and style semantics: mode-specific line spacing and inheritance, explicit false/zero, ODF before/after, DOCX inline break source ranges and section kinds, tabs, keep/widow/orphan settings, meaningful empty paragraphs.
2. Load-bearing measurement: injected metrics, mixed runs, lossless wrapping, indents/tabs, content/style/width/backend-aware cache.
3. Line-fragment pagination with bounded keep/progress rules and stable source/page mappings.
4. Renderer/input/navigation parity, shared line geometry, fragment-aware editing/selection and an honest Web View state.
5. Twelve fixture windows without tuning, stress/incremental tests, Android visual/geometry smoke evidence. Physical-device Chapter 1 checklist remains after Plans 6-11.

No pagination fidelity or visual parity claim is made for this baseline repair. Template gallery replacement is still Plan 11; online discovery is still deferred.
