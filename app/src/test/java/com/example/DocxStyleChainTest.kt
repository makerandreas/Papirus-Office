package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.OfficeRuns
import com.makerandreas.papirusoffice.data.StyleResolver
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Plan 8A commit 1 gate: the DOCX style chain, its linked character styles,
 * the run-property toggles and the per-paragraph numbering state.
 *
 * Every case builds a small `word/styles.xml` inside the test and parses it
 * through [OfficeDocumentParser], so the assertion is about the rule the file
 * declares rather than about a checked-in fixture (audit-019 section 4.2,
 * fixture independence). The six Inky fixtures keep their own regression
 * assertions in [StyleChainMetricsTest] and `PaginationFidelityTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocxStyleChainTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val wNs = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    /** One `w:style` exactly as the file states it; every part is optional. */
    private fun style(
        id: String,
        type: String = "paragraph",
        name: String? = null,
        basedOn: String? = null,
        link: String? = null,
        rPr: String = "",
        pPr: String = ""
    ): String = buildString {
        append("<w:style w:type=\"$type\" w:styleId=\"$id\">")
        append("<w:name w:val=\"${name ?: id}\"/>")
        if (basedOn != null) append("<w:basedOn w:val=\"$basedOn\"/>")
        if (link != null) append("<w:link w:val=\"$link\"/>")
        if (pPr.isNotEmpty()) append("<w:pPr>$pPr</w:pPr>")
        if (rPr.isNotEmpty()) append("<w:rPr>$rPr</w:rPr>")
        append("</w:style>")
    }

    /** A `w:rPr` in `w:docDefaults`. */
    private fun defaults(rPr: String): String = "<w:rPrDefault><w:rPr>$rPr</w:rPr></w:rPrDefault>"

    private fun paragraph(pStyle: String? = null, pPr: String = "", text: String = "Text"): String =
        "<w:p><w:pPr>" +
            (pStyle?.let { "<w:pStyle w:val=\"$it\"/>" } ?: "") +
            "$pPr</w:pPr><w:r><w:t>$text</w:t></w:r></w:p>"

    private fun docx(
        styles: String = "",
        body: String = paragraph(),
        docDefaults: String = ""
    ): OfficeParsedDocument = runBlocking {
        val file = File.createTempFile("docx-style-chain-", ".docx", context.cacheDir)
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                fun part(name: String, xml: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(xml.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                part(
                    "word/styles.xml",
                    "<w:styles xmlns:w=\"$wNs\"><w:docDefaults>$docDefaults</w:docDefaults>$styles</w:styles>"
                )
                part("word/document.xml", "<w:document xmlns:w=\"$wNs\"><w:body>$body</w:body></w:document>")
            }
            OfficeDocumentParser(context).parseDocument(file, bypassCache = true).also {
                assertFalse(it.failureReason, it.isParsingFailed)
            }
        } finally {
            OfficeDocumentParser.clearCacheForFile(file.absolutePath)
            file.delete()
        }
    }

    private fun paragraphs(parsed: OfficeParsedDocument) =
        parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>()

    private fun headings(parsed: OfficeParsedDocument) =
        parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>()

    /** Bare hex on the DOCX side, with or without a leading `#`. */
    private fun hex(value: String?) = value?.removePrefix("#")?.uppercase()

    @Test
    fun docDefaultsSupplyOnlyWhatNoLevelSets() {
        val parsed = docx(
            docDefaults = defaults("<w:sz w:val=\"24\"/><w:rFonts w:ascii=\"Aptos\"/><w:b/>"),
            styles = style("Base") + style("Child", basedOn = "Base", rPr = "<w:sz w:val=\"28\"/>"),
            body = paragraph("Base") + paragraph("Child")
        )

        val base = parsed.styles.paragraphStyles.getValue("Base")
        assertEquals(12f, base.fontSizeSp, 0.01f)
        assertEquals("Aptos", base.fontFamily)
        assertTrue(base.isBold)

        val child = parsed.styles.paragraphStyles.getValue("Child")
        assertEquals(14f, child.fontSizeSp, 0.01f)
        assertTrue("inherited bold must survive the child", child.isBold)
        assertEquals("Base", child.parentStyleName)
        assertEquals(12f, parsed.styles.defaultParagraphStyle!!.fontSizeSp, 0.01f)
    }

    @Test
    fun complexScriptSizeStandsInOnlyWhenNoAsciiSizeExists() {
        val parsed = docx(
            docDefaults = defaults("<w:szCs w:val=\"32\"/><w:rFonts w:ascii=\"Aptos\"/>"),
            styles = style("Body"),
            body = paragraph("Body")
        )

        assertEquals(16f, parsed.styles.paragraphStyles.getValue("Body").fontSizeSp, 0.01f)
    }

    @Test
    fun togglesAreTriStateAcrossDefaultsAndOwnProperties() {
        val parsed = docx(
            docDefaults = defaults("<w:b/><w:i/><w:u w:val=\"single\"/><w:vanish/>"),
            styles = style("Inherits") +
                style("Off", rPr = "<w:b w:val=\"0\"/><w:i w:val=\"false\"/><w:u w:val=\"none\"/><w:vanish w:val=\"0\"/>") +
                style("DoubleUnderline", rPr = "<w:u w:val=\"double\"/>"),
            body = paragraph("Inherits") + paragraph("Off")
        )

        val inherits = parsed.styles.paragraphStyles.getValue("Inherits")
        assertTrue(inherits.isBold)
        assertTrue(inherits.isItalic)
        assertTrue(inherits.isUnderline)
        assertTrue(inherits.isHidden)

        val off = parsed.styles.paragraphStyles.getValue("Off")
        assertFalse("w:b w:val=0 must un-bold", off.isBold)
        assertFalse("w:i w:val=false must un-italicise", off.isItalic)
        assertFalse("w:u w:val=none must remove the underline", off.isUnderline)
        assertFalse("w:vanish w:val=0 must un-hide", off.isHidden)

        val double = parsed.styles.paragraphStyles.getValue("DoubleUnderline")
        assertTrue(double.isUnderline)
    }

    /**
     * The owner decision of 2026-10-06 (audit-019 section 4.1): the paragraph
     * style wins for the properties it sets, and its linked character style
     * supplies the rest. Pairing must work through `w:link` on either element
     * and through Word's `X`/`X Char` naming convention.
     */
    @Test
    fun ownRunPropertiesBeatLinkedCharacterStyle() {
        val parsed = docx(
            docDefaults = defaults("<w:sz w:val=\"20\"/>"),
            styles = style(
                "Linked1",
                link = "Linked1Char",
                rPr = "<w:sz w:val=\"28\"/><w:b/>"
            ) + style(
                "Linked1Char",
                type = "character",
                name = "Linked 1 Char",
                rPr = "<w:sz w:val=\"40\"/><w:color w:val=\"0F4761\"/>"
            ) + style(
                "Named2",
                name = "Named 2",
                rPr = "<w:sz w:val=\"32\"/>"
            ) + style(
                "Named2Char",
                type = "character",
                name = "Named 2 Char",
                rPr = "<w:sz w:val=\"44\"/><w:i/>"
            ) + style(
                "BackLink3"
            ) + style(
                "BackLink3Char",
                type = "character",
                name = "Back Link 3 Char",
                link = "BackLink3",
                rPr = "<w:sz w:val=\"36\"/>"
            ),
            body = paragraph("Linked1") + paragraph("Named2") + paragraph("BackLink3")
        )

        val linked = parsed.styles.paragraphStyles.getValue("Linked1")
        assertEquals("own 14 pt beats the linked 20 pt", 14f, linked.fontSizeSp, 0.01f)
        assertTrue("own w:b applies", linked.isBold)
        assertEquals("the link fills what the style leaves unset", "0F4761", hex(linked.colorHex))

        val named = parsed.styles.paragraphStyles.getValue("Named2")
        assertEquals(16f, named.fontSizeSp, 0.01f)
        assertTrue("the name-convention twin still contributes italic", named.isItalic)

        val backLink = parsed.styles.paragraphStyles.getValue("BackLink3")
        assertEquals("a w:link declared on the character style pairs too", 18f, backLink.fontSizeSp, 0.01f)
    }

    /** [MS-OI29500] section 2.1.235(a): Word ignores all but the last link to a given style. */
    @Test
    fun lastLinkWinsWhenTwoParagraphStylesClaimOneCharacterStyle() {
        val parsed = docx(
            docDefaults = defaults("<w:sz w:val=\"20\"/>"),
            styles = style("First", link = "Shared", rPr = "<w:sz w:val=\"24\"/>") +
                style("Second", link = "Shared") +
                style("Shared", type = "character", name = "Shared Char", rPr = "<w:sz w:val=\"50\"/>"),
            body = paragraph("First") + paragraph("Second")
        )

        assertEquals("the later claimant takes the pair", 25f, parsed.styles.paragraphStyles.getValue("Second").fontSizeSp, 0.01f)
        assertEquals("the earlier claimant keeps its own size", 12f, parsed.styles.paragraphStyles.getValue("First").fontSizeSp, 0.01f)
    }

    @Test
    fun characterStylesAreExposedWithTheirResolvedChain() {
        val parsed = docx(
            styles = style("EmBase", type = "character", name = "Emphasis Base", rPr = "<w:sz w:val=\"40\"/>") +
                style(
                    "Em",
                    type = "character",
                    name = "Emphasis",
                    basedOn = "EmBase",
                    rPr = "<w:color w:val=\"FF0000\"/><w:b/>"
                ),
            body = paragraph("Em")
        )

        val byId = parsed.styles.characterStyles.getValue("Em")
        assertEquals("Emphasis", byId.name)
        assertEquals(20f, byId.fontSizeSp!!, 0.01f)
        assertEquals("FF0000", hex(byId.colorHex))
        assertTrue(byId.isBold)
        assertEquals("the lookup also accepts the style name", byId, parsed.styles.characterStyles.getValue("Emphasis"))
    }

    @Test
    fun ignoredBuiltInStyleKeepsOnlyItsIdentity() {
        val parsed = docx(
            docDefaults = defaults("<w:sz w:val=\"24\"/>"),
            styles = style("Base", rPr = "<w:sz w:val=\"96\"/>") +
                style(
                    "NoList",
                    name = "No List",
                    basedOn = "Base",
                    rPr = "<w:sz w:val=\"96\"/><w:vanish/>",
                    pPr = "<w:spacing w:after=\"480\"/>"
                ),
            body = paragraph("NoList")
        )

        val noList = parsed.styles.paragraphStyles.getValue("NoList")
        assertEquals("the ignored built-in contributes no size", 12f, noList.fontSizeSp, 0.01f)
        assertFalse(noList.isHidden)
        assertNull("its children, including basedOn, are ignored", noList.parentStyleName)
        assertEquals(0f, noList.spaceAfterUnits, 0.01f)
        assertEquals("NoList", paragraphs(parsed).single().styleName)
    }

    @Test
    fun styleIdPastTheLimitIsIgnoredAndFallsBackToTheDefault() {
        val longId = "long" + "x".repeat(252)
        assertTrue(longId.length > 253)
        val parsed = docx(
            docDefaults = defaults("<w:sz w:val=\"24\"/><w:b/>"),
            styles = style(longId, name = "Long Style", rPr = "<w:sz w:val=\"96\"/>"),
            body = paragraph(longId)
        )

        assertNull("a styleId longer than 253 characters is ignored", parsed.styles.paragraphStyles["Long Style"])
        val element = paragraphs(parsed).single()
        assertEquals("the paragraph stays a paragraph", longId, element.styleName)
        val resolved = StyleResolver.resolveParagraphStyle(element.styleName, parsed.styles)
        assertEquals("it resolves to the documentation default, not the ignored style", 12f, resolved.fontSizeSp, 0.01f)
        assertTrue("an unknown styleId never becomes a heading", headings(parsed).isEmpty())
    }

    @Test
    fun numberingStateFollowsTheStyleChainAndDirectProperties() {
        val parsed = docx(
            styles = style(
                "Num1",
                pPr = "<w:numPr><w:ilvl w:val=\"2\"/><w:numId w:val=\"7\"/></w:numPr>"
            ) + style(
                "NoNum",
                basedOn = "Num1",
                pPr = "<w:numPr><w:numId w:val=\"0\"/></w:numPr>"
            ) + style("Plain"),
            body = paragraph("Num1", text = "inherit") +
                paragraph("Num1", pPr = "<w:numPr><w:numId w:val=\"9\"/></w:numPr>", text = "direct") +
                paragraph("Num1", pPr = "<w:numPr><w:numId w:val=\"0\"/></w:numPr>", text = "suppressed") +
                paragraph("Num1", pPr = "<w:numPr><w:ilvl w:val=\"5\"/></w:numPr>", text = "relevel") +
                paragraph("NoNum", text = "inherited suppression") +
                paragraph("Plain", text = "plain")
        )

        val refs = paragraphs(parsed).map { it.numbering }
        val inherited = refs[0]!!
        assertEquals(7, inherited.numId)
        assertEquals(2, inherited.ilvl)
        assertTrue(inherited.fromStyle)
        assertFalse(inherited.suppressed)

        val direct = refs[1]!!
        assertEquals(9, direct.numId)
        assertEquals(0, direct.ilvl)
        assertFalse(direct.fromStyle)

        val suppressed = refs[2]!!
        assertEquals(0, suppressed.numId)
        assertTrue(suppressed.suppressed)

        val relevel = refs[3]!!
        assertEquals(7, relevel.numId)
        assertEquals("a direct w:ilvl alone re-levels the inherited numbering", 5, relevel.ilvl)
        assertTrue(relevel.fromStyle)

        val styleSuppressed = refs[4]!!
        assertEquals(0, styleSuppressed.numId)
        assertTrue("a style-level numId 0 suppression reaches the paragraph", styleSuppressed.suppressed)
        assertTrue(styleSuppressed.fromStyle)

        assertNull("a style without w:numPr carries no reference", refs[5])
        assertFalse(
            "style-chain numbering must not inject placeholder text",
            paragraphs(parsed).first().text.startsWith("\u2022 ")
        )
    }

    @Test
    fun headingIdentityComesFromDeclaredStyleDataNotIdShape() {
        val parsed = docx(
            styles = style("CustomHead", name = "heading 3", pPr = "<w:outlineLvl w:val=\"2\"/>") +
                style("NamedOnly", name = "heading 4") +
                style("Body", name = "Body Text"),
            body = paragraph("CustomHead") + paragraph("NamedOnly") + paragraph("Body") +
                paragraph("para3") + paragraph(null, pPr = "<w:outlineLvl w:val=\"1\"/>")
        )

        val sequence = parsed.elements.map {
            when (it) {
                is OfficeDocumentElement.Heading -> "H${it.level}"
                is OfficeDocumentElement.Paragraph -> "P"
                else -> "?"
            }
        }
        assertEquals(listOf("H3", "H4", "P", "P", "H2"), sequence)
        assertNull("an id the style table does not know stays unknown", parsed.styles.paragraphStyles["para3"])
    }

    @Test
    fun highlightAndVanishReachTheDisplayedSpanStyle() {
        val parsed = docx(
            styles = style("Highlighted", rPr = "<w:highlight w:val=\"yellow\"/>") +
                style("Hidden", rPr = "<w:vanish/>"),
            body = paragraph("Highlighted", text = "Marker") + paragraph("Hidden", text = "Ghost")
        )
        val document = parsed.toOfficeDocument()
        val bodyElements = document.body.elements
        val highlighted = bodyElements.filterIsInstance<OfficeParagraph>()[0]
        val hidden = bodyElements.filterIsInstance<OfficeParagraph>()[1]

        val highlightedSpan = OfficeRuns.toAnnotatedString(highlighted, document.styles, 1f, Color.Black)
            .spanStyles.first().item
        assertEquals(Color(0xFFFFFF00), highlightedSpan.background)

        val hiddenSpan = OfficeRuns.toAnnotatedString(hidden, document.styles, 1f, Color.Black)
            .spanStyles.first().item
        assertEquals(Color.Transparent, hiddenSpan.color)
    }
}
