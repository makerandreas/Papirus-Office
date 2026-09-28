# Audit 012: Plan 1 synchronization, state ledger and split options

Date: 2026-09-28. Owner sequence: **Plan 5e -> Plan 1 synchronization -> Plans 6 to 11 -> physical-device Chapter 1 acceptance -> Chapter 2**.

Scope: everything Plan 1 (`anti-slop/plan-01-master-index.md`) owns after Plan 5e landed, plus the reference corrections audit-011 §9 assigns to it, plus the two obligations `DESIGN.md` already writes against Plan 1 (lines 175 and 227). This file is the analysis and the split options. It changes no product code, no test and no sample byte.

Method: local file and ZIP inspection of the checked-out tree at `439ed05` (merge of PR #19), `sha256sum` of all twelve fixtures against `tests/inky/fixture-identities.properties`, source greps over `app/src/main` and `app/src/test`, and `gh` reads of PR and Actions metadata. No JDK, no Android SDK, no Gradle run, no device. Every "actual" value below was measured in this sandbox; every CI number is quoted with its run id.

---

## 1. Recommendation

**Split Plan 1 into two PRs, land 1A first.**

| PR | Name | Files | Why it is separate |
|---|---|---|---|
| **1A** | Ledger and index synchronization | `anti-slop/plan-01-master-index.md`, `anti-slop/plan-2026-09-24-remaining-pr-roadmap-v2.md`, `anti-slop/plan-04-to-09-writer-fidelity.md` (Plan 5 record), `anti-slop/plan-5e-progress.md` (status), `anti-slop/plan-11-hybrid-experience-design.md` (edition pin) | Mechanical, evidence-led, and it is the thing that unblocks the next work: it records 5e as landed, retires the stale "5d is active next" line, and re-numbers the PR slots that audit-011 §8 says now collide with the merged 5c/5d/5e PRs |
| **1B** | Product documentation truth pass | `AGENTS.md`, `DESIGN.md`, `PROJECT_CONTEXT.md`, `CONCEPT.md` | Judgment-heavy, touches the two files agents are instructed from (`AGENTS.md`, `DESIGN.md` front matter), and carries the 5e state changes (flow view, metrics pipeline, blank packages, reference paths) |

**Why not one PR.** The two halves answer different review questions. 1A is "is the record true"; 1B is "is the description of the app true". Mixing them makes a correction to the PR numbering wait on a wording review of `DESIGN.md`, and it puts the agent-facing convention file in the same diff as a historical-plan edit.

**Why not three.** A third PR containing only historical audit/plan hygiene (em-dash clean-up, the 7.2 citation) has no gate that the first two do not already cover, and audit-011 §9.8 warns against relabelling old audit claims as current evidence. Fold the small historical corrections into 1A or 1B by file ownership, and leave the dated audits alone.

Both halves run the **documentation-only reduced Delivery Gate** (`AGENTS.md` antislop addendum item 3): R-02, R-15, R-16, R-17, R-36, R-38 and C-5 with evidence; R-03, R-25, R-26, R-27, R-32, R-34, R-35 reported N/A with the reason "no UI shipped in this deliverable".

---

## 2. Verified state on which Plan 1 must be written

Everything in this section was measured here, not inherited from a previous document.

### 2.1 Merge and CI state

* `main` head is `439ed05`, the merge of **PR #19 "Plan 5e: metric-driven pagination, source fragments and shared input"**, merged 2026-09-28 03:14 UTC. PRs 11 through 19 are all merged; no open pull request exists.
* Plan-to-PR mapping, read from `gh pr list`: 5a = PR #15, 5b = PR #16, 5c = PR #17, 5d = PR #18, 5e = PR #19. Plan 3C / Plan 11 documentation = PR #14. Plan 3B = PR #13, Plan 3A = PR #12, Plan 2 = PR #11.
* Latest workflow run on `main` is the scheduled **36398216577**, success, Unit Tests 9m08s plus Build 6m38s. The PR #19 head run is **36377627605**, reported by the owner as 247 tests, zero failures, errors or skips.

### 2.2 Fixture identity

All twelve `tests/inky` files match the SHA-256 recorded in `tests/inky/fixture-identities.properties` (`sha256sum`, byte for byte). Producers are Microsoft Office Word 16.0000 for the DOCX side and Collabora Office 26.04.3.1 for the ODT side. `FixtureIdentityTest` is the guard; `SampleMatrix` holds the references (DOCX 15/23/22/10/18/21, ODT 15/23/22/11/19/22) and the per-format windows.

### 2.3 Blank new-document packages

`app/src/main/assets/templates/Untitled.odt`, `.ods` and `.odp` are **Collabora Office 26.04.3.1 resaves**, not the hand-built 2017 to 2023 packages audit-008 §6 described:

| Package | Zip entries | `mimetype` position | `office:version` | `meta.xml` | Page layout |
|---|---|---|---|---|---|
| `Untitled.odt` | 9 | first entry, stored | 1.4 | present | `Mpm1`, 21.001 cm x 29.7 cm, 2 cm margins, master page `Standard` |
| `Untitled.ods` | 9 | first entry, stored | 1.4 | present | n/a (one `table:table`) |
| `Untitled.odp` | 8 | first entry, stored | 1.4 | present | n/a (one `draw:page`) |

Asset filenames are capitalised `Untitled.*`; the extracted cache filenames stay lowercase `untitled.*`. `TemplateManager` opens the exact asset path and returns `null` on failure; the styled-template fallbacks (`styles/Default.ott`, `wizard/styles/default.ots`, `slidia/Default.otp`) and the host-filesystem fallback are gone. `CreateNewDocumentTest` asserts byte identity, mimetype, content and manifest version, one body element, ODS sheet count, ODP page count, and cache replacement after the cached copy is overwritten with `"stale cache"`.

### 2.4 Pagination and input after 5e

* `LayoutEngine` owns pagination. `ParagraphMeasurer` produces `LineLayout` values that carry `startOffset`/`endOffset`, `caretOffsets`, `caretAdvances`, `left`, `width`, `height`, `baseline` and `discretionaryHyphen`. `TextMetrics` is the injected measurement backend; `PageEndReason` is explicit (`END`, `OVERFLOW`, `AUTHORED`, `KEEP`).
* `PaginationFidelityTest` asserts all twelve fixtures against the recorded windows **and** asserts per-element source coverage: the concatenation of every fragment's `[sourceStart, sourceEnd)` equals the element text, and consecutive fragments are adjacent. No hint, scaling or spacing constant is used to reach a window.
* `FragmentPaginationTest` (12 cases) and `FragmentInputTest` cover body-bounded fragments, widow/orphan and keep rules, section kinds, and the projection.
* Editor and Viewer share one input field per logical paragraph. `ParagraphProjection` is a `VisualTransformation`: inserted line breaks and page gaps are display-only, so wrapping does not touch the global edit value, the undo history or the IME composition.
* Inky flow (Web) view is **not available**: `InkyModule.kt:124` holds `isWebView = false`, and the editor's mobile-view button raises `R.string.inky_flow_view_unavailable` ("Flow view is not available yet. Use paginated view."). The corresponding `BasicTextField` branch is unreachable. Cellina and Slidia still have a live `isWebView` toggle; Inky does not.
* `TextLayoutManager` is referenced from exactly one production site, `InkyModule.kt:3897`, inside the `showTextFormattingInspector` debug dialog. It is not part of the pagination path.
* Unit test sources contain 234 `@Test` annotations plus one instrumented `@Test`; `SourceHygieneGuardTest` scans only the main source set, so no documentation edit in this plan can break it.

### 2.5 Measured page counts

Owner-reported final matrix for the PR #19 head (CI run 36377627605, table advance backend, not device glyphs):

| Sample | ODT measured / reference | DOCX measured / reference | ODT window | DOCX window |
|---|---|---|---|---|
| 1 | 15 / 15 | 15 / 15 | 12 to 18 | 12 to 18 |
| 2 | 23 / 23 | 24 / 23 | 18 to 28 | 18 to 28 |
| 3 | 18 / 22 | 21 / 22 | 18 to 26 | 18 to 26 |
| 4 | 10 / 11 | 11 / 10 | 9 to 13 | 8 to 12 |
| 5 | 18 / 19 | 19 / 18 | 15 to 23 | 15 to 21 |
| 6 | 19 / 22 | 22 / 21 | 17 to 27 | 15 to 26 |

All twelve are inside their windows. Sample-3 ODT (18 against a reference of 22) is the weakest pair and is the one Plan 7 and Plan 8 must explain with structure, not with a widened window. Plan 10 may legitimately move counts again when real Typefaces load; audit-011 §8 records that windows tighten only with measured support.

---

## 3. Documentation drift inventory

IDs are `P1-nn`. "Actual" is what this audit measured at `439ed05`. Owner column says which Plan 1 PR takes it.

### Group A: reference paths (audit-011 §9.1)

| ID | Location | Claim | Actual | Owner |
|---|---|---|---|---|
| P1-01 | `AGENTS.md:30`, `:116`, `:117`, `:124`, `:151` | ODF 1.4 lives in `docs/html` | the four ODF 1.4 part files are in `docs/odf/`; no `docs/html` exists | 1B |
| P1-02 | `CONCEPT.md:180` | "Follow ODF 1.4 in `docs/html`" | `docs/odf/` | 1B |
| P1-03 | `DESIGN.md:33` front matter, `:226` | `odf_1_4: docs/html` | `docs/odf/` | 1B |
| P1-04 | `PROJECT_CONTEXT.md:33`, `:220`, `:234`, `:320`, `:331` | `docs/html` (five sites, including the repository tree) | `docs/odf/` | 1B |
| P1-05 | `DESIGN.md:18` to `:22` front matter, and body citations at `:160`, `:166`, `:173`, `:175`, `:227` | guide PDFs at `docs/GS262-...pdf`, `docs/WG262-...pdf`, and so on | all five PDFs are under `docs/lo-guides/` | 1B |
| P1-06 | `anti-slop/plan-04-to-09-writer-fidelity.md:12`, `anti-slop/plan-11-hybrid-experience-design.md:102`, `:153` | `docs/html` | `docs/odf/` | 1A |
| P1-07 | `anti-slop/plan-01-master-index.md:18` | ODF 1.4 Parts 1-4 in `docs/html` | `docs/odf/` | 1A |
| P1-08 | `PROJECT_CONTEXT.md:234` tree | lists `docs/html/`, five `docs/*.pdf`, and no `docs/design.md-references/`, `sdk-references/`, `tests/`, `skills/`, `CONCEPT.md` or `antislop.md` | those directories exist and are load-bearing | 1B |

### Group B: blank package description (superseded by 5e Gate 0)

| ID | Location | Claim | Actual | Owner |
|---|---|---|---|---|
| P1-09 | `PROJECT_CONTEXT.md:92` | "hand-built and non-conformant (no `office:version`, no `meta.xml`, no page layout; `mimetype` is not the first entry in the odt), so a blank Inky document opens on the Letter fallback (audit-008 §6). Regeneration from Collabora 26.04 with a `TemplatePackageTest` is queued." | all three are Collabora 26.04 resaves, version 1.4, `mimetype` first, A4 page layout present in the ODT. The package identity test exists as `CreateNewDocumentTest.templateManager_extractsUntitledOdtOdsOdp`, not as a file named `TemplatePackageTest` | 1B |
| P1-10 | `AGENTS.md:84`, `PROJECT_CONTEXT.md:84`, `DESIGN.md:167` | `assets/templates/untitled.od{t,s,p}` (lowercase) | asset filenames are `Untitled.od{t,s,p}`; lowercase is the extracted cache name. The case mismatch is exactly the 5e Gate 0 defect | 1B |
| P1-11 | `AGENTS.md:84` | blank extraction has no failure mode described | `TemplateManager.extractAssetTemplate` returns `null` on a missing asset and no longer substitutes a styled gallery template | 1B |

### Group C: engine and layout description (superseded by 5e)

| ID | Location | Claim | Actual | Owner |
|---|---|---|---|---|
| P1-12 | `AGENTS.md:105` | engine is `OfficeDocumentParser`, `DocxDocumentParser`, `SwDocEngine`, `LayoutEngine`, `TextLayoutManager` | pagination runs through `LayoutEngine` + `ParagraphMeasurer` + `TextMetrics`; `TextLayoutManager` is used only by the debug formatting inspector (`InkyModule.kt:3897`) | 1B |
| P1-13 | `PROJECT_CONTEXT.md:47` | "`LayoutEngine` & `TextLayoutManager`: Calculates line wraps, margins, paragraph indentations, tabs, bullet prefixes, and multi-page layouts." | same split as P1-12, plus `ParagraphMeasurer`, `LayoutUnits`, `FontRegistry`, `PageTransform` and `PageEndReason` are unnamed | 1B |
| P1-14 | `anti-slop/plan-01-master-index.md` §3.1 "Document views" row | "the Web View path is a **second renderer** (`InkyModule.kt:2292-2360`, a plain `BasicTextField`) that bypasses the layout engine... Plan 5 must either align it with the paginator or retire it" | retired: `isWebView = false` at `InkyModule.kt:124`, a string resource states the flow view is unavailable, and the branch is unreachable | 1A |
| P1-15 | `anti-slop/plan-01-master-index.md` §3.3 "multiple views" row | "the `isWebView` toggle is the nearest analogue, and Plan 5 handles it" | same as P1-14 | 1A |
| P1-16 | `anti-slop/plan-01-master-index.md` §3.3 "Go to Page" row | "the page numbers themselves are wrong (65/88 vs 21)" | all twelve fixtures now sit inside their recorded windows; the worst gap is Sample-3 ODT at 18 against 22 | 1A |
| P1-17 | `anti-slop/plan-01-master-index.md` §4 row 9 (Zoom) | "100 % is defined by the guide as fit-to-page; today it is a hard 320 dp card" | `PageTransform` (PR #15) owns one `pageScale`; `PageTransformTest` and `Plan2ChromeTest` cover it. The sentence describes pre-5a code | 1A |
| P1-18 | `anti-slop/plan-01-master-index.md` §4 row 2 (Editing Engine) | "incremental layout must survive the metric change" | survives; `FragmentInputTest` and the 100+ page stress coverage are the guards | 1A |
| P1-19 | `anti-slop/plan-01-master-index.md` §4 row 12 (Stress) | "pagination cost and media memory are the two risks" | automated 100+ page coverage exists; media memory is still Plan 6 | 1A |

### Group D: registry, PR numbering and plan status (audit-011 §9.3 and §8)

| ID | Location | Claim | Actual | Owner |
|---|---|---|---|---|
| P1-20 | `plan-01-master-index.md` §2 row 5 | "**5d** (breaks and defaults) is active next; **5e** (metrics and windows) follows" | 5a to 5e are all merged, in PRs #15, #16, #17, #18 and #19 | 1A |
| P1-21 | `plan-01-master-index.md` §2 rows 6 to 9 | Plan 6 "scheduled as PR 17", Plan 7 "PRs 18-19", Plan 8 "PRs 20-21", Plan 9 "PR 22" | those numbers are taken by 5c, 5d and 5e. Next free slots start at PR #20 | 1A |
| P1-22 | `plan-2026-09-24-remaining-pr-roadmap-v2.md` §4.12 | per-PR table keyed on 13 to 22 with 17 to 22 as future work | the 17/18/19 rows are 5c/5d/5e; the table needs a "landed" column and a re-numbered future column | 1A |
| P1-23 | `plan-5e-progress.md:5` | "**Status: started, not complete.**" | implementation and automated acceptance are complete; CI run 36377627605 is green and PR #19 is merged | 1A |
| P1-24 | `plan-04-to-09-writer-fidelity.md` § Plan 5 | the 5A record is written; 5B to 5E are not | needs one 5B/5C/5D/5E record block each, per plan-01 §6's update rule | 1A |
| P1-25 | `plan-01-master-index.md` §6 item 5 | last entry is dated 2026-09-27 | needs a 2026-09-28 entry recording 5d, 5e and this synchronization | 1A |

### Group E: edition pins and historical claims (audit-011 §9.2, §9.4, §9.6, §9.8)

| ID | Location | Claim | Actual | Owner |
|---|---|---|---|---|
| P1-26 | `plan-01-master-index.md` §1 and §3 | the WG chapter map cites **WG 24.8** (`WG24801-IntroducingWriter.html`) | `AGENTS.md:30` and `DESIGN.md` pin 26.2; `DESIGN.md:227` explicitly assigns "the historical 24.8 chapter-map update" to Plan 1 | 1A |
| P1-27 | `plan-11-hybrid-experience-design.md:66` | cites **LibreOffice Writer Guide 7.2** as its Chapter 1 reference | the pinned edition is 26.2; 7.2 is two shelves older. Either re-map or date the file as historical | 1A |
| P1-28 | `plan-11-hybrid-experience-design.md` §4 sample inventory | per-sample XML counts (paragraphs, lists, links, sections) | measured before the 5b re-baseline; audit-008 §1 and §4 supersede audit-007 on the regenerated fixtures | 1A |
| P1-29 | `plan-01-master-index.md` §5 item 5, and audit-001/002/010 | "CI is the only build evidence available here"; audit-010's Chapter 1 PASS table | audit-011 §9.8: those are not current device evidence. Keep them labelled as history, and keep the device gate as the owner's post-Plan-11 pass | 1A |
| P1-30 | `plan-01-master-index.md` §2 row 3 | Plan 10 "document-font thread remains parked pending fidelity" | still true, and it now has a concrete consequence: real Typefaces will move page counts again (audit-011 §8) | 1A |

### Group F: content hygiene in active documents

| ID | Location | Finding | Recommendation | Owner |
|---|---|---|---|---|
| P1-31 | `plan-2026-09-24-remaining-pr-roadmap-v2.md` (113 em dashes at scan time), `plan-04-to-09-writer-fidelity.md` (12) | R-02 forbids the em dash in text the agent writes; both files are active planning documents, not dated audits | **done in 1A**: 113 and 12 removed, replaced case by case with a colon, comma, semicolon, period or parenthesis, and empty table cells written as a bare dash became `none`. One occurrence was deliberately kept: `plan-11` line 152 quotes the verbatim external title of the Microsoft Learn Open XML SDK page, which contains an em dash as published; rewriting a cited title would misrepresent the source. Do not touch the dated `audit-*` files | 1A |
| P1-32 | `InkyModule.kt:2084-2092` | the editor's mobile-view button is a control whose only behaviour is a toast | honest label, but still a dead control under R-26. Record it as a Plan 11 editor-package item (remove it, or give it a real destination) rather than describing it as shipped | 1A ledger |
| P1-33 | `curatedTemplates` (`TemplateManager.kt:60`, `:70` to `:142`, `:211`) | 12 list entries: 3 blank `asset://templates/Untitled.*` items plus 9 `filesamples.com` downloads. The declaration at line 60 and the parser usage at line 211 are not entries | the docs' "9 of the 12" is still correct; confirm it survives the edit rather than re-deriving it | 1B, verify only |

---

## 4. What each PR contains

### 1A: ledger and index synchronization

1. `plan-01-master-index.md` §2: row 5 becomes "5a to 5e landed (PRs #15 to #19); Plan 6 is next"; rows 6 to 11 get the re-numbered slots (Plan 6 = PR #20, Plan 7A/7B = #21/#22, Plan 8A/8B = #23/#24, Plan 9 = #25, Plans 10 and 11 keep plan IDs and lose predicted numbers).
2. `plan-01-master-index.md` §3: re-map the Chapter 1 table onto 26.2 (P1-26), replace the Web-view and multiple-views rows with the retired state (P1-14, P1-15), and correct the Go to Page row (P1-16).
3. `plan-01-master-index.md` §4: update rows 2, 9 and 12 (P1-17 to P1-19) and mark which sections are now unblocked versus still gated on Plans 7 to 9.
4. `plan-01-master-index.md` §5 and §6: keep the cross-plan invariants, add the 2026-09-28 next-actions entry (P1-25).
5. `plan-04-to-09-writer-fidelity.md`: add the 5B/5C/5D/5E record blocks (P1-24), fix `docs/html` (P1-06).
6. `plan-2026-09-24-remaining-pr-roadmap-v2.md` §4.12: mark 17/18/19 as 5c/5d/5e, re-number the future column (P1-22), and remove em dashes from the lines it touches (P1-31).
7. `plan-5e-progress.md`: status to complete, with the merged head, the merge commit and the CI run (P1-23).
8. `plan-11-hybrid-experience-design.md`: pin 26.2 or date the file as historical (P1-27), mark the sample inventory as pre-5b (P1-28), fix `docs/html` (P1-06), record the dead mobile-view control as a Plan 11 item (P1-32).

### 1B: product documentation truth pass

1. `AGENTS.md`: `docs/html` to `docs/odf` (P1-01), engine list (P1-12), blank asset case and failure mode (P1-10, P1-11).
2. `DESIGN.md`: front matter `odf_1_4` and the five guide PDF paths (P1-03, P1-05), body citations at lines 160, 166, 173, 175 and 226, and the Create New line 167 case fix (P1-10).
3. `PROJECT_CONTEXT.md`: `docs/html` at five sites plus the repository tree (P1-04, P1-08), the blank-package paragraph (P1-09), the engine description (P1-13), and the lowercase asset paths (P1-10).
4. `CONCEPT.md:180` (P1-02).
5. Re-verify the "9 of the 12" curated-template count while editing (P1-33).

Neither PR touches Kotlin, resources, tests, `tests/inky` bytes, `fixture-identities.properties` or `.github/workflows`.

---

## 5. Gates and sequencing

| Step | Gate to enter | Evidence to record |
|---|---|---|
| 1A | every number in §2 of this audit is re-checked against the tree at the PR head | `sha256sum` output for the twelve fixtures, the `gh pr list` mapping, the three blank-package inspections |
| 1B | 1A merged | the diff, plus a line-by-line list of the `P1-nn` IDs it closes |
| Plan 6 | 1A merged, so the PR slot is unambiguous | the new registry row |

CI for both is the existing Unit Tests plus Build workflow. Documentation edits cannot affect `SourceHygieneGuardTest`, which reads only `app/src/main/java`, so a green CI run is a no-regression signal and nothing more; it is not evidence that the documentation is correct.

---

## 6. Boundaries

* No pagination, parser, renderer or save change. No window is widened, tightened or added.
* No sample byte, hash or reference count changes.
* No claim of device verification, visual parity or format compatibility. The physical-device Chapter 1 pass stays where the owner put it: after Plan 11.
* The dated `audit-001` through `audit-011` files are not rewritten. Where they are superseded, the superseding file says so; that is the existing convention.
* Plan 1 does not absorb Plan 11 work. The local template gallery, the fabricated resume, the Google Drive placeholder and the dead mobile-view control are recorded, not fixed.

---

## 7. Documentation-only Delivery Gate for this audit

| Gate | Result |
|---|---|
| R-02 | No em dash in the new text of this file. Existing em dashes in two active planning files are reported as P1-31, not silently copied into new content. |
| R-15 / R-16 | Every next action names a file, a line and an owner. No marketing or generated description. |
| R-17 | Every number here is measured in this sandbox or quoted with a CI run id and its source. Sample counts come from `sha256sum` and `zipfile`; PR numbers come from `gh pr list`. |
| R-36 / C-5 | Measured, inherited, quoted and unperformed states are separated. No pagination fidelity, format compatibility, native capability or device claim. |
| R-38 | No replacement dummy content is proposed; the fabricated catalog and resume entries are identified as Plan 11 work. |
| R-03 / R-25 / R-26 / R-27 / R-32 / R-34 / R-35 | N/A: no UI shipped in this deliverable. R-26 is still reported as an open finding against existing app code (P1-32). |

Local evidence collected in this audit: twelve `sha256sum` fixture comparisons; three blank-package ZIP inspections (`mimetype` position, `office:version`, `meta.xml`, page layout, master page); `gh pr list` and `gh run view` reads; 234 unit plus 1 instrumented `@Test` count; `gh` run status for `main`; greps for `docs/html`, `isWebView`, `TextLayoutManager`, `Untitled.od*` and `filesamples.com` across the tree; em-dash counts per document. No Gradle, no JDK, no device, no screenshot.

---

## 8. 1A execution record (2026-09-28)

Applied as one PR on `arena/01a0e7ac-papirus-office`:

| File | Change |
|---|---|
| `anti-slop/plan-01-master-index.md` | §1 edition note and ODF path; §2 registry rows 5 to 11 with landed PRs and the next free slot (#20); §3 chapter map re-pinned to 26.2 with the document-views, Go to Page and multiple-views rows corrected; §4 rows 2, 3, 4, 6, 9, 11, 12 plus the deferred-device-pass note; §5 invariants 1, 5, 6; §6 schedule note and next-action entries 6 to 8; audits-consumed paragraph now includes `audit-011` and `audit-012`, plus the PR-numbering note |
| `anti-slop/plan-2026-09-24-remaining-pr-roadmap-v2.md` | §4.12 rewritten as a landed table plus a re-numbered forward table; §4.10 and §4.11 dependency references; §5 sequencing table and device-checklist resume points; §6 invariants; Appendix A ODF path correction; em-dash sweep (113) |
| `anti-slop/plan-04-to-09-writer-fidelity.md` | Plan 5 section gained 5B, 5C, 5D and 5E implementation records and the 2026-09-28 amendment; `docs/html` corrected; em-dash sweep (12) |
| `anti-slop/plan-5e-progress.md` | status from "started, not complete" to merged, with the merge commit, the CI runs and the explicit not-approved device scope |
| `anti-slop/plan-11-hybrid-experience-design.md` | §3 edition corrected to 26.2 with the 7.2 citation kept as the historical source; §4 sample inventory marked as pre-5b; §5 PR-slot paragraph and dependency note re-numbered; editor-surfaces row gained the dead mobile-view control (P1-32); `docs/html` corrected at two sites |

No Kotlin, resource, test, fixture or workflow file was touched. 1B (the product-documentation truth pass over `AGENTS.md`, `DESIGN.md`, `PROJECT_CONTEXT.md` and `CONCEPT.md`) is the remaining half of Plan 1 and carries P1-01 to P1-05, P1-08 to P1-13 and P1-33.

**Post-merge correction (same day).** PR #20 was assigned to 1A and PR #21 is held for 1B, which consumed the two slots the forward schedule had reserved for Plan 6 and Plan 7A. The tables were updated the same day: the record now keys on plan IDs with PR numbers marked as expectations, and Plans 6 to 9 are expected at #22 to #27. This is the exact failure mode `audit-011` §8 warned about, and the fix is the labelling rule, not a new set of fixed numbers.
**CI evidence for 1A.** PR #20, run [36415846086](https://github.com/makerandreas/Papirus-Office/actions/runs/36415846086) (commit 1) and run [36415971612](https://github.com/makerandreas/Papirus-Office/actions/runs/36415971612) (commit 2): both green, Unit Tests 5m35s and Build 6m32s on the second run. Documentation edits cannot affect `SourceHygieneGuardTest`, which reads only `app/src/main/java`, so a green run here is a no-regression signal and not evidence that the documentation is correct.
