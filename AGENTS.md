# Project Conventions & Specification References

> This file serves as the “brain” of the project, providing important context regarding what instructions you must follow. The content of this file can be updated **Only** on my behalf.

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
- **Reference editions**: LibreOffice guides are cited from the 26.2 shelf (`https://books.libreoffice.org/en/`, Writer Guide Chapter 1 at `https://books.libreoffice.org/en/WG262/WG26201-IntroducingWriter.html`); ODF 1.4 from `docs/odf`; OOXML from ECMA-376, with the Microsoft Open Specifications implementation notes in `docs/ooxml` (page-indexed in its `README.md`). Older guide editions in historical plan files are not updated retroactively.

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
   - **Shipped**: a two-tab pager. Tab 1 "Create New" holds three module cards (Inky Document, Cellina Spreadsheet, Slidia Presentation) that open the module on the bundled blank package `assets/templates/Untitled.od{t,s,p}` with `MainActivity.pendingNewDocument = true`, and a "Create Pagella PDF Document" group with three rows: Create from Image (JPEG/PNG/WebP), Create from Camera (CAMERA permission) and Convert from Document (ODF/OOXML/legacy MS). Tab 2 "Create from Template" has All/ODT/ODS/ODP filter chips, a search field and a download list from `TemplateManager.searchTemplates`.
   - **Known deltas (code, owned by the Plan 11 home-entry surfaces package and the Plan 3 backlog, not by documentation)**: 9 of the 12 built-in `curatedTemplates` entries are third-party sample files from filesamples.com with invented names, not templates (R-38); the 112 real LibreOffice templates bundled under `assets/templates/` are not surfaced; the blank assets are `Untitled.odt`, `Untitled.ods` and `Untitled.odp` (the extracted cache filenames are lowercase); `TemplateManager` returns `null` if an asset cannot be extracted; the in-editor "Create from Template" dialog injects a fabricated resume (`InkyModule.kt:1157`); two literals and per-type raw hex colours remain on the screen.
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
  - Kotlin parser and document model (`OfficeDocumentParser`, `DocxDocumentParser`, `SwDocEngine`). `LayoutEngine` paginates through `ParagraphMeasurer` and the injected `TextMetrics` backend; `TextLayoutManager` is used only by the debug formatting inspector (`InkyModule.kt:3897`).
  - **ODF Import System (`data.odf`)**: Context-driven parser paths use `SvXMLImport`, `SvXMLImportContext`, and `OdfXmlToken` across `.odt`, `.ods` and `.odp`; coverage is partial and is checked against ODF 1.4 and repository fixtures.
  - **OpenXML / OOXML Engine**: Kotlin parser paths cover selected `.docx`, `.xlsx` and `.pptx` structures; do not call parsing complete. Writer fidelity plans specify remaining style, numbering, field, table, section, relationship and package work.
- **LibreOfficeKit (LOKit) JNI Bridge**: native `.so` libraries are shipped under `app/src/main/libs/<abi>/` (`liblo-native-code.so` + NSS chain, from LibreOffice Viewer for Android). `LibreOfficeCore` probes them at startup; `LokitEngine` reports NATIVE vs SIMULATED mode and falls back to the Kotlin engine when loading fails. The probe and bundled libraries do not establish that document rendering uses LOKit; the current pagination path is Kotlin (`LayoutEngine`).
- **DocumentSession & SessionManager**: Tracks active document lifecycle, file path, dirty flags (`isSaved`), autosave timers, and undo/redo stacks.
- **UndoManager & HistoryManager**: Dual-stack Command Pattern (`UndoAction`). Includes `PendingTypingBuffer`, which owns the debounce → baseline-commit protocol and the flush-then-delete sequence (surfaced via `flushPendingTyping`) before deletions and undo actions to prevent race conditions.
- **Modular Equation Pipeline**: `EquationParser` converts selected LaTeX-style input to MathML/OMML representations. Writer-side embedding and round-trip support are not yet established; rendered KaTeX/MathJax preview is planned.

---

## Reference Material 
### `/docs/odf`
All document format specifications, standards, and schema definitions placed in `/docs/odf` serve as the authoritative standard for document parsing, serializing, package handling, and rendering:
- **ODF v1.4 Standards**:
  - `Part 1: Introduction` (architecture, conformance, namespaces, references)
  - `Part 2: Packages` (ZIP container, `mimetype`, `META-INF/manifest.xml`, encryption, signatures)
  - `Part 3: OpenDocument Schema` (elements, styles, XML schema rules for text, spreadsheets, presentations)
  - `Part 4: Recalculated Formula (OpenFormula) Format` (OpenFormula expressions, syntax, evaluators)
- Whenever implementing or modifying parsers, serializers, or document processors in `com.makerandreas.papirusoffice`:
  1. Consult the checked-in ODF 1.4 specification files in `docs/odf` and ECMA-376 for OOXML.
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
If JNI is available on the agent for unit tests, use it. Otherwise, use the GitHub API Approach instead: the CI report comment on the pull request, read as described in "Reading the CI report" below.

### Local toolchain check (2026-10-04, audit-016 §8)
A JDK is not obtainable in the agent sandbox: `java`, `javac` and `/usr/lib/jvm` are absent, the Debian mirrors and every JDK vendor host are unreachable, and the one PyPI reachable JDK package (`jdk4py 25.0.2.1`) ships a runtime without `javac`. The Gradle distribution host, Maven Central, Google Maven and JitPack are unreachable as well and no `~/.gradle` cache exists, so `./gradlew testDebugUnitTest` cannot run locally even with a JDK. GitHub Actions is the compile and test evidence path; Google AI Studio runs the same suite locally for the owner. Statements derived only from source reading must say so instead of reporting a build or test result.

### The three-tier evidence ladder (owner's arrangement, 2026-10-07)
The owner has confirmed that the agent sandbox missing the Android suite is acceptable, because the tier above it covers what the sandbox cannot:

1. **Agent sandbox.** Source reading, `gh` against `api.github.com` (the CI report, see below), and a standalone `kotlinc` type-check of dependency-light engine files. Cannot reach Compose, Robolectric, JUnit, the Android SDK or Gradle.
2. **Google AI Studio, the owner's build environment.** Runs the real `testDebugUnitTest` suite, and has an Android cloud device emulator for UI-level checks. This is where a test count, a Robolectric result or an interaction claim gets verified when GitHub Actions is not enough.
3. **GitHub Actions.** The compile and test record that is attached to a commit and citable by run id, and the only evidence an audit can point at later.
4. **The owner's Realme C3.** Scheduled after Plan 11. The only source of visual, gesture and IME evidence, and the reason the fixture-independence rule in audit-019 section 4.2 exists.

Do not report a UI behaviour, a Robolectric result or a device measurement as verified when the evidence came from tier 1. Say which tier the claim came from.

### A type-check is still possible without Gradle (2026-10-07, audit-020 §7)
The paragraph above is right about `./gradlew` and wrong to read as "nothing here can compile". Two allowlisted hosts give a working Kotlin compiler:

```bash
pip install --target /tmp/jdkpkg jdk4py       # Temurin 25.0.2 JRE; no javac, and kotlinc does not need one
npm pack kotlin-compiler@2.4.20               # full Kotlin compiler; the GitHub release asset redirects off the allowlist, the npm mirror does not
tar xzf kotlin-compiler-2.4.20.tgz
export JAVA_HOME=/tmp/jdkpkg/jdk4py/java-runtime
/tmp/npx/package/bin/kotlinc-jvm -nowarn <sources> -d /tmp/out
```

Use it for engine files whose only external dependency is `org.xmlpull.v1` (a 53-line signature-faithful stub covers `DocxNumbering.kt`: `name`, `eventType`, `attributeCount` as `val`s so Kotlin's synthetic-property view of the Java getters holds, the rest as functions). It is the cheapest way to disprove an `Unresolved reference` before spending a CI run. Always pair it with a negative control that reintroduces the reported error, so the check is shown to be sensitive. It cannot reach Compose, Robolectric, JUnit or the Android SDK, so UI files, `OfficeDocumentParser.kt` and the Robolectric suites still belong to CI.

## Reading the CI report (the GitHub API approach, in full)

The Unit Tests job ends with a `Post CI report to the pull request` step. On
every `pull_request` run it writes one comment to the pull request, generated by
`scripts/ci-dump-comment.py`, that mirrors what the job log shows. **That comment
is readable from the agent sandbox and it is the compile and test evidence.**
It is not a fallback of last resort: read it before writing a fix, and read it
again after the next run rather than assuming the fix landed.

What the sandbox can and cannot reach, re-verified 2026-10-07 against PR #35 run
`37623177005`:

| Source | Reachable | How |
|---|---|---|
| Pull-request comments (the CI report) | **yes** | `gh api repos/makerandreas/Papirus-Office/issues/<PR>/comments` |
| Run and job metadata, step conclusions | **yes** | `gh api .../actions/runs/<id>` and `.../actions/runs/<id>/jobs` |
| Commit-to-run mapping for a branch | **yes** | `gh run list --branch <branch>` (`head_sha` is the real PR head) |
| Check-run annotations | yes, but thin | `gh api .../check-runs/<id>/annotations` carries `Process completed with exit code 1.`, not the compiler output |
| Artifact contents (`unit-test-reports`) | **no** | `gh run download` fails with `EOF` from `productionresultssa*.blob.core.windows.net`; only the artifact's name and size come back from `api.github.com` |
| Raw job logs | **no** | `gh run view --log-failed` fails with `EOF` from `results-receiver.actions.githubusercontent.com` |

### Recipe

```bash
# 1. The reports, newest last. Each starts "### CI report for `<sha>` (run <id>)".
gh api repos/makerandreas/Papirus-Office/issues/<PR>/comments --paginate \
  --jq '.[] | .body' > /tmp/ci-reports.md

# 2. Which commit each report actually belongs to.
gh run list --branch <head-branch> --limit 30 \
  --json databaseId,headSha,status,conclusion,createdAt

# 3. Which step failed, when the report only says the build did.
gh api repos/makerandreas/Papirus-Office/actions/runs/<id>/jobs \
  --jq '.jobs[] | {name, conclusion, steps: [.steps[] | select(.conclusion != "success" and .conclusion != "skipped") | .name]}'
```

### The commit in the report header

Before 2026-10-07 the header named `GITHUB_SHA`. On a `pull_request` event that
is the ephemeral `refs/pull/<N>/merge` commit GitHub builds for the run ("Merge
`<head>` into `<base>`"); it is on no branch, `git show <sha>` cannot resolve it
in a normal clone, and it does not appear in `gh pr view --json commits`. Every
one of the twelve PR #35 reports was labelled that way, which is why the failure
looked like twelve attempts at commits that did not exist and why no report
could be lined up with the code it described. From the Plan 8B CI-triage commit
onward the workflow passes `HEAD_SHA`/`HEAD_REF`/`BASE_SHA`/`BASE_REF` to the
script, the header names the pull-request head, and the next line states the
merge the run used. Reports posted before that commit still show a merge-ref
SHA: map those through `gh run list --branch` or
`gh api .../actions/runs/<id>` (`head_sha`) instead of trusting the header.

### What is in the report

* **Header line 1**: head SHA and run link.
* **Header line 2** (new): head SHA, head branch, base SHA.
* **Header line 3**: `Unit tests: N run, F failed, E errors, S skipped (Xs across N suites)` when test XML was written, otherwise `No test results were written`, which means the build died before tests and the cause is in the sections below.
* **Kotlin compile errors**: every `e:` line, repo-relative paths (`app/src/...`), duplicates collapsed. `:app:compileDebugKotlin` is main source; `:app:compileDebugUnitTestKotlin` is test source.
* **Failed tasks** and the **Gradle failure block**: which task died and the `FAILURE:` text.
* **Failing tests**: class, test name, message and stack trace (first 4000 characters).
* **Per-class results**: the suite table with per-suite JUnit seconds.
* **Plan 6C timings** and the **Plan 5 element dump**: `<system-out>` of `Plan6cLoadingProgressTest` and `Plan5ElementDumpTest`.
* **Native inventory**: `liblo-native-code.so` sizes, ELF header, NEEDED chain and the JNI seam check. The full file is only in the artifact, which the sandbox cannot open.

The comment is capped at 60,000 characters and says so when it truncates.

### Rules that follow from this

1. Never report a build or test result that a CI report does not show. Say "not verified here" instead (R-36, C-5).
2. Read the newest report for the newest head SHA. `git rev-parse HEAD` and the report's SHA must agree before a failure is attributed to the current tree.
3. One `Unresolved reference` is one line of code. Fix the line the report names; do not add an import that looks plausible. On PR #35, `import java.nio.charset.Charsets` was deleted as a fix at `ef2945b` and re-added as a "fix" at `764a598`, costing four more red runs; `import com.makerandreas.papirusoffice.data.LayoutUnits` was added at `6c30c38` for an error no report ever showed.
4. A push to `main` produces no report, because the step is gated on `github.event_name == 'pull_request'`. Post-push evidence needs a pull request.

## Notice on Navigator
Navigator Deck: note that Sections and Indexes are readable for ODT **(Plan 7C)**, DOCX follows in Plan 8, and FCT Compact can show **"Go to entry…"** on linked TOC entries.

## Online Resources
For other online resources, check the `CONCEPT.md` file on the **Links Used** section. All handful resources are available there for inspiration.

## Handling `build.yml`
- Before creating a new build, delete all old assets in the `nightly` tag, delete **all** old releases with their tag (`gh release delete nightly --yes --cleanup-tag`) and force-push the `nightly` tag to the active commit (`${{ github.sha }}`).
- Add `target_commitish: ${{ github.sha }}` and `make_latest: false` on the `Drop Papirus Nightly Release` step so each release is fresh: current timestamp, tag pointed exactly at the current commit, no old files retained.
- Write the release description by strictly following `antislop.md` rules (no em dash, no fabricated claims, real changes only).

## antislop project addendum (Papirus-specific, keeps `antislop.md` as the untouched upstream core)
1. **Target versus shipped.** Every statement about the UI in `AGENTS.md`, `DESIGN.md`, `PROJECT_CONTEXT.md`, release notes and PR bodies is labelled either *shipped* (with `file:line`, a test name or a device screenshot) or *target* (with the plan that owns the code change). An unlabelled UI claim is a defect (R-36, C-5).
2. **References are patterns, not skins.** The app-to-app map in `DESIGN.md` (Google Workspace, WPS Office, M365 Copilot, Office 365, FlexiPDF, LibreOffice) is bounded by surface and borrows task-level interaction patterns only; no screen may read as a clone of one of them (R-30), and no reference implies Papirus has that product's features.
3. **Documentation-only deliverables run a reduced Delivery Gate**: R-02, R-15, R-16, R-17, R-36, R-38 and C-5 with evidence; R-26, R-27, R-32, R-34 and R-35 are reported as N/A with the reason "no UI shipped in this deliverable". UI and code deliverables run the full gate.
4. **Editions are pinned in `DESIGN.md` front matter** (design sources by filename, LibreOffice 26.2 guides, ODF 1.4 in `docs/odf`, ECMA-376). A citation to another edition is a doc bug unless the file is a dated historical plan or audit.

<!-- antislop:start -->
## antislop
For UI, copy, people, mobile layout, or code comments work, read `antislop.md` (core) and then the skill for the task:
- UI / visual: `skills/antislop-ui/SKILL.md`
- Copy & text: `skills/antislop-copywriting/SKILL.md`
- People: `skills/antislop-human/SKILL.md`
- Mobile / responsive: `skills/antislop-layoutmobile/SKILL.md`
- Code comments: `skills/antislop-code/SKILL.md`
Before starting, follow the core's "Two Usage Modes" section in strict order: explicit session instruction first, then global preference, then ask. A session instruction always wins. For a resolved mode, say `antislop active: <mode> (session override).` or `antislop active: <mode> (global preference).` once before presenting findings or making edits, using the actual mode and source. Acknowledging the user's request without naming the source does not replace this notice.
Only an explicit choice of antislop during or after selects a session mode. A request to review, audit, or avoid file edits does not select a mode; read the global preference in that case. Another skill's mode does not select antislop's mode.
If the mode is unresolved, ask during/after and end the response; wait for the answer before any UI review, planning, or concept. For read-only tasks, put the active-mode notice only at the start of the final answer, never in progress messages. For editing tasks, announce before the first edit and omit it from the final answer.
To update antislop later: download `antislop.md` again, or run `npx antislop-ai --update` if it was installed as skill folders.
<!-- antislop:end -->

## Provisions for providing reports, answers and/or output
Use `/ELI10` *("explain like I'm 10)* protocol because this is my first time I'm doing vibe coding development like this, so I can easily understand about the context of your output with a very basic, understandable explanation.
