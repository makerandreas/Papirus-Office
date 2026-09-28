package com.example

import com.makerandreas.papirusoffice.data.*
import org.junit.Assert.*
import org.junit.Test

class HyphenationLayoutTest {
    @Test fun longWordsMakeProgressWithOrWithoutDictionary() {
        val pattern = "." + buildString { repeat(11) { append("x2") }; append("x") }
        val hyphenator = HyphenationEngine.parse(listOf(pattern))
        val text = "x".repeat(195)
        for (dictionary in listOf(null, hyphenator)) {
            val layout = LayoutEngine(hyphenator = dictionary, advanceSource = TableAdvanceSource)
                .layoutParagraph(0, OfficeParagraph(text), DocumentStyles())
            assertTrue(layout.lines.size > 1)
            assertEquals(text, layout.lines.joinToString("") { text.substring(it.startOffset, it.endOffset) })
        }
    }

    @Test fun softHyphenIsVisualOnlyAtABreakAndNeverDropsSourceOffsets() {
        val text = "abc\u00addefghijklmnop"
        val spec = PageStyleSpec.FALLBACK.copy(widthDp = 185f, marginStartDp = 72f, marginEndDp = 72f)
        val layout = LayoutEngine(spec, advanceSource = TableAdvanceSource).layoutParagraph(0, OfficeParagraph(text), DocumentStyles())
        assertTrue(layout.lines.first().discretionaryHyphen)
        assertEquals("abc-", layout.lines.first().text)
        assertEquals(text, layout.lines.joinToString("") { text.substring(it.startOffset, it.endOffset) })
        val wide = LayoutEngine(advanceSource = TableAdvanceSource).layoutParagraph(0, OfficeParagraph(text), DocumentStyles())
        assertEquals(text.replace("\u00ad", ""), wide.lines.single().text)
    }
}
