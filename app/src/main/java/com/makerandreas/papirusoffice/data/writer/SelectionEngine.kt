package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.DocumentTextMerger
import com.makerandreas.papirusoffice.data.DocumentTextProjection
import com.makerandreas.papirusoffice.data.OfficeDocument

object SelectionEngine {
    fun extract(doc: OfficeDocument, selection: SelectionRange): String =
        extract(DocumentTextProjection.editorText(doc), selection)

    fun extract(text: String, selection: SelectionRange): String {
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)
        return if (start < end) text.substring(start, end) else ""
    }

    fun delete(doc: OfficeDocument, selection: SelectionRange): OfficeDocument =
        delete(doc, selection, DocumentTextProjection.editorText(doc))

    /**
     * Applies offsets from the editor's current flat text to the document model.
     * [editorText] is supplied by Inky when its buffer may be ahead of the model.
     */
    fun delete(doc: OfficeDocument, selection: SelectionRange, editorText: String): OfficeDocument {
        val start = selection.min.coerceIn(0, editorText.length)
        val end = selection.max.coerceIn(0, editorText.length)
        val newText = if (start < end) editorText.removeRange(start, end) else editorText
        return DocumentTextMerger.mergeEditedText(doc, newText)
    }

    fun delete(text: String, selection: SelectionRange): String {
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)
        return if (start < end) text.removeRange(start, end) else text
    }

    fun insert(doc: OfficeDocument, offset: Int, insertedText: String): OfficeDocument {
        val fullText = DocumentTextProjection.editorText(doc)
        val safeOffset = offset.coerceIn(0, fullText.length)
        val newText = fullText.substring(0, safeOffset) + insertedText + fullText.substring(safeOffset)
        return DocumentTextMerger.mergeEditedText(doc, newText)
    }

    fun insert(text: String, offset: Int, insertedText: String): String {
        val safeOffset = offset.coerceIn(0, text.length)
        return text.substring(0, safeOffset) + insertedText + text.substring(safeOffset)
    }
}
