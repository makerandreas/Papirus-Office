package com.makerandreas.papirusoffice.data

import java.io.File
import java.util.UUID
import java.util.Stack

// ==========================================================
// PHASE 4: Document Commands & Transactions Framework
// ==========================================================

interface DocumentCommand {
    fun execute(document: OfficeDocument): OfficeDocument
    fun undo(document: OfficeDocument): OfficeDocument
}

class DocumentTransaction(val name: String) {
    private val commands = mutableListOf<DocumentCommand>()

    fun addCommand(command: DocumentCommand) {
        commands.add(command)
    }

    fun execute(document: OfficeDocument): OfficeDocument {
        var doc = document
        for (cmd in commands) {
            doc = cmd.execute(doc)
        }
        return doc
    }

    fun undo(document: OfficeDocument): OfficeDocument {
        var doc = document
        for (i in commands.indices.reversed()) {
            doc = commands[i].undo(doc)
        }
        return doc
    }

    fun isNotEmpty(): Boolean = commands.isNotEmpty()
}

// ==========================================================
// PHASE 1: Editing Engine (Decoupled Core State Manager)
// ==========================================================

class EditingEngine(
    var document: OfficeDocument,
    var cursor: DocumentCursor = DocumentCursor(),
    val layoutEngine: LayoutEngine = LayoutEngine()
) {
    private val undoStack = Stack<DocumentTransaction>()
    private val redoStack = Stack<DocumentTransaction>()
    private var currentTransaction: DocumentTransaction? = null

    fun beginTransaction(name: String) {
        currentTransaction = DocumentTransaction(name)
    }

    fun commitTransaction() {
        val tx = currentTransaction ?: return
        if (tx.isNotEmpty()) {
            undoStack.push(tx)
            redoStack.clear()
        }
        currentTransaction = null
    }

    fun rollbackTransaction() {
        currentTransaction = null
    }

    fun executeCommand(command: DocumentCommand) {
        val tx = currentTransaction
        if (tx != null) {
            document = command.execute(document)
            tx.addCommand(command)
        } else {
            beginTransaction("Command: ${command.javaClass.simpleName}")
            document = command.execute(document)
            currentTransaction?.addCommand(command)
            commitTransaction()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val tx = undoStack.pop()
            document = tx.undo(document)
            redoStack.push(tx)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val tx = redoStack.pop()
            document = tx.execute(document)
            undoStack.push(tx)
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    // Decoupled Editing APIs
    fun insertText(text: String) {
        executeCommand(InsertTextCommand(cursor.paragraphIndex, cursor.offset, text))
        cursor = cursor.copy(offset = cursor.offset + text.length)
    }

    fun deleteBackward() {
        if (cursor.offset > 0) {
            val pIdx = cursor.paragraphIndex
            val element = document.body.elements.getOrNull(pIdx)
            if (element is OfficeDocElement.ParagraphElement) {
                val text = element.paragraph.text
                val charToDelete = text[cursor.offset - 1].toString()
                executeCommand(DeleteTextCommand(pIdx, cursor.offset - 1, charToDelete))
                cursor = cursor.copy(offset = cursor.offset - 1)
            }
        } else if (cursor.paragraphIndex > 0) {
            mergeParagraphs(cursor.paragraphIndex - 1, cursor.paragraphIndex)
        }
    }

    fun splitParagraph() {
        val pIdx = cursor.paragraphIndex
        val element = document.body.elements.getOrNull(pIdx)
        if (element is OfficeDocElement.ParagraphElement) {
            val originalText = element.paragraph.text
            val leftText = originalText.substring(0, cursor.offset)
            val rightText = originalText.substring(cursor.offset)

            executeCommand(SplitParagraphCommand(pIdx, leftText, rightText))
            cursor = cursor.copy(paragraphIndex = pIdx + 1, elementIndex = pIdx + 1, offset = 0)
        }
    }

    fun mergeParagraphs(targetIdx: Int, sourceIdx: Int) {
        val targetElem = document.body.elements.getOrNull(targetIdx)
        val sourceElem = document.body.elements.getOrNull(sourceIdx)
        if (targetElem is OfficeDocElement.ParagraphElement && sourceElem is OfficeDocElement.ParagraphElement) {
            val targetText = targetElem.paragraph.text
            val sourceText = sourceElem.paragraph.text

            executeCommand(MergeParagraphsCommand(targetIdx, sourceIdx, targetText, sourceText))
            cursor = cursor.copy(paragraphIndex = targetIdx, elementIndex = targetIdx, offset = targetText.length)
        }
    }

    fun insertTable(rows: Int, cols: Int) {
        val cells = List(cols) { OfficeTableCell("") }
        val tableRows = List(rows) { OfficeTableRow(cells) }
        val table = OfficeTable(rows = tableRows, numColumns = cols)
        executeCommand(InsertTableCommand(cursor.paragraphIndex + 1, table))
    }

    fun insertImage(imagePath: String, file: File?, widthDp: Float = 250f, heightDp: Float = 200f) {
        val image = OfficeImage(imagePath, file, widthDp, heightDp)
        executeCommand(InsertImageCommand(cursor.paragraphIndex + 1, image))
    }

    fun insertBookmark(name: String) {
        executeCommand(InsertBookmarkCommand(cursor.paragraphIndex, name))
    }
}

// ==========================================================
// PHASE 4 (Continued): Concrete Commands implementations
// ==========================================================

class InsertTextCommand(val paragraphIndex: Int, val offset: Int, val textToInsert: String) : DocumentCommand {
    override fun execute(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        if (element is OfficeDocElement.ParagraphElement) {
            val oldPara = element.paragraph
            val originalText = oldPara.text
            val newText = if (offset in 0..originalText.length) {
                originalText.substring(0, offset) + textToInsert + originalText.substring(offset)
            } else {
                originalText + textToInsert
            }
            elements[paragraphIndex] = OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
        }
        return document.copy(body = DocumentBody(elements))
    }

    override fun undo(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        if (element is OfficeDocElement.ParagraphElement) {
            val oldPara = element.paragraph
            val originalText = oldPara.text
            val startIndex = offset
            val endIndex = offset + textToInsert.length
            val newText = if (startIndex in 0..originalText.length && endIndex <= originalText.length) {
                originalText.removeRange(startIndex, endIndex)
            } else {
                originalText.removeSuffix(textToInsert)
            }
            elements[paragraphIndex] = OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
        }
        return document.copy(body = DocumentBody(elements))
    }
}

class DeleteTextCommand(val paragraphIndex: Int, val offset: Int, val deletedText: String) : DocumentCommand {
    override fun execute(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        if (element is OfficeDocElement.ParagraphElement) {
            val oldPara = element.paragraph
            val originalText = oldPara.text
            val startIndex = offset
            val endIndex = offset + deletedText.length
            val newText = if (startIndex in 0..originalText.length && endIndex <= originalText.length) {
                originalText.removeRange(startIndex, endIndex)
            } else {
                originalText
            }
            elements[paragraphIndex] = OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
        }
        return document.copy(body = DocumentBody(elements))
    }

    override fun undo(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        if (element is OfficeDocElement.ParagraphElement) {
            val oldPara = element.paragraph
            val originalText = oldPara.text
            val newText = if (offset in 0..originalText.length) {
                originalText.substring(0, offset) + deletedText + originalText.substring(offset)
            } else {
                originalText + deletedText
            }
            elements[paragraphIndex] = OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
        }
        return document.copy(body = DocumentBody(elements))
    }
}

class SplitParagraphCommand(val paragraphIndex: Int, val leftText: String, val rightText: String) : DocumentCommand {
    override fun execute(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        if (element is OfficeDocElement.ParagraphElement) {
            val oldPara = element.paragraph
            elements[paragraphIndex] = OfficeDocElement.ParagraphElement(oldPara.copy(text = leftText))
            elements.add(paragraphIndex + 1, OfficeDocElement.ParagraphElement(OfficeParagraph(text = rightText, styleName = oldPara.styleName)))
        }
        return document.copy(body = DocumentBody(elements))
    }

    override fun undo(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        if (paragraphIndex in elements.indices && paragraphIndex + 1 in elements.indices) {
            val leftElement = elements[paragraphIndex]
            val rightElement = elements[paragraphIndex + 1]
            if (leftElement is OfficeDocElement.ParagraphElement && rightElement is OfficeDocElement.ParagraphElement) {
                val joinedText = leftElement.paragraph.text + rightElement.paragraph.text
                elements[paragraphIndex] = OfficeDocElement.ParagraphElement(leftElement.paragraph.copy(text = joinedText))
                elements.removeAt(paragraphIndex + 1)
            }
        }
        return document.copy(body = DocumentBody(elements))
    }
}

class MergeParagraphsCommand(val targetIdx: Int, val sourceIdx: Int, val targetText: String, val sourceText: String) : DocumentCommand {
    override fun execute(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val targetElem = elements.getOrNull(targetIdx)
        if (targetElem is OfficeDocElement.ParagraphElement) {
            elements[targetIdx] = OfficeDocElement.ParagraphElement(targetElem.paragraph.copy(text = targetText + sourceText))
            elements.removeAt(sourceIdx)
        }
        return document.copy(body = DocumentBody(elements))
    }

    override fun undo(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val targetElem = elements.getOrNull(targetIdx)
        if (targetElem is OfficeDocElement.ParagraphElement) {
            elements[targetIdx] = OfficeDocElement.ParagraphElement(targetElem.paragraph.copy(text = targetText))
            elements.add(sourceIdx, OfficeDocElement.ParagraphElement(OfficeParagraph(text = sourceText)))
        }
        return document.copy(body = DocumentBody(elements))
    }
}

class InsertTableCommand(val insertIndex: Int, val table: OfficeTable) : DocumentCommand {
    override fun execute(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val safeIndex = insertIndex.coerceIn(0, elements.size)
        elements.add(safeIndex, OfficeDocElement.TableElement(table))
        return document.copy(body = DocumentBody(elements))
    }

    override fun undo(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val safeIndex = insertIndex.coerceIn(0, elements.size - 1)
        if (safeIndex in elements.indices) {
            elements.removeAt(safeIndex)
        }
        return document.copy(body = DocumentBody(elements))
    }
}

class InsertImageCommand(val insertIndex: Int, val image: OfficeImage) : DocumentCommand {
    override fun execute(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val safeIndex = insertIndex.coerceIn(0, elements.size)
        elements.add(safeIndex, OfficeDocElement.ImageElement(image))
        return document.copy(body = DocumentBody(elements))
    }

    override fun undo(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val safeIndex = insertIndex.coerceIn(0, elements.size - 1)
        if (safeIndex in elements.indices) {
            elements.removeAt(safeIndex)
        }
        return document.copy(body = DocumentBody(elements))
    }
}

class InsertBookmarkCommand(val paragraphIndex: Int, val bookmarkName: String) : DocumentCommand {
    override fun execute(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        if (element is OfficeDocElement.ParagraphElement) {
            elements[paragraphIndex] = OfficeDocElement.ParagraphElement(element.paragraph.copy(bookmark = bookmarkName))
        }
        return document.copy(body = DocumentBody(elements))
    }

    override fun undo(document: OfficeDocument): OfficeDocument {
        val elements = document.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        if (element is OfficeDocElement.ParagraphElement) {
            elements[paragraphIndex] = OfficeDocElement.ParagraphElement(element.paragraph.copy(bookmark = null))
        }
        return document.copy(body = DocumentBody(elements))
    }
}

// ==========================================================
// PHASE 2: Selection Engine (Anchor, Caret, Range State)
// ==========================================================

data class SelectionRange(
    val anchor: DocumentCursor = DocumentCursor(),
    val active: DocumentCursor = DocumentCursor()
) {
    val isCollapsed: Boolean
        get() = anchor == active

    val min: DocumentCursor
        get() = if (isBefore(anchor, active)) anchor else active

    val max: DocumentCursor
        get() = if (isBefore(anchor, active)) active else anchor

    private fun isBefore(c1: DocumentCursor, c2: DocumentCursor): Boolean {
        if (c1.paragraphIndex < c2.paragraphIndex) return true
        if (c1.paragraphIndex > c2.paragraphIndex) return false
        return c1.offset <= c2.offset
    }
}

class SelectionEngine(
    var selection: SelectionRange = SelectionRange()
) {
    fun setCursor(cursor: DocumentCursor) {
        selection = SelectionRange(anchor = cursor, active = cursor)
    }

    fun extendSelection(cursor: DocumentCursor) {
        selection = selection.copy(active = cursor)
    }

    fun selectAll(document: OfficeDocument) {
        val elements = document.body.elements
        if (elements.isEmpty()) return
        val lastIdx = elements.size - 1
        val lastElem = elements[lastIdx]
        val lastOffset = if (lastElem is OfficeDocElement.ParagraphElement) lastElem.paragraph.text.length else 0

        selection = SelectionRange(
            anchor = DocumentCursor(0, 0, 0, 0),
            active = DocumentCursor(lastIdx, lastIdx, 0, lastOffset)
        )
    }
}

// ==========================================================
// PHASE 3: Cursor Navigation Engine
// ==========================================================

object CaretNavigation {
    fun moveLeft(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        if (cursor.offset > 0) {
            return cursor.copy(offset = cursor.offset - 1)
        }
        if (cursor.paragraphIndex > 0) {
            val prevIdx = cursor.paragraphIndex - 1
            val prevElem = document.body.elements.getOrNull(prevIdx)
            val len = if (prevElem is OfficeDocElement.ParagraphElement) prevElem.paragraph.text.length else 0
            return DocumentCursor(prevIdx, prevIdx, 0, len)
        }
        return cursor
    }

    fun moveRight(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        val elem = document.body.elements.getOrNull(cursor.paragraphIndex)
        val textLength = if (elem is OfficeDocElement.ParagraphElement) elem.paragraph.text.length else 0
        if (cursor.offset < textLength) {
            return cursor.copy(offset = cursor.offset + 1)
        }
        if (cursor.paragraphIndex < document.body.elements.size - 1) {
            val nextIdx = cursor.paragraphIndex + 1
            return DocumentCursor(nextIdx, nextIdx, 0, 0)
        }
        return cursor
    }

    fun moveUp(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        if (cursor.paragraphIndex > 0) {
            val prevIdx = cursor.paragraphIndex - 1
            val prevElem = document.body.elements.getOrNull(prevIdx)
            val textLength = if (prevElem is OfficeDocElement.ParagraphElement) prevElem.paragraph.text.length else 0
            return DocumentCursor(prevIdx, prevIdx, 0, cursor.offset.coerceAtMost(textLength))
        }
        return cursor
    }

    fun moveDown(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        if (cursor.paragraphIndex < document.body.elements.size - 1) {
            val nextIdx = cursor.paragraphIndex + 1
            val nextElem = document.body.elements.getOrNull(nextIdx)
            val textLength = if (nextElem is OfficeDocElement.ParagraphElement) nextElem.paragraph.text.length else 0
            return DocumentCursor(nextIdx, nextIdx, 0, cursor.offset.coerceAtMost(textLength))
        }
        return cursor
    }

    fun moveWordLeft(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        val elem = document.body.elements.getOrNull(cursor.paragraphIndex)
        if (elem is OfficeDocElement.ParagraphElement) {
            val text = elem.paragraph.text
            if (cursor.offset == 0) return moveLeft(document, cursor)

            var i = cursor.offset - 1
            while (i > 0 && text[i].isWhitespace()) { i-- }
            while (i > 0 && !text[i].isWhitespace()) { i-- }
            val finalOffset = if (i > 0 && text[i].isWhitespace()) i + 1 else i
            return cursor.copy(offset = finalOffset)
        }
        return moveLeft(document, cursor)
    }

    fun moveWordRight(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        val elem = document.body.elements.getOrNull(cursor.paragraphIndex)
        if (elem is OfficeDocElement.ParagraphElement) {
            val text = elem.paragraph.text
            val len = text.length
            if (cursor.offset == len) return moveRight(document, cursor)

            var i = cursor.offset
            while (i < len && !text[i].isWhitespace()) { i++ }
            while (i < len && text[i].isWhitespace()) { i++ }
            return cursor.copy(offset = i)
        }
        return moveRight(document, cursor)
    }

    fun home(cursor: DocumentCursor): DocumentCursor {
        return cursor.copy(offset = 0)
    }

    fun end(document: OfficeDocument, cursor: DocumentCursor): DocumentCursor {
        val elem = document.body.elements.getOrNull(cursor.paragraphIndex)
        val len = if (elem is OfficeDocElement.ParagraphElement) elem.paragraph.text.length else 0
        return cursor.copy(offset = len)
    }
}

// ==========================================================
// PHASE 6: Field Engine (Document Field Evaluators)
// ==========================================================

object FieldEngine {
    fun evaluateField(fieldType: String, document: OfficeDocument): String {
        return when (fieldType.lowercase()) {
            "date" -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            "time" -> java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            "author" -> document.properties.isAutoCorrectEnabled.let { document.metadata.author.ifBlank { "User" } }
            "pagenumber" -> "1"
            "pagecount" -> "1"
            "filename" -> "Document"
            "title" -> document.metadata.title.ifBlank { "Untitled Document" }
            "subject" -> document.metadata.subject.ifBlank { "General" }
            else -> ""
        }
    }
}

// ==========================================================
// PHASE 7: Table Engine (Data structures & manipulations)
// ==========================================================

object TableEngine {
    fun addRow(table: OfficeTable): OfficeTable {
        val numCols = if (table.rows.isNotEmpty()) table.rows[0].cells.size else 1
        val newCells = List(numCols) { OfficeTableCell("", listOf(OfficeParagraph(""))) }
        return table.copy(rows = table.rows + OfficeTableRow(newCells))
    }

    fun addColumn(table: OfficeTable): OfficeTable {
        val updatedRows = table.rows.map { row ->
            row.copy(cells = row.cells + OfficeTableCell("", listOf(OfficeParagraph(""))))
        }
        return table.copy(rows = updatedRows, numColumns = table.numColumns + 1)
    }

    fun setCellText(table: OfficeTable, rowIdx: Int, colIdx: Int, text: String): OfficeTable {
        val updatedRows = table.rows.mapIndexed { rIdx, row ->
            if (rIdx == rowIdx) {
                val updatedCells = row.cells.mapIndexed { cIdx, cell ->
                    if (cIdx == colIdx) {
                        cell.copy(text = text, paragraphs = listOf(OfficeParagraph(text)))
                    } else {
                        cell
                    }
                }
                row.copy(cells = updatedCells)
            } else {
                row
            }
        }
        return table.copy(rows = updatedRows)
    }
}

// ==========================================================
// PHASE 8: Drawing Layer (Floating Objects & Canvas Overlay)
// ==========================================================

enum class DrawingObjectType {
    IMAGE, SHAPE, OLE, CHART, FORMULA, TEXT_BOX
}

data class FloatingObject(
    val id: String = UUID.randomUUID().toString(),
    val type: DrawingObjectType,
    val bounds: OfficeRect = OfficeRect(10f, 10f, 150f, 150f),
    val fillHex: String? = null,
    val content: String = ""
)

class DrawingLayer {
    private val floatingObjects = mutableListOf<FloatingObject>()

    fun addFloatingObject(obj: FloatingObject) {
        floatingObjects.add(obj)
    }

    fun removeFloatingObject(id: String) {
        floatingObjects.removeAll { it.id == id }
    }

    fun updateBounds(id: String, newBounds: OfficeRect) {
        val idx = floatingObjects.indexOfFirst { it.id == id }
        if (idx != -1) {
            floatingObjects[idx] = floatingObjects[idx].copy(bounds = newBounds)
        }
    }

    fun getAllObjects(): List<FloatingObject> = floatingObjects.toList()
}

// ==========================================================
// PHASE 9: Document Services (Search, Replace, TOC, Spell Check)
// ==========================================================

object DocumentServices {

    fun search(document: OfficeDocument, query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        if (query.isEmpty()) return results
        document.body.elements.forEachIndexed { elemIdx, element ->
            if (element is OfficeDocElement.ParagraphElement) {
                val text = element.paragraph.text
                var index = text.indexOf(query, ignoreCase = true)
                while (index != -1) {
                    results.add(SearchResult(elemIdx, index, query.length, text.substring(index, index + query.length)))
                    index = text.indexOf(query, index + 1, ignoreCase = true)
                }
            }
        }
        return results
    }

    fun replaceAll(document: OfficeDocument, query: String, replacement: String): OfficeDocument {
        if (query.isEmpty()) return document
        val updatedElements = document.body.elements.map { element ->
            if (element is OfficeDocElement.ParagraphElement) {
                val text = element.paragraph.text
                val newText = text.replace(query, replacement, ignoreCase = true)
                OfficeDocElement.ParagraphElement(element.paragraph.copy(text = newText))
            } else {
                element
            }
        }
        return document.copy(body = DocumentBody(updatedElements))
    }

    fun generateTableOfContents(document: OfficeDocument): List<TOCEntry> {
        val toc = mutableListOf<TOCEntry>()
        document.body.elements.forEachIndexed { index, element ->
            if (element is OfficeDocElement.ParagraphElement) {
                val p = element.paragraph
                if (p.styleName?.contains("Heading", ignoreCase = true) == true) {
                    val level = when {
                        p.styleName.contains("Heading 1", ignoreCase = true) -> 1
                        p.styleName.contains("Heading 2", ignoreCase = true) -> 2
                        p.styleName.contains("Heading 3", ignoreCase = true) -> 3
                        else -> 1
                    }
                    toc.add(TOCEntry(p.text, level, index))
                }
            }
        }
        return toc
    }

    fun runSpellCheck(document: OfficeDocument): List<SpellIssue> {
        val dictionary = setOf("the", "and", "document", "papirus", "office", "editor", "engine", "paragraph", "table", "image", "layout", "hello", "world")
        val issues = mutableListOf<SpellIssue>()
        document.body.elements.forEachIndexed { elemIdx, element ->
            if (element is OfficeDocElement.ParagraphElement) {
                val words = element.paragraph.text.split(Regex("[\\s,.:;!?()]+"))
                var offset = 0
                for (word in words) {
                    if (word.isNotBlank() && word.all { it.isLetter() }) {
                        val wordLower = word.lowercase()
                        if (!dictionary.contains(wordLower) && wordLower.length > 2) {
                            issues.add(SpellIssue(elemIdx, offset, word.length, word, "Unknown word: $word"))
                        }
                    }
                    offset += word.length + 1
                }
            }
        }
        return issues
    }

    fun autoCorrect(text: String): String {
        val replacements = mapOf(
            "teh" to "the",
            "recieve" to "receive",
            "dont" to "don't",
            "cant" to "can't",
            "paprus" to "papirus",
            "adress" to "address"
        )
        var result = text
        replacements.forEach { (misspell, correction) ->
            result = result.replace(Regex("\\b$misspell\\b", RegexOption.IGNORE_CASE), correction)
        }
        return result
    }
}

data class SearchResult(val elementIndex: Int, val charOffset: Int, val length: Int, val matchedText: String)
data class TOCEntry(val title: String, val level: Int, val elementIndex: Int)
data class SpellIssue(val elementIndex: Int, val charOffset: Int, val length: Int, val word: String, val message: String)
