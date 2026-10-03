# Papirus Office: Plan 5c pre-write analysis (documentation refresh)

> **Current Plan 7 sequence (2026-10-03):** 7B canonical model/importer convergence -> 7C authored indexes and named sections -> 7D tables end to end -> 7E font-face aliases/final calibration, with exactly one PR per plan. This sequence supersedes any older combined package or downstream PR forecast; see `audit-014-2026-10-03-plan-7b-convergence.md`.


**Date:** 2026-09-27
**Plan:** 5c, documentation only. No app code, resources, tests or workflow files change in this plan.
**Input:** read-only sweep of `AGENTS.md`, `DESIGN.md` (v2.0), `PROJECT_CONTEXT.md`, `CONCEPT.md`, `antislop.md` + `skills/antislop-*`, `anti-slop/plan-*` and `audit-001..008`, PR #1..#16 on GitHub, the Create New / Welcome / theme / options code, `assets/templates`, `app/src/main/libs`, `tests/`, `docs/`, and the online LibreOffice Writer Guide 26.2 Chapter 1.
**Method note.** Every "shipped" statement below carries a `file:line` or a command result. Nothing was compiled or run on a device in this sandbox (no JDK, no LFS objects, no outbound web except GitHub and the fetch tool). Items that still need a device are marked **[needs run]**.
**Output of this document:** a numbered list D-01..D-24 that Plan 5c will resolve or record, a proposed `DESIGN.md` v3.0 structure, the list of reference files the rewrite needs, and (§9) the owner's decisions plus the reference screenshots received so far.
**Status after commit 1 (5c-1):** D-01, D-02, D-03, D-09, D-10, D-11, D-12, D-13, D-14, D-15, D-16 (three project docs), D-17, D-18, D-19, D-21, D-24 are resolved in `AGENTS.md`, `PROJECT_CONTEXT.md`, `DESIGN.md` v2.1, `CONCEPT.md` and `docs/InkyC1Checklist.md`; D-04..D-08 and D-22 are recorded as labelled deltas for the owning code plans; D-20 and D-23 close with `DESIGN.md` v3.0.
**Status after 5c-2 documentation pass:** D-08 (Welcome shipped/target brief), D-20 (shape scale) and D-23 (identity/motif/surface briefs) are closed in `DESIGN.md` v3.0; D-04..D-07 and D-22 remain labelled code deltas. The newly observed malformed UI font resources, Drive placeholder copy and Cellina/Slidia/Pagella sample/claim issues are recorded in §9.3 for later asset/UI plans, not silently treated as solved by this document.

---

## 1. Where the project stands

| PR | Merged | Plan | What it did |
|---|---|---|---|
| #1, #3..#10 | 09-18 to 09-23 | audits 001..004 | security/crash fixes, LOKit simulated seam, Navigator, page geometry, unified page stack, span/style fidelity |
| #11 | 09-24 | Plan 2 | page stack fits viewport, one 48 dp status bar, no floating Edit FAB |
| #12 | 09-24 | Plan 3A | literal toasts and contentDescriptions moved to `strings.xml`, source hygiene guard |
| #13 | 09-24 | Plan 3B | ribbon reduced to 6 tabs, dead hub tools labelled, Navigator categories |
| #14 | 09-25 | docs | `DESIGN.md` v2.0 hybrid direction, Plan 11 |
| #15 | 09-26 | Plan 5a | units, styles seam, fonts, metrics, element dump, native inventory CI step |
| #16 | 09-27 | Plan 5b | fixture re-baseline on M365 DOCX + Collabora 26.04 ODT (audit-008) |
| next | | **Plan 5c** | this documentation refresh |
| then | | 5d, 5e, Plan 1 index, Plans 6..11 | breaks/defaults, metrics/windows, master index, Writer fidelity, UI packages |

Base of Plan 5c: `d604290`. After PR #17 was opened, the owner added the reference files and all five LibreOffice 26.2 PDFs to `main` in batches `eb0f1a8..ad0076d`. This branch merged `origin/main` as `86cf1a3`; that merge adds upstream reference material, not app code written for 5c.

---

## 2. Writer Guide 26.2, Chapter 1: what the docs currently cite and what changed

Before 5c, the docs cited three different editions: `DESIGN.md` front matter linked the **7.2** PDF, `anti-slop/plan-01-master-index.md` maps against **24.8**, and the project standard (user decision 2026-09-27) is **26.2**. The 26.2 chapter is published online at `https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html` (published March 2026, "Based on LibreOffice 26.2"). The full 26.2 shelf (Getting Started, Writer, Calc, Impress, Draw, Math) is at `https://books.libreoffice.org/en/index.html`.

Deltas between the 24.8 mapping in plan-01 and the 26.2 text that matter for `DESIGN.md`, `CONCEPT.md` and the checklist:

| 26.2 Chapter 1 item | What changed or what Papirus has | Doc consequence |
|---|---|---|
| Sidebar has **nine** decks by default: Properties, Styles, Gallery, Navigator, Page, Style Inspector, Manage Changes, Accessibility Check, Find | Papirus has Ribbon, Navigator, Navigate By and Formatting decks in the Standard Bottom Sheet | Cite nine decks in the concept mapping; Accessibility Check and Find have no Papirus deck yet, say so |
| Status bar adds "Status of the accessibility check" and keeps Page number (click opens Go to Page), word/character count, page style, language, insert/selection mode, view layout, zoom | Papirus status bar (PR 11): Page x of y (opens Go to), words/chars, zoom | Mapping table in plan-11 / DESIGN §8 should list 26.2 fields and mark which exist |
| "Starting a new document": Start Center offers Create: Writer Document, Templates, Open File, Remote files, recent thumbnails with a type filter and a remove-thumbnail action | Papirus: Start Screen FAB opens the Create New screen; Files tab + SAF picker = Open File; Google Drive = honest placeholder; filter chips = type filter | This is the section the Create New docs must mirror (see §3) |
| "From a template": a template is "a set of predefined styles and settings"; a new installation "may contain only a few templates"; more from extensions.libreoffice.org | Papirus bundles 112 real LibreOffice templates in `assets/templates/` but surfaces none of them; the gallery lists remote sample files instead | Docs must not call the gallery a template library (D-04) |
| Untitled documents are named "Untitled X" | `strings.xml:533` `default_document_title` = "Untitled Document" (Plan 3A fixed the Indonesian default) | OK, no doc change |
| View > User Interface > Single Toolbar; note that Writer has other UI variations (contextual groups) | Papirus's Standard Bottom Sheet is the mobile analogue of the contextual/tabbed UI | Keep the existing DESIGN §7 adaptation note; cite 26.2 |
| Navigator: Navigate By box, Previous/Next, category list, Content Navigation View, rename objects | Papirus has Navigate By + Previous/Next + categories (PR 4, PR 13) | No doc change in 5c; Plan 1 re-maps against 26.2 |
| Reload, Close (save prompt), Undo list, multiple windows | Papirus: Reload, Close prompt, Undo/Redo exist (`docs/InkyC1Checklist.md`); multiple windows are out of scope on phone | Checklist header should cite 26.2 |

Nothing in the 26.2 text invalidates the Chapter 1 completion criteria in `docs/InkyC1Checklist.md`; the checklist only needs its edition line updated.

---

## 3. Create New: shipped behaviour versus the three documents

### 3.1 What ships (source of truth for the 5c rewrite)

Entry points:
- Start Screen FAB (`HomeDashboard.kt:534-550`) calls `onNavigateToModule("create_new_document")`; `MainActivity.kt:395` renders `NewDocumentScreen`.
- First run only: `MainActivity.kt:304-306` shows `WelcomeScreen` when `papirus_first_run/is_first_run` is true. `WelcomeScreen.kt` is one centered column: 112 dp logo tile with 28 dp corner radius (`WelcomeScreen.kt:40-52`), title `welcome_title`, body `welcome_desc`, one full-width 56 dp button labelled `welcome_grant_btn` = "Get Started". No permission is requested; the key name is a leftover from an earlier permission-grant design.

`NewDocumentScreen.kt` (993 lines):
- `TopAppBar` titled `create_new_document` ("Create New Document"), back arrow, and a search icon that is visible only on tab 2.
- A two-page `HorizontalPager` with a bottom `NavigationBar`: tab 1 `tab_create_new` ("Create New"), tab 2 `tab_from_template` ("Create from Template"). The pager opens on tab 1, so the shipped flow is **module-first**, not template-first.
- **Tab 1, section "Create New Document":** three equal cards (Inky Document, Cellina Spreadsheet, Slidia Presentation; 48 dp logo, `RoundedCornerShape(16)`, 1 dp `outlineVariant` border). Each card sets `MainActivity.pendingNewDocument = true` and opens the module on the bundled blank package: `TemplateManager.getInkyNormalTemplateFile` / `getCalcDefaultTemplateFile` / `getSlidiaDefaultTemplateFile` extract `assets/templates/untitled.odt|ods|odp` (fallbacks `styles/Default.ott`, `wizard/styles/default.ots`, and a `slidia/Default.otp` that does not exist in assets).
- **Tab 1, section "Create Pagella PDF Document":** a grouped list of three rows: Create from Image (`image/jpeg|png|webp` via `PagellaPdfCreator.createPdfFromImageUri`), Create from Camera (CAMERA permission, FileProvider, `createPdfFromImageFile`), Convert from Document (ODF, OOXML and legacy MS MIME types via `convertDocumentToPdf`). Each opens Pagella and shows a toast.
- **Tab 2 "Create from Template":** `FilterChip`s All / ODT / ODS / ODP, a search field, and `TemplateManager.searchTemplates`. The first result set is the hard-coded `curatedTemplates` list (12 items): three `asset://templates/untitled.*` entries named "Official LibreOffice Writer Blank / Calc Blank / Slidia Presentation", and **nine entries that point at `https://filesamples.com/samples/document/od{t,s,p}/sample{1,2,3}.*`** with invented names and descriptions ("Resume (Modern Layout)", "Business Proposal", "Monthly Personal Budget", "Executive Business Slide Deck", and so on). Fallbacks: Google Custom Search (`TemplateSearchRepository`), then a Gemini-generated JSON list of URLs. Downloads land in `getExternalFilesDir("templates")` with a ZipSafe size cap and sanitized names; a downloaded template opens as an ordinary file (no `pendingNewDocument`). Empty states exist for offline (CloudOff icon on a radial-gradient glow) and no results (Article icon on a primary glow).
- Hard-coded literals on this screen: "Querying ODF repositories...", "Papirus Template • ${type}"; raw colours `0xFF2563EB` / `0xFF10B981` / `0xFFD97706` per template type and `Color(0xFF10B981)` for the check icon.
- No template preview images anywhere.

Also relevant:
- `InkyModule.kt:771,1154-1219,1972`: the editor has its own "Create from Template" dialog whose load path replaces the document body with a hard-coded "RESUME (MODERN) / John Doe / University of Antigravity" text (`InkyModule.kt:1157`). Still open from plan-01 §3.2.
- `audit-008 §6`: `untitled.odt|ods|odp` are hand-built packages without `office:version`, `meta.xml` or a page layout (and `mimetype` is not the first zip entry in the odt), so a blank Inky document opens on the Letter fallback rather than a declared A4 page. Regeneration from Collabora 26.04 plus a `TemplatePackageTest` is queued for a later code plan.
- `assets/templates/` holds 115 files: 3 `untitled.*`, `styles/{Default,Modern,Simple}.ott`, `officorr/*`, `offimisc/*`, `personal/{CV,Resume1page}.ott`, 23 `presnt/*.otp`, `wizard/{agenda,letter,report,styles}/*`, `l10n/*`, `internal/*`. Only the three `untitled.*` files are reachable from the UI.

### 3.2 Findings against the documents

| ID | Where | Claim | Reality | Rule | 5c action |
|---|---|---|---|---|---|
| D-01 | `AGENTS.md:81-82` | Create New = "Template selection (Blank Document, Resume, Letter, Invoice, Report, Agenda) and direct module creation" | No such template set exists in code or assets under those names | R-36 | Rewrite the entry from §3.1 |
| D-02 | `PROJECT_CONTEXT.md:78-80` | "Template Gallery: ... Formal Letter, Modern Resume, Meeting Agenda, Academic Report, Invoice, Project Plan" and a "Scan/Import PDF (Pagella)" quick-create | Gallery names are not in code; Pagella has three concrete entries (Image, Camera, Convert from Document) | R-36 | Rewrite §2 of PROJECT_CONTEXT from §3.1 |
| D-03 | `DESIGN.md:35`, `CONCEPT.md:34`, `plan-11:20,74` | Welcome / Create New = WPS "template-first creation flow" | Shipped flow is module-first (tab 1) with templates on tab 2 | evidence | Keep WPS as the pattern source, describe the flow as it ships, mark template-first as a target if the reference files confirm it |
| D-04 | `TemplateManager.kt` `curatedTemplates` | (docs call this a template library) | 9 of 12 entries are third-party sample files with invented names and descriptions | R-38, R-17, R-36 | Docs: call them "remote sample files used as placeholders" and list removal/replacement as a code item for the Plan 11 home-entry surfaces package; no code change in 5c |
| D-05 | `InkyModule.kt:1157` | (not documented) | In-editor "Create from Template" injects fabricated resume content | R-38 | Record as known delta in PROJECT_CONTEXT §2; code fix stays in Plan 3 backlog |
| D-06 | `assets/templates/`, audit-008 §6 | Docs imply "blank document templates" | Blank packages are non-conformant and open on Letter; 112 real templates unsurfaced | evidence | State both facts; point to the regeneration item |
| D-07 | `NewDocumentScreen.kt`, `strings.xml:69` | AGENTS "translate all strings ... add to strings.xml" | Two literals and per-type raw hex colours on this screen; "Get Started" is a generic CTA | R-15, R-29 (code) | Docs must not claim the screen is fully tokenised; list as known delta |
| D-08 | `CONCEPT.md`, `DESIGN.md:35` | Welcome = "friendly entry" | One centered column, one button, first run only, no permission request | evidence | Describe as shipped; the WPS-inspired target goes in the surface brief once the reference files are in |

---

## 4. Other stale or unverifiable claims found on the way

| ID | Where | Claim | Reality (evidence) | 5c action |
|---|---|---|---|---|
| D-09 | `PROJECT_CONTEXT.md:160-161,192` | Ribbon has References and Mailings tabs | PR 13 reduced the ribbon to File, Home, Insert, Layout, Review, View; `AGENTS.md:51` already says so; `strings.xml` has no references/mailings tab keys | Remove the two bullets and fix the glossary row |
| D-10 | `PROJECT_CONTEXT.md:216-219` | `docs/html` contains `OpenDocument-v1.4-cs01-part*.odt` | Directory holds `OpenDocument-v1.4-part1..4-*.html` | Fix the tree |
| D-11 | `PROJECT_CONTEXT.md:104` | Loading screen loads `libsofficeapp.so` | The probed library is `liblo-native-code.so` (`LokitEngine.kt:8-11,41`) | Fix the name; describe the SIMULATED fallback |
| D-12 | `PROJECT_CONTEXT.md:312` | "Always run `compile_applet` before completing any modification turn" | No such script or Gradle task exists in the repo; CI runs `./gradlew testDebugUnitTest` (`build.yml:49-53`) | Replace with the real verification path (unit tests in CI, device test by the owner) |
| D-13 | `DESIGN.md:8`, `plan-01`, `docs/InkyC1Checklist.md` | Writer Guide 7.2 / 24.8 | Project standard is 26.2 (§2) | Point every citation at `books.libreoffice.org/en/WG262/` |
| D-14 | `audit-006 §2.1`, `PROJECT_CONTEXT.md:51-53` | Native `.so` files are "~60 KB stubs" | They are Git LFS objects: the pointers declare 187.1 MB (arm64-v8a) and 128.5 MB (armeabi-v7a) for `liblo-native-code.so`; this sandbox sees 130-byte pointers because LFS is not installed. CI checks out with `lfs: true` and runs `scripts/native-inventory.sh` (`build.yml:28,45`) | Docs: say "LFS-tracked official LibreOffice Viewer binaries; runtime status reported by `LokitEngine.statusLabel`; the CI native-inventory artefact is the evidence"; do not repeat the stub figure |
| D-15 | `AGENTS.md:139-142` | build.yml rules | Typos: `mske_latest`, `releae`, `stritcly`, `${{ github-sha }}` (the real expression is `${{ github.sha }}`) | Fix wording |
| D-16 | `DESIGN.md` (2), `AGENTS.md` (6), `PROJECT_CONTEXT.md` (18) | em dashes | R-02 governs agent-written text; these files are agent-written | Replace while editing the files; `antislop.md` and `skills/` are upstream files and stay as they are |
| D-17 | `DESIGN.md:87` | "Default icon family: Material Symbols Rounded" | Code uses `androidx.compose.material:material-icons-extended` (`Icons.Rounded.*`), which is the older Material Icons set, not Material Symbols; `Icons.Default.*`/Filled also occur (e.g. `CellinaModule.kt:569`, `PagellaModule.kt:147`); `libs.versions.toml` has no Symbols dependency | Label Material Symbols as target; state the shipped set |
| D-18 | `DESIGN.md:49-54`, `AGENTS.md:87`, `PROJECT_CONTEXT.md:91` | Theme modes System dynamic, Papirus static, Custom, Wallpaper ("verify current implementation") | Verified: `ThemeSettings` stores `dynamic_color_enabled` and `theme_mode_preference` (SYSTEM/LIGHT/DARK) only; Options exposes a dynamic-color switch and the three modes (`PapirusOfficeOptionsScreen.kt:699-776`); no custom seed, no wallpaper palette, no image picker | Split into shipped (2 modes) and target (Custom, Wallpaper) |
| D-19 | `DESIGN.md:70-73`, `Type.kt` | Google Sans target; "Google Sans Flex" family | `GoogleSansFlexFontFamily` is built from `google_sans_{light,regular,medium,bold}.ttf`, not from the bundled `google_sans_flex_*.ttf`, so display/headline/title and body/label request the same static files; `google_sans_flex_*` files are bundled but unused (the body family does load `google_sans_italic`); `FontLoadingStrategy.OptionalLocal` with intended SansSerif fallback; the bundled files later failed basic name-table inspection (§9.3) | Describe what code requests, not what rendered; keep the on-device validation gate **[needs run]** |
| D-20 | `DESIGN.md:103-104` | "small semantic shape scale" | No `Shape.kt`; radii are ad hoc: 16 dp module cards, 12 dp icon tiles, 28 dp search field and 112 dp Welcome logo tile with 28 dp radius (`NewDocumentScreen.kt`, `WelcomeScreen.kt:40-52`) | v3.0 defines the scale and lists the shipped inventory as a delta |
| D-21 | `DESIGN.md:18,41`, `AGENTS.md:23` | "Material 3 Expressive supplies the design system" | Compose BOM `2024.09.00` (material3 1.3.0) has no `MaterialExpressiveTheme`, `MotionScheme`, emphasized type styles or the new components; the app uses classic M3 with dynamic colour | State M3 Expressive as the target and the BOM upgrade as a prerequisite owned by a code plan |
| D-22 | `PapirusOfficeOptionsScreen.kt:96,717,760,765` | AGENTS "all strings in strings.xml" | Appearance copy is hard-coded ("Appearance & Theme", "Dynamic Color (Material You)", ...) | Known delta for the Options package; docs must not claim otherwise |
| D-23 | `DESIGN.md` as a whole | antislop: `DESIGN.md` holds identity, personality, palette, typography, mood, dials (data, not commands) | v2.0 is mostly rules and acceptance gates; it has dials but no personality, mood, motif, voice or imagery direction, and it repeats rules that live in `AGENTS.md` and `antislop.md` | v3.0 is direction-first (see §5) |
| D-24 | `PROJECT_CONTEXT.md:82-85`, `DESIGN.md:36` | About = "vertically paged sections" | `AboutScreen.kt` is a `Scaffold` + `LazyColumn`; no pager | Mark as target |

---

## 5. `DESIGN.md` v3.0: proposed structure

The rewrite is written **from the reference files the owner supplied through GitHub**, in this priority: Material / Google sources first; WPS Office, Microsoft and LibreOffice second; the LibreOffice 26.2 guides for office concepts. Nothing in v3.0 is invented by the agent: every direction statement names its source file, and every shipped statement names `file:line`.

```
front matter: version 3.0, status, Dial line, source list (uploaded files by name + online URLs)
0. How to read this file: "target" vs "shipped (verified YYYY-MM-DD)"; what antislop extracts from it
1. Identity and personality: what Papirus is, who it is for, mood words, the identity motif, what it is not
2. Reference map: surface -> primary source -> borrowed pattern -> what is deliberately not borrowed
3. Colour: system dynamic default (API 31+), Papirus static schemes as shipped in Color.kt, module accents,
   document vs chrome, contrast floor; Custom and Wallpaper modes as target
4. Typography: what Type.kt loads today, the M3 scale in sp, emphasized styles as target (after BOM upgrade),
   document fonts stay separate
5. Shape, spacing, elevation: M3 shape scale for Papirus, 4/8 dp rhythm, shipped radius inventory as delta
6. Motion: M3 Expressive spring schemes as target, shipped baseline (no MotionScheme in BOM 2024.09), reduced motion
7. Icons and imagery: shipped Material Icons Rounded, target Material Symbols Rounded, Colibre parked, logos, empty states
8. Surface briefs (one each): Welcome, Start Screen tabs, Create New, Options / Crash Logs / About,
   editor screens, bottom-sheet decks (Inky, Cellina, Slidia, Pagella). Each brief: source pattern,
   shipped baseline with evidence, target, known deltas and the plan that owns the code change
9. Content and copy: en_US, no em dash, specific CTAs, honest placeholders and unavailable states
10. Accessibility floor: 48 dp, 4.5:1, focus, TalkBack labels, reduced motion, keyboard (pointer to antislop-human)
11. Standards boundary: ODF 1.4 in docs/html, ECMA-376, Writer Guide 26.2 chapter map
12. Evidence log and the Delivery Gate result for this document
```

Sizing note (pre-write proposal): v2.0 was 136 lines and v3.0 was estimated at 300-400 lines. The owner chose **one file with surface briefs**; final v3.0 keeps everything in `DESIGN.md`. The final numbering places the briefs in §9 after the Material 3 Expressive adoption contract (§3), rather than splitting into `docs/design/`.

---

## 6. antislop for this plan

- **Mode question:** must be asked before the copy work starts (project default is Mode 1, During). Documentation is copy, so `skills/antislop-copywriting/SKILL.md` applies to the prose; `antislop-ui` and `antislop-layoutmobile` apply to the design statements inside `DESIGN.md`.
- **What the Delivery Gate means for a docs-only deliverable:** R-02 (no em dash), R-15 (no generic CTAs quoted as recommendations), R-16 (no buzzwords), R-17 (every number has a source), R-36 (no fabricated claims), R-38 (no fabricated content), C-5 (evidence over claims), plus the project's own "target versus shipped" labelling. R-26/R-27/R-32/R-34/R-35 are marked N/A with the reason "no UI shipped in this plan".
- **Upstream core:** `antislop.md` and `skills/*` are the unmodified upstream release (there is no `VERSION` file, so the release cannot be named). Editing `antislop.md` would be lost on the next `npx antislop-ai --update`. Recommendation: leave the core untouched and add a short **project addendum** to `AGENTS.md` above the pointer block with the four rules the core does not state: (1) label every UI statement in docs as target or shipped with evidence; (2) app-to-app references are task-level patterns bounded by surface (how R-30 applies to the hybrid map); (3) docs-only deliverables run the reduced gate above; (4) the reference map and Writer Guide edition are pinned in `DESIGN.md` front matter. The owner can instead ask for the addendum inside `antislop.md`; both options are cheap.

---

## 7. Strategy and sequence for Plan 5c

Work packages, each a separate commit on this branch, in this order:

1. **5c-1 Create New truth pass** (no reference files needed): rewrite `AGENTS.md` §Application Screens item 2, `PROJECT_CONTEXT.md` §2, and the Create New row in `DESIGN.md` §3 from §3.1 of this document; add the known-delta list (D-04..D-07) with owning plans. Also D-09..D-12, D-14, D-15 in the same files.
2. **5c-2 DESIGN.md v3.0** (after the uploads): structure from §5; sources cited by filename; shipped facts from `Theme.kt`, `Color.kt`, `Type.kt`, `NewDocumentScreen.kt`, `WelcomeScreen.kt`, `PapirusOfficeOptionsScreen.kt`, `AboutScreen.kt`; D-13, D-16..D-21, D-23, D-24 resolved here. `CONCEPT.md:34` and `plan-11` rows get a one-line alignment edit only where they contradict the new file.
3. **5c-3 Edition pin**: every Writer Guide citation to 26.2 (D-13), including `docs/InkyC1Checklist.md` header; leave the plan-01 chapter re-mapping to Plan 1.
4. **5c-4 Delivery Gate report** in the PR body (reduced gate from §6), then PR "Plan 5c: documentation refresh (Create New truth pass, DESIGN.md v3.0, edition pin)".

Out of scope, recorded for the owning plans: the fabricated gallery entries and the in-editor resume text (Plan 11 home-entry surfaces package / Plan 3 backlog), blank-template regeneration (5d or the template package), BOM upgrade for M3 Expressive (Plan 11), Options and Create New literals (Plan 3 backlog), icon-set migration (Plan 11).

---

## 8. Reference files originally requested from the owner (resolved in §9)

The rewrite needs the files below in the workspace. Formats that can be read here: PDF (text is extracted with pypdf/pdfminer; scanned PDFs cannot be read), ODT/ODF (unzipped and parsed as XML), Markdown/HTML/TXT, and PNG/JPG screenshots (viewed as images). Please upload the files themselves; links alone are not enough for offline reading, although the online 26.2 guides can also be fetched chapter by chapter.

**Primary (Material / Google)**
1. Material 3 Expressive guidance the owner wants followed (design PDF or exported pages): colour roles and dynamic colour, type scale and emphasized styles, shape scale, motion schemes, and the component pages for navigation bar, FAB, bottom sheets, dialogs, cards, chips, lists, search.
2. Google Workspace mobile screenshots (Docs, Sheets, Slides on Android): home tabs, create flow, an editor dialog or sheet. These anchor the Start Screen and editor-dialog briefs.
3. Google Sans / Google Sans Flex usage or licence note the project relies on (so the typography section can state what may be shipped).

**Secondary**
4. WPS Office Android screenshots: first-run welcome and the new-document / template screen (source for the Welcome and Create New briefs).
5. Microsoft 365 Copilot mobile screenshots: a Word editor screen and the bottom command sheet (editor and deck briefs); FlexiPDF for the Pagella deck if available.
6. LibreOffice 26.2 guides: Getting Started, Writer, Calc, Impress, Draw (PDF or ODT). Chapter 1 of each and the Start Center / template chapters are the parts the design file cites; the Writer Guide Chapter 2 is needed later for the post-device-test feature work, not for 5c.

**Optional but useful**
7. Any brand assets or an existing brand note (logo files are in `res/drawable`, but a written identity statement, if one exists, would replace agent-written personality text).
8. Device screenshots of the current Welcome, Start Screen, Create New (both tabs) and Options > Appearance, so the "shipped" statements can cite a picture as well as code.

---

## 9. Owner decisions and reference material received (2026-09-27, second session)

Decisions: antislop **Mode 1 (During)**; Create New documented as **shipped + target, labelled**; project antislop rules live in an **`AGENTS.md` addendum** (upstream `antislop.md` untouched); `DESIGN.md` v3.0 stays **one file with surface briefs**; the owner will follow **all** Material 3 Expressive guidance, so Material is the primary source for v3.0.

Upload status: the message listed `OFL.txt`, `README.txt` (Google Sans package), `libreoffice.org-design.md`, `material.io-design.md`, `office.com-design.md`, `wps.com-design.md`, `google.com-design.md`, `GS262-GettingStarted_compressed.pdf`, `IG262-ImpressGuide_compressed.pdf` and three screenshots. That first attempt delivered only three Word screenshots inline. The subsequent attachments still did not populate `/home/user/uploads/`, but the owner committed the five design notes, `OFL.txt`, `README.txt`, and five 26.2 guides to `main`. Commit `86cf1a3` merged the files into this branch. The owner also supplied eleven M365 mobile screenshots inline (three Word, four Excel, four PowerPoint). None of the screenshot JPEGs is in Git; the observations in §9.1-9.2 are from the inline images, not from any checked-in asset. The guide PDFs and reference notes were read locally; the site notes are alpha website summaries, not photographs of the corresponding mobile apps.

### 9.1 Screenshots: Microsoft Word for Android, dark theme, `Sample-1.docx` (the Inky DOCX fixture), Indonesian UI

| Screenshot | What it shows | Use in v3.0 |
|---|---|---|
| 17:32, reading view (`Screenshot_2026-09-27-17-32-41-62_4a24d271e133915ae237d4bec6ffe368.jpg`) | Top bar: close (X), title "Sample-1 - Disimpan" (saved state in the title), cloud-upload, search, overflow. Document rendered as full pages on a dark gap with the page number at the page foot. Bottom navigation bar with five equal items, icon above label: Tampilan Seluler (mobile view), Judul (headings), Edit, Bagikan (share), Baca Dengan Lantang (read aloud). | Editor-screen brief, Viewer mode: one bottom bar owns view toggle, outline, edit, share and read-aloud; page stack with visible page numbers; save state shown in the title, not as a toast |
| 17:33, editing with keyboard (`Screenshot_2026-09-27-17-33-00-31_4a24d271e133915ae237d4bec6ffe368.jpg`) | Top bar: done (check), title with saved state, format (A with pen), cloud-upload, search, page view, undo, overflow. Above the keyboard: one compact formatting strip (Bold, Italic, Underline, highlight swatch, font-colour swatch, bulleted list, numbered list, indent) ending in an expand chevron that opens the full deck. Selected toggles use a filled container. | Toolbar Hub brief: single-row strip pinned to the keyboard, trailing expand control, state shown by container fill; the compact strip and the full deck share icons and order |
| 17:33, Home deck expanded (`Screenshot_2026-09-27-17-33-08-11_4a24d271e133915ae237d4bec6ffe368.jpg`) | Deck header row: tab name "Beranda" with an up/down chevron as the tab switcher, a lightbulb (assistant), undo, redo, collapse. Content as rows on a dark surface: a two-field row "Times New Roman" and "14"; a four-segment toggle row (B, I, U, strikethrough) with Italic highlighted; visually tall list rows with leading icon, label and trailing chevron: Sorot (highlight, swatch under the icon), Warna Font (font colour, red swatch), Bersihkan Pemformatan (clear formatting), Pemformatan Lainnya... (more formatting, opens a subpage). Rows are full-width and separated by hairlines; physical dp height cannot be measured from a screenshot without device density. The document stays visible above the deck. | Standard Bottom Sheet brief for Inky, Cellina, Slidia: header = tab switcher + undo/redo + collapse; content = field rows, segmented toggles and list rows with chevrons into subpages; the deck reads as a settings-like list, not as a desktop ribbon strip |

Observed constants worth carrying into the brief as targets: the deck occupies roughly the lower 40 to 45 percent of a 20:9 phone screen at default height; every row should provide a full-width 48 dp+ target (Papirus rule, not a measured Microsoft dp size); thin colour swatches sit under the icon; the deck and the strip use the same glyphs; the assistant entry point is in the deck header rather than the canvas.

### 9.2 Screenshots: Microsoft Excel and PowerPoint for Android, dark theme, Indonesian UI

The images below are inline chat evidence, not test runs of Papirus. Their filenames identify the screenshots; those JPEGs are not part of the Git tree. UI strings visible in them are Indonesian; Papirus copy remains en_US.

| Screenshot filename (prefix after `Screenshot_2026-09-27-`, common suffix `_4a24d271e133915ae237d4bec6ffe368.jpg`) | Observation and bounded use |
|---|---|
| `19-47-43-65` (Excel, viewer) | Sheet grid fills most of the screen; row/column headers and selected table are visible. Top title includes a saved state. Above the bottom sheet tabs is a horizontal summary of selected values (sum, average, count). Tabs name sheets and offer add-sheet. Use in Cellina viewer brief: keep grid, sheet identity and selection summary legible. |
| `19-47-48-55` (Excel, quick tools) | Same workbook, but sheet tabs give way to a compact formatting toolbar at the bottom; selection summary persists above it. Use in Cellina Toolbar Hub brief: the selection summary and command strip have different roles. |
| `19-47-57-66` (Excel, cell edit + keyboard) | Selected cell border; formula entry row with `fx`, red cancel and green commit; navigation/number/undo tools and keyboard below. Use in Cellina brief: commit/cancel and formula context remain visible with the IME. |
| `19-48-02-45` (Excel, Home deck) | Expanded command deck with Home tab switch, assistant, undo/redo, font and size row, bold/italic/underline/strike segments, rows for border, fill, font colour, alignment. Sheet remains visible above the deck and the formula row. Use in Cellina deck brief, not as proof of Papirus implementation. |
| `19-48-30-83` (PowerPoint, viewer) | Multiple slides stacked vertically on a dark canvas, one with an orange selection outline. Bottom labelled bar offers present, notes, edit, search, share. Use in Slidia viewer brief. |
| `19-48-38-14` (PowerPoint, editor) | Single fitted slide, top title/action row, notes/comments above a bottom thumbnail filmstrip, then compact insertion tools. Use in Slidia editor brief: canvas, slide order, notes and quick tools have separate jobs. |
| `19-48-45-95` (PowerPoint, selected object) | One text box shows move/resize/rotation handles; bottom compact tools switch to text formatting. Use in Slidia contextual-selection brief: selection changes commands without covering the canvas. |
| `19-48-52-32` (PowerPoint, Home deck) | Selected text box remains visible above a command deck; the deck has Home switch, undo/redo, font/size row, bold/italic/underline/strike, font-colour and clear-format rows. Use in Slidia deck brief with explicit collapse/back behavior. |

### 9.3 Limits of the sources and new code evidence

- `material.io-design.md` is an alpha "Material Modern" *website style snapshot* with a purple `#6442D6` example palette and pixel-based marketing scale, not the official Material 3 Expressive specification. Official M3 guidance takes precedence; Papirus keeps its existing blue/module seeds. `google.com-design.md` describes Google Search, not the Google Workspace Android apps. `wps.com-design.md`, `office.com-design.md`, `libreoffice.org-design.md` are marketing-site summaries, not mobile app screenshots. Borrow only hierarchy, colour restraint, search patterns and spacing; use the supplied M365 screenshots for verified mobile interaction patterns. Neither Google Workspace mobile dialogs nor a FlexiPDF mobile deck screenshot was supplied, so those two mappings stay at task-pattern level.
- `docs/GS262-GettingStarted_compressed.pdf`: Chapter 1 (PDF pp. 23-37) covers Start Center module/template/open/remote paths, menus, toolbars, Sidebar, status, Save and AutoRecovery; Chapter 4 (pp. 153-169) distinguishes a real template from a sample file. `docs/WG262-WriterGuide_compressed.pdf` Chapter 1 starts PDF p. 17; Chapter 2 starts p. 41 (next feature phase). `docs/CG262-CalcGuide.pdf` Chapter 1 starts p. 17 (Formula Bar, sheets, status, selection). `docs/IG262-ImpressGuide_compressed.pdf` Chapter 1 starts p. 20 (Slides pane, workspace views, decks, notes and slide sorter). `docs/DG262-DrawGuide.pdf` Chapter 1 starts p. 21 (pages pane, layers, canvas, object properties). These PDFs are now in Git; guide citations in v3.0 name the local PDF and chapter/page.
- **Font asset health and licence relationship, unresolved code issue:** `README.txt` describes a Google Sans variable font with GRAD/opsz/wght axes, and `OFL.txt` is the generic SIL OFL 1.1 text, but neither identifies the checked-in `res/font/*.ttf` binaries or supplies a copyright-holder line for those binaries. A read-only fontTools `TTFont(path)["name"]` probe fails with `KeyError: 'name'` on **all 13** checked-in `.ttf` files; the four upright standard faces are byte-identical despite different filenames (`md5sum`). More than half the bytes in `google_sans_regular.ttf` are `EF BF BD` replacement-character sequences. The check confirms the repository resources cannot be counted as validated font files; actual device rendering, provenance and legally distributable replacement files need an owning code/asset plan. Do not silently substitute a claim of licensed or functioning Google Sans in any product docs. This does not alter code in Plan 5c.
- **Motion inspection:** read-only search of `app/src/main/java/**/*.kt` found no `MotionScheme`, `ANIMATOR_DURATION_SCALE` or `areAnimatorsEnabled` references. Existing editor sheets use `AnimatedVisibility`, expand/shrink and fade (`InkyModule.kt:2936-2939`); this does not establish runtime behavior under system reduced-motion settings.
- **Start Screen Drive placeholder:** `HomeDashboard.kt:1210-1275` shows a "Google Drive Sync" title and describes direct Google Workspace collaboration, then a "Connect Google Account" button displays only a placeholder toast. This is not an honest on-screen placeholder until after the press. The user-visible copy and action need an up-front unavailable state under the Plan 11 home-entry package / Plan 3 copy backlog.
- **Additional truth deltas for future code plans:** `CellinaModule.kt:96-136` seeds a sample grid/formula and `:809` hardcodes "100% • Sheet 1 of 3"; do not call that a file-derived status. `SlidiaModule.kt:135-168` seeds demo slides with unverified marketing claims when no real content has been loaded. `PagellaModule.kt:85-120,202-254` uses Android `PdfRenderer` to show a page or, when unavailable, displays text claiming a native renderer and annotation export; `:195-196` sends a callback naming an export, not evidence of a persisted PNG. No code is changed here: Plan 11 editor surfaces and the Plan 3 copy/backlog should own review and fixes. `Icons.Default.*`/Filled are used alongside Rounded (e.g. `CellinaModule.kt:569`, `PagellaModule.kt:147`), so "all icons are Rounded" was too strong.

## 10. Evidence index

- Create New: `app/src/main/java/com/example/ui/home/NewDocumentScreen.kt` (whole file), `core/util/TemplateManager.kt` (`curatedTemplates`, `get*TemplateFile`, `searchTemplates`), `ui/home/WelcomeScreen.kt`, `MainActivity.kt:60,165,269-270,304-306,365-368,395`, `HomeDashboard.kt:534-550`, `modules/inky/InkyModule.kt:346-349,409-418,771,1154-1219,1972`, `res/values/strings.xml:13-31,42-47,66-69,533-534`.
- Theme: `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`, `res/font/*`, `ui/options/PapirusOfficeOptionsScreen.kt:96,455-459,699-776`, `ui/home/AboutScreen.kt:46,81`.
- Build: `gradle/libs.versions.toml` (`composeBom = "2024.09.00"`), `app/build.gradle.kts:14-19,134,149-150,188-200`.
- Native: `app/src/main/libs/*/*.so` LFS pointers (`size 196227296` for arm64-v8a and `size 134699252` for armeabi-v7a, read from the pointer files), `core/jni/LokitEngine.kt:8-41`, `.github/workflows/build.yml:28,39-46,49-53,97`.
- Templates and fixtures: `app/src/main/assets/templates/` (115 files), `anti-slop/audit-008-2026-09-27-fixture-rebaseline.md §6`, `tests/`.
- Local design sources: `material.io-design.md`, `google.com-design.md`, `wps.com-design.md`, `office.com-design.md`, `libreoffice.org-design.md`, `README.txt`, `OFL.txt`; local PDFs in `docs/` listed in §9.3. Screenshots are inline chat observations in §9.1-9.2, not checked-in assets.
- Online guides and official M3: `https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html`, `https://books.libreoffice.org/en/index.html`, `https://m3.material.io/styles/typography/type-scale-tokens`, `https://m3.material.io/styles/motion/overview/how-it-works`, `https://m3.material.io/styles/shape/overview-principles`, `https://m3.material.io/components/toolbars/overview`.
