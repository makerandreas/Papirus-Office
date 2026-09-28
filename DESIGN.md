---
version: 3.0
name: Papirus Office design direction
status: "Target specification, not a claim that every feature or Material 3 Expressive API ships. Code baseline inspected 2026-09-27."
dials: "ENERGY 2 / RHYTHM 1 / MOTION 3"
reference_files:
  primary_site_notes:
    - material.io-design.md
    - google.com-design.md
  secondary_site_notes:
    - wps.com-design.md
    - office.com-design.md
    - libreoffice.org-design.md
  font_package_notes:
    - README.txt
    - OFL.txt
  libreoffice_26_2_guides:
    - docs/lo-guides/GS262-GettingStarted_compressed.pdf
    - docs/lo-guides/WG262-WriterGuide_compressed.pdf
    - docs/lo-guides/CG262-CalcGuide.pdf
    - docs/lo-guides/IG262-ImpressGuide_compressed.pdf
    - docs/lo-guides/DG262-DrawGuide.pdf
  mobile_screenshot_observations: anti-slop/audit-009-2026-09-27-docs-refresh-analysis.md (sections 9.1 and 9.2)
reference_urls:
  material_3: "https://m3.material.io/"
  material_3_type: "https://m3.material.io/styles/typography/type-scale-tokens"
  material_3_shape: "https://m3.material.io/styles/shape/overview-principles"
  material_3_motion: "https://m3.material.io/styles/motion/overview/how-it-works"
  material_3_toolbars: "https://m3.material.io/components/toolbars/overview"
  material_3_sheets: "https://m3.material.io/components/bottom-sheets/overview"
  writer_26_2_chapter_1: "https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html"
  libreoffice_26_2_shelf: "https://books.libreoffice.org/en/"
  odf_1_4: docs/odf
  ooxml: ECMA-376
---

# Papirus Office design direction

## 0. Reading rule

**Target** means a design decision, not available behavior. Every target below names a source pattern and an owning plan. **Shipped (source-inspected 2026-09-27)** means the cited `file:line` exists in the branch; it does not establish that the feature worked on a device. Paths beginning `ui/`, `modules/`, `core/` or `MainActivity.kt` are relative to `app/src/main/java/com/example/`; `res/` and `assets/` are relative to `app/src/main/`. See `PROJECT_CONTEXT.md` for architecture, `CONCEPT.md` for interaction terminology and `anti-slop/audit-009-2026-09-27-docs-refresh-analysis.md` for the evidence and unresolved deltas.

The five `*-design.md` files are owner-provided **alpha website style summaries**, not the official design specifications or screenshots of those companies' Android apps. In particular, `google.com-design.md` describes Google Search, not Google Workspace; `wps.com-design.md` and `office.com-design.md` describe landing pages, not their editors. The official Material 3 pages linked above govern M3 Expressive. The M365 mobile screenshots were supplied inline but are **not checked into Git**; observations and exact filenames are in audit-009 sections 9.1-9.2. Do not treat screenshot pixel sizes as Android dp measurements. Material and Google are primary; WPS, Microsoft and LibreOffice are secondary, bounded to tasks.

## 1. Identity and personality

Papirus Office is an Android-first office suite for opening and working with text, spreadsheets, slides and PDFs. ODF is a first-class *product goal*; practical OOXML compatibility is tested feature by feature, not promised for all files. LibreOffice APIs are used where available, but current rendering falls back to a Kotlin engine (`core/jni/LokitEngine.kt:8-41`, `PROJECT_CONTEXT.md:52`). The app's document canvas, not a mascot or hero graphic, is its focal point. Source: `CONCEPT.md` product/module map, `material.io-design.md` Overview/Layout, `google.com-design.md` Overview/Layout and the LibreOffice 26.2 guides in the front matter.

**Target, Plan 11 UI packages:** calm, legible application chrome with one decisive action per task, warm rounded geometry and a distinct but restrained module accent. The page, grid, slide or PDF is the main visual. Use generous space on entry screens; use purposeful density around editing. The existing dials (ENERGY 2 / RHYTHM 1 / MOTION 3) mean consistent controls with brief, responsive motion, not animated document content. Material's soft surfaces and Google Search's quiet search hierarchy support this; WPS's welcoming marketing rhythm informs entry screens without importing cloud promises (`material.io-design.md`, `google.com-design.md`, `wps.com-design.md`). No copied logo, corporate palette, stock hero, faux AI badge or proprietary Office UI asset.

## 2. Reference map and precedence

Material 3 Expressive is the system for every row: semantic colour, type, shape, motion, updated components, accessibility and adaptive layout. Where a secondary example conflicts, retain Material and the Papirus module identity. A reference supplies a **task pattern**, not permission to recreate a competitor's screen.

| Surface | Bounded source | Borrow for the target | Do not borrow |
|---|---|---|---|
| Start Screen: Recents, Files, Drive tab | Google Workspace pattern assigned in `CONCEPT.md:27-31`; `google.com-design.md` Search/Components informs only search hierarchy | Search-first scan path, quiet results, visible selected tab | The Google Search page as a file manager, or a fake Drive connection. No Workspace mobile screenshot was supplied. |
| Editor dialogs across modules | Google Workspace task pattern in `CONCEPT.md:27-31`; M3 dialogs/text fields | One task per dialog, clear confirmation/cancel, context preserved | Unverified Google Docs/Sheets/Slides Android-specific anatomy. |
| Welcome and Create New | WPS pattern in `CONCEPT.md:27-31`; `wps.com-design.md` Layout/Components | Friendly entry, spacious choice of real document types | WPS marketing blue, sales copy, large desktop hero padding, or a claimed cloud account. |
| Options, Crash Logs, About | Android Settings task pattern in `CONCEPT.md:27-31`; M3 lists/switches | Grouped rows and understandable system state | A settings skin applied to the editor canvas. |
| Editor viewing and editing | Supplied M365 Word, Excel and PowerPoint screenshots (audit-009 sections 9.1-9.2) | Canvas-first viewing, context tools, visible saved/unsaved state | Copilot, cloud sync, voice features or Microsoft visuals without Papirus implementations. |
| Inky, Cellina, Slidia command decks | M365 screenshots (audit-009 sections 9.1-9.2); Office 365 mapping in `CONCEPT.md:27-31` | Compact tools lead to a deeper command deck with selection-aware rows | A fixed desktop ribbon or controls that imply unavailable commands work. |
| Pagella command deck | FlexiPDF mapping in `CONCEPT.md:27-31`; M3 sheet guidance | PDF-specific page, ink and export commands after they work | A detailed FlexiPDF mobile layout: none was supplied. |
| Document concepts | Local 26.2 LibreOffice guides; `libreoffice.org-design.md` only for restrained site hierarchy | Templates, styles, sheets, slides, pages, selection and file lifecycle | Desktop menus crammed into a phone, LibreOffice website green as a Papirus accent. |

`office.com-design.md` describes a light Microsoft landing page (calm spacing and one dominant action). Use the **actual M365 mobile screenshots** for editor/deck structure instead. `libreoffice.org-design.md` describes a website, while the local 26.2 PDF guides define the office concepts. No Google Workspace, WPS Android or FlexiPDF screenshot is present; those mappings remain provisional at interaction-pattern level until supplied (audit-009 §9.3).

## 3. Material 3 Expressive adoption contract

**Target, Plan 11 design-system prerequisite and UI packages:** follow the applicable official M3 Expressive guidance on every surface, not just its visual motifs. The uploaded `material.io-design.md` offers a useful airy tonal example, but its `#6442D6` purple and `80px` button are website examples, not Papirus tokens or Android dp. Keep the existing Papirus accents and responsive touch targets. The following is a review checklist for each implementation package, not a claim that all Expressive APIs already ship:

| Foundation or component | Papirus application | Current boundary |
|---|---|---|
| Colour and personalization | Light/dark semantic pairs, tonal surfaces and dynamic colour where supported; static, user-seed and wallpaper modes must say which source was used (§4). | System dynamic/static only (`ui/theme/Theme.kt:266-296`). |
| Baseline plus emphasized typography | Use baseline for reading, emphasized weight for selected/actions/headlines; do not turn every label into display text (§5). | Only baseline roles in `ui/theme/Type.kt:55-160`. |
| Shapes and shape morphing | Semantic corner scale; use a morph to clarify a selected/expanded *control* when it helps, never to deform document contents (§6). | Literal radii and default `MaterialTheme.shapes` (`ui/theme/Theme.kt:294-298`). |
| Physics-based motion | Spatial springs for movement/size, effect springs for opacity/colour, consistent speeds, retargeting and reduced-motion alternatives (§7). | Local `AnimatedVisibility`/fade; no theme motion scheme (`modules/inky/InkyModule.kt:2936-2949`). |
| Navigation and actions | Navigation bar on narrow entry screens; rail/supporting pane on wider screens; one FAB for the primary task. Use the newer docked or floating toolbar where a real command strip needs it; M3 no longer recommends a bottom app bar ([M3 toolbars](https://m3.material.io/components/toolbars/overview)). | `ui/home/HomeDashboard.kt:497-555`; custom editor strips. |
| Inputs and content | Task-appropriate search/text fields, chips, segmented buttons, cards, lists and feedback. Use the Expressive loading indicator only for a **real** loading state; use button groups, split buttons or FAB menus only where multiple actions actually exist. | BOM `2024.09.00` uses Material 3 1.3.0 (`gradle/libs.versions.toml:1-30`); later Expressive APIs unavailable. |
| Sheets and dialogs | A standard/dismissible sheet can expose commands while the canvas remains usable; modal for blocking choices; dialog for a concise decision, full page for lengthy work (§9.8-9.10). | Custom 40% deck surfaces in Inky/Cellina/Slidia, not M3 Expressive sheets (`modules/inky/InkyModule.kt:2931-2949`). |

For each future UI component, use the current [M3 component page](https://m3.material.io/components) for its anatomy, states, accessibility, responsive behavior and motion. This checklist is a routing aid, not a substitute for the component guidance or a reason to add irrelevant widgets.

The actual Compose BOM and API upgrade, `MaterialExpressiveTheme` or equivalent theme motion/type/shape tokens, and component migration require a code PR under Plan 11. Review experimental API status and device support when doing that work; v3.0 adds **no** dependency or UI. M3 sources: front matter URLs for [type](https://m3.material.io/styles/typography/type-scale-tokens), [shape](https://m3.material.io/styles/shape/overview-principles), [motion](https://m3.material.io/styles/motion/overview/how-it-works), [toolbars](https://m3.material.io/components/toolbars/overview) and [sheets](https://m3.material.io/components/bottom-sheets/overview).

## 4. Colour and personalization

**Shipped (source-inspected):** `ThemeSettings` defaults dynamic colour on at API 31+ (`ui/theme/Theme.kt:16-25`); `PapirusTheme` uses the Android `dynamicLightColorScheme` or `dynamicDarkColorScheme` then (`ui/theme/Theme.kt:266-273`). Otherwise it selects static light/dark palettes by workspace (`ui/theme/Theme.kt:274-298`). Options exposes only a dynamic switch and System / Light / Dark (`ui/options/PapirusOfficeOptionsScreen.kt:699-776`). This consumes Android's system colour scheme; it is not a separate Papirus wallpaper-colour generator.

**Target, Plan 11:** foreground/background pairs use semantic `primary`/`onPrimary`, containers, `surface`/`onSurface`, `outline`, `error` and their dark equivalents. Validate contrast for each pair and disabled/selected state; the raw website palettes are **not** colour specifications for the app (`material.io-design.md` Colours, `google.com-design.md` Colours, [M3 colour roles](https://m3.material.io/styles/color/roles)). Keep the editor's paper/canvas colours driven by document content, separate from theme chrome (`modules/slidia/SlidiaModule.kt:57-58`, `modules/pagella/PagellaModule.kt:210-227`). Source panels are quiet so selection and content take priority.

| Stable Papirus area identifier | Seed in `ui/theme/Color.kt:19-23` | Purpose |
|---|---|---|
| Suite | `#2563EB` | Start/create/Options identity |
| Inky | `#0F9D58` | Text documents |
| Cellina | `#16A3B7` | Sheets and formulas |
| Slidia | `#F59E0B` | Slides |
| Pagella | `#D93025` | PDFs |

Seeds identify modules; a dynamic/custom scheme can render different tones. **Known shipped delta:** some module chrome bypasses those theme roles, e.g. Slidia's local `0xFFD97706` (`modules/slidia/SlidiaModule.kt:182`) and Pagella's local `0xFFDC2626` (`modules/pagella/PagellaModule.kt:124`). Plan 11 owns the token review. Do not recolour user-authored cells, selected text or page pixels to make a theme look consistent.

| Mode | Availability and behaviour |
|---|---|
| System dynamic | **Shipped** on API 31+ by default, with a user switch (`ui/theme/Theme.kt:16-25,266-273`; `ui/options/PapirusOfficeOptionsScreen.kt:770-776`). Respect system light/dark unless the user selects Light or Dark. |
| Papirus static | **Shipped** below API 31 and when dynamic is off: per-workspace static light/dark (`ui/theme/Theme.kt:274-298`). |
| Custom seed | **Target, Plan 11**: user-chosen seed generates contrast-checked semantic tones; the current Options screen has no such choice (`ui/options/PapirusOfficeOptionsScreen.kt:731-776`). |
| Papirus wallpaper palette | **Target, Plan 11**: independent of Android dynamic colour. Read `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)` where supported (API 27+); derive Papirus semantic tones rather than reusing `dynamicLightColorScheme`. Handle unavailable/null/live wallpapers with a SAF-selected image or clearly named static fallback; no broad storage permission. See `anti-slop/plan-11-hybrid-experience-design.md` §1. |

## 5. Typography and font provenance

**Shipped code declaration:** `ui/theme/Type.kt:15-50,55-160` requests `google_sans_{light,regular,medium,bold}` for display/headline/title and body/label roles (with the italic file on the body family); the code family is separate. `GoogleSansFlexFontFamily` is named for Flex but points at the same four `google_sans_*` resources, not the bundled `google_sans_flex_*`. Fonts use `FontLoadingStrategy.OptionalLocal`. The `Type.kt` scale defines display 57/45/36 sp, headline 32/28/24 sp, title 22/16/14 sp, body 16/14/12 sp and label 14/12/11 sp (`ui/theme/Type.kt:55-160`). These are code declarations, **not** proof of a successfully rendered UI font.

**Unresolved asset blocker (read-only inspection, audit-009 §9.3):** `README.txt` describes a Google Sans variable package with GRAD/opsz/wght axes and static files; `OFL.txt` contains the general SIL OFL 1.1 text. Neither establishes the provenance or rights holder for the checked-in resources. `fontTools.ttLib.TTFont(path)["name"]` fails with `KeyError: 'name'` for all 13 checked-in `res/font/*.ttf` files; several differently named weights have identical bytes. Do not call this a functioning or licensed Google Sans integration merely because `Type.kt` references it. A separate asset/code plan must establish provenance, distribute valid fonts and verify them on a device. A system sans-serif fallback is the intended reliability path, **not yet a verified device result**. No font binaries or resources change in Plan 5c.

**Target, Plan 11 after the asset and BOM prerequisites:** choose a legally distributable Google Sans face for brand-sized UI roles and a readable plain face for body/labels; default to Roboto/system sans-serif if validation fails. The uploaded `material.io-design.md` names Google Sans and Google Sans Text for different roles, but the latter is not evidenced among this repo's `res/font` files. Use M3 baseline **and selective emphasized** styles for selected rows, important actions and headlines, not for entire lists. The official [M3 type scale](https://m3.material.io/styles/typography/type-scale-tokens) defines 15 baseline and 15 emphasized roles; retain user font scaling, legible line height and Android `sp`. Do not transplant the website's 96 px hero heading into a 320 dp phone. Screen headings, field labels and status numbers should have distinct but related roles (`google.com-design.md` Typography, `material.io-design.md` Typography).

UI fonts never rewrite document font identity: an ODT/DOCX may name Times New Roman or another document face; layout substitutions must not change the saved family. Writer and Calc font/metric work is owned by Plans 5e and later fidelity plans (`anti-slop/plan-2026-09-24-remaining-pr-roadmap-v2.md`, Writer Guide 26.2 Chapter 1). Keep code/diagnostic text distinct from document content.

## 6. Shape, spacing and depth

**Target, Plan 11:** use M3's semantic corner progression, not a single radius everywhere. The scale below is from the official [M3 shape guidance](https://m3.material.io/styles/shape/overview-principles); the website snapshot supplies a softer visual direction but its px values are not Android dp tokens (`material.io-design.md` Shapes/rounded, `google.com-design.md` Shapes).

| Role | Target radius | Where it belongs |
|---|---:|---|
| None / extra small / small | 0 / 4 / 8 dp | Document edges, small grouped controls |
| Medium / large / large increased | 12 / 16 / 20 dp | Inputs, cards, primary controls by hierarchy |
| Extra large / extra large increased / extra extra large | 28 / 32 / 48 dp | Sheets, prominent entry containers and occasional focal elements |
| Full | Component-dependent | FAB, filter chip or selection indicator where the shape communicates an action |

The 35-shape Material library and shape morphing are available as **target tools** for deliberate state changes, such as selection or expansion. They are not a requirement to use 35 shapes or morph the document page; no new illustration or shape asset is requested. **Shipped:** `ui/theme/Theme.kt:294-298` does not pass a Papirus `Shapes` object; `ui/home/NewDocumentScreen.kt:169` sets a literal 28 dp search field and `modules/inky/InkyModule.kt:2944-2947` sets a 28 dp sheet top. Reconcile existing radii and component tokens in Plan 11 before changing any visual rhythm.

**Target, Plan 11:** 4 dp micro-spacing and an 8 dp working rhythm, with 12/16/24/32/48 dp gaps chosen for content grouping. Website sources range from compact Google search spacing (`google.com-design.md` spacing) to oversized marketing sections (`material.io-design.md`, `wps.com-design.md`); use the *relative hierarchy*, not page-level px measurements. On phones, one clear column and scrollable commands beat a marketing-grid layout. Tonal surface contrast and thin borders do most of the separating; reserve elevation for a real layer such as a sheet or floating action (`material.io-design.md` Elevation, `office.com-design.md` Elevation). The document page remains a neutral reading surface.

## 7. Motion and adaptive layout

**Shipped:** local visibility/slide/fade transitions exist (`ui/home/HomeDashboard.kt:589-593`, `modules/inky/InkyModule.kt:2936-2949`), but `PapirusTheme` passes no motion scheme (`ui/theme/Theme.kt:294-298`), and the BOM cannot supply the later Expressive API (`gradle/libs.versions.toml:1-30`). A targeted source scan found no `MotionScheme` or explicit animator-duration-scale check (audit-009 §9.3); that is not a measured device claim.

**Target, Plan 11:** [M3 motion physics](https://m3.material.io/styles/motion/overview/how-it-works) provides expressive and standard schemes, with fast/default/slow *spatial* springs for position, size and shape, and *effect* springs for colour/opacity. Use expressive motion on meaningful entry, sheet and selection transitions (MOTION 3); choose restrained standard motion where precision matters, such as a cursor, cell grid or slide object drag. Let gestures interrupt and retarget motion; avoid a slow bounce over text. With system animation reduction or disabled animators, present the state immediately or with minimal non-spatial feedback; provide a non-gesture route for every transition. This is a design test, not evidence that current code honors reduced motion.

**Target, Plan 11:** choose navigation by task and width: compact phone below 600 dp, medium 600-839 dp, expanded 840 dp and above (window size classes in `DESIGN.md` v2.1 / [Android adaptive layouts](https://developer.android.com/develop/ui/compose/layouts/adaptive)). At 320 dp and large font scaling, lists reflow, the keyboard does not cover an active control, and the deck scrolls or expands instead of clipping. On medium/expanded width, put document and supporting pane side by side where helpful; do not simply stretch a phone-width sheet. The supplied M365 screenshots show phone patterns, not proof of Papirus tablet behavior (audit-009 §9).

## 8. Icons and imagery

**Shipped:** the dependency is `androidx.compose.material:material-icons-extended` (`AGENTS.md` Binding Experience Direction); many icons use `Icons.Rounded.*`, but other screens also use `Icons.Default.*`/Filled (`modules/cellina/CellinaModule.kt:569`, `modules/pagella/PagellaModule.kt:147`). This is a mixed Material Icons baseline, **not** an installed Material Symbols family. The `app/src/main/share/config/images_colibre.zip` exists as an optional candidate; its licence/content have not been reviewed (`CONCEPT.md:40`).

**Target, Plan 11:** use Material Symbols Rounded consistently in application chrome once the dependency/assets and weight/optical alignment are confirmed; prefer the existing module logo for identity, and retain recognizable document thumbnails instead of stock illustration. Colibre only after an explicit provenance and asset review. Convey saved/failed/selected states with a readable label or accessible description, not hue or a magic/sparkle icon. `material.io-design.md` Components and the official [M3 icons](https://m3.material.io/styles/icons) are the icon-system references; the M365 screenshots inform *placement* of commands, not a glyph/brand copy.

## 9. Surface briefs

Every brief uses the sources in §2 and applies the M3 adoption contract in §3. **Shipped** describes code, **target** names the plan. See audit-009 §9 for screenshot evidence and source limitations.

### 9.1 Welcome

- **Sources:** `wps.com-design.md` Layout/Components gives clear entry and one prominent action; `material.io-design.md` Components keeps it tonal and touch-friendly. This is not a WPS Android screen spec.
- **Shipped:** first-run gate `MainActivity.kt:304-306` shows a centered column with a 112 dp logo tile (28 dp radius, 80 dp icon), title/body, and one 56 dp button labelled "Get Started" (`ui/home/WelcomeScreen.kt:28-79`, `res/values/strings.xml:67-69`). The callback sets `is_first_run=false` and opens Home; it does **not** ask for permission (`MainActivity.kt:365-371`, `ui/home/WelcomeScreen.kt:18-21`).
- **Target, Plan 11 home-entry package:** keep a single grounded promise about on-device documents and the system picker, with a specific action such as "Go to documents" that leads to the Start Screen. Let the content column reflow at large text, preserve brand colour in one action and avoid fake cloud onboarding or a permission explanation. "Get Started" is a shipped generic CTA, not recommended copy (R-15).

### 9.2 Start Screen tabs

- **Sources:** Google Workspace task map (`CONCEPT.md:27-31`), calm search/navigation hierarchy in `google.com-design.md` Layout/Search, and LibreOffice Start Center module, recent, template and open-file paths (`docs/lo-guides/GS262-GettingStarted_compressed.pdf`, Chapter 1, PDF pp. 24-35). No Workspace mobile screen was supplied.
- **Shipped:** Recents / Files / Google Drive bottom tabs, `HorizontalPager`, a top search field and a 56 dp create FAB (`ui/home/HomeDashboard.kt:475-580`). Recents filters only **file names** plus selected module, not document body or author (`ui/home/HomeDashboard.kt:655-690`). The Google Drive tab is a placeholder, not OAuth/file sync; its promotional copy and "Connect Google Account" button do **not** disclose this until the placeholder toast after a press (`ui/home/HomeDashboard.kt:1210-1275`).
- **Target, Plan 11 home-entry package:** make the actual recent files and storage paths easy to scan; keep the create action away from the last item and system bar. Use module badges as labels plus colour, visible empty/loading/error states and Drive copy that discloses the unavailable state before a tap (Plan 3 copy backlog). Only claim metadata/full-text search after it works. A rail or list-detail pane on wider widths may supplement the tabs; do not force a Google Search landing page onto a file browser.

### 9.3 Create New

- **Sources:** WPS entry hierarchy (`wps.com-design.md` Components/Layout); LibreOffice Start Center (`docs/lo-guides/GS262-GettingStarted_compressed.pdf`, Chapter 1, PDF pp. 24-35) and real template definition (Chapter 4, PDF pp. 166-169). Blank, template, open and remote are different tasks.
- **Shipped:** Start Screen FAB opens `NewDocumentScreen` (`ui/home/HomeDashboard.kt:534-550`, `MainActivity.kt:395`). It starts on tab 1 of a two-page pager: three module cards open blank `assets/templates/Untitled.odt|ods|odp` via `TemplateManager` with `pendingNewDocument`, followed by Image / Camera / Convert entries for Pagella. Tab 2 has All / ODT / ODS / ODP filters, search and `TemplateManager.searchTemplates` fallback sources (`ui/home/NewDocumentScreen.kt:71-200`, `core/util/TemplateManager.kt:24-41,90-160`, audit-009 §3.1). It is **module-first**, not template-first. No template preview is implemented (audit-009 §3.1).
- **Known delta:** the 12 hard-coded gallery entries are three blank assets and nine `filesamples.com` samples presented under invented template names; another 112 bundled files are not surfaced (`core/util/TemplateManager.kt:90-171`, audit-009 §3.1). The `untitled.*` packages do not declare a conformant page layout (`anti-slop/audit-008-2026-09-27-fixture-rebaseline.md` §6); the separate Inky template dialog inserts fake resume copy (`modules/inky/InkyModule.kt:1154-1219`). Those are **not** a usable official template catalogue.
- **Target, Plan 11 home-entry package / Plan 3 copy backlog / 5d template-default work:** preserve the fast blank route and the distinct Pagella creation tasks. List only installed, attributable templates with a real preview or honest "Preview unavailable" state; otherwise show fewer results. Loading, offline, zero results and download errors need clear language and retry. Opening a template creates a **new** unsaved document without overwriting its source; do not use a sample-file URL as a template. Fix blank defaults in the owning code plan, not in this doc.

### 9.4 Editor shell and Inky

- **Sources:** M365 Word viewer/editor screenshots (audit-009 §9.1); Writer Guide 26.2 Chapter 1 (`docs/lo-guides/WG262-WriterGuide_compressed.pdf`, PDF pp. 17-40) for page/status, Navigator, save/undo and Sidebar concepts.
- **Shipped:** Inky has viewer/edit modes, `TopAppBar` title and a single 48 dp status area with page/zoom access (`modules/inky/InkyModule.kt:2047-2062,2455-2480`). The keyboard-mode Toolbar Hub and 40%-height command surface are implemented as custom Compose rows/surface (`modules/inky/InkyModule.kt:2582-2608,2931-2949`). Only File and Home have Writer ribbon decks; Insert / Layout / Review / View are declared but unavailable (`modules/inky/WriterRibbonModel.kt:7-44`). These references establish screen structure, not device rendering fidelity.
- **Target, Plan 11 editor package and later Writer fidelity plans:** preserve the document when chrome changes mode. In viewer mode, put read/navigation/edit actions in one reachable bar. In editor mode, show the active tool state and an explicit expand path to the command deck while leaving text visible. Save/unsaved/error status must reflect the session, not a toast. Re-map Writer Guide 26.2's nine Sidebar decks and accessibility-check field in Plan 1 rather than claiming they already exist in Inky (`docs/lo-guides/WG262-WriterGuide_compressed.pdf`, Chapter 1; audit-009 §2).

### 9.5 Cellina

- **Sources:** four M365 Excel screenshots (audit-009 §9.2), Calc Guide 26.2 Chapter 1 (`docs/lo-guides/CG262-CalcGuide.pdf`, PDF pp. 17-50): Formula Bar/Name Box, sheet tabs, cell selection and status aggregation.
- **Shipped:** a grid, coordinates, `formulaText`, sheet list and status controls exist (`modules/cellina/CellinaModule.kt:95-136,538-589,790-834`), with a custom 40%-height command surface (`:837-852`). The initial grid and `=SUM(B2:C3)` are seeded sample values; the text "100% • Sheet 1 of 3" is hard-coded (`:99-136,809`). Do **not** describe these as calculations or status read from the opened file.
- **Target, Plan 11 editor package (UI); Calc data work remains unscheduled:** prioritize selected cell, address, formula entry and visible grid. With the keyboard open, keep commit/cancel and formula context reachable; when selection changes, a compact quick-tool row may replace sheet tabs, but the current sheet stays identifiable. If a range has data, a summary row may expose *computed* sum/average/count, not the M365 screenshot's sample numbers. Sheet switching/addition and freeze/scroll behavior must reflect the workbook. No toolbar may cover the cell being edited.

### 9.6 Slidia

- **Sources:** four M365 PowerPoint screenshots (audit-009 §9.2); Impress Guide 26.2 Chapter 1 (`docs/lo-guides/IG262-ImpressGuide_compressed.pdf`, PDF pp. 20-43): Slides pane, Normal/Outline/Notes/Slide Sorter, selected-object decks, slide show.
- **Shipped:** Slidia has a slide state list, selected index, top chrome, a custom 40% deck and a viewer-mode Edit FAB (`modules/slidia/SlidiaModule.kt:134-190,982-1007,1367-1381`). The default slide list contains demo marketing text, **not** verified user content (`:135-168`); do not make claims of full format support from those slides.
- **Target, Plan 11 editor package (UI); Impress data work remains unscheduled:** viewer may stack readable slides with a clear Present/Notes/Edit route; editor fits the active slide above a navigable thumbnail filmstrip. Selection handles indicate the real selected object and switch the quick tools to its context. Expanded commands must leave a route back to slide order, notes and canvas; no fake "insert" action and no FAB covering a slide or status control. On wider screens, use a persistent thumbnail pane, not a 40% sheet stretched across the display.

### 9.7 Pagella

- **Sources:** FlexiPDF is the bounded PDF-command reference in `CONCEPT.md:27-31`, but no FlexiPDF screen was supplied; M3 sheets and Draw Guide 26.2 Chapter 1 (`docs/lo-guides/DG262-DrawGuide.pdf`, PDF pp. 21-33) provide page/object selection concepts.
- **Shipped:** Android `PdfRenderer` renders one current page from an existing local PDF, with previous/next and zoom buttons; an ink Canvas can hold gesture paths (`modules/pagella/PagellaModule.kt:85-120,132-198,202-299`). A failed/missing render falls back to static text that falsely claims native PDF rendering and export (`:229-253`); the "Export" button at `:195-196` calls a message callback and is not proof a PNG was saved. Continuous multi-page scroll and a FlexiPDF-like bottom deck are **targets**, not shipped behavior.
- **Target, Plan 11 Pagella package / Plan 3 copy backlog:** page/zoom/ink tools grouped by actual PDF task, with real progress/error/empty states. Only offer export when there is persisted output; distinguish PDF page, ink overlay and file actions. Preserve document contrast in light/dark chrome. Validate continuous paging before promising it. Do not infer a particular FlexiPDF deck layout without a source image.

### 9.8 Toolbar Hub and command decks

- **Sources:** M365 Word compact strip and Home deck (audit-009 §9.1); Excel selection/formula/keyboard/deck and PowerPoint contextual text/slide tools (audit-009 §9.2); [M3 toolbars](https://m3.material.io/components/toolbars/overview) and [bottom sheets](https://m3.material.io/components/bottom-sheets/overview).
- **Shipped:** Inky quick actions scroll horizontally and show selected toggle containers (`modules/inky/InkyModule.kt:2582-2608,2672-2695`); a fixed `fillMaxHeight(0.40f)` controls its sheet (`:2931-2949`). Cellina and Slidia use the same fixed fraction in their own surfaces (`modules/cellina/CellinaModule.kt:837-852`, `modules/slidia/SlidiaModule.kt:982-996`). The physical row heights and keyboard/window-inset behaviour still need on-device checks.
- **Target, Plan 11 editor packages:** the compact row is the near-canvas path to frequent commands; its final labelled control expands the same selection context into a scrollable command deck. Keep state/icon order consistent between strip and deck. Header: named active tab/deck, undo/redo when real, collapse/back; body: font/size fields, segmented state toggles, rows with labels/chevrons into subpages where justified. Inky fields concern text and styles; Cellina fields concern cell/number/format/selection; Slidia fields concern slide/object/text; Pagella fields concern page/annotation/export and **must not** reuse Office ribbon tabs. Do not put a pretend assistant action in the deck just because the Microsoft screenshot has one.
- **Sizing rule:** the screenshots show an initial phone deck around the lower two-fifths of the visible screen, **not** an invariant height or a measured dp spec. Grow or scroll for large text, keyboard, landscape and foldables; keep a visible document anchor and always give an explicit collapse/back route. M3 standard sheets are generally supplementary; using one as Papirus's office command surface is an intentional adaptation, not a claim of default M3 behavior.

### 9.9 Options, Crash Logs and About

- **Sources:** Android Settings task structure in `CONCEPT.md:27-31`, M3 list/switch and segmented controls; `google.com-design.md` for restrained search states. `office.com-design.md` offers only spacing/action hierarchy, not a settings implementation.
- **Shipped:** Options Appearance has a card with System/Light/Dark segmented choice and API-gated dynamic-colour switch (`ui/options/PapirusOfficeOptionsScreen.kt:699-779`). Crash Logs reads a bounded local log tail, filters by tag/query and has copy/share/export actions (`ui/home/CrashLogsScreen.kt:60-95,182-198,310-350`). About is a `Scaffold` with `LazyColumn` sections, not a pager (`ui/home/AboutScreen.kt:42-90`). Some Appearance copy is hard-coded (`ui/options/PapirusOfficeOptionsScreen.kt:716-765`).
- **Target, Plan 11 system surfaces / Plan 3 copy backlog:** group preferences by consequence, show current value and unsupported modes plainly, and allow recovery when diagnostics are empty. About may gain vertically paged **sections** with visible page indicators/buttons and a non-gesture path; it must never feel like an endless social feed. The vertical transition is an optional interaction only. Native/SIMULATED engine status and supported export types must match runtime evidence (`core/jni/LokitEngine.kt:8-41`).

### 9.10 Editor dialogs

- **Sources:** bounded Google Workspace editor-dialog map (`CONCEPT.md:27-31`), M3 [dialogs](https://m3.material.io/components/dialogs) and LibreOffice 26.2 task concepts (local guides in the front matter). No actual Workspace dialog screenshot was supplied; avoid invented Google-specific layout details.
- **Shipped:** `ui/components/SaveAsDialog.kt:19-49,51-100` shows format choice and a separate non-ODF warning; other editor sheets/dialogs remain module-specific (`ui/components/` directory). Code presence alone does not prove every format can round-trip.
- **Target, Plan 11 editor-dialog package / format plans:** title the operation, keep one primary choice and a distinct Cancel/Back, retain the filename/format context when a warning appears, and disclose compatibility limits before the user confirms. Short decisions can use a dialog; longer settings/preview work belongs in a scrolling sheet or full page with focus and keyboard handling. Never substitute a fabricated successful export confirmation for a saved file.

## 10. Content and copy

**Target, Plan 3 copy backlog and Plan 11 UI packages:** UI strings are en_US and belong in `strings.xml`; no em dash in user-visible text. Prefer a verb tied to the actual operation ("Open file", "Create spreadsheet", "Save as ODS", "Go to documents") over generic "Get Started". Name what a file search examines, whether a template is installed or downloaded, and whether a command is unavailable. Loading and empty states are descriptive, not a glowing illustration of an unimplemented operation. Source: `AGENTS.md` antislop addendum, `google.com-design.md` Search/Components and `wps.com-design.md` Components. **Shipped deltas:** Welcome "Get Started" (`ui/home/WelcomeScreen.kt:73-78`), template tab sample labels (`core/util/TemplateManager.kt:90-171`), Pagella fallback copy (`modules/pagella/PagellaModule.kt:229-253`). No copy or app resources are changed by this document.

## 11. Accessibility floor and verification

**Target, owning UI package per surface:** at least 48 dp for interactive areas, even when a glyph is smaller; semantic label and state for TalkBack; focus visibility and keyboard/Enter/Escape access; non-gesture navigation for sheet/section/view changes. Check 4.5:1 for normal text, 3:1 for large text and meaningful non-text UI, in light/dark and every available palette; compute contrast, do not assert from a screenshot. Reflow at 320 dp with large text and IME; avoid clipped fields and hidden last rows. Reduce or remove animation when system settings request it. Distinguish loading, empty and failure with text, not just a hue or spinner. These are acceptance requirements, **not claims that today's UI passes** (`AGENTS.md` conventions; `skills/antislop-human/SKILL.md` contrast and focus checks; `skills/antislop-layoutmobile/SKILL.md` mobile/keyboard checks).

**Target verification, Plan 11 and the owning format plan:** record task, screen size, type scale, system theme, palette, action/disabled state, TalkBack/focus order, keyboard visible/hidden, and how save/error was confirmed. A code citation verifies declared behavior; a device screenshot or test verifies what the user actually saw. `docs/InkyC1Checklist.md` is the Writer Chapter 1 device pass; future work proceeds from Writer Guide 26.2 Chapter 2 after that pass.

## 12. Standards and evidence ledger

- **Normative formats:** ODF 1.4 Parts 1-4 in `docs/odf`; ECMA-376 for OOXML. Open XML SDK docs are useful for package/part APIs but not a replacement for the standard (`AGENTS.md` Reference Material). Format compatibility is earned by parse/render/save/reopen tests, not by matching a screenshot.
- **LibreOffice 26.2 scope:** Getting Started Ch. 1 (Start Center and document lifecycle) and Ch. 4 (styles/templates), `docs/lo-guides/GS262-GettingStarted_compressed.pdf`; Writer Ch. 1 (window, Sidebar, Navigator, views, save), `docs/lo-guides/WG262-WriterGuide_compressed.pdf`, with [online Chapter 1](https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html); Calc Ch. 1 (formula, grid, sheets/status), `docs/lo-guides/CG262-CalcGuide.pdf`; Impress Ch. 1 (slide pane, views, notes), `docs/lo-guides/IG262-ImpressGuide_compressed.pdf`; Draw Ch. 1 (pages, layers, drawing tools), `docs/lo-guides/DG262-DrawGuide.pdf`. Next Writer feature work starts from **Ch. 2, Working with Text: Basics** (Writer PDF p. 41), not from an older edition. Plan 1 owns the historical 24.8 chapter-map update (`anti-slop/plan-11-hybrid-experience-design.md:74`).
- **Plan 5c evidence:** `anti-slop/audit-009-2026-09-27-docs-refresh-analysis.md` §3.1 has the Create New code walk; §9.1-9.2 records the 11 inline M365 screenshots; §9.3 records source limits, local guide pages, font inspection and extra module deltas; §10 indexes files. Website source notes and all five local PDFs are listed in this file's front matter. No screenshot JPEG was added to Git.
- **Documentation-only Delivery Gate:** the final reduced report is in PR #17. R-02, R-15, R-16, R-17, R-36, R-38 and C-5 require evidence; R-26/27/32/34/35 are N/A **for this doc edit**, not waived for later UI work (`AGENTS.md` antislop project addendum). Code, resources, tests and workflows are unchanged by Plan 5c.
