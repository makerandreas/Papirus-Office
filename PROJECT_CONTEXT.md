# Papirus Office — Project Description Context & Master Architecture Reference

---

## 📘 1. Product Identity & Overview

**Papirus Office** (initially codenamed and developed as **LibreDroid Office**) is a modern, modular, open-source office suite engineered for Android smartphones, foldables, and tablets. It delivers a PC-class productivity and document editing experience on mobile devices while remaining strictly open-source, compliant with international document standards, legally compliant (adopting the "Papirus Office" brand to respect trademark separation from LibreOffice and The Document Foundation), and ergonomically optimized for touch interfaces.

- **Codename / Concept Heritage**: Originally conceptualized as **LibreDroid Office**, created to bridge the full power of desktop-class office editing (based on LibreOffice architecture and the Document Liberation Project) to mobile devices.
- **Document Standards Compliance**: Built on the **OASIS OpenDocument Format (ODF v1.4)** standards (`.odt`, `.ods`, `.odp`, `.ott`, `.ots`, `.otp`), with complete import/export compatibility for Microsoft Office OpenXML (`.docx`, `.xlsx`, `.pptx`) and Adobe PDF (`.pdf`).
- **Design System**: Strict adherence to **Material Design 3 (M3) Expressive**, featuring 8dp grid rhythm, fluid micro-interactions, full edge-to-edge layout, dynamic Google Sans UI typography, and module-specific color branding.
- **License**: Mozilla Public License 2.0 — see [LICENSE](LICENSE) (file-level copyleft; same steward-license family as LibreOffice's MPL-2.0).

### The Suite Modules & Color Conventions
Default static accent colors (for Android 11 and below, or when Dynamic Color is disabled):
- 🟦 **Papirus (Base Suite)**: `#2563EB` — Suite Home, Start Center, File Manager, Universal Options.
- 🟩 **Inky (Word Processing)**: `#0F9D58` — Documents, text editing, typography, layouts (`.odt`, `.docx`, `.txt`, `.rtf`). *(Equivalent to LibreOffice Writer / MS Word)*
- 🟦 **Cellina (Spreadsheets)**: `#16A3B7` — Spreadsheets, grid calculations, formulas, charts (`.ods`, `.xlsx`, `.csv`). *(Equivalent to LibreOffice Calc / MS Excel)*
- 🟧 **Slidia (Presentations)**: `#F59E0B` — Slide decks, presentations, animations, speaker notes (`.odp`, `.pptx`). *(Equivalent to LibreOffice Impress / MS PowerPoint)*
- 🟥 **Pagella (PDF & Document Manager)**: `#D93025` — PDF viewing, document annotation, page extraction, format conversion.

---

## 🏛️ 2. Core Architecture & Dual-Engine Model

Papirus Office utilizes a **Dual-Engine Architecture** balancing lightweight mobile rendering with desktop-grade document fidelity:

### A. The Papirus Engine (`com.makerandreas.papirusoffice.data`)
A pure Kotlin and Jetpack Compose document engine that directly parses document structures and renders them dynamically in Compose canvas and layout components:
- **`OfficeDocumentParser` & `DocxDocumentParser`**: Unpacks ZIP packages (`content.xml`, `styles.xml`, `document.xml`, `xl/worksheets/`, `ppt/slides/`), parses XML nodes, extracts inline images, metadata, and styles.
  - **ODF Processing (`SvXMLImport`, `SvXMLImportContext`, `OdfXmlToken`)**: Fully modular context-driven parsing for text documents (`.odt`), spreadsheets (`.ods` with table column/row repeating and sheet metadata), and presentations (`.odp` with `<draw:page>`, `<draw:frame>`, `<draw:text-box>`, `<draw:custom-shape>`, `<draw:g>`).
  - **OpenXML / OOXML Processing**:
    - `.docx`: Direct WordprocessingML paragraph, run, heading, table, and style parsing.
    - `.xlsx`: Dedicated SpreadsheetML parser extracting shared strings (`xl/sharedStrings.xml`), workbook sheet definitions (`xl/workbook.xml`), cell references (A1 coordinate mapping), and sheet tables.
    - `.pptx`: Dedicated PresentationML parser extracting slides (`ppt/slides/slide*.xml`), slide title placeholders, body paragraphs, and slide breaks.
- **`LayoutEngine` & `TextLayoutManager`**: Calculates line wraps, margins, paragraph indentations, tabs, bullet prefixes, and multi-page layouts.
- **`SwDocEngine` / `EditingEngine`**: Core word processing document model handling character spans, formatting attributes, and cursor selections.
- **`DocumentSerializer`**: Writes lossless ODF v1.4 and OOXML packages respecting strict namespace schemas.

### B. LibreOfficeKit (LOKit) JNI Bridge & C++ OOXML / Equation Layer
**Status: planned (Phase 2 🔄)** — no native `.so` ships yet; the app runs its pure-Kotlin engine with a simulated fallback. The drop-in contract lives in `app/src/main/jniLibs/<abi>/` (see `docs/LOKIT_INTEGRATION.md`); `LokitEngine` reports the active mode to the About screen and diagnostics log.
- Planned: native C++/JNI bindings to LibreOffice's core rendering engine (`LibreOfficeKit`) for high-fidelity vector tile rendering, complex table layout recalculation, OpenFormula evaluation in spreadsheets, and lossless PDF conversion.
- Console event logs (`lok::Document::postWindow`, `lok::Document::dispatch`) mirror LOKit dispatch names; entries are tagged `[simulated]` until a native build is bundled.
- **Modular Equation Pipeline**:
  - LaTeX-style syntax user input converted by `EquationParser` (fractions, roots, symbols; rendered KaTeX / MathJax preview is planned).
  - Bidirectional conversion: LaTeX ↔ MathML for ODF (`.odt`, `.ods`, `.odp`), LaTeX ↔ OMML for OOXML (`.docx`, `.xlsx`, `.pptx`).
  - Conversion is unconditional today; build-flag gating (`ENABLE_MATHML_SUPPORT`, `ENABLE_OMML_SUPPORT`) is planned.

### C. State & Session Management
- **`DocumentSessionState` & `DocumentSession`**: Represents the active document lifecycle, file path, temporary caches, dirty flags (`isSaved`), and layout configurations.
- **`UndoManager` & `HistoryManager`**: Robust dual-stack Command Pattern (`UndoAction`). Tracks all document mutations (typing, deletions, style changes, insertions).
  - Features single-step `undo()` / `redo()`.
  - Supports deep history stack jumps (`undoTo(entry)` / `redoTo(entry)`).
  - Includes a **Synchronous Typing Flusher** to prevent race conditions between active text input buffers and keyboard selection deletions.

---

## 📱 3. Screens & Navigation Hierarchy

### 1. Start Screen (Start Center)
The central launchpad of the application (equivalent to LibreOffice *Start Center*):
- **Recents Tab**: Chronological list of recently accessed documents with file thumbnails, timestamps, file sizes, pinned/starred status, and module color badges.
- **Files Tab (`FilesSubPage`)**: Device file system browser with folder traversal, sorting, search, and Android Storage Access Framework (SAF) system picker integration.
- **Google Drive Tab**: Cloud storage synchronization, Google Drive file listing, and download/cache mechanisms.
- **Top Bar & Module Filter Chips**: Filter view by All, Inky (Writer), Cellina (Calc), Slidia (Impress), or Pagella (PDF).
- **Search Bar**: Real-time filtering by document title, author, and content snippets.

### 2. Create New Screen (`NewDocumentScreen`)
- **Template Gallery**: Blank document templates alongside pre-formatted templates (Formal Letter, Modern Resume, Meeting Agenda, Academic Report, Invoice, Project Plan).
- **Module Selector**: Quick-create buttons for Blank Document (Inky), Blank Spreadsheet (Cellina), Blank Presentation (Slidia), or Scan/Import PDF (Pagella).

### 3. About Screen (`AboutScreen`)
- Displays application version, release channel, build date, and system info.
- Documents the LibreOfficeKit core engine attribution, Document Liberation Project contributions, OASIS ODF v1.4 compliance, and open-source licenses (GPL/LGPL/Apache).

### 4. Papirus Office Options (Settings)
- **General Options**: User profile (author name, initials), autosave frequency, default file format (ODF vs OOXML).
- **Inky View Settings (`InkyViewSettingsSubpage`)**: Toggles for margins, page shadows, non-printing formatting marks (pilcrow `¶`, spaces, tabs), and spellcheck underline.
- **Load / Save Preferences (`LoadSaveGeneralSubpage`)**: Auto-recovery settings, backup copies on save, default directory paths.
- **Appearance & Dynamic Theming**: Toggle between Material You dynamic palette (Android 12+) and static suite module accents.
- **Crash Logs Screen (`CrashLogsScreen`)**: Real-time error diagnostic console, stack trace viewer, and log export.

### 5. Editor Screen (Dual Modes: Viewer & Editor)
Every suite module operates in two distinct modes:
- **Viewer Mode**: Clean, distraction-free reading canvas. Edit bars and toolbars are collapsed. Tap-to-select enables read-only FCT (Copy, Select All).
- **Editor Mode**: Interactive editing with virtual keyboard integration, live formatting, real-time typing buffer, Toolbar Hub, and Bottom Sheet Ribbon.
  - **Inky Module**: Word processor with paginated/continuous document view, zoom scaling, margin rulers, and text canvas.
  - **Cellina Module**: Spreadsheet workbook with row/column headers, cell grid, formula bar (`=SUM(...)`), sheet tabs, and cell coordinate selector.
  - **Slidia Module**: Presentation deck editor with thumbnail navigation rail, slide canvas, speaker notes drawer, and presentation slideshow playback.
  - **Pagella Module**: PDF reader with thumbnail scrubber, multi-page continuous vertical scroll, zoom/fit-to-page, and annotation overlay.

### 6. Loading Screen & Splash
- Startup initialization screen displaying animated suite branding while loading core native libraries (`libsofficeapp.so`), fonts, and parsing document packages.

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
  - **Open Ribbon Button** (ViewAgenda icon with primary container color): Opens the 40% Standard Bottom Sheet.

### C. Standard Bottom Sheet (Material 3 Expressive 40% Screen Deck)
A persistent bottom sheet surface occupying exactly 40% of the screen height when active, featuring:
1. **Ribbon Deck (`bottomBarDeck = "ribbon"`)**: Desktop-class tabbed ribbon:
   - **File Tab**: Save, Save As, Export PDF, Print, Share, Document Properties.
   - **Home Tab**: Clipboard actions, Font styling, Paragraph alignment/spacing, Paragraph Styles gallery.
   - **Insert Tab**: Image, Table, Shape, Page Break, Header/Footer, Bookmark, Hyperlink.
   - **Layout Tab**: Margins, Page Orientation (Portrait/Landscape), Paper Size (A4, Letter, Legal), Columns, Watermark.
   - **References Tab**: Table of Contents, Insert Footnote, Insert Endnote, Citations.
   - **Mailings Tab**: Mail merge fields, envelope and label formatting.
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
| **Standard Bottom Sheet** | Persistent Bottom Sheet Deck | Bottom sheet occupying 40% of the screen height containing the Ribbon, Navigator, and formatting subpages. |
| **Ribbon** | Tabbed Office Ribbon Deck | Desktop-class multi-tab formatting panel (File, Home, Insert, Layout, References, Mailings, Review, View). |
| **Navigator** | Document Structure Tree | Outlining tool displaying hierarchical document nodes (Headings, Tables, Bookmarks, Sections, Images) for rapid jumping. |
| **Navigate By** | Element Navigation Stepper | Quick navigation controller to step forward/backward through specific elements (e.g. Next Bookmark, Previous Table). |
| **Inky** | Word Processing Module | The word processing component of Papirus Office (equivalent to LibreOffice Writer / MS Word). |
| **Cellina** | Spreadsheet Module | The spreadsheet component of Papirus Office (equivalent to LibreOffice Calc / MS Excel). |
| **Slidia** | Presentation Module | The slide presentation component of Papirus Office (equivalent to LibreOffice Impress / MS PowerPoint). |
| **Pagella** | PDF & Document Manager | The PDF viewing, annotating, and format conversion component of Papirus Office. |
| **Start Center / Start Screen** | Welcome & Document Hub | Top-level dashboard containing Recents, Device File Explorer, Google Drive Sync, and Create New actions. |
| **LOKit / LibreOfficeKit** | Planned native C++ engine (Phase 2 🔄) | Drop-in contract in `app/src/main/jniLibs/` (see `docs/LOKIT_INTEGRATION.md`); until bundled, the pure-Kotlin Papirus engine runs with simulated telemetry. |
| **ODF v1.4** | OASIS OpenDocument Format 1.4 | The open international standard format for office documents (`.odt`, `.ods`, `.odp`), authoritative in `/sources`. |
| **OOXML** | Office Open XML | Microsoft Office document format standard (`.docx`, `.xlsx`, `.pptx`). |
| **DocumentSession** | Active Document Session | State container tracking the open document, edit mode, dirty flag, file URI, autosave state, and engine instances. |
| **UndoManager / HistoryManager** | Undo & Redo Engine | Stack-based command pattern engine tracking `UndoAction` items with multi-step history and synchronous typing flush. |

---

## 📁 6. Project Directory Structure & Key Files

```
├── AGENTS.md                                # Injected system guidelines and project conventions for AI agents
├── DESIGN.md                                # Material 3 Expressive design tokens, colors, typography, shapes
├── PROJECT_CONTEXT.md                       # (This file) Master architecture & context reference
├── metadata.json                            # AI Studio application metadata (Name, Description, Capabilities)
├── sources/                                 # Authoritative OASIS ODF v1.4 and OpenFormula specifications
│   ├── OpenDocument-v1.4-cs01-part1-introduction.odt
│   ├── OpenDocument-v1.4-cs01-part2-packages.odt
│   ├── OpenDocument-v1.4-cs01-part3-schema.odt
│   └── OpenDocument-v1.4-cs01-part4-formula.odt
├── app/src/main/jniLibs/                    # Phase 2 drop-in: native .so per ABI (empty; see docs/LOKIT_INTEGRATION.md)
│   ├── arm64-v8a/                           # (planned) liblo-native-code.so + dependency chain
│   ├── armeabi-v7a/                         # (planned)
│   ├── x86_64/                              # (planned)
│   └── x86/                                 # (planned)
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
    │       │   ├── NewDocumentScreen.kt     # Create New document & template gallery
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

- **Phase 1: Environment Setup** ✅ — Android build environment, Gradle Kotlin DSL, Version Catalog, Room, and Compose setup.
- **Phase 2: JNI Implementation & Stress Test Stage 1** 🔄 — seam landed (`LokitEngine` probe + `jniLibs` drop-in contract, see `docs/LOKIT_INTEGRATION.md`); native `.so` not bundled yet.
- **Phase 3: Building LibreOffice Core Engine & OOXML Compatibility Foundation** ✅ / 🔄
  - *Task 1: LibreOffice Core JNI Integration* 🔄 — probe + simulated fallback; native facade pending (see `docs/LOKIT_INTEGRATION.md` §4).
  - *Task 2: Real-World Document Stress Testing* 🔄 — compatibility suite added (`SampleFilesCompatibilityTest`); must be green in CI before claiming.
  - *Task 3: OOXML Standards & SDK References* ✅ — ECMA-376 specifications, OpenXML SDK architecture.
  - *Task 4: OOXML Compatibility Layer* ✅ — WordprocessingML, SpreadsheetML, PresentationML Kotlin/Compose parsers and OMML/MathML equation pipelines.
  - *Task 5: Reverse Engineering & Behavioral Testing* 🔄 — planned; no verification artifacts in the repo yet.
- **Phase 4: Core Editing & UI Implementation (Material 3 Expressive)** 🔄 — FCT, Toolbar Hub, 40% Standard Bottom Sheet Ribbon, Touch Targets, Navigator Deck.
- **Phase 5: ARM Optimization** 🔜 — ARMv7 and ARM64-v8a neon optimizations, binary size minimization.
- **Phase 6: Low-End Device Testing** 🔄 — XLSX/PPTX streaming + archive budgets landed; on-device 2 GB profiling pending (see `docs/PHASE6_MEMORY_PLAN.md`).
- **Phase 7: PC-Level Office Features Rollout** 🔜 — Diagrams (Mermaid.js), Equations (KaTeX/MathML/OMML), Stylus Ink (Google Ink API), Citations/BibTeX, Full 500+ OpenFormula functions, and Optional Gemini AI Copilot.

---

## 🔗 8. Key Reference Links & Specifications
- **LibreOffice Core**: [gerrit.libreoffice.org](https://gerrit.libreoffice.org/) / [github.com/LibreOffice/core](https://github.com/LibreOffice/core)
- **Collabora Online**: [github.com/CollaboraOnline/online](https://github.com/CollaboraOnline/online)
- **ECMA-376 OOXML**: [ecma-international.org](https://ecma-international.org/publications-and-standards/standards/ecma-376/)
- **Microsoft OpenXML SDK**: [learn.microsoft.com](https://learn.microsoft.com/en-us/office/open-xml/open-xml-sdk) / [github.com/dotnet/Open-XML-SDK](https://github.com/dotnet/Open-XML-SDK)
- **OASIS OpenDocument (ODF v1.4)**: Authoritative specification in `/sources`
- **Google Ink API**: [developer.android.com/develop/ui/views/touch-and-input/stylus-input/about-ink-api](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/about-ink-api)
- **KaTeX / MathML**: [github.com/KaTeX/KaTeX](https://github.com/KaTeX/KaTeX)
- **Mermaid.js**: [github.com/mermaid-js/mermaid](https://github.com/mermaid-js/mermaid)
- **Material 3 Expressive**: [m3.material.io](https://m3.material.io/)

---

## ⚙️ 9. Engineering Guidelines for Future AI Agents

1. **ODF v1.4 Normative Rule**: When parsing, serializing, or modifying OpenDocument structures, always consult the standard files in `/sources`. Preserve existing XML nodes non-destructively so documents round-trip cleanly with LibreOffice desktop and Microsoft Office.
2. **Text Input & Undo Synchronization**: When modifying text editing or Undo/Redo logic, always ensure the active typing buffer is synchronously flushed to `UndoManager` prior to handling deletions, selections, or external undo actions to avoid debounce race conditions.
3. **Touch Targets & Accessibility**: Every interactive control, toolbar icon, menu item, and button MUST have a minimum touch target size of `48dp x 48dp` with meaningful `contentDescription`.
4. **Theme & Color Fidelity**:
   - Maintain module color separation: Base Suite Blue (`#2563EB`), Inky Green (`#0F9D58`), Cellina Teal (`#16A3B7`), Slidia Amber (`#F59E0B`), Pagella Red (`#D93025`).
   - Use dynamic Material You theming (`MaterialTheme.colorScheme`) on Android 12+ while supporting static module accents on Android 11 and below.
   - UI typography (`Google Sans`) is strictly separate from document body typography (which preserves the document's own embedded fonts and styles).
5. **Testing Verification**:
   - Execute local JVM tests via `gradle :app:testDebugUnitTest`.
   - Never attempt to launch emulators or run instrumented tests requiring `adb`.
   - Always run `compile_applet` before completing any modification turn.
