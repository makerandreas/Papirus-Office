# Audit 008 (2026-09-27): the regenerated fixtures, re-measured (Plan 5b)

**Baseline:** `main` `afb5f77` ("Replacing Sample-1.docx"), on top of `366530b`. Branch `arena/01a0e1a6-papirus-office`.
**Scope:** `tests/inky/Sample-{1..6}.{odt,docx}` and the three `app/src/main/assets/templates/untitled.od{t,s,p}` new-document templates. `tests/cellina` and `tests/slidia` were re-saved too but stay out of scope (roadmap v2 Appendix B).
**Method:** all twelve packages unzipped; every number below is read from the raw XML by a script that mirrors the rules in `SampleMatrixTest.kt` (same regexes, same "last `w:sectPr`", same "first body paragraph's style chain" walk), run on 2026-09-27. Nothing is copied over from audit-007.
**Supersedes:** audit-007 §1, §2, §3, §5 and §11.3 (the fixture facts and the windows). Audit-007's *engine* findings (C, D, G, the parser gaps it names) still describe the code and still stand, but some of their sample evidence is gone (see §5).
**Why now:** CI has been red since `366530b`. The unit-test job fails and the build job passes, because the fixtures changed under tests that pin their XML. Plan 5d (breaks and defaults, formerly "5B") cannot be measured until that is fixed.

## Plan numbering (user decision, 2026-09-27)

Plan 5 is now five PRs. The old names are kept in brackets because audit-007 and roadmap v2 use them.

| Plan | Content | Old name |
|---|---|---|
| 5a | Measuring stick (merged as PR #15) | PR 15 / Plan 5A |
| **5b** | **Fixture re-baseline (this audit plus the test updates)** | new |
| 5c | Documentation refresh (`DESIGN.md` from the attached reference files and the 26.2 guides; Create New docs) | new |
| 5d | Breaks and defaults | PR 16a / Plan 5B |
| 5e | Metrics and windows | PR 16b / Plan 5C |

---

## 0. Findings in one screen

| # | Finding | Evidence | Consequence |
|---|---|---|---|
| A | All six DOCX are now saved by **Microsoft Office Word 16** and carry `<Pages>` in `app.xml`: **15 / 23 / 22 / 10 / 18 / 21**. Sample-3 is 22 pages, not audit-007's 20 (the user re-checked in M365). | §1 | `SampleMatrix` references; the Sample-3 DOCX window moves to 18..26 |
| B | All six ODT are saved by **Collabora Office 26.04.3.1** (LibreOffice core) and carry `meta:page-count` **15 / 23 / 22 / 11 / 19 / 22**, matching the counts the user read. The ODT side finally has references. | §1 | ODT windows re-derived; audit-007's provisional windows for Sample-2 (15..22) and Sample-3 (12..20) **excluded** their own references |
| C | Collabora writes **one page geometry into all six ODTs**: A4 (21.001 × 29.7 cm), 2.54 cm top and sides, 1 cm bottom margin plus a footer of `fo:min-height` 1.54 cm (Sample-3: 2.54 cm bottom, empty 0 cm footer). Body = 2.54 cm on all four sides, like the DOCX. | §2 | audit-007 finding B is gone: no fixed-height header, no 0 cm Sample-3 margin. The `svg:height` header rule keeps its test on a synthetic `styles.xml` |
| D | The body starts on master page `Standard` → `Mpm1` in every ODT; extra masters are now `Converted1..n` → `Mpm2`. The `MasterPage2..7`, `First_20_Page` and `pm1_f` layouts no longer exist. | §2 | `PageGeometryTest` and `SampleMatrixTest` rows rewritten |
| E | Collabora **renamed the heading styles**: every ODT now uses `Heading_20_1..9` (the display names are "Heading n"). The OnlyOffice `Judul1/Judul2` and the TextMaker names are gone from the ODT side. The DOCX side goes the other way: M365 on an Indonesian locale writes **localised style ids** (`Judul1` = `heading 1`, `DaftarParagraf` = `List Paragraph`, `Keterangan` = `caption`, `Kutipan` = `Quote`). | §3.3, §3.4 | `Sample5StyleFidelityTest` moves to `Heading_20_1/2`. On the DOCX side the `heading[1-9]` id regex never matches `Judul1`, but `extractDocxStyles` already classifies by `w:name` through `NavigatorStringCatalog` and by `w:outlineLvl`, so detection still works (`OfficeDocumentParser.kt`, `extractDocxStyles`) |
| F | **Sample-3.odt now has 27 `text:h`** (audit-007: zero). Sample-5.odt's headings are real `text:h` (38) instead of styled paragraphs. | §3.3 | The "Sample-3 has no headings" special case (roadmap §2.4.6) is gone |
| G | **Sample-6.odt has no `text:section` any more** (audit-007: five). Its TOC, 45 bookmarks and 45 links survive. | §3.3 | Roadmap ⚑G-7 (sections) has no sample left in the ODT set; see §5 |
| H | M365 moved Sample-1/3's paragraph defaults out of `w:pPrDefault` into the `Normal` style (`after 160, line 276/278`). `pPrDefault` now holds only `suppressAutoHyphens` there. Every default paragraph style id is now `Normal`. | §3.1 | 5d's metric-only style chain must read Normal's `pPr`, not only `docDefaults` |
| I | The **untitled templates are not Collabora/M365 output**. They are hand-built packages from 2017 to 2023 without `office:version`, without `meta.xml`, without any page layout, and `untitled.odt` stores `mimetype` fourth instead of first. | §6 | They open on the Letter fallback page, not A4. Recommendation in §6.3 |

---

## 1. Provenance and reference page counts

| # | ODT generator | ODT `meta:page-count` / words | DOCX `app.xml` `<Application>` | DOCX `<Pages>` | Reference ODT / DOCX |
|---|---|---|---|---|---|
| 1 | Collabora_Office/26.04.3.1 (Android AArch64) | 15 / 5184 | Microsoft Office Word 16 (re-uploaded in `afb5f77`; the first upload was a Collabora DOCX) | 15 | 15 / 15 |
| 2 | same | 23 / 5063 | Microsoft Office Word 16 | 23 | 23 / 23 |
| 3 | same | 22 / 9948 | Microsoft Office Word 16 | 22 | 22 / 22 |
| 4 | same | 11 / 1974 | Microsoft Office Word 16 | 10 | 11 / 10 |
| 5 | same | 19 / 5223 | Microsoft Office Word 16 | 18 | 19 / 18 |
| 6 | same | 22 / 5223 | Microsoft Office Word 16 | 21 | 22 / 21 |

Sample-5 and Sample-6 report the same word count, but they are different documents (different text, different `content.xml` digests). It is a coincidence.

The ODT count is equal to or one page above the DOCX count in every pair. That is the expected LibreOffice-vs-Word layout gap on otherwise shared metrics, not a producer defect. The per-format windows (§4) stay per format for that reason.

## 2. Declared page geometry

### 2.1 DOCX (last `w:sectPr`)

| # | `pgSz` (twips) | `pgMar` top / bottom / left / right | header / footer distance | sections |
|---|---|---|---|---|
| 1 | 11906 × 16838 | 1440 all | 0 / 567 | 1 |
| 2 | 11907 × 16840 | 1440 all | 0 / 567 | 5 |
| 3 | 11906 × 16838 | 1440 / **2203** / 1440 / 1440 | 0 / **1440** | 1 |
| 4 | 11906 × 16838 | 1440 all | 0 / 567 | 6 |
| 5 | 11907 × 16840 | 1440 all | 0 / 567 | 5 |
| 6 | 11907 × 16840 | 1440 all | 0 / 567 | 5 |

Sample-3 changed: its body is now 6.27 in × 9.16 in (bottom margin 1.53 in), not 9.69 in. Sample-1 changed its page size from 11907 × 16840 to 11906 × 16838 (a 1-twip rounding difference between producers).

### 2.2 ODT (`style:page-layout` behind the master page the body starts on)

| # | master pages (`name` → layout) | first body master | page | margins top / bottom / sides | header | footer `fo:min-height` | body top / bottom |
|---|---|---|---|---|---|---|---|
| 1 | Standard → Mpm1 | none (ODF default Standard) | 21.001 × 29.7 cm | 2.54 / 1 / 2.54 cm | none | 1.54 cm | 2.54 / 2.54 cm |
| 2 | Standard → Mpm1, Converted1..4 → Mpm2 | Standard | same | same | none | 1.54 cm | 2.54 / 2.54 cm |
| 3 | Standard → Mpm1 | none | same | 2.54 / **2.54** / 2.54 cm | none | **0 cm** | 2.54 / 2.54 cm |
| 4 | Standard → Mpm1, Converted1..5 → Mpm2 | none | same | 2.54 / 1 / 2.54 cm | none | 1.54 cm | 2.54 / 2.54 cm |
| 5 | Standard → Mpm1, Converted1..4 → Mpm2 | Standard | same | same | none | 1.54 cm | 2.54 / 2.54 cm |
| 6 | Standard → Mpm1, Converted1..4 → Mpm2 | Standard | same | same | none | 1.54 cm | 2.54 / 2.54 cm |

"Converted*" are the page styles LibreOffice creates for Word sections. The parser's current rule (first master = the first paragraph's style chain, else "Standard") is correct for every file. The header-height path in `SvXMLImport` (fixed `svg:height` over `fo:min-height`) no longer has a fixture, so `PageGeometryTest.fixedHeaderHeightWinsOverMinHeight` pins it on a minimal `styles.xml` instead.

## 3. Defaults, styles and inventory

### 3.1 DOCX defaults (`w:docDefaults`, then the default paragraph style)

| # | `rPrDefault` | `pPrDefault` | Normal (`w:styleId`) | Normal `pPr` spacing |
|---|---|---|---|---|
| 1 | Aptos 12 pt | `suppressAutoHyphens` only | `Normal`, TNR **11 pt** | after 160, line 276 auto |
| 2 | Aptos 12 pt | tab 714, after 160, line 278, `jc both` | `Normal`, TNR | none |
| 3 | Aptos 12 pt | `suppressAutoHyphens` only | `Normal`, TNR | after 160, line 278 auto |
| 4 | Aptos 12 pt | line 360, `firstLine 720`, `jc both` (no `w:after`) | `Normal`, TNR | none |
| 5 | Aptos 12 pt | after 160, line 278, `jc both` | `Normal`, TNR | none |
| 6 | Aptos 12 pt | tab 714, after 160, line 278, `jc both` | `Normal`, TNR | none |

The effective body is the same as audit-007 recorded (TNR 12 pt, 1.158 lines, 8 pt after; Sample-1 11 pt, 1.15 lines; Sample-4 1.5 lines with a first-line indent). The change is *where* that value lives: for Sample-1 and Sample-3 it now sits on `Normal`, not in `docDefaults`.

### 3.2 ODT defaults (`default-style` paragraph, then `Standard`)

| # | `default-style` | `Standard` font / size / line-height / margin-bottom |
|---|---|---|
| 1 | Aptos 12 pt | TNR / **11 pt** / 115 % / 0.282 cm |
| 2 | Aptos 12 pt | TNR / (12 pt inherited) / 116 % / 0.282 cm |
| 3 | Aptos 12 pt | TNR / (12 pt) / 116 % / 0.282 cm |
| 4 | Aptos 12 pt | TNR / (12 pt) / **150 %** / **0 cm** |
| 5 | Aptos 12 pt | TNR / (12 pt) / 116 % / 0.282 cm |
| 6 | `Aptos1` 12 pt (the `style:font-face` name Collabora declared; family Aptos) | TNR / (12 pt) / 116 % / 0.282 cm |

The ODT and DOCX twins are now **metric twins in every pair** (Collabora imported the DOCX defaults faithfully). Audit-007 finding F ("the OnlyOffice exports lost `pPrDefault`") no longer applies to the fixtures.

### 3.3 ODT structure

| # | `text:h` | heading styles used (via automatic styles) | tables | images | `text:list` | links | bookmarks | sections | soft breaks + break-before |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 28 | Heading 1–4 | 3 | 6 | 38 | 0 | 0 | 0 | 14 + 0 |
| 2 | 30 | Heading 1–3 | 2 | 11 | 144 | 15 | 15 | 0 | 16 + 1 |
| 3 | **27** | Heading_20_1 (20 pt), Heading_20_2 (16 pt) direct | 1 | 0 | 0 | 0 | 0 | 0 | 21 + 0 |
| 4 | 25 | Heading 1–2 | 0 | 1 | 33 | 2 | 17 | 0 | 3 + 2 |
| 5 | **38** | Heading 1–4 (P3/P9/P13/P33/P37 → Heading_20_1, 14 pt bold) | 0 | 2 | 98 | 14 | 14 | 0 | 12 + 1 |
| 6 | 45 | Heading 1–3 | 1 | 3 | 111 | 45 | 45 | **0** | 15 + 1 |

`Heading_20_1` is 14 pt bold in Samples 2 and 5, bold at body size in 1, 4 and 6, and 20 pt regular in 3. That is the document's own design, so the 24/20/16 sp fallback in `StyleResolver` is still wrong for all six (5d).

### 3.4 DOCX structure

| # | top paragraph styles (`w:pStyle` id = `w:name`, count) | tables | drawings | hyperlinks | bookmarks | `lastRenderedPageBreak` | `w:br type=page` | other |
|---|---|---|---|---|---|---|---|---|
| 1 | DaftarParagraf = List Paragraph 31, Judul3 = heading 3 16, Keterangan = caption 9, Judul2 6, Judul1 4 | 3 | 6 | 0 | 0 | 14 | 0 | `numId 0` ×1 |
| 2 | List Paragraph 115, heading 3 15, caption 13, toc 2 9, heading 2 9, heading 1 6, toc 1 6 | 2 | 12 | 15 | 15 | 22 | 2 | `w:sdt` ×2, `m:oMath` ×2, `mc:AlternateContent` ×2 |
| 3 | heading 2 20, heading 1 7, Quote 4, Title 1 | 1 | 0 | 0 | 0 | 25 | 0 | |
| 4 | toc 2 14, heading 2 14, heading 1 11, toc 1 7 | 0 | 1 | 23 | 22 | 3 | 2 | |
| 5 | List Paragraph 70, heading 4 15, heading 3 9, toc 2 8, heading 2 8, heading 1 6 | 0 | 1 | 14 | 14 | 17 | 2 | `m:oMath` ×2 |
| 6 | List Paragraph 79, Quote 31, toc 3 28, heading 3 28, toc 2 11, heading 2 11, heading 1 6 | 1 | 3 | 45 | 46 | 19 | 2 | |

### 3.5 Font families declared

The audit-007 §8 corpus is still present in full (Times New Roman, Aptos, Aptos Display, Calibri, Cambria Math, Courier New, Arial, Basic Sans, Segoe UI Variable, Microsoft YaHei UI, MS Mincho). **New:** Cambria, Carlito, Symbol, Wingdings, Noto Sans CJK SC, Noto Sans Devanagari. Symbol and Wingdings already map to OpenSymbol by roadmap §0. The Noto families are Collabora's own East-Asian/complex-script defaults. `FontRegistrySubstitutionTest` does not read the fixtures and is unaffected; extending its corpus is a Plan 10 A1 task.

## 4. Windows (recorded, not asserted until 5e)

Staged at roughly ±20 % of the reference, the same staging audit-007 used for the DOCX side. `SampleMatrixTest.windowsContainTheirReferenceCounts` now checks both formats.

| # | DOCX ref → window | ODT ref → window |
|---|---|---|
| 1 | 15 → 12..18 | 15 → 12..18 |
| 2 | 23 → 18..28 | 23 → 18..28 |
| 3 | 22 → **18..26** (was 16..24 around 20) | 22 → 18..26 |
| 4 | 10 → 8..12 | 11 → 9..13 |
| 5 | 18 → 15..21 | 19 → 15..23 |
| 6 | 21 → 15..26 | 22 → 17..27 |

The ±10 % tightening still waits until Plans 7/8 (roadmap §0).

## 5. What this changes in the roadmap

| Roadmap item | Before | Now |
|---|---|---|
| 5d (old 16a) · body rect = margins + header/footer | Needed for Sample-4/5's fixed headers and Sample-3's 0 cm margins | Still needed (every ODT has a 1.54 cm footer inside a 1 cm margin), but only the footer half is exercised by a fixture. Header height is covered synthetically |
| 5d · fake breaks out | Sample-2 dominated (22 `lastRenderedPageBreak` + 10 soft breaks) | Now in **all twelve files**: 3–25 `lastRenderedPageBreak` per DOCX, 3–21 soft breaks per ODT (§3.3, §3.4). The dump in `Plan5ElementDumpTest` prints the new parse-level counts |
| 5d · style chain metrics | `docDefaults` + `basedOn` | Also read the default paragraph style's own `pPr` (Sample-1/3, finding H) |
| PR 20 (8A) · heading detection | `heading[1-9]` regexes are "sample-tuned" | The id regexes now match **no** DOCX heading style (`Judul1..4`); detection rests entirely on the `w:name` catalog lookup and `w:outlineLvl`, which already exist. Retiring the regexes (PR 20) is therefore safe, and the fixtures prove it |
| PR 19 (7B) ⚑G-7 · ODT sections | Sample-6 had 5 sections | No ODT fixture has a section. G-7 needs a synthetic fixture or is deferred; the DOCX side still has 5–6 `w:sectPr` per file |
| Roadmap §2.4.6 · Sample-3 has zero `text:h` | special case | gone |
| audit-007 finding F · ODT twins not metric-identical | per-format windows justified by metrics | Twins are metric-identical now; per-format windows are still justified by the observed 0–1 page LibreOffice/Word gap (§1) |

## 6. The untitled templates

### 6.1 Inventory

| File | Entries (in ZIP order) | `mimetype` | `office:version` | `meta.xml` | `styles.xml` | Page layout |
|---|---|---|---|---|---|---|
| `untitled.odt` (1268 B) | content.xml, META-INF/, META-INF/manifest.xml, **mimetype**, styles.xml | stored, **4th** | absent | absent | a `default-style` with only `use-window-font-color` | **none** |
| `untitled.ods` (667 B) | mimetype, META-INF/manifest.xml, content.xml | stored, 1st | absent | absent | absent | none |
| `untitled.odp` (668 B) | mimetype, META-INF/manifest.xml, content.xml | stored, 1st | absent | absent | absent | none |

File dates inside the ZIPs run from 2017 to 2023; none of them came from Collabora or M365.

### 6.2 Against ODF 1.4 (`docs/html`)

* **Part 2 §3.3:** "The 'mimetype' file shall be the first file of the zip file. It shall not be compressed." `untitled.odt` breaks the first rule. Tolerant readers (including Papirus's own ZIP walk) still open it; strict validators and some consumers identify the type by the first 30+ bytes and will not.
* **Part 3 §19.390:** "The office:version attribute shall be present in every `<office:document-content>`, `<office:document-styles>` … The value … shall be '1.4'." Absent in all three roots.
* **No page layout:** a blank Inky document therefore opens on `PageStyleSpec.FALLBACK` (Letter, 816 × 1056 units, see `PageGeometryTest.layoutEngineKeepsLetterFallbackWithoutDeclaredGeometry`), while every sample and the user's region use A4. What is saved back also carries no page geometry.
* Empty `office:text` / `office:spreadsheet` / `office:presentation` bodies are schema-valid.

### 6.3 Recommendation (not done in 5b)

Replace the three templates with blank documents **saved from Collabora Office 26.04**, the same producer as the samples. That fixes the ZIP order, the version attribute, `meta.xml`, and gives Inky an A4 `Standard` page with LibreOffice's default styles. It is an asset change the user makes on the device, like the sample regeneration. Once it lands, a pure-JVM `TemplatePackageTest` (mimetype first and stored, `office:version="1.4"`, a `Standard` master page) keeps it that way. It is not added now because it would fail against the current assets, and 5b's purpose is a green baseline. `CreateNewDocumentTest` only checks that the files extract and open, so it is unaffected either way.

## 7. Test changes made in Plan 5b

| File | Change |
|---|---|
| `SampleMatrix.kt` | References (DOCX 15/23/22/10/18/21, ODT 15/23/22/11/19/22, both non-null now), windows per §4, provenance strings |
| `SampleMatrixTest.kt` | All twelve rows re-measured (§2, §3); the window check now requires the ODT window to contain its reference too |
| `PageGeometryTest.kt` | Sample-5/Sample-1 ODT expectations moved to the Collabora layout; new `fixedHeaderHeightWinsOverMinHeight` on a synthetic `styles.xml` so the header rule keeps a test |
| `Sample5StyleFidelityTest.kt` | `Judul1/Judul2` → `Heading_20_1/Heading_20_2` (14 pt bold / 12 pt inherited) |
| `Plan5ElementDumpTest.kt` | Parse-level break inventory updated to §3.3 (printed, not asserted) |

Tests whose assertions depend on laid-out page counts (`Sample5UnifiedPaginationTest`, `HyphenationEngineTest`, `Sample5StyleFidelityTest`'s 12..30 check, both at 12..30) are left as they are. Their numbers come from CI, not from reading XML, and the first CI run on this branch decides whether they need anything. Those results go in §8.

## 8. CI evidence

Recorded after the first CI run on this branch.
