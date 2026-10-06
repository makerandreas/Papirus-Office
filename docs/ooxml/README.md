# `docs/ooxml`: the OOXML reference shelf

This directory holds the Microsoft Open Specifications that Papirus Office uses as the *implementation*
counterpart to the ECMA-376 standard. ECMA-376 (linked from `CONCEPT.md`) says what a `.docx`, `.xlsx`
or `.pptx` package must contain. The `[MS-*]` documents here record what Microsoft Office actually
accepts, ignores, rewrites or refuses, which is the behaviour a reader has to match when a file was
written by Word, Excel or PowerPoint.

The twenty PDFs were added to the repository root in eleven commits (`8e4bc20` to `f6ce775`, batches 1
to 11) and moved into this directory on 2026-10-06 without content changes.

## How to use this shelf

* Page numbers below are 1-based **PDF** page numbers of **these exact files**, measured on 2026-10-06
  with `pypdf 6.19.0`. Every file carries a printed `N / total` header, so a page found here can be
  re-found in any viewer.
* The quickest way to locate a topic without a viewer:

  ```bash
  pip install --break-system-packages pypdf
  python3 - <<'PY'
  from pypdf import PdfReader
  r = PdfReader("docs/ooxml/[MS-OI29500].pdf")
  for i, page in enumerate(r.pages, 1):
      text = page.extract_text() or ""
      if "gridSpan" in text:
          print(i, text.splitlines()[:1])
  PY
  ```

* A full text dump of `[MS-OI29500]` (936 pages) takes about 35 seconds in this environment; a
  keyword-to-page index over the dump is the fastest way to answer "where does Microsoft say X?".
* Read `[MS-OI29500]` first for WordprocessingML, `[MS-OE376]` second (it covers the older ECMA-376
  first edition), and `[MS-DOCX]` third for the extension namespaces. The rest of the shelf is
  reference material for the other modules and for later plans.

## Inventory

| File | Code and release | Pages | What it is | Roadmap relevance |
|---|---|---|---|---|
| `[MS-OI29500].pdf` | v20260818, 2026-08-18 | 936 | Office implementation information for ISO/IEC 29500. Section 2.1 lists every normative variation Word, Excel and PowerPoint introduce against the standard, keyed by the standard's own section numbers. | **Plan 8A and 8B, first stop.** Section 2.1 notes for WordprocessingML run from p.62 (17.3) to p.236 (17.18). |
| `[MS-OE376].pdf` | v20220816, 2022-08-16 | 1027 | The same kind of conformance notes for ECMA-376 1st edition (the 2006 format). Older text, still authoritative for parts that were numbered differently. | Plan 8 secondary. Useful when a 2007-era file behaves differently from a 2016+ file. |
| `[MS-DOCX].pdf` | v20260818, 2026-08-18 | 119 | Word extensions to the `.docx` package: the 2010 to 2026 wordml namespaces, `w:compatSetting` (§2.3, p.17), `numFmt` extensions (§2.4, p.21), `stylisticSet`, text outline and glow effects. | Plan 8A/8B reference for elements that are *not* in ECMA-376. |
| `[MS-OODF14].pdf` | v20240820, 2024-08-20 | 848 | Office implementation information for ODF 1.4. | ODF side, Plan 9 and cross-format convergence (Plan 8B acceptance). |
| `[MS-OODF13].pdf` | v20241112, 2024-11-12 | 835 | Office implementation information for ODF 1.3, the predecessor shelf. | ODF side, historical. |
| `[MS-OODF3].pdf` | v20250218, 2025-02-18 | 823 | Office implementation information for ODF 1.2. | ODF side, historical. |
| `[MS-OODF2].pdf` | v20210817, 2021-08-17 | 953 | Office implementation information for ODF 1.1 version 2. | ODF side, historical. |
| `[MS-OODF].pdf` | v20160914, 2016-09-14 | 927 | Office implementation information for ODF 1.1. | ODF side, historical. |
| `[MS-CUSTOMUI].pdf` | v20250218, 2025-02-18 | 553 | Custom UI XML markup (the 2006 `customui` namespace) plus the `idMso` and `imageMso` tables. | Plan 11 (ribbon decks, command surface). |
| `[MS-CUSTOMUI2].pdf` | v20240820, 2024-08-20 | 205 | Custom UI XML markup version 2 (the 2009 namespace): ribbons, context menus, Backstage. | Plan 11 (ribbon decks, context menus). |
| `[MS-ODRAWXML].pdf` | v20260217, 2026-02-17 | 419 | Drawing extensions: charts, diagrams, SVG, ink, 3D models, WordprocessingShape/Group/Canvas. | Plan 6 (images) and Slidia/Cellina later. |
| `[MS-PPTX].pdf` | v20240820, 2024-08-20 | 170 | PowerPoint extensions to PresentationML 2010 to 2023 (transitions, media, sections, zoom). | Slidia, later stage. |
| `[MS-XLSX].pdf` | v20260519, 2026-05-19 | 437 | Excel extensions to SpreadsheetML 2009 to 2026 (pivot tables, tables, dynamic arrays). | Cellina, later stage. |
| `[MS-OFFMACRO].pdf` | v20160914, 2016-09-14 | 43 | Macro-enabled file format, `vbaProject` parts. | Security/detection only, no plan owns macro editing. |
| `[MS-OFFMACRO2].pdf` | v20240820, 2024-08-20 | 33 | Macro-enabled file format version 2. | Same. |
| `[MS-OEXTXML].pdf` | v20240820, 2024-08-20 | 14 | Shared extensibility (`extlst`) mechanism that Office uses to add markup without breaking older consumers. | Reference for tolerant parsing policy. |
| `[MS-OINTXML].pdf` | v20250218, 2025-02-18 | 40 | Office intelligence extensions (editor suggestions, goal settings). | Not planned. |
| `[MS-OREACTXML].pdf` | v20260818, 2026-08-18 | 19 | Comment reactions (`commentsExtensible`, reactions). | Not planned; comment work is a later stage. |
| `[MS-OTASKXML].pdf` | v20240820, 2024-08-20 | 28 | Document task history (`documenttasks`). | Not planned. |
| `[MS-OWEXML].pdf` | v20240820, 2024-08-20 | 24 | Web extension task panes and content. | Not planned. |

Total: 20 documents, 8,453 pages.

## Where to look for the Plan 8 questions

All page numbers are from `[MS-OI29500]` (v20260818) unless stated otherwise. They were verified by
keyword scan on 2026-10-06; the section ranges are the minimum and maximum pages on which the
document writes a "Section 17.x.y" label for that part.

### Styles and the paragraph style chain (Plan 8A)

| Standard section | Topic | Pages | Notes that matter |
|---|---|---|---|
| 17.7.4.1 | `aliases` (alternate style names) | 105 | Word discards aliases after consecutive commas and caps the value at 253 characters. |
| 17.7.4.5 | `latentStyles` | 106 | Word allows `defUIPriority` 0 to 99 and defaults it to 0 (the standard says 99); an omitted `count` applies the latent defaults to all known built-in styles. |
| 17.7.4.6 | `link` (linked style reference) | 106 | **Word ignores all but the last link to a given style**, which is why a duplicate `w:link` cannot be resolved per occurrence. |
| 17.7.4.17 | `style` (style definition) | 116 | Word **ignores any child elements** of the styles with id `NoList`, `DefaultParagraphFont` and `TableNormal`; a `styleId` longer than 253 characters is ignored; a document with more than 4,079 styles does not open. |
| 17.7.3 | Toggle properties | 105 | The style-hierarchy toggle rule: in Word the effective value of a toggle is true if and only if it is false at an even number of levels, and a level that does not set the property **takes the document defaults value**. This is the rule behind tri-state `w:b`, `w:i`, `w:vanish`, `w:keepNext` and friends. |
| 17.7.8.2 | style `pPr` | 120 | Word forbids `cnfStyle`, `divId`, `pStyle`, `rPr` and `sectPr` as children of a style's `pPr`. |
| 17.7.9.1 | style `rPr` | 120 | Word forbids `cs`, `highlight`, `oMath`, `rStyle` and `rtl` as children of a style's `rPr`. |
| 17.7.6.1 | table style conditional `pPr` | 117 | Word forbids `cnfStyle`, `divId`, `pStyle`, `rPr` and `sectPr` there, and may fail to open a file whose conditional `pPr` carries `framePr`, `suppressOverlap` or `textDirection`. |
| 17.15.1.x | `settings.xml` (compat, defaults) | 147 to 164 | `settings.xml` behaviour, including the compat block that changes how older files lay out. |
| `[MS-DOCX]` 2.3 | `compatSetting` elements | 17 to 20 | How Word records the compatibility mode of a file. |

### Runs and character formatting (Plan 8A, H-2)

| Standard section | Topic | Pages | Notes that matter |
|---|---|---|---|
| 17.3 | Paragraph and run property containers | 62 to 80 | One note per property element (`rFonts`, `sz`, `b`, `i`, `u`, `color`, `highlight`, `vanish`, `spacing`, `ind`, `jc`, `tabs`, `keepNext`, `pageBreakBefore`, `outlineLvl`). |
| 17.4 | Tables | 80 to 95 | `gridSpan`, `vMerge`, `tblGrid`, `tcPr` and `trPr` notes. |
| 17.7 | Styles | 104 to 120 | See the table above. |
| 17.8 | Fonts | 120 to 122 | Word may rewrite substitution elements (`altName`, `panose1`, `charset`, `family`, `notTrueType`, `pitch`, `sig`) when a font is missing. |

### Numbering (Plan 8B, H-3)

| Standard section | Topic | Pages | Notes that matter |
|---|---|---|---|
| 17.9.1 to 17.9.6 | `abstractNum`, `abstractNumId`, `ilvl`, `isLgl`, `lvl` | 123 | Word will not load a negative `abstractNumId`; `ilvl` is 0 to 255; `isLgl` affects all levels including the immediate one. |
| 17.9.11 | `lvlText` | 124 | At most nine `%[1-9]` sequences, and at most 31 characters after substitution; bullets ignore `%x`. |
| 17.9.18 | `numId` | 125 | At least 0. **0 is the value a document uses to suppress numbering.** |
| 17.9.21 / 17.9.22 | `numStyleLink` / numbering `pPr` | 125 | Value length capped at 253 characters; Word allows only `jc`, `ind` and `tabs` as children of a numbering level's `pPr`. |

### Sections, page numbering, headers and footers (Plan 8B, H-6)

| Standard section | Topic | Pages | Notes that matter |
|---|---|---|---|
| 17.6.18 / 17.6.19 | `sectPr` | 103, 104 | Word applies the containing paragraph's `rsid` and forbids `sectPr` inside tables, headers, footers, comments, footnotes, endnotes and text boxes. |
| 17.10.1 | `evenAndOddHeaders` | 126 | Page parity is computed from the section's `pgNumType` start value, not from the page ordinal; a missing header in a later section **inherits the previous section's header** rather than becoming a blank one. |
| 17.10.2 | `footerReference` | 126 | Word expects the relationship type `.../2006/relationships/footer`, not the standard's `.../2006/footer`. |
| 17.11 to 17.15 | Footnotes, endnotes, glossary, comments/revisions, move ranges, `settings.xml` | 127 to 164 | Revision markup that must never leak into body text. |
| 17.16 | Fields and hyperlinks (instruction syntax, `fldSimple`, form fields) | 164 to 226 | Plan 8B H-4. |
| 17.17 to 17.18 | Subdocuments and imported external content; shared simple types (`ST_*`) | 226 to 236 | Reference only. |

## Fixture notes: what the checked-in DOCX files actually contain

Measured on 2026-10-06 by unpacking the six `tests/inky/*.docx` packages directly (ElementTree over
`word/styles.xml`, `word/numbering.xml` and `word/document.xml`).

1. **Style ids are localised, style names are not.** The paragraph style ids are Indonesian
   (`Judul1`, `DaftarParagraf`, `Keterangan`, `Kutipan`, `TOC1` to `TOC3`, `Bibliografi`,
   `Subjudul`) while the `w:name` values are the English built-in names (`heading 1`,
   `List Paragraph`, `caption`, `Quote`, `toc 1`, `Title`, `Subtitle`). No style id in any fixture
   matches `heading[1-9]` or `para[1-9]`, so detection that relies on those id shapes matches
   nothing in this corpus.
2. **The linked character style holds the heading formatting.** In Samples 1, 2, 5 and 6 the
   paragraph heading styles carry no size, font or colour of their own; the matching character
   styles (`Heading1Char` and so on, names `Heading 1 Char` to `Heading 9 Char`) carry
   `Aptos Display`, `sz 40` (20 pt) for level 1, `sz 32` (16 pt) for level 2 and `sz 28` (14 pt)
   for level 3, with colour `0F4761`. Samples 1, 2, 5 and 6 declare **no `w:link` element at all**,
   so the paragraph-to-character pairing is only available through the **name convention**
   (`heading 1` to `Heading 1 Char`, case-insensitive). Sample 4 is the only fixture with explicit
   links: 34 `w:link` elements (17 paragraph/character pairs, including `Judul1` to `Judul1KAR`).
3. **Two fixtures set their own level-1 size, which discriminates the precedence rule.**
   `Sample-2.docx` and `Sample-5.docx` put `sz 28` (14 pt) on the `Judul1` paragraph style while
   `Heading1Char` still says 20 pt. A reader has to decide whether the paragraph style's own value
   or the linked character style wins for a property both set; the fixtures cannot be satisfied by
   an "either" rule. Recommended: the paragraph style's own `w:rPr` wins for properties it sets,
   and the linked character style supplies the rest. Record the decision in `DocxCharLinkTest`.
4. **`w:docDefaults` carries Aptos 12 pt in all six files**, but the paragraph spacing lives in
   different places: Samples 1 and 3 keep only `suppressAutoHyphens` in `pPrDefault` and put the
   effective spacing on `Normal`; Samples 2, 4, 5 and 6 set `spacing` (and in Sample 4 `ind`, in
   Samples 2 and 6 `tabs`) in `pPrDefault`. A reader must merge both.
5. **Toggle properties are tri-state.** `w:b` with no `w:val` means true, `w:val="0"` means false,
   and absence means "inherit" (17.7.3 above). The current run path sets bold at the element and
   resets it at `</w:p>`, so a bold run leaks across the rest of its paragraph.
6. **Numbering lives in the style chain as well as on the paragraph.** In Samples 1, 2 and 5 all
   nine heading styles carry `numId 1`; in Sample 6 all nine carry `numId 15` (`BAB %1` through
   `%1.%2.…%9.`); in Sample 4 only `Judul2` (numId 1) and `Judul3` (numId 0, a suppression) carry
   `w:numPr`; Sample 3 has none and no `word/numbering.xml` at all. Direct `w:numPr` paragraphs
   number 27, 106, 0, 14, 75 and 91 respectively, and a direct `numId 0` suppression appears in
   Samples 1, 2, 5 and 6 (one to three paragraphs each).
7. **`w:lastRenderedPageBreak` is a hint.** Word writes 14, 22, 25, 3, 17 and 19 of them in Samples
   1 to 6; they are not authored page breaks and must not become one.
8. **A straight run path is not enough.** Heading sizes in Samples 1, 4 and 6 grow when rule 2 is
   implemented (level 1 goes from the current 12 pt to the linked style's 20 pt), so the page counts
   of those fixtures will move. The DOCX windows are 12..18, 18..28, 18..26, 8..12, 15..21 and
   15..26; only a CI run can show whether each count stays inside its window. Treat the run-model
   change as measurement-bearing, not neutral.

## Relationship to `docs/odf`

`docs/odf` holds the ODF 1.4 standard itself (parts 1 to 4). This directory holds Microsoft's
implementation notes for both families: `[MS-OI29500]` and `[MS-OE376]` for OOXML, `[MS-OODF*]` for
ODF. When a plan needs "what does Word do here", the answer belongs in this directory; when it needs
"what does the standard require", the answer belongs in `docs/odf` or in ECMA-376.

## Maintenance

* New PDFs go into `docs/ooxml/` directly and get one row in the inventory table with their code,
  release date and page count. The version string is on page 1 of every file.
* Page references in plans and audits should cite the code and the section (`[MS-OI29500] §2.1.242,
  p.116`) so a reader can confirm them.
* Nothing here is redistributed by the app; these files are development references, the same role the
  LibreOffice guides in `docs/lo-guides` and the ODF parts in `docs/odf` already have.
