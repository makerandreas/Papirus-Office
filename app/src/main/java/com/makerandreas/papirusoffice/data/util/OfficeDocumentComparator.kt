package com.makerandreas.papirusoffice.data.util

import com.makerandreas.papirusoffice.data.*

data class RoundTripResult(
    val isSuccess: Boolean,
    val differences: List<String>
)

object OfficeDocumentComparator {

    fun compare(original: OfficeDocument, restored: OfficeDocument): RoundTripResult {
        val differences = mutableListOf<String>()

        // 1. Compare Metadata
        if (original.metadata.title.isNotEmpty() && original.metadata.title != restored.metadata.title) {
            differences.add("Metadata Title mismatch: expected '${original.metadata.title}', got '${restored.metadata.title}'")
        }
        if (original.metadata.author.isNotEmpty() && original.metadata.author != restored.metadata.author && original.metadata.author != restored.metadata.creator) {
            differences.add("Metadata Author mismatch: expected '${original.metadata.author}', got '${restored.metadata.author}'")
        }

        // 2. Helper to extract clean list of elements from body
        val origElements = flattenElements(original.body.elements)
        val restElements = flattenElements(restored.body.elements)

        if (origElements.size != restElements.size) {
            differences.add("Element count mismatch: expected ${origElements.size}, got ${restElements.size}")
        } else {
            for (i in origElements.indices) {
                val orig = origElements[i]
                val rest = restElements[i]

                when {
                    orig is OfficeParagraph && rest is OfficeParagraph -> {
                        if (orig.text != rest.text) {
                            differences.add("Paragraph[$i] text mismatch: expected '${orig.text}', got '${rest.text}'")
                        }
                        if (!orig.styleName.isNullOrEmpty() && orig.styleName != rest.styleName) {
                            differences.add("Paragraph[$i] styleName mismatch: expected '${orig.styleName}', got '${rest.styleName}'")
                        }
                        compareRuns("Paragraph[$i]", orig.runs, rest.runs, differences)
                    }
                    orig is OfficeHeading && rest is OfficeHeading -> {
                        if (orig.text != rest.text) {
                            differences.add("Heading[$i] text mismatch: expected '${orig.text}', got '${rest.text}'")
                        }
                        if (orig.level != rest.level) {
                            differences.add("Heading[$i] level mismatch: expected level ${orig.level}, got ${rest.level}")
                        }
                        if (!orig.styleName.isNullOrEmpty() && orig.styleName != rest.styleName) {
                            differences.add("Heading[$i] styleName mismatch: expected '${orig.styleName}', got '${rest.styleName}'")
                        }
                        compareRuns("Heading[$i]", orig.runs, rest.runs, differences)
                    }
                    orig is OfficeListItem && rest is OfficeListItem -> {
                        if (orig.text != rest.text) {
                            differences.add("ListItem[$i] text mismatch: expected '${orig.text}', got '${rest.text}'")
                        }
                        compareRuns("ListItem[$i]", orig.runs, rest.runs, differences)
                    }
                    orig is OfficeTable && rest is OfficeTable -> {
                        if (orig.rows.size != rest.rows.size) {
                            differences.add("Table[$i] row count mismatch: expected ${orig.rows.size}, got ${rest.rows.size}")
                        } else {
                            for (r in orig.rows.indices) {
                                val origRow = orig.rows[r]
                                val restRow = rest.rows[r]
                                if (origRow.cells.size != restRow.cells.size) {
                                    differences.add("Table[$i] row[$r] cell count mismatch: expected ${origRow.cells.size}, got ${restRow.cells.size}")
                                } else {
                                    for (c in origRow.cells.indices) {
                                        if (origRow.cells[c].text != restRow.cells[c].text) {
                                            differences.add("Table[$i] cell[$r][$c] text mismatch: expected '${origRow.cells[c].text}', got '${restRow.cells[c].text}'")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    orig::class != rest::class -> {
                        differences.add("Element[$i] class mismatch: expected ${orig::class.simpleName}, got ${rest::class.simpleName}")
                    }
                }
            }
        }

        val isSuccess = differences.isEmpty()
        return RoundTripResult(isSuccess, differences)
    }

    private fun compareRuns(
        label: String,
        origRuns: List<OfficeTextRun>,
        restRuns: List<OfficeTextRun>,
        differences: MutableList<String>
    ) {
        if (origRuns.isEmpty()) return
        if (restRuns.isEmpty()) {
            differences.add("$label runs mismatch: expected ${origRuns.size} runs, got 0 runs")
            return
        }

        val cleanOrig = origRuns.filter { it.text.isNotEmpty() }
        val cleanRest = restRuns.filter { it.text.isNotEmpty() }

        if (cleanOrig.size != cleanRest.size) {
            differences.add("$label run count mismatch: expected ${cleanOrig.size}, got ${cleanRest.size}")
            return
        }

        for (j in cleanOrig.indices) {
            val rOrig = cleanOrig[j]
            val rRest = cleanRest[j]

            if (rOrig.text != rRest.text) {
                differences.add("$label run[$j] text mismatch: expected '${rOrig.text}', got '${rRest.text}'")
            }
            if (rOrig.isBold != rRest.isBold) {
                differences.add("$label run[$j] isBold mismatch: expected ${rOrig.isBold}, got ${rRest.isBold}")
            }
            if (rOrig.isItalic != rRest.isItalic) {
                differences.add("$label run[$j] isItalic mismatch: expected ${rOrig.isItalic}, got ${rRest.isItalic}")
            }
            if (rOrig.isUnderline != rRest.isUnderline) {
                differences.add("$label run[$j] isUnderline mismatch: expected ${rOrig.isUnderline}, got ${rRest.isUnderline}")
            }
        }
    }

    private fun flattenElements(elements: List<OfficeElement>): List<OfficeElement> {
        return elements.map { elem ->
            when (elem) {
                is OfficeDocElement.ParagraphElement -> elem.paragraph
                is OfficeDocElement.TableElement -> elem.table
                is OfficeDocElement.ImageElement -> elem.image
                else -> elem
            }
        }
    }
}
