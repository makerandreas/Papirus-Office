# Papirus Office — Plan Registry (2026-09-24)

**Purpose:** one page that says what every plan is, what it closes, what it depends on, and where its detail lives. Update the status column as plans land; the plans themselves stay immutable reference documents.

**Context of this registry.** It exists because the post-PR-C test produced a large, multi-layer defect set, and the letter naming (PR A to PR I) collided with the project's own history: `PR A/B1/B2/C` are *merged* pull requests (#7, #8, #9, #10) while `D` to `I` were only proposed names. Plans now carry numbers; the letters survive only in parentheses and in sub-item IDs (`D-1`, `E-EN-2`, `G-1` …), which read as `plan-n item`: **D = 4, E = 5, F = 6, G = 7, H = 8, I = 9**.

---

## 1. The registry

| # | Plan | Closes | Depends on | Detail | Status |
|---|---|---|---|---|---|
| **1** | **Master index** (this file) | — | — | here | living document |
| **2** | **Screenshots verification and UI/UX backlog** | F-01, F-02, F-03, F-04, F-05, F-06, F-22, F-29, F-30, F-31 | audit-005, audit-006 | `plan-02-screenshots-and-ui-backlog.md` | ready to start |
| **3** | **Compliance sweep** (docs, specs, localization, dead controls) | the `AGENTS.md` / `DESIGN.md` / `antislop` / ODF / OOXML findings in audit-006 §2-§3 | audit-006 | `plan-03-compliance-sweep.md` | ready to start (parallel with 2) |
| **4** | **Viewer/Editor chrome and input fixes** (was PR D) | F-01, F-02, F-03, F-04, F-05, F-06 | — | `plan-04-to-09-writer-fidelity.md` § Plan 4 | detailed |
| **5** | **Layout metrics and pagination** (was PR E) | page-count half of finding 6, F-10 metrics, F-21, F-24, F-25, F-28 | — | same file, § Plan 5 | detailed |
| **6** | **Image pipeline and load performance** (was PR F) | F-07, F-18, save-path guard for images | Plan 5 (extents are layout units) | same file, § Plan 6 | detailed |
| **7** | **ODF structural fidelity** (was PR G) | F-08, F-09, F-11, F-12, F-13, F-14, F-15, F-23, O-03, O-04, O-05 | Plan 5 (numbering + font seams) | same file, § Plan 7 | detailed |
| **8** | **OOXML structural fidelity** (was PR H) | F-16, F-17, F-19, F-20, F-26, F-27, DOCX halves of F-10/F-11 | Plan 5, Plan 7 | same file, § Plan 8 | detailed |
| **9** | **Save round-trip integrity** (was PR I) | O-01, the "non-destructive package preservation" rule in `AGENTS.md`, the ODF/OOXML writer findings in audit-006 §3 | Plan 6, Plan 7, Plan 8 | same file, § Plan 9 | scoped, not scheduled |
| **10** | **Font rendering engine + UI design language** | old PR D (bundled Typeface and substitution) and old PR E (Font Style UI, SAF/user fonts, curated Google Fonts), plus the UI design-language review against `DESIGN.md` and m3.material.io | Plan 5 (FontRegistry seam), Plan 7, Plan 8 | `plan-10-font-engine-and-design-language.md` | parked, deliberately |

**Audits this registry consumes:** `audit-005-2026-09-24.md` (findings F-01…F-20 from the written report) and `audit-006-2026-09-24.md` (findings F-21…F-31 from the screenshots, plus the compliance sweep). `audit-003-2026-09-22.md` holds the pre-A/B/C backlog; `anti-slop/plan-2026-09-22-remaining-writer-fixes.md` holds the A/B/C plan that has since merged.

---

## 2. Why the order is 2 → 5 → 6/7/8 and not file-by-file

* **Plan 2 first** because it is the only plan whose changes are visible to the user in one afternoon and whose risk is near zero: it removes a duplicate counter, stops an overlap, centres a label, restores the keyboard and the Viewer FCT, and makes 100 % mean fit-to-width. It also unblocks the manual test loop the later plans need.
* **Plan 5 before 6-8** because every fidelity plan ends in a page count, and page counts are meaningless while the paginator measures 12 pt text at 30 units and adds a flat 12 units after all 547 elements.
* **Plans 6, 7, 8 in any order after 5**, but 7 before 8 in practice: Plan 8 reuses the numbering model and the table geometry that Plan 7 lands, and the DOCX side is where the user reported the widest gap, so it deserves the steadier ground.
* **Plan 9 last** because a writer can only be correct once the reader models what it must write back.
* **Plan 10 stays parked** by the user's decision (focus shifted to fidelity). Its font half is *blocked* on Plan 5's `FontRegistry` seam and its design half is independent of everything.

---

## 3. Cross-plan invariants

Every plan's PR must hold these, or it is not ready:

1. **One source of truth per decision:** one paginator (`LayoutEngine`), one unit system (`LayoutUnits`), one style resolver (`StyleResolver` + `OfficeRuns`), one numbering model, one media store. A plan that adds a second path must delete the first in the same PR (this is how F-2 `OfficeDocElement` retirement is honoured).
2. **en_US + `strings.xml`:** no new hard-coded copy, no new `contentDescription` literal, no Toast that should be a snackbar (audit-006 §2.1).
3. **48 dp touch targets** (`DESIGN.md:379`) and token colours only (`DESIGN.md:384`), verified at 320 dp width.
4. **No em dash** in any user-visible string (`antislop` R-02).
5. **Evidence, not claims:** each PR body lists the findings it closes, the test suite it ran, and (for UI) the Delivery Gate with device evidence. CI is the only build evidence available in this environment (no local JDK).
6. **Sample matrix green:** the 12 files in `tests/inky` are the regression floor for plans 5, 6, 7, 8 and 9.

---

## 4. Coverage check — every finding has a home

| Finding | Covered by | Finding | Covered by |
|---|---|---|---|
| F-01 per-page counter | 2, 4 | F-17 DOCX tables | 8 |
| F-02 FAB overlap | 2, 4 | F-18 image extents | 6 |
| F-03 counter centring | 2, 4 | F-19 DOCX TOC | 8 |
| F-04 keyboard dead | 2, 4 | F-20 DOCX run formatting | 8 |
| F-05 Viewer FCT | 2, 4 | F-21 14 sp new-document default | 5 |
| F-06 zoom not fitted | 2, 4 | F-22 32 dp zoom buttons | 2 |
| F-07 image delay / never loads | 6 | F-23 table header as paragraphs | 7 |
| F-08 size right by accident | 5, 7 | F-24 pages end empty | 5 |
| F-09 font family lost | 7, 10 | F-25 empty page | 5 |
| F-10 paragraph metrics | 5 | F-26 TOC field code as text | 8 |
| F-11 ODF tables | 7 | F-27 TOC leaders | 8 |
| F-12 bullet size | 7 | F-28 DOCX empty page | 5, 8 |
| F-13 multilevel numbering | 7, 8 | F-29 counters in both modes | 2 |
| F-14 TOC + hyperlinks (ODF) | 7 | F-30 FAB covers status bar | 2 |
| F-15 heading runs | 7 | F-31 page-card double frame | 2 |
| F-16 DOCX heading sizes | 8 | | |

Nothing in audits 005 and 006 is unassigned. Findings that need a device or a build to close are marked **[needs run]** in audit-006 and are verified through `docs/InkyC1Checklist.md`.
