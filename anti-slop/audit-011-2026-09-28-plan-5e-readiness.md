# Audit 011: Plan 5e readiness and the next implementation sequence

**Date:** 2026-09-28

**Baseline:** `0f7fc99dec5a214248d343668eb976a4e0a3f709`, branch `arena/01a0e5ed-papirus-office`.

**Deliverable:** analysis and proposed strategy only. No application code, samples, templates, tests, workflow, or upstream `antislop.md` changes.

**Method:** review of the audit/plan history, targeted source inspection, ZIP CRC checks and XML parsing, GitHub PR/check evidence, the local Writer Guide, local ODF specification sections, and official online references. This is not a line-by-line audit of every Kotlin/Java file or a full schema validation. No JDK or device is available locally. The roadmap's standing antislop Mode 1 decision applies to this document.

## 1. Recommendation

Keep the owner's sequence: **Plan 5e -> Plan 1 synchronization -> Plans 6, 7, 8, 9, 10, 11 -> physical-device Chapter 1 acceptance -> Chapter 2**.

Before changing pagination, restore a green current baseline and correct the small parsing/measurement contracts described below. These can be the first isolated commits of Plan 5e; there is no need to invent another numbered Plan 5 sub-plan. Keep the template-gallery replacement in Plan 11's home-entry package, but fix the blank-asset lookup regression now because it blocks reliable New Document testing.

Do not tune metrics until the sample page counts look right. First prove units, content preservation, break positions, spacing semantics, line fragments, renderer placement, and caret mapping. Then apply the recorded twelve page-count windows. Report unsupported structure separately rather than compensating for missing content with larger fonts or gaps.

## 2. What the earlier work establishes

The historical documents describe different baselines. Their old counts and source line numbers are not current acceptance evidence.

| Record / PRs | Contribution | What must remain protected |
|---|---|---|
| Audits 001 and 002; PR #1 and #3 hardening | Copy/control cleanup, build work, bounded package handling, simulated-native seam, typing-buffer/undo fixes | Do not repeat blanket claims that every control or native capability is verified. Retain bounded extraction and flush-before-delete/undo behavior. PR #2 was closed, not merged. |
| Audit 003; PR #4-6 | Structural Navigator, build/test repairs, status/locale/media-path fixes following device feedback | App-locale UI labels must remain independent of document style names. A successful open is not evidence of a safe save. |
| Remaining-writer-fixes plan; PR #7 A, #8 B1, #9 B2 | Declared geometry, common Viewer/Editor page stack, selection/run presentation, optional hyphenation | Keep one editable document value and layout-based navigation. Do not restore per-page independent text models. |
| Audit 004; PR #10 C | ODF styles, parent/default cascade, character-style inheritance, caret formatting indicator | Preserve nullable character size and the merger's run slicing. Native font-file loading was explicitly deferred. |
| Audits 005/006; Plan 2 / PR #11 | Device-reported chrome defects became one status bar, fit-to-width geometry, focus bridge and Viewer FCT work | Device acceptance remains distinct from source or CI evidence. Plan 4 is consumed by Plan 2, not another implementation pass. |
| Plan 3A / PR #12 | Resource strings and source hygiene guard | en_US resources and no new guard allowances. |
| Plan 3B / PR #13 | Six Writer ribbon tabs, unavailable-command disclosure, Navigator category honesty, object information | Do not expose controls or categories as functional before parser/writer support exists. |
| Plan 3C / PR #14 and Plan 11 | Hybrid experience contract and documentation honesty | Borrow task patterns, not proprietary skins or unsupported feature claims. |
| Audit 007; Plan 5a / PR #15 | Units, metrics/font seams, page transform, element dump, fixture matrix, native inventory | These are measurement infrastructure, not proof of accurate on-device pagination. |
| Audit 008; Plan 5b / PR #16 | Producer re-baseline and per-format references | This supersedes audit-007's old sample geometry and provisional ODT windows. |
| Audit 009; Plan 5c / PR #17 | DESIGN v3, 26.2 guide references, current/target distinctions, font/template/Drive findings | The design contract did not ship Expressive APIs, real template browsing, valid UI fonts, or cloud integration. |
| Audit 010; Plan 5d / PR #18 | Soft-break removal, authored-break support, default/paragraph metric parsing, body rectangle | Merged and CI-green at its final revision, but the edge cases in section 5 still need tests and fixes. |

### Evidence hierarchy and CI state

- GitHub confirms PR #18 merged on 2026-09-27 at 22:42:17 UTC. Its final report for `7045a2b`, [run 36355811273](https://github.com/makerandreas/Papirus-Office/actions/runs/36355811273), records **204 tests, zero failures/errors/skips**. Its Unit Tests and Build checks succeeded.
- The current baseline is later than that PR. [Run 36370092065](https://github.com/makerandreas/Papirus-Office/actions/runs/36370092065), on `0f7fc99`, has **Unit Tests failed / Build succeeded**. Check annotations only expose exit code 1, not the failing test names. The Actions log download failed with EOF, so the exact cause is not verified here.
- The current asset case mismatch is independently reproducible by inspecting filenames and lookup strings. It is a candidate for the failing tests, not a proven CI diagnosis.
- The final PR #18 dump still reports per-character stub measurement (`MMMM=4.0`, `iiii=4.0`). Those page counts are useful historical baselines, not Android glyph-layout results.
- There was no fresh local Gradle run. A checklist's presence, a test's name, or audit-010's PASS label does not establish a physical-device pass.

## 3. Current samples and blank assets

### 3.1 Verification method and limits

Python `zipfile.testzip()` found no CRC errors in any of the **16 files in `tests/`**. `xml.etree.ElementTree` parsed every `.xml` and `.rels` entry in those packages without a well-formedness error. Metadata and structural counts below come from the actual package parts, not filenames or producer assumptions.

These checks do not validate full ODF/OOXML schemas, resolve every relationship, evaluate formulas, prove rendered page counts, or establish save round-trip fidelity. Stored page/word counts may be stale in general; the project's references also have the owner's earlier confirmation in audit-008. Do not replace them with counts of soft-break hints.

### 3.2 Inky matrix

All six ODT files identify `Collabora_Office/26.04.3.1` on Android AArch64. All six DOCX files identify `Microsoft Office Word`, AppVersion `16.0000`.

| Sample | Stored pages ODT / DOCX | ODT soft breaks | ODT explicit break-before uses | DOCX last-rendered hints | DOCX page br / intermediate sectPr | Plan 5e window ODT / DOCX |
|---|---|---|---|---|---|---|
| 1 | 15 / 15 | 14 | 0 | 14 | 0 / 0 | 12..18 / 12..18 |
| 2 | 23 / 23 | 16 | 2 | 22 | 2 / 4 | 18..28 / 18..28 |
| 3 | 22 / 22 | 21 | 0 | 25 | 0 / 0 | 18..26 / 18..26 |
| 4 | 11 / 10 | 3 | 2 | 3 | 2 / 5 | 9..13 / 8..12 |
| 5 | 19 / 18 | 12 | 2 | 17 | 2 / 4 | 15..23 / 15..21 |
| 6 | 22 / 21 | 15 | 2 | 19 | 2 / 4 | 17..27 / 15..26 |

ODT uses are body references to the automatic styles carrying `fo:break-before="page"`, not the count of style definitions. Sample-2/5/6 each reuse one such style twice; Sample-4 uses two styles once each. No fixture contains `fo:break-after`. All observed DOCX section types are omitted, so this corpus does not exercise `continuous`, `nextColumn`, or odd/even transitions. Its authored-break counts cannot validate those semantics.

The raw DOCX hint counts and page-break counts correct stale entries in audit-010's inventory. They do not require changing the reference windows in `SampleMatrix.kt`.

Historical final PR #18 CI output, **not remeasured in this audit**:

| Sample | CI pages ODT / DOCX | Outside the recorded Plan 5e window |
|---|---|---|
| 1 | 12 / 12 | Neither |
| 2 | 22 / 23 | Neither |
| 3 | 11 / 12 | Both |
| 4 | 7 / 11 | ODT |
| 5 | 14 / 16 | ODT |
| 6 | 21 / 23 | Neither |

The windows were printed, not enforced, by that dump. This explains how Plan 5d could correctly pass its tests without completing Plan 5e. Replacing the stub with advances may move counts substantially in either direction. No predicted final counts are asserted here.

### 3.3 Cellina and Slidia

| File | Current evidence | Next testing use |
|---|---|---|
| `tests/cellina/Sample-1.xlsx` | 34,532 bytes, 27 ZIP entries; `Application=Excel Android`, `AppVersion=16.0300`; 12 worksheets in `xl/workbook.xml` | Confirms the producer change from the earlier Collabora XLSX. Pin sheet names/order, relationships, representative values/formulas and styles, not just entry count. Metadata does not separately identify the Copilot shell. |
| `tests/cellina/Sample-1.ods` | 50,006 bytes, 8 entries; Collabora 26.04.3.1; stored table-count 12, cell-count 1,494 | Cross-format smoke fixture; do not assume exact expanded-cell equality from raw XML counts. |
| `tests/slidia/Sample-1.odp` | 1,338,900 bytes, 43 entries; Collabora 26.04.3.1; 51 draw:page elements | Retain open/slide-content smoke tests; Writer pagination work should not redesign Impress import. |
| `tests/slidia/Sample-1.pptx` | 1,560,408 bytes, 268 entries; Microsoft Office PowerPoint 16.0000; stored Slides=51 and 51 presentation slide IDs | Verify slide order and relationships in the Slidia work package. |

### 3.4 Blank documents: the current tree differs from the intended update

Actual names are **`Untitled.odt`, `Untitled.ods`, `Untitled.odp`**, with a capital U. `TemplateManager.kt:25-41` still requests `templates/untitled.*`. Its gallery entries also use lowercase paths. Android asset lookup and the sandbox filesystem are case-sensitive.

Consequences from source inspection:

- Inky can fall back to `styles/Default.ott` instead of the new blank Writer package.
- Cellina can fall back to `wizard/styles/default.ots` instead of its blank package.
- Slidia's fallback `templates/slidia/Default.otp` is not present.
- `CreateNewDocumentTest` expects the lowercase output names. The fix can preserve those cache/output names while correcting the source asset paths. Do not merely weaken the test to accept any nonempty fallback.

| Blank asset | Verified package state | Assessment |
|---|---|---|
| `Untitled.odt` | 7,366 bytes, 9 entries; Collabora generator; content and manifest declare 1.4; first uncompressed `mimetype`, no extra field; styles/meta/settings present; 21.001 x 29.7 cm page with 2 cm margins; two empty Standard paragraphs | The old Writer blank-package finding is materially improved. Full strict/extended schema conformance and create/save/reopen remain unverified. Its empty paragraphs also exercise the importer's empty-paragraph behavior. |
| `Untitled.ods` | 667 bytes, 3 entries; correct first/uncompressed `mimetype`; no `office:version` or `manifest:version`; empty `office:spreadsheet`; no sheet, styles or metadata | Still a minimal package, not established ODF 1.4 output. Regenerate and test a usable empty workbook. |
| `Untitled.odp` | 668 bytes, 3 entries; same ZIP MIME properties; no version declarations; empty `office:presentation`; no slide, styles or metadata | Same follow-up for a usable blank presentation. |

ODF 1.4 Part 3 section 19.390 requires `office:version="1.4"` on the document roots. Missing `styles.xml` or `meta.xml` alone is not the conformance argument; those files are not universally mandatory. The version omission is a concrete defect against the selected 1.4 target. Part 2 section 3.3 defines the `mimetype` ordering/storage rules.

**Proposed prerequisite:** correct exact-case asset lookup, re-save/validate the two remaining blanks, and test source-package identity plus parse/create/save/reopen. Keep blank packages behind Create New, excluded from the template gallery.

### 3.5 Bundled template inventory

All **115 files** under `assets/templates/` passed the same ZIP CRC and XML well-formedness checks. MIME types agree with these counts:

- 69 Writer `.ott` templates.
- 18 Calc `.ots` templates.
- 23 Impress `.otp` templates.
- Three blank `.odt/.ods/.odp` packages.
- `internal/idxexample.odt` and `internal/html.oth`.

Thus **110 are the usual OTT/OTS/OTP gallery candidates**, not 112. The historical count of 112 nonblank assets includes the internal ODT example and OTH web template. Exclude or explicitly classify those two; do not label them as ordinary Writer/Calc/Impress templates. Not every wizard/localization template is necessarily usable without additional UI, so the gallery needs per-entry eligibility rather than an assertion that all 110 work.

## 4. Plan 5d verified changes

Source inspection agrees with the main merged scope:

- The active DOCX shared parser ignores `lastRenderedPageBreak`/`soft-page-break` and no longer appends textual page-break markers (`OfficeDocumentParser.kt:1339-1341,1388-1395`).
- ODF body and paragraph contexts no longer turn soft breaks into model page breaks (`SvXMLImportContext.kt:103-105,206-208`).
- DOCX defaults, basedOn styles and direct paragraph metrics reach `ParagraphStyle`; ODF paragraph metric fields are populated.
- `LayoutEngine.performLayout` starts/resets at `bodyTopDp` and checks `bodyBottomDp` (`LayoutEngine.kt:353-382`).
- `StyleResolver` no longer has the 24/20/16 heading-size ladder and can resolve document defaults.
- `BreakSemanticsTest`, `StyleChainMetricsTest` and `BodyRectTest` cover the sample changes. Their successful PR #18 result should be retained as evidence, not dismissed because additional cases remain.

The remaining `LayoutDump.PAGE_BREAK_MARKER` string is a detector, not evidence that the importer still injects it.

## 5. Findings that change the Plan 5e strategy

All findings below are **source-inspected**, not claims of a newly reproduced device failure. P0 denotes the immediate baseline gate; P1 denotes pagination correctness work; P2 denotes documentation or later-package work. R-17/R-36 apply to claims made about these behaviors, not to every algorithmic defect by itself.

| ID | Priority / owner | Evidence and consequence | Proposed regression |
|---|---|---|---|
| E11-01 | P0, prerequisite | `TemplateManager.kt:25-41` requests lowercase names absent from current assets. Current Unit Tests job is red, with cause unconfirmed. | Exact asset identity, blank file package checks, and all three Create New flows; obtain a fresh full CI report. |
| E11-02 | P1, first 5e commit | `SvXMLImport.kt:194-199` maps either break-before or break-after to `pageBreakBefore`. The ODF body context emits it before the paragraph. Break-after is therefore misplaced. `auto` cannot clear an inherited page-break flag. | A/break-after/B; break-before; inherited page/auto override; empty break-bearing paragraphs; nested paragraphs. Assert positions, not only counts. |
| E11-03 | P1, first 5e commit | `OfficeDocumentParser.kt:1388-1395` emits a page break while current paragraph text is still buffered until paragraph end. A mid-paragraph break is placed before the whole accumulated paragraph rather than between its two text segments. | DOCX `A`, page br, `B`: A must precede the boundary and B must follow it; preserve logical paragraph/style and text offsets. |
| E11-04 | P1, first 5e commit | `paraHasSectPr` becomes a break without section-type interpretation (`OfficeDocumentParser.kt:1334-1336,1532-1534`). Corpus tests cover omitted/default types only. | Mixed nextPage/continuous/nextColumn and odd/even section cases. Associate type with the section it governs, including the terminal sectPr; terminal section properties are not an unconditional trailing page. Full per-section geometry remains Plan 8B. |
| E11-05 | P1, style contract | `keepNext` and `pageBreakBefore` become true on element presence even for `w:val="0"` (`OfficeDocumentParser.kt:454-459,1324-1332`). ODF keep-with-next only records true. | Child/direct false overrides inherited true in both formats. Distinguish absent from explicit false. |
| E11-06 | P1, line-height model | DOCX `atLeast` and `exact` share the exact field; cascade/direct overlay can retain an inherited exact height when a child selects automatic proportional height. `TextMetrics.kt:175-182` multiplies natural height for both formats. | Exact -> proportional -> exact cascade; atLeast below/above natural height; ODF percentage versus DOCX auto semantics, including mixed-size runs. |
| E11-07 | P1, line layout | `LayoutEngine.kt:210-217` still uses `fontSizeSp * 2.5f`, raw Paint and splitting only on spaces. Parsed indents and most metric fields are not consumed. | Advances, newline/blank line, tabs, repeated spaces, nonbreaking spaces, long tokens, indents, mixed runs and optional hyphenation with lossless source offsets. |
| E11-08 | P1, pagination architecture | `place()` reserves each complete paragraph on one page; no line fragments exist. There are no widow/orphan fields or tab-stop model in `ParagraphStyle`. Whole-block placement cannot meet the stated two-line widow/orphan behavior on long paragraphs. | Paragraph longer than a page, keep chains, widow/orphan overrides, progress on impossible constraints, no omitted/duplicated source spans. |
| E11-09 | P1, renderer parity | `LayoutDrivenDocumentRenderer.kt:265-277` uses margin padding and a separate Column with constant gap rather than element bounds/body top. Five `sizeSp + 5f` line-height expressions remain. | First baseline/body origin, block bounds, line ends and fragment clipping agree with pagination at all checklist zoom levels and font scales. |
| E11-10 | P1, editing/navigation | Layout cache is indexed only by paragraph index (`LayoutEngine.kt:142,205-206`). `hitTest` handles legacy `OfficeDocElement.ParagraphElement`, not the canonical paragraph/heading cases, and uses character-count ratios (`:482-516`). | Canonical paragraph/heading taps, unequal-width glyphs, split-paragraph caret/selection, style-only edits, insertion/deletion before cached elements, incremental versus force-rebuild parity. |
| E11-11 | P1, one rendering path | `InkyModule.kt:2347-2422` retains a Web View `BasicTextField` outside the paginated renderer. | Converge on shared model/styles/geometry or temporarily disable the alternate renderer honestly. Keep flow-view feature expansion out of this PR. |
| E11-12 | P2, diagnostic truth | `LayoutDump.toText():91-93` still labels marginTop/contentBottom as the flow used even though placement uses bodyTop/bodyBottom. The heuristic calls thin overflow pages "inflated metrics" without proving the cause. | Dump actual used bounds, metric source, break origin and overflow reason. Thin pages are diagnostics, not automatic defects. |

Additional style-test gaps: DOCX default paragraph selection is name-based (`Normal`) rather than driven by `w:default`; ODF `defaultParagraphStyle` is the raw family default, which can differ from a named Standard style. Specify default-selection semantics per format and test unnamed paragraphs, custom default IDs, missing parents and cycles. Do not blindly layer Normal onto every explicitly selected DOCX style.

### 5.1 Two standards corrections to the old blueprint

**Line height:** local ODF 1.4 Part 3 section 20.204 explicitly defines percent as font size times percent, taking the maximum descendant font size and increasing the line area for overflowing content. A nonnegative absolute length is fixed, while `normal` is implementation-defined. OOXML `w:spacing w:lineRule="auto"` uses 240ths of normal single spacing. Therefore the current universal `naturalHeight * factor` formula and its comment in `TextMetrics` are not valid for both formats. Represent the mode/basis explicitly, with separate automatic, percentage/minimum and exact behavior; preserve the source meaning through inheritance.

**Paragraph spacing:** roadmap section 4.4b's universal `spaceAfter(previous) + spaceBefore(next)` is not a correct DOCX rule. The Open XML SDK's standards excerpt for `SpacingBetweenLines` specifies the maximum of the applicable spacing contributions, including inter-line spacing, and gives 12 pt after plus 4 pt before as a 12 pt gap, not 16 pt. Define the boundary calculation in terms of line/paragraph boxes and apply the appropriate format and contextual-spacing rules. Do not implement a blind sum and then shrink fonts to compensate.

ODF references: Part 3 sections 20.184/185 (break-after/before), 20.204 (line height), 20.214/228 (orphans/widows), 20.362 (default tab stops). OOXML references: ECMA-376 WordprocessingML plus the official SDK [SectionType](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.sectiontype?view=openxml-3.0.1) and [SpacingBetweenLines](https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.wordprocessing.spacingbetweenlines?view=openxml-3.0.1) remarks. SDK examples explain the format; they are not an Android parser dependency.

## 6. Proposed Plan 5e implementation sequence

Keep these as separately reviewable commits within the final Plan 5 sub-plan. Each stage runs the relevant synthetic tests and the twelve-fixture dump. Do not change all parser, metrics and renderer behavior in one untraceable patch.

### Gate 0: known baseline and fixture identity

1. Repair exact-case blank asset lookup; validate/regenerate remaining blank packages without substituting unrelated templates.
2. Capture the current full `testDebugUnitTest` report and fix the actual failures. PR #18's green run does not waive this gate.
3. Record sample SHA-256 digests, producer metadata, source/reference page counts and metric backend in a small fixture manifest/test helper. Future re-saves must update that evidence deliberately.
4. Correct dump headers/reason labels before using the dump to explain further changes.

### Commit 1: break and style semantics

Implement E11-02 through E11-06 with tiny in-memory or generated ZIP fixtures first. Model break-before, break-after and inline boundaries without textual marker pollution. Preserve source positions and paragraph identity across inline breaks. Preserve section kinds for Plan 8B rather than flattening every kind to a page break.

Default, explicit false, explicit zero, inherited exact and newly proportional values must remain distinguishable. Add widow/orphan, keep-together/keep-with-next and tab settings to the same resolved-style path, not a second resolver. Preserve empty paragraphs where they carry layout or editing meaning.

### Commit 2: make metrics the measuring path

Remove raw Paint/fallback fudge from `LayoutEngine`; inject an advance/line-metrics backend. Measure resolved paragraph and already-supported run styles through one contract and `LayoutUnits.ptToUnits`.

- Use declared first-line/start/end indents to determine each line's origin and available width.
- Handle hard newlines and positional tab advances. DOCX default tab interval belongs to settings; paragraph stops and clear overrides belong to paragraph properties. A tab definition inside pPr must never be appended to body text as if it were an inline tab.
- Keep line-to-source offset mapping exact, including whitespace and discretionary hyphens. Do not use text trimming as a layout optimization.
- Make the cache sensitive to content, resolved styles, width, measurement backend and relevant layout options. Test incremental output against a forced rebuild.

The JVM table is deliberately approximate and generic Paint is not bundled Liberation/Carlito/Caladea loading. Do not claim device parity merely because both routes return a matching family label. Full actual-face loading stays Plan 10, but backend identity and renderer parity checks are required now.

### Commit 3: line-fragment pagination

Add a layout-only fragment representation containing stable element identity, source range, line range, page bounds and continuation state. One logical paragraph can occupy several pages without duplicating document content.

Apply spacing at logical paragraph boundaries, not again on every continuation. Honor declared keep/widow/orphan rules with bounded lookahead and an explicit progress rule for content taller than a page. A keep chain that cannot fit must not cause an infinite flush loop. Retain both first-page and range/offset lookup semantics for navigation rather than silently overwriting `elementPageIndex` on every fragment.

Do not suppress every intentional blank page just to pass a "no blank pages" assertion. No *unexplained* blank pages is the invariant. Likewise, a page with one large table or image is not automatically defective because it has fewer than three elements.

### Commit 4: renderer, input and navigation consume that layout

Use computed body origin, bounds, line heights and fragments instead of an independent flowing Column. Remove constant block gaps and `sizeSp + 5f`. Verify that Compose does not independently wrap different text into those boxes; use a shared on-device layout/measurement result or measured parity checks, not family-name equality alone.

Adapt `DocumentTextWindows`, focus registration, selection and hit testing to fragment ranges while retaining one global edit value and the typing-buffer/undo protocol. A naive BasicTextField per fragment risks duplicated text, broken IME composition and lost selection. Test insertion, deletion and selection across a page boundary.

Resolve the Web View bypass in this stage. The lower-risk option is an honestly unavailable flow-view control until it can consume the same pipeline; do not invent a second paginator.

### Commit 5: acceptance and performance evidence

- Add `ParagraphMetricsTest`, fragment/pagination tests, spacing semantics, tab-stop and cache invalidation tests.
- Add `PaginationFidelityTest` for all twelve windows from `SampleMatrix.kt`, with parser/content invariants alongside page counts. Retain existing behavior tests when consolidating `Sample5UnifiedPaginationTest`; do not delete useful checks merely because its window is superseded.
- Assert empty document = one page; ordinary one-page document = one page; all source ranges accounted for exactly once; soft hints do not alter pagination; oversized content terminates; authored blank pages remain explainable.
- Cover variable-width-glyph hit testing, Viewer/Editor page parity, Go to Page, FCT, selection, undo/redo and session restore at 50/100/150/200/300 percent.
- Publish metric backend, per-page fragment bounds, authored/overflow end reasons and incremental/full-rebuild comparisons. Run a synthetic 100+ page stress document with timing/rebuild counts.
- Obtain an Android screenshot/geometry smoke pass before claiming visual pagination is fixed. The owner's full physical-device Chapter 1 pass remains after the later plans.

**If a window fails:** determine whether text/structure is missing, geometry is wrong, font substitution differs, a paragraph is split incorrectly, or media/table sizing is still a later-plan limitation. Publish the result. Do not silently widen windows, falsify measured counts, add break hints back, or inflate spacing. If an existing gate requires a later-plan dependency, request an explicit scope/gate decision supported by the dump.

## 7. Template manager ownership and online sources

### 7.1 Local-only gallery: Plan 11 home-entry package

This is the implementation home already identified in audit-009 / DESIGN; Plan 3 provides the honesty requirements and Plan 1 should make the ownership explicit.

Proposed contract from the owner's current instruction:

1. Recursively discover eligible assets **only under `app/src/main/assets/templates/`**. Exclude the three Untitled blank packages. Resolve the two internal assets and wizard-only entries through explicit eligibility rules.
2. Delete the nine fabricated sample entries and their descriptions, not just hide them. Remove the editor's fabricated resume fallback and make both template entry points use the same selected asset. `InkyModule.kt:1154-1157` still contains the sample resume; its handler can read the already-open file rather than the selected template.
3. Remove CSE/Gemini fallbacks from the local gallery path. Keep search/filtering offline and deterministic. UI labels can group by module while retaining actual `.ott/.ots/.otp` types.
4. Create a new unsaved working document from the selected package. Never edit/overwrite the source template. Converting template MIME/manifest/content types into a document is a package operation, not a filename-only rename; coordinate with Plan 9's writer support.
5. Use real metadata or a transparent filename fallback. Show a preview only when an embedded thumbnail or generated document preview actually exists. Test filtering, no network calls, blank exclusion, duplicate filenames, unsupported entries and new-document state.

Do not add a fourth Plan 5 subject by redesigning the gallery during metrics work. Only the broken blank lookup is an immediate prerequisite.

### 7.2 Online templates: later, explicit opt-in

The official [ONLYOFFICE repository](https://templates.onlyoffice.com/) advertises DOCX/XLSX/PPTX templates and PDF forms; it is not an ODF-only feed. The [LibreOffice Extensions repository](https://extensions.libreoffice.org/) includes templates and many non-template extensions. Filter by actual package capability and source metadata.

**Provider lifecycle warning:** Google's Custom Search JSON API is closed to new customers; existing customers have until January 1, 2027 to migrate. Do not make a new gallery depend on that API. Official current notice: [1](https://developers.google.com/custom-search/v1/overview).

Google Search grounding is a discovery aid, not a complete repository crawler or a guarantee that a citation URL downloads a document. The [Gemini grounding documentation](https://ai.google.dev/gemini-api/docs/google-search) describes search-grounded answers and attribution. An AI Studio switch is not by itself proof that the installed Android app sends a search-enabled request.

Recommended future pipeline:

- Separate source adapters and user-visible local/online modes; no network request for ordinary local gallery browsing.
- Treat model/search results as untrusted candidates. Resolve the official landing page and actual download, check redirects/allowed hosts, content type, ZIP/package structure, size/expansion caps and checksum before catalog admission.
- Record title, source URL, author, license/redistribution conditions, actual format, retrieval date and preview provenance. Do not promise to fetch "all templates" without a documented enumeration mechanism and usage permission.
- Do not execute macros or install arbitrary `.oxt` extensions. If supporting template bundles, safely inspect/extract only eligible template files.
- Keep API secrets out of logs. `TemplateSearchRepository.kt:70` currently logs the full request URL containing its key; remove/redact that before enabling the online path. Grounding citations are currently cast into template entries with guessed types (`:31-56`), which must also change.
- Honor provider attribution/display terms, quota, cancellation and offline behavior. Prefer an explicit source catalog with validated files over generated direct-download URLs.

No template downloads, repository crawl, provider account setup or online implementation occurred in this audit.

## 8. Writer Guide 26.2 Chapter 1 and the remaining plans

The checked-in `docs/lo-guides/WG262-WriterGuide_compressed.pdf` has Chapter 1 on PDF pages **17-40**, with Chapter 2 beginning on page **41**. The local PDF and [online 26.2 Chapter 1](https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html) are the behavioral references. Format layout rules come from ODF/OOXML, not a user-interface guide.

| Chapter 1 area | Current evidence / limitation | Owner |
|---|---|---|
| Title, menus, sidebar/decks, contextual tools | Code for title/status and File/Home decks exists; four ribbon decks are disclosed as unavailable. 26.2's nine sidebar decks are not nine shipped Papirus decks. | Plan 11; truthful capability states remain guarded by Plan 3 tests |
| Rulers, page information, zoom and views | Common page transform exists; metrics/body placement/alternate flow view still differ. Fit-to-width at 100% is Papirus's chosen mobile convention, not a universal Writer definition of 100%. | Plan 5e geometry; Plan 11 affordances |
| New/open/from-template | Local file paths exist; blank case mismatch and fabricated template catalog remain. | Immediate blank prerequisite; Plan 11 gallery |
| Save, Save As, autosave, reload and close | UI/control paths and regression tests exist. No claim that edited complex ODT/DOCX files preserve all structure. | Plan 9, with Plan 6 image-save refusal first |
| Go to Page / Navigator / Navigate By | Basic categories and navigation exist; supported structural content and accurate page/offset maps are dependencies. | 5e, 7A/B, 8A/B |
| Outline folding | Double-tap implementation exists; discoverability and accessible non-gesture controls still need work. | Plan 11 editor package / Plan 3 backlog |
| Reminders | Cap/cycling have regression coverage; preserve session-only behavior and do not serialize reminders into documents. | Regression guard and device checklist |
| Undo/redo, selection, caret | Preserve global edit value, typing-buffer flush and run slicing through fragment work. | Plan 5e regressions, then physical-device gate |
| Accessibility status, Style Inspector, remote storage, encryption, multiple desktop windows | Not established as shipped equivalents. Accessibility belongs in the Plan 11 mapping; remote/encryption/multiple-window features stay explicit exclusions unless separately approved. | Plan 1 scope ledger / Plan 11, not hidden Chapter 2 prerequisites |

Passing the project's twelve-section checklist completes the **agreed Chapter 1 scope**, not every desktop Writer capability mentioned by the chapter. Plan 1 should preserve that distinction.

### Remaining-plan order and boundaries

| Plan | Proposed focus and exit evidence |
|---|---|
| **1, after 5e** | Synchronize actual merged PR numbers, completed/open findings, 26.2 mapping, latest fixture references, test/backend/device status and ownership. Use stable Plan IDs for future work; old PR 17-22 predictions collide with the now-merged 5c/5d PRs. |
| **6** | Live-path image extents, bounded persistent/self-healing media, decode sizing and real progress; no artificial loading delays. Refuse unsafe image saves until round-trip support exists. |
| **7A/7B** | ODF numbering, heading runs, links/bookmarks, authored TOC snapshots, table geometry, font identity and sections. Use synthetic section cases where regenerated fixtures no longer contain the old structures. |
| **8A/8B** | Extend the existing DOCX metric cascade, not replace it; add run/character formatting, numbering, fields/TOC snapshots, tables, relationships and section geometry. Preserve section-type information introduced in 5e. |
| **9** | Pre-change package/relationship/style/media integrity gate, non-destructive save, untouched-part preservation and safe failures. Test open -> edit -> save -> reopen in Papirus and the originating applications. |
| **10** | Actual document Typeface loading and measured paint/layout parity; retain original document font identities. A substitution map is not proof a bundled file is used. Recheck pagination after real fonts load; do not force earlier counts by scaling. |
| **11** | Expressive dependency upgrade and theme foundations, verified UI font/icon provenance, local template gallery, entry/settings/editor/dialog/deck packages. Adaptive sizes, IME, TalkBack, contrast and reduced-motion evidence per package. Never restyle a nonfunctional command into an apparent working feature. |
| **Device gate** | Run all twelve `InkyC1Checklist` sections on physical hardware, including 100+ pages, imported samples, reload/save/reopen, selection and session restore. Record APK/commit/device/version and failures. Only then start Chapter 2 feature implementation. |

Small smoke tests during 5e do not replace the final device pass. Plans 7/8 and real font loading may change page counts for valid reasons; retain source evidence and tighten targets only with measured support, never below the agreed references by deleting content.

## 9. Reference and documentation corrections for Plan 1

1. **Actual paths:** ODF specs are under `docs/odf/`, not `docs/html`; guide PDFs under `docs/lo-guides/`, not directly in `docs/`; five design summaries under `docs/design.md-references/`. Fix active documentation links, leaving dated historical observations identifiable.
2. **Audit-010 is not authoritative for regenerated geometry:** it repeats old Sample-3 zero-margin and Sample-4/5 fixed-header examples despite audit-008's rebaseline. Use current XML and `SampleMatrixTest` rather than copying those acceptance numbers.
3. **5d status:** the master index still calls 5d active next. Record it as merged with the remaining edge cases linked here; retain 5e as unfinished until its gates pass.
4. **Guides versus design summaries:** the checked-in `*-design.md` files are alpha website-style notes, not official Android UI specifications. DESIGN v3 already states this correctly. Keep Material/Google primary and other sources bounded by task.
5. **UI typography versus document typography:** audit-009's malformed/duplicate Google Sans asset finding remains a validation task, not a reason to substitute UI fonts into saved documents. This audit did not rerun a font-binary validation.
6. **Java/native references:** targeted review of `sdk-references/StyleCreation.java` and `HardFormatting.java` illustrates explicit PAGE_AFTER, style families and direct property state. These examples are references, not evidence Papirus executes UNO paragraph layout. The online LibreOffice ParagraphProperties API currently identifies itself as 26.8 SDK documentation; do not relabel it as the pinned 26.2 guide.
7. **Native status:** PR #18's CI inventory lists real ARM ELF libraries and LibreOfficeKit exports, but no matching named `Java_com_example_core_jni_LibreOfficeCore_*` symbols for the five Kotlin native methods. Together with the fallback source, that supports caution, not a claim that the Kotlin renderer is backed by a working native Writer engine. Symbol inventory alone cannot exclude dynamic registration; prove the actual runtime bridge before claiming native capability. Do not fetch hundreds of MB of LFS binaries for this metrics plan.
8. **Historical overclaims:** audit-001's token-color change does not guarantee contrast; audit-002's broad all-controls claims and audit-010's Chapter 1 PASS table are not current device evidence. Record tests and screenshots rather than repeating those labels.

## 10. Delivery gate and verification record

| Gate | Result for this document |
|---|---|
| R-02 | No em dash in the new audit. |
| R-15 / R-16 | Next actions name concrete code paths, tests and owners; no marketing or generated template descriptions. |
| R-17 | Sample/package counts measured from current files; historical CI counts explicitly labeled; current failing job distinguished from PR #18's green run. ZIP/XML checks are not called schema or rendering validation. |
| R-36 / C-5 | Implemented, source-inspected, historical, proposed and device-unverified states separated. No full Chapter 1/native/format compatibility claim. |
| R-38 | Identifies existing fabricated catalog/resume content; does not add replacement dummy content. |
| R-26 / R-27 / R-32 / R-34 / R-35 | N/A: no UI shipped in this deliverable. Their later implementation/device requirements are retained. |

Local evidence collected: all 16 test-package CRC/XML checks; all 115 template-asset CRC/XML checks; exact-case path comparison; raw break and producer metadata; local ODF normative text; local Writer PDF chapter boundaries/content; targeted source inspection. Remote evidence: PR merge/check/comment data and current CI check annotations via `gh`, official Writer/SDK/provider/repository pages. Local Gradle, schema validator, renderer screenshots and physical-device tests were not run.

**Next implementation recommendation:** approve Plan 5e with Gate 0 first, then the five isolated commits above. Defer the full local gallery to Plan 11 and online discovery to a separately scoped, provider-independent extension.
