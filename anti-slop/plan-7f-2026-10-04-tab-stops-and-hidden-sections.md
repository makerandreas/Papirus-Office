# Plan 7F: ODF tab leaders and hidden-section layout

**Scope drafted:** 2026-10-04. **Scope corrected:** 2026-10-05. **Status:** implemented in PR #33; both feature gates passed; PR remains open and unmerged.
**PR:** one PR, **#33**, after the separate selection-projection correction (**#32**, merged as `466240e`). Plan 7E is merged as PR #31. Forecasts can move; the plan ID does not.
**Scope owner:** the owner approved this split when PR #30 landed (recorded in that PR's body: 7E owns font-face declarations, alias resolution and final calibration; 7F owns ODF tab stops/leaders and hidden-section layout). The first draft of this document was written in a session whose commits never reached a remote (audit-017 section 1); this version is rebuilt from the tree and the raw fixture packages and is labelled static where it states design rather than measured fact.
**References:** roadmap v2 sections 4.7d items 5 and 6 and 4.7f (current scope/sequence), `audit-015` section 9 (Plan 7C record), `audit-017` sections 4.3 and 4.4 (fixture re-derivation) and section 13.4 (the withdrawn `text:display` expectation), and `audit-018` (selection-projection correction, reforecast, and merge closeout).
**Depends on:** Plan 7E (the resolved family is an input to tab measurement) and the merged selection-projection correction PR #32 as the chosen merge-order gate. Plan 8A does not technically depend on 7F, but the agreed forecast schedules this one-PR Plan 7F before 8A; implementation PRs remain sequential.

**Goal:** the fixture-authored textual tab leaders render, and content inside supported body-level hidden-section ranges is not laid out. “Leader support” here means the ODF `style:leader-text` subset defined below, not general leader-line styling.

**Delivery structure:** one PR with two separate feature commits and commit-level test gates. Commit 1 delivers leader import/model/layout/painting and its tests; it must pass before commit 2 starts. Commit 2 delivers hidden-section layout and synthetic tests. Any records-only cleanup comes after both gates. Plan 7F remains distinct from the already-merged selection-projection correction in PR #32.

---

## 1. What already ships, so 7F does not rebuild it

Verified in the tree on 2026-10-04:

* **Positions and alignment.** `SvXMLImport.kt:797-806` reads `<style:tab-stops>` and `<style:tab-stop>`, maps `style:type` (`right`, `center`, `char` to DECIMAL, otherwise LEFT) and appends `ParagraphTabStop(positionUnits, alignment)`. `SvXMLImport.kt:496` reads `style:tab-stop-distance` into `defaultTabIntervalUnits`.
* **Model.** Positions and `TabAlignment` (`LEFT, RIGHT, CENTER, DECIMAL, CLEAR`) predate 7F; `ParagraphTabStop` now carries nullable `leaderText` in addition to `positionUnits` and `alignment` (`OfficeDocument.kt:302-312`). `ParagraphStyle.defaultTabIntervalUnits` still defaults to `48f`, and tab stops participate in the paragraph cache key, so a tab change invalidates measurement.
* **Layout.** `ParagraphMeasurer.tabAdvance` (`:57-90`) walks the sorted stops, applies RIGHT, CENTER and DECIMAL offsets from the text that follows the tab, skips `CLEAR` stops (and steps past a cleared position), honours the style interval, and falls back to `48f` when the style sets none. It is consumed twice, while choosing a break (`:92`) and while measuring the line (`:120`).

**Pre-7F baseline gap (verified 2026-10-04):** neither `style:leader-style` (§19.489) nor `style:leader-text` (§19.490) was read (`grep -rn "leader" app/src/main/java` had no hit), `ParagraphTabStop` had no leader field, and the display projection painted no leaders. The implementation and tested support boundary are recorded below.

Normative shape, re-read in `docs/odf/OpenDocument-v1.4-part3-schema.html`:

* `style:leader-style` styles a leader **line** (for example `dotted`, `dash`, or `solid`). `style:leader-type` (§19.492) says whether a **line** is drawn (`none`, `single`, `double`); it does not suppress a textual leader.
* `style:leader-text` is a **single Unicode character** used as a textual leader. When both `leader-text` and `leader-style` are specified, §19.490 gives `leader-text` precedence. The consumer should substitute a character it supports when the requested one is unsupported. Its default is U+0020 SPACE; absent or space-only leaders therefore do not create visible marks here.
* `style:leader-text-style` (§19.491), `leader-color` (§19.488), and `leader-width` (§19.493) also attach to `<style:tab-stop>` (17.8), but are separate styling features. `style:leader-char` (§19.487) is a different attribute for `<text:index-entry-tab-stop>` (8.13.6), not `<style:tab-stop>`.

**Scope correction (2026-10-05):** all leader declarations found in Samples 2, 4, 5 and 6 pair `style:leader-text="."` with `style:leader-style="dotted"`. 7F implements single-character **textual leaders** and the `leader-text` precedence rule, including a measured supported-character fallback to `.`. It does not claim support for line-only leaders or line/text styling. This is the exact checked-in ODF subset being delivered; later support for other text characters can extend it without implying that the ODF line-pattern vocabulary is already implemented.

Fixture load (audit-017 section 4.3, re-verified in this session from the raw packages; every declaration uses `dotted` plus `.`):

| file | `<text:tab>` in body | leader-style / leader-text pairs |
|---|---|---|
| Sample-1.odt | 0 | 0 / 0 |
| Sample-2.odt | 49 | 2 / 2 |
| Sample-3.odt | 0 | 0 / 0 |
| Sample-4.odt | 35 | 2 / 2 |
| Sample-5.odt | 43 | 2 / 2 |
| Sample-6.odt | 167 | 3 / 3 |

## 2. Item 1: leaders

1. `ParagraphTabStop` gains only the declared `leaderText` character (nullable; the default preserves all existing construction sites). `leader-style`/`leader-type` are not modeled as line render instructions in this scope.
2. `SvXMLImport` reads and validates a one-Unicode-scalar `style:leader-text` on `<style:tab-stop>`. A declared text leader wins over the accompanying line style/type as §19.490 requires. When its resolved measurement source reports that glyph unsupported, use `.` if supported and mark the fallback on the layout run; if neither is supported, render no leader. With `leader-text` absent, line-style-only declarations render nothing in 7F. `leader-type="none"` does not suppress explicit text.
3. `ParagraphMeasurer` preserves the existing `tabAdvance` result exactly and records per-line display-only leader runs inside the already reserved tab gap. A gap narrower than one leader glyph renders no leader.
4. The display projection paints the repeated text inside that gap using the tab's resolved run style. The remaining reserved gap is represented by the existing scaled nonbreaking-space projection. Source text, tab/caret advances, selection mapping, IME state, and undo history are unchanged; no renderer-only measurement table is introduced (plan-01 §5 invariant 1).
5. Tests: import tab leaders from all six ODT samples; assert the `.` text leaders in Samples 2, 4, 5 and 6; exercise LEFT, RIGHT, CENTER, DECIMAL and `CLEAR` advances; prove line-style/type do not override explicit text; test supported-character fallback, a gap narrower than one glyph, source/display offset mapping, and unchanged reserved tab advances.

## 3. Item 2: hidden-section layout

Plan 7C records `SectionDisplay.HIDDEN` on `DocumentSectionRange` (`DocumentSemantics.kt:69-79`) and keeps Navigator rows and jumps out of hidden ranges (`DocumentIndexEngine.kt:410,414`). Before 7F, no layout path read `namedSectionRanges`, so hidden text was still placed; the implementation now excludes hidden ranges, and everything nested inside them, from layout.

* The exclusion happens in one place, decided by the same `DocumentSectionRange` values Plan 7C records, so the renderer and the Navigator cannot disagree. Filtering uses each range against the original `DocumentBody.elements` indices; it must not compact or reindex the body before pagination.
* Conditional sections (`SectionDisplay.CONDITIONAL`) keep their current treatment: recorded, laid out, and reported with their condition. 7F does not evaluate conditions, because no plan owns a condition evaluator.
* Tests, all synthetic: body-level hidden content absent from layout; a nested hidden range inside a visible one; conditional and visible sections unchanged; page breaks inside hidden ranges do not flush pages; later visible elements retain original body indices; `DocumentTextProjection`/`DocumentTextWindows` and the editable source text remain stable. Sections contained inside table cells are explicitly unsupported and are not a 7F acceptance case.

**No fixture contains a hidden section:** all six `.odt` files have zero `text:display` attributes (any value) and zero `<text:section>` body elements in every XML part, so this item cannot move the page matrix. Its tests can only be synthetic.

**Withdrawn expectation (2026-10-04).** The first draft of this document, written in the lost session and recovered from `docs/01a10750-1f33-7425-b811-fda994620228.txt`, expected Sample-6 to move because it "declares the only `text:display` range". That claim was re-checked against the raw packages and is false; it is recorded here so the expectation is not reintroduced from the older draft. The recovered draft's other points are folded into this document: the sequential implementation order above, the DOCX `w:tabs` boundary below, and the non-goals in section 5.

## 4. Gates

* All twelve fixtures stay inside their recorded windows (`SampleMatrix.kt`) and **no count moves**. Leaders are painted inside an advance that is already reserved, and no fixture has a hidden section, so a movement is a defect to explain rather than a calibration.
* `Plan7dTableLayoutTest`, `PaginationFidelityTest` and the `Plan7e*` suites stay green.
* Hidden-section support is limited to the body-level section ranges represented by Plan 7C; sections contained inside table cells remain unsupported.
* No editing features: tab-stop editing, rulers, leader pickers and the FCT Tab settings dialog stay with their own plans (Plan 11 packages).
* No new claim about painting bundled fonts: that remains Plan 10 A1.

## 5. Out of scope and non-goals

DOCX `w:tabs` leaders and the DOCX half of the TOC entries (Plan 8A owns `w:pPr/w:tabs`; Plan 8B owns the DOCX TOC snapshot), field and TOC generation (Plan 8B / Plan 9), hidden *text* runs (`text:display` on spans, `w:vanish`), conditional-section evaluation, and sections contained inside table cells.

* No general ODF line-leader implementation (`leader-style` without `leader-text`), no `leader-color`, `leader-text-style` or `leader-width` styling, and no tab-stop editing UI (rulers, leader pickers, the FCT Tab settings dialog) which belongs to the Plan 11 packages.
* No TOC regeneration and no "Update Index" control; Plan 9 owns writing.
* No page furniture: `w:headerReference`/`w:footerReference` stay recorded and not rendered.
* No change to how the Navigator lists hidden sections: the Plan 7C behaviour stands, including graying and jumps that land on the nearest visible position.
* No new claim about painting bundled fonts: that remains Plan 10 A1.

## 6. Implementation closeout (2026-10-05)

Plan 7F was delivered in the one PR #33 using the two feature/test-gated commits required above:

| Commit | Delivery | CI gate |
|---|---|---|
| `65e6e3c` | ODF textual-leader import, model, measurement and display projection; `Plan7fTabLeaderTest` | Run `37310408444`: 355 tests across 65 suites, 0 failures/errors/skips; Unit Tests and Build passed. The focused leader suite passed 5/5. |
| `428ea38` | Body-level hidden-section layout using original element indices; `Plan7fHiddenSectionLayoutTest` | Run `37311218794`: 358 tests across 66 suites, 0 failures/errors/skips; Unit Tests and Build passed. Hidden-section tests passed 3/3 and leader tests remained 5/5. |

The final run's twelve-fixture matrix stayed unchanged from the post-7E baseline: ODT `14/23/21/10/18/20`, DOCX `15/25/25/11/19/24`; every count remained within its recorded `SampleMatrix` window. `PaginationFidelityTest` passed. Local Gradle execution was unavailable because this sandbox has no Java/JDK; verification is from GitHub Actions. PR #33 is open and not merged.
