# Plan 1 — Master Index and Working Guide

**Date:** 2026-09-24
**Role:** the entry point for all work described in `anti-slop/plan-*.md`. It says what each plan is, what it closes, where its detail lives, and how the reference guide behind the test checklist maps onto the plans.
**Status:** **active.** This document is the plan we are executing first; it is a living file, updated as plans land.

---

## 1. Where this work comes from

Two references define "correct" for the Inky module:

| Reference | What it is | Where it lives |
|---|---|---|
| **LibreOffice Writer Guide, Chapter 1 "Introducing Writer"** (WG 24.8, November 2024, LibreOffice Documentation Team) | the behaviour Papirus Office is reproducing: the window, the sidebar decks, the toolbars, the status bar, saving and opening, Go to Page, the Navigator, outline folding, reminders, undo/redo, reload and close | `https://books.libreoffice.org/en/WG248/WG24801-IntroducingWriter.html` (structure reproduced in §3 below) |
| **`docs/InkyC1Checklist.md`** | the project's own pass/fail tests for Chapter 1, in the user's words | in tree |

The document format standards are separate and equally binding: ODF 1.4 Parts 1-4 in `docs/html`, ECMA-376 / Open XML SDK for OOXML (links in `CONCEPT.md`). Where the guide describes a desktop affordance that has no mobile analogue (docked toolbars, floating windows, menu-bar nesting), the project's answer is in `AGENTS.md` (Toolbar Hub, Standard Bottom Sheet, FCT) and `DESIGN.md` (M3 Expressive tokens).

**Provenance note (why the sweep is mechanical).** Most of this repository was generated with Google AI Studio (`gemini-3.1-pro` and the `gemini-3.x-flash` variants), and `audit-006` shows the expected signature: hard-coded UI copy, controls that only toast, documentation that describes features the code does not have, and structural parsers that look complete but skip the hard parts (numbering, tables, TOC). That is not a reason to distrust the code line by line; it is a reason to make the guards **automatable**. Every plan below therefore closes with a check that a machine can run (a grep count, an XML assertion, a page-count window), not with a claim.

---

## 2. The registry

| # | Plan | Closes | Depends on | Detail | Status |
|---|---|---|---|---|---|
| **1** | **Master index and working guide** (this file) | — | — | here | **active** |
| **2** | **Screenshots and UI backlog** | F-01…F-06, F-22, F-29, F-30, F-31 | — | `plan-02-screenshots-and-ui-backlog.md` | **landed as PR #11** (merge `e10f956`); acceptance list still open on device |
| **3** | **Compliance sweep** | the `AGENTS.md` / `DESIGN.md` / `antislop` / ODF / OOXML findings in `audit-006` §2-§3 | — | `plan-03-compliance-sweep.md` (§8 records 3A; §9 records 3B) | 3A **landed as PR 12** (`55a9a97`); 3B **complete** per user confirmation and its implementation record; 3C is the docs alignment in PR 14 / Plan 11 |
| **4** | **Chrome and input** (was PR D) | F-01…F-06 | — | `plan-04-to-09-writer-fidelity.md` § Plan 4 | **consumed by Plan 2 / PR #11** (same scope; keep as design record, do not re-execute) |
| **5** | **Layout metrics and pagination** (was PR E) | page-count half of finding 6, F-10, F-21, F-24, F-25, F-28 | — | same file, § Plan 5; evidence `audit-007-2026-09-26-sample-matrix.md` (§12.1 = PR 15 baseline dump) | **5A in review as PR #15** (`LayoutUnits`, style/page seam, `FontRegistry` + Martel Sans, `TextMetrics`, `LayoutDump`, `PageTransform`, `SampleMatrixTest`, CI inventory + PR report comment); **5B = PR 16a, 5C = PR 16b** next; windows per format (user decision 2026-09-26) |
| **6** | **Image pipeline and load performance** (was PR F) | F-07, F-18, the image half of save integrity | Plan 5 | same file, § Plan 6 | scheduled as PR 17 |
| **7** | **ODF structural fidelity** (was PR G) | F-08, F-09, F-11…F-15, F-23, O-03…O-05 | Plan 5 | same file, § Plan 7 | scheduled as PRs 18-19 |
| **8** | **OOXML structural fidelity** (was PR H) | F-16…F-20, F-26, F-27, DOCX halves of F-10/F-11 | Plans 5, 7 | same file, § Plan 8 | scheduled as PRs 20-21 |
| **9** | **Save round-trip integrity** (was PR I) | O-01, the "non-destructive package preservation" rule, the writer findings in `audit-006` §3 | Plans 6, 7, 8 | same file, § Plan 9 | scheduled as PR 22 (its own pre-change gate first) |
| **10** | **Font engine + UI design language** | old PR D (bundled Typeface and substitution) and old PR E (Font Style UI, SAF/user fonts, curated Google Fonts), plus the `DESIGN.md` / m3.material.io review | Plans 5, 7, 8 | `plan-10-font-engine-and-design-language.md` | document-font thread remains parked pending fidelity; its UI-design thread is re-scoped by Plan 11 (B1's doc alignment is PR 14) |
| **11** | **Hybrid experience design** | User's source-map decisions, Material 3 Expressive, Writer Guide Chapter 1 and the six ODT/DOCX fixture pairs | docs now; UI packages coordinated with Plans 5–10 | `plan-11-hybrid-experience-design.md` | PR 14 documentation alignment now; implementation packages follow their gates without renumbering PR 15–22 fidelity work |

Plans 2 and 3 are the ones the user asked to start with. Plans 4-9 keep the letters D-I in parentheses and in sub-item IDs (`D-1`, `E-EN-2`, `G-1` …), which read as `plan-n item`: D = 4, E = 5, F = 6, G = 7, H = 8, I = 9.

**Audits consumed:** `audit-005-2026-09-24.md` (F-01…F-20 from the written report), `audit-006-2026-09-24.md` (F-21…F-31 from the screenshots + the compliance sweep), `audit-003-2026-09-22.md` (pre-A/B/C backlog), `plan-2026-09-22-remaining-writer-fixes.md` (the merged A/B/C plan). Added 2026-09-26: `audit-007-2026-09-26-sample-matrix.md` (the twelve-file measurement behind Plan 5's split and its per-format windows; it also withdraws the "~60 KB stub" description of `liblo-native-code.so`, which is an LFS pointer to a 196 MB arm64 build).

---

## 3. Writer Guide Chapter 1 mapped onto the plans

Every section of WG 24.8 Chapter 1 is listed below with what it means for Papirus, which plan owns the work, and what the code does today. Sections marked **guard** are already implemented and must simply keep working; their tests live in `docs/InkyC1Checklist.md` and in the plan's regression gate.

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
| Document views (Web / Full screen variants) | Viewer mode, Editor mode, and the `isWebView` toggle | 5, 4 | the Web View path is a **second renderer** (`InkyModule.kt:2292-2360`, a plain `BasicTextField`) that bypasses the layout engine. It is the same dual-path defect class as F-2, and Plan 5 must either align it with the paginator or retire it |

### 3.2 Documents

| WG Chapter 1 section | Papirus surface | Plan | State today |
|---|---|---|---|
| Starting a new document (Start Center, from a template) | New Document screen, template picker | 3 | the template flow injects a hard-coded English resume text as "template content" (`InkyModule.kt` ~`:1190`), which is fabricated content rather than a real template |
| Opening an existing document | Start Center → Recents / Files, SAF | 6 | works, but images arrive late or never (F-07) |
| Opening files not in `.odt` format | DOCX support (and ODS/ODP/XLSX/PPTX in the other modules) | 8, 7 | DOCX opens; fidelity is the largest gap in the whole report |
| Saving a document: Save, Save As, Save a copy, Save all, Save to remote, autosave | top-bar save, Save As dialog, autosave timer | 9 | save exists; the written content loses structure (dangling style refs on ODT, `[Image: path]` on DOCX) |
| Saving as a Microsoft Word document | `DocumentSerializer.serializeToFormat(..., "DOCX")` | 8, 9 | writes a document.xml without styles, numbering, table grid or images |
| Exchanging with Apple Pages | — | — | out of scope; recorded here so it is a decision, not an omission |
| Password protection, OpenPGP encryption, remote servers (Google Drive, WebDAV, FTP, CMIS) | Google Drive tab is a labelled placeholder | 3 (honesty), unassigned (feature) | no encryption path; `AGENTS.md` lists Google Drive as a screen, which the docs will correct in Plan 3 |
| Reloading a document (discard changes after last save) | Reload action + confirmation dialog | 9, guard | present per the checklist |
| Closing a document (save-or-discard prompt) | back/close handler + Save before Exit dialog | 9, guard | present per the checklist |

### 3.3 Moving through a document

| WG Chapter 1 section | Papirus surface | Plan | State today |
|---|---|---|---|
| Go to Page (status-bar field, Ctrl+G) | tap the page field in the bottom bar → Go to Page dialog | 4 (bar), 5 (accuracy) | works, but the bar it lives on is wrong and the page numbers themselves are wrong (65/88 vs 21) |
| Using the Navigator (categories, Navigate By, double-click to jump, content navigation view, heading level filter) | Navigator deck + Navigate By deck | 4 (categories), 7/8 (content), 5 (page index) | 12 categories exist as rows; the ones a parser can fill (headings, tables, images, pages) list and jump, the rest render "not yet available" from PR 13 (verified: no parser constructs the fetch/section/bookmark/comment/footnote/shape/OLE classes); the Hyperlinks category is added there too, its filter option lands with its data in 18/20 |
| Using outline folding (options toggle, hide/show content under headings, include sub-levels) | double-tap a heading toggles (`OutlineEngine`) | 3 (discoverability), guard | implemented, but the affordance is invisible: the guide gives it a setting and a visible button, Papirus gives it an undocumented double-tap |
| Setting reminders (up to five; the sixth deletes the first; not saved with the document) | Set Reminder in FCT, Reminder filter in the Navigator, Prev/Next | 2, guard | the checklist already tests the cap; the guide's "not saved with the document" matches `ReminderManager` behaviour to confirm in Plan 2 |
| Undoing and redoing changes (undo list, multi-step undo, redo list) | top-bar undo/redo, Actions to Undo/Redo subpage, dual-stack `HistoryManager` | guard | implemented; Plans 4-5 must not disturb the buffer-flush protocol |
| Displaying multiple views of a document | no mobile analogue (the guide's Window > New Window) | — | the `isWebView` toggle is the nearest analogue, and Plan 5 handles it with the document-views row above |

### 3.4 What this mapping means for the plans

* **Nothing in Chapter 1 is unassigned.** Two items are explicitly parked rather than fixed: encryption/remote storage (no plan, by decision) and the multiple-window view (no mobile analogue).
* **Three Chapter 1 items are owned by no *fidelity* plan and are easy to forget**, so they are written down here: outline-folding discoverability (Plan 3), the fake template content (Plan 3), and the `isWebView` second renderer (Plan 5).
* **Plans 7 and 8 are Chapter 1 requirements too**, not just format work: without numbering, tables and the TOC, the Navigator's categories and the status bar's page count cannot be right, and `docs/InkyC1Checklist.md` items 6, 7 and 10 cannot pass.

---

## 4. `docs/InkyC1Checklist.md` mapped onto the plans

The checklist is the acceptance suite. Each of its twelve sections needs a different plan before it can pass, and the ordering below is why Plan 2 runs first: five of the twelve sections are gated by UI or input defects that Plan 2 removes.

| # | Checklist section | Needs | Notes |
|---|---|---|---|
| 1 | Document Lifecycle (new/save/close/open, reload yes/no, save-before-exit) | Plan 9, Plan 4 (dialogs) | currently the save itself is the weak link, not the dialogs |
| 2 | Editing Engine, stages 1-2 ("Layout Engine does not rebuild the entire document") | Plan 5, guard | incremental layout must survive the metric change |
| 3 | Multiple Undo (`abcde` → undo → redo) | guard | implemented; Plan 5's caret mapping must not disturb it |
| 4 | Caret (`Home`, `End`, `Ctrl+↑/↓`, `↑`, `↓`) | Plan 5, guard | caret geometry is computed from the same metrics being replaced |
| 5 | Selection, stages 1-2 (FCT + ribbon undo interplay) | Plan 2 | stage 2 starts with "tap and hold to open FCT: Compact", which F-05 blocks in Viewer |
| 6 | Go To (20-page document, jump, out-of-range toast) | Plan 4, Plan 5 | needs a document whose page count is believable |
| 7 | Navigator (all categories, correct order, jump to exact location) | Plan 4, Plans 7/8, Plan 5 | Images and Hyperlinks categories must exist before the objects can be listed |
| 8 | Reminder (five, sixth deletes first, Prev/Next) | Plan 2, guard | the Navigator Reminder filter is part of the Plan 2 deck work |
| 9 | Zoom (50/100/150/200/300 with scroll, edit, select, FCT, undo, reload at each) | Plan 2, Plan 5 | 100 % is defined by the guide as fit-to-page; today it is a hard 320 dp card |
| 10 | Save Compatibility (contents, headings, paragraphs, bold/italic/underline/strikethrough, alignment) | Plan 9, Plans 7/8 | text survives today; headings, numbering and tables do not |
| 11 | Session Restore (page 15, 170 %, scroll, caret) | Plan 5, Plan 2 | zoom semantics change in Plan 2, so stored sessions must migrate rather than break |
| 12 | Stress test (100+ pages: scroll, edit, undo/redo, reload, close, save) | Plan 5, Plan 6, `docs/PHASE6_MEMORY_PLAN.md` | pagination cost and media memory are the two risks |

**Sequencing consequence.** Plan 2 lands first (it unblocks checklist items 5, 8, 9 and the dialogs of 1), Plan 3 runs beside it (it makes the remaining work machine-checkable), then Plan 5 before any fidelity plan, because every fidelity acceptance is expressed as a page count.

---

## 5. Cross-plan invariants

Every plan's PR must hold these, or it is not ready:

1. **One source of truth per decision:** one paginator (`LayoutEngine`), one unit system (`LayoutUnits`), one style resolver (`StyleResolver` + `OfficeRuns`), one numbering model, one media store. A plan that adds a second path deletes the first in the same PR. This is how the `OfficeDocElement` retirement (F-2) and the `isWebView` renderer get resolved instead of lingering.
2. **en_US + `strings.xml`:** no new hard-coded copy, no new `contentDescription` literal (`audit-006` §2.1 counts 231 and 72), and no Toast where the guide would show a dialog or a snackbar.
3. **48 dp touch targets** (`DESIGN.md:379`) and token colours only (`DESIGN.md:384`), verified at 320 dp width.
4. **No em dash** in user-visible strings (`antislop` R-02).
5. **Evidence, not claims:** each PR body lists the findings it closes, the suite it ran, and, for UI, the Delivery Gate with device evidence. CI is the only build evidence available here (no local JDK).
6. **The sample matrix is the floor:** the 12 files in `tests/inky` (Sample-1…6, ODT + DOCX) must stay green through Plans 5-9.

---

## 6. Working agreement for Plan 1

* **Deliverable:** this file. Its acceptance is completeness: every Chapter 1 section has an owner (§3, including the three items no fidelity plan owns), every checklist section is mapped to the plan that makes it pass (§4), and every finding has a home. The `Closes` column in §2 covers the full set: F-01…F-06 → 2/4, F-07 → 6, F-08/F-09 → 7 and 10, F-10/F-21/F-24/F-25/F-28 → 5, F-11…F-15/F-23 → 7, F-16…F-20/F-26/F-27 → 8, F-22/F-29/F-30/F-31 → 2, O-01 → 9, O-02 → 5 and 8, O-03…O-05 → 7.
* **Execution schedule:** the PR-by-PR order for everything above lives in `anti-slop/plan-2026-09-24-remaining-pr-roadmap-v2.md` (PRs 13-22, baseline `55a9a97`; v1 at `plan-2026-09-24-remaining-pr-roadmap.md` is the pre-PR-12 record), which also records the 2026-09-24 decisions (staged page windows, display-the-bundled-faces, TOC snapshot, save refusal). This file keeps the mapping and the numbers; the roadmap keeps the order.

* **Update rule:** when a plan lands, update its status here and add one line to the plan's own file recording what actually shipped versus what was written. Numbers in this file are the ones the other documents cite, so corrections happen here first.
* **Next actions after this file:**
  1. ~~Plan 2, commits 1-2~~ **done**: PR #11 merged the whole plan (its commits 1-2 became the renderer and status-bar commits of PR #11).
  2. ~~Plan 3A/3B~~ **complete**: PR 12 landed with the sweep and `SourceHygieneGuardTest`; Plan 3B is complete per its implementation record and the user's confirmation. Plan 3C is the current docs alignment (PR 14 / Plan 11).
  3. ~~Then Plan 5 with its per-page element dump~~ **scheduled as roadmap PR 15** (dump first, metrics seams second, transform third), exactly because the empty-page mechanism (F-25) is still an open question that only a trace can answer.
  4. **2026-09-26:** PR 14 (Plan 3C / Plan 11 docs) is merged (`5c99072`). Plan 5 starts with the evidence commit of PR 15 (`audit-007`); the empty-page question (F-25) is narrowed by code reading (the paginator cannot author a blank page except at element 0), so the dump confirms per file whether the blank pages come from the fake breaks or from inflated metrics. Plan 5 is now three PRs (15, 16a, 16b) and its windows are per format. The device checklist run that was postponed in roadmap v2 §5 ("Now, before PR 15": items 5, 8, 9) is still owed and is independent of PR 15's code.
