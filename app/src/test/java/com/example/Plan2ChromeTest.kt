package com.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.input.TextFieldValue
import com.example.modules.inky.LayoutDrivenDocumentRenderer
import com.example.modules.inky.RendererFocusBridge
import com.example.ui.theme.PapirusTheme
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentCursor
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeParagraph
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Plan 2 regression tests for the page-stack chrome.
 *
 * The per-page `x / y` marker (F-01) is asserted by absence: the unified status
 * bar is the only page counter in either mode, so no card may print one again.
 * The focus bridge (F-04) is asserted by its contract, because the screen's
 * keyboard button is a thin caller of exactly this interface.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class Plan2ChromeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun document(paragraphs: Int): OfficeDocument = OfficeDocument(
        body = DocumentBody(
            elements = (1..paragraphs).map { index ->
                OfficeParagraph(
                    text = "Paragraph $index " + "with enough words to wrap across a line or two. ".repeat(3)
                )
            }
        )
    )

    private fun OfficeDocument.flatText(): String =
        body.elements.filterIsInstance<OfficeParagraph>().joinToString("\n\n") { it.text }

    @Test
    fun viewerPageStack_drawsNoPerPageCounter() {
        val doc = document(paragraphs = 6)

        composeTestRule.setContent {
            PapirusTheme {
                LayoutDrivenDocumentRenderer(
                    document = doc,
                    zoomScale = 1f,
                    isEditMode = false,
                    cursor = DocumentCursor(),
                    onCursorChange = {},
                    editorValue = TextFieldValue(doc.flatText()),
                    onViewerSelectionChange = {},
                    viewportWidthDp = 360f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.waitForIdle()

        // The removed marker rendered "n / m" inside every card. Nothing in this
        // document contains that shape, so any hit is the regression itself.
        val markerNodes = composeTestRule.onAllNodesWithText(" / ", substring = true).fetchSemanticsNodes()
        assertTrue("page cards must not print their own page counter", markerNodes.isEmpty())

        // The document still renders its own content.
        composeTestRule.onNodeWithText("Paragraph 1", substring = true).assertExists()
    }

    @Test
    fun focusBridge_reportsUnavailable_whenNothingIsEditable() {
        val doc = document(paragraphs = 2)
        val bridge = RendererFocusBridge()

        composeTestRule.setContent {
            PapirusTheme {
                LayoutDrivenDocumentRenderer(
                    document = doc,
                    zoomScale = 1f,
                    isEditMode = false,
                    cursor = DocumentCursor(),
                    onCursorChange = {},
                    editorValue = TextFieldValue(doc.flatText()),
                    onViewerSelectionChange = {},
                    viewportWidthDp = 360f,
                    focusBridge = bridge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.waitForIdle()

        // Viewer fields are read-only: the caller must not assume the IME will appear.
        assertFalse(bridge.requestFirstEditable())
    }

    @Test
    fun focusBridge_focusesFirstEditable_inEditorMode() {
        val doc = document(paragraphs = 3)
        val bridge = RendererFocusBridge()
        var value by mutableStateOf(TextFieldValue(doc.flatText()))

        composeTestRule.setContent {
            PapirusTheme {
                LayoutDrivenDocumentRenderer(
                    document = doc,
                    zoomScale = 1f,
                    isEditMode = true,
                    cursor = DocumentCursor(),
                    onCursorChange = {},
                    editorValue = value,
                    onEditorValueChange = { value = it },
                    viewportWidthDp = 360f,
                    focusBridge = bridge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.waitForIdle()

        assertTrue("the editor must report a focusable field to the keyboard button", bridge.requestFirstEditable())
    }

    @Test
    fun viewerToolbarRequest_staysSilentWithoutASelection() {
        val doc = document(paragraphs = 2)
        var toolbarRequests = 0

        composeTestRule.setContent {
            PapirusTheme {
                LayoutDrivenDocumentRenderer(
                    document = doc,
                    zoomScale = 1f,
                    isEditMode = false,
                    cursor = DocumentCursor(),
                    onCursorChange = {},
                    editorValue = TextFieldValue(doc.flatText()),
                    onViewerSelectionChange = {},
                    onViewerToolbarRequest = { rect -> if (rect != null) toolbarRequests++ },
                    viewportWidthDp = 360f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        composeTestRule.waitForIdle()

        // No selection: the field must not ask for the FCT on load, otherwise the
        // toolbar would flash above the text every time a document opens.
        assertTrue(toolbarRequests == 0)
    }
}
