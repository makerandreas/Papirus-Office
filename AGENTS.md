# Project Conventions & Specification References

## 📘 Papirus Office — Description Context & Architecture Summary

**Papirus Office** (originally conceptualized as **LibreDroid Office**) is a modular, open-source office suite for Android based on the LibreOffice architecture, Document Liberation Project, and OASIS OpenDocument Format (ODF v1.4) standards. It delivers a PC-class document editing experience optimized ergonomically for mobile devices (smartphones, foldables, tablets) using **Material 3 Expressive** and Jetpack Compose.

> *Note on Naming*: The project was initially conceived under the codename **LibreDroid Office**. To ensure trademark safety and prevent trademark conflicts with LibreOffice and The Document Foundation, the production name **Papirus Office** is adopted.

For complete deep architectural documentation, consult `PROJECT_CONTEXT.md` and `DESIGN.md`.

### Suite Modules & Color Conventions
Default static accent colors for Android 11 and below (devices without dynamic color / Material You support):
- **Papirus (Base Suite)**: `#2563EB` (Primary Suite Blue — Start Center, File Manager, Universal Options)
- **Inky**: `#0F9D58` (Word Processing Green — Text documents, `.odt`, `.docx`, `.txt`, `.rtf`)
- **Cellina**: `#16A3B7` (Spreadsheets Cyan/Teal — Workbooks, formulas, `.ods`, `.xlsx`, `.csv`)
- **Slidia**: `#F59E0B` (Presentations Amber/Orange — Slide decks, `.odp`, `.pptx`)
- **Pagella**: `#D93025` (PDF Viewer Red — PDF viewing, document annotation, conversion)

---

## 🎛️ Key UI Terminologies & Ecosystem

### 1. FCT (Floating Contextual Toolbar)
A smart contextual menu (`com.example.ui.components.PapirusTextToolbar`) anchored dynamically above/below the active cursor or text selection in the document viewport:
- **FCT Compact**: Pill-shaped horizontal bar with quick actions (Cut, Copy, Paste, Delete, Select All, AI Options, More '...').
- **FCT Expanded**: 260dp card container with multi-category navigation:
  - *General Options*: Selection Mode, Character, Paragraph, Section Options, Bullets and Numbering, Skip/Remove Numbering, Restart from Beginning, Tab/Border/Shading Settings, Synonyms, Set Reminder.
  - *Selection Mode*: Select non-contiguous text, Block to select (rectangular marquee).
  - *Character Mode*: Character Style, Character Options.
  - *Paragraph Mode*: Paragraph Style, Paragraph Options.
  - *Synonyms Mode*: Contextual thesaurus/dictionary suggestions for selected words.
  - *AI Options Mode*: Gemini AI integration (Generate Text, Proofread, Translate, Tone Rewrite: *Lucu*, *Profesional*, *Akademis*, *Naratif*).

### 2. Toolbar Hub
A horizontally scrollable quick-action formatting bar docked immediately above the virtual keyboard (or viewport bottom in edit mode):
- **Scrollable Section**: Font family dropdown, Font size dropdown, Bold, Italic, Underline (long-press opens Underline Options), Strikethrough, Highlight Color, Font Color, Bulleted List, Numbered List, Indentation (Decrease/Increase), Insert Image, Insert Table, Insert Link, Insert Comment.
- **Persistent Trailing Actions**: Insert Tab character (`\t`), Toggle Soft Keyboard, and **Open Ribbon Button** (opens the Standard Bottom Sheet).

### 3. Standard Bottom Sheet (40% Screen Height Deck)
A persistent Material 3 Expressive bottom sheet with a 40% screen height constraint:
- **Ribbon Deck**: Tabbed desktop-class office ribbon with 8 standard tabs:
  - *File*: Save, Save As, Export PDF, Print, Share, Document Properties.
  - *Home*: Clipboard, Font formatting, Paragraph alignment/spacing, Styles gallery.
  - *Insert*: Image, Table, Shape, Page Break, Header/Footer, Bookmark, Hyperlink.
  - *Layout*: Margins, Page Orientation, Paper Size, Columns, Watermark.
  - *References*: Table of Contents, Footnotes, Endnotes, Citations.
  - *Mailings*: Mail merge fields, envelope and label formatting.
  - *Review*: Spellcheck, Word Count, Track Changes, Comment management.
  - *View*: Viewer vs Editor mode toggle, Zoom levels, Non-printing characters, Rulers.
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

1. **Start Screen (Start Center)**:
   - **Recents**: Chronological document list with preview thumbnail, timestamp, size, pinned status, and module badge.
   - **Files**: Device file system explorer, folder traversal, sorting, SAF system picker.
   - **Google Drive**: Cloud storage integration, account sync, online document access.
   - **Filter Chips**: All, Inky (Writer), Cellina (Calc), Slidia (Impress), Pagella (PDF).
2. **Create New Screen**:
   - Template selection (Blank Document, Resume, Letter, Invoice, Report, Agenda) and direct module creation.
3. **About Screen**:
   - Versioning, LibreOfficeKit core engine attribution, Document Liberation Project credits, open-source licenses.
4. **Papirus Office Options (Settings)**:
   - General (User profile, autosave interval, default format), Inky View Settings (margins, non-printing characters), Load/Save Subpage, Appearance (Dynamic Material You toggle vs static accents), Crash Logs Screen.
5. **Editor Screen (Dual Mode: Viewer & Editor)**:
   - **Inky**: Word processing canvas, margins, rulers, continuous scroll, text layout.
   - **Cellina**: Spreadsheet grid, formula bar, cell coordinate indicator, sheets tab bar.
   - **Slidia**: Slide canvas, slide thumbnail rail, speaker notes, slideshow player.
   - **Pagella**: PDF continuous reader, thumbnail scrubber, zoom/fit-to-page, annotation layer.
6. **Loading Screen**:
   - Application startup splash and document engine initialization progress.

---

## 🏛️ Papirus Engine & Architecture

- **Papirus Engine (`com.makerandreas.papirusoffice.data`)**:
  - Pure Kotlin / Compose parser and document model (`OfficeDocumentParser`, `DocxDocumentParser`, `SwDocEngine`, `LayoutEngine`, `TextLayoutManager`).
  - **ODF Import System (`data.odf`)**: Context-driven parser implementing `SvXMLImport`, `SvXMLImportContext`, and `OdfXmlToken` supporting `.odt`, `.ods` (multi-sheet tables with repeated columns/rows and values), and `.odp` (slides, frames, custom shapes, text boxes, and drawing groups).
  - **OpenXML / OOXML Engine**: Complete parsing for `.docx` (WordprocessingML), `.xlsx` (SpreadsheetML with sharedStrings and sheet mapping), and `.pptx` (PresentationML with slide titles, body placeholders, and slide counts).
- **LibreOfficeKit (LOKit) JNI Bridge**: Native C++ `.so` libraries in `/app/src/libs` providing desktop-class document rendering, complex layouts, OpenFormula evaluation, and PDF export.
- **DocumentSession & SessionManager**: Tracks active document lifecycle, file path, dirty flags (`isSaved`), autosave timers, and undo/redo stacks.
- **UndoManager & HistoryManager**: Dual-stack Command Pattern (`UndoAction`). Includes a synchronous typing buffer flusher (`flushPendingTyping`) before deletions and undo actions to prevent race conditions.
- **Modular Equation Pipeline**: LaTeX-style input via KaTeX/MathJax preview with bidirectional conversion to MathML (ODF) and OMML (OOXML).

---

## Reference Material 
### `/sources`
All document format specifications, standards, and schema definitions placed in `/sources` serve as the authoritative standard for document parsing, serializing, package handling, and rendering:
- **ODF v1.4 Standards**:
  - `Part 1: Introduction` (architecture, conformance, namespaces, references)
  - `Part 2: Packages` (ZIP container, `mimetype`, `META-INF/manifest.xml`, encryption, signatures)
  - `Part 3: OpenDocument Schema` (elements, styles, XML schema rules for text, spreadsheets, presentations)
  - `Part 4: Recalculated Formula (OpenFormula) Format` (OpenFormula expressions, syntax, evaluators)
- Whenever implementing or modifying parsers, serializers, or document processors in `com.makerandreas.papirusoffice`:
  1. Consult the relevant specification files in `/sources`.
  2. Adhere strictly to the normative rules (e.g., exact namespace definitions, element ordering, MIME header constraints, non-destructive package preservation).

### `/app/src/libs`
There are subdirectories for each architecture. Make sure to consult these subdirectories and its necessary `so` libraries if needed.

### `sdk-references`
When necessary, consult all SDK examples in `/sdk-references` directory.

## Test Sample Files (`/tests`)
Files in `/tests` are reference sample files for analyzing, development references, regression testing, and compatibility verification across LibreOffice, Microsoft Word, and Papirus Office.

## Strings for localization
When necessary, translate all strings to `en_US` and add to `strings.xml`

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
