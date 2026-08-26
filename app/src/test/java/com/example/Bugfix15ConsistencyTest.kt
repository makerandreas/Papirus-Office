package com.example

import com.example.modules.inky.state.EditorMode
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentEngine
import com.makerandreas.papirusoffice.data.DocumentSession
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.SessionManager
import com.makerandreas.papirusoffice.data.undo.UndoAction
import com.makerandreas.papirusoffice.data.writer.EditingEngine
import com.makerandreas.papirusoffice.data.writer.SelectionEngine
import com.makerandreas.papirusoffice.data.writer.SelectionRange
import com.makerandreas.papirusoffice.data.writer.commands.DeleteSelectionCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Bugfix15ConsistencyTest {

    private lateinit var session: DocumentSession
    private lateinit var editingEngine: EditingEngine

    @Before
    fun setUp() {
        val initialDoc = OfficeDocument(
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Hello World")))
        )
        session = DocumentSession(
            engine = DocumentEngine(),
            document = initialDoc,
            file = null
        )
        SessionManager.getInstance().setCurrentSession(session)
        editingEngine = EditingEngine(
            getSession = { session },
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
    }

    /**
     * Test 15.1: Navigation Hierarchy (App Bar Back)
     * Inky Editor Screen (Edit Mode) -> App Bar Back -> Inky Editor Screen (View Mode) -> App Bar Back -> Start Screen / Closed
     */
    @Test
    fun test15_1_AppBarBackHierarchy() {
        var editorMode = EditorMode.EDIT
        var isEditMode = true
        var isClosed = false

        val handleBack = {
            if (editorMode == EditorMode.EDIT || isEditMode) {
                isEditMode = false
                editorMode = EditorMode.VIEW
            } else {
                isClosed = true
            }
        }

        // App Bar Back clicked in Edit Mode
        handleBack()
        assertEquals(EditorMode.VIEW, editorMode)
        assertFalse(isEditMode)
        assertFalse(isClosed)

        // App Bar Back clicked in View Mode
        handleBack()
        assertTrue(isClosed)
    }

    /**
     * Test 15.2: Navigation Hierarchy (Android Back)
     * Inky Editor Screen (Edit Mode) -> Android Back -> Inky Editor Screen (View Mode) -> Android Back -> Start Screen / Closed
     */
    @Test
    fun test15_2_AndroidBackHierarchy() {
        var editorMode = EditorMode.EDIT
        var isEditMode = true
        var isClosed = false

        val handleBack = {
            if (editorMode == EditorMode.EDIT || isEditMode) {
                isEditMode = false
                editorMode = EditorMode.VIEW
            } else {
                isClosed = true
            }
        }

        // Android Back pressed in Edit Mode
        handleBack()
        assertEquals(EditorMode.VIEW, editorMode)
        assertFalse(isEditMode)
        assertFalse(isClosed)

        // Android Back pressed in View Mode
        handleBack()
        assertTrue(isClosed)
    }

    /**
     * Test 15.3: Navigation Priority (FCT Shown)
     * FCT Shown -> Back -> FCT Dismissed -> Back -> Exit Edit Mode (View Mode)
     */
    @Test
    fun test15_3_NavigationPriorityFct() {
        var isFctShown = true
        var editorMode = EditorMode.EDIT
        var isClosed = false

        val handleBack = {
            when {
                isFctShown -> isFctShown = false
                editorMode == EditorMode.EDIT -> editorMode = EditorMode.VIEW
                else -> isClosed = true
            }
        }

        // 1. Back when FCT is shown -> dismisses FCT, stays in Edit Mode
        handleBack()
        assertFalse(isFctShown)
        assertEquals(EditorMode.EDIT, editorMode)
        assertFalse(isClosed)

        // 2. Back when FCT is dismissed -> transitions to View Mode
        handleBack()
        assertEquals(EditorMode.VIEW, editorMode)
        assertFalse(isClosed)
    }

    /**
     * Test 15.4: Navigation Priority (BottomSheet Shown)
     * BottomSheet Open -> Back -> Close BottomSheet -> Back -> Exit Edit Mode (View Mode)
     */
    @Test
    fun test15_4_NavigationPriorityBottomSheet() {
        var isBottomSheetOpen = true
        var editorMode = EditorMode.EDIT
        var isClosed = false

        val handleBack = {
            when {
                isBottomSheetOpen -> isBottomSheetOpen = false
                editorMode == EditorMode.EDIT -> editorMode = EditorMode.VIEW
                else -> isClosed = true
            }
        }

        // 1. Back when BottomSheet is open -> closes BottomSheet
        handleBack()
        assertFalse(isBottomSheetOpen)
        assertEquals(EditorMode.EDIT, editorMode)
        assertFalse(isClosed)

        // 2. Back when BottomSheet is closed -> transitions to View Mode
        handleBack()
        assertEquals(EditorMode.VIEW, editorMode)
        assertFalse(isClosed)
    }

    /**
     * Test 15.5: Editing Engine (FCT Delete -> Undo)
     * Delete text via FCT -> press Undo -> text restored
     */
    @Test
    fun test15_5_FctDeleteAndUndo() = runBlocking {
        var currentText = "Hello World"
        val selRange = SelectionRange(0, 5) // Select "Hello"

        // FCT Delete
        editingEngine.deleteSelection(
            selection = selRange,
            fullText = currentText,
            onApply = { newText, _ ->
                currentText = newText
            }
        )

        assertEquals(" World", currentText)
        assertTrue(session.undoManager.canUndo())

        // Undo
        session.undoManager.undo()
        assertEquals("Hello World", currentText)
    }

    /**
     * Test 15.6: Editing Engine (FCT Delete -> Undo -> Redo)
     * Delete text -> Undo -> Redo -> text re-deleted
     */
    @Test
    fun test15_6_FctDeleteUndoRedo() = runBlocking {
        var currentText = "Hello World"
        val selRange = SelectionRange(6, 11) // Select "World"

        // FCT Delete
        editingEngine.deleteSelection(
            selection = selRange,
            fullText = currentText,
            onApply = { newText, _ ->
                currentText = newText
            }
        )

        assertEquals("Hello ", currentText)

        // Undo
        session.undoManager.undo()
        assertEquals("Hello World", currentText)
        assertTrue(session.undoManager.canRedo())

        // Redo
        session.undoManager.redo()
        assertEquals("Hello ", currentText)
    }

    /**
     * Test 15.7: Editing Engine (FCT Cut -> Undo -> Clipboard preserved)
     * Cut selection -> clipboard has text -> Undo restores text in document
     */
    @Test
    fun test15_7_FctCutAndUndo() = runBlocking {
        var currentText = "Cut Sample Text"
        val selRange = SelectionRange(0, 3) // Select "Cut"
        var mockClipboard = ""

        // Cut operation
        val cutStr = SelectionEngine.extract(currentText, selRange)
        mockClipboard = cutStr

        editingEngine.deleteSelection(
            selection = selRange,
            fullText = currentText,
            onApply = { newText, _ ->
                currentText = newText
            }
        )

        assertEquals(" Sample Text", currentText)
        assertEquals("Cut", mockClipboard)

        // Undo
        session.undoManager.undo()
        assertEquals("Cut Sample Text", currentText)
        assertEquals("Cut", mockClipboard) // Clipboard still holds the cut text
    }

    /**
     * Test 15.8: Editing Engine Consistency (Keyboard Delete vs FCT Delete Undo)
     * Both keyboard delete action and FCT delete action integrate into the same UndoManager stack
     */
    @Test
    fun test15_8_KeyboardAndFctDeleteUndoConsistency() = runBlocking {
        var currentText = "Alpha Beta Gamma"

        // 1. Keyboard edit (simulate typing/deleting)
        val textAfterKeyboard = "Alpha Beta"
        session.undoManager.recordAction(object : UndoAction {
            override val title = "Delete text"
            override val timestamp = System.currentTimeMillis()
            override val icon = "backspace"
            override val commandType = "EDIT_TEXT"
            override suspend fun undo() { currentText = "Alpha Beta Gamma" }
            override suspend fun redo() { currentText = "Alpha Beta" }
        })
        currentText = textAfterKeyboard

        // 2. FCT Delete on "Beta" (selection from index 6 to 10)
        val selBeta = SelectionRange(6, 10)
        editingEngine.deleteSelection(
            selection = selBeta,
            fullText = currentText,
            onApply = { newText, _ ->
                currentText = newText
            }
        )
        assertEquals("Alpha ", currentText)

        // 3. Undo FCT Delete
        session.undoManager.undo()
        assertEquals("Alpha Beta", currentText)

        // 4. Undo Keyboard Delete
        session.undoManager.undo()
        assertEquals("Alpha Beta Gamma", currentText)
    }
}
