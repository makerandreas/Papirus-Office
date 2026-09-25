# Project Conventions & Specification References

## 📘 Papirus Office — Description Context & Architecture Summary

**Papirus Office** (originally conceptualized as **LibreDroid Office**) is an Android-first, modular, open-source office suite built around LibreOffice technologies and APIs where available. It treats ODF 1.4 as a first-class format and aims for practical OOXML compatibility. Actual parser, renderer, native-engine and save coverage is feature- and test-dependent; do not describe complete compatibility or full native integration without evidence. The UI direction is Material 3 Expressive with adaptive layouts for phones, foldables and tablets.

> *Note on Naming*: The project was initially conceived under the codename **LibreDroid Office**. To ensure trademark safety and prevent trademark conflicts with LibreOffice and The Document Foundation, the production name **Papirus Office** is adopted.

For complete deep architectural documentation, consult `PROJECT_CONTEXT.md` and `DESIGN.md`. For early concepts that shapes the app this day, see `CONCEPT.md`.

### Suite Modules & Color Conventions
Static Papirus accents (used as seeds/identifiers, not as replacements for semantic color roles):
- **Papirus (Base Suite)**: `#2563EB` (Primary Suite Blue — Start Center, File Manager, Universal Options)
- **Inky**: `#0F9D58` (Word Processing Green — Text documents, `.odt`, `.docx`, `.txt`, `.rtf`)
- **Cellina**: `#16A3B7` (Spreadsheets Cyan/Teal — Workbooks, formulas, `.ods`, `.xlsx`, `.csv`)
- **Slidia**: `#F59E0B` (Presentations Amber/Orange — Slide decks, `.odp`, `.pptx`)
- **Pagella**: `#D93025` (PDF Viewer Red — PDF viewing, document annotation, conversion)

---

## Binding Experience Direction

`DESIGN.md` is the design source of truth; `CONCEPT.md` defines the planned product surfaces and interaction terminology. The product uses Material 3 Expressive, with a bounded hybrid inspiration map: Google Workspace patterns for Start Screen tabs and editor dialogs; WPS Office for Welcome and Create New; Android Settings for Options, Crash Logs and About; M365 Copilot for editor screens; Microsoft Office 365 for the Inky/Cellina/Slidia command ribbon and SoftMaker FlexiPDF for Pagella; LibreOffice for office concepts and general layout, adapted to Android. These references are interaction precedents, not assets or pixel-copy targets.

- Android 12+ defaults to the Android system dynamic palette. Android 11 and below use Papirus static schemes. Custom colors and an app-generated system-wallpaper palette are separate optional modes; derive the latter from wallpaper colors (for example, `WallpaperManager.getWallpaperColors` where supported), not from the Material system scheme. If unavailable, offer a user-selected image or a clear static fallback.
- Google Sans is the intended UI family; retain Roboto/system sans-serif as a tested fallback. Do not confuse UI typography with the document's stored font identity.
- Material Symbols Rounded is the default icon style. Colibre in `app/src/main/share/config/images_colibre.zip` is a possible optional set pending asset and license review.
- In product copy and documentation, distinguish a design target from shipped behavior. Do not describe a simulated/native fallback as a fully integrated LibreOffice API, or imply full ODF/OOXML support without tests.

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
   - **Google Drive**: Placeholder only, not yet implemented. There is no OAuth, file listing, upload or download; the screen says so and a press raises a "this is a placeholder" notice rather than faking a connection.
   - **Filter Chips**: All, Inky (Writer), Cellina (Calc), Slidia (Impress), Pagella (PDF).
2. **Create New Screen**:
   - Template selection (Blank Document, Resume, Letter, Invoice, Report, Agenda) and direct module creation.
3. **About Screen**:
   - Versioning, LibreOfficeKit core engine attribution, Document Liberation Project credits, open-source licenses.
4. **Papirus Office Options (Settings)**:
   - General (user profile, autosave interval, default format), Inky View Settings (margins, non-printing characters), Load/Save, Appearance (system/static/custom/wallpaper palette modes are the target; verify current implementation), and Crash Logs.
5. **Editor Screen (Dual Mode: Viewer & Editor)**:
   - **Inky**: Word processing canvas, margins, rulers, continuous scroll, text layout.
   - **Cellina**: Spreadsheet grid, formula bar, cell coordinate indicator, sheets tab bar.
   - **Slidia**: Slide canvas, slide thumbnail rail, speaker notes, slideshow player.
   - **Pagella**: PDF continuous reader, thumbnail scrubber, zoom/fit-to-page, annotation layer.
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
Pre-built native `.so` libraries per ABI (`arm64-v8a`, `armeabi-v7a`) from the official LibreOffice Viewer for Android. Never assume a native capability without checking `LokitEngine.isNativeAvailable`.

### `sdk-references` and `app/src/main/sdk-examples`
When necessary, consult all SDK examples in `/sdk-references` and `app/src/main/sdk-examples` directory.

## Test Sample Files (`/tests`)
Files in `/tests` are reference sample files for analyzing, development references, regression testing, and compatibility verification across LibreOffice, Microsoft Word, and Papirus Office.

## Strings for localization
When necessary, translate all strings to `en_US` and add to `strings.xml`

## Notice on JNI
If JNI is available on the agent for unit tests, use it. Otherwise, use the GitHub API Approach instead.

## Handling `build.yml`
- Before creating a new build, execute a deletion of all old assets in the `nightly` tag, delete **all** old release with its tag (`gh release delete nightly --yes --cleanup-tag`) and forcing push tag `nightly` to active SHA commit (`${{ github-sha }}`)
- Add `target_commitish: ${{ github-sha }}` and `mske_latest: false` on the `Drop Papirus Nightly Release` step to freshly make new release with current timestamp, tag exactly pointed to current commit, and all old files are not retained again.
- Make sure to rewrite description of the releae by stritcly following `antislop.md` rules. 

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
