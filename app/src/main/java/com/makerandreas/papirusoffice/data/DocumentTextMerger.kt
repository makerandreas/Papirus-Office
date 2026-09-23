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
            is OfficeParagraph -> element.copy(text = text, runs = resliceRuns(element.text, element.runs, text))
            is OfficeHeading -> element.copy(text = text, runs = resliceRuns(element.text, element.runs, text))
            is OfficeListItem -> element.copy(text = text, runs = resliceRuns(element.text, element.runs, text))
            is OfficeDocElement.ParagraphElement ->
                OfficeDocElement.ParagraphElement(
                    element.paragraph.copy(
                        text = text,
                        runs = resliceRuns(element.paragraph.text, element.paragraph.runs, text)
                    )
                )
            else -> element
        }
    }

    /**
     * Keep run formatting across an edit by slicing the old runs at the common
     * prefix and suffix. Inserted middle text inherits the run covering
     * prefix-1 (the character before the insertion), not the run at prefix.
     * Empty original runs stay empty so unstyled paragraphs remain unstyled.
     */
    internal fun resliceRuns(
        oldText: String,
        oldRuns: List<OfficeTextRun>,
        newText: String
    ): List<OfficeTextRun> {
        if (oldRuns.isEmpty()) return emptyList()
        if (newText.isEmpty()) return emptyList()
        if (oldText == newText && oldRuns.joinToString("") { it.text } == oldText) return oldRuns

        val prefix = newText.commonPrefixWith(oldText).length
        val rawSuffix = newText.commonSuffixWith(oldText).length
        val suffixLen = minOf(rawSuffix, newText.length - prefix, oldText.length - prefix)

        val prefixRuns = sliceRuns(oldRuns, 0, prefix)
        val suffixRuns = sliceRuns(oldRuns, oldText.length - suffixLen, oldText.length)
        val middleLen = newText.length - prefix - suffixLen
        val middleRuns = if (middleLen > 0) {
            val inheritIndex = if (prefix > 0) prefix - 1 else 0
            val inherit = runCovering(oldRuns, inheritIndex)
            listOf(inherit.copy(text = newText.substring(prefix, prefix + middleLen)))
        } else {
            emptyList()
        }
        return coalesceRuns(prefixRuns + middleRuns + suffixRuns)
    }

    private fun sliceRuns(runs: List<OfficeTextRun>, start: Int, end: Int): List<OfficeTextRun> {
        if (start >= end || runs.isEmpty()) return emptyList()
        val out = ArrayList<OfficeTextRun>(runs.size)
        var offset = 0
        for (run in runs) {
            val runEnd = offset + run.text.length
            val sliceStart = maxOf(start, offset)
            val sliceEnd = minOf(end, runEnd)
            if (sliceEnd > sliceStart) {
                out.add(run.copy(text = run.text.substring(sliceStart - offset, sliceEnd - offset)))
            }
            offset = runEnd
            if (offset >= end) break
        }
        return out
    }

    private fun runCovering(runs: List<OfficeTextRun>, index: Int): OfficeTextRun {
        var offset = 0
        for (run in runs) {
            val runEnd = offset + run.text.length
            if (index < runEnd || run === runs.last()) return run
            offset = runEnd
        }
        return runs.last()
    }

    private fun coalesceRuns(runs: List<OfficeTextRun>): List<OfficeTextRun> {
        if (runs.size <= 1) return runs.filter { it.text.isNotEmpty() }
        val out = ArrayList<OfficeTextRun>(runs.size)
        for (run in runs) {
            if (run.text.isEmpty()) continue
            val prev = out.lastOrNull()
            if (prev != null &&
                prev.isBold == run.isBold &&
                prev.isItalic == run.isItalic &&
                prev.isUnderline == run.isUnderline &&
                prev.styleName == run.styleName &&
                prev.characterStyle == run.characterStyle &&
                prev.hyperlink == run.hyperlink
            ) {
                out[out.lastIndex] = prev.copy(text = prev.text + run.text)
            } else {
                out.add(run)
            }
        }
        return out
    }
}
