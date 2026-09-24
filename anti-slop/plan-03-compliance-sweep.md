# Plan 3 — Compliance Sweep

**Date:** 2026-09-24
**Status:** ready to start, parallel with Plan 2. No dependency on the fidelity plans; two items are gated by them and are marked.
**Evidence:** `anti-slop/audit-006-2026-09-24.md` §2 (project documents) and §3 (format specifications). Counts quoted there are reproducible greps.
**Rule of the plan:** fix against the documents, not against taste. Each item cites the clause it violates and is done when the cited grep count reaches zero or the cited spec behaviour is demonstrated by a test.

---

## 1. Localization (violates `AGENTS.md` "translate all strings to `en_US` and add to `strings.xml`")

| # | Item | Current | Target |
|---|---|---|---|
| 3.1 | `contentDescription` literals | 231 repo-wide, 72 in `modules/inky` | every one moves to `strings.xml`; a lint rule or a CI grep in the unit-test job fails on new ones |
| 3.2 | `Toast` literals | 316 toasts repo-wide, 57 in `InkyModule`, 49 with a literal | literals move to `strings.xml`; toasts that report a state change become M3 snackbars (see 3.5) |
| 3.3 | Indonesian copy in user-visible strings | `InkyModule.kt:200` default title `Draft Dokumen Baru`; `:3247/:3822` default file names `Inky_Dokumen.*`; `PapirusConfigManager.kt:382` reset message; `HomeDashboard.kt:1273` button label | en_US master in `values/`, Indonesian in `values-in/`; document *content* (style names like `Judul1`) stays untouched because `NavigatorStringCatalog` treats it as data, which is correct |

Not in scope: the Indonesian strings that exist because a *document* uses them (Navigator recognition). That behaviour is deliberate (P2-2) and stays.

## 2. Copy (violates `antislop` R-02, hard gate)

| # | Item | Fix |
|---|---|---|
| 3.4 | em dash in user-visible copy: `UniversalEmailSheet.kt:461` (Toast), `PapirusEmailEngine.kt:139,208` (log lines rendered in the email transcript), `LokitEngine.kt:29` (engine status shown in the Inky diagnostics list) | replace with comma, colon, or parentheses; keep the meaning. `strings.xml:160` is an XML comment and is documentation, not copy, so it may stay (note it in the PR body) |

## 3. Honesty and dead controls (violates `antislop` R-26, R-27; overstates `AGENTS.md` screen list)

| # | Item | Current | Fix |
|---|---|---|---|
| 3.5 | Four Toolbar Hub tools do nothing | "Add image" and "Add table" toast only (`InkyModule.kt:2695-2704`); link and comment are the same | either implement (Plans 6/7 own the insertion paths) or mark the control with a visible "Coming in a later build" state; a bare toast that says the tool was "selected" is the exact anti-pattern R-26 names |
| 3.6 | Six of eight ribbon tabs are empty | only `File` (`:3182`) and `Home` (`:3251`) have content | hide unimplemented tabs until their deck exists, **or** render an empty-state card ("This tab arrives with the Insert/Layout work") — user decision, see audit-006 §5.3 |
| 3.7 | Google Drive screen is a placeholder presented as a feature | `HomeDashboard.kt:1210-1276`; `AGENTS.md` and `PROJECT_CONTEXT.md` list it as an integrated screen | keep the honest placeholder, correct the wording in *both* documents to "placeholder, not yet implemented", and localize the button label |
| 3.8 | Zero `TODO`/`FIXME` in `app/src/main/java` | all stubs are invisible in source | every intentional stub gets a labelled marker so the next sweep finds them |

## 4. Design system (violates `DESIGN.md`)

| # | Item | Fix |
|---|---|---|
| 3.9 | 32 dp zoom targets (`InkyModule.kt:2439,2454`) | 48 dp touch target (visual size may stay smaller inside the box); `DESIGN.md:379` |
| 3.10 | Chrome greys outside the token set and below contrast floor | replace `Color.DarkGray` cell text (≈2.3:1), `Color.Gray` counters (≈3.9:1) and the `0.5.dp Color.LightGray` borders with `onSurfaceVariant` / `outlineVariant`; keep the *page* white/black (that is paper, not chrome) |
| 3.11 | Colour picker uses Material 2014 hues (`HomeSubpages.kt:1394-1398`) | acceptable as document colours; if it is presented as an app palette, re-derive from the module accents |
| 3.12 | `DESIGN.md` vs m3.material.io | review the four component families the screenshots exercise (bottom bar, FAB role, sheet deck, dialog header) against the current M3 guidance and record deviations in `DESIGN.md`. This is the design-language half of Plan 10 and can be done here as documentation only |

## 5. ODF / OOXML conformance (violates the specs in `docs/html` and ECMA-376)

These are recorded here so the sweep is complete; the code fixes belong to Plans 6, 7, 8 and 9.

| # | Clause | Violation | Owning plan |
|---|---|---|---|
| 3.13 | ODF Part 2 §4.16.9 | images written without a manifest entry | 9 |
| 3.14 | ODF Part 3 §5.3, §19.504-19.507, §19.803 | list styles never read; writer flattens every item into its own list | 7, 9 |
| 3.15 | ODF Part 3 §8.3, §8.2.2 | TOC and index content dropped; no tokens exist | 7 |
| 3.16 | ODF Part 3 §9.1.6, §17.16 | column widths ignored on read, `auto` on write | 7, 9 |
| 3.17 | ODF Part 3 §5.6 | `text:soft-page-break` treated as authored structure and injected into the editable text | 5 |
| 3.18 | ODF Part 3 §5.1.2 | heading spans dropped (`Heading` has no runs) | 7 |
| 3.19 | ODF Part 3 §3.16, §19.517 | dangling `text:style-name` references in generated content | 9 |
| 3.20 | ODF Part 3 §19.4xx, §3.1 | `fo:font-family` with a comma list, no `style:font-face`; `office:version="1.2"` while targeting 1.4 | 9 (`FontRegistry` in 5 supplies the families) |
| 3.21 | ECMA-376 §17.7.2, §17.3.2 | DOCX style hierarchy and run properties unread | 8 |
| 3.22 | ECMA-376 §17.9 | `numbering.xml` never read; writer hard-codes `w:numId=1` | 8, 9 |
| 3.23 | ECMA-376 §17.3.1.26 | `w:lastRenderedPageBreak` treated as a hard break | 5, 8 |
| 3.24 | ECMA-376 §17.4.1, §17.4.4x | per-section geometry unread; `w:tblGrid`/`w:vMerge` unread | 8 |
| 3.25 | ECMA-376 §17.16.5 | field instructions rendered as body text | 8 |
| 3.26 | ECMA-376 Part 2 (OPC) | regenerated `document.xml` references styles and numbering the copied parts need not define | 9 |
| 3.27 | ODF Part 3 §5.6, ECMA-376 §17.3.1.26 | **[gated by Plan 9]** a save currently writes `[Image: <device path>]` into DOCX, which leaks an absolute path into any shared file | 6 stops it, 9 removes the need |

## 6. Documentation accuracy

| # | Item | Fix |
|---|---|---|
| 3.28 | `AGENTS.md` screen list presents Google Drive as integrated | mark as placeholder (same PR as 3.7) |
| 3.29 | `CONCEPT.md` equation pipeline reads as shipped end-to-end | note the writer-side gap (MathML/OMML are produced by `EquationParser` but not yet embedded by either writer) |
| 3.30 | About screen credits a native engine | keep the credit, state the SIMULATED fallback in the same screen (the `LokitEngine.statusLabel` string already says so) |
| 3.31 | Nightly release body | the "Recent Changes" bullets are static; either generate them from the commits since the last nightly or label them as a standing feature summary |

---

## 7. Acceptance and evidence

* **Grep-verifiable:** 3.1, 3.2, 3.3, 3.4, 3.8 — the audit's own counts reach zero (or are reduced to the tolerated carve-outs named above).
* **Device-verifiable:** 3.5, 3.6, 3.9, 3.10 — checked at 320 dp width with the Delivery Gate report attached (`R-25`, `R-26`, `R-27`, `R-32`, `R-35`).
* **Spec-verifiable:** 3.13-3.27 — each closes with a unit test that asserts the parsed or written XML, not with prose.
* **Documentation:** 3.28-3.31 — diffs against the cited files.

**Size:** medium; 3.1-3.4 are mechanical and can land as one PR, 3.5-3.12 as a second (UI), 3.13-3.27 follow their owning plans, 3.28-3.31 as a docs-only PR.
