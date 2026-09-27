# Project Conventions & Specification References

## 📘 Papirus Office: Description Context & Architecture Summary

**Papirus Office** (originally conceptualized as **LibreDroid Office**) is an Android-first, modular, open-source office suite built around LibreOffice technologies and APIs where available. It treats ODF 1.4 as a first-class format and aims for practical OOXML compatibility. Actual parser, renderer, native-engine and save coverage is feature- and test-dependent; do not describe complete compatibility or full native integration without evidence. The UI direction is Material 3 Expressive with adaptive layouts for phones, foldables and tablets.

> *Note on Naming*: The project was initially conceived under the codename **LibreDroid Office**. To ensure trademark safety and prevent trademark conflicts with LibreOffice and The Document Foundation, the production name **Papirus Office** is adopted.

For complete deep architectural documentation, consult `PROJECT_CONTEXT.md` and `DESIGN.md`. For early concepts that shapes the app this day, see `CONCEPT.md`.

### Suite Modules & Color Conventions
Static Papirus accents (used as seeds/identifiers, not as replacements for semantic color roles):
- **Papirus (Base Suite)**: `#2563EB` (Primary Suite Blue: Start Center, File Manager, Universal Options)
- **Inky**: `#0F9D58` (Word Processing Green: text documents, `.odt`, `.docx`, `.txt`, `.rtf`)
- **Cellina**: `#16A3B7` (Spreadsheets Cyan/Teal: workbooks, formulas, `.ods`, `.xlsx`, `.csv`)
- **Slidia**: `#F59E0B` (Presentations Amber/Orange: slide decks, `.odp`, `.pptx`)
- **Pagella**: `#D93025` (PDF Viewer Red: PDF viewing, document annotation, conversion)

---

## Binding Experience Direction

`DESIGN.md` is the design source of truth; `CONCEPT.md` defines the planned product surfaces and interaction terminology. The product uses Material 3 Expressive, with a bounded hybrid inspiration map: Google Workspace patterns for Start Screen tabs and editor dialogs; WPS Office for Welcome and Create New; Android Settings for Options, Crash Logs and About; M365 Copilot for editor screens; Microsoft Office 365 for the Inky/Cellina/Slidia command ribbon and SoftMaker FlexiPDF for Pagella; LibreOffice for office concepts and general layout, adapted to Android. These references are interaction precedents, not assets or pixel-copy targets.

- **Shipped theme (verified 2026-09-27, `ui/theme/Theme.kt`, `ThemeSettings`, `PapirusOfficeOptionsScreen.kt:699-776`)**: Android 12+ (API 31) defaults to the Android system dynamic palette, with a user switch to turn it off; Android 11 and below, or dynamic colour off, use the Papirus static light/dark schemes per workspace. The only other preference is theme mode: System, Light, Dark. **Target, not shipped**: Custom seed colours and an app-generated system-wallpaper palette (derive the latter from `WallpaperManager.getWallpaperColors` where supported, not from the Material system scheme, with a user-selected image or the static scheme as fallback).
- Google Sans is the intended UI family, not a validated shipped face. `ui/theme/Type.kt:15-50` requests static `google_sans_*` for display/headline/title/body/label and `google_sans_code_*` for code with `FontLoadingStrategy.OptionalLocal`; the bundled `google_sans_flex_*` files are unused. Read-only inspection found malformed `res/font/*.ttf` name tables and byte-identical files under different weight names (audit-009 §9.3); `README.txt`/`OFL.txt` do not establish provenance for those binaries. Keep system sans-serif as the intended fallback pending device and licence verification in a later code/asset plan. UI typography must not overwrite a document's saved font identity.
- **Icons**: the shipped dependency is `androidx.compose.material:material-icons-extended`, used mostly as `Icons.Rounded.*` but mixed with `Icons.Default.*`/Filled (e.g. `modules/cellina/CellinaModule.kt:569`, `modules/pagella/PagellaModule.kt:147`). Material Symbols Rounded is the target family and needs its own dependency or font before it can be claimed. Colibre in `app/src/main/share/config/images_colibre.zip` is a possible optional set pending asset and license review.
- **Material 3 Expressive is the target design system.** The build is on Compose BOM `2024.09.00` (material3 1.3.0), which has no `MaterialExpressiveTheme`, `MotionScheme`, emphasized type styles or the Expressive components; the BOM upgrade is a code-plan prerequisite (Plan 11), not a documentation claim.
- In product copy and documentation, distinguish a design target from shipped behavior. Do not describe a simulated/native fallback as a fully integrated LibreOffice API, or imply full ODF/OOXML support without tests.
- **Reference editions**: LibreOffice guides are cited from the 26.2 shelf (`https://books.libreoffice.org/en/`, Writer Guide Chapter 1 at `https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html`); ODF 1.4 from `docs/html`; OOXML from ECMA-376. Older guide editions in historical plan files are not updated retroactively.

## 🎛️ Key UI Terminologies & Ecosystem
> This section will be updated as the application develops, along with `PROJECT_CONTEXT.md`.

### 1. FCT (Floating Contextual Toolbar)
A smart contextual menu (`com.example.ui.components.PapirusTextToolbar`) anchored dynamically above/below the active cursor or text selection in the document viewport:
- **FCT Compact**: Pill-shaped horizontal bar with quick actions (Cut, Copy, Paste, Delete, Select All, AI Options, More '...').
- **FCT Expanded**: 260dp card container with multi-category navigation:
  - *General Options*: Selection Mode, Character, Paragraph, Section Options, Bullets and Numbering, Skip/Remove Numbering, Restart from Beginning, Tab/Border/Shading Settings, Synonyms, Set Reminder.
  - *Selection Mode*: Select non-contiguous text, Block to select (rectangular marquee).
  - *Character Mode*: Character Style, Character Options.
  - *Paragraph Mode*: Paragraph Style, Paragraph Options.
  - *Synonyms Mode*: Contextual thesaurus/dictionary suggestions for selected words.
  - *AI Options Mode*: Gemini AI integration (Generate Text, Proofread, Translate, Tone Rewrite: *Funny*, *Professional*, *Academic*, *Narrative*).

### 2. Toolbar Hub
A horizontally scrollable quick-action formatting bar docked immediately above the virtual keyboard (or viewport bottom in edit mode):
- **Scrollable Section**: Font family dropdown, Font size dropdown, Bold, Italic, Underline (long-press opens Underline Options), Strikethrough, Highlight Color, Font Color, Bulleted List, Numbered List, Indentation (Decrease/Increase), Insert Image, Insert Table, Insert Link, Insert Comment.
- **Persistent Trailing Actions**: Insert Tab character (`\t`), Toggle Soft Keyboard, and **Open Ribbon Button** (opens the Standard Bottom Sheet).

### 3. Standard Bottom Sheet (Adaptive Command Deck)
A Material 3 Expressive bottom-sheet command deck. It should start compact on phones and expand/scroll as content, keyboard, display size or accessibility settings require; 40% is a design reference point, not a hard height cap:
- **Ribbon Deck**: Tabbed desktop-class office ribbon with 6 standard tabs: File, Home, Insert, Layout, Review, View. References and Mailings are not part of the Writer set in `CONCEPT.md` and are not declared. Only File and Home own a deck today; the other four render in a disabled tone and say plainly that the deck is not in this build (plan-03 §0) instead of opening an empty page:
  - *File*: Save, Save As, Export PDF, Print, Share, Document Properties. (implemented)
  - *Home*: Clipboard, Font formatting, Paragraph alignment/spacing, Styles gallery. (implemented)
  - *Insert*: Image, Table, Shape, Page Break, Header/Footer, Bookmark, Hyperlink. (declared, not yet implemented)
  - *Layout*: Margins, Page Orientation, Paper Size, Columns, Watermark. (declared, not yet implemented)
  - *Review*: Spellcheck, Word Count, Track Changes, Comment management. (declared, not yet implemented)
  - *View*: Viewer vs Editor mode toggle, Zoom levels, Non-printing characters, Rulers. (declared, not yet implemented)
- **Navigator Deck**: Document outline tree displaying Headings, Tables, Frames, Images, Bookmarks, Sections, Hyperlinks, Comments, and Footnotes for instant jumping.
- **Navigate By Deck**: Quick navigation stepper to browse forward/backward through specific elements (Heading, Page, Table, Graphic, Bookmark).
- **Formatting Subpages & Dialogs**:
  - *Font Style*: Font family picker with typography preview.
  - *Font Size*: Numerical font size picker and stepper.
  - *Color Pickers*: Text Color, Highlight Color, Paragraph Shading Color, Underline Color.
  - *Underline Style*: Single, Double, Dotted, Dashed, Wave, and Underline Color.
  - *Bullet & Numbering Options*: Bullet shapes (Disc, Circle, Square, Arrow, Checkmark), Numbering styles (Decimal, Roman, Alphabetical, Multilevel).
  - *Border Settings*: Box, Top, Bottom, Left, Right, All, None, width, and color.
  - *Paragraph Styles*: Normal, Title, Subtitle, Headings, Custom Style Creator, Style Options.
  - *Actions to Undo / Redo*: Timeline list of recorded history entries for multi-step rollback/rollforward.
  - *Change Capitalization*: UPPERCASE, lowercase, Title Case, Sentence case, tOGGLE cASE.

---

## 📱 Application Screens
> This section will be updated as the application develops, along with `PROJECT_CONTEXT.md`.

1. **Start Screen (Start Center)**:
   - **Recents**: Chronological document list with preview thumbnail, timestamp, size, pinned status, and module badge.
   - **Files**: Device file system explorer, folder traversal, sorting, SAF system picker.
   - **Google Drive shipped (`HomeDashboard.kt:1210-1275`)**: no OAuth, listing, upload or download. The screen currently advertises cloud access and shows a "Connect Google Account" button that only raises a placeholder toast. **Target, Plan 11 home-entry package / Plan 3 copy backlog**: disclose the unavailable connection before the press.
   - **Filter Chips**: All, Inky (Writer), Cellina (Calc), Slidia (Impress), Pagella (PDF).
2. **Create New Screen** (`ui/home/NewDocumentScreen.kt`, opened by the Start Screen FAB; the WPS-inspired target lives in `DESIGN.md`):
   - **Shipped**: a two-tab pager. Tab 1 "Create New" holds three module cards (Inky Document, Cellina Spreadsheet, Slidia Presentation) that open the module on the bundled blank package `assets/templates/untitled.od{t,s,p}` with `MainActivity.pendingNewDocument = true`, and a "Create Pagella PDF Document" group with three rows: Create from Image (JPEG/PNG/WebP), Create from Camera (CAMERA permission) and Convert from Document (ODF/OOXML/legacy MS). Tab 2 "Create from Template" has All/ODT/ODS/ODP filter chips, a search field and a download list from `TemplateManager.searchTemplates`.
   - **Known deltas (code, owned by the Plan 11 home-entry surfaces package and the Plan 3 backlog, not by documentation)**: 9 of the 12 built-in `curatedTemplates` entries are third-party sample files from filesamples.com with invented names, not templates (R-38); the 112 real LibreOffice templates bundled under `assets/templates/` are not surfaced; the blank `untitled.*` packages are non-conformant and open on the Letter fallback (audit-008 §6); the in-editor "Create from Template" dialog injects a fabricated resume (`InkyModule.kt:1157`); two literals and per-type raw hex colours remain on the screen.
   - **First run only**: `WelcomeScreen` (one centered column with a logo tile and full-width "Get Started" button, no permission request) precedes the Start Screen when `papirus_first_run/is_first_run` is true.
3. **About Screen**:
   - Versioning, LibreOfficeKit core engine attribution, Document Liberation Project credits, open-source licenses.
4. **Papirus Office Options (Settings)**:
   - General (user profile, autosave interval, default format), Inky View Settings (margins, non-printing characters), Load/Save, Appearance (shipped: dynamic-colour switch plus System/Light/Dark; target: Custom and Wallpaper palette modes; the Appearance copy is still hard-coded rather than in `strings.xml`), and Crash Logs.
5. **Editor Screen (Dual Mode: Viewer & Editor)**:
   - **Inky**: Word processing canvas, margins, rulers, continuous scroll, text layout.
   - **Cellina**: Spreadsheet grid, formula bar, cell coordinate indicator, sheets tab bar.
   - **Slidia**: Slide canvas, slide thumbnail rail, speaker notes, slideshow player.
   - **Pagella shipped**: local PDF page rendering via Android `PdfRenderer`, previous/next and zoom buttons, in-memory ink overlay (`modules/pagella/PagellaModule.kt:85-120,132-198,202-299`). **Target, Plan 11 Pagella package**: continuous reading, thumbnail scrubber and persisted annotation/export. The no-file fallback text currently claims native PDF rendering without evidence (audit-009 §9.3).
6. **Loading Screen**:
   - Application startup splash and document engine initialization progress.

---

## 🏛️ Papirus Engine & Architecture
> This section will be updated as the application develops, along with `PROJECT_CONTEXT.md`.

- **Papirus Engine (`com.makerandreas.papirusoffice.data`)**:
  - Pure Kotlin / Compose parser and document model (`OfficeDocumentParser`, `DocxDocumentParser`, `SwDocEngine`, `LayoutEngine`, `TextLayoutManager`).
  - **ODF Import System (`data.odf`)**: Context-driven parser paths use `SvXMLImport`, `SvXMLImportContext`, and `OdfXmlToken` across `.odt`, `.ods` and `.odp`; coverage is partial and is checked against ODF 1.4 and repository fixtures.
  - **OpenXML / OOXML Engine**: Kotlin parser paths cover selected `.docx`, `.xlsx` and `.pptx` structures; do not call parsing complete. Writer fidelity plans specify remaining style, numbering, field, table, section, relationship and package work.
- **LibreOfficeKit (LOKit) JNI Bridge**: native `.so` libraries shipped under `app/src/main/libs/<abi>/` (`liblo-native-code.so` + NSS chain, from LibreOffice Viewer for Android). `LibreOfficeCore` probes them at startup; `LokitEngine` reports NATIVE vs SIMULATED mode and falls back to the pure-Kotlin engine when absent.
- **DocumentSession & SessionManager**: Tracks active document lifecycle, file path, dirty flags (`isSaved`), autosave timers, and undo/redo stacks.
- **UndoManager & HistoryManager**: Dual-stack Command Pattern (`UndoAction`). Includes `PendingTypingBuffer`, which owns the debounce → baseline-commit protocol and the flush-then-delete sequence (surfaced via `flushPendingTyping`) before deletions and undo actions to prevent race conditions.
- **Modular Equation Pipeline**: `EquationParser` converts selected LaTeX-style input to MathML/OMML representations. Writer-side embedding and round-trip support are not yet established; rendered KaTeX/MathJax preview is planned.

---

## Reference Material 
### `/docs/html`
All document format specifications, standards, and schema definitions placed in `/docs/html` serve as the authoritative standard for document parsing, serializing, package handling, and rendering:
- **ODF v1.4 Standards**:
  - `Part 1: Introduction` (architecture, conformance, namespaces, references)
  - `Part 2: Packages` (ZIP container, `mimetype`, `META-INF/manifest.xml`, encryption, signatures)
  - `Part 3: OpenDocument Schema` (elements, styles, XML schema rules for text, spreadsheets, presentations)
  - `Part 4: Recalculated Formula (OpenFormula) Format` (OpenFormula expressions, syntax, evaluators)
- Whenever implementing or modifying parsers, serializers, or document processors in `com.makerandreas.papirusoffice`:
  1. Consult the checked-in ODF 1.4 specification files in `docs/html` and ECMA-376 for OOXML.
  2. Follow the relevant normative rules (namespaces, package parts/relationships, element constraints and MIME requirements); preserve content where supported and verify round trips rather than assuming lossless behavior.

### `app/src/main/libs`
Pre-built native `.so` libraries per ABI (`arm64-v8a`, `armeabi-v7a`) from the official LibreOffice Viewer for Android, tracked with Git LFS: the pointer for `liblo-native-code.so` declares 196,227,296 bytes (arm64-v8a) and 134,699,252 bytes (armeabi-v7a). An agent sandbox without LFS sees pointer files of about 130 bytes, so do not report library sizes or "stub" status from such a checkout; CI checks out with `lfs: true` and publishes `scripts/native-inventory.sh` output as the evidence. Never assume a native capability without checking `LokitEngine.isNativeAvailable`; `LokitEngine.statusLabel` reports SIMULATED when the library does not load.

### `sdk-references` and `app/src/main/sdk-examples`
When necessary, consult all SDK examples in `/sdk-references` and `app/src/main/sdk-examples` directory.

## Test Sample Files (`/tests`)
Files in `/tests` are reference sample files for analyzing, development references, regression testing, and compatibility verification across LibreOffice, Microsoft Word, and Papirus Office.

## Strings for localization
When necessary, translate all strings to `en_US` and add to `strings.xml`

## Notice on JNI
If JNI is available on the agent for unit tests, use it. Otherwise, use the GitHub API Approach instead.

## Handling `build.yml`
- Before creating a new build, delete all old assets in the `nightly` tag, delete **all** old releases with their tag (`gh release delete nightly --yes --cleanup-tag`) and force-push the `nightly` tag to the active commit (`${{ github.sha }}`).
- Add `target_commitish: ${{ github.sha }}` and `make_latest: false` on the `Drop Papirus Nightly Release` step so each release is fresh: current timestamp, tag pointed exactly at the current commit, no old files retained.
- Write the release description by strictly following `antislop.md` rules (no em dash, no fabricated claims, real changes only).

## antislop project addendum (Papirus-specific, keeps `antislop.md` as the untouched upstream core)
1. **Target versus shipped.** Every statement about the UI in `AGENTS.md`, `DESIGN.md`, `PROJECT_CONTEXT.md`, release notes and PR bodies is labelled either *shipped* (with `file:line`, a test name or a device screenshot) or *target* (with the plan that owns the code change). An unlabelled UI claim is a defect (R-36, C-5).
2. **References are patterns, not skins.** The app-to-app map in `DESIGN.md` (Google Workspace, WPS Office, M365 Copilot, Office 365, FlexiPDF, LibreOffice) is bounded by surface and borrows task-level interaction patterns only; no screen may read as a clone of one of them (R-30), and no reference implies Papirus has that product's features.
3. **Documentation-only deliverables run a reduced Delivery Gate**: R-02, R-15, R-16, R-17, R-36, R-38 and C-5 with evidence; R-26, R-27, R-32, R-34 and R-35 are reported as N/A with the reason "no UI shipped in this deliverable". UI and code deliverables run the full gate.
4. **Editions are pinned in `DESIGN.md` front matter** (design sources by filename, LibreOffice 26.2 guides, ODF 1.4 in `docs/html`, ECMA-376). A citation to another edition is a doc bug unless the file is a dated historical plan or audit.

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
Before starting, ask the user when antislop applies: during the work, or after it is done.
<!-- antislop:end -->
