# Audit 015 (2026-10-04): Plan 7C readiness, strings sweep status, and delivery strategy

**Baseline:** `main` `1c98e81d451ea349885fac0de84e010204c8b7b1` (Plan 7B merged as PR #27; push CI run `37115436637` green), branch `arena/01a10486-papirus-office`.
**Scope:** readiness for Plan 7C (authored indexes, named section ranges, Navigator jumps, status context). This is an analysis and strategy record. No production code changes are made here.
**Method:** antislop DURING mode. I read audits 001 to 014 and the plan files in this directory, plus `AGENTS.md`, `PROJECT_CONTEXT.md`, `DESIGN.md` and `CONCEPT.md`. I traced code paths from `SvXMLImport` through `DocumentIndexEngine`, `NavigationEngine`, `UniversalNavigatorSheet` and the status resolver in `InkyModule`. I re-derived the raw ZIP/XML of the six `tests/inky/*.odt` fixtures with Python `zipfile` and `xml.etree`, and checked ODF 1.4 Part 3 (`docs/odf`) for §8.2.2, §8.2.3, §8.3 and the `text:section` attributes. I also checked LibreOffice Writer Guide 26.2 Chapter 1 (`docs/lo-guides/WG262-WriterGuide_compressed.pdf`, book pages 26-27 and 37-38) and the LibreOffice `sw` API reference for `GetCurTOX`/`GetCurrSection`. No JDK is available in this sandbox, so nothing was compiled.

---

## 1. Fixture evidence (re-derived 2026-10-04)

| # | Index elements | Entries | Linked | Levels via template chain | Entry auto-styles | Anchors resolve | `index-title` | `text:section` |
|---|---|---|---|---|---|---|---|---|
| 1 | 0 | - | - | - | - | - | - | 0 |
| 2 | 1 `text:table-of-content` | **15** | **15** | L1 6, L2 9 | `P5`, `P6` | 15/15 | none | 0 |
| 3 | 0 | - | - | - | - | - | - | 0 |
| 4 | 1 | **21** | **0** | L1 7, L2 14 | `P11`, `P12` | n/a | none | 0 |
| 5 | 1 | **14** | **14** | L1 6, L2 8 | `P6`, `P7` | 14/14 | none | 0 |
| 6 | 1 | **45** | **45** | L1 6, L2 11, L3 28 | `P6`, `P7`, `P8` | 45/45 | none | 0 |

Facts that shape the implementation:

1. **The automatic style names change from file to file**, so a `P5`/`P6` name guess is wrong by construction. Every entry resolves `P* -> Contents_20_N -> Standard`. Each `text:table-of-content-entry-template` declares `text:outline-level` and `text:style-name="Contents_20_N"`. The correct level is the first ancestor in the paragraph's style chain that names a template style, read from that template's `text:outline-level`. Today `text:table-of-content-source` is swallowed by `OdfIgnoreSubtreeContext` (`SvXMLImportContext.kt:31`), so 7C must read the template map before it discards the rest of that subtree.
2. **The page label is the text after the last `<text:tab/>`.** Samples 4 and 6 have entries with two tabs (for example `1.1<tab/> Latar Belakang<tab/>1`), so the split is last-tab, not first-tab. Labels `ii` and `iii` appear in all four TOCs and must stay strings.
3. **No entry is empty, and no entry text is duplicated** in any fixture. Anchor names are unique per TOC.
4. **Sample 6 has one `text:soft-page-break` inside a TOC link.** It must not create an extra entry.
5. **Every TOC carries `text:style-name="Sect1"` and `text:protected="true"`.** A TOC uses a section style, but it is not a named `text:section`. Counting it as one would break the "zero named sections" gate.
6. **No fixture has `text:index-title` or `text:section`.** Title handling and every section case need synthetic ODF 1.4 inputs (audit-008 §5 already noted that sections are synthetic-only).

## 2. What 7B left in place (shipped)

- `DocumentSemantics.kt`: `BodyElementRange` (half-open), `DocumentIndexKind`, `DocumentIndexEntry(elementIndex, level, targetAnchor, displayedPageLabel)`, `DocumentIndexRange`, `SectionDisplay`, `DocumentSectionRange(parentId, depth, display, condition, isProtected, protectionKey)`.
- Both `OfficeParsedDocument` (`OfficeDocumentModel.kt:120-121`) and `OfficeDocument` (`OfficeDocument.kt:33-34`) carry `authoredIndexes` and `namedSectionRanges`. Adapters copy them, and `OdtDocumentWriter.saveCapability` (`writer/OdtDocumentWriter.kt:144-147`) refuses modified saves when they are present.
- Tokens `XML_TABLE_OF_CONTENT`, `XML_INDEX_BODY`, `XML_INDEX_TITLE` and `XML_SECTION` exist (`OdfXmlToken.kt:47-51`), but no dedicated context consumes them. TOC paragraphs already reach the body through the generic `SvXMLImportContext`, so 7C adds identity, not new content.

## 3. Findings that 7C must handle

| ID | Finding | Evidence | Consequence for 7C |
|---|---|---|---|
| F-1 | An index-title paragraph styled `Contents_20_Heading` would become a fake heading | Importer: `resolveHeadingLevel` matches "heading" in the display name, giving level 1 (`SvXMLImport.kt:837-863`). Navigator: `headingLevelFromStyleName` matches "heading" and takes the first digit run, "20", clamped to **6** (`NavigatorStringCatalog.kt:155-166, 213`) | Paragraphs inside an index range are never promoted to headings in either path. A synthetic title test pins this |
| F-2 | ODF encoded style names are not decoded before the digit scan | Same code: `Heading_20_2` resolves to 6 on the Navigator path when it arrives as an `OfficeParagraph` | Decode `_20_` (and `_xHH_`) before matching. This is small, and it touches the same resolver 7C changes |
| F-3 | `DocumentIndexKind` has no `BIBLIOGRAPHY`, and source-feature detection skips `bibliography` | `DocumentSemantics.kt:18-25`; `OdtImportPipeline.kt:246-247` | Add the kind and the detector arm. Otherwise a modified ODT with a bibliography passes save capability and loses it |
| F-4 | The model has no entry text and no title range | `DocumentIndexEntry`, `DocumentIndexRange` | Add `titleRange: BodyElementRange?` and an entry `text` (before the last tab), so the "not empty / not duplicated" gates test model data rather than re-parsed XML |
| F-5 | The canonical-to-parsed adapter drops element kinds without shifting the ranges | `DocumentSerializer.kt:170-199` (`mapNotNull`, `else -> null`) | Remap ranges through the same index map, or assert that no element was dropped. Imported ODT bodies are unaffected today; edited bodies may not be |
| F-6 | Edits keep the sidecars but never validate them | `DocumentTextMerger.mergeEditedText` uses `document.copy(...)`; extra blocks only append at the end | Policy: an in-place edit keeps the ranges, a range past the body end is dropped rather than crashing, and save stays refused (7B) |
| F-7 | Navigator flattening puts `doc.sections` before the body | `DocumentIndexEngine.kt:443-455` | Build index and section nodes from body indices directly. Retire the legacy `OfficeSection` path that nothing produces |
| F-8 | A range can open with a `PageBreak` element | Break-before inserts happen inside the child context (`SvXMLImportContext.kt:190-191, 345-346, 802-803`) | "First element" means the first non-`PageBreak` element. An empty range resolves to the nearest valid index |
| F-9 | The Navigator honesty classifier is format-blind and its owner markers are stale | `NavigatorCategoryHonesty.kt:66, 68, 84` use `plan-18/20` and `plan-19/21`; `UniversalNavigatorSheet.kt:383, 439, 465, 830` have TODOs with the old numbers; `InkyModule.kt:690, 5071` say "plans 19/21" | Once ODT produces indexes, a DOCX with a TOC (Sample-6.docx) must not read "No indexes in this document". Availability depends on the format: ODT readable after 7C, DOCX still not readable until 8B. Re-point the markers to plan IDs |
| F-10 | The status resolver needs Android `Context` and is private to `InkyModule` | `InkyModule.kt:5073-5093` | Extract a pure resolver that returns a typed result (index type, then innermost section, then heading/table/list item), with string formatting at the UI edge, so status priority can be unit-tested |
| F-11 | Hidden-item notices are hard-coded literals, and a matching resource is unused | `NavigationEngine.kt:173, 194, 229, 249, 299, 319` ("This item is hidden"), while `R.string.object_is_hidden` exists | WG Ch.1 p.38: a hidden section stays listed, grey, with a "hidden" tooltip. 7C lists hidden and conditional sections with the existing `navigator_hidden_label` and replaces the literals with typed notices |
| F-12 | ODT tab stops are not parsed (DOCX only), and leaders are not painted | `OfficeDocumentParser.kt:277-290` (DOCX `w:tab`); nothing reads `style:tab-stops` | Roadmap v2 §4.7b item 1 and E-5b assign "tab/leader" to 7C, but the 7C work list does not. This is an owner decision (§6) |
| F-13 | Sections are allowed inside table cells and headers/footers, which are not body ranges | ODF 1.4 §5.4 child lists | Only `office:text` body sections become ranges. Others produce a diagnostic, never a wrong range |

LibreOffice precedent for the status rule: the Navigator checks `GetCurTOX()` before `GetCurrSection()`, and `GetCurrSection` resolves the innermost section node through `FindSectionNode` (docs.libreoffice.org `content.cxx` and `SwDoc::GetCurrSection`). WG Ch.1 Table 2 lists "Section: Name of section" and "Index: Type of index (Table of Contents, Index or Bibliography)". This matches the plan's priority.

## 4. Strings audit (whole repository)

- `values/strings.xml`: 728 keys, no duplicates, and no `R.string.*` reference without a key. `values-in/strings.xml`: 46 keys, all present in the base file, consistent with the en_US-only policy (plan-03 §0).
- **29 keys look unused** (no `R.string`/`@string` reference; `getIdentifier` is not used anywhere): `menu_crash_log`, `menu_options`, `quick_menu_opened`, `inky_title`, `inky_desc`, `cellina_title`, `cellina_desc`, `slidia_title`, `slidia_desc`, `recent_documents_header`, `options_search_hint`, `doc_default_name`, `reset_success_message`, `fct_format`, `fct_back`, `fct_inky_ai`, `fct_search_web`, `fct_share_text`, `fct_pgp_encrypt`, `saving_doc_title`, `about_native_engine_none`, `about_native_engine_lokit`, `open_navigation_bar`, `actionsToUndoTitle`, `actionsToRedoTitle`, `menu_reload`, `menu_close`, `btn_reload`, `cd_pagella_pdf_document`. Several are the resources the literals below should be using (for example, `open_navigation_bar` exists while `InkyModule.kt:2215` has `Text("Open navigation bar")`).
- **About 854 user-facing literals remain** (heuristic scan with comments masked: 684 `Text("...")`, 111 `title/subtitle/description/placeholder/...`, 46 `label = "..."` excluding animation labels, 13 Navigator notices/labels). Largest files: `CellinaModule.kt` 110, `SlidiaModule.kt` 103, `UniversalXmlImportSheet.kt` 85, `UniversalOdfSheet.kt` 66, `UniversalClipboardSheet.kt` 55, `HomeSubpages.kt` 54, `UniversalFormsSheet.kt` 50, `SwTextFormattingInspectorDialog.kt` 47, `InkyModule.kt` 36. Plan 3A moved toasts and `contentDescription`s and guards them in `SourceHygieneGuardTest`. That guard does not cover `Text(...)`, named UI parameters or data-layer notices, so these literals grew without failing CI.
- 218 log and exception messages are also literals. They are developer diagnostics and are not localization targets. Exceptions that reach the UI as `failureReason` (`OdtImportPipeline.kt:50, 55`) are an exception to that.
- Style: 27 strings use `...` rather than the typographic ellipsis. This is low priority (Android lint `TypographyEllipsis`).

**Strategy:** 7C takes only the strings it touches (Navigator hidden/empty notices, the new status strings for index type and section name, index kind names). The remaining literals are a separate Plan 3 follow-up ("3D strings sweep"), which also extends the guard to `Text("...")` and named UI parameters with per-file allowances that ratchet down. Mixing an 850-literal sweep into a parser PR would hide the semantic diff.

## 5. Writer Guide 26.2 Chapter 1 checks relevant to 7C

| WG Ch.1 statement | Today (shipped) | After 7C (target) |
|---|---|---|
| Status bar "Section or object information" shows the section name and the index type (Table 2, p.27) | Heading level+text, "Table", "List item" only (`InkyModule.kt:5073-5093`) | Index type wins inside an index; otherwise the innermost section name; otherwise the existing heading/list/table behavior |
| Navigator lists sections and indexes; double-tap jumps (p.37-38) | Both categories say "Not yet available in this build" | ODT: index rows jump to the index start, section rows to the first non-break element or the nearest valid index |
| Hidden sections appear grey with a "hidden" tooltip (p.38) | Engine blocks the jump with a hard-coded toast | Listed with the existing Hidden label; jump policy decided in §6 |
| Clicking the status area opens Edit Sections or the index dialog | No dialog | Out of scope: no section or index dialog exists, and the status slot stays display-only |

## 6. Delivery strategy

### Option A (recommended): one Plan 7C PR in four ordered commits

This keeps the binding "exactly one PR per plan" rule from audit-014 §1 and the forecast slot #28. Each commit compiles and is pushed for a CI checkpoint:

1. **Model and importer.** Add `BIBLIOGRAPHY`, `titleRange` and entry `text` (F-3, F-4). Add contexts `OdfIndexContext` (`table-of-content` plus the other index kinds), `OdfIndexSourceContext` (reads only the entry templates, ignores the rest), `OdfIndexBodyContext`, `OdfIndexTitleContext` and `OdfSectionContext`. Each records `elements.size` at start and end, and child paragraphs flow normally. Template-chain levels, last-tab labels, anchors from the first `text:a` href without the `#`, no heading promotion inside index ranges (F-1, F-2), body-only sections (F-13), and the bibliography detector arm.
2. **Range integrity.** Adapter remapping or a drop assertion (F-5), validation after edits (F-6), and helpers `firstNavigableIndex(range)` and `innermostSectionAt(index)` (F-8).
3. **Navigation projection and status.** Add `DocumentIndex.authoredIndexes` and `IndexNode`, and build `SectionNode` from `namedSectionRanges` with depth, protection and visibility, replacing the flattened legacy path (F-7). Add `goToIndex`, `goToSection` and next/previous for `NavigateBy.INDEX`. Extract the pure status resolver (F-10). Make honesty format-aware with plan-ID markers (F-9). Replace the Navigator literals with typed notices (F-11). Add the new en_US strings.
4. **Evidence and docs.** Tests (below), this audit's implementation record, plan-01 row 7, roadmap v2 §4.7b, the plan-04-to-09 Plan 7 record, and `PROJECT_CONTEXT.md`. `AGENTS.md` is owner-edited only, so changes there are proposed in the PR body instead.

**Tests:** `Plan7cAuthoredIndexTest` checks the exact inventory (2: 15/15, 4: 21/0, 5: 14/14, 6: 45/45; 1 and 3 have none), per-level counts, labels as strings including `ii`/`iii`, no empty or duplicate entry, every entry index inside its range and pointing at a paragraph, an unchanged body element count against 7B for all six fixtures, and the Sample-6 soft page break not adding an entry. `Plan7cSectionRangeTest` uses synthetic nested, sibling, empty, protected (with key), hidden (`display="none"`), conditional, section-inside-index, index-inside-section and table-cell cases, and checks that all six fixtures have zero named sections. `Plan7cNavigationStatusTest` checks index jumps, first-element and empty-range jumps, index-over-section priority, the innermost section, unchanged heading/list/table output and Sample 3 as control. `NavigatorCategoryHonestyTest` is updated for the format-aware availability.

### Option B: split into 7C-1 (producer) and 7C-2 (consumer)

Smaller diffs, but it breaks the one-PR-per-plan rule, moves forecast slots #29 to #40 by one, and leaves an intermediate main where the sidecars are populated but the Navigator still says "not yet available". Use it only if the 7C review diff is too large to read.

### Option C (not recommended): bundle the strings sweep into 7C

See §4.

## 7. Owner decisions before implementation

1. **Tab stops and leaders (F-12):** (a) defer ODF `style:tab-stops` and leader painting to 7E, where they belong with metrics calibration (recommended; the change is recorded in the roadmap so nothing is silently dropped); (b) include them in 7C; or (c) assign them to their own plan.
2. **Hidden or conditional section jump:** (a) list and jump to the nearest visible position with a "hidden" notice (recommended, closest to WG); or (b) list but block the jump, as the engine does today.
3. **TOC links in the Hyperlinks category:** 45 internal `#_TOC...` links appear as hyperlink rows for Sample 6. Keep them, or group them under the index. This needs a LibreOffice behavior check before anything is claimed.
4. **Tapping a TOC entry to follow its anchor:** not in the 7C work list. Defer, or add as a small follow-on, since the anchors and bookmarks now exist on both sides.
5. **Strings sweep:** approve a separate Plan 3D (recommended) or leave it in the backlog.

### 7.1 Owner answers (2026-10-04)

1. **Tab stops and leaders:** option (a). ODF `style:tab-stops` and leader painting move to **Plan 7E** and are recorded in roadmap v2 §4.7d item 5. 7C keeps the authored entry text and the page label after the last tab, and does not paint leaders.
2. **Hidden sections:** option (a). Hidden sections are listed greyed out and the jump lands on the nearest visible position with a notice. Conditional sections are listed as visible, because the condition is not evaluated.
3. **TOC links:** grouped under the index. They are no longer projected into the Hyperlinks list.
4. **Following a TOC entry:** added to 7C, with this interaction: tap and hold a TOC entry to open FCT Compact, then tap **Go to entry…**. The option appears only when the entry link resolves to a bookmark.
5. **Strings sweep:** a separate **Plan 3D**, saved as `plan-3d-strings-sweep.md`.

## 8. antislop tooling

`skills/antislop/VERSION` is `3.2.20`, the latest upstream release (`miqdadbadjuber/anti-slop` `v3.2.20`, 2026-09-29). No update is needed. The owner chose DURING mode for this session and asked for it to be saved at `.config/antislop/settings.json` in the repository root. The core reads `~/.config/antislop/settings.json`, so other sessions find the repository copy only if they are pointed at it.

## 9. Implementation record (Plan 7C, forecast PR #28)

The record below is **target until merged**. Line numbers refer to the branch head that adds this section. The sandbox has no JDK, so GitHub Actions on the PR is the build and test evidence.

| Item | Status on the branch | Evidence |
|---|---|---|
| Model: `BIBLIOGRAPHY`, entry `text`, `titleRange` (F-3, F-4) | shipped | `DocumentSemantics.kt` (`DocumentIndexKind`, `DocumentIndexEntry.text`, `DocumentIndexRange.titleRange`) |
| Index contexts with half-open ranges; paragraphs keep flowing | shipped | `OdfSemanticRangeContexts.kt:50` (shared text-flow dispatch), `OdfSemanticRangeCollector.kt:77`, `:101` |
| Entry level from the template outline level through the style chain | shipped | `OdfSemanticRangeCollector.kt:207` |
| No heading promotion inside an index (F-1); `_20_` decoding (F-2) | shipped | `SvXMLImportContext.kt:268`, `NavigatorStringCatalog.kt:219` |
| Sections: name, style, display, condition, protection, key, depth and parent; unnamed sections keep nesting balanced | shipped | `OdfSemanticRangeCollector.kt:136` |
| Section inside a table cell stays in the cell and is reported (F-13) | shipped | `OdfSemanticRangeContexts.kt:159` |
| Sidecars returned for ODT only | shipped | `SvXMLImport.kt:1096` |
| Serializer remaps ranges through dropped elements (F-5) | shipped | `DocumentSerializer.kt:171` |
| Merger validates ranges after each edit path (F-6) | shipped | `DocumentRanges.kt:117`, `DocumentTextMerger.kt` (three `withValidatedRanges()` calls) |
| Jump targets skip a leading page break; an empty range goes to the nearest valid element (F-8) | shipped | `DocumentRanges.kt:68` |
| Hidden sections go to the nearest visible element outside every hidden range | shipped | `DocumentRanges.kt:92`, `NavigationEngine.kt:247` |
| Navigation projection: `authoredIndexes`, range sections, no `doc.sections` prepend (F-7) | shipped | `DocumentIndexEngine.kt:380`, `:412`, `:535` |
| TOC links grouped under the index | shipped | `DocumentIndexEngine.kt:131` |
| Format-aware honesty: sections and indexes readable for ODT only (F-9) | shipped | `NavigatorCategoryHonesty.kt:57` |
| Typed Navigator notices instead of engine literals (F-11) | shipped | `NavigationEngine.kt:52` and the sheet's notice mapping |
| `goToIndex`, `goToIndexEntry`, next and previous for `NavigateBy.INDEX` | shipped | `NavigationEngine.kt:282` |
| Indexes category in the ALL list, entry rows, section rows indented by depth | shipped | `UniversalNavigatorSheet.kt:476`, `:1199` |
| Pure status resolver: index type first, else innermost section joined with the detail (F-10) | shipped | `StatusObjectResolver.kt:29`, `InkyModule.kt:716` |
| FCT Compact "Go to entry…", offered only when both selection ends sit in one linked table-of-contents entry (owner follow-up, 2026-10-04) | shipped | `StatusObjectResolver.kt:66`, `PapirusTextToolbar.kt:198`, `InkyModule.kt:3732`, `:3844`, `NavigationEngine.kt:318` |
| Tests | shipped, CI pending | `Plan7cAuthoredIndexTest`, `Plan7cSectionRangeTest`, `Plan7cNavigationStatusTest`; updated `Plan7bSemanticImportTest`, `NavigationEngineTest`, `NavigatorCategoryHonestyTest` |

**Known limits, all recorded rather than hidden:**

- The paginator still lays out hidden-section content. Hiding it changes pagination, so it moves to Plan 7E with the final calibration (roadmap v2 §4.7d item 6). Until then a hidden section is greyed in the Navigator and the jump avoids it, but its text stays on the page.
- Conditional sections are not evaluated.
- DOCX indexes and sections are not read. The Navigator keeps the "not yet available" wording for DOCX files.
- None of the six fixtures has a `text:index-title`. Each one authors "Daftar Isi" or "DAFTAR ISI" as a heading before the TOC, so `titleRange` is only exercised by synthetic tests.
- Tapping the status area still opens nothing. No section or index dialog exists.
