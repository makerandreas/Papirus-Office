# Audit 018 (2026-10-05): editor-text projection correction and PR reforecast

**Baseline:** PR #31 (Plan 7E), merged 2026-10-04; repository checkout is shallow at `61b0644` on `arena/01a109ef-papirus-office`.
**Scope:** turn the post-Plan-7E selection-coordinate review into a separate corrective change, then reconcile current plan forecasts. Plan 7F remains separate and unchanged in ownership.
**Status:** correction committed as `987986a` and pushed as [PR #32](https://github.com/makerandreas/Papirus-Office/pull/32); the PR is open and unmerged. GitHub Actions run `37259378329` passed both Unit Tests (350 tests across 64 suites; 0 failed, errors, or skipped) and Build (SemVer & Nightly). `SelectionProjectionConsistencyTest` passed both tests.

---

## 1. Verified discrepancy

`InkyModule` presents a flat edit string made from paragraph, heading, and list-item body text, joined by two newlines. It skips structural elements such as tables and does not put generated list labels into editable text. Several neighboring code paths did not share that coordinate system:

- `OfficeDocument.toPlainText()` includes list labels and table-cell text.
- `SelectionEngine` used `toPlainText()` for model-based extraction, deletion, and insertion.
- `DocumentTextMerger` and `DocumentTextWindows` used only textual body elements, but each repeated its own projection/separator logic.
- Document loading initialized the editor from `DocxParseResult.text`, while the active-session synchronization independently assembled a model projection.
- `DeleteSelectionCommand` received the current editor string from `EditingEngine`, but its model execution recalculated offsets from the document instead of using that string.

Consequently, selections after list items or tables could refer to different characters in the editor and the model. The issue is coordinate disagreement, not a reason to remove `toPlainText()`; the serializer still uses its broader semantics, which are retained.

## 2. Correction implemented

A single `DocumentTextProjection` now defines the editor text, textual elements, and `\n\n` separator. Structured parse results project through `OfficeParsedDocument.toOfficeDocument()`; parse results without a structured model preserve their raw text as a fallback. The helper is used by:

- Inky session synchronization and document initialization/open/template/reload paths;
- model-based selection extraction, deletion, insertion, and `DeleteSelectionCommand` (which consumes the current editor string when supplied);
- `DocumentTextMerger`; and
- `DocumentTextWindows` element-to-offset mapping.

`OfficeDocument.toPlainText()` is left intact and documented as a broader plain-text view, not an editor-coordinate source.

`SelectionProjectionConsistencyTest` covers a parsed heading/list/table/paragraph document, projection parity between parser/model paths, the mapped window after the table, selecting and deleting the first character after the table, table preservation, and undo restoration. It also covers the raw-text fallback.

## 3. Verification status

The local sandbox has no Java runtime, so the new test and Gradle build were **not** run locally. `git diff --check` passed; source search confirms the selection/merge/window paths no longer derive editor offsets from `toPlainText()`, and Inky's remaining `parseResult.text` use is metadata-only. CI run `37259378329` passed on PR #32's head commit `987986a`: Unit Tests reported 350 tests across 64 suites with 0 failures, errors, or skips (including both `SelectionProjectionConsistencyTest` tests), and Build (SemVer & Nightly) passed. The PR remains open and unmerged.

## 4. Plan 7F boundary and delivery shape

Plan 7F remains one PR with two separately gated feature commits:

1. ODF tab leader import/model/layout/painting, preserving current tab advances; its tests pass before the next feature commit starts.
2. Body-level hidden-section layout with synthetic tests; the twelve-fixture page matrix remains unchanged.

It does not add conditional-section evaluation, sections contained inside table cells, DOCX `w:tabs` or `w:vanish`, hidden text runs, TOC regeneration, or UI editing. The selection-coordinate correction is not folded into Plan 7F.

## 5. PR status and forward forecast

| PR status / slot | Work |
|---|---|
| #31 | Plan 7E, merged |
| #32 — open; CI green | Separate shared editor-text projection / selection-coordinate correction (not a Plan) |
| #33 | Plan 7F, one PR with separate leader and hidden-section commits/gates |
| #34 | Plan 8A |
| #35 | Plan 8B |
| #36 | Plan 9 |
| #37-#38 | Plan 10 resume |
| #39-#43 | Plan 11 UI packages |

Plan IDs remain authoritative; #32 is now an actual open PR, while #33 onward are forecasts, not reservations. This reforecast supersedes the 2026-10-04 forward slots only; it does not alter the owners or scope of Plans 7E, 7F, or 8-11.

---

## 7. Merge closeout (2026-10-05)

PR #32 was subsequently merged as `466240e99aa30f24d016b800279d6a625d72454d`. Its PR-head run `37259378329` passed 350 tests across 64 suites and the merge workflow `37259905276` passed both Unit Tests and Build (SemVer & Nightly). The open/unmerged statements above describe the audit snapshot before merge and are retained as historical facts. The current forward sequence remains Plan 7F at forecast #33, then 8A #34 and onward.

---

## 6. Synchronized planning references

The current sequence and scope are synchronized in:

- `plan-01-master-index.md` §2 and its 2026-10-05 reforecast note;
- `plan-2026-09-24-remaining-pr-roadmap-v2.md` overview, §§4.7e-4.7f, §4.12, §5, and v2.9 note;
- `plan-04-to-09-writer-fidelity.md` current forecast, Plan 7E record, and sequence table;
- `plan-7f-2026-10-04-tab-stops-and-hidden-sections.md` forecast, commit gates, and explicit non-goals; and
- `plan-11-hybrid-experience-design.md` forward PR references.
