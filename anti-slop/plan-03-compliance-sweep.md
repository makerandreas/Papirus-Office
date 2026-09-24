# Plan 3 — Compliance Sweep

**Date:** 2026-09-24
**Status:** ready to start, parallel with Plan 2. No dependency on the fidelity plans; two items are gated by them and are marked.
**Evidence:** `anti-slop/audit-006-2026-09-24.md` §2 (project documents) and §3 (format specifications). Counts quoted there are reproducible greps.
**Rule of the plan:** fix against the documents, not against taste. Each item cites the clause it violates and is done when the cited grep count reaches zero or the cited spec behaviour is demonstrated by a test.

## 0. Decisions taken by the user (2026-09-24)

These answered the open questions of `audit-005` §5 and `audit-006` §5. They are binding for every item below, so a fresh session does not have to ask again.

| Question | Decision | Consequence for this plan |
|---|---|---|
| Localization policy | **en_US everywhere.** One language in the UI. | 3.1 and 3.2 move every literal into `values/strings.xml` in en_US. 3.3 replaces the hardcoded Indonesian copy with en_US resources instead of adding translations. `values-in/` keeps only the keys it already has; it is not extended, and no new string gets an Indonesian sibling. Document content (style names such as `Judul1`) is data and stays untouched. |
| Ribbon tabs without an implementation | **Disabled tab plus an honest note on press** (options 2 and 3 of the question combined): the tab stays visible, renders in a disabled state, and a press tells the user plainly that the deck is not in this build. | 3.6. The tab *set* comes from `CONCEPT.md`, which lists the planned tabs for every module except Pagella. For Writer that list is File, Home, Insert, Layout, Review, View plus the conditional tabs (Drawing, Object, Picture, Table, Fontwork, Chart). `InkyModule.kt:2935` declares eight tabs, and two of them (`References`, `Mailings`) are **not** in `CONCEPT.md` at all, so the same item has to decide between renaming and dropping them while the list is rebuilt. |
| Plan order | Merge Plan 2 before Plan 3; Plan 3 runs in a fresh session. | Plan 2 landed as PR #11. Nothing in this plan depends on it except the two items already paid down below. |

---

## 1. Localization (violates `AGENTS.md` "translate all strings to `en_US` and add to `strings.xml`")

| # | Item | Current | Target |
|---|---|---|---|
| 3.1 | `contentDescription` literals | 231 repo-wide, 72 in `modules/inky` | every one moves to `strings.xml` **in en_US only** (§0); a lint rule or a CI grep in the unit-test job fails on new ones |
| 3.2 | `Toast` literals | 316 toasts repo-wide, 57 in `InkyModule`, 49 with a literal (counts are pre-Plan-2: the `"Edit Mode Active"` toast is already gone) | literals move to `strings.xml` **in en_US only** (§0); toasts that report a state change become M3 snackbars (see 3.5) |
| 3.3 | Indonesian copy in user-visible strings | `InkyModule.kt:200` default title `Draft Dokumen Baru`; `:3247/:3822` default file names `Inky_Dokumen.*`; `PapirusConfigManager.kt:382` reset message; `HomeDashboard.kt:1273` button label; `InkyModule.kt:3440/:3451` (`Ukuran font diubah ke ...`, `Menempelkan sebagai ...`) | per §0: **replace with en_US copy in `values/`**, do not add `values-in` entries for them. Document *content* (style names like `Judul1`) stays untouched because `NavigatorStringCatalog` treats it as data, which is correct |

Not in scope: the Indonesian strings that exist because a *document* uses them (Navigator recognition). That behaviour is deliberate (P2-2) and stays.

## 2. Copy (violates `antislop` R-02, hard gate)

| # | Item | Fix |
|---|---|---|
| 3.4 | em dash in user-visible copy: `UniversalEmailSheet.kt:461` (Toast), `PapirusEmailEngine.kt:139,208` (log lines rendered in the email transcript), `LokitEngine.kt:29` (engine status shown in the Inky diagnostics list) | replace with comma, colon, or parentheses; keep the meaning. `strings.xml:160` is an XML comment and is documentation, not copy, so it may stay (note it in the PR body) |

## 3. Honesty and dead controls (violates `antislop` R-26, R-27; overstates `AGENTS.md` screen list)

| # | Item | Current | Fix |
|---|---|---|---|
| 3.5 | Four Toolbar Hub tools do nothing | "Add image" and "Add table" toast only (`InkyModule.kt:2695-2704`); link and comment are the same | either implement (Plans 6/7 own the insertion paths) or mark the control with a visible "Coming in a later build" state; a bare toast that says the tool was "selected" is the exact anti-pattern R-26 names |
| 3.6 | Six of eight ribbon tabs are empty | `InkyModule.kt:2935` declares `File, Home, Insert, Layout, References, Mailings, Review, View`; only `File` (`:3282`) and `Home` (`:3351`) have content, the rest fall through to a literal "$currentTabName options will be implemented soon." (`:3390`) while the tab still looks fully live | per §0: rebuild the list from `CONCEPT.md`, render the not-yet-implemented tabs **disabled** with an accessible reason, and have a press raise the honest note as a string resource. Also decide the fate of `References` and `Mailings`, which `CONCEPT.md` does not list for Writer |
| 3.7 | Google Drive screen is a placeholder presented as a feature | `HomeDashboard.kt:1210-1276`; `AGENTS.md` and `PROJECT_CONTEXT.md` list it as an integrated screen | keep the honest placeholder, correct the wording in *both* documents to "placeholder, not yet implemented", and localize the button label |
| 3.8 | Zero `TODO`/`FIXME` in `app/src/main/java` | all stubs are invisible in source | every intentional stub gets a labelled marker so the next sweep finds them |

## 4. Design system (violates `DESIGN.md`)

| # | Item | Fix |
|---|---|---|
| 3.9 | 32 dp zoom targets (`InkyModule.kt:2439,2454` in the old bar) | **Inky done in Plan 2**: the -/+ pair is gone, the zoom control is one 48 dp target. Still open in the other modules: `CellinaModule.kt:800,811` (12 dp icons), `SlidiaModule.kt:904,915`, `PagellaModule.kt:106,110`. Target everywhere: 48 dp touch target, visual size may stay smaller inside the box; `DESIGN.md:379` |
| 3.10 | Chrome greys outside the token set and below contrast floor | **page-stack part done in Plan 2**: table grid lines now use `outline`/`outlineVariant`, cell text uses the document text colour, `[Image]` uses `onSurfaceVariant`, and the `Color.Gray` page counters were deleted with the per-card marker. Remaining: the same pattern in `CellinaModule`, `SlidiaModule`, `PagellaModule` and `HomeDashboard`. Keep the *page* white/black (that is paper, not chrome) |
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

---

## 8. Implementation record (PR 12 = plan 3A, branch `arena/01a0d161-papirus-office`)

What landed for items 3.1-3.4 and 3.8, per the roadmap's PR 12 scope. CI is the compile evidence (no local JDK).

| Item | State | Notes |
|---|---|---|
| 3.1 `contentDescription` literals | done | 226 named-arg literals moved to `values/strings.xml` as `cd_*` resources; `stringResource(...)` at every site (all were `Icon` params in composable scope; no semantics blocks existed) |
| 3.2 `Toast` literals | done | 282 literal toasts moved: non-parameterised ones use the `R.string` `makeText` overload, parameterised ones `context.getString(res, args)` with `%1$s`-style positional placeholders (argument order preserved through renumbered placeholders) |
| 3.3 Indonesian copy | done | `Draft Dokumen Baru` → `Untitled Document`; save-root `Inky_Dokumen.*` → `Untitled.*`; the font-size and paste-special toasts, the settings-reset popup, the Drive button and the `Ubah Ukuran Font` label/description all became en_US resources; the simulated `documentLoad` diagnostics entry now says `Untitled.odt`. `values-in` keeps exactly its 27 keys |
| 3.4 em dashes | done | the SMTP-simulation toast, the two email-transcript `addLog` lines and the engine status label use colon/comma instead; the `strings.xml` comment stays (documentation, named here per the plan) |
| 3.8 stub markers | done | `TODO(plan-6/7/8)` on the four toast-only hub tools, `TODO(plan-3B)` on the unimplemented ribbon decks, `TODO(unassigned)` on the Drive placeholder |
| guard | done | `SourceHygieneGuardTest` (plain JUnit, runs in `testDebugUnitTest`): comment-masked scan of `app/src/main/java` failing on literal toast messages, literal `contentDescription`s, raw `Color.Gray/DarkGray/LightGray` above the recorded per-file allowances, em dashes in string literals, and resurrected 3.3 literals; self-test proves all five rules fire on synthetic offenders and stay silent on commented-out code and format-arg quotes |

**Carve-out recorded:** 47 raw grey occurrences across 14 files sit in the guard's allowance map at PR-12 counts (plan 3B's token sweep burns them down; the HomeSubpages picker palette counts as document colours per 3.11).

**CI evidence:** run `35953418179` on `8a32e62` — both jobs green (Build Debug APK nightly + Unit Tests, so the guard test compiles and passes). Three review-fix commits after the initial push: missing `com.example.R` imports in the three contentDescription-only files plus two guard-test source bugs (raw-string regex swallowing its closing quotes; `$count` interpolating in a synthetic fixture); a string-vs-`Int` ternary in the frozen-pane toast (CellinaModule); escaped literal quotes in the two parameterised failure strings. A temporary CI diagnostics step that re-emits failing gradle lines as check annotations (used because the sandbox cannot download action logs) was removed after the failure was read — workflow restored to original.

**Sweep method note:** the extraction was scripted (paren-aware argument parsing, interpolated literals converted to positional format args, duplicate templates deduped against the existing 283 keys); every transformation was verified by a guard port before commit. 3.5-3.7, 3.9-3.12, 3.28 remain for PRs 13-14; 3.13-3.27 stay with their owning fidelity plans.

## 9. Implementation record (PR 13 = plan 3B, branch `arena/01a0d3c8-papirus-office`)

What landed for items 3.5, 3.6, 3.7, 3.9, 3.10, 3.11, 3.28 and the two ⚑ items (3.32 Navigator categories, 3.33 status-bar object information), per roadmap v2 §4.1. CI is the compile evidence (no local JDK).

| Item | State | Notes |
|---|---|---|
| 3.5 Hub tools | done | the four toast-only tools (image, table, link, comment) render at 38 % on-surface tint, their `cd_add_*` strings now name the state ("Add Image, not available in this build"), and a press raises `toast_add_*_unavailable`; the four `toast_add_*_selected` strings are retired (zero references) |
| 3.6 Ribbon | done | `WriterRibbonModel.kt` declares the six `CONCEPT.md` Writer tabs (File, Home, Insert, Layout, Review, View); **References/Mailings dropped**, not hidden; the pager hosts `WriterRibbonTab.withDecks` and maps a tab to its page through `pageOf`, so page index and strip position are never assumed equal; unimplemented tabs are dimmed 38 % with a hoisted `stringResource(cd_ribbon_tab_unavailable, label)` semantics reason and answer a press with `toast_ribbon_tab_unavailable`; `activeRibbonTab` state deleted; the "will be implemented soon" literal is gone |
| 3.7 / 3.28 Drive | done | placeholder screen kept honest (button raises the placeholder notice); `AGENTS.md` and `PROJECT_CONTEXT.md` now say "placeholder, not yet implemented" and the `AGENTS.md` ribbon section lists the six real tabs with which decks exist |
| 3.9 48 dp targets | done | `CellinaModule.kt:800,811` and `SlidiaModule.kt:904,915` zoom pairs → `Modifier.size(48.dp)` with the 12 dp glyphs unchanged; **Pagella `:106,110` needed no change** — those `IconButton`s carry no size override, so Material 3's minimum already gives 48 dp (the audit line was a line count, not a measurement) |
| 3.10 Chrome greys | done | all **46 occurrences in 13 files → 0**; Material tokens where the surface is chrome; explicit ARGB document colours with a comment where the surface is a pinned document preview (Cellina's sheet grid, Pagella's paper, Slidia's black slideshow and white slide canvases, and the picker palette per 3.11). The three diagnostic consoles moved to `ui/theme/TerminalPalette.kt` (fixed terminal colours, documented as an intentional non-token exception with contrast figures). `SourceHygieneGuardTest`'s allowance map is **empty** (all 14 entries deleted, including the stale `LayoutDrivenDocumentRenderer` one that masked a comment) |
| 3.11 Palette | done | the picker keeps the exact Material-2014 hues as literal `Color(0xFF…)` document colours with one comment recording the decision |
| ⚑3.32 Navigator categories | done | `data/navigation/NavigatorCategoryHonesty.kt` (15 keys: 5 readable or session state, 10 `NOT_READABLE_YET`) plus `NavigatorEmptyRow` in the sheet: a category a parser can see says "No %1$s in this document.", one no parser can see says "Not yet available in this build." and carries a `TODO(plan-NN)` marker. **Hyperlinks** category added in the honest shape (rows and jump land with the parser in 18/20; no dead filter entry). The **Indexes** filter no longer claims the document has none. Counts are suppressed (`count = null`) where no count can be computed. The `NavigateBy.INDEX` arm, the ternary `contentDescription`s and the `"Hidden"` label were guard-invisible literals and are now resources (`cd_collapse`, `cd_expand`, `navigator_item_hidden`) |
| ⚑3.33 Status-bar object info | done | the centre slot shows `statusBarObjectInfo ?: wordsCharsText`: heading level + text (resolved through the Navigator index, so paragraph-styled headings count), "Table", "List item", and nothing invented for row/column, sections, frames or indexes (19/21). The caret mapping reuses `DocumentTextWindows.elementForOffset`, the same seam the toolbar hub uses. The leading page counter's geometry is untouched (320 dp), and PR 15's E-7 fit-scale region was deliberately not touched |
| Tests | done | `WriterRibbonTabsTest` (deck/availability/deck-count/page mapping, References/Mailings absent) and `NavigatorCategoryHonestyTest` (re-derives the readable/unreadable split from the source tree, asserts the sheet only uses declared keys and every declared key is rendered); `SourceHygieneGuardTest` green with zero allowances |

**Review note:** the inherited work-in-progress carried two defects that reading could catch but no local compiler could: a doubled `else` in the ribbon pager (`if`/`else`/`else`, which cannot compile) and a `Modifier.semantics { contentDescription = <nullable> }` that leaned on a smart cast into a non-composable lambda. CI caught a third, this PR's own: the new document-colour vals were inserted between `@OptIn(ExperimentalMaterial3Api::class)` and the composable in `CellinaModule`/`SlidiaModule`, which detached the opt-in and turned every `TopAppBar` in those files into an un-opted experimental API (the `WriterRibbonModel` companion's unqualified `entries` lookup was fixed in the same pass). The vals now sit above the annotation. Pre-push evidence for a tree with no local JDK: the Python port of the guard (all five rules), a source-derived re-run of the two new tests' assertions, an `R.string` reference-vs-declaration scan, and a comment/string-masked brace-balance pass over every touched file.

**CI evidence:** run `36016012598` on `4c91322` — both jobs green (Build Debug APK + Unit Tests, so the guard and the two new tests compile and pass, and `assembleDebug` succeeds). The two earlier runs on this branch failed at `compileDebugKotlin`; their failure lines were read through a temporary diagnostics step that re-emits gradle errors as check annotations (this sandbox cannot download action logs). The step is removed again and `.github/workflows/build.yml` is byte-identical to `55a9a97`.

**Method note:** the sweep was done in place plus scripted passes; no local toolchain exists, so CI (`./gradlew testDebugUnitTest`) remains the only compile evidence.
