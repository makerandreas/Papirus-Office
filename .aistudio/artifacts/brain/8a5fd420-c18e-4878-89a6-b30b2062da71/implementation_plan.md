# Implementation Plan: ODT Verification Probe & Plan 9B DOCX Save Round-Trip Integrity

Implement verification probes for Plan 9 ODT output and complete Plan 9B to achieve true round-trip document fidelity for Word (.docx) documents, eliminating data loss during save operations while preventing reader/writer echo-chamber testing bias.

## User Review & Critical Decisions

> [!IMPORTANT]
> Key architectural decisions confirmed through interactive clarification:
> - **Execution Priority**: Execute the ODT XML dump probe (`Plan9DumpXmlTest`) first to inspect generated ODF XML, then proceed directly with Plan 9B DOCX implementation.
> - **Bias Mitigation Strategy**: Supplement internal parser round-trip tests with direct structural XML assertions (inspecting `word/document.xml`, `w:pgSz`, `w:r`, `w:pStyle`, and relationship files directly from ZIP streams) to ensure independent format compliance.
> - **Commit Cadence**: Execute the 7 commits of Plan 9B sequentially, gating each commit with unit test runs and compilation checks before proceeding.

- **Confirmed Decision 1**: All round-trip tests will perform two-level validation: (a) re-reading via the internal parser for application-level fidelity, and (b) direct structural XML / regex inspection of the raw unzipped parts to eliminate blind spots.
- **Confirmed Decision 2**: Preservation of source section properties (`w:sectPr`) will be prioritized over arbitrary defaults to respect user decision D4 (A4 remains A4, margins and headers/footers preserved).
- **Confirmed Decision 3**: Complete retirement of `OfficeDocElement` (F-2) will be guarded by codebase-wide reference audits to prevent silent element drops during save.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. Validates the generated ODT XML structure from Plan 9 using a local probe test to verify XML correctness independent of the internal reader.
  2. Implements `DocxDocumentWriter`, a dedicated document serializer for DOCX files conforming to `DocumentFormatWriter`.
  3. Stops data loss in DOCX saves: preserves authored page geometry (`w:sectPr`), author-level formatting runs (`OfficeTextRun` with bold, italic, underline, color, size, vanish), style linkage (`w:pStyle`, `w:rStyle`), tables with grid definitions (`w:tblGrid`, `w:gridSpan`, `w:vMerge`), numbering definitions (`w:numPr`), and embedded media files (`word/media/*`).
  4. Guarantees that "Save As" to a new file produces a valid, complete OpenXML package with all required parts (`[Content_Types].xml`, `word/styles.xml`, `word/numbering.xml`).

- **Target Audience / Persona**:
  Users creating, opening, editing, and saving `.docx` and `.odt` office documents on Android devices (phones, foldables, tablets) who expect byte-level styling preservation and zero unintended document reflow or formatting loss.

- **Key Value**:
  True round-trip save fidelity without data loss or layout corruption, respecting source documents' styles, page sizes, and media.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  - *Open & Save Existing Document*: User opens an existing `.docx` document, makes text or styling edits, and taps Save. The app preserves the original page size (e.g. A4), original style definitions, untouched images, and numbering schemes.
  - *Save As / Export*: User saves an existing or new document to a new file location. The application writes a fully conformant OpenXML container containing all essential parts rather than an orphaned partial document.
  - *Error Prevention*: The application safely handles embedded media without dropping images or throwing unhandled exceptions.

- **Visual Identity & Theme**:
  - *Engine Domain*: This task operates purely on the data serialization, XML generation, and test validation layer of Papirus Office (`com.makerandreas.papirusoffice.data`).
  - *User-Facing Stability*: Prevents visual jumps, unexpected page reflows, font resetting to defaults, or missing illustrations when viewing saved documents in external office suites (e.g., LibreOffice, Microsoft Word) or reloading inside Papirus Office.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Independent Structural XML Assertions vs Pure Parser Round-Trip**
  - *Chosen Approach*: Assert raw XML structure inside the ZIP archive (`word/document.xml`, `word/styles.xml`) directly using string/regex and XML inspection alongside `DocxDocumentParser` parsing.
  - *Why*: Mitigates the "grading your own essay" problem where writer and reader share identical flaws.
  - *Trade-Off*: Requires maintaining schema assertions alongside high-level model assertions.

- **Decision 2: Dedicated `DocxDocumentWriter` Class vs Extending `DocxDocumentParser`**
  - *Chosen Approach*: Extract all saving logic into a dedicated `DocxDocumentWriter` implementing `DocumentFormatWriter` (matching the pattern used by `OdtDocumentWriter`).
  - *Why*: Decouples reading from writing, enforces modularity, simplifies testing, and conforms to Clean Architecture.

- **Decision 3: Non-Destructive Package Merging for Existing Files**
  - *Chosen Approach*: When overwriting an existing file, copy all unmodified parts (`word/styles.xml`, `word/numbering.xml`, `word/media/*`, `docProps/*`) byte-for-byte and only replace or update `word/document.xml`, `[Content_Types].xml`, and relationships as needed.
  - *Why*: Preserves Microsoft Word proprietary features, themes, font tables, and document metadata that Papirus Office does not yet model.

- **Decision 4: Append-Only Style Allocation**
  - *Chosen Approach*: Use collision-safe name allocation (`StyleNameAllocator` or equivalent) to append newly created styles without overriding existing document styles.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Papirus Office Data Layer                       │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
               ┌───────────────────┴───────────────────┐
               ▼                                       ▼
  ┌─────────────────────────┐             ┌─────────────────────────┐
  │   OdtDocumentWriter     │             │   DocxDocumentWriter    │
  │   (Plan 9 - Verified)   │             │       (Plan 9B)         │
  └────────────┬────────────┘             └────────────┬────────────┘
               │                                       │
               │ (writes ODF 1.4)                      │ (writes OpenXML)
               ▼                                       ▼
  ┌─────────────────────────┐             ┌─────────────────────────┐
  │   content.xml + styles  │             │   word/document.xml     │
  │   META-INF/manifest.xml │             │   word/styles.xml       │
  │   [ODT ZIP Package]     │             │   [DOCX ZIP Package]    │
  └────────────┬────────────┘             └────────────┬────────────┘
               │                                       │
               └───────────────────┬───────────────────┘
                                   │
                                   ▼
  ┌─────────────────────────────────────────────────────────────────────┐
  │                        Validation Gateways                          │
  ├──────────────────────────────────┬──────────────────────────────────┤
  │    Internal Parser Re-read       │    Direct Structural XML Probe   │
  │    (Application-level check)     │    (Schema & byte-level check)   │
  │    OfficeDocumentParser.parse()  │    ZipInputStream + regex / DOM  │
  └──────────────────────────────────┴──────────────────────────────────┘
```

### Execution Phases & Commit Milestones

#### Phase 0: ODT Verification Probe
- Run Task 2 probe (`Plan9DumpXmlTest`) to dump and inspect generated ODT `content.xml`.
- Verify full test suite baseline (`456 tests, 0 failed`) and compiler warning status via Gradle.

#### Phase 1: Pre-Change Gate (Commit 1)
- Add baseline DOCX tests in `Plan9RoundTripTest.kt` targeting `Sample-3.docx` (which has 0 media):
  - Record run count loss (asserting current drop of 436 runs).
  - Record page size discrepancy (asserting current US Letter vs source A4).
  - Record missing styles on Save-As to new paths.
  - Assert image refusal remains active for fixtures with media.
  - Add structural XML check inspecting raw `word/document.xml` directly.

#### Phase 2: Page Geometry & Sections (Commit 2)
- Implement `DocxDocumentWriter` with section properties generation.
- Read source `w:sectPr` (`w:pgSz`, `w:pgMar`, headers/footers) from `OfficeDocument` metadata/styles and write exact twips values.
- Verify Sample-3.docx page size equals A4 (`11906 x 16838`) in raw XML and re-opened document.

#### Phase 3: Run-Level Formatting (Commit 3)
- Map `OfficeTextRun` to `w:r` with `<w:rPr>`.
- Support tri-state formatting (`w:b`, `w:i`, `w:u`, `w:color`, `w:highlight`, `w:vanish`, `w:sz`).
- Ensure direct formatting overrides style defaults without leaking paragraph properties.
- Verify 436 runs preserved in Sample-3.docx.

#### Phase 4: Styles & Package Creation (Commit 4)
- Handle "Save As" to a new file by generating complete package parts (`word/styles.xml`, `[Content_Types].xml`, `_rels/.rels`).
- Implement append-only style naming for new styles.
- Verify all `w:pStyle` and `w:rStyle` references resolve to declared styles.

#### Phase 5: Tables & Numbering (Commit 5)
- Emit `w:tblGrid` on every table, with cell column spans (`w:gridSpan`) and row spans (`w:vMerge`).
- Emit `w:numPr` (`w:ilvl` and `w:numId`) linked to `numbering.xml`.
- Validate against synthetic documents with merged cells and list items.

#### Phase 6: Media Preservation (Commit 6)
- Package embedded images into `word/media/*`.
- Register media parts in `[Content_Types].xml` and relationships in `word/_rels/document.xml.rels`.
- Remove the `containsEmbeddedImages` save refusal guard in `DocxDocumentParser`.
- Assert all 6 DOCX fixtures round-trip successfully with images preserved.

#### Phase 7: Cleanup & Wrapper Retirement (Commit 7)
- Audit all references to `OfficeDocElement` across the entire project via grep.
- Retire `generateDocxXmlFromElements` and `OfficeDocElement` sealed class (closing F-2).
- Verify compilation and run full test suite.
