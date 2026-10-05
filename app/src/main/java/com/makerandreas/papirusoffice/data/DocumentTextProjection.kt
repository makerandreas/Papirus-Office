package com.makerandreas.papirusoffice.data

/**
 * Canonical flat-text view used by Inky's editor and its selection offsets.
 * It projects body paragraphs, headings, and list-item text in document order,
 * joined with [BLOCK_SEPARATOR]. Structural elements such as tables, images,
 * and page breaks do not consume editor-text offsets; generated list labels are
 * also excluded because they are not part of the editable text field.
 *
 * Keep selection, merging, and element-window mapping on this same projection.
 * [OfficeDocument.toPlainText] intentionally has broader diagnostic/export
 * semantics and is not an editor-coordinate source.
 */
object DocumentTextProjection {
    const val BLOCK_SEPARATOR = "\n\n"

    fun editorText(document: OfficeDocument): String = editorText(document.body.elements)

    fun editorText(document: OfficeParsedDocument): String =
        editorText(document.toOfficeDocument())

    /** Uses the parsed structure when available; plain-text imports keep their raw text. */
    fun editorText(result: DocxParseResult): String =
        result.parsedDocument?.let { editorText(it) } ?: result.text

    fun editorText(elements: Iterable<OfficeElement>): String =
        elements.mapNotNull { elementText(it) }.joinToString(BLOCK_SEPARATOR)

    @Suppress("DEPRECATION")
    fun elementText(element: OfficeElement): String? = when (element) {
        is OfficeParagraph -> element.text
        is OfficeHeading -> element.text
        is OfficeListItem -> element.text
        is OfficeDocElement.ParagraphElement -> element.paragraph.text
        else -> null
    }

    fun isTextual(element: OfficeElement): Boolean = elementText(element) != null
}
