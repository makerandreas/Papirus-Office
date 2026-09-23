package com.makerandreas.papirusoffice.data

/**
 * Merges edited plain text back onto an existing [OfficeDocument] without
 * flattening headings, tables, images, or page breaks into a bag of
 * [OfficeParagraph]s. Textual elements consume `\n\n` blocks in order;
 * structural elements are preserved in place.
 */
object DocumentTextMerger {

    fun mergeEditedText(document: OfficeDocument, editedText: String): OfficeDocument {
        val original = document.body.elements
        if (original.isEmpty()) {
            val blocks = splitBlocks(editedText)
            val elements = if (blocks.isEmpty()) {
                listOf(OfficeParagraph(""))
            } else {
                blocks.map { OfficeParagraph(text = it) }
            }
            return document.copy(body = DocumentBody(elements), isModified = true)
        }

        val blocks = splitBlocks(editedText).toMutableList()
        val result = mutableListOf<OfficeElement>()

        for (element in original) {
            when {
                isStructural(element) -> result.add(element)
                isTextual(element) -> {
                    // Edited text is authoritative: elements past the edited
                    // block count clear instead of resurrecting stale text.
                    val block = if (blocks.isNotEmpty()) blocks.removeAt(0) else ""
                    result.add(replaceText(element, block))
                }
                else -> result.add(element)
            }
        }

        // Trailing blocks are typed content too (e.g. Enter at document end);
        // dropping blank ones would orphan the caret's target paragraph.
        for (extra in blocks) {
            result.add(OfficeParagraph(text = extra))
        }

        return document.copy(
            body = DocumentBody(elements = result),
            isModified = true
        )
    }

    private fun splitBlocks(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        return text.split("\n\n")
    }

    private fun isStructural(element: OfficeElement): Boolean {
        return when (element) {
            is OfficeImage,
            is OfficeTable,
            is OfficePageBreak,
            is OfficeBookmark,
            is OfficeShape,
            is OfficeComment,
            is OfficeSection,
            is OfficeFootnoteElement,
            is OfficeHyperlink,
            is OfficeField,
            is OfficeFormula,
            is OfficeDocElement.ImageElement,
            is OfficeDocElement.TableElement,
            is OfficeDocElement.BookmarkElement,
            is OfficeDocElement.ShapeElement -> true
            else -> false
        }
    }

    fun isTextual(element: OfficeElement): Boolean {
        return when (element) {
            is OfficeParagraph,
            is OfficeHeading,
            is OfficeListItem,
            is OfficeDocElement.ParagraphElement -> true
            else -> false
        }
    }

    // The legacy wrapper arm is defensive: runtime import paths emit direct
    // implementors. Full OfficeDocElement retirement is tracked in the plan.
    @Suppress("DEPRECATION")
    private fun replaceText(element: OfficeElement, text: String): OfficeElement {
        return when (element) {
            is OfficeParagraph -> element.copy(text = text)
            is OfficeHeading -> element.copy(text = text)
            is OfficeListItem -> element.copy(text = text)
            is OfficeDocElement.ParagraphElement ->
                OfficeDocElement.ParagraphElement(element.paragraph.copy(text = text))
            else -> element
        }
    }
}
