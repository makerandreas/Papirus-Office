# Plan 7F: ODF tab leaders and hidden-section layout

**Date:** 2026-10-04. **Status:** planned, not implemented.
**Forecast:** one PR after Plan 7E (PR #31). The forecast slot moves with the order; the plan ID does not.
**Scope owner:** the owner approved this split when PR #30 landed (recorded in that PR's body: 7E owns font-face declarations, alias resolution and final calibration; 7F owns ODF tab stops/leaders and hidden-section layout). The first draft of this document was written in a session whose commits never reached a remote (audit-017 section 1); this version is rebuilt from the tree and the raw fixture packages and is labelled static where it states design rather than measured fact.
**References:** roadmap v2 section 4.7d items 5 and 6, `audit-015` section 9 (Plan 7C record), `audit-017` sections 4.3 and 4.4 (fixture re-derivation) and section 13.4 (the withdrawn `text:display` expectation).
**Depends on:** Plan 7E (the resolved family is an input to tab measurement). Plan 8A does not depend on this plan: 8A schedules after 7E, and 7F may run beside it or after it, as the owner prefers.

**Goal:** the authored leaders in the tabbed entries render, and content the file asks to hide is not laid out.

---

## 1. What already ships, so 7F does not rebuild it

Verified in the tree on 2026-10-04:

* **Positions and alignment.** `SvXMLImport.kt:797-806` reads `<style:tab-stops>` and `<style:tab-stop>`, maps `style:type` (`right`, `center`, `char` to DECIMAL, otherwise LEFT) and appends `ParagraphTabStop(positionUnits, alignment)`. `SvXMLImport.kt:496` reads `style:tab-stop-distance` into `defaultTabIntervalUnits`.
* **Model.** `TabAlignment` (`LEFT, RIGHT, CENTER, DECIMAL, CLEAR`) and `ParagraphTabStop(positionUnits, alignment)` at `OfficeDocument.kt:302-304`; `ParagraphStyle.defaultTabIntervalUnits` defaults to `48f` (`OfficeDocument.kt:346`) and the field participates in the cache key at `LayoutEngine.kt:165`, so a tab change invalidates measurement.
* **Layout.** `ParagraphMeasurer.tabAdvance` (`:57-90`) walks the sorted stops, applies RIGHT, CENTER and DECIMAL offsets from the text that follows the tab, skips `CLEAR` stops (and steps past a cleared position), honours the style interval, and falls back to `48f` when the style sets none. It is consumed twice, while choosing a break (`:92`) and while measuring the line (`:120`).

**Missing:** `style:leader-style` (ODF 1.4 Part 3 section 19.489) and `style:leader-text` (section 19.490) are read nowhere (`grep -rn "leader" app/src/main/java` returns no hit), `ParagraphTabStop` has no leader field, and nothing paints a leader.

Normative shape of the two attributes, re-read in `docs/odf/OpenDocument-v1.4-part3-schema.html` for this document:

* `style:leader-style` styles a leader **line** (values include `none`, `dash`, `dot-dash`, `dot-dot-dash`, `dotted`), and `style:leader-type` (19.492) says whether a line is drawn at all (`none`, `single`, `double`). Both attach to `<style:tab-stop>` (17.8).
* `style:leader-text` is a **single character** used as a textual leader, data type `character` (18.3.7); the section says a consumer that cannot render the requested character should display one it supports. `style:leader-char` (19.487) is a different attribute and belongs to `<text:index-entry-tab-stop>` (8.13.6), not to `style:tab-stop`.

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

1. `ParagraphTabStop` gains a leader. It carries what the file declared: the character from `style:leader-text` and the line style from `style:leader-style`/`style:leader-type`, either of which may be absent. The default keeps every existing construction site source-compatible.
2. `SvXMLImport` reads the three attributes on `<style:tab-stop>`. Precedence follows the file: a declared `style:leader-text` character is the glyph; when it is absent and `style:leader-style` is a pattern other than `none`, the line pattern is drawn; `style:leader-type="none"` suppresses the leader; an unsupported declared character falls back to a supported one as 19.490 instructs, and the fallback is recorded, not silent.
3. `ParagraphMeasurer` keeps the current advance arithmetic unchanged and records, per line, the leader runs: the gap between the tab origin and the resolved target position, which the renderer fills. A gap narrower than one leader glyph renders nothing.
4. The renderer paints the leader inside that gap through the same resolved family and style the paginator measured with. No second implementation, no renderer-only table (plan-01 section 5 invariant 1).
5. Tests: parse per sample; gap arithmetic for LEFT, RIGHT, CENTER, DECIMAL and `CLEAR`; the TOC entries in Samples 2, 4, 5 and 6 show the declared leader text; a tabbed line still ends at the same advance as before the leader is painted.

## 3. Item 2: hidden-section layout

Plan 7C records `SectionDisplay.HIDDEN` on `DocumentSectionRange` (`DocumentSemantics.kt:69-79`) and keeps Navigator rows and jumps out of hidden ranges (`DocumentIndexEngine.kt:410,414`), but no layout path reads `namedSectionRanges`: the paginator's consumers use paragraphs and elements only, so hidden text is still placed. 7F excludes hidden ranges, and everything nested inside them, from layout.

* The exclusion happens in one place, decided by the same `DocumentSectionRange` values Plan 7C records, so the renderer and the Navigator cannot disagree.
* Conditional sections (`SectionDisplay.CONDITIONAL`) keep their current treatment: recorded, laid out, and reported with their condition. 7F does not evaluate conditions, because no plan owns a condition evaluator.
* Tests, all synthetic: hidden content absent from layout; a nested hidden range inside a visible one; a hidden range inside a table cell; conditional and visible sections unchanged.

**No fixture contains a hidden section:** all six `.odt` files have zero `text:display` attributes (any value) and zero `<text:section>` body elements in every XML part, so this item cannot move the page matrix. Its tests can only be synthetic.

**Withdrawn expectation (2026-10-04).** The first draft of this document, written in the lost session and recovered from `docs/01a10750-1f33-7425-b811-fda994620228.txt`, expected Sample-6 to move because it "declares the only `text:display` range". That claim was re-checked against the raw packages and is false; it is recorded here so the expectation is not reintroduced from the older draft. The recovered draft's other points are folded into this document: the scheduling freedom above, the DOCX `w:tabs` boundary below, and the non-goals in section 5.

## 4. Gates

* All twelve fixtures stay inside their recorded windows (`SampleMatrix.kt`) and **no count moves**. Leaders are painted inside an advance that is already reserved, and no fixture has a hidden section, so a movement is a defect to explain rather than a calibration.
* `Plan7dTableLayoutTest`, `PaginationFidelityTest` and the `Plan7e*` suites stay green.
* No editing features: tab-stop editing, rulers, leader pickers and the FCT Tab settings dialog stay with their own plans (Plan 11 packages).
* No new claim about painting bundled fonts: that remains Plan 10 A1.

## 5. Out of scope and non-goals

DOCX `w:tabs` leaders and the DOCX half of the TOC entries (Plan 8A owns `w:pPr/w:tabs`; Plan 8B owns the DOCX TOC snapshot), field and TOC generation (Plan 8B / Plan 9), hidden *text* runs (`text:display` on spans, `w:vanish`), and conditional-section evaluation.

* No rewriting of the tab-stop model beyond adding the leader fields, and no tab-stop editing UI (rulers, leader pickers, the FCT Tab settings dialog) which belongs to the Plan 11 packages.
* No TOC regeneration and no "Update Index" control; Plan 9 owns writing.
* No page furniture: `w:headerReference`/`w:footerReference` stay recorded and not rendered.
* No change to how the Navigator lists hidden sections: the Plan 7C behaviour stands, including graying and jumps that land on the nearest visible position.
* No new claim about painting bundled fonts: that remains Plan 10 A1.
