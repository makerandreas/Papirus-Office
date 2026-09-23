# Papirus Office: Plan for the remaining audit-003 items (P0-1, P0-2, P1-3, P1-4)

**Date:** 2026-09-22 (pre-change gate for the four open items)
**Status:** PR A (P0-2) merged as #7 (CI green, run 35742199462); it also carries the D3 unit-scale fix, D4 engine injection, and the first half of D1 (card aspect from page box). The D2 `contains("b")` bold bug stays deferred into PR C unless a hotfix is requested. PR B is delivered as two drops per user call on 2026-09-23: **PR B1 = #8** merged (structural: single page source, one-model Editor, image interleave, merger fix; CI green, run 35838683919; Sample-5 window measured in CI within 12..30). **PR B2** is 4 commits on `arena/01a0cd95-papirus-office` pending CI: S3 `AnnotatedString` runs (new `OfficeRuns` mapper + renderer rewrite, card sizing kept per user call), S5 Viewer selection via read-only per-paragraph fields on the shared `docBodyText` model (plan's `SelectionContainer` cannot expose its range to the custom FCT; user-approved deviation; the existing Viewer compact FCT branch covers Copy/Select All with zero toolbar changes), S7 guards (OfficeRuns/toGlobalSelection unit tests, Sample-5 determinism guard, both CI deprecation warnings cleared), and S6 hyphenation as the last commit (pure-Kotlin TeX `.hyph` parser over the bundled `en_us.hyph`, optional `LayoutEngine` engine, default-off DataStore toggle under Options -> Writing Aids; byte-identical wrap when off). Device gate (C3 of the plan) runs before merge. PR C (P1-4 + D2) follows. Approved calls on record: 3-PR order with B split B1→B2; single-PR B2 with 5 logical commits (S3, S5, S7, S6, plan status); `partitionTextToPages` kept as test stub (done in B1, `PartitionTextToPagesStub.kt`); hyphenation wired in PR B2, default off; S3 keeps current card sizing (geometric single-transform card = named follow-up ticket); antislop applied during (Mode 1).
**Branch:** `arena/01a0c97e-papirus-office`
**Depends on:** `anti-slop/audit-003-2026-09-22.md`, merged PR #5 (CI + `OdtSampleHeadingTest`), merged PR #6 (P1-1, P1-2, P2-1, P2-2, P0-3)
**Constraint:** no JDK in this sandbox (verified: `java` not found). All compile/test evidence comes from CI through `gh` (GitHub API approach, per `AGENTS.md` JNI notice).

---

## 1. Re-verification against current `main` (HEAD `3f75bec`)

Every audit claim was re-checked against the code after PR #5/#6 landed.

| Audit item | Still open? | Evidence in current code |
|---|---|---|
| **P0-1** unify Viewer/Editor stacks | **Yes** | `InkyModule.kt:101` still defines `partitionTextToPages`; `:529` still feeds `pagesList` into the Editor branch (`~L2273+`): per-page `BasicTextField` with its own `pageTextVal`, joined back with `joinToString("\n\n")`. Global single `textStyle` from `activeFontFamily/Size/Bold/Italic`. Image `firstOrNull()` on page 0 only (`~L2290`). Viewer branch (`~L2254`) renders `LayoutDrivenDocumentRenderer`. |
| **P0-2** honor `style:page-layout` / `fo:margin` | **Yes** | `LayoutEngine.kt:93` `LayoutEngine(pageWidthDp = 816f, pageHeightDp = 1056f)`; `InkyModule.kt:506` constructs `LayoutEngine()` with defaults, no document input. Fixed `50f` top, `60f` bottom reserve, `marginX = 40f`, `12f` gaps, `maxLineWidth = pageWidthDp - 80f` inside `performLayout`. `SvXMLImport.parseOdfStyles()` reads only `style:name/family/parent-style-name/display-name/default-outline-level`; it ignores `<style:page-layout>`, `<style:page-layout-properties>` and `<style:text-properties>`/`<style:paragraph-properties>` children. `OdfXmlToken` already has `XML_PAGE_LAYOUT`, `XML_PAGE_LAYOUT_PROPERTIES`, `XML_FONT_SIZE`, `XML_TEXT_ALIGN`, `XML_LINE_HEIGHT`, `XML_MARGIN_*`: the tokens exist, the handling does not. |
| **P1-3** `SelectionContainer` FCT | **Yes** | `SelectionContainer` appears nowhere in `app/src`. `customTextToolbar` is provided root-wide via `LocalTextToolbar provides customTextToolbar` (`InkyModule.kt:1902`), so the plumbing exists: a selectable Viewer would call `showMenu()` on the real toolbar with no new wiring. Editor per-page `BasicTextField`s are the reason FCT binds unreliably on imported docs (each sheet owns its own value; merged back only by text join). |
| **P1-4** ODF fidelity | **Yes** | `SvXMLImport.toDocumentStyles()` emits `ParagraphStyle(name, parentStyleName)` with default `fontSizeSp = 12f`: no size, no family, no alignment from the file. `StyleResolver` then falls back to the 24/20/16/18 heuristic, which is why headings are "almost right". Runtime `.odt` path is `OfficeDocumentParser.kt:677` -> `SvXMLImport` (the older `OdtDocumentParser` that does read `fo:font-size`/`fo:font-name`/`fo:text-align` is used by `DocumentSerializer` only, so its richer parsing never reaches the UI). |
| P2-3 CI asserts (18 ± 3 pages, image round-trip) | Still open | `OdtSampleHeadingTest` has heading-count asserts only; no `pages.size` guard. See "decision D4" below: recommend folding into PRs A/B instead of a separate PR. |

## 2. New findings from this pass (not in audit-003)

These were found while verifying and change the design of P0-1/P1-4:

- **D1: Viewer displays a different wrap than the engine paginates.** `LayoutEngine` measures in its own world (`fontSizeSp * 2.5f` units on an 816-wide page) but `LayoutDrivenDocumentRenderer.ParagraphText` renders `Text()` inside a fixed `320*zoom × 452*zoom` dp card with `24*zoom` padding and `13sp` body text. The card aspect (0.708) does not equal the layout aspect (816/1056 = 0.773), so displayed line breaks, overflow and clipping are decoupled from the pagination the Navigator and status bar report. **A unified stack must define one transform**: canonical layout page width/height, one scale factor (`zoom` and card-size derived from `PageLayout.widthDp/heightDp`), and text rendered from `LineLayout`/`AnnotatedString` produced by the engine, not re-wrapped by `Text()`.
- **D2: `OdfSpanContext` guesses run formatting from style-name letters.** `SvXMLImportContext.kt:296-298`: `styleName.contains("b", ignoreCase = true)` marks a span bold for any style containing the letter *b* (e.g. "Tabel", "TableContents"); `contains("i")` and `contains("u")` do the same for italic/underline. This fabricates formatting on import and must be replaced by real character-style resolution (belongs in P1-4, but small enough to hotfix).
- **D3: two length scales in ODF import.** `OdfFrameContext.parseDimensionToDp()` (`SvXMLImportContext.kt:600-610`) converts `in` to 160 units/in, while `LayoutEngine`'s world implies 96 (816 = Letter @ 96). P0-2 must pick one canonical unit system for the page-layout path (recommend: layout units = CSS px at 96/in, so A4 = 793.7 × 1122.5) and reuse one converter in both places.
- **D4: the renderer instantiates its own `LayoutEngine`.** `LayoutDrivenDocumentRenderer.kt:42` `remember { LayoutEngine() }` is used for `hitTest` even when `layoutResult` is injected. Once page geometry is per-document (P0-2), a default engine here silently diverges; the renderer should receive the engine (or geometry) from the caller.
- **D5: default font sizes disagree.** `StyleResolver.resolveParagraphStyle(null)` returns 14f; `ParagraphStyle` default is 12f; Editor text renders at `activeFontSize`. One constant, defined once.
- **D6: per-page join/trim drift.** Editor builds pages with `trim()` per page and rejoins with `"\n\n"`; `DocumentTextMerger.splitBlocks` splits on `"\n\n"`. Consecutive empty paragraphs therefore shift between `pagesList` and the merged model. Retiring `pagesList` removes the whole class of drift; until then it explains some "text moves between pages" reports.

## 3. Proposed sequencing: three PRs, smallest-foundation first

Order rationale: P0-2 first because it only changes what `LayoutEngine` consumes and is independently testable; P0-1 then rewrites the consumer UI while the acceptance asserts from the start; P1-4 last because fidelity parsing is only *visible* once the unified renderer consumes style data (and its run round-trip rides on P0-1's `AnnotatedString`).

### PR A: P0-2, page geometry from the document (S, ~1 day)

Scope:
1. `data/odf/SvXMLImport.kt`: extend `parseOdfStyles()` to also consume `<style:page-layout>` (+ child `<style:page-layout-properties>`) and `<style:default-style style:family="page">`. Extract `fo:page-width`, `fo:page-height`, `fo:margin-top/right/bottom/left`, `style:print-orientation` into a new `OdfPageLayoutInfo(name, widthDp, heightDp, marginsPx..., landscape)` map, resolved via the master page of the first (`standard`) `<office:text>`-bearing `<style:master-page style:page-layout-name=...>` when present (ODF 1.4 Part 3, §17/§19 semantics; consult `sources/OpenDocument-v1.4-part3-schema.html`).
2. Shared `LengthUnits` converter (fix D3): CSS px @ 96/in; `cm`/`mm`/`in`/`pt`/`pc` single implementation; used by both the new page-layout path and `OdfFrameContext`.
3. `data/OfficeDocument.kt`: `DocumentStyles` gains `pageStyles: Map<String, PageStyleSpec>` + `defaultPageStyle: PageStyleSpec?` (new small data class; carries width/height/margin dp + orientation).
4. `data/LayoutEngine.kt`: constructor takes `PageSpec` (default = Letter fallback 816×1056, keep the constant but define `LayoutEngineDefaults` once); replace `50f/60f/40f/12f` magic with `spec.marginTop/Bottom/Left/Right` and a named `paragraphGap`; `layoutParagraph` wraps against `width - margins`, not `- 80f`.
5. `InkyModule.kt:506`: construct `LayoutEngine(spec)` from `activeLayoutDocument.styles.defaultPageStyle ?: fallback`. `LayoutDrivenDocumentRenderer` gets the same spec injected (fix D4); card size derives from `spec` aspect (first step toward D1; full single-transform lands in PR B).
6. DOCX parity: `DocxDocumentParser` maps `w:pgSz`/`w:pgMar` into the same `PageStyleSpec` (1440 twips/in ÷ 15 = layout units per twip... define once), so both import paths converge on one geometry model.
7. Tests (bundled P2-3 part 1): JVM test parsing `tests/inky/Sample-5.odt` asserts discovered page = A4 (793.7×1122.5 layout units) + non-default margins; asserts `performLayout` uses them (fallback when absent). No ±3 page-count assert yet (that needs PR B's metrics; see D-notes).

Acceptance: unit tests green in CI; Viewer page count for Sample-5 drops toward ~30s on device (heuristic text metrics still bound the floor until PR B); no behavior change when a doc declares no page layout (Letter fallback preserved, `OdtSampleHeadingTest` unaffected).

Risk/rollback: additive model fields + defaults, no UI rewrite. Revert = one PR.

### PR B: P0-1 + P1-3, one render/edit/pagination stack (M, ~2-4 days, one PR but land per commit)

Scope (audit §3.4 order, adapted):
1. **Single source of truth for pages.** Editor renders `documentLayout.pages` (same `PageLayout` list as Viewer) inside page sheets; `pagesList`/`partitionTextToPages` removed from UI path. Keep or delete the function: decision D2 below.
2. **One model, one viewport.** Editing stays `docBodyText` + `DocumentTextMerger` (already in place at `InkyModule.kt:508-512`): every keystroke re-merges into `activeLayoutDocument`, layout re-runs (cache makes this incremental), sheets re-render. Remove per-page `pageTextVal` state and the `"\n\n"` join (fixes D6 drift). Focus/caret restoration after re-layout: map `DocumentCursor` ↔ string offset via `LineLayout.startOffset` and reuse `hitTest` inverse for click-to-caret (the pieces exist).
3. **`AnnotatedString` runs.** New `OfficeRuns.toAnnotatedString(styleSheet)` helper: paragraph `OfficeTextRun` + resolved `ParagraphStyle`/`CharacterStyle` → `SpanStyle` (font family map reusing the Editor's existing name→`FontFamily` switch, size, weight, italic, decoration, color) + `ParagraphStyle` for align/line-height. Viewer `ParagraphText` renders this instead of the flat `String` (fixes "Editor unformatted" and starts D1 convergence: font size in the card derives from the resolved style, single scale transform from `PageSpec`).
4. **Image interleave in both modes.** Replace `firstOrNull()` with the per-element `when` loop already used by `RenderLaidOutElement`; Editor sheets use the same element loop (read-only image composable inside the text column, tap selects/focuses text; no image editing UI in scope).
5. **FCT: Viewer selection.** Wrap the Viewer page column in `androidx.compose.foundation.text.selection.SelectionContainer` (`isEditable = false`). Because `LocalTextToolbar` is already provided root-wide (`InkyModule.kt:1902`), `showMenu(rect)` arrives on `customTextToolbar` with no new plumbing: FCT Compact shows Copy / Select All / AI Options. Hide cut/paste in Viewer via the existing `showBottomBar`/mode state. Editor: with step 2 the toolbar is attached to the single `BasicTextField` semantics path; verify `rectState` updates on handle drag (the audit's `BringIntoViewResponder` no-op concern disappears when there is exactly one viewport; keep `BringIntoViewResponder` disabled only if scroll-jank returns on C3).
6. **Hyphenation/line metrics (audit §4.3)**: replace `text.split(" ")` greedy wrap in `layoutParagraph` with `TextLayoutManager`'s wrap + `PapirusAssetEngine` hyphenation dictionary lookup, behind an `InkyPreferences` toggle default **off** on 3 GB devices. Ship as the last commit of PR B so it can be reverted independently; decision D3 below.
7. Tests (bundled P2-3 part 2): assert one pagination source (`totalDocPages == pagesList-less layout count` tautology replaced by a guard: Editor and Viewer use `documentLayout.pages.size`); Robolectric test on `tests/inky/Sample-5.*`: `documentLayout.pages.size in 15..21` (audit target 18 ± 3; if the ±1 pt size work in PR C is needed to pass, use a looser `12..30` here and tighten in PR C; measure on CI, do not tune blind); image presence test per §5 recipe of audit-003.

Acceptance: on C3, Editor shows headings/images matching Viewer; page counts identical in both modes; FCT appears on long-press in Viewer (Copy works) and in paginated Editor for imported docs; `Chapter1RegressionTest` + `WriterSection7RegressionTest` stay green (they exercise `DocumentTextMerger`/undo, both retained).

Risk/rollback: the biggest change; land as 4-5 reviewable commits (steps 1-2 first, then 3-5, then 6). Fallback if step 6 regresses memory: toggle off + follow-up PR.

### PR C: P1-4, ODF style fidelity (M, ~1-2 days after PR B)

Scope:
1. `SvXMLImport.parseOdfStyles()`: capture children of `<style:style>`: `<style:text-properties>` (`fo:font-family`, `style:font-name`, `fo:font-size` (+`-asian`/`-complex`), `fo:font-weight`, `fo:font-style`, `fo:color`, `style:text-underline-style`, `style:text-line-through-style`), `<style:paragraph-properties>` (`fo:text-align`, `fo:margin-*`, `fo:text-indent`, `fo:line-height`, `fo:background-color`), `<style:graphic-properties>` for image size passthrough. Feed into `ParagraphStyle`/`CharacterStyle` (fields already exist; extend with `lineHeight`, `underlineStyle` only if needed by renderer).
2. Cascade: resolve parent-style chain and `style:default-style` inheritance for the *properties* (the outline-level chain in `resolveHeadingLevel` already demonstrates the walk).
3. Fix D2 properly: `OdfSpanContext` maps `text:style-name` through resolved `CharacterStyle` (bold/italic/underline/size/color/font), substring heuristics deleted.
4. Headings: use the document's own `fo:font-size` when present; `StyleResolver` heuristic remains only as fallback (keeps `OdtSampleHeadingTest` green by design).
5. `DocumentTextMerger` run-awareness (audit §3.4 step 2 second half): when a block's text changed, reslice the old `runs` by common prefix/suffix diff instead of dropping style spans; new text inherits the paragraph default run. Keeps toolbar-applied formatting (which writes to global toolbar state today) honest while per-run toolbar editing is out of scope.
6. Tests: extend `OdtSampleHeadingTest` per audit §3.4 with the ±1 pt heading-size assert on Sample-5; tighten PR B's page-count window to 18 ± 3 if it was loosened; regression test: ODT with `<text:span text:style-name="Tabel1">` does not fabricate bold (D2).

Acceptance: Sample-5 headings within 1 pt of LibreOffice-computed sizes in Viewer *and* Editor; alignment + line-height visibly honored; imported italic/underline/bold spans render per file, not per name-guess.

### Out of scope (explicitly not touched, antislop Mode 2)

P1-1, P1-2, P2-1, P2-2, P0-3 (done), the `audit-003` §1.1 note about per-card page markers still present in `LayoutDrivenDocumentRenderer.kt:122-131` (kept by PR #6's design), `PROJECT_CONTEXT.md` §6 sources listing update (§8 note), anything else without an approved number. If you want the per-card marker removal or the docs fix, name them as new numbers and they get their own slice.

## 4. Follow-up tickets named by PR B2 (not in B2 scope)

- **F-1 Card geometry single transform (rest of D1):** `LayoutDrivenDocumentRenderer` still sizes its page card from a fixed `320x452` dp box scaled by zoom, while pagination measures in `PageStyleSpec` units. Render `LineLayout` directly (one transform from layout units to dp, card from `PageLayout.widthDp/heightDp`) so displayed wraps are the paginated wraps. Blocked on nothing, but changes what the user sees on every page, so it owns a PR.
- **F-2 OfficeDocElement retirement:** the deprecated `OfficeDocElement` wrapper layer (and its defensive arms in `LayoutEngine`, `DocumentTextMerger`, `InkyModule`, hit-testing) is dead weight after B1/B2 moved to direct `OfficeElement` implementors. One mechanical PR: delete the class, drop the wrapper arms and the scoped `@Suppress`.
- **F-3 Geometric run fidelity:** `OfficeRuns` ignores per-run font size in B2 (character-size styles resolve to the paragraph size). PR C's real size parsing (P1-4) is the prerequisite; then map `characterStyle.fontSizeSp` through `OfficeRuns.mergeRun`.

## 5. Verification without a local JDK (GitHub API approach)

Confirmed in sandbox: `java` absent, so nothing compiles here. CI already does exactly what is needed (`.github/workflows/build.yml`: `testDebugUnitTest` + full debug build on `pull_request`, JDK 17, ~6 min, all recent runs green). Loop per PR:

1. Implement locally (code-only), sanity-check structure with `grep`/reading, never claim "builds" without CI evidence.
2. `git push origin arena/01a0c97e-papirus-office` + `gh pr create` (one PR per step above).
3. `gh pr checks --watch`; on failure: `gh run view <id> --log-failed` for exact compile/test errors, fix, push, repeat.
4. Device gate: each merged PR's CI build produces the debug APK; install on the C3, run the audit's manual checklist (Sample-5: page count, Editor formatting, FCT, images via SAF + Recents reopen). Nightly release keeps the moving-tag channel for after-hours builds.
5. Before each PR's merge: run the antislop Delivery Gate against the changed code (comment hygiene via `skills/antislop-code`, UI copy via `skills/antislop-copywriting` for any new strings; strings en_US in `values/strings.xml` per `AGENTS.md`).
6. When all three land: write the follow-up report `anti-slop/audit-004-<date>.md` documenting per-item status + CI evidence + device notes, per the antislop Mode 2 convention.

## 6. Decisions needed before PR A starts

- **D1 PR split:** approve the 3-PR sequence (A: P0-2 → B: P0-1+P1-3 → C: P1-4) as the recommendation, or bundle A+B if speed beats review granularity.
- **D2 `partitionTextToPages`:** delete outright (no test references it today; `grep` confirms) or keep as a test stub as the audit suggested. Recommendation: delete; keep behavior in a test-local helper only if a future test needs it.
- **D3 Hyphenation step (PR B commit 6):** include in PR B behind an off-by-default preference, or defer to a follow-up (P1-5). Recommendation: include the wiring, default off, so C3 memory risk is zero until you flip it.
- **D4 P2-3 asserts:** the audit lists them as a separate ticket; this plan folds them into PRs A and B as acceptance tests (guards land with the change they guard). If you want them as their own PR instead, say so.
- **D5 D2-fix hotfix:** the `contains("b")` bold bug is actively wrong formatting on import. Options: hotfix PR now (30 min, no scope creep elsewhere), or hold until PR C step 3. Recommendation: hold it inside PR C only if PR C lands soon; otherwise hotfix first.
- **Antislop timing (per `AGENTS.md`):** apply the skills *during* implementation (Mode 1, recommended for a UI-heavy refactor) or *after* each PR (Mode 2 audit pass)? Default: during.
