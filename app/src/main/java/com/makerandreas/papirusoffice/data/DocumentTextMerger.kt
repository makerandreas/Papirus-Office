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
                    val block = if (blocks.isNotEmpty()) blocks.removeAt(0) else textualText(element)
                    result.add(replaceText(element, block))
                }
                else -> result.add(element)
            }
        }

        for (extra in blocks) {
            if (extra.isNotBlank()) {
                result.add(OfficeParagraph(text = extra))
            }
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

    private fun isTextual(element: OfficeElement): Boolean {
        return when (element) {
            is OfficeParagraph,
            is OfficeHeading,
            is OfficeListItem,
            is OfficeDocElement.ParagraphElement -> true
            else -> false
        }
    }

    private fun textualText(element: OfficeElement): String {
        return when (element) {
            is OfficeParagraph -> element.text
            is OfficeHeading -> element.text
            is OfficeListItem -> element.text
            is OfficeDocElement.ParagraphElement -> element.paragraph.text
            else -> ""
        }
    }

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
