package com.example

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * Pins the twelve `tests/inky` fixtures exactly as audit-007 §2 and §3
 * tabulate them, straight from the package XML with no parser in between
 * (pure JVM). If a fixture is regenerated (the Collabora `.odt` set) or a
 * table in the audit was wrong, this is the test that says so, and the
 * parser tests (`PageGeometryTest`, `Plan5ElementDumpTest`) stay about the
 * parser. Reference page counts and windows live in [SampleMatrix].
 *
 * Every mismatch in a format is collected and reported in one failure so a
 * single CI run shows the whole picture.
 */
class SampleMatrixTest {

    // ---- expectations -----------------------------------------------------

    /** `w:sectPr` (last), `w:styles` defaults and break inventory of one DOCX (audit-007 §2.1, §3.1, §5). */
    data class DocxRow(
        val sample: Int,
        val pgW: Int, val pgH: Int,
        val top: Int, val bottom: Int, val left: Int, val right: Int,
        val headerDistance: Int, val footerDistance: Int,
        val sections: Int,
        val defaultFont: String, val defaultSz: Int,
        val defaultAfter: Int?, val defaultLine: Int?, val defaultFirstLine: Int?,
        val normalId: String, val normalFont: String, val normalSz: Int?,
        val appPages: Int?,
        val lastRenderedBreaks: Int, val hardBreaks: Int
    )

    /** Master pages, the layout the body starts on, and paragraph defaults of one ODT (audit-007 §2.2, §3.2, §5). */
    data class OdtRow(
        val sample: Int,
        val masters: Map<String, String>,
        /** `style:master-page-name` reached from the first body paragraph's style chain; null = ODF default "Standard". */
        val firstMaster: String?,
        /** Page layout behind [firstMaster] (or "Standard"). */
        val layout: String,
        val pageW: String, val pageH: String,
        val top: String, val bottom: String, val left: String, val right: String,
        val headerHeight: String?, val headerMinHeight: String?,
        val footerHeight: String?, val footerMinHeight: String?,
        val defaultFont: String, val defaultSize: String, val defaultLine: String?,
        val normalName: String, val normalFont: String, val normalSize: String?, val normalLine: String?, val normalAfter: String?,
        val metaPages: Int?,
        val softBreaks: Int, val breakBefore: Int
    )

    private val docxRows = listOf(
        DocxRow(1, 11907, 16840, 1440, 1440, 1440, 1440, 0, 567, 1, "Aptos", 24, 160, 276, null, "para0", "Times New Roman", 22, null, 0, 0),
        DocxRow(2, 11907, 16840, 1440, 1440, 1440, 1440, 0, 567, 5, "Aptos", 24, 160, 278, null, "Normal", "Times New Roman", null, 23, 22, 2),
        DocxRow(3, 11906, 16838, 1440, 1440, 1440, 1440, 0, 0, 1, "Aptos", 24, 160, 278, null, "style0", "Times New Roman", null, null, 0, 0),
        DocxRow(4, 11906, 16838, 1440, 1440, 1440, 1440, 0, 567, 6, "Aptos", 24, 0, 360, 720, "987", "Times New Roman", null, null, 0, 2),
        DocxRow(5, 11907, 16840, 1440, 1440, 1440, 1440, 0, 567, 5, "Aptos", 24, 160, 278, null, "Normal", "Times New Roman", null, 18, 3, 2),
        DocxRow(6, 11907, 16840, 1440, 1440, 1440, 1440, 0, 567, 5, "Aptos", 24, 160, 278, null, "para0", "Times New Roman", null, null, 0, 2)
    )

    private val odtRows = listOf(
        OdtRow(
            1, mapOf("Standard" to "Mpm1"), "Standard", "Mpm1",
            "8.2681in", "11.6929in", "1in", "0.3937in", "1in", "1in",
            null, null, null, "0.6063in",
            "Aptos", "12pt", null,
            "Standard", "Times New Roman", "11pt", "115%", "0.111in",
            9, 8, 0
        ),
        OdtRow(
            2, mapOf(
                "Standard" to "Mpm1", "First_20_Page" to "Mpm2",
                "Converted1" to "Mpm3", "Converted2" to "Mpm3", "Converted3" to "Mpm3", "Converted4" to "Mpm3"
            ), "First_20_Page", "Mpm2",
            "8.2681in", "11.6929in", "1in", "1in", "1in", "1in",
            null, null, null, null,
            "Aptos", "12pt", null,
            // Correction to audit-007 §1/§3.2: Standard carries no fo:font-size, so the body is 12 pt from default-style.
            "Standard", "Times New Roman", null, "115%", "0.111in",
            18, 10, 1
        ),
        OdtRow(
            3, mapOf("Standard" to "Mpm1", "MasterPage2" to "Mpm2"), "MasterPage2", "Mpm2",
            "21cm", "29.7cm", "0cm", "0cm", "2.54cm", "2.54cm",
            null, null, null, null,
            "Aptos", "12pt", "100%",
            "style0", "Times New Roman", "12pt", "100%", null,
            null, 0, 0
        ),
        OdtRow(
            4, mapOf(
                "Standard" to "Mpm1", "MasterPage2" to "Mpm2", "MasterPage3" to "Mpm3", "MasterPage4" to "Mpm4",
                "MasterPage5" to "Mpm5", "MasterPage6" to "Mpm6", "MasterPage7" to "Mpm7"
            ), "MasterPage2", "Mpm2",
            "21cm", "29.7cm", "0cm", "1cm", "2.54cm", "2.54cm",
            "2.54cm", "0cm", null, "1cm",
            "Aptos", "12pt", "100%",
            "987", "Times New Roman", "12pt", "100%", null,
            null, 2, 2
        ),
        OdtRow(
            5, mapOf(
                "Standard" to "Mpm1", "MasterPage2" to "Mpm2", "MasterPage3" to "Mpm3",
                "MasterPage4" to "Mpm4", "MasterPage5" to "Mpm5", "MasterPage6" to "Mpm6"
            ), "MasterPage2", "Mpm2",
            "21cm", "29.71cm", "0cm", "1cm", "2.54cm", "2.54cm",
            "2.54cm", "0cm", null, "1cm",
            "Aptos", "12pt", "100%",
            "Normal", "Times New Roman", "12pt", null, null,
            null, 2, 2
        ),
        OdtRow(
            6, mapOf(
                "Standard_Next" to "pm1", "Standard" to "pm1_f",
                "Chapter2" to "pm2", "Chapter3" to "pm3", "Chapter4" to "pm4", "Chapter5" to "pm5"
            ), null, "pm1_f",
            "21.003cm", "29.704cm", "2.540cm", "2.540cm", "2.540cm", "2.540cm",
            null, null, null, null,
            "Times New Roman", "12pt", "116%",
            "Normal", "Times New Roman", "12pt", "116%", "0.282cm",
            null, 0, 2
        )
    )

    // ---- tests --------------------------------------------------------------

    @Test
    fun docxFixturesMatchAuditTables() {
        val problems = ArrayList<String>()
        for (row in docxRows) {
            val name = SampleMatrix.docxName(row.sample)
            val file = SampleMatrix.findTestFile(name)
            assertTrue("$name must exist (${file.absolutePath})", file.exists() && file.length() > 0)
            val document = entry(file, "word/document.xml")
            val styles = entry(file, "word/styles.xml")
            if (document == null || styles == null) {
                problems += "$name: word/document.xml or word/styles.xml missing"
                continue
            }
            val app = entry(file, "docProps/app.xml").orEmpty()
            val check = Checker(name, problems)

            val sections = Regex("<w:sectPr\\b[^>]*>.*?</w:sectPr>", RegexOption.DOT_MATCHES_ALL).findAll(document).map { it.value }.toList()
            check.eq("sections", row.sections, sections.size)
            val last = sections.lastOrNull().orEmpty()
            val pgSz = Regex("<w:pgSz\\b[^>]*/?>").find(last)?.value.orEmpty()
            check.eq("pgSz w", row.pgW, intAttr(pgSz, "w:w"))
            check.eq("pgSz h", row.pgH, intAttr(pgSz, "w:h"))
            val pgMar = Regex("<w:pgMar\\b[^>]*/?>").find(last)?.value.orEmpty()
            check.eq("pgMar top", row.top, intAttr(pgMar, "w:top"))
            check.eq("pgMar bottom", row.bottom, intAttr(pgMar, "w:bottom"))
            check.eq("pgMar left", row.left, intAttr(pgMar, "w:left"))
            check.eq("pgMar right", row.right, intAttr(pgMar, "w:right"))
            check.eq("pgMar header", row.headerDistance, intAttr(pgMar, "w:header"))
            check.eq("pgMar footer", row.footerDistance, intAttr(pgMar, "w:footer"))

            val rPrDefault = Regex("<w:rPrDefault>.*?</w:rPrDefault>", RegexOption.DOT_MATCHES_ALL).find(styles)?.value.orEmpty()
            val pPrDefault = Regex("<w:pPrDefault>.*?</w:pPrDefault>", RegexOption.DOT_MATCHES_ALL).find(styles)?.value.orEmpty()
            check.eq("docDefaults font", row.defaultFont, attr(rPrDefault, "w:ascii"))
            check.eq("docDefaults sz", row.defaultSz, intAttr(Regex("<w:sz\\b[^>]*/?>").find(rPrDefault)?.value.orEmpty(), "w:val"))
            check.eq("docDefaults after", row.defaultAfter, intAttr(pPrDefault, "w:after"))
            check.eq("docDefaults line", row.defaultLine, intAttr(pPrDefault, "w:line"))
            check.eq("docDefaults firstLine", row.defaultFirstLine, intAttr(pPrDefault, "w:firstLine"))

            val normal = Regex("<w:style\\b([^>]*)>(.*?)</w:style>", RegexOption.DOT_MATCHES_ALL).findAll(styles).firstOrNull { m ->
                attr(m.groupValues[1], "w:type") == "paragraph" && attr(m.groupValues[1], "w:default") == "1"
            }
            if (normal == null) {
                problems += "$name: no default paragraph style"
            } else {
                check.eq("Normal styleId", row.normalId, attr(normal.groupValues[1], "w:styleId"))
                check.eq("Normal font", row.normalFont, attr(normal.groupValues[2], "w:ascii"))
                check.eq("Normal sz", row.normalSz, intAttr(Regex("<w:sz\\b[^>]*/?>").find(normal.groupValues[2])?.value.orEmpty(), "w:val"))
            }

            check.eq("app.xml Pages", row.appPages, Regex("<Pages>(\\d+)</Pages>").find(app)?.groupValues?.get(1)?.toIntOrNull())
            check.eq("lastRenderedPageBreak", row.lastRenderedBreaks, Regex("<w:lastRenderedPageBreak\\b").findAll(document).count())
            check.eq("br type=page", row.hardBreaks, Regex("<w:br w:type=\"page\"").findAll(document).count())
            check.eq("reference page count", SampleMatrix.references.getValue(row.sample).docxPages, SampleMatrix.referenceFor(name))
        }
        if (problems.isNotEmpty()) fail("DOCX fixtures differ from audit-007:\n" + problems.joinToString("\n"))
    }

    @Test
    fun odtFixturesMatchAuditTables() {
        val problems = ArrayList<String>()
        for (row in odtRows) {
            val name = SampleMatrix.odtName(row.sample)
            val file = SampleMatrix.findTestFile(name)
            assertTrue("$name must exist (${file.absolutePath})", file.exists() && file.length() > 0)
            val stylesXml = entry(file, "styles.xml")
            val contentXml = entry(file, "content.xml")
            if (stylesXml == null || contentXml == null) {
                problems += "$name: styles.xml or content.xml missing"
                continue
            }
            val metaXml = entry(file, "meta.xml").orEmpty()
            val check = Checker(name, problems)

            // Master pages: name -> page layout (styles.xml only).
            val masters = LinkedHashMap<String, String>()
            for (m in Regex("<style:master-page\\b[^>]*>").findAll(stylesXml)) {
                val n = attr(m.value, "style:name") ?: continue
                val layout = attr(m.value, "style:page-layout-name") ?: continue
                masters[n] = layout
            }
            check.eq("master pages", row.masters, masters.toMap())

            // First body paragraph's master page through its style chain.
            val styleTags = LinkedHashMap<String, Pair<String?, String?>>()
            for (m in STYLE_ELEMENT.findAll(contentXml + stylesXml)) {
                val head = m.groupValues[1]
                val n = attr(head, "style:name") ?: continue
                if (!styleTags.containsKey(n)) {
                    styleTags[n] = attr(head, "style:master-page-name") to attr(head, "style:parent-style-name")
                }
            }
            val body = Regex("<office:text\\b.*?</office:text>", RegexOption.DOT_MATCHES_ALL).find(contentXml)?.value.orEmpty()
            val firstParagraph = Regex("<text:(?:p|h)\\b[^>]*>").find(body)?.value.orEmpty()
            var current: String? = attr(firstParagraph, "text:style-name")
            var firstMaster: String? = null
            var depth = 0
            while (current != null && depth < 10) {
                val (master, parent) = styleTags[current] ?: break
                if (!master.isNullOrBlank()) { firstMaster = master; break }
                current = parent
                depth++
            }
            check.eq("first master page", row.firstMaster, firstMaster)
            check.eq("layout of first master", row.layout, masters[row.firstMaster ?: "Standard"])

            // The page layout the body starts on.
            val layout = Regex("<style:page-layout(?=[ >])[^>]*style:name=\"" + Regex.escape(row.layout) + "\"[^>]*>(.*?)</style:page-layout>", RegexOption.DOT_MATCHES_ALL)
                .find(stylesXml)?.groupValues?.get(1)
            if (layout == null) {
                problems += "$name: page layout ${row.layout} not found"
            } else {
                val props = Regex("<style:page-layout-properties\\b[^>]*/?>").find(layout)?.value.orEmpty()
                check.eq("page-width", row.pageW, attr(props, "fo:page-width"))
                check.eq("page-height", row.pageH, attr(props, "fo:page-height"))
                check.eq("margin-top", row.top, attr(props, "fo:margin-top"))
                check.eq("margin-bottom", row.bottom, attr(props, "fo:margin-bottom"))
                check.eq("margin-left", row.left, attr(props, "fo:margin-left"))
                check.eq("margin-right", row.right, attr(props, "fo:margin-right"))
                val header = Regex("<style:header-style>\\s*<style:header-footer-properties\\b([^>]*)/?>").find(layout)?.groupValues?.get(1)
                val footer = Regex("<style:footer-style>\\s*<style:header-footer-properties\\b([^>]*)/?>").find(layout)?.groupValues?.get(1)
                check.eq("header svg:height", row.headerHeight, header?.let { attr(it, "svg:height") })
                check.eq("header fo:min-height", row.headerMinHeight, header?.let { attr(it, "fo:min-height") })
                check.eq("footer svg:height", row.footerHeight, footer?.let { attr(it, "svg:height") })
                check.eq("footer fo:min-height", row.footerMinHeight, footer?.let { attr(it, "fo:min-height") })
            }

            // Paragraph defaults: default-style, then Standard / "Normal".
            val defaultStyle = Regex("<style:default-style style:family=\"paragraph\">(.*?)</style:default-style>", RegexOption.DOT_MATCHES_ALL)
                .find(stylesXml)?.groupValues?.get(1).orEmpty()
            val defaultText = Regex("<style:text-properties\\b[^>]*>").find(defaultStyle)?.value.orEmpty()
            val defaultPara = Regex("<style:paragraph-properties\\b[^>]*>").find(defaultStyle)?.value.orEmpty()
            check.eq("default-style font", row.defaultFont, attr(defaultText, "style:font-name") ?: attr(defaultText, "fo:font-family"))
            check.eq("default-style size", row.defaultSize, attr(defaultText, "fo:font-size"))
            check.eq("default-style line-height", row.defaultLine, attr(defaultPara, "fo:line-height"))

            val normal = STYLE_ELEMENT.findAll(stylesXml).firstOrNull { m ->
                val head = m.groupValues[1]
                attr(head, "style:family") == "paragraph" && (
                    attr(head, "style:name") == "Standard" || attr(head, "style:name") == "Normal" ||
                        attr(head, "style:display-name") == "Normal"
                    )
            }
            if (normal == null) {
                problems += "$name: no Standard/Normal paragraph style"
            } else {
                val inner = normal.groupValues[3]
                val text = Regex("<style:text-properties\\b[^>]*>").find(inner)?.value.orEmpty()
                val para = Regex("<style:paragraph-properties\\b[^>]*>").find(inner)?.value.orEmpty()
                check.eq("Normal name", row.normalName, attr(normal.groupValues[1], "style:name"))
                check.eq("Normal font", row.normalFont, attr(text, "style:font-name") ?: attr(text, "fo:font-family"))
                check.eq("Normal size", row.normalSize, attr(text, "fo:font-size"))
                check.eq("Normal line-height", row.normalLine, attr(para, "fo:line-height"))
                check.eq("Normal margin-bottom", row.normalAfter, attr(para, "fo:margin-bottom"))
            }

            check.eq("meta page-count", row.metaPages, Regex("meta:page-count=\"(\\d+)\"").find(metaXml)?.groupValues?.get(1)?.toIntOrNull())
            check.eq("soft-page-break", row.softBreaks, Regex("<text:soft-page-break\\b").findAll(contentXml).count())
            check.eq("fo:break-before=page", row.breakBefore, Regex("fo:break-before=\"page\"").findAll(contentXml + stylesXml).count())
            check.eq("reference page count", SampleMatrix.references.getValue(row.sample).odtPages, SampleMatrix.referenceFor(name))
        }
        if (problems.isNotEmpty()) fail("ODT fixtures differ from audit-007:\n" + problems.joinToString("\n"))
    }

    @Test
    fun windowsContainTheirReferenceCounts() {
        // A window that excludes its own reference count would be a target nobody can hit.
        for (sample in SampleMatrix.sampleNumbers) {
            val ref = SampleMatrix.references.getValue(sample)
            val docx = SampleMatrix.docxWindows.getValue(sample)
            assertTrue("Sample-$sample DOCX window $docx must contain reference ${ref.docxPages}", ref.docxPages in docx)
            val odt = SampleMatrix.odtWindows.getValue(sample)
            assertTrue("Sample-$sample ODT window $odt must be non-empty", odt.min <= odt.max)
        }
    }

    // ---- helpers ------------------------------------------------------------

    private class Checker(private val name: String, private val problems: MutableList<String>) {
        fun eq(what: String, expected: Any?, actual: Any?) {
            if (expected != actual) problems += "$name: $what expected <$expected> but was <$actual>"
        }
    }

    private fun entry(file: File, path: String): String? = ZipFile(file).use { zip ->
        val e = zip.getEntry(path) ?: return null
        zip.getInputStream(e).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun attr(tag: String, attribute: String): String? =
        Regex("(?:^|\\s)" + Regex.escape(attribute) + "=\"([^\"]*)\"").find(tag)?.groupValues?.get(1)

    private fun intAttr(tag: String, attribute: String): Int? = attr(tag, attribute)?.toIntOrNull()

    private companion object {
        /** `<style:style ...>...</style:style>` or self-closing; group 1 = attributes, group 3 = inner XML. */
        val STYLE_ELEMENT = Regex("<style:style(?=[ >/])([^>]*?)(/>|>(.*?)</style:style>)", RegexOption.DOT_MATCHES_ALL)
    }
}
