package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class PartitionTextToPagesStubTest {

    @Test
    fun emptyTextKeepsSingleEmptyPage() {
        assertEquals(listOf(""), partitionTextToPages(""))
    }

    @Test
    fun heuristicWrapsPastDefaultLineBudget() {
        // 120 paragraphs of one line each overflow the 46-line budget into 3 pages.
        val text = (1..120).joinToString("\n") { "line $it" }
        val pages = partitionTextToPages(text)
        assertEquals(3, pages.size)
    }

    @Test
    fun explicitPageBreakMarkerSplitsImmediately() {
        val pages = partitionTextToPages("alpha\nPage Break\nbeta")
        assertEquals(listOf("alpha", "beta"), pages)
    }
}
