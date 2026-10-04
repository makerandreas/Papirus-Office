package com.example

import com.makerandreas.papirusoffice.data.BodyElementRange
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.SectionDisplay
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import com.makerandreas.papirusoffice.data.navigation.NavigationEngine
import com.makerandreas.papirusoffice.data.navigation.NavigatorNotice
import com.makerandreas.papirusoffice.data.navigation.VisibilityState
import com.makerandreas.papirusoffice.data.odf.OdfImportDiagnostics
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import com.makerandreas.papirusoffice.data.toOfficeDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan 7C exit gate for named section ranges: nested, sibling, empty,
 * protected, hidden and conditional sections, plus a section inside a table
 * cell, which stays out of the body ranges (audit-015 F-13).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7cSectionRangeTest {

    private val unsupported = mutableListOf<String>()

    private fun parse(): OfficeParsedDocument {
        val diagnostics = object : OdfImportDiagnostics {
            override fun unsupportedTag(fileName: String, tagName: String, attributes: Map<String, String>) {
                unsupported += tagName
            }
            override fun malformedXml(fileName: String, errorMessage: String, cause: Throwable?) = Unit
        }
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0">
              <office:body><office:text>
                <text:p>Intro</text:p>
                <text:section text:name="Outer" text:protected="true" text:protection-key="abc">
                  <text:p>Outer first</text:p>
                  <text:section text:name="Inner"><text:p>Inner</text:p></text:section>
                  <text:p>Outer last</text:p>
                </text:section>
                <text:section text:name="Sibling"><text:p>Sibling</text:p></text:section>
                <text:section text:name="Empty"/>
                <text:section text:name="Hidden" text:display="none"><text:p>Secret</text:p></text:section>
                <text:section text:name="Cond" text:display="condition" text:condition="ooow:x==1"><text:p>Maybe</text:p></text:section>
                <table:table table:name="T"><table:table-row><table:table-cell>
                  <text:section text:name="InCell"><text:p>Cell text</text:p></text:section>
                </table:table-cell></table:table-row></table:table>
                <text:p>End</text:p>
              </office:text></office:body>
            </office:document-content>
        """.trimIndent()
        val parsed = SvXMLImport(diagnostics = diagnostics).parseOdfXml(xml, "sections.odt")
        assertFalse(parsed.failureReason, parsed.isParsingFailed)
        return parsed
    }

    @Test
    fun rangesCoverBodyFlowWithNestingDisplayAndProtection() {
        val parsed = parse()
        val texts = parsed.elements.map {
            when (it) {
                is OfficeDocumentElement.Paragraph -> it.text
                is OfficeDocumentElement.Table -> "table"
                else -> it::class.java.simpleName
            }
        }
        assertEquals(
            listOf("Intro", "Outer first", "Inner", "Outer last", "Sibling", "Secret", "Maybe", "table", "End"),
            texts
        )

        val sections = parsed.namedSectionRanges
        assertEquals(listOf("Outer", "Inner", "Sibling", "Empty", "Hidden", "Cond"), sections.map { it.name })
        val byName = sections.associateBy { it.name }

        val outer = byName.getValue("Outer")
        assertEquals(BodyElementRange(1, 4), outer.bodyRange)
        assertEquals(0, outer.depth)
        assertNull(outer.parentId)
        assertTrue(outer.isProtected)
        assertEquals("abc", outer.protectionKey)

        val inner = byName.getValue("Inner")
        assertEquals(BodyElementRange(2, 3), inner.bodyRange)
        assertEquals(1, inner.depth)
        assertEquals(outer.id, inner.parentId)
        assertFalse(inner.isProtected)

        assertEquals(BodyElementRange(4, 5), byName.getValue("Sibling").bodyRange)
        assertEquals(0, byName.getValue("Sibling").depth)

        val empty = byName.getValue("Empty")
        assertTrue(empty.bodyRange.isEmpty)
        assertEquals(5, empty.bodyRange.startInclusive)

        val hidden = byName.getValue("Hidden")
        assertEquals(SectionDisplay.HIDDEN, hidden.display)
        assertEquals(BodyElementRange(5, 6), hidden.bodyRange)

        val cond = byName.getValue("Cond")
        assertEquals(SectionDisplay.CONDITIONAL, cond.display)
        assertEquals("ooow:x==1", cond.condition)

        assertEquals("ids are unique", sections.size, sections.map { it.id }.distinct().size)
    }

    @Test
    fun sectionInsideATableCellStaysInTheCellAndIsReported() {
        unsupported.clear()
        val parsed = parse()
        val table = parsed.elements.filterIsInstance<OfficeDocumentElement.Table>().single()
        assertEquals("Cell text", table.rows.single().cells.single().text)
        assertTrue(parsed.namedSectionRanges.none { it.name == "InCell" })
        assertTrue("cell section must be reported", unsupported.contains("text:section"))
        assertTrue(parsed.elements.none { it is OfficeDocumentElement.Paragraph && it.text == "Cell text" })
    }

    @Test
    fun navigatorListsRangeSectionsAndHiddenOnesJumpToNearestVisibleText() {
        val document = parse().toOfficeDocument()
        val index = DocumentIndexEngine(document).getDocumentIndex()
        assertEquals(listOf("Outer", "Inner", "Sibling", "Empty", "Hidden", "Cond"), index.sections.map { it.sectionName })

        val byName = index.sections.associateBy { it.sectionName }
        val inner = byName.getValue("Inner")
        assertEquals(1, inner.depth)
        assertEquals(byName.getValue("Outer").id, inner.parentId)
        assertEquals(2, inner.elementIndex)
        assertTrue(byName.getValue("Outer").isProtected)

        val hidden = byName.getValue("Hidden")
        assertEquals(VisibilityState.HIDDEN, hidden.visibility)
        assertEquals("nearest visible element after the hidden range", 6, hidden.elementIndex)

        val cond = byName.getValue("Cond")
        assertEquals("conditions are not evaluated, so the section is listed visible", VisibilityState.VISIBLE, cond.visibility)
        assertTrue(cond.isConditional)

        assertEquals("empty section jumps to the nearest valid element", 5, byName.getValue("Empty").elementIndex)

        val engine = NavigationEngine(document)
        engine.goToSection(hidden.id)
        assertEquals(6, engine.state.value.navTargetSignal?.targetElementIndex)
        assertEquals(NavigatorNotice.HIDDEN_SECTION_NEAREST_VISIBLE, engine.state.value.notice)

        engine.clearNotice()
        engine.goToSection(byName.getValue("Sibling").id)
        assertEquals(4, engine.state.value.navTargetSignal?.targetElementIndex)
        assertNull(engine.state.value.notice)
    }

    @Test
    fun sectionsWithoutANameKeepNestingBalancedButProduceNoRange() {
        val xml = """
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:body><office:text>
                <text:section><text:section text:name="Child"><text:p>a</text:p></text:section></text:section>
                <text:section text:name="Next"><text:p>b</text:p></text:section>
              </office:text></office:body>
            </office:document-content>
        """.trimIndent()
        val parsed = SvXMLImport().parseOdfXml(xml, "unnamed.odt")
        assertEquals(listOf("Child", "Next"), parsed.namedSectionRanges.map { it.name })
        assertEquals(listOf(0, 0), parsed.namedSectionRanges.map { it.depth })
        assertTrue(parsed.namedSectionRanges.all { it.parentId == null })
    }
}
