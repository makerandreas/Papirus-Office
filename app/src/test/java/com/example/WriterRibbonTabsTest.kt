package com.example

import com.example.modules.inky.WriterRibbonDeck
import com.example.modules.inky.WriterRibbonTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan 3B (PR 13) regression test for the ribbon identities.
 *
 * The strip and the pager both read [WriterRibbonTab], so the properties that
 * keep them from disagreeing are asserted here instead of on a screen CI
 * cannot open: every tab either owns a deck or is marked unavailable, the
 * pager's page list is a subset of the strip in strip order, and a page index
 * is never assumed to equal a tab position.
 */
class WriterRibbonTabsTest {

    @Test
    fun everyDeclaredTabEitherOwnsADeckOrIsUnavailable() {
        for (tab in WriterRibbonTab.entries) {
            assertEquals(
                "${tab.name}: isImplemented must mean 'owns a deck'",
                tab.deck != null,
                tab.isImplemented
            )
            assertTrue("${tab.name}: blank label", tab.label.isNotBlank())
        }
    }

    @Test
    fun stripIsTheConceptWriterSet() {
        assertEquals(
            listOf("FILE", "HOME", "INSERT", "LAYOUT", "REVIEW", "VIEW"),
            WriterRibbonTab.entries.map { it.name }
        )
        // References and Mailings are not part of CONCEPT.md's Writer set: a
        // tab that could never be enabled is the louder lie (plan-03 3.6).
        assertFalse(WriterRibbonTab.entries.any { it.name == "REFERENCES" || it.name == "MAILINGS" })
        assertEquals("labels must be unique", WriterRibbonTab.entries.size, WriterRibbonTab.entries.map { it.label }.distinct().size)
    }

    @Test
    fun pagerHostsOnlyImplementedDecksInStripOrder() {
        val decks = WriterRibbonTab.withDecks
        assertEquals(listOf(WriterRibbonTab.FILE, WriterRibbonTab.HOME), decks)
        assertEquals(decks, WriterRibbonTab.entries.filter { it.isImplemented })
        assertEquals(
            "one deck per implemented tab",
            WriterRibbonDeck.entries.size,
            WriterRibbonTab.entries.mapNotNull { it.deck }.distinct().size
        )
        assertEquals("the pager's page count is the deck count", decks.size, WriterRibbonTab.entries.count { it.isImplemented })
    }

    @Test
    fun pageIndexIsAMappingNotAPosition() {
        WriterRibbonTab.withDecks.forEachIndexed { page, tab ->
            assertEquals(page, WriterRibbonTab.pageOf(tab))
        }
        for (tab in WriterRibbonTab.entries.filterNot { it.isImplemented }) {
            assertEquals("${tab.name} has no deck to page to", -1, WriterRibbonTab.pageOf(tab))
        }
        // The pager opens on the Home deck, whose page is not its strip position
        // by accident but by this lookup.
        assertEquals(1, WriterRibbonTab.pageOf(WriterRibbonTab.HOME))
        assertNotEquals(WriterRibbonTab.pageOf(WriterRibbonTab.HOME), WriterRibbonTab.pageOf(WriterRibbonTab.FILE))
    }
}
