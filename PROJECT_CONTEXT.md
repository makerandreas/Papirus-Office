# Papirus Office: Project Description Context & Master Architecture Reference

---

## 📘 1. Product Identity & Overview

**Papirus Office** (initially codenamed and developed as **LibreDroid Office**) is a modern, modular, open-source office suite engineered for Android smartphones, foldables, and tablets. It delivers a PC-class productivity and document editing experience on mobile devices while remaining strictly open-source, compliant with international document standards, legally compliant (adopting the "Papirus Office" brand to respect trademark separation from LibreOffice and The Document Foundation), and ergonomically optimized for touch interfaces.

- **Codename / Concept Heritage**: Originally conceptualized as **LibreDroid Office**, with the goal of adapting LibreOffice-based office workflows to Android devices.
- **Format direction**: ODF 1.4 (`.odt`, `.ods`, `.odp`, `.ott`, `.ots`, `.otp`) is a first-class target, with OOXML (`.docx`, `.xlsx`, `.pptx`) compatibility and PDF support. Compatibility is feature- and test-dependent; do not claim complete import/export coverage without format-specific evidence.
- **Design direction**: Android-first Material 3 Expressive with a deliberately bounded hybrid of office-app interaction patterns, Papirus module identity, adaptive layouts and accessible touch targets. Google Sans is the intended UI family with a Roboto/system fallback pending on-device validation. `DESIGN.md` is authoritative for design decisions; this context records architecture and implementation status.
- **License**: Mozilla Public License 2.0, see [LICENSE](LICENSE) (file-level copyleft; same steward-license family as LibreOffice's MPL-2.0).

### The Suite Modules & Color Conventions
Default static accent colors (for Android 11 and below, or when Dynamic Color is disabled):
- 🟦 **Papirus (Base Suite)**: `#2563EB`: Suite Home, Start Center, File Manager, Universal Options.
- 🟩 **Inky (Word Processing)**: `#0F9D58`: documents, text editing, typography, layouts (`.odt`, `.docx`, `.txt`, `.rtf`). *(Equivalent to LibreOffice Writer / MS Word)*
- 🟦 **Cellina (Spreadsheets)**: `#16A3B7`: spreadsheets, grid calculations, formulas, charts (`.ods`, `.xlsx`, `.csv`). *(Equivalent to LibreOffice Calc / MS Excel)*
- 🟧 **Slidia (Presentations)**: `#F59E0B`: slide decks, presentations, animations, speaker notes (`.odp`, `.pptx`). *(Equivalent to LibreOffice Impress / MS PowerPoint)*
- 🟥 **Pagella (PDF & Document Manager)**: `#D93025`: PDF viewing, document annotation, page extraction, format conversion.

---

### Experience reference map (design target, not implementation claim)

- Start Screen tabs and Editor dialogs across modules: Google Workspace interaction patterns.
- Welcome and Create New: WPS Office patterns.
- Options, Crash Logs and About: Android Settings patterns; About is planned as vertically paged sections with accessible non-gesture navigation.
- Editor screens: M365 Copilot mobile office patterns.
- Standard Bottom Sheet: Microsoft Office 365 command model for Inky, Cellina and Slidia; SoftMaker FlexiPDF for Pagella.
- General office concepts and layout: LibreOffice, adapted rather than copied from desktop.
- Icons: shipped dependency `material-icons-extended` is used as both Rounded and Default/Filled (`CellinaModule.kt:569`, `PagellaModule.kt:147`); Material Symbols Rounded is the target family. Colibre is an optional candidate pending asset/license review. A Papirus wallpaper palette (target, not shipped) is distinct from Android 12+ system dynamic color and should use system wallpaper colors directly where available, with a user-selected image fallback if needed.
- Reference editions: LibreOffice 26.2 guides (`https://books.libreoffice.org/en/`), ODF 1.4 in `docs/odf`, ECMA-376 for OOXML; design sources are listed by filename in `DESIGN.md` front matter.

## 🏛️ 2. Document Engine and Native Bridge

Papirus currently uses its Kotlin document engine for office-document parsing, editing and layout. LibreOfficeKit libraries are bundled and probed at startup, but a native document-rendering path is not established in the current builds: `LokitEngine` reports its availability and falls back to Kotlin when loading fails.

### A. The Papirus Engine (`com.makerandreas.papirusoffice.data`)
A pure Kotlin and Jetpack Compose document engine that directly parses document structures and renders them dynamically in Compose canvas and layout components:
- **`OfficeDocumentParser` & `DocxDocumentParser`**: Unpacks ZIP packages (`content.xml`, `styles.xml`, `document.xml`, `xl/worksheets/`, `ppt/slides/`), parses XML nodes, extracts inline images, metadata, and styles.
  - **ODF Processing (`SvXMLImport`, `SvXMLImportContext`, `OdfXmlToken`)**: Context-driven parser paths exist for text documents (`.odt`), spreadsheets (`.ods`) and presentations (`.odp`); coverage is partial and continues to be measured against real sample files and ODF 1.4.
  - **OpenXML / OOXML Processing**:
    - `.docx`: WordprocessingML parser paths cover selected paragraphs, runs, headings, tables and styles; advanced style resolution, numbering, fields and section behavior remain fidelity work.
    - `.xlsx`: SpreadsheetML paths include shared strings, workbook sheets, cell references and selected table content; feature coverage is not complete.
    - `.pptx`: PresentationML paths include slides, titles, body paragraphs and slide breaks; feature coverage is not complete.
- **Pagination and text layout**: `LayoutEngine` owns pagination. `ParagraphMeasurer` produces line fragments using the injected `TextMetrics` measurement backend; `LayoutUnits`, `FontRegistry`, `PageTransform` and `PageEndReason` support measurement, font resolution, page transforms and page-ending decisions. `TextLayoutManager` is used by the debug formatting inspector, not the pagination path (`InkyModule.kt:3897`).
- **`SwDocEngine` / `EditingEngine`**: Core word processing document model handling character spans, formatting attributes, and cursor selections.
- **`DocumentSerializer`**: Serialization paths exist; package round-trip integrity and complete ODF/OOXML conformance are not yet established. Save fidelity is tracked separately in the Plan 9 work.

### B. LibreOfficeKit (LOKit) JNI Bridge
**Status: native libraries bundled in the repository; runtime use is not yet established.** Pre-built LibreOffice Viewer for Android binaries (`liblo-native-code.so` + NSS dependency chain) are tracked with Git LFS under `app/src/main/libs/<abi>/` (`arm64-v8a`: pointer size 196,227,296 bytes; `armeabi-v7a`: 134,699,252 bytes) and packaged through `jniLibs.directories.add("src/main/libs")` in `app/build.gradle.kts`. A checkout without LFS only contains pointer files of about 130 bytes, so size or "stub" statements made from such a checkout (audit-006 §2.1) are not evidence; CI checks out with `lfs: true` and stores `scripts/native-inventory.sh` output with the test reports. `LibreOfficeCore` probes the library at startup; `LokitEngine` reports NATIVE vs SIMULATED to the About screen and diagnostics log and falls back to the pure-Kotlin engine when loading fails. Every rendering path in the current builds is the Kotlin engine.
- Planned: native C++/JNI bindings to LibreOffice's core rendering engine (`LibreOfficeKit`) for high-fidelity vector tile rendering, complex table layout recalculation, OpenFormula evaluation in spreadsheets, and lossless PDF conversion.
- Console event logs (`lok::Document::postWindow`, `lok::Document::dispatch`) mirror LOKit dispatch names; entries are tagged `[simulated]` until a native build is bundled.
- **Modular Equation Pipeline**:
  - `EquationParser` converts selected LaTeX-style input to MathML and OMML representations.
  - Writer-side embedding and round-trip support in ODF/OOXML packages is not yet complete; do not describe the pipeline as end-to-end until package tests verify it.
  - Build-flag gating (`ENABLE_MATHML_SUPPORT`, `ENABLE_OMML_SUPPORT`) is a possible future boundary, not a current guarantee.

### C. State & Session Management
- **`DocumentSessionState` & `DocumentSession`**: Represents the active document lifecycle, file path, temporary caches, dirty flags (`isSaved`), and layout configurations.
- **`UndoManager` & `HistoryManager`**: Dual-stack Command Pattern (`UndoAction`). Tracks all document mutations (typing, deletions, style changes, insertions).
  - Features single-step `undo()` / `redo()`.
  - Supports deep history stack jumps (`undoTo(entry)` / `redoTo(entry)`).
  - Includes a **Synchronous Typing Flusher** to prevent race conditions between active text input buffers and keyboard selection deletions.

---

## 📱 3. Screens & Navigation Hierarchy

### 1. Start Screen (Start Center)
The central launchpad of the application (equivalent to LibreOffice *Start Center*):
- **Recents Tab**: Chronological list of recently accessed documents with file thumbnails, timestamps, file sizes, pinned/starred status, and module color badges.
- **Files Tab (`FilesSubPage`)**: Device file system browser with folder traversal, sorting, search, and Android Storage Access Framework (SAF) system picker integration.
- **Google Drive Tab shipped (`HomeDashboard.kt:1210-1275`)**: no OAuth, Drive listing, upload or download exists. The screen advertises Google Workspace cloud access and offers a "Connect Google Account" button, but pressing it raises only a placeholder toast. **Target, Plan 11 home-entry package / Plan 3 copy backlog**: disclose the unavailable state on screen before asking for a press.
- **Top Bar & Module Filter Chips**: Filter view by All, Inky (Writer), Cellina (Calc), Slidia (Impress), or Pagella (PDF).
- **Search Bar shipped (`HomeDashboard.kt:655-690`)**: filters Recents by filename only; it does not search authors or document content, and the top search query is not passed to Files or Drive (`HomeDashboard.kt:571-580`). **Target, Plan 11 home-entry package**: label any broader search by the fields it really indexes.

### 2. Create New Screen (`NewDocumentScreen`)
Opened from the Start Screen FAB (`HomeDashboard.kt:534-550`, route `create_new_document`). Verified against `ui/home/NewDocumentScreen.kt` and `core/util/TemplateManager.kt` on 2026-09-27.

**Shipped**
- **Frame**: `TopAppBar` titled "Create New Document" with a back arrow, a two-page `HorizontalPager`, and a bottom `NavigationBar` with the tabs "Create New" and "Create from Template". The pager opens on "Create New", so the shipped flow is module-first. A search icon appears in the app bar only on the template tab.
- **Tab 1, "Create New Document"**: three equal cards (Inky Document, Cellina Spreadsheet, Slidia Presentation; 48 dp module logo, 16 dp corner radius, 1 dp outline). Each card sets `MainActivity.pendingNewDocument = true` and opens the module on the bundled blank package (`assets/templates/Untitled.odt`, `.ods`, `.odp`, extracted by `TemplateManager.get*TemplateFile`).
- **Tab 1, "Create Pagella PDF Document"**: a grouped list with three rows: *Create from Image* (JPEG/PNG/WebP through `PagellaPdfCreator.createPdfFromImageUri`), *Create from Camera* (CAMERA permission, FileProvider, `createPdfFromImageFile`) and *Convert from Document* (ODF, OOXML and legacy MS types through `convertDocumentToPdf`). Each row opens Pagella and confirms with a toast.
- **Tab 2, "Create from Template"**: filter chips All / ODT / ODS / ODP, a search field, and a result list from `TemplateManager.searchTemplates`: the built-in `curatedTemplates` list first, then Google Custom Search (`TemplateSearchRepository`), then a Gemini-generated URL list. Downloads are size-capped and name-sanitized into `getExternalFilesDir("templates")`; a downloaded file opens as an ordinary document. Offline and no-result empty states exist.
- **New document identity**: a new Inky document is titled "Untitled Document" (`strings.xml:533`) and reports as unsaved until the first Save.

**Known deltas (code work, owned by the Plan 11 home-entry surfaces package and the Plan 3 backlog)**
- 9 of the 12 `curatedTemplates` entries point at `filesamples.com` sample documents under invented names and descriptions; they are placeholders, not templates (antislop R-38, R-17). The three remaining entries are the blank `untitled.*` assets.
- 112 real LibreOffice templates are bundled under `assets/templates/` (`personal/`, `officorr/`, `offimisc/`, `presnt/`, `wizard/`, `styles/`) and none is surfaced in the UI.
- The bundled blank packages `Untitled.odt`, `Untitled.ods` and `Untitled.odp` are Collabora Office 26.04.3.1 resaves. Each has `office:version="1.4"`, a `meta.xml` entry and `mimetype` first in the ZIP; the ODT includes A4 page geometry. `CreateNewDocumentTest.templateManager_extractsUntitledOdtOdsOdp` checks the extracted packages. `TemplateManager` opens the exact asset path and returns `null` if extraction fails. The asset names use an uppercase `U`; extracted cache names remain lowercase (`untitled.*`).
- The in-editor "Create from Template" dialog (`InkyModule.kt:771,1154-1219`) replaces the body with a fabricated resume text (`InkyModule.kt:1157`).
- The screen still holds two literals ("Querying ODF repositories...", "Papirus Template • type") and raw per-type hex colours; the Welcome button label "Get Started" is a generic CTA (R-15).
- No template previews exist.

**Target** (design pattern: WPS Office create flow, detailed in `DESIGN.md` surface brief "Create New"): blank documents first with a real, previewable template gallery built from the bundled LibreOffice templates, per-module grouping, and an honest offline state; online sources only as an explicit, opt-in extension.

### 2a. Welcome Screen (`WelcomeScreen`, first run only)
- **Shipped**: shown once when `papirus_first_run/is_first_run` is true (`MainActivity.kt:304-306`): one centered column with a 112 dp logo tile (28 dp radius, 80 dp icon, `WelcomeScreen.kt:40-52`), the title "Welcome to Papirus Office", a short body about on-device documents and the system file picker, and one full-width 56 dp button. No permission is requested; the key name `welcome_grant_btn` is a leftover from an earlier permission design.
- **Target**: WPS-inspired friendly entry with a specific primary action label, defined in `DESIGN.md`.

### 3. About Screen (`AboutScreen`)
- Current/target content includes version and system information, engine status, project attributions and open-source licenses.
- Target: present About as vertically paged sections with clear indicators/buttons and a non-gesture navigation path; keep vertical swipe as an optional page transition, not the only navigation method. Shipped: a `Scaffold` with a `LazyColumn` of sections (`AboutScreen.kt:46,81`); no pager.
- State ODF/OOXML coverage and LibreOfficeKit native/simulated status accurately; do not imply complete conformance or active native rendering without evidence.

### 4. Papirus Office Options (Settings)
- **General Options**: User profile (author name, initials), autosave frequency, default file format (ODF vs OOXML).
- **Inky View Settings (`InkyViewSettingsSubpage`)**: Toggles for margins, page shadows, non-printing formatting marks (pilcrow `¶`, spaces, tabs), and spellcheck underline.
- **Load / Save Preferences (`LoadSaveGeneralSubpage`)**: Auto-recovery settings, backup copies on save, default directory paths.
- **Appearance & Theming**: Shipped (`ThemeSettings`, `PapirusOfficeOptionsScreen.kt:699-776`): a "Dynamic Color (Material You)" switch (API 31+, default on) and a theme mode choice System / Light / Dark, stored in `papirus_office_theme_prefs`. With dynamic colour off, or below API 31, the per-workspace Papirus static schemes from `Color.kt` apply. Target: user custom colours and a separate Papirus palette derived from system wallpaper colours (not from the system dynamic scheme) with an image-picker fallback. The Appearance copy is still hard-coded in the composable rather than in `strings.xml`.
- **Crash Logs Screen (`CrashLogsScreen`)**: Diagnostic console, stack trace viewer and log export, organized with Android-settings-inspired patterns.

### 5. Editor Screen (Dual Modes: Viewer & Editor)
Every suite module operates in two distinct modes:
- **Viewer Mode**: Clean, distraction-free reading canvas. Edit bars and toolbars are collapsed. Tap-to-select enables read-only FCT (Copy, Select All).
- **Editor Mode**: Interactive editing with virtual keyboard integration, live formatting, real-time typing buffer, Toolbar Hub, and Bottom Sheet Ribbon.
  - **Inky Module**: Word processor with paginated/continuous document view, zoom scaling, margin rulers, and text canvas.
  - **Cellina Module**: Spreadsheet workbook with row/column headers, cell grid, formula bar (`=SUM(...)`), sheet tabs, and cell coordinate selector.
  - **Slidia Module**: Presentation deck editor with thumbnail navigation rail, slide canvas, speaker notes drawer, and presentation slideshow playback.
  - **Pagella Module shipped**: Android `PdfRenderer` current-page display, zoom/previous/next, and in-memory ink paths (`PagellaModule.kt:85-120,132-198,202-299`). **Target, Plan 11 Pagella package**: thumbnail scrubber, continuous paging and persisted annotation/export; do not cite the fallback's native-renderer text as implementation evidence (`PagellaModule.kt:229-253`).

### 6. Loading Screen & Splash
- Startup initialization screen displaying suite branding while `LibreOfficeCore` probes `liblo-native-code.so` (SIMULATED fallback when it does not load), fonts are registered, and the document package is parsed.

---

## 🎛️ 4. Toolbar & Interaction Ecosystem

### A. FCT (Floating Contextual Toolbar)
An intelligent popup toolbar (`PapirusTextToolbar.kt`) anchored dynamically directly above or below selected text or cursor position within the document viewport:
- **Automatic Viewport Bounding**: Dynamically flips above or below the selection depending on available screen real estate, top app bar clearance, and virtual keyboard height.
- **FCT Modes (`FctMode`)**:
  1. **Compact Mode**: A sleek, horizontal pill container providing immediate text actions:
     - Cut, Copy, Paste, Delete, Select All.
     - **AI Options** button.
     - **More (`...`)** button to expand into General Options.
  2. **General Options Mode (Expanded)**: A 260dp card view with category navigation:
     - *Selection Mode...* (Non-contiguous text selection, Block/rectangular selection).
     - *Character* (Direct jump to Character Style & Options).
     - *Paragraph* (Direct jump to Paragraph Style & Options).
     - *Section Options...*
     - *Set Reminder* (Creates local document reminder notification).
     - *Bullets and Numbering Options...*
     - *Skip Numbering / Remove Numbering / Restart from Beginning*.
     - *Tab Settings / Border Settings / Shading Settings*.
     - *Synonyms* (Contextual thesaurus lookup).
  3. **Selection Mode**: Switches between non-contiguous text selection and rectangular block selection.
  4. **Character Mode**: Fine-grained character styles and typography settings.
  5. **Paragraph Mode**: Paragraph formatting, line spacing, and paragraph alignment.
  6. **Synonyms Mode**: Contextual word synonym suggestions retrieved from the integrated offline dictionary.
  7. **AI Options Mode**: Gemini AI-powered generative assistant:
     - Generate Text from prompt.
     - Proofread and correct grammatical errors.
     - Translate into target languages.
     - Rewrite with distinct tones (*Lucu*, *Profesional*, *Akademis*, *Naratif*).

### B. Toolbar Hub
A horizontally scrollable quick-action toolbar docked immediately above the virtual keyboard (or viewport bottom in edit mode):
- **Left Scrollable Section**:
  - Font Style Dropdown (family preview).
  - Font Size Dropdown (numerical display with stepper/sheet trigger).
  - Quick Formatting: Bold, Italic, Underline (with long-press to open Underline Options), Strikethrough.
  - Color Pickers: Highlight Color, Font Color.
  - Lists: Bulleted List, Numbered List.
  - Indentation: Decrease Indent, Increase Indent.
  - Insert Objects: Image, Table, Hyperlink, Comment.
- **Persistent Trailing Actions**:
  - Insert Tab character (`\t`).
  - Toggle Soft Keyboard button.
  - **Open Ribbon Button** (semantic command icon): Opens the adaptive Standard Bottom Sheet.

### C. Standard Bottom Sheet (Material 3 Expressive Adaptive Command Deck)
A touch-oriented bottom-sheet surface. A compact height around 40% may be a starting point on phones, but the deck expands or scrolls for content, keyboard, display-size and accessibility needs; it is not a fixed 40% cap. It is planned to contain:
1. **Ribbon Deck (`bottomBarDeck = "ribbon"`)**: tabbed command deck with six tabs (File, Home, Insert, Layout, Review, View). File and Home are implemented; Insert, Layout, Review and View are declared and render as visibly unavailable (PR 13). References and Mailings are not part of the Writer set:
   - **File Tab**: Save, Save As, Export PDF, Print, Share, Document Properties.
   - **Home Tab**: Clipboard actions, Font styling, Paragraph alignment/spacing, Paragraph Styles gallery.
   - **Insert Tab**: Image, Table, Shape, Page Break, Header/Footer, Bookmark, Hyperlink.
   - **Layout Tab**: Margins, Page Orientation (Portrait/Landscape), Paper Size (A4, Letter, Legal), Columns, Watermark.
   - **Review Tab**: Spellcheck language, Word Count dialog, Track Changes, Comment management.
   - **View Tab**: Viewer vs Editor mode switch, 100% / Fit Width zoom, Show/Hide Rulers, Non-printing characters.
2. **Navigator Deck (`bottomBarDeck = "navigator"`)**:
   - Universal document tree navigator (`UniversalNavigatorSheet.kt`, `DocumentNavigator.kt`).
   - Lists Headings, Tables, Text Frames, Graphics/Images, Bookmarks, Sections, Hyperlinks, Comments, and Footnotes.
   - Allows instant tap-to-scroll navigation to any document element.
3. **Navigate By Deck (`bottomBarDeck = "navigate_by"`)**:
   - Quick stepper navigation allowing the user to browse through previous/next elements by Heading, Page, Table, Graphic, or Bookmark.
4. **Dedicated Bottom Sheet Subpages**:
   - **Font Style**: Comprehensive font family list (Google Sans, Roboto, Merriweather, Open Sans, Noto Serif, Montserrat, etc.).
   - **Font Size**: Step-based and slider-based typography point size picker (6pt – 96pt).
   - **Color Pickers**: Modern M3 color palette grids with custom hex/RGB inputs for **Text Color**, **Highlight Color**, and **Paragraph Shading Color**.
   - **Underline Style & Color**: Underline styles (Single, Double, Dotted, Dashed, Wave) and dedicated Underline Color picker.
   - **Bullet & Numbering Options**: Disc, Circle, Square, Arrow, Checkmark, Decimal (1, 2, 3), Roman Numerals (I, II, III / i, ii, iii), Alphabetical (A, B, C / a, b, c), and Multilevel lists.
   - **Border Settings**: Top, Bottom, Left, Right, Box, All, None, with border stroke width and border color.
   - **Paragraph Styles**: Standard styles (Normal, Title, Subtitle, Heading 1, 2, 3, Quote) plus *Create New Style* and *Style Options* inspectors.
   - **Actions to Undo / Redo (`ActionsToUndoSubpage.kt`)**: Visual timeline list of recorded actions in the history stack, enabling multi-step rollback or rollforward in a single tap.
   - **Change Capitalization**: UPPERCASE, lowercase, Title Case, Sentence case, tOGGLE cASE.

---

## 📚 5. Terminology & Glossary

| Term | Full Name / Meaning | Context & Role in Papirus Office |
|---|---|---|
| **FCT** | **Floating Contextual Toolbar** | Floating action menu positioned dynamically near the active text selection or cursor. Has Compact and Expanded modes. |
| **FCT Compact** | Compact Floating Toolbar | Pill-shaped floating bar showing essential quick actions (Cut, Copy, Paste, Delete, Select All, AI, More). |
| **FCT Expanded** | Expanded Floating Toolbar | Multi-category card dialog for Character, Paragraph, Section, Reminders, Lists, Borders, Shading, and AI. |
| **Toolbar Hub** | Contextual Keyboard Dock | Horizontally scrollable formatting bar docked directly above the virtual keyboard or bottom of viewport. |
| **Standard Bottom Sheet** | Adaptive Command Deck | Bottom-sheet host for the Ribbon, Navigator and formatting subpages; starts compact and can expand or scroll as needed. |
| **Ribbon** | Tabbed Office Ribbon Deck | Six-tab command deck (File, Home, Insert, Layout, Review, View); File and Home implemented, the other four visibly unavailable. |
| **Navigator** | Document Structure Tree | Outlining tool displaying hierarchical document nodes (Headings, Tables, Bookmarks, Sections, Images) for rapid jumping. |
| **Navigate By** | Element Navigation Stepper | Quick navigation controller to step forward/backward through specific elements (e.g. Next Bookmark, Previous Table). |
| **Inky** | Word Processing Module | The word processing component of Papirus Office (equivalent to LibreOffice Writer / MS Word). |
| **Cellina** | Spreadsheet Module | The spreadsheet component of Papirus Office (equivalent to LibreOffice Calc / MS Excel). |
| **Slidia** | Presentation Module | The slide presentation component of Papirus Office (equivalent to LibreOffice Impress / MS PowerPoint). |
| **Pagella** | PDF & Document Manager | The PDF viewing, annotating, and format conversion component of Papirus Office. |
| **Start Center / Start Screen** | Document Hub | Top-level dashboard with Recents, Files (device explorer + SAF), the Google Drive placeholder tab, filter chips and the Create New FAB. |
| **LOKit / LibreOfficeKit** | Bundled native libraries and JNI probe (`app/src/main/libs/<abi>/`) | `liblo-native-code.so` + NSS chain from LibreOffice Viewer for Android. `LokitEngine` reports NATIVE or SIMULATED and falls back to Kotlin when loading fails. A probe result does not establish that document rendering uses LOKit; current pagination uses `LayoutEngine`. |
| **ODF v1.4** | OASIS OpenDocument Format 1.4 | The open international standard format for office documents (`.odt`, `.ods`, `.odp`), with authoritative project copies in `docs/odf/`. |
| **OOXML** | Office Open XML | Microsoft Office document format standard (`.docx`, `.xlsx`, `.pptx`). |
| **DocumentSession** | Active Document Session | State container tracking the open document, edit mode, dirty flag, file URI, autosave state, and engine instances. |
| **UndoManager / HistoryManager** | Undo & Redo Engine | Stack-based command pattern engine tracking `UndoAction` items with multi-step history and synchronous typing flush. |

---

## 📁 6. Project Directory Structure & Key Files

```
├── AGENTS.md                                # Injected system guidelines and project conventions for AI agents
├── DESIGN.md                                # Design direction: identity, reference map, tokens, surface briefs (target vs shipped)
├── PROJECT_CONTEXT.md                       # (This file) Master architecture & context reference
├── metadata.json                            # AI Studio application metadata (Name, Description, Capabilities)
├── docs/odf/                               # Authoritative OASIS ODF v1.4 and OpenFormula specifications (HTML)
│   ├── OpenDocument-v1.4-part1-introduction.html
│   ├── OpenDocument-v1.4-part2-packages.html
│   ├── OpenDocument-v1.4-part3-schema.html
│   └── OpenDocument-v1.4-part4-formula.html
├── docs/InkyC1Checklist.md                  # Writer Guide 26.2 Chapter 1 device test checklist
├── docs/lo-guides/GS262-GettingStarted_compressed.pdf # LibreOffice 26.2 Getting Started guide
├── docs/lo-guides/WG262-WriterGuide_compressed.pdf    # LibreOffice 26.2 Writer guide
├── docs/lo-guides/CG262-CalcGuide.pdf                 # LibreOffice 26.2 Calc guide
├── docs/lo-guides/IG262-ImpressGuide_compressed.pdf   # LibreOffice 26.2 Impress guide
├── docs/lo-guides/DG262-DrawGuide.pdf                 # LibreOffice 26.2 Draw guide
├── docs/design.md-references/                 # Owner-provided design reference summaries
├── sdk-references/                            # LibreOffice SDK reference examples
├── tests/                                     # Format fixtures and regression inputs
├── skills/                                    # Project antislop skills
├── CONCEPT.md                                 # Product concept and interaction terminology
├── antislop.md                                # Antislop core rules
├── anti-slop/                               # Numbered audits and plans (evidence trail)
├── app/src/main/assets/templates/           # 115 template assets; Create New uses Untitled.od{t,s,p}
├── app/src/main/libs/                       # Pre-built native .so per ABI (LibreOffice Viewer for Android, Git LFS)
│   ├── arm64-v8a/                           # liblo-native-code.so + NSS dependency chain
│   └── armeabi-v7a/                         # liblo-native-code.so + NSS dependency chain
└── app/src/main/java/
    ├── com/example/
    │   ├── MainActivity.kt                  # Root Activity, edge-to-edge window setup, navigation host
    │   ├── modules/
    │   │   ├── inky/InkyModule.kt           # Master Inky (Writer) screen: FCT, Toolbar Hub, Bottom Sheet, Editor
    │   │   ├── cellina/CellinaModule.kt     # Master Cellina (Calc) screen: Grid canvas, formula bar
    │   │   ├── slidia/SlidiaModule.kt       # Master Slidia (Impress) screen: Slide editor, presenter
    │   │   └── pagella/PagellaModule.kt     # Master Pagella (PDF) screen: PDF viewer & page manager
    │   └── ui/
    │       ├── components/
    │       │   ├── PapirusTextToolbar.kt    # Full FCT (Compact & Expanded) implementation
    │       │   ├── ActionsToUndoSubpage.kt  # Actions to Undo / Redo visual history list
    │       │   ├── UniversalNavigatorSheet.kt # Document Navigator tree sheet
    │       │   ├── SwTextFormattingInspectorDialog.kt # Deep character & paragraph inspector
    │       │   ├── GeminiCopilotDialog.kt   # Gemini AI document copilot modal
    │       │   └── SaveAsDialog.kt          # Export & Save As format dialog
    │       ├── home/
    │       │   ├── HomeDashboard.kt         # Start Screen / Start Center (Recents, Filter chips)
    │       │   ├── FilesSubPage.kt          # Device file system explorer & SAF picker
    │       │   ├── NewDocumentScreen.kt     # Create New: module cards, Pagella PDF entries, template tab
│       │   ├── WelcomeScreen.kt         # First-run welcome card
    │       │   ├── AboutScreen.kt           # About Papirus Office dialog & licensing
    │       │   └── CrashLogsScreen.kt       # Error diagnostics & crash log viewer
    │       └── options/
    │           ├── PapirusOfficeOptionsScreen.kt # Master options & settings screen
    │           ├── InkyViewSettingsSubpage.kt    # Inky view toggles (margins, marks)
    │           └── LoadSaveGeneralSubpage.kt     # Load/Save preferences
    └── com/makerandreas/papirusoffice/
        ├── PapirusApplication.kt            # Application subclass, crash handler, provider initialization
        └── data/
            ├── DocumentCoreEngines.kt       # Parser, serializer, and document core orchestrator
            ├── DocumentNavigator.kt         # Navigator data engine (headings, bookmarks, tables)
            ├── DocumentSession.kt           # Document session lifecycle, dirty state, autosave
            ├── UndoManager.kt               # UndoManager, HistoryManager, UndoAction
            ├── SwDocEngine.kt               # Word processing document model
            ├── LayoutEngine.kt              # Text layout and line calculation engine
            ├── OfficeDocumentParser.kt      # ODF & OOXML package parser (ODT, ODS, ODP, DOCX, XLSX, PPTX)
            ├── DocxDocumentParser.kt        # DOCX & multi-format parser adapter
            └── odf/                         # Modular ODF import engine
                ├── OdfXmlToken.kt           # ODF XML tokens (styles, draw, text, table)
                ├── SvXMLImport.kt           # Parser coordinator & document builder
                └── SvXMLImportContext.kt    # Context hierarchy for paragraphs, tables, drawings, pages
```

---

## 🗺️ 7. Architecture Phases & Development Roadmap

Papirus Office (codenamed LibreDroid Office during conceptualization) follows a structured phased development roadmap:

- **Phase 1: Environment Setup** ✅: Android build environment, Gradle Kotlin DSL, Version Catalog, Room, and Compose setup.
- **Phase 2: JNI Implementation & Stress Test Stage 1** 🔄: native `.so` bundled under `app/src/main/libs/<abi>/` (LibreOffice Viewer for Android); `LokitEngine` probe + fallback landed, Kotlin→JNI facade calls pending.
- **Phase 3: Building LibreOffice Core Engine & OOXML Compatibility Foundation** ✅ / 🔄
  - *Task 1: LibreOffice Core JNI Integration* 🔄: native probe + simulated fallback landed; remaining Kotlin `external` facade methods not yet wired to native.
  - *Task 2: Real-World Document Stress Testing* 🔄: compatibility suite added (`SampleFilesCompatibilityTest`); must be green in CI before claiming.
  - *Task 3: OOXML Standards & SDK References* ✅: ECMA-376 specifications, OpenXML SDK architecture.
  - *Task 4: OOXML Compatibility Foundation* 🔄: Kotlin parsers cover selected WordprocessingML, SpreadsheetML and PresentationML structures; equation conversions exist, but package persistence and broad format fidelity remain incomplete.
  - *Task 5: Reverse Engineering & Behavioral Testing* 🔄: planned; no verification artifacts in the repo yet.
- **Phase 4: Core Editing & UI Implementation (Material 3 Expressive)** 🔄: FCT, Toolbar Hub, adaptive Standard Bottom Sheet, touch targets and Navigator Deck; design target and shipped coverage are tracked separately in `DESIGN.md` and the plans.
- **Phase 5: ARM Optimization** 🔜: ARMv7 and ARM64-v8a neon optimizations, binary size minimization.
- **Phase 6: Low-End Device Testing** 🔄: XLSX/PPTX streaming + archive budgets landed; on-device 2 GB profiling pending (see `docs/PHASE6_MEMORY_PLAN.md`).
- **Phase 7: PC-Level Office Features Rollout** 🔜: Diagrams (Mermaid.js), Equations (KaTeX/MathML/OMML), Stylus Ink (Google Ink API), Citations/BibTeX, Full 500+ OpenFormula functions, and Optional Gemini AI Copilot.

---

## 🔗 8. Key Reference Links & Specifications
- **LibreOffice Core**: [gerrit.libreoffice.org](https://gerrit.libreoffice.org/) / [github.com/LibreOffice/core](https://github.com/LibreOffice/core)
- **Collabora Online**: [github.com/CollaboraOnline/online](https://github.com/CollaboraOnline/online)
- **ECMA-376 OOXML**: [ecma-international.org](https://ecma-international.org/publications-and-standards/standards/ecma-376/)
- **Microsoft OpenXML SDK**: [learn.microsoft.com](https://learn.microsoft.com/en-us/office/open-xml/open-xml-sdk) / [github.com/dotnet/Open-XML-SDK](https://github.com/dotnet/Open-XML-SDK)
- **OASIS OpenDocument (ODF v1.4)**: Authoritative project specification files in `docs/odf`
- **Google Ink API**: [developer.android.com/develop/ui/views/touch-and-input/stylus-input/about-ink-api](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/about-ink-api)
- **KaTeX / MathML**: [github.com/KaTeX/KaTeX](https://github.com/KaTeX/KaTeX)
- **Mermaid.js**: [github.com/mermaid-js/mermaid](https://github.com/mermaid-js/mermaid)
- **Material 3 Expressive**: [m3.material.io](https://m3.material.io/)
- **LibreOffice 26.2 guides** (Getting Started, Writer, Calc, Impress, Draw): [books.libreoffice.org/en](https://books.libreoffice.org/en/index.html); Writer Guide Chapter 1: [WG26201-IntroducingWriter](https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html)

---

## ⚙️ 9. Engineering Guidelines for Future AI Agents

1. **ODF v1.4 Normative Rule**: When parsing, serializing or modifying OpenDocument structures, consult the checked-in standard files in `docs/odf`. Preserve package content where the implementation supports it and verify round trips against LibreOffice and Microsoft Office; do not assume preservation without tests.
2. **Text Input & Undo Synchronization**: When modifying text editing or Undo/Redo logic, always ensure the active typing buffer is synchronously flushed to `UndoManager` prior to handling deletions, selections, or external undo actions to avoid debounce race conditions.
3. **Touch Targets & Accessibility**: Every interactive control, toolbar icon, menu item, and button MUST have a minimum touch target size of `48dp x 48dp` with meaningful `contentDescription`.
4. **Theme & Color Fidelity**:
   - Maintain module color separation: Base Suite Blue (`#2563EB`), Inky Green (`#0F9D58`), Cellina Teal (`#16A3B7`), Slidia Amber (`#F59E0B`), Pagella Red (`#D93025`).
   - Shipped: Android 12+ system dynamic color by default (user switch), Papirus static schemes below Android 12 or when the switch is off, theme mode System / Light / Dark. Target: custom and user-selected-wallpaper palette modes. Always use semantic `MaterialTheme.colorScheme` roles.
   - Google Sans is the UI target, not a validated shipped face: `Type.kt` requests `OptionalLocal` font resources, but read-only fontTools parsing found malformed bundled TTFs and byte-identical weights (audit-009 §9.3). `README.txt`/`OFL.txt` do not establish those binaries' provenance. System sans-serif is the intended fallback pending a device and licence check in a later asset/code plan. UI typography stays separate from document font identity and saved styles.
5. **Testing Verification**:
   - Execute local JVM tests via `gradle :app:testDebugUnitTest`.
   - Never attempt to launch emulators or run instrumented tests requiring `adb`.
   - There is no local compile helper in this repository. CI (`.github/workflows/build.yml`) runs `./gradlew testDebugUnitTest` on every push and pull request; a sandbox without a JDK cannot compile, so say so and rely on CI plus the owner's device test.
