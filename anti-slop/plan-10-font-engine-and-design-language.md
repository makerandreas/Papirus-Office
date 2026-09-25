# Plan 10 — Font Rendering Engine and UI Design Language

**Date:** 2026-09-24; scope note updated 2026-09-25
**Status:** **document-font thread parked on purpose.** The user's decision: prioritize document fidelity, with old PR D (bundled Typeface and substitution) and old PR E (Font Style UI, SAF/user fonts, curated Google Fonts) held for later. The broader hybrid UI source map is now defined in `anti-slop/plan-11-hybrid-experience-design.md` and supersedes this plan's narrower Thread B framing. Plan 11 does not authorize the UI implementation by itself; re-scope before coding.
**Why the document-font thread remains separate:** Plan 5 introduces `FontRegistry` as a substitute-or-system map, and Plans 7/8 preserve imported family identity. Actual document-face loading and metrics parity still need the document fidelity dependencies. This must not block independent UI shell/theme work.
**Evidence:** `anti-slop/audit-004-2026-09-24.md` (D/E recorded as later bonus work), `anti-slop/audit-006-2026-09-24.md` §2.2 (design-system compliance), F-08/F-09 in `audit-005` (document family fidelity), and Plan 11 (updated product UI direction).

---

## 1. Why this plan exists as one unit

Two threads were always entangled, and separating them is what made the earlier plan hard to schedule:

* **Thread A, the font engine.** Which face actually draws a run of text, and which face supplies its metrics. Today the answer differs between the paginator (an Android `Paint` at a fudged size) and the renderer (Compose `sp` text through a coarse family switch), and the bundled fonts in `assets/fonts` are extracted to `/storage/emulated/0/Fonts` but never used for display. Until one face supplies both, no fidelity target can be met, which is why this plan is *downstream* of Plan 5 and *upstream* of any "matches M365" claim.
* **Thread B, the design language.** How the app presents typography controls (Font Style subpage, font size stepper, per-style previews) and how far the app's own chrome follows `DESIGN.md` and Material 3 Expressive.

They belong together because the font picker is the visible face of the font engine: shipping the engine without the UI leaves the user unable to see the result, and shipping the UI without the engine would present choices that change nothing.

---

## 2. Thread A — font engine

### A1. Bundled faces become the real substitutes (`DESIGN.md`, `AGENTS.md` module colours)
`assets/fonts` already ships metric-compatible substitutes: Liberation Serif (Times New Roman), Liberation Sans (Arial/Helvetica), Liberation Mono (Courier New), Carlito (Calibri), Caladea (Cambria), Gentium (GenBas/GenBkBas), OpenSymbol (`opens___.ttf`), plus a remote `fonts.json` for Roboto and Open Sans. Plan 5's `FontRegistry` maps names to families; this plan makes it load **actual `Typeface`s** from those files (`FontFamily(Typeface)` on device, a descriptor table on JVM), so:
* "Times New Roman" renders as Liberation Serif in both pagination and painting (closes F-09);
* Symbol and Wingdings map to OpenSymbol, which is the missing piece behind the emoji/Wingdings residue noted in audit-003;
* the glyph cache is shared between measure and draw, so a run's width cannot differ between the two.

### A2. Font substitution policy, written down
Order: exact family match → metric-compatible substitute from `assets/fonts` → user-supplied font directory (`/storage/emulated/0/Fonts`, already implemented by `FontProvider`) → system family (`serif`/`sans-serif`/`monospace`) → `FontFamily.Default`. The same order must be used by the paginator and the renderer; a unit test asserts the two lookups return the same family for a 30-name sample drawn from the test corpus.

### A3. Font Style UI (`AGENTS.md` "Font Style: font family picker with typography preview")
The subpage exists; it needs to (a) list only families that resolve through A2, (b) preview each name **in its own face** at the current size, (c) show the substitution when a document asks for a face that is not present ("Times New Roman → Liberation Serif"), and (d) write the *document's* family name, never the substitute's.

### A4. SAF and user fonts (`AGENTS.md` "Font management: Google Fonts on demand + `/storage/emulated/0/Fonts`")
Already half-built by `FontProvider`; remaining work is the Options entry that points at the directory, a refresh action, and a graceful message when a font file fails to load. Curated Google Fonts download stays opt-in and must not be presented as a document requirement.

### A5. Metrics parity test
For Sample-6.odt's `Normal` (Times New Roman 12 pt, line-height 116 %, margin-bottom 0.282 cm), assert that the measured line height and the drawn line height agree within 2 %, and that the measured advance width of a fixed sentence differs by less than 3 % from the face actually drawn. This is the test that makes "fidelity" a number rather than an impression.

---

## 3. Thread B — UI design language

| # | Item | Rule |
|---|---|---|
| B1 | Reconcile `DESIGN.md` with the current Material 3 Expressive guidance for the four component families this project leans on: tonal bottom bar, FAB role (creation vs mode switch), the 40 % standard bottom sheet deck, and the full-page dialog header (`← Back | Title` / `Apply | ⋮`) | `DESIGN.md` is direction, m3.material.io is reference; deviations get written into `DESIGN.md` rather than silently fixed |
| B2 | Typography scale audit: the app uses Google Sans names in `DESIGN.md` while the screenshots show Roboto-class rendering | pick the shipped face, record the fallback, and make the scale (Display LG 40/475 …) match real `sp` values in code |
| B3 | Token hygiene: finish the Plan 3 item 3.10 sweep so no chrome colour bypasses the M3 token set | `DESIGN.md:384` |
| B4 | Empty-state shapes per module (already required by `DESIGN.md`) and the empty-state language for the six unimplemented ribbon tabs from Plan 3 item 3.6 | `antislop` R-27, R-26 |
| B5 | Motion: confirm the slide-in transitions and predictive back required by `CONCEPT.md` (Phase 4 "Global Transitions") actually exist, and record the dial values used (ENERGY 2 / RHYTHM 1 / MOTION 3) | `DESIGN.md` dials |
| B6 | One Delivery Gate run over the whole Inky screen after B1-B5, with device screenshots at 320 dp and a tablet width | `antislop` R-35 |

---

## 4. Dependencies, and what must not be re-decided

* **Depends on** Plan 5 (`FontRegistry`, `TextMetrics`, real paragraph metrics) and benefits from Plans 7 and 8 (which supply the family names that arrive from ODF and OOXML respectively).
* **Must not re-decide:** the colour conventions in `AGENTS.md` (Suite `#2563EB`, Inky `#0F9D58`, Cellina `#16A3B7`, Slidia `#F59E0B`, Pagella `#D93025`); the 48 dp rule; en_US + `strings.xml`; no unbranded colours for chrome.
* **Explicitly out of scope:** embedding fonts *inside* saved documents (ODF `office:font-face-decls` with `svg:font-face-src`, OOXML `w:embedRegular`). Plan 9 will keep those declarations intact on round trip, but Papirus will not author them until a licensing review exists for each bundled face.

---

## 5. Acceptance

1. A document asking for Times New Roman renders Liberation Serif, and the same face supplies its metrics (A1, A5 tests green).
2. The Font Style list previews each family in its own face and states substitutions (A3).
3. A font dropped into `/storage/emulated/0/Fonts` appears in the list after a refresh and survives a restart (A4).
4. `DESIGN.md` matches the shipped typography scale and component decisions, with deviations recorded (B1-B3).
5. Delivery Gate PASS with device evidence at two widths (B6).

**Size:** medium to large; A1-A2 are the critical path (2-3 days), A3-A4 the UI (3-4 days), B1-B6 a separate documentation-plus-polish PR (2-3 days). Land A before B, because B's screenshots are the proof that A worked.
