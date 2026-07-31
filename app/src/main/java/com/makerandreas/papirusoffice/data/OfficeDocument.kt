package com.makerandreas.papirusoffice.data

import java.io.File

data class OfficeDocument(
    val metadata: DocumentMetadata = DocumentMetadata(),
    val styles: DocumentStyles = DocumentStyles(),
    val sections: List<DocumentSection> = emptyList(),
    val body: DocumentBody = DocumentBody(),
    val header: DocumentHeader = DocumentHeader(),
    val footer: DocumentFooter = DocumentFooter(),
    val footnote: DocumentFootnote = DocumentFootnote()
)

data class DocumentMetadata(
    val title: String = "",
    val creator: String = "",
    val description: String = "",
    val subject: String = "",
    val creationDate: String = "",
    val language: String = "en-US",
    val wordCount: Int = 0,
    val paragraphCount: Int = 0,
    val characterCount: Int = 0
)

data class DocumentStyles(
    val paragraphStyles: Map<String, ParagraphStyle> = emptyMap(),
    val characterStyles: Map<String, CharacterStyle> = emptyMap()
)

data class ParagraphStyle(
    val name: String,
    val fontSizeSp: Float = 12f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val colorHex: String? = null
)

data class CharacterStyle(
    val name: String,
    val fontSizeSp: Float = 12f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false
)

data class DocumentSection(
    val name: String = "",
    val elements: List<OfficeDocElement> = emptyList()
)

data class DocumentBody(
    val elements: List<OfficeDocElement> = emptyList()
)

sealed class OfficeDocElement {
    data class ParagraphElement(val paragraph: OfficeParagraph) : OfficeDocElement()
    data class TableElement(val table: OfficeTable) : OfficeDocElement()
    data class ImageElement(val image: OfficeImage) : OfficeDocElement()
    data class ShapeElement(val shape: OfficeShape) : OfficeDocElement()
    data class BookmarkElement(val bookmark: OfficeBookmark) : OfficeDocElement()
    data class HyperlinkElement(val hyperlink: OfficeHyperlink) : OfficeDocElement()
    data class FieldElement(val field: OfficeField) : OfficeDocElement()
}

data class OfficeParagraph(
    val text: String,
    val styleName: String? = null,
    val runs: List<OfficeTextRun> = emptyList()
)

data class OfficeTextRun(
    val text: String,
    val styleName: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

data class OfficeTable(
    val rows: List<OfficeTableRow>,
    val numColumns: Int = 0
)

data class OfficeTableRow(
    val cells: List<OfficeTableCell>
)

data class OfficeTableCell(
    val text: String,
    val paragraphs: List<OfficeParagraph> = emptyList()
)

data class OfficeImage(
    val imagePath: String,
    val imageFile: File? = null,
    val widthDp: Float = 0f,
    val heightDp: Float = 0f
)

data class OfficeShape(
    val type: String,
    val bounds: OfficeRect = OfficeRect(),
    val fillColorHex: String? = null
)

data class OfficeBookmark(
    val name: String
)

data class OfficeHyperlink(
    val text: String,
    val targetUri: String
)

data class OfficeField(
    val type: String, // e.g., "PageNumber", "Date"
    val value: String
)

data class OfficeRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)

data class DocumentHeader(
    val elements: List<OfficeDocElement> = emptyList()
)

data class DocumentFooter(
    val elements: List<OfficeDocElement> = emptyList()
)

data class DocumentFootnote(
    val elements: List<OfficeDocElement> = emptyList()
)

data class OfficeFile(
    val file: File,
    val uri: String? = null,
    val displayName: String = file.name
)

fun OfficeParsedDocument.toOfficeDocument(): OfficeDocument {
    val docElements = this.elements.map { elem ->
        when (elem) {
            is OfficeDocumentElement.Paragraph -> {
                OfficeDocElement.ParagraphElement(
                    OfficeParagraph(
                        text = elem.text,
                        styleName = elem.styleName,
                        runs = elem.runs.map { run ->
                            OfficeTextRun(
                                text = run.text,
                                isBold = run.isBold,
                                isItalic = run.isItalic,
                                isUnderline = run.isUnderline
                            )
                        }
                    )
                )
            }
            is OfficeDocumentElement.Heading -> {
                OfficeDocElement.ParagraphElement(
                    OfficeParagraph(
                        text = elem.text,
                        styleName = elem.styleName ?: "Heading ${elem.level}",
                        runs = listOf(OfficeTextRun(text = elem.text, isBold = true))
                    )
                )
            }
            is OfficeDocumentElement.ListItem -> {
                OfficeDocElement.ParagraphElement(
                    OfficeParagraph(
                        text = elem.bullet + elem.text,
                        runs = listOf(OfficeTextRun(text = elem.bullet + elem.text))
                    )
                )
            }
            is OfficeDocumentElement.Table -> {
                OfficeDocElement.TableElement(
                    OfficeTable(
                        numColumns = elem.numColumns,
                        rows = elem.rows.map { row ->
                            OfficeTableRow(
                                cells = row.cells.map { cell ->
                                    OfficeTableCell(
                                        text = cell.text,
                                        paragraphs = cell.paragraphs.map { cellPara ->
                                            OfficeParagraph(
                                                text = cellPara.text,
                                                styleName = cellPara.styleName,
                                                runs = cellPara.runs.map { cellRun ->
                                                    OfficeTextRun(
                                                        text = cellRun.text,
                                                        isBold = cellRun.isBold,
                                                        isItalic = cellRun.isItalic,
                                                        isUnderline = cellRun.isUnderline
                                                    )
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                )
            }
            is OfficeDocumentElement.ImageElement -> {
                OfficeDocElement.ImageElement(
                    OfficeImage(
                        imagePath = elem.imagePath,
                        imageFile = elem.imageFile,
                        widthDp = elem.widthDp,
                        heightDp = elem.heightDp
                    )
                )
            }
        }
    }

    val formatStr = when {
        isOdt -> "ODT"
        isDocx -> "DOCX"
        isOds -> "ODS"
        isXlsx -> "XLSX"
        isOdp -> "ODP"
        isPptx -> "PPTX"
        else -> "TXT"
    }

    val metadata = DocumentMetadata(
        title = "Document",
        creator = "Papirus Office",
        wordCount = this.plainText.split(Regex("\\s+")).count { it.isNotBlank() },
        paragraphCount = this.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().size,
        characterCount = this.plainText.length
    )

    return OfficeDocument(
        metadata = metadata,
        body = DocumentBody(elements = docElements)
    )
}

