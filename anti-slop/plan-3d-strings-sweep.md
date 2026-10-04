# Plan 3D: Strings sweep

**Status:** planned 2026-10-04 (owner decision 5, audit-015 §7.1). No PR slot has been chosen. Plan IDs are authoritative; when a slot is picked, record the forecast number here and in plan-01 row 3.
**Parent:** Plan 3 compliance sweep (`plan-03-compliance-sweep.md`). 3A moved toasts and `contentDescription`s and added `SourceHygieneGuardTest`. 3B made the Navigator's empty states honest. 3C aligned the docs.
**Rule being enforced:** `AGENTS.md` says user-visible strings are translated to en_US and added to `values/strings.xml`. `values-in` is not extended (plan-03 §0).
**Evidence:** audit-015 §4. Measured at `1c98e81` with comments masked and heuristic matching, so treat the counts as approximate.

## 1. Scope

1. **About 854 user-facing literals.** 684 `Text("...")`, 111 named UI parameters (`title`, `subtitle`, `description`, `placeholder`, ...), 46 `label = "..."` (animation labels excluded) and 13 Navigator notices and labels. Plan 7C already moved the Navigator notices into typed `NavigatorNotice` values backed by strings, so recount before starting.
2. **Largest files first:** `CellinaModule.kt` (110), `SlidiaModule.kt` (103), `UniversalXmlImportSheet.kt` (85), `UniversalOdfSheet.kt` (66), `UniversalClipboardSheet.kt` (55), `HomeSubpages.kt` (54), `UniversalFormsSheet.kt` (50), `SwTextFormattingInspectorDialog.kt` (47), `InkyModule.kt` (36).
3. **29 keys that look unused.** Wire them in where a literal duplicates one (for example `open_navigation_bar`, while `InkyModule.kt` has `Text("Open navigation bar")`). Delete only the keys that no screen needs, and list each deletion in the PR.
4. **Exceptions that reach the UI** as `failureReason` (`OdtImportPipeline.kt`, the two package-failure messages) move to string resources or to a typed reason that the UI maps.
5. **Typography:** 27 strings use `...` instead of the ellipsis character `…` (Android lint `TypographyEllipsis`). Fix them in the same pass.
6. **FCT literals** that Plan 7C did not touch (`"Delete"`, `"AI options"`, the General Options menu labels) are part of item 1.

## 2. Out of scope

- Log and exception messages that never reach the UI (about 218). They are developer diagnostics.
- New translations. `values-in` stays as it is.
- Copy rewrites beyond what antislop R-02 requires for a moved string. Wording changes are reviewed separately from mechanical moves.

## 3. Guard

Extend `SourceHygieneGuardTest` with two rules:

- `Text("...")` with a literal first argument.
- Named UI parameters (`title`, `subtitle`, `description`, `placeholder`, `label`, `text`) with a literal value. Animation `label` arguments and test sources are excluded.

Both rules start with a **per-file allowance map** set to the counts measured at the start of 3D. Each commit lowers its files' allowances, and the PR ends with the map empty, the same ratchet 3A used for raw greys. A file that goes over its allowance fails CI.

## 4. Delivery

One PR, ordered so the diff stays reviewable:

1. Guard with the measured allowances (CI green, nothing moved yet).
2. One commit per module group: Cellina, Slidia, Universal sheets, Home, Inky and the shared components. Each commit moves the strings and lowers the allowances.
3. Unused-key cleanup and typography.
4. Docs: plan-01 row 3, this file's implementation record, and the plan-03 cross-reference.

## 5. Acceptance

- The allowance map is empty and `SourceHygieneGuardTest` passes.
- `values/strings.xml` has no duplicate keys and no unused keys except those listed in the PR as intentionally kept.
- The CI unit tests and the build pass. Screens look the same, because only the source of the text changes.
