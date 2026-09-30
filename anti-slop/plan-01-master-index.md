# Plan 1 - Master Index and Working Guide

**Date:** 2026-09-24
**Role:** the entry point for all work described in `anti-slop/plan-*.md`. It says what each plan is, what it closes, where its detail lives, and how the reference guide behind the test checklist maps onto the plans.
**Status:** **active.** This document is the plan we are executing first; it is a living file, updated as plans land.

---

## 1. Where this work comes from

Two references define "correct" for the Inky module:

| Reference | What it is | Where it lives |
|---|---|---|
| **LibreOffice Writer Guide, Chapter 1 "Introducing Writer"** (WG 26.2 shelf, LibreOffice Documentation Team) | the behaviour Papirus Office is reproducing: the window, the sidebar decks, the toolbars, the status bar, saving and opening, Go to Page, the Navigator, outline folding, reminders, undo/redo, reload and close | `https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html`; local copy `docs/lo-guides/WG262-WriterGuide_compressed.pdf`, Chapter 1 on PDF pages 17 to 40, Chapter 2 beginning on page 41 (`audit-011` §8). Structure reproduced in §3 below |
| **`docs/InkyC1Checklist.md`** | the project's own pass/fail tests for Chapter 1, in the user's words | in tree |

**Edition note.** This index was written against WG 24.8 and re-pinned to 26.2 during Plan 1 (2026-09-28), which is the edition `AGENTS.md` and the `DESIGN.md` front matter already cite. The re-map is a citation and ownership update, not a scope change: the chapter's section list used below was checked against the 26.2 PDF, and the differences between 24.8 and 26.2 that matter to Papirus are recorded where a row changed.

The document format standards are separate and equally binding: ODF 1.4 Parts 1 to 4 in `docs/odf`, ECMA-376 / Open XML SDK for OOXML (links in `CONCEPT.md`). Where the guide describes a desktop affordance that has no mobile analogue (docked toolbars, floating windows, menu-bar nesting), the project's answer is in `AGENTS.md` (Toolbar Hub, Standard Bottom Sheet, FCT) and `DESIGN.md` (M3 Expressive tokens).

**Provenance note (why the sweep is mechanical).** Most of this repository was generated with Google AI Studio (`gemini-3.1-pro` and the `gemini-3.x-flash` variants), and `audit-006` shows the expected signature: hard-coded UI copy, controls that only toast, documentation that describes features the code does not have, and structural parsers that look complete but skip the hard parts (numbering, tables, TOC). That is not a reason to distrust the code line by line; it is a reason to make the guards **automatable**. Every plan below therefore closes with a check that a machine can run (a grep count, an XML assertion, a page-count window), not with a claim.

---

## 2. The registry

| # | Plan | Closes | Depends on | Detail | Status |
|---|---|---|---|---|---|
| **1** | **Master index and working guide** (this file) | - | - | here | **active** |
| **2** | **Screenshots and UI backlog** | F-01…F-06, F-22, F-29, F-30, F-31 | - | `plan-02-screenshots-and-ui-backlog.md` | **landed as PR #11** (merge `e10f956`); acceptance list still open on device |
| **3** | **Compliance sweep** | the `AGENTS.md` / `DESIGN.md` / `antislop` / ODF / OOXML findings in `audit-006` §2-§3 | - | `plan-03-compliance-sweep.md` (§8 records 3A; §9 records 3B) | 3A **landed as PR 12** (`55a9a97`); 3B **complete** per user confirmation and its implementation record; 3C is the docs alignment in PR 14 / Plan 11 |
| **4** | **Chrome and input** (was PR D) | F-01…F-06 | - | `plan-04-to-09-writer-fidelity.md` § Plan 4 | **consumed by Plan 2 / PR #11** (same scope; keep as design record, do not re-execute) |
| **5** | **Layout metrics and pagination** (was PR E) | page-count half of finding 6, F-10, F-21, F-24, F-25, F-28 | - | same file, § Plan 5; evidence `audit-007`, `audit-008`, `audit-009`, `audit-010`, `audit-011`; implementation record `plan-5e-progress.md` | **5a to 5e all landed**: 5a **PR #15**, 5b **PR #16**, 5c **PR #17**, 5d **PR #18**, 5e **PR #19** (merge `439ed05`, 2026-09-28). All twelve fixtures sit inside their per-format windows under the table advance backend; Plan 10 will move counts again when real Typefaces load |
| **6** | **Image pipeline and load performance** (was PR F) | F-07, F-18, the image half of save integrity | Plan 5 | same file, § Plan 6 | **6a to 6d all landed (COMPLETE)**: 6A **PR #22** (`eff150d`, run `36578390234`, 252 tests), 6B **PR #23** (`ac713e4`, runs `36587725933`/`36588655662`, 260 tests), 6C **PR #24** (`bdd2724`, run `36708650093`, 275 tests), 6D **PR #25** (closeout gate, 276 tests; dead `DocxDocumentParser` plumbing removed, `LayoutEngine` unified on `DocumentImages.box`, `PaginationImageTest` added, 12-file matrix re-confirmed at ODT `15/23/18/10/18/19` and DOCX `15/25/21/11/19/23`) |
| **7** | **ODF structural fidelity** (was PR G) | F-08, F-09, F-11…F-15, F-23, O-03…O-05 | Plan 5 | same file, § Plan 7 | **next active plan**: reforecasted as **PR #26 (7A)** and **PR #27 (7B)** |
| **8** | **OOXML structural fidelity** (was PR H) | F-16…F-20, F-26, F-27, DOCX halves of F-10/F-11 | Plans 5, 7 | same file, § Plan 8 | after Plan 7: reforecasted as **PR #28 (8A)** and **PR #29 (8B)** |
| **9** | **Save round-trip integrity** (was PR I) | O-01, the "non-destructive package preservation" rule, the writer findings in `audit-006` §3 | Plans 6, 7, 8 | same file, § Plan 9 | after Plans 6-8: reforecasted as **PR #30**; its own pre-change gate first, and its scope is firmed by a short plan document before it starts |
| **10** | **Font engine + UI design language** | old PR D (bundled Typeface and substitution) and old PR E (Font Style UI, SAF/user fonts, curated Google Fonts), plus the `DESIGN.md` / m3.material.io review | Plans 5, 7, 8 | `plan-10-font-engine-and-design-language.md` | document-font thread stays parked until Plans 7 and 8 converge (reforecast **PR #31-#32**); when real Typefaces load, pagination is re-measured and windows tighten only with measured support (`audit-011` §8). Its UI-design thread is re-scoped by Plan 11 (B1's doc alignment landed in PR #14) |
| **11** | **Hybrid experience design** | User's source-map decisions, Material 3 Expressive, Writer Guide Chapter 1 and the six ODT/DOCX fixture pairs | docs now; UI packages coordinated with Plans 5 to 10 | `plan-11-hybrid-experience-design.md` | PR #14 documentation alignment landed; implementation packages follow their own gates, reforecast at **PR #33-#37** after Plan 9 (`#30`) and Plan 10 (`#31-#32`). Plan 11 owns the physical-device Chapter 1 pass, and the owner has deferred that pass until Plan 11 completes |

Plans 2 and 3 are the ones the user asked to start with. Plans 4-9 keep the letters D-I in parentheses and in sub-item IDs (`D-1`, `E-EN-2`, `G-1` …), which read as `plan-n item`: D = 4, E = 5, F = 6, G = 7, H = 8, I = 9.

**Audits consumed:** `audit-005-2026-09-24.md` (F-01…F-20 from the written report), `audit-006-2026-09-24.md` (F-21…F-31 from the screenshots + the compliance sweep), `audit-003-2026-09-22.md` (pre-A/B/C backlog), `plan-2026-09-22-remaining-writer-fixes.md` (the merged A/B/C plan). Added 2026-09-26: `audit-007-2026-09-26-sample-matrix.md` (the twelve-file measurement behind Plan 5's split and its per-format windows; it also withdraws the "~60 KB stub" description of `liblo-native-code.so`, which is an LFS pointer to a 196 MB arm64 build). Added 2026-09-27: `audit-008` (fixture and blank re-baseline), `audit-009` (Plan 5c documentation refresh), `audit-010` (Plan 5d strategy). Added 2026-09-28: `audit-011` (Plan 5e readiness and the remaining-plan order; §9 lists the reference corrections this file applies) and `audit-012` (this synchronization: the verified 5e state and the 33-item drift inventory, IDs `P1-01` to `P1-33`).

**Numbering note (2026-09-28).** The PR numbers predicted for Plans 6 to 9 in earlier revisions of this table (17 to 22) are void: PRs #17, #18 and #19 are 5c, 5d and 5e, and Plan 1's own two documentation PRs took #20 (1A, the ledger) and #21 (1B, the product-docs pass). **The record keys on plan IDs; the PR numbers in the Status column are expectations, not reservations.** If a plan splits, merges or is preceded by unplanned work, the plan ID stays and the number moves. `audit-010` §6 still numbers its own chapters against the old schedule and is superseded on that point only.

---

## 3. Writer Guide Chapter 1 mapped onto the plans

Every section of WG 26.2 Chapter 1 is listed below with what it means for Papirus, which plan owns the work, and what the code does today. Sections marked **guard** are already implemented and must simply keep working; their tests live in `docs/InkyC1Checklist.md` and in the plan's regression gate.

### 3.1 The window

| WG Chapter 1 section | Papirus surface | Plan | State today |
|---|---|---|---|
| Title bar | Inky top app bar (document name, Saved/Modified) | 4 | present; shares the bar with search, overflow, undo/redo, save |
| Menu bar (commands, dialogs, submenus) | ribbon tab deck + overflow menu; dialogs are full-page per `CONCEPT.md` | 3, 10 | PR 13 rebuilt the tab set to `CONCEPT.md`'s Writer list (File, Home, Insert, Layout, Review, View; References/Mailings dropped): 2 tabs have decks, the other 4 render disabled with an honest reason (`InkyModule.kt` ribbon region, pre-13 lines `:2936`, `:3383-3390`); dialog headers not yet uniform |
| Sidebar decks | Standard Bottom Sheet decks + FCT Expanded + Toolbar Hub | 2, 3, 7, 8 | Navigator deck exists; PR 13 added the missing **Hyperlinks** category and split its empty states into "no X in this document" (classes a parser produces) and "not yet available" (classes no parser produces yet); Page (layout) and Style Inspector have no counterpart; Manage Changes (Review) has no deck; Find exists as Find & Replace |
| Toolbars: Standard and Formatting, context-sensitive, hide/show, docking, customization | Toolbar Hub (scrollable, persistent trailing actions) | 4 (context switching), 10 (customization) | one static hub; context-sensitive toolbars for table/image are documented in `CONCEPT.md` and `AGENTS.md` but absent, because nothing detects the context yet (Plans 7/8 supply it) |
| Rulers | not present; `AGENTS.md` lists margins/rulers under Inky View Settings | 5 (geometry), 4 (drawing) | `PageStyleSpec` carries margins; no ruler UI, and no margins editing UI |
| Status bar (page, word count, language, insert mode, selection mode, modified, signature, view layout, zoom) | the unified bottom bar from P1-1 | 4, 3 | page + words/chars + zoom present; PR 13 added the WG "section or object information" slot (heading level+text, Table, List item; row/column and section names follow 19/21). Language, insert mode, selection mode, signature and view layout are absent. Chapter 1 fidelity needs page + count + zoom only; the rest is a later decision |
| Context (right-click) menus | FCT Expanded (long-press) | 2, 3 | FCT Expanded exists; several of its entries are stubs (`audit-006` §3, item 3.5), and in Viewer the FCT never appears at all (F-05) |
| Dialogs | full-page dialogs with `← Back | Title`, `Apply/OK | ⋮` | 10, 3 | structure exists for several dialogs; not uniform |
| Document views (Web / Full screen variants) | Viewer mode and Editor mode on one paginated pipeline | 5 (**done**), 11 (affordance) | **retired, not re-implemented.** `InkyModule.kt:124` holds `isWebView = false` and the editor's mobile-view button raises `R.string.inky_flow_view_unavailable` ("Flow view is not available yet. Use paginated view."), so the second `BasicTextField` renderer is unreachable. This closes the dual-path defect class that F-2 named. The button itself is now a control whose only behaviour is a toast, which is an R-26 dead control: recorded as a Plan 11 editor-package item, not described as a feature |

### 3.2 Documents

| WG Chapter 1 section | Papirus surface | Plan | State today |
|---|---|---|---|
| Starting a new document (Start Center, from a template) | New Document screen, template picker | 3 | the template flow injects a hard-coded English resume text as "template content" (`InkyModule.kt` ~`:1190`), which is fabricated content rather than a real template |
| Opening an existing document | Start Center → Recents / Files, SAF | 6 | works, but images arrive late or never (F-07) |
| Opening files not in `.odt` format | DOCX support (and ODS/ODP/XLSX/PPTX in the other modules) | 8, 7 | DOCX opens; fidelity is the largest gap in the whole report |
| Saving a document: Save, Save As, Save a copy, Save all, Save to remote, autosave | top-bar save, Save As dialog, autosave timer | 9 | save exists; the written content loses structure (dangling style refs on ODT, `[Image: path]` on DOCX) |
| Saving as a Microsoft Word document | `DocumentSerializer.serializeToFormat(..., "DOCX")` | 8, 9 | writes a document.xml without styles, numbering, table grid or images |
| Exchanging with Apple Pages | - | - | out of scope; recorded here so it is a decision, not an omission |
| Password protection, OpenPGP encryption, remote servers (Google Drive, WebDAV, FTP, CMIS) | Google Drive tab is a labelled placeholder | 3 (honesty), unassigned (feature) | no encryption path; `AGENTS.md` lists Google Drive as a screen, which the docs will correct in Plan 3 |
| Reloading a document (discard changes after last save) | Reload action + confirmation dialog | 9, guard | present per the checklist |
| Closing a document (save-or-discard prompt) | back/close handler + Save before Exit dialog | 9, guard | present per the checklist |

### 3.3 Moving through a document

| WG Chapter 1 section | Papirus surface | Plan | State today |
|---|---|---|---|
| Go to Page (status-bar field, Ctrl+G) | tap the page field in the bottom bar → Go to Page dialog | 4 (bar, **done**), 5 (accuracy, **done**) | the bar is the unified 48 dp status bar from PR #11, and page counts now land inside their recorded windows for all twelve fixtures (worst gap: Sample-3 ODT at 18 pages against a reference of 22). The old "65/88 against 21" observation describes the pre-5a engine and is withdrawn. Sample-3 is the pair Plans 7 and 8 must explain with structure, not with a widened window |
| Using the Navigator (categories, Navigate By, double-click to jump, content navigation view, heading level filter) | Navigator deck + Navigate By deck | 4 (categories), 7/8 (content), 5 (page index) | 12 categories exist as rows; the ones a parser can fill (headings, tables, images, pages) list and jump, the rest render "not yet available" from PR 13 (verified: no parser constructs the fetch/section/bookmark/comment/footnote/shape/OLE classes); the Hyperlinks category is added there too, its filter option lands with its data in 18/20 |
| Using outline folding (options toggle, hide/show content under headings, include sub-levels) | double-tap a heading toggles (`OutlineEngine`) | 3 (discoverability), guard | implemented, but the affordance is invisible: the guide gives it a setting and a visible button, Papirus gives it an undocumented double-tap |
| Setting reminders (up to five; the sixth deletes the first; not saved with the document) | Set Reminder in FCT, Reminder filter in the Navigator, Prev/Next | 2, guard | the checklist already tests the cap; the guide's "not saved with the document" matches `ReminderManager` behaviour to confirm in Plan 2 |
| Undoing and redoing changes (undo list, multi-step undo, redo list) | top-bar undo/redo, Actions to Undo/Redo subpage, dual-stack `HistoryManager` | guard | implemented; Plans 4-5 must not disturb the buffer-flush protocol |
| Displaying multiple views of a document | no mobile analogue (the guide's Window > New Window) | - | **explicit exclusion.** The `isWebView` toggle was the nearest analogue and Plan 5 retired it (see the document-views row above). Cellina and Slidia still carry a live `isWebView` toggle; Inky does not. Reopening this would mean a second renderer, which §5 invariant 1 forbids |

### 3.4 What this mapping means for the plans

* **Nothing in Chapter 1 is unassigned.** Two items are explicitly parked rather than fixed: encryption/remote storage (no plan, by decision) and the multiple-window view (no mobile analogue, and the toggle that stood in for it was retired in Plan 5e).
* **Three Chapter 1 items are owned by no *fidelity* plan and are easy to forget**, so they are written down here: outline-folding discoverability (Plan 3, still open), the fake template content (Plan 3, still open), and the `isWebView` second renderer (**closed in Plan 5e**: retired, not re-implemented; the remaining piece is the dead mobile-view button, owned by Plan 11).
* **Plans 7 and 8 are Chapter 1 requirements too**, not just format work: without numbering, tables and the TOC, the Navigator's categories and the status bar's page count cannot be right, and `docs/InkyC1Checklist.md` items 6, 7 and 10 cannot pass.

---

## 4. `docs/InkyC1Checklist.md` mapped onto the plans

The checklist is the acceptance suite. Each of its twelve sections needs a different plan before it can pass, and the ordering below is why Plan 2 runs first: five of the twelve sections are gated by UI or input defects that Plan 2 removes.

| # | Checklist section | Needs | Notes |
|---|---|---|---|
| 1 | Document Lifecycle (new/save/close/open, reload yes/no, save-before-exit) | Plan 9, Plan 4 (dialogs) | currently the save itself is the weak link, not the dialogs |
| 2 | Editing Engine, stages 1-2 ("Layout Engine does not rebuild the entire document") | Plan 5 (**done**), guard | incremental layout survived the metric change; `FragmentInputTest` and the 100+ page incremental/full-rebuild test are the guards. Still device-unverified, like every row in this table |
| 3 | Multiple Undo (`abcde` → undo → redo) | guard | implemented and undisturbed: 5e routing input through one field per logical paragraph left the global edit value, the undo callbacks and the IME composition unchanged |
| 4 | Caret (`Home`, `End`, `Ctrl+↑/↓`, `↑`, `↓`) | Plan 5 (**done**), guard | caret advances are now measured per line (`LineLayout.caretOffsets` / `caretAdvances`) instead of derived from a character count. `ParagraphMetricsTest` covers measured caret advances (`hitTestSupportsDirectParagraphsAndMeasuredCaretAdvances`) and positional tabs (`tabsAdvanceFromCurrentPositionAndClearSkipsDefaultStop`); grapheme-boundary handling is implemented at `ParagraphMeasurer.kt:33,108` but has no dedicated test yet, and no row here has been run on hardware |
| 5 | Selection, stages 1-2 (FCT + ribbon undo interplay) | Plan 2 | stage 2 starts with "tap and hold to open FCT: Compact", which F-05 blocks in Viewer |
| 6 | Go To (20-page document, jump, out-of-range toast) | Plan 4 (**done**), Plan 5 (**done**) | page counts are now inside their recorded windows for all twelve fixtures, so the target page is believable; the dialog and the out-of-range toast are unchanged |
| 7 | Navigator (all categories, correct order, jump to exact location) | Plan 4, Plans 7/8, Plan 5 | Images and Hyperlinks categories must exist before the objects can be listed |
| 8 | Reminder (five, sixth deletes first, Prev/Next) | Plan 2, guard | the Navigator Reminder filter is part of the Plan 2 deck work |
| 9 | Zoom (50/100/150/200/300 with scroll, edit, select, FCT, undo, reload at each) | Plan 2 (**done**), Plan 5 (**done**) | `PageTransform` (PR #15) owns one `pageScale` for card, margins, gap, text, images and taps; the "hard 320 dp card" description is pre-5a and withdrawn. Papirus treats fit-to-width as its 100 % mobile convention, which is a project decision rather than a universal Writer definition (`audit-011` §8) |
| 10 | Save Compatibility (contents, headings, paragraphs, bold/italic/underline/strikethrough, alignment) | Plan 9, Plans 7/8 | text survives today; headings, numbering and tables do not |
| 11 | Session Restore (page 15, 170 %, scroll, caret) | Plan 5, Plan 2 | zoom semantics changed in Plan 2 and the transform is now single-source; stored sessions must still be shown to migrate rather than break, which needs a device run |
| 12 | Stress test (100+ pages: scroll, edit, undo/redo, reload, close, save) | Plan 5 (**done**), Plan 6 (**done**), `docs/PHASE6_MEMORY_PLAN.md` | pagination cost is covered by an automated 100+ page test with cached and full-rebuild equality, and media memory/recovery is covered by `DocumentMediaStoreTest` (128 MiB per-doc / 256 MiB global LRU caps). Timing lives in the JUnit artifact and is not a device performance claim |

**Sequencing consequence.** Plan 2 landed first (it unblocked checklist items 5, 8, 9 and the dialogs of 1), Plan 3 ran beside it, and Plan 5 landed before any fidelity plan because every fidelity acceptance is expressed as a page count. **As of 2026-09-28, no section of this checklist has been run on physical hardware.** Items 1 and 10 wait on Plans 9, 7 and 8; item 7 waits on Plans 7 and 8 for parser-backed categories; items 5, 8, 9, 11 and 12 are code-complete and await the owner's post-Plan-11 device pass. That deferral is the owner's decision and does not change any automated window or source-integrity assertion.

---

## 5. Cross-plan invariants

Every plan's PR must hold these, or it is not ready:

1. **One source of truth per decision:** one paginator (`LayoutEngine`), one measurement backend (`TextMetrics`, injected), one unit system (`LayoutUnits`), one style resolver (`StyleResolver` in `LayoutEngine.kt` + `OfficeRuns`), one numbering model, one media store. A plan that adds a second path deletes the first in the same PR. **Plan 5e applied this twice:** the second `BasicTextField` renderer behind `isWebView` was retired rather than aligned, and raw Paint sizing was replaced rather than kept as a fallback. `OfficeDocElement` retirement (F-2) remains Plan 9's to finish. Note that `TextLayoutManager` is not a second paginator: its only production caller is the debug formatting inspector (`InkyModule.kt:3897`).
2. **en_US + `strings.xml`:** no new hard-coded copy, no new `contentDescription` literal (`audit-006` §2.1 counts 231 and 72), and no Toast where the guide would show a dialog or a snackbar.
3. **48 dp touch targets** (`DESIGN.md:379`) and token colours only (`DESIGN.md:384`), verified at 320 dp width.
4. **No em dash** in user-visible strings (`antislop` R-02).
5. **Evidence, not claims:** each PR body lists the findings it closes, the suite it ran, and, for UI, the Delivery Gate with device evidence. CI is the only build evidence available in this repository's automation (no local JDK in the sandbox); the owner's physical-device pass is the only source of visual, gesture and IME evidence, and it is scheduled after Plan 11.
6. **The sample matrix is the floor:** the 12 files in `tests/inky` (Sample-1…6, ODT + DOCX) must stay green through Plans 6 to 11, with byte identity held by `tests/inky/fixture-identities.properties` and `FixtureIdentityTest`. Re-saving a fixture is a deliberate act that updates the manifest, `SampleMatrix` references and the recorded windows in the same PR, never a side effect.

---

## 6. Working agreement for Plan 1

* **Deliverable:** this file. Its acceptance is completeness: every Chapter 1 section has an owner (§3, including the three items no fidelity plan owns), every checklist section is mapped to the plan that makes it pass (§4), and every finding has a home. The `Closes` column in §2 covers the full set: F-01…F-06 → 2/4, F-07 → 6, F-08/F-09 → 7 and 10, F-10/F-21/F-24/F-25/F-28 → 5, F-11…F-15/F-23 → 7, F-16…F-20/F-26/F-27 → 8, F-22/F-29/F-30/F-31 → 2, O-01 → 9, O-02 → 5 and 8, O-03…O-05 → 7.
* **Execution schedule:** the PR-by-PR order for everything above lives in `anti-slop/plan-2026-09-24-remaining-pr-roadmap-v2.md` (baseline `55a9a97`; v1 at `plan-2026-09-24-remaining-pr-roadmap.md` is the pre-PR-12 record), which also records the 2026-09-24 decisions (staged page windows, display-the-bundled-faces, TOC snapshot, save refusal). This file keeps the mapping and the numbers; the roadmap keeps the order. Its §4.12 PR table is re-numbered by the 2026-09-28 synchronization, because PRs #17 to #19 became 5c, 5d and 5e.

* **Update rule:** when a plan lands, update its status here and add one line to the plan's own file recording what actually shipped versus what was written. Numbers in this file are the ones the other documents cite, so corrections happen here first.
* **Next actions after this file:**
  1. ~~Plan 2, commits 1-2~~ **done**: PR #11 merged the whole plan (its commits 1-2 became the renderer and status-bar commits of PR #11).
  2. ~~Plan 3A/3B~~ **complete**: PR 12 landed with the sweep and `SourceHygieneGuardTest`; Plan 3B is complete per its implementation record and the user's confirmation. Plan 3C is the current docs alignment (PR 14 / Plan 11).
  3. ~~Then Plan 5 with its per-page element dump~~ **scheduled as roadmap PR 15** (dump first, metrics seams second, transform third), exactly because the empty-page mechanism (F-25) is still an open question that only a trace can answer.
  4. **2026-09-26:** PR 14 (Plan 3C / Plan 11 docs) is merged (`5c99072`). Plan 5 starts with the evidence commit of PR 15 (`audit-007`); the empty-page question (F-25) is narrowed by code reading (the paginator cannot author a blank page except at element 0), so the dump confirms per file whether the blank pages come from the fake breaks or from inflated metrics. Plan 5 is renumbered into 5a-5e (audit-008).
  5. **2026-09-27:** Plan 5a (PR #15 measuring stick), Plan 5b (PR #16 fixture re-baseline with M365 DOCX and Collabora 26.04 ODT, audit-008), and Plan 5c (PR #17 documentation refresh, DESIGN.md v3.0, audit-009) merged. Plan 5d (breaks and defaults: fake breaks out, authored breaks in, body rect, metric style chain, F-21 defaults; audit-010) is active next.
  6. **2026-09-28, Plan 5 complete and Plan 1 synchronization.** Plan 5d merged as **PR #18** and Plan 5e as **PR #19** (merge `439ed05`). Plan 5e was executed in five batches and closed with CI run `36377627605`: 247 tests, zero failures, errors or skips, and all twelve fixtures inside their recorded windows under the table advance backend (measured ODT 15/23/18/10/18/19, DOCX 15/24/21/11/19/22 against references 15/23/22/11/19/22 and 15/23/22/10/18/21). Implementation record: `anti-slop/plan-5e-progress.md`. Owner decision recorded there and in `audit-011` §8: real-device visual, gesture and IME testing is deferred until after Plan 11, and that deferral changes no automated fixture window or source-integrity assertion.
  7. **2026-09-28, this file.** Plan 1 runs as two PRs. **1A** (this file, the roadmap v2 §4.12 table, the Plan 5 records in `plan-04-to-09-writer-fidelity.md`, the `plan-5e-progress.md` status and the `plan-11` edition pin) synchronizes the ledger: the 5a to 5e status, the void PR 17 to 22 predictions, the 26.2 chapter re-map, and the retired flow view. **1B** applies the product-documentation truth pass to `AGENTS.md`, `DESIGN.md`, `PROJECT_CONTEXT.md` and `CONCEPT.md`: `docs/html` to `docs/odf`, guide PDFs to `docs/lo-guides/`, the blank-package description, and the engine description. Drift inventory with 33 measured items and per-file ownership: `anti-slop/audit-012-2026-09-28-plan-1-synchronization.md`.
  8. **Plan 6 complete (2026-09-30).** Plan 6 shipped as four increments: **6A PR #22** (`eff150d`, image extents + fail-safe image saves, 252 unit tests in run `36578390234`), **6B PR #23** (`ac713e4`, durable `filesDir/media` store with LRU caps and self-heal, 260 unit tests in runs `36587725933`/`36588655662`), **6C PR #24** (`bdd2724`, `DocumentImages` sizing, `ImagePredecoder`, real `LoadingStage` progress, 275 unit tests in run `36708650093`), and **6D PR #25** (closeout gate: dead `DocxDocumentParser.parseDocxFile`/`parseOdtFile`/`imageExtractor`/`[Image: …]` removed, `LayoutEngine` unified on `DocumentImages.box`, `PaginationImageTest` added, per-suite JUnit timings and `Plan6cLoadingProgressTest` stdout added to the CI PR comment, 276 unit tests). The 12-file matrix is re-confirmed at ODT `15/23/18/10/18/19` and DOCX `15/25/21/11/19/23`, all inside their windows with zero empty pages; the two DOCX shifts versus 5E (Sample-2 24→25, Sample-6 22→23) come from now-parsed `wp:extent` image boxes, not from a window change. The sandbox itself has no Java runtime. **Reforecast PR slots (2026-09-30):** Plan 7A `#26`, 7B `#27`, 8A `#28`, 8B `#29`, 9 `#30`, Plan 10 `#31`-`#32`, Plan 11 `#33`-`#37`; plan IDs, not predicted PR numbers, are authoritative. Plan 7A is next. The owner's physical-device Chapter 1 pass remains after Plan 11, followed by Chapter 2 (`docs/lo-guides/WG262-WriterGuide_compressed.pdf`, PDF page 41).
