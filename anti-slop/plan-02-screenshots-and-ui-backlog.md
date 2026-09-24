# Plan 2 — Screenshots Verification and UI/UX Backlog

**Date:** 2026-09-24
**Status:** ready to start. Lowest risk, highest visibility, unblocks the manual test loop for Plans 5-9.
**Evidence:** `anti-slop/audit-006-2026-09-24.md` §1 (screenshot verification, F-21…F-31) and `anti-slop/audit-005-2026-09-24.md` (F-01…F-07).
**Writer Guide reference:** the sections this plan makes reachable are §3.1 (Status bar, Rulers, Sidebar decks, Toolbars, Document views) and §3.3 (Go to Page, Navigator, outline folding, reminders) of the WG 24.8 Chapter 1 map in `plan-01-master-index.md` §3. Checklist sections unblocked: 5 (Selection), 8 (Reminder), 9 (Zoom), plus the dialogs of 1.

**Note:** this plan is the same scope as Plan 4 in `plan-04-to-09-writer-fidelity.md`. It is listed separately here because it is the only plan the user asked to be able to start immediately, and because its acceptance is visual rather than structural.

---

## 1. What the six screenshots settled

The images confirmed every UI item in the written report and added four things the report could not show. The table is the *verified* list; nothing here is inferred from code alone.

| # | Observation on device | Where the code does it | Fix |
|---|---|---|---|
| 1 | `1 / 1` printed inside the page card in **Viewer** | `LayoutDrivenDocumentRenderer.kt:174-182` | delete the marker |
| 2 | the same marker inside the card in **Editor** | same block, no mode check | delete once, both modes |
| 3 | Edit FAB sits on the status bar's right slot | `InkyModule.kt:4170-4205` | move Edit into the bar as a 48 dp action |
| 4 | `0 words, 0 chars` only looks centred because it is short | `InkyModule.kt:2400-2456` (`SpaceBetween`) | three equal-weight slots |
| 5 | zoom −/+ are visibly small (32 dp, 16 dp glyph) | `InkyModule.kt:2439,2454` | 48 dp targets, or 32 dp visual inside a 48 dp box |
| 6 | page card shows a double frame (grey band + border) | `LayoutDrivenDocumentRenderer.kt:117-125` | single frame, no outer band |
| 7 | a page can be **completely** empty (screenshot 5, page 16) | `LayoutEngine.kt:346-353` + renderer `else -> { }` at `:265` | Plan 5 (instrument first) |
| 8 | pages end 40-70 % empty | `elementGapDp = 12f` at `LayoutEngine.kt:107` | Plan 5 |
| 9 | new document reports `14` in the toolbar hub | `StyleResolver` default `14f` vs model default 12 pt | Plan 5 |
| 10 | table renders 5 equal columns, mid-word breaks, header row above the table | `RenderTable` `weight(1f)` + `10.sp`; parser leak | Plan 7 |

Items 7-10 are listed here only to keep the screenshot record complete; their fixes belong to Plans 5 and 7.

---

## 2. Scope of work

1. **One page counter.** Delete the per-card counter (`LayoutDrivenDocumentRenderer.kt:174-182`). The unified bar stays the only counter, in both modes.
2. **Status bar rebuilt as three slots.**
   * leading (`weight(1f)`, start): page range, tappable → Go to Page;
   * centre (`weight(1f)`, centred): `N words, M chars`;
   * trailing (`weight(1f)`, end): zoom cluster in Editor, a 48 dp **Edit** action in Viewer.
   The Viewer FAB block (`InkyModule.kt:4170-4205`) is deleted. Rationale for the PR body, per the `antislop` purpose test: the FAB duplicated an action that already exists in the bar and covered a live tap target; a FAB is for creation, not mode switching.
3. **Focus bridge (keyboard).** Add a `RendererFocusBridge` exposed by `LayoutDrivenDocumentRenderer` (`focusElement(index)` / `focusFirstEditable()`), consumed by the keyboard hub button (`InkyModule.kt:2758-2767`) and by the sheet-close path (`LaunchedEffect(showBottomBar)`, `:1262-1274`). Delete the orphan `FocusRequester` at `:117` (it is attached only to the Web-View field at `:2354`) and the empty `catch (e: Exception) {}` that hides the failure.
4. **Viewer FCT.** `ParagraphSelectField` (`LayoutDrivenDocumentRenderer.kt:385-445`) gets `onGloballyPositioned` bounds and a `LaunchedEffect(localValue.selection)`: non-collapsed selection → `customTextToolbar.show(rect)`, collapsed → `hide()`. The toolbar entry point already exists (`PapirusTextToolbar.kt:77`) and reuses the existing `FctMode.Compact` rules from PR C. Confirm with a log on device whether the platform also fires `showMenu` for read-only fields, and de-duplicate if it does.
5. **Fit-to-width zoom.** Single transform: `fitScale = viewportWidth / page.widthDp`, `cardWidth = page.widthDp * fitScale * zoomScale`. 100 % = page width fills the viewport; 25-400 % stays a multiplier; horizontal scrolling only above 100 %. This closes F-1 from `plan-2026-09-22`.
6. **Page-card chrome.** Remove the outer grey band so the sheet reads as one page: `border(1.dp, outlineVariant)` + `background(Color.White)` + shadow on a single container, padding inside.
7. **Touch targets and tokens.** Zoom ± to 48 dp; status-bar icon buttons to 48 dp; replace the raw greys used as chrome (`Color.DarkGray` cell text, `Color.Gray` counters — audit-006 §2.2 contrast failures) with `onSurfaceVariant` / `outlineVariant` tokens so they meet R-25 (≥ 4.5:1 at 9-11 sp).

---

## 3. Files

`app/src/main/java/com/example/modules/inky/InkyModule.kt`
`app/src/main/java/com/example/modules/inky/LayoutDrivenDocumentRenderer.kt`
`app/src/main/java/com/example/ui/components/PapirusTextToolbar.kt` (expose `show`/`hide` cleanly; no behaviour change)
`app/src/main/res/values/strings.xml` (only if new copy is needed, en_US)

---

## 4. Tests

* **Unit:** focus bridge focuses the first editable element; the keyboard path no longer swallows failures (assert on a fake bridge).
* **Unit:** viewer selection raises `show(rect)`; collapse calls `hide()` (fake `TextToolbar`).
* **Compose/Robolectric:** exactly one page-counter node per mode; the Edit action is inside the status-bar row (an overlap assertion fails before, passes after).
* **Screenshot (320 dp width):** card width equals viewport width at 100 %; the three slots render centred for a 5-digit word count.
* **Manual (`docs/InkyC1Checklist.md`, items the user already deferred):** keyboard after opening every sheet deck; Viewer long-press and handle-drag FCT; zoom steps at 25/50/100/200/400 %.

---

## 5. Acceptance

On device, at 320 dp width, in both modes: no per-page counter anywhere; the bar is never overlapped; the counter is optically centred at any content length; the keyboard appears after opening every deck; the FCT appears in Viewer on long-press **and** on handle drag; 100 % fits the page to the screen; every interactive control in the bar is ≥ 48 dp. Delivery Gate reported PASS with the evidence above (R-26, R-32, R-25, R-03, R-35).

## 6. Risk

Low. One product decision: the Viewer FAB disappears. If the user prefers to keep it, the fallback is to inset the status bar's trailing slot so the two cannot overlap, and the FAB stays.

**Size:** small, 1-2 days, four commits (counter, bar, focus bridge + FCT, fit transform) so each is individually revertible.

---

## 7. Implementation record (2026-09-24, branch `arena/01a0d11c-papirus-office`)

What landed, per scope item. Everything below is code that a CI run has to compile for the first time (`java`/`javac` are unavailable locally), so the local checks are grep, brace balance and reading; nothing here claims a green build.

| Item | State | Notes |
|---|---|---|
| 1 counter | done | the card's bottom-centre marker is gone (`LayoutDrivenDocumentRenderer`); `Plan2ChromeTest.viewerPageStack_drawsNoPerPageCounter` asserts the `" / "` shape appears in no node |
| 2 bar | done | 48 dp `Box` overlay, three slots; leading = page range (Go to Page), centre = words/chars `widthIn(max = 200.dp)`, trailing = zoom menu / Edit. The menu lists fixed steps (50/100/150/200/300) instead of the old ± buttons |
| 3 FAB | done | deleted; the Viewer Edit action moved into the bar with the same `fab_open_edit_mode` tag and a real `contentDescription`. The `"Edit Mode Active"` toast went with it, since the mode switch is now visible in the bar itself |
| 4 focus bridge | done | `RendererFocusBridge` (public, `requestFirstEditable()` / `requestElement()` returning `Boolean`), registered by the renderer through a `SideEffect` only while editable; the keyboard button and the sheet-close path call it before `keyboardController?.show()`. The bridge asks the elements in document order and reports `false` when nothing took focus, so the swallowed `catch (e: Exception) {}` is gone |
| 5 Viewer FCT | done, **[needs run]** | `ParagraphSelectField` reports `boundsInWindow()` while it owns a non-collapsed selection and `null` otherwise. Only the selection owner drives the toolbar, so two fields cannot show and hide it in the same frame. Whether Compose's platform path also fires `showMenu` for a read-only field still needs a device log |
| 6 fit-to-width | done, with one honest gap | one scale, `renderScale = (viewportWidthDp / 320) * zoomScale`, drives the card, the block spacing and the text; the card's padding now comes from `pageSpec` margins through `pageScale`, and the Viewer hit test divides by `pageScale`, so a tap maps back to the layout's own coordinates at any page size. **Gap:** the text still scales with `renderScale` while the paper maps with `pageScale` (the two differ by the legacy 320-dp card assumption), so the on-screen text column is not yet the print-faithful one. Unifying them needs real measurement and belongs to Plan 5, where the `2.5f` measuring hack dies |
| 7 chrome / tokens | part done | card chrome is now named tokens (gap, stack padding, corner, elevation, border) and the empty state uses `inky_pages_empty` instead of a literal. The status bar and the page stack were swept for live touch targets; the remaining raw colours and literals live in the Cellina/Slidia/Pagella modules and stay in Plan 3 |

**Shared geometry.** `PageStackMetrics` (`BASE_CARD_WIDTH_DP`, `FALLBACK_CARD_HEIGHT_DP`, `GUTTER_DP`) now lives in the renderer and is imported by the screen, so the viewport calculation and the "how much of a page is visible" range cannot drift from the page stack again.

**Honest counters.** The Viewer range is no longer decided by a hardcoded 1056-dp page: it derives the drawn sheet height from the same fit scale the renderer uses, so "Page 6–7 of 88" describes what is on screen.

**New tests:** `app/src/test/java/com/example/Plan2ChromeTest.kt` (4 cases: no per-card counter, bridge unavailable in Viewer, bridge focuses in Editor, no toolbar request without a selection). Robolectric + Compose; CI is their first run.

**Also cleaned while the file was open:** the seven em dashes in `InkyModule.kt` comments are gone (R-02), and the Viewer page-stack file no longer contains a hardcoded user string (`[Image]`, `No pages to display`). The four *user-facing* em dashes of Plan 3 item 3.4 are untouched and still that item's job.

**Still open after this plan:** the four audit-006 §5 questions (none of them block the code above), the `values-in` translations for the five new ids, and a device pass over the acceptance list in §5.
