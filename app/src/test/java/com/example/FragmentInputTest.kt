package com.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.modules.inky.LayoutDrivenDocumentRenderer
import com.makerandreas.papirusoffice.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FragmentInputTest {
    @get:Rule val compose = createComposeRule()

    @Test fun multiPageParagraphHasOneInputAndCrossBoundaryEditPreservesGlobalText() {
        val source = "Words for a long paragraph spanning several pages. ".repeat(15)
        val page = PageStyleSpec.FALLBACK.copy(widthDp = 240f, heightDp = 200f,
            marginStartDp = 20f, marginEndDp = 20f, marginTopDp = 20f, marginBottomDp = 20f)
        var value by mutableStateOf(TextFieldValue(source))
        var document by mutableStateOf(OfficeDocument(body = DocumentBody(listOf(OfficeParagraph(source)))))
        var zoom by mutableStateOf(1f)
        val layout = LayoutEngine(page, advanceSource = TableAdvanceSource).performLayout(document)
        assertTrue(layout.pages.size > 1)
        val boundary = layout.pages[1].elements.first().sourceStart
        compose.setContent {
            MaterialTheme {
                LayoutDrivenDocumentRenderer(document, zoom, true, DocumentCursor(), {},
                    pageSpec = page, editorValue = value, onEditorValueChange = {
                        value = it
                        document = DocumentTextMerger.mergeEditedText(document, it.text)
                    }, viewportWidthDp = 320f,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()))
            }
        }
        for (scale in listOf(0.5f, 1f, 1.5f, 2f, 3f)) {
            compose.runOnIdle { zoom = scale }
            compose.onAllNodesWithTag("doc_body_editor_element_0").assertCountEquals(1)
        }
        val selection = TextRange(boundary - 3, boundary + 3)
        compose.onNodeWithTag("doc_body_editor_element_0").performTextInputSelection(selection)
        compose.onNodeWithTag("doc_body_editor_element_0").performTextInput("X")
        compose.runOnIdle {
            assertEquals(source.replaceRange(selection.min, selection.max, "X"), value.text)
            assertEquals(1, document.body.elements.size)
            assertEquals(value.text, (document.body.elements.single() as OfficeParagraph).text)
        }
    }
}
