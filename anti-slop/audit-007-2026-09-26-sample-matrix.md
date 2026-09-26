# Audit 007 (2026-09-26): the six sample pairs, both formats, and what they change for Plan 5

**Baseline:** `main` `5c99072` (PR #14 merged). Branch `arena/01a0dc79-papirus-office`.
**Scope:** `tests/inky/Sample-{1..6}.{odt,docx}` only. `tests/cellina` and `tests/slidia` stay out of scope (roadmap v2 Appendix B rule).
**Method:** all twelve packages unzipped; every number below is read from the raw XML with the regexes listed in Appendix A, run on 2026-09-26. Nothing is quoted from another plan without re-deriving it. Where this file and `plan-2026-09-24-remaining-pr-roadmap-v2.md` §2 disagree, the difference is called out and this file wins for the rows it re-measured.
**Why now:** PR 15 (Plan 5A) is the first PR that touches page metrics. Before it moves anything, the corpus had to be measured on both sides, because until now the plans quoted the ODT side in detail and the DOCX side only in outline.
**User decisions taken on 2026-09-26 (recorded here, not re-asked):**

1. "5a" is PR 15 (Plan 5A); "5b" is PR 16.
2. Page geometry is honoured **as each file declares it**; page-count windows are kept **per format**.
3. PR 16 is split: **PR 16a = Plan 5B** (breaks and defaults), **PR 16b = Plan 5C** (metrics and windows).
4. Reference page counts for the DOCX side come from **Microsoft 365 Copilot** (the user re-saved every `.docx` in M365 and read the count): Sample-1 **15**, Sample-3 **20**, Sample-4 **10**. Sample-2 (23) and Sample-5 (18) are the `<Pages>` values Word itself wrote into `docProps/app.xml`; Sample-6 (21) was recorded earlier in roadmap v2 §0. The ODT side has no trustworthy reference yet: the user will regenerate the six `.odt` fixtures with **Collabora Office** at a later stage and read the counts there.
5. `antislop.md` is a filter for the work, not a file to edit.

---

## 0. The eight findings in one screen

| # | Finding | Evidence | Lands in |
|---|---|---|---|
| A | Every DOCX declares A4 with 1 in margins and a 1 cm footer distance; the body is 6.27 in × 9.69 in in all six. | §2 | `SampleMatrixTest` (PR 15) |
| B | The ODT twins do **not** all declare the same geometry. Sample-4/5 carry the top margin as a fixed-height header (`svg:height="2.54cm"`, `fo:margin-top="0cm"`), which is equivalent. Sample-3 declares 0 cm top and bottom with no header or footer, which is a genuine export defect. `PageGeometryTest` currently reads "margin-top 0" as the whole truth. | §2.2 | PR 15 (test wording), PR 16a (body rect = margins + header/footer heights) |
| C | In four of six DOCX there is **no** direct `w:sz` in the body; sizes come from `docDefaults` and the `basedOn` chain. `extractDocxStyles` never fills `basedOn`; `SvXMLImport` captures no paragraph spacing. | §3, §9 | PR 16a (metric-only chain), PR 20 (full chain) |
| D | TextMaker headings (Sample-1, Sample-6) are **body-size bold**; the 20/16/14 pt "Heading n Char" styles are orphaned (no `w:link`, no `w:rStyle` anywhere). The current 24/20/16 sp fallback inflates 45 + 28 headings. | §4 | PR 16a (defaults), PR 20 (char-link rule) |
| E | Sample-2's page count is dominated by fake breaks: 22 `w:lastRenderedPageBreak` + 10 `text:soft-page-break`, all treated as hard breaks today. | §5 | PR 16a, first commit |
| F | The OnlyOffice ODT exports (Sample-3/4/5) **lost the DOCX `pPrDefault`**: line height 100 % and no after-spacing where the DOCX twin says 1.15 lines and 8 pt after. The twins are therefore not metric-identical, which is why per-format windows are the honest choice. | §3 | windows table §11.3 |
| G | The paginator's `flushPage()` only emits a page that has elements, so the "double break makes a blank page" suspicion (plan-02 item 7) is **refuted** by code reading; the only blank page it can author is a break at element 0. Blank pages come from E plus inflated metrics. The E-0 dump confirms this per file. | §9 | PR 15 E-0 |
| H | Several producer quirks will break a naive reader: attribute order (`style:parent-style-name` before `style:name`), numeric style ids (`987`, `style0`, `para0`) where only `style:display-name`/`w:name` carries identity, Sample-3.odt with zero `text:h`, `w:numId="0"` suppression, `w:br type="page"` inside a heading run, `m:oMath`/`mc:AlternateContent`/`w:sdt`/`w:pict` content. | §7 | PR 15 dump counts them; PR 18/20/21 handle them |

---

## 1. Provenance and reference page counts

| # | ODT generator | DOCX generator | ODT `meta:page-count` / words | DOCX `app.xml` `<Pages>` / words | **M365 reference (DOCX)** | ODT reference |
|---|---|---|---|---|---|---|
| 1 | LibreOffice 7.0.4.2 | TextMaker NX rev. 1229 | 9 / 5183 | none / none | **15** (user, 2026-09-26) | pending Collabora |
| 2 | LibreOffice 7.0.4.2 | Microsoft Office Word | 18 / 5063 | 23 / 5738 | **23** (app.xml) | pending Collabora |
| 3 | OnlyOffice 9.4.1.15 | WPS Office | none | none / 9950 | **20** (user) | pending Collabora |
| 4 | OnlyOffice 9.4.1.15 | OnlyOffice 9.0.4.50 | none | none | **10** (user) | pending Collabora |
| 5 | OnlyOffice 9.4.1.15 | Microsoft Office Word | none | 18 / 6065 | **18** (app.xml, matches roadmap §0) | pending Collabora |
| 6 | TextMaker | TextMaker NX rev. 1229 | none | none | **21** (roadmap v2 §0) | pending Collabora |

Notes that matter for the windows:

* `meta:page-count` is what LibreOffice 7.0.4 wrote at save time. It is a claim, not a measurement we can reproduce: Sample-1's ODT and DOCX carry the same six images at the same sizes (§6), the same body font and size (Times New Roman 11 pt), the same line height (115 % vs `w:line=276`) and the same after-spacing (0.111 in vs 160 twips), yet the ODT claims 9 pages and M365 renders the DOCX at 15. Until the Collabora count exists, Sample-1's ODT window bridges both numbers (§11.3).
* Sample-2's twins are metric twins after all. **Correction 2026-09-26 (PR 15, `SampleMatrixTest`):** the first draft read the ODT `Standard` as 11 pt; it carries no `fo:font-size`, so the body inherits 12 pt from `default-style`, exactly as the DOCX `Normal` inherits 12 pt from `docDefaults`. 18 (LO claim) vs 23 (M365) is therefore the same unexplained producer gap as Sample-1, not a font-size difference.
* Sample-6 is the only pair whose declared metrics match on both sides (TextMaker wrote both): Times New Roman 12 pt, 116 % vs `w:line=278`, 0.282 cm vs `w:after=160`, 2.54 cm margins all round. Its shared window (15..26) stands.

---

## 2. Declared page geometry

### 2.1 DOCX (`w:sectPr`, last section unless stated; all sections in a file share `pgSz`/`pgMar`)

| # | `pgSz` (twips) | `pgMar` top/bottom/left/right | header / footer distance | sections | `titlePg` | `pgNumType` sequence |
|---|---|---|---|---|---|---|
| 1 | 11907 × 16840 | 1440 all | 0 / 567 | 1 | no | decimal |
| 2 | 11907 × 16840 | 1440 all | 0 / 567 | 5 | first | lowerRoman start 1 → start 1 → (none) ×3 |
| 3 | 11906 × 16838 | 1440 all | 0 / **0** | 1 | no | decimal |
| 4 | 11906 × 16838 | 1440 all | 0 / 567 | 6 | first | lowerRoman → start 1 → (none) ×4 |
| 5 | 11907 × 16840 | 1440 all | 0 / 567 | 5 | first | lowerRoman start 1 → start 1 → (none) ×3 |
| 6 | 11907 × 16840 | 1440 all | 0 / 567 | 5 | first | lowerRoman start 1 → decimal start 1 → decimal ×3 |

Body rectangle for every DOCX: **6.27 in × 9.69 in** (8.27 − 2 by 11.69 − 2). In WordprocessingML the header and footer live inside the margin; they only push the body when their content is taller than `margin − distance`, which a one-line page-number footer at 12 pt never is. Sample-1/2/5/6 footers carry page numbers (`footerReference default`, plus `first` in Sample-4).

### 2.2 ODT (`style:page-layout` reached from the master page the body actually uses)

| # | master pages the body references (`style:master-page-name` in `content.xml`) | page size | margins top/bottom/left/right | header style | footer style | body top / bottom as LibreOffice computes it |
|---|---|---|---|---|---|---|
| 1 | `Standard` → `Mpm1` | 8.2681 × 11.6929 in | 1 / 0.3937 / 1 / 1 in | none | `min-height 0.6063in` | 1 in / 1 in (0.3937 + 0.6063) |
| 2 | `First_20_Page` → `Mpm2`; `Converted1..4` → `Mpm3` (`Standard` → `Mpm1` is never referenced) | same | Mpm2: 1 in all, no footer; Mpm3: as Mpm1 | none | Mpm3 `0.6063in` | 1 in / 1 in on every layout |
| 3 | `MasterPage2` → `Mpm2` (`Standard` → `Mpm1` identical) | 21 × 29.7 cm | **0 / 0** / 2.54 / 2.54 cm | none | none | **0 cm / 0 cm** |
| 4 | `MasterPage2..7` → `Mpm2..7` (all identical) | 21 × 29.7 cm | **0** / 1 / 2.54 / 2.54 cm | `svg:height="2.54cm"`, `min-height 0cm`, header contains an empty `text:p` | `min-height 1cm` | **2.54 cm** / 2 cm |
| 5 | `MasterPage2..6` → `Mpm2..6` (all identical) | 21 × 29.71 cm | **0** / 1 / 2.54 / 2.54 cm | same as 4 | `min-height 1cm` | **2.54 cm** / 2 cm |
| 6 | first paragraph has no master reference → default `Standard` → `pm1_f`, `next-style-name Standard_Next` → `pm1`; `Chapter2..5` → `pm2..5` | 21.003 × 29.704 cm | 2.540 all | none | `min-height 0.000cm` (footer with page number) | 2.54 cm / 2.54 cm + footer content |

What this means:

* **Sample-4 and Sample-5 are not defective.** OnlyOffice encodes Word's top margin as a fixed-height header (`svg:height` on `style:header-footer-properties` "specifies the height of a header or footer", ODF 1.4 Part 3 §20.407.2; `fo:min-height` §20.212 is the minimum for content-sized headers); body top = `fo:margin-top` + header height = 2.54 cm, the same as the DOCX. The engine must compute the body rectangle from margins **plus** header/footer heights (fixed `svg:height`, else `fo:min-height`, else content). Today `PageStyleSpec` has no header/footer fields and `SvXMLImport` reads only `page-layout-properties`, so these two files paginate against a 29.7 cm tall body. This is the biggest single geometry error in the ODT path and it is a parser gap, not a fixture quirk.
* **Sample-3.odt is defective as declared**: no header, no footer, 0 cm top and bottom. Honouring the file (user decision 2) means the body is the full 29.7 cm tall. Its ODT window is provisional (§11.3) and the fixture will be replaced by the Collabora regeneration.
* **The body does not use master page `Standard` in five of six files.** `PageGeometryTest` resolves "Standard → Mpm1" by name. It is harmless here because every referenced layout in a file shares its geometry (Sample-2's `Mpm2` differs only by moving 0.6063 in from footer to margin), but the rule the engine needs is: first master = the first body paragraph's `style:master-page-name` (via its automatic style), then `style:next-style-name`, default `Standard`. That rule belongs to the section model (roadmap ⚑G-7, PR 19); PR 15's matrix test records which master each file starts on so the later change has a fixed target.
* `PageGeometryTest.sample5OdtDeclaresA4WithDeclaredMargins` stays true as a statement about `fo:margin-top`; its name and its comment should say "declared margins", and a second assertion should pin the header `svg:height` once `PageStyleSpec` carries it (PR 16a). Nothing about the test's numbers is wrong; its wording implies a body top of 0 that LibreOffice would not produce.

---

## 3. Paragraph and text defaults (the numbers 5B/5C paginate with)

### 3.1 DOCX

| # | `docDefaults` `rPr` | `docDefaults` `pPr` | Normal (default paragraph style) | Effective body |
|---|---|---|---|---|
| 1 | Aptos 12 pt (`sz 24`) | after 160, line 276 auto, `jc both`, tab 714 | `para0` Times New Roman **`sz 22`** (11 pt), `lang id-id` | TNR 11 pt, 1.15, 8 pt after, justified |
| 2 | Aptos 12 pt | after 160, line 278 auto, `jc both` | `Normal` TNR, no size, `lang id-ID` | TNR 12 pt, 1.158, 8 pt after |
| 3 | Aptos 12 pt | after 160, line 278 auto, `jc both` | `style0` "Normal" TNR (cs Segoe UI Variable, eastAsia Microsoft YaHei UI) | TNR 12 pt, 1.158, 8 pt after |
| 4 | Aptos 12 pt | **after 0, before 0, line 360 auto, `ind firstLine 720`**, `jc both` | `987` "Normal" (OnlyOffice writes `w:line=240` on 133 styles) | TNR 12 pt; per-style line values decide (§3.3) |
| 5 | Aptos 12 pt | after 160, line 278 auto, `jc both` | `Normal` TNR, no size, no `pPr` | TNR 12 pt, 1.158, 8 pt after |
| 6 | Aptos 12 pt | after 160, line 278 auto, `jc both`, tab 714 | `para0` TNR, no size | TNR 12 pt, 1.158, 8 pt after |

Direct formatting in `document.xml` (why the chain is the whole story):

| # | `w:sz` | `w:rFonts` | `w:spacing` (p) | `w:ind` | `w:jc` | `w:b/` | `w:i/` | `w:u` | `vertAlign` | `w:color` |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | **0** | 0 | 0 | 71 | 68 | 63 | 132 | 0 | 0 | 0 |
| 2 | 40 | 49 | 17 | 118 | 99 | 0 | 0 | 0 | 7 | 0 |
| 3 | **0** | 76 | 0 | 0 | 54 | 10 | 67 | 0 | **54** | 0 |
| 4 | 8 | 2 | 116 | 116 | 23 | 95 | 15 | 0 | 0 | 0 |
| 5 | **0** | 27 | 0 | 92 | 17 | 0 | 0 | 0 | 17 | 0 |
| 6 | **0** | 0 | 0 | 150 | 51 | 54 | 13 | 0 | 0 | 0 |

Sample-2 and Sample-5 carry **zero** direct `w:b`/`w:i` and still show bold headings: every bold in those files comes from the style chain, which the current parser (`w:b` scanned per paragraph, one `TextRun` per paragraph) cannot see.

### 3.2 ODT

| # | `default-style` (paragraph) | `Standard` / "Normal" | Effective body |
|---|---|---|---|
| 1 | Aptos 12 pt | `Standard`: TNR 11 pt, line-height 115 %, margin-bottom 0.111 in, justify, widows 2, orphans 2, lang id-ID | TNR 11 pt, 1.15, 8 pt after |
| 2 | Aptos 12 pt | `Standard`: TNR, **no `fo:font-size`** (corrected 2026-09-26; first draft said 11 pt), line-height 115 %, margin-bottom 0.111 in | TNR **12 pt** (from `default-style`), 1.15, 8 pt after |
| 3 | Aptos 12 pt, line-height 100 %, justify, tab 36 pt | `style0` (display "Normal"): TNR 12 pt, **line-height 100 %**, no margins | TNR 12 pt, single, **0 after** |
| 4 | Aptos 12 pt, line-height 100 %, text-indent 1.27 cm, justify | `987` (display "Normal"): TNR, line-height 100 %, text-indent 1.27 cm | per paragraph: 115.833 % ×5, 150 % ×3, margin-bottom 8 pt ×6 in `content.xml` |
| 5 | Aptos 12 pt, line-height 100 %, justify | `Normal`: TNR 12 pt, **empty paragraph properties** | TNR 12 pt, **single, 0 after** (no `fo:line-height` or `fo:margin-bottom` anywhere in `content.xml`) |
| 6 | TNR 12 pt | `Normal`: margin-bottom 0.282 cm, line-height 116 %, widows/orphans declared | TNR 12 pt, 1.16, 8 pt after |

Reading note (added 2026-09-26): Sample-3/4/5 (OnlyOffice and WPS exports) name the default font with `fo:font-family="Aptos"` and never with `style:font-name`; Sample-1/2/6 use `style:font-name`. A reader that only looks at `style:font-name` sees no default font in three of six files. `SampleMatrixTest` pins both spellings.

Finding F in one sentence: OnlyOffice's ODT export did not carry Word's `pPrDefault` into `style:default-style`, so Sample-3 and Sample-5 are single-spaced with no paragraph spacing **as declared**, and an honest engine will paginate them shorter than the M365 count of their DOCX twins. That is not something the engine should "repair"; it is why the windows are per format and why the ODT fixtures are being regenerated.

### 3.3 Sample-4 is the line-spacing stress case

`docDefaults` says 1.5 lines, `Normal` says `w:line=240` (single), the body carries 5 × `w:line=278` and 3 × `w:line=360`, the ODT side carries 115.833 % and 150 % per paragraph. A resolver that reads only `docDefaults`, or only the paragraph style, gets Sample-4 wrong in opposite directions; it has to apply the full order `docDefaults → style chain → direct pPr`.

---

## 4. What a "heading" really is, per file

| # | DOCX heading 1 (style as written) | DOCX heading 2 | Where the size comes from | ODT side |
|---|---|---|---|---|
| 1 | `para1` "heading 1": `basedOn para0`, `numPr numId 1`, `ind left 360 hanging 360`, keepNext, keepLines, outlineLvl 0, **rPr `b` + `caps` only** | `para2`: same shape, `ilvl 1` | inherits 11 pt from `para0`; `char1..3` (Aptos Display 20/16/14 pt, `#0f4761`) are never referenced | 28 `text:h`; outline numbering `1`, `1.1`, `1.1.1` from `text:outline-style` |
| 2 | `Judul1` "heading 1": `basedOn Normal`, `numPr numId 1` (no `ilvl`), `jc center`, keepNext, keepLines, rPr **`b` + `caps` + `sz 28`** (14 pt) | `Judul2` bold | the style itself | 30 `text:h` |
| 3 | `style1` "heading 1": `basedOn style0`, spacing before 360 after 80, rPr **Aptos Display `#0f4761` 20 pt** (Word's built-in look) | `style2`: 16 pt | the style itself | **0 `text:h`**: headings are `text:p` whose automatic style has `parent-style-name="style1"` (display-name "heading 1"); 7 + 20 of them |
| 4 | `988` "Heading 1": `basedOn 987`, `jc center`, `ind firstLine 0`, rPr **`b` only** | `989` | inherits 12 pt; `w:link` present (27 styles), the only file with it | 7 `text:h` (all level 1) |
| 5 | `Judul1` as Sample-2 (14 pt bold caps centred) | `Judul2` bold, hanging indent | the style itself | 0 `text:h`; `text:p` parented to `Judul1` (display-name "heading 1", `list-style-name WWNum1`, 14 pt bold uppercase) |
| 6 | `para1` "heading 1": `basedOn para0`, `numPr ilvl 0 numId 15`, `jc center`, keepNext, keepLines, rPr **`b` only** | `para2`: `ind left 501 hanging 501`, `spacing before 320`, `b` | inherits 12 pt; `char1..3` orphaned (no `w:link`, no `w:rStyle` in the body) | 45 `text:h`; `BAB %1` numbering from `Makalah_20_Default` in `styles.xml` |

Two consequences:

* A resolver must **not** invent a size for a heading style that declares none. `StyleResolver`'s 24/20/16 sp fallbacks (`LayoutEngine.kt:69-71`) are wrong for Samples 1, 4 and 6 and merely coincidental for Sample-3. The honest default is the chain's size, which in every file ends at `docDefaults`/`default-style`.
* The Word "Heading n Char" convention (roadmap §2.3 item 2) must be applied **only when the body references the character style** (`w:rStyle`) or the paragraph style links it (`w:link`). Applying it by name would turn Sample-6's 12 pt headings into 20 pt ones, the opposite of what Word shows. This narrows the rule PR 20 implements.

Numbering that changes label text and indent (both affect line count): Sample-6 `abstractNum 15` levels `BAB %1` (suffix space), `%1.%2` (suffix tab), `%1.%2.%3` (suffix tab); Sample-1 `abstractNum` with `%1.`, `%1.%2.`, `%1.%2.%3.`; Sample-2/5 `%1.` … `%9.` decimal/lowerLetter/lowerRoman; Sample-4 adds `upperLetter`; Sample-6 has one `bullet` level (`·`). Three Sample-6 headings carry `numId 0` (suppression), unchanged from roadmap §2.3.

---

## 5. Break inventory (what moves text to a new page today, and what should)

| # | `w:br type="page"` | `w:lastRenderedPageBreak` | `w:pageBreakBefore` | `w:keepNext` (body) | `sectPr` | ODT `text:soft-page-break` | ODT `fo:break-before="page"` (automatic styles in `content.xml`) | ODT `fo:keep-with-next="always"` (all styles) |
|---|---|---|---|---|---|---|---|---|
| 1 | 0 | 0 | 0 | 14 | 1 | 8 | 0 | 15 (styles) |
| 2 | 2 | **22** | 0 | 29 | 5 | 10 | 1 | 19 |
| 3 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 |
| 4 | 2 | 0 | 0 | 0 | 6 | 2 | 2 | 0 |
| 5 | 2 | 3 | 0 | 2 | 5 | 2 | 2 | 0 |
| 6 | 2 | 0 | 0 | 10 | 5 | 0 | 2 | 13 |

Today (`OfficeDocumentParser.kt`, shared parse block, and `SvXMLImport`): `w:lastRenderedPageBreak` and `text:soft-page-break` both become `OfficePageBreak` (plus the `--- Page Break ---` text pollution, plan-03 3.17), `fo:break-before` is not read, `w:br type="page"` is read correctly, section boundaries (`sectPr` with `type=nextPage`) are not breaks in the model. Sample-2 therefore gets 22 + 2 forced breaks in the DOCX and 10 in the ODT before any metric is applied; Sample-6's chapter breaks (`fo:break-before="page"` on the automatic style) do not happen at all in the ODT and do happen in the DOCX (`w:br` inside the heading run). PR 16a's first commit removes the fake breaks and adds the real ones, and re-runs the E-0 dump so the effect is visible on its own.

Section starts: every multi-section file uses `type=nextPage` (or Word's default, which is nextPage), so a section boundary is also a page break in the model. `titlePg` on the first section is why the front-matter footers differ.

---

## 6. Structure counts, both formats (re-measured; roadmap §2 rows confirmed unless marked)

| # | `text:p` / `w:p` | `text:h` | lists (`text:list` / `numPr`) | `text:span` / `w:r` | `text:a` / `w:hyperlink` | tables | frames / drawings (`inline`) | TOC | sections (`text:section` / `sectPr`) | `text:tab` / `w:tab` | custom tab stops (`w:tabs`) | bookmarks | notes (`text:note` / `footnoteReference`) | `text:sequence` / `SEQ` fields | line breaks (`text:line-break` / `w:br textWrapping`) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 183 / 211 | 28 | 70 / 54 | 149 / 485 | 0 / 0 | 3 / 3 (header rows 1) | 6 / 6 | 0 / 0 | 0 / 1 | 0 / 0 | 0 | 0 / 0 | 0 / 0 | 9 / 9 (`fldChar` wrapped, 0 `fldSimple`) | 1 / n.a. |
| 2 | 308 / 351 | 30 | 41 / 106 | 185 / 2182 | 15 / 15 | 2 / 2 (header rows 2) | 11 / 12 | 1 / 1 (`TOC \o "1 - 2" \z`) | 0 / 5 | 49 / 0 | 15 | 30 / 15 | 0 / 0 | 13 / 13 | 17 |
| 3 | 170 / 169 | 0 | 0 / 0 | 423 / 421 | 0 / 0 | 1 / 1 | 0 / 0 | 0 / 0 | 0 / 1 | 0 / 0 | 0 | 0 / 0 | **1 / 1** | 0 / 0 | 0 |
| 4 | 112 / 116 | 7 | 15 / 17 | 476 / 464 | 23 / 23 | 0 / 0 | 1 / 1 | 1 / 1 | 0 / 6 | 35 / 35 | 21 | 44 / 22 | **2 / 2** | 0 / 0 | 0 |
| 5 | 218 / 217 | 0 | 123 / 75 | 2587 / 2587 | 14 / 14 | 0 / 0 | 2 / 1 | 1 / 1 | 0 / 5 | 43 / 0 | 14 | 28 / 14 | 0 / 0 | 0 / 0 | 4 |
| 6 | 322 / 371 | 45 | 115 / 133 | 451 / 407 | 45 / 45 | 1 / 1 (header rows 1) | 3 / 3 | 1 / 1 (`\o "1 - 9"`) | 5 / 5 | 167 / 167 | 0 | 92 / 46 | 0 / 0 | 0 / 0 | 3 |

Roadmap v2 §2.1/§2.2 rows re-measured with the same boundary rule: all confirmed (including Sample-5 `text:p` 218 and no TOC in Sample-1.odt). The ODT `text:sequence` counts equal the DOCX `SEQ` field counts (9 and 13), and the ODT `text:tab` counts equal the DOCX `w:tab` counts where the same producer wrote both sides (Sample-4: 35, Sample-6: 167).

Content the parser must survive but Plan 5 does not render: `m:oMath` (Sample-2: 2, Sample-5: 2), `mc:AlternateContent` (Sample-2: 2, Sample-4: 1), `w:sdt` (Sample-2: 2), `w:pict` (Sample-2: 2, Sample-4: 1), `draw:object` (Sample-2: 1, Sample-5: 1), footnotes (Sample-3: 1, Sample-4: 2, with `footnotes.xml` parts in both), endnotes parts in Sample-2/4/5/6 (empty separators), `stylesWithEffects.xml` (Sample-1/6), `docProps/custom.xml` (Sample-3), glossary part (Sample-2). Media: Sample-1 six JPEGs, Sample-2 ten, Sample-4 one PNG, Sample-5 one JPEG, Sample-6 three.

Image sizes are the same on both sides where the pair shares a producer lineage: Sample-1 `svg:width/height` = `wp:extent` to two decimals for all six (5.20 × 2.95 in, 6.27 × 1.98 in, 6.10 × 3.58 in, 6.27 × 1.06 in, 6.27 × 2.47 in, 5.09 × 5.05 in); Sample-6 likewise (3.05 × 3.05, 1.72 × 4.45, 2.41 × 4.91 in). Sample-2's DOCX has 12 drawings to the ODT's 11 frames (one `w:pict` twin). This is the fixture for PR 17's `ImageExtentParsingTest`.

---

## 7. Producer quirks a reader must survive

1. **Attribute order.** OnlyOffice writes `<style:style style:parent-style-name="style1" style:family="paragraph" style:name="P23">`. The SAX importer is order-independent; any regex in `OfficeDocumentParser.kt` that anchors on `style:name` first silently drops every automatic style in Sample-3/4/5.
2. **Identity lives in the display name.** Style ids are `style0`/`style1` (Sample-3), `987`/`988` (Sample-4), `para0`/`char1` (Sample-1/6), `Judul1` (Sample-2/5). Only `style:display-name` / `w:name` says "heading 1". Heading detection by id substring ("Heading", "Judul") in `LayoutDrivenDocumentRenderer.kt` misses Samples 1, 3, 4 and 6 in one direction or the other.
3. **Sample-3.odt has no `text:h` and no `style:default-outline-level`.** Its 27 headings are only recognisable through the parent style's display name. Sample-5's are the same shape with `Judul1`.
4. **`w:numId="0"`** on three Sample-6 headings and on Sample-4/5 paragraphs means "no numbering here" (ECMA-376 §17.9.18), not "list 0".
5. **Page breaks inside runs.** Sample-2/4/5/6 start chapter headings with `<w:r><w:br w:type="page"/></w:r>` before the bookmark and text; Sample-6's PENDAHULUAN heading starts with `w:br type="textWrapping"` (a blank line inside the heading, not a break).
6. **Fields.** Sample-1 has nine `SEQ` fields in `fldChar` wrappers and zero `fldSimple`; the instruction text arrives entity-escaped in Sample-4/6 (`&quot;`). Instruction text must never reach the body.
7. **Fonts named in `rFonts` but absent on Android:** Aptos, Aptos Display, Cambria Math, Courier New, Calibri, MS Mincho, Arial, Basic Sans, Segoe UI Variable, Microsoft YaHei UI, Times New Roman. `w:asciiTheme` is used by none of the six; theme major/minor is Times New Roman in Sample-1/2/4/5/6 and absent in Sample-3.
8. **OnlyOffice ODT carries no `office:font-face-decls`** in `styles.xml`; families come only from `fo:font-family` on the styles themselves (Sample-3/4/5). A reader that resolves families only through `style:font-name` finds nothing there.

---

## 8. Font substitution input for `FontRegistry` (PR 15 E-EN-5)

| Named in the corpus | Where | Bundled stand-in (roadmap §0 rule) | Note |
|---|---|---|---|
| Times New Roman | body of all six | Liberation Serif | metric-compatible |
| Aptos | `docDefaults`/`default-style` in all six (rarely reaches text: Normal overrides) | **decision needed**: no metric-compatible face is bundled; propose Carlito with the mapping recorded as "not metric-compatible" | Aptos reaches text only where a run has no family up the chain |
| Aptos Display | Word built-in headings (Sample-3 heading 1/2; orphaned Char styles elsewhere) | same decision as Aptos | affects Sample-3 only |
| Calibri | Sample-3 direct runs | Carlito | metric-compatible |
| Cambria Math | Sample-2/5 equation runs | Caladea | metric-compatible for Cambria; the math glyphs are not rendered in Plan 5 |
| Courier New | Sample-2 | Liberation Mono | metric-compatible |
| Arial | Sample-4 heading 8/9, Title | Liberation Sans | metric-compatible |
| Basic Sans, Segoe UI Variable, Microsoft YaHei UI, MS Mincho | `cs`/`eastAsia` slots and one Sample-4 style | Liberation Sans / system default | recorded fallback; none of them paints Latin body text in these files |

The registry resolves **one family per name for both measuring and painting**; the decision for Aptos is the only open choice and it is cosmetic for pagination (every body paragraph resolves to Times New Roman before Aptos is reached).

---

## 9. Code cross-check at `5c99072` (where each finding lands)

| Finding | Code today | File:line |
|---|---|---|
| A, B (body rect) | `PageStyleSpec` has margins only; `SvXMLImport` reads `page-layout-properties`, ignores `header-style`/`footer-style`; DOCX `extractDocxPageStyleSpec` regex reads the last `w:sectPr` `pgSz`/`pgMar` | `data/OfficeDocument.kt` (`PageStyleSpec`, `FALLBACK` 816 × 1056, margins 50/60/40/40), `data/odf/SvXMLImport.kt`, `data/OfficeDocumentParser.kt` |
| B (first master) | master page picked by name `Standard` | `PageGeometryTest.kt` (asserts `Standard → Mpm1`), importer master resolution |
| C (chain) | `extractDocxStyles` stores id/name/outlineLvl only, `basedOn` never populated; `w:b/i/u` are paragraph-wide flags, one `TextRun` per paragraph; no `w:rPr` size/font/colour, no `w:pPr` spacing/indent read | `data/OfficeDocumentParser.kt` DOCX branch |
| C, D (defaults) | `StyleResolver` default 14 sp, heading fallbacks 24/20/16 | `data/LayoutEngine.kt:62, 69-71, 77, 82-85` |
| E (fake breaks) | `w:lastRenderedPageBreak` → hard break + `--- Page Break ---`; `text:soft-page-break` → `PageBreak` | `data/OfficeDocumentParser.kt` shared parse block; `data/odf/SvXMLImportContext.kt` |
| F (spacing) | `ParagraphStyle` has no spacing/indent/line-height; `elementGapDp = 12f` between all elements; line height `textSize * 1.2f`; measuring at `fontSizeSp * 2.5f` | `data/OfficeDocument.kt`, `data/LayoutEngine.kt:107, 154, 331` |
| G (blank pages) | `flushPage()` guards on `currentPageElements.isNotEmpty()`; `OfficePageBreak` at index 0 adds an empty page 1 | `data/LayoutEngine.kt:302-354` |
| H.2 (heading by id) | style name contains "Heading"/"Judul" | `modules/inky/LayoutDrivenDocumentRenderer.kt:621-622` |
| Renderer scale split | text at `renderScale`, margins at `pageScale`; line height `(sizeSp + 5f) * zoom`; `PageStackMetrics.BASE_CARD_WIDTH_DP = 320` | `LayoutDrivenDocumentRenderer.kt:149-150, 243-245, 474…650` (roadmap §1.3 rows confirmed) |
| Open-sequence delays | `delay(500)`, `delay(500)`, `delay(400)` with `loading_status_*` strings | `modules/inky/InkyModule.kt:1121-1135` |

Tests that pin today's behaviour and must be re-worded, not deleted: `PageGeometryTest` (declared margins), `Sample5StyleFidelityTest.mappedHeadingStyleBeatsHeuristic` (asserts the 24/20/16 fallbacks explicitly; PR 16a replaces the expectation with the document default), `Sample5UnifiedPaginationTest` (window `12..30`, both formats; PR 16b tightens per format).

---

## 10. Native libraries (`app/src/main/libs`), corrected record

The checked-in `.so` files are Git LFS pointers (130 to 134 bytes each). The pointer sizes describe a full LibreOffice Android build with its NSS/NSPR dependency set, not a stub:

| Library | arm64-v8a | armeabi-v7a |
|---|---|---|
| `liblo-native-code.so` | **196,227,296** | **134,699,252** |
| `libc++_shared.so` | 1,374,336 | 963,028 |
| `libnss3.so` | 1,652,648 | 987,880 |
| `libsqlite3.so` | 1,275,288 | 711,556 |
| `libfreebl3.so` | 1,153,016 | 870,976 |
| `libssl3.so` | 696,288 | 445,744 |
| `libnssckbi.so` | 612,248 | 388,548 |
| `libsoftokn3.so` | 475,104 | 265,948 |
| `libnspr4.so` | 365,680 | 216,548 |
| `libnssutil3.so` | 268,864 | 159,464 |
| `libsmime3.so` | 237,840 | 136,524 |
| `libnssdbm3.so` | 199,648 | 107,616 |
| `libplc4.so` | 23,088 | 13,192 |
| `libplds4.so` | 14,528 | 8,372 |

`audit-005` (evidence base) and `audit-006` (rows on `isNativeAvailable` and the LOKit attribution) describe `liblo-native-code.so` as "60 KB per ABI, a stub". That figure is `ls` reporting `total 60` (sixty 1 KB blocks for the fourteen pointer files in a directory), not the size of any library; the pointers say 196 MB and 135 MB, and CI checks them out with `lfs: true`, so the APK ships the real files. The conclusion those rows drew from it ("all rendering comes from the Kotlin engine") therefore has no evidence either way from the repository: whether the native path loads is a runtime fact that `LokitEngine` reports as NATIVE or SIMULATED on the About screen, and the device pass should record which one it shows. What the binary exports (a `libreofficekit_hook` symbol, a LibreOffice version string) could not be checked from this sandbox: the LFS batch API answered with a download action, but the object host (`github-cloud.githubusercontent.com`) refuses the TLS handshake from here. Two honest options, for the user to pick: (1) a CI step in the `test` job that runs `readelf -h`, `nm -D | grep -c lok`, and `strings | grep -m1 LibreOffice` on the LFS-fetched library and prints the result into the run log; (2) skip, and keep describing the native path by its runtime behaviour only.

---

## 11. What this changes for the PR plan

### 11.1 PR 15 (Plan 5A), amendments to roadmap v2 §4.3

1. **`SampleMatrixTest` (new, pure JVM).** For all twelve files it asserts the declared page size, margins, header/footer heights, first master page (ODT) or section count (DOCX), body font/size, default line height and after-spacing, exactly as tabulated in §2 and §3, plus the reference counts of §1 as named constants with their provenance in a comment. It replaces the "truth" wording of `PageGeometryTest` and gives PR 16a/16b a fixed target for every file, not only Sample-5/6.
2. **E-0 dump on all six pairs.** For each file: body rectangle used, default metrics used, page count, the reference count from §1, and the number of pages holding fewer than three elements. Printed and asserted (the assertion is only "the dump ran and named the mechanism"; no window moves in PR 15).
3. **`PageStyleSpec` grows `headerHeightDp`/`footerHeightDp`** (fixed `svg:height`, else `fo:min-height`) as part of E-EN-3, populated by `SvXMLImport` for ODT and left 0 for DOCX (Word keeps them inside the margin). Defaults keep today's rendering byte-identical; PR 16a makes the paginator use them.

Everything else in §4.3 stands: `LayoutUnits`, `TextMetrics`, `FontRegistry` seam (with §8 as its input corpus), one render transform, zero pagination change, Sample-5 stays `12..30`.

### 11.2 PR 16 becomes PR 16a (Plan 5B) and PR 16b (Plan 5C)

**PR 16a, Plan 5B, "breaks and defaults" (parsing side, each commit visible in the dump):**

1. `w:lastRenderedPageBreak` and `text:soft-page-break` stop being breaks; the `--- Page Break ---` text goes (roadmap E-4 first half, plan-03 3.17).
2. `fo:break-before/after`, `w:br type="page"` inside runs, `w:pageBreakBefore`, section starts (`type=nextPage`) become one model fact; `keep-with-next`/`w:keepNext` recorded on the paragraph.
3. Body rectangle = margins + header/footer heights (finding B). Sample-4/5 ODT stop paginating against a 29.7 cm body.
4. **Metric-only style chain**, both formats: `docDefaults`/`default-style` → default paragraph style → `basedOn`/`parent-style-name` → paragraph style → direct `pPr`/paragraph properties, for **size, line height, before/after spacing, indents, keep/break flags only**. Run formatting, fonts, colours and the char-link rule stay in PR 20. Sample-4 (§3.3) is the acceptance case for the order.
5. F-21: the 14 sp default and the 24/20/16 heading fallbacks are replaced by the chain's result (finding D); `Sample5StyleFidelityTest.mappedHeadingStyleBeatsHeuristic` is re-worded to assert the document default.
6. Windows: **widened only where the dump says the current window is dishonest**, never tightened in this PR.

**PR 16b, Plan 5C, "metrics and windows" (measurement side):**

1. `TextMetrics` load-bearing: measure at `ptToUnits(size)` with the resolved family; line height = `max(ascent + descent, size × lineHeightFactor)`; the `2.5f` fudge, `fallbackTextSize`, `elementGapDp`, and the renderer's `(sizeSp + 5f)` all die (roadmap E-2/E-3).
2. Widows/orphans floor where declared (Sample-1/2/6).
3. Tab stops enough for line-count honesty: default tab distance (`style:tab-stop-distance`, `w:defaultTabStop`) and paragraph-level `w:tabs`/`style:tab-stops` (Sample-6 TOC has 167 tabs; wrong tab width changes its line count).
4. Windows per §11.3, asserted in `PaginationFidelityTest` for all six pairs.

### 11.3 Windows (staged, per format; rule = floor(ref × 0.8) .. ceil(ref × 1.2), except where roadmap §0 already fixed a tighter window)

| # | DOCX reference (M365) | DOCX window at PR 16b | ODT declared metrics vs twin | ODT window at PR 16b (provisional until the Collabora fixtures land) |
|---|---|---|---|---|
| 1 | 15 | **12..18** | same font, size, spacing, images; LO claims 9 | **9..18** (bridges the LO claim and the M365 count; replaced by the Collabora count) |
| 2 | 23 | **18..28** | metric twin at 12 pt (§3.2 correction); LO claims 18 | **15..22** |
| 3 | 20 | **16..24** | 0 cm top/bottom margins, single-spaced, 0 after | **12..20** |
| 4 | 10 | **8..12** | header-carried margin (equivalent), mixed spacing | **7..12** |
| 5 | 18 | **15..21** (roadmap §0, kept) | header-carried margin (equivalent), single-spaced, 0 after | **12..21** (replaces "15..21 both formats") |
| 6 | 21 | **15..26** (roadmap §0, kept) | metric-identical twin | **15..26** (kept) |

The ±10 % tightening commit stays where roadmap §0 put it (PR 21 or 22). When the user delivers the Collabora-regenerated `.odt` files, the ODT column is re-derived from Collabora's own page count with the same rule, and finding B/F stop applying to Sample-3/4/5.

### 11.4 What does not change

PR 17 to 22 scope and order; the binding decisions in roadmap §0 other than the per-format wording of the window row; Plan 10 parked; Plan 11's UI packages remain a separate track (they touch chrome, not the layout path) and still need a Compose BOM bump PR of their own before any Material 3 Expressive component can be used (BOM `2024.09.00` = Material3 1.3.0 today; no Expressive API is referenced anywhere in `app/src/main/java`).

---

## 12. Open items and their resolution

Resolved by the user on 2026-09-26 (before PR 15 code):

1. **Aptos stand-in** (§8): **Martel Sans** (Google Fonts, SIL OFL 1.1). Regular and Bold ship in `assets/fonts`; the family has no italic face. `FontRegistry` records the choice as a stand-in that is not metric-compatible.
2. **Native inventory in CI** (§10): **added**. `scripts/native-inventory.sh` runs in the `test` job with `continue-on-error`; its output is in the job log and in the `unit-test-reports` artifact.
3. **Sample-1 ODT**: **keep** the bridging window `9..18` until the Collabora count exists.

Still open:

4. **Collabora regeneration**: when it happens, drop the six new `.odt` files in place with the same names and record the six status-bar page counts in §1; `SampleMatrix.references`, the `SampleMatrixTest` ODT rows and the §11.3 ODT column are then updated in one commit.
5. **Engine status on the device**: at the next device pass, note whether the About screen reports NATIVE or SIMULATED (§10); the CI inventory (§12.1) says what the binary exports, the device says whether it loads.

### 12.1 PR 15 evidence from CI

Source: GitHub Actions run 36230161495 on PR #15 (`Plan5ElementDumpTest` system-out and the native inventory step, mirrored into the PR comment by `scripts/ci-dump-comment.py`); 191 unit tests, 0 failures. The numbers are what the paginator does **today**, under Robolectric, with the page box each file declares (`defaultPageStyle`, else the Letter fallback). They are the baseline PR 16a/16b move from, not targets.

| file | pages | ref | window | thin (<3) | break elements in model | blank paragraphs | mechanism |
|---|---|---|---|---|---|---|---|
| Sample-1.odt | 16 | (LO 9) | 9..18 | 0 | 7 | 0 | no thin pages |
| Sample-1.docx | 17 | 15 | 12..18 | 0 | 0 | 0 | no thin pages |
| Sample-2.odt | 22 | (LO 18) | 15..22 | 0 | 7 | 0 | no thin pages |
| Sample-2.docx | **38** | 23 | 18..28 | **9** | **22** | 0 | break elements (8 of 9 thin pages end at a break element) |
| Sample-3.odt | **9** | n/a | 12..20 | 0 | 0 | 0 | no thin pages (0 cm top/bottom margins honoured: flow 0..1122.5) |
| Sample-3.docx | 15 | 20 | 16..24 | 0 | 0 | 0 | no thin pages |
| Sample-4.odt | 7 | n/a | 7..12 | 0 | 0 | **43** | no thin pages |
| Sample-4.docx | 8 | 10 | 8..12 | 0 | 0 | 0 | no thin pages |
| Sample-5.odt | 12 | n/a | 12..21 | 0 | 0 | 3 | no thin pages |
| Sample-5.docx | 18 | 18 | 15..21 | 2 | 3 | 0 | break elements (1 of 2 thin pages ends at a break element) |
| Sample-6.odt | 21 | n/a | 15..26 | 0 | 0 | 0 | no thin pages |
| Sample-6.docx | 27 | 21 | 15..26 | 0 | 0 | 0 | no thin pages |

What the dump established beyond the counts:

1. **CI measures nothing.** Every file prints `measurement: Paint at 35.0 px for 14 pt: MMMM=4.0 iiii=4.0 (per-character stub)`: Robolectric's legacy graphics answer one unit per character, so no paragraph ever wraps on CI. Each paragraph is one line of 52.5 units plus the 12-unit gap; 14 of them make the `reserved` 891.0 printed on every full page of every file. **Every CI page count in this table, including the 12..30 guard for Sample-5, is paragraph-count arithmetic plus break elements, not text layout.** PR 16b must run the paginator on `TextMetrics` with `TableAdvanceSource` under Robolectric (the `measuresRealGlyphs()` probe already selects it) before any window can mean anything; on device, `PaintAdvanceSource` measures real glyphs.
2. **Defaults are not read from the file.** `default body style: 14.0 pt (no family) line factor 1.00` for all twelve files: `StyleResolver` never sees `default-style`/`docDefaults` (Aptos 12 pt) nor `Standard`/`Normal` (Times New Roman). PR 16a item "defaults from file".
3. **Sample-2.docx is the fake-break case.** 22 break elements (its 22 `w:lastRenderedPageBreak`) produce 38 pages, nine of them with one or two paragraphs; 8 of those 9 end at a break element. Sample-5.docx has 3 (its `lastRenderedPageBreak` count). PR 16a item "fake breaks out" has its before-number.
4. **Sample-1/2.odt** carry 7 break elements each in the model against 8 and 10 `text:soft-page-break` in the XML plus 0 and 1 `fo:break-before` (the reader drops breaks that fall at index 0 or collapse, and does not read `fo:break-before` at all); Sample-4/5/6.odt carry 0 against 4/4/2 at parse level. PR 16a item "real breaks in".
5. **Sample-4.odt has 43 blank paragraphs** (empty `text:p` used as vertical spacing by the producer); each costs a full one-line paragraph today (52.5 units plus the 12-unit gap, the same as a paragraph with text). Spacing semantics are PR 16b's.
6. **ODT body rectangle:** for Sample-4/5.odt the paginator flows from top 0.0 (declared margin of `Mpm1`), while the body's first master page (`MasterPage2` -> `Mpm2`) has body top 96.0 through its 2.54 cm header (`PageGeometryTest` pins it). PR 16a item "body rect incl. header/footer + first-master rule".
7. **Sample-3.odt** at 9 pages is the file's own 0 cm top/bottom margins honoured (finding B in §2.2); it stays out of any window until the Collabora fixture replaces it.

Native inventory (same run, `scripts/native-inventory.sh`; full text in the `unit-test-reports` artifact):

* `liblo-native-code.so` is 196 227 296 bytes (arm64-v8a, ELF64 AArch64, 4989 exported symbols) and 134 699 252 bytes (armeabi-v7a, ELF32 ARM, 4991). NEEDED: the bundled NSS/NSPR/sqlite chain plus `libGLESv2`, `libandroid`, `libjnigraphics`, `liblog`, `libz`, `libc++_shared`, `libm`, `libdl`, `libc`; all resolvable on device, so `System.loadLibrary` succeeds.
* It is a **LibreOfficeKit** build: JNI exports are `Java_org_libreoffice_kit_Office_*`, `Java_org_libreoffice_kit_Document_*`, `Java_org_libreoffice_kit_LibreOfficeKit_*`, `Java_org_libreoffice_android_Bootstrap_*`, `Java_com_sun_star_*`, plus `libreofficekit_hook`, `libreofficekit_hook_2`, `libreofficekit_initialize`, `libreofficekit_set_javavm`.
* **None of the five `external fun` declarations in `LibreOfficeCore.kt` has a symbol** (`Java_com_example_core_jni_LibreOfficeCore_nativeInitialize`, `nativeRenderPage`, `nativeEvaluateFormula`, `nativeRegisterCallback`, `nativeCreateDocument` all MISSING). Consequence: the loader reports the library as loaded (the About screen's NATIVE), while every call throws `UnsatisfiedLinkError` and takes the catch branch that returns the simulated result. Nothing crashes; nothing native runs either. This is the answer to open item 5 as far as a binary can give it; the device pass only confirms the loader half. Fixing the seam (binding to `org.libreoffice.kit.*` or building the expected symbols) is a separate plan, not Plan 5.

---

## Appendix A. How each column was measured

Regexes were run over the unzipped `content.xml`/`styles.xml`/`meta.xml` (ODT) and `word/document.xml`/`word/styles.xml`/`word/numbering.xml`/`docProps/app.xml` (DOCX). Counts are `len(re.findall(pattern, xml))` unless stated.

| Column | Pattern |
|---|---|
| ODT elements | `<text:h[ />]`, `<text:p[ />]`, `<text:list[ />]`, `<text:span[ />]`, `<text:a [^>]*xlink:href`, `<table:table[ />]`, `<draw:frame[ />]`, `<text:table-of-content[ />]`, `<text:section[ />]`, `<text:soft-page-break`, `<text:tab/>`, `<text:line-break/>`, `<text:bookmark(-start\|-end)?[ />]`, `<text:note[ />]`, `<text:sequence[ />]`, `<draw:object[ />]` |
| ODT geometry | `<style:page-layout style:name="…">` bodies, then `fo:page-width`, `fo:page-height`, `fo:margin-*` on `page-layout-properties`, `svg:height`/`fo:min-height` on `header-style`/`footer-style`; master pages from `<style:master-page …>`; body references from `style:master-page-name="…"` in `content.xml` |
| ODT defaults | `<style:default-style style:family="paragraph">` and the style whose `style:display-name` is `Normal` or whose `style:name` is `Standard`; `fo:font-size`, `fo:font-family`, `fo:line-height`, `fo:margin-bottom`, `fo:text-indent`, `fo:widows`, `fo:orphans` |
| DOCX elements | `<w:p[ >]`, `<w:r[ >]`, `<w:tbl>`, `<w:drawing>`, `<w:hyperlink`, `<w:sectPr`, `<w:numPr>`, `<w:br w:type="page"`, `<w:lastRenderedPageBreak`, `<w:pStyle`, `<w:instrText`, `<w:fldSimple`, `<w:fldChar`, `<w:tab/>`, `<w:tabs>`, `<w:bookmarkStart`, `<w:footnoteReference`, `<w:sz `, `<w:rFonts`, `<w:spacing `, `<w:ind `, `<w:jc `, `<w:b/>`, `<w:i/>`, `<w:u `, `<w:vertAlign`, `<w:color `, `<w:pageBreakBefore`, `<w:keepNext`, `<wp:inline`, `<wp:anchor`, `<wp:extent`, `<mc:AlternateContent`, `<w:sdt>`, `<w:pict>`, `<m:oMath>` |
| DOCX geometry | every `<w:sectPr …>…</w:sectPr>` body: `<w:pgSz …/>`, `<w:pgMar …/>`, `<w:pgNumType …/>`, `<w:type w:val="…"/>`, `<w:titlePg/>`, `<w:(header\|footer)Reference w:type="…"` |
| DOCX defaults | `<w:docDefaults>…</w:docDefaults>`; `<w:style w:type="paragraph" w:default="1" …>` or, when absent (TextMaker), the style named `Normal`; heading styles by `<w:name w:val="heading N"/>` and `<w:outlineLvl` |
| numbering | `<w:abstractNum`, `<w:num w:numId=`, `<w:lvlText w:val="…"/>`, `<w:numFmt w:val="…"/>` |
| provenance | `<meta:generator>`, `meta:page-count`, `meta:word-count` (ODT); `<Application>`, `<Pages>`, `<Words>` in `docProps/app.xml` (DOCX) |
| native libraries | `size` line of each Git LFS pointer under `app/src/main/libs/<abi>/` |
