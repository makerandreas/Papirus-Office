package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.toPlainText

object SelectionEngine {
    fun extract(doc: OfficeDocument, selection: SelectionRange): String {
        val fullText = doc.toPlainText()
        val start = selection.min.coerceIn(0, fullText.length)
        val end = selection.max.coerceIn(0, fullText.length)
        return if (start < end) fullText.substring(start, end) else ""
    }

    fun extract(text: String, selection: SelectionRange): String {
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)
        return if (start < end) text.substring(start, end) else ""
    }

    fun delete(doc: OfficeDocument, selection: SelectionRange): OfficeDocument {
        val fullText = doc.toPlainText()
        val start = selection.min.coerceIn(0, fullText.length)
        val end = selection.max.coerceIn(0, fullText.length)
        val newText = if (start < end) fullText.removeRange(start, end) else fullText
        val newElements = newText.split("\n\n").map { OfficeParagraph(text = it) }
        return doc.copy(body = DocumentBody(elements = newElements), isModified = true)
    }

    fun delete(text: String, selection: SelectionRange): String {
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)
        return if (start < end) text.removeRange(start, end) else text
    }

    fun insert(doc: OfficeDocument, offset: Int, insertedText: String): OfficeDocument {
        val fullText = doc.toPlainText()
        val safeOffset = offset.coerceIn(0, fullText.length)
        val newText = fullText.substring(0, safeOffset) + insertedText + fullText.substring(safeOffset)
        val newElements = newText.split("\n\n").map { OfficeParagraph(text = it) }
        return doc.copy(body = DocumentBody(elements = newElements), isModified = true)
    }

    fun insert(text: String, offset: Int, insertedText: String): String {
        val safeOffset = offset.coerceIn(0, text.length)
        return text.substring(0, safeOffset) + insertedText + text.substring(safeOffset)
    }
}
