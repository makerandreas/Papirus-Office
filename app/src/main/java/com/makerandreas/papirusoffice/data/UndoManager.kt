package com.makerandreas.papirusoffice.data

import java.util.Stack

interface Command {
    fun execute()
    fun undo()
}

class UndoManager {
    private val undoStack = Stack<Command>()
    private val redoStack = Stack<Command>()

    fun executeCommand(command: Command) {
        command.execute()
        undoStack.push(command)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val command = undoStack.pop()
            command.undo()
            redoStack.push(command)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val command = redoStack.pop()
            command.execute()
            undoStack.push(command)
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

// Concrete Command Implementations

class TextInsertCommand(
    private val textToInsert: String,
    private val position: Int,
    private var document: OfficeDocument,
    private val onUpdate: (OfficeDocument) -> Unit
) : Command {
    override fun execute() {
        // Real implementation of inserting text to the document body (paragraph simulation)
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == 0 && element is OfficeDocElement.ParagraphElement) {
                val oldPara = element.paragraph
                val originalText = oldPara.text
                val newText = if (position in 0..originalText.length) {
                    originalText.substring(0, position) + textToInsert + originalText.substring(position)
                } else {
                    originalText + textToInsert
                }
                OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }

    override fun undo() {
        // Revert text insertion
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == 0 && element is OfficeDocElement.ParagraphElement) {
                val oldPara = element.paragraph
                val originalText = oldPara.text
                val startIndex = position
                val endIndex = position + textToInsert.length
                val newText = if (startIndex in 0..originalText.length && endIndex <= originalText.length) {
                    originalText.removeRange(startIndex, endIndex)
                } else {
                    originalText.removeSuffix(textToInsert)
                }
                OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }
}

class DeleteCommand(
    private val deletedText: String,
    private val position: Int,
    private var document: OfficeDocument,
    private val onUpdate: (OfficeDocument) -> Unit
) : Command {
    override fun execute() {
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == 0 && element is OfficeDocElement.ParagraphElement) {
                val oldPara = element.paragraph
                val originalText = oldPara.text
                val startIndex = position
                val endIndex = position + deletedText.length
                val newText = if (startIndex in 0..originalText.length && endIndex <= originalText.length) {
                    originalText.removeRange(startIndex, endIndex)
                } else {
                    originalText
                }
                OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }

    override fun undo() {
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == 0 && element is OfficeDocElement.ParagraphElement) {
                val oldPara = element.paragraph
                val originalText = oldPara.text
                val newText = if (position in 0..originalText.length) {
                    originalText.substring(0, position) + deletedText + originalText.substring(position)
                } else {
                    originalText + deletedText
                }
                OfficeDocElement.ParagraphElement(oldPara.copy(text = newText))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }
}

class ParagraphStyleCommand(
    private val paragraphIndex: Int,
    private val oldStyleName: String?,
    private val newStyleName: String?,
    private var document: OfficeDocument,
    private val onUpdate: (OfficeDocument) -> Unit
) : Command {
    override fun execute() {
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == paragraphIndex && element is OfficeDocElement.ParagraphElement) {
                OfficeDocElement.ParagraphElement(element.paragraph.copy(styleName = newStyleName))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }

    override fun undo() {
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == paragraphIndex && element is OfficeDocElement.ParagraphElement) {
                OfficeDocElement.ParagraphElement(element.paragraph.copy(styleName = oldStyleName))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }
}

class CharacterStyleCommand(
    private val paragraphIndex: Int,
    private val runIndex: Int,
    private val oldStyleName: String?,
    private val newStyleName: String?,
    private var document: OfficeDocument,
    private val onUpdate: (OfficeDocument) -> Unit
) : Command {
    override fun execute() {
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == paragraphIndex && element is OfficeDocElement.ParagraphElement) {
                val oldPara = element.paragraph
                val updatedRuns = oldPara.runs.mapIndexed { rIdx, run ->
                    if (rIdx == runIndex) {
                        run.copy(styleName = newStyleName)
                    } else {
                        run
                    }
                }
                OfficeDocElement.ParagraphElement(oldPara.copy(runs = updatedRuns))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }

    override fun undo() {
        val currentBody = document.body
        val updatedElements = currentBody.elements.mapIndexed { idx, element ->
            if (idx == paragraphIndex && element is OfficeDocElement.ParagraphElement) {
                val oldPara = element.paragraph
                val updatedRuns = oldPara.runs.mapIndexed { rIdx, run ->
                    if (rIdx == runIndex) {
                        run.copy(styleName = oldStyleName)
                    } else {
                        run
                    }
                }
                OfficeDocElement.ParagraphElement(oldPara.copy(runs = updatedRuns))
            } else {
                element
            }
        }
        document = document.copy(body = DocumentBody(elements = updatedElements))
        onUpdate(document)
    }
}

class TableInsertCommand(
    private val tableIndex: Int,
    private val table: OfficeTable,
    private var document: OfficeDocument,
    private val onUpdate: (OfficeDocument) -> Unit
) : Command {
    override fun execute() {
        val currentBody = document.body
        val elementsMutable = currentBody.elements.toMutableList()
        if (tableIndex in 0..elementsMutable.size) {
            elementsMutable.add(tableIndex, OfficeDocElement.TableElement(table))
        } else {
            elementsMutable.add(OfficeDocElement.TableElement(table))
        }
        document = document.copy(body = DocumentBody(elements = elementsMutable))
        onUpdate(document)
    }

    override fun undo() {
        val currentBody = document.body
        val elementsMutable = currentBody.elements.toMutableList()
        if (tableIndex in 0 until elementsMutable.size) {
            elementsMutable.removeAt(tableIndex)
        } else {
            if (elementsMutable.isNotEmpty()) {
                elementsMutable.removeAt(elementsMutable.size - 1)
            }
        }
        document = document.copy(body = DocumentBody(elements = elementsMutable))
        onUpdate(document)
    }
}
