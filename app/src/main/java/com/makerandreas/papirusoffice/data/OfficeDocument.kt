package com.makerandreas.papirusoffice.data

import java.io.File

// ==========================================
// LAYER 1: OfficeDocument (Root Document Model)
// ==========================================
data class OdtPackageData(
    val entries: Map<String, ByteArray> = emptyMap(),
    val originalContentXml: String? = null,
    val originalStylesXml: String? = null,
    val originalManifestXml: String? = null,
    val originalMetaXml: String? = null,
    val originalSettingsXml: String? = null
)

data class OfficeDocument(
    val metadata: DocumentMetadata = DocumentMetadata(),
    val styles: DocumentStyles = DocumentStyles(),
    val sections: List<DocumentSection> = emptyList(),
    val body: DocumentBody = DocumentBody(),
    val resources: OfficeResources = OfficeResources(),
    val properties: DocumentProperties = DocumentProperties(),
    val parserReport: ParserReport = ParserReport(),
    val header: DocumentHeader = DocumentHeader(),
    val footer: DocumentFooter = DocumentFooter(),
    val footnote: DocumentFootnote = DocumentFootnote(),
    val odtPackageData: OdtPackageData? = null
)

fun OfficeDocument.toPlainText(): String {
    return body.elements.joinToString("\n\n") { element ->
        when (element) {
            is OfficeParagraph -> element.text
            is OfficeDocElement.ParagraphElement -> element.paragraph.text
            is OfficeHeading -> element.text
            is OfficeListItem -> "${element.bullet} ${element.text}"
            is OfficeTable -> element.rows.joinToString("\n") { row -> row.cells.joinToString("\t") { cell -> cell.text } }
            is OfficeDocElement.TableElement -> element.table.rows.joinToString("\n") { row -> row.cells.joinToString("\t") { cell -> cell.text } }
            else -> ""
        }
    }
}

// ==========================================
// LAYER 2: Body as sequence of OfficeElements
// ==========================================
sealed interface OfficeElement

data class DocumentBody(
    val elements: List<OfficeElement> = emptyList()
)

// Legacy Compatibility wrapper mapping to OfficeDocElement
@Deprecated("Use direct implementors of OfficeElement (e.g. OfficeParagraph, OfficeTable) for modern flows.")
sealed class OfficeDocElement : OfficeElement {
    data class ParagraphElement(val paragraph: OfficeParagraph) : OfficeDocElement()
    data class TableElement(val table: OfficeTable) : OfficeDocElement()
    data class ImageElement(val image: OfficeImage) : OfficeDocElement()
    data class ShapeElement(val shape: OfficeShape) : OfficeDocElement()
    data class BookmarkElement(val bookmark: OfficeBookmark) : OfficeDocElement()
    data class HyperlinkElement(val hyperlink: OfficeHyperlink) : OfficeDocElement()
    data class FieldElement(val field: OfficeField) : OfficeDocElement()
}

fun OfficeElement.extractParagraph(): OfficeParagraph? {
    return when (this) {
        is OfficeParagraph -> this
        is OfficeDocElement.ParagraphElement -> this.paragraph
        else -> null
    }
}

fun OfficeElement.replaceParagraph(newPara: OfficeParagraph): OfficeElement {
    return when (this) {
        is OfficeDocElement.ParagraphElement -> OfficeDocElement.ParagraphElement(newPara)
        else -> newPara
    }
}

// ==========================================
// LAYER 3: Rich Paragraphs & Text Runs
// ==========================================
data class OfficeParagraph(
    val text: String,
    val styleName: String? = null,
    val alignment: String? = null, // "Left", "Center", "Right", "Justify"
    val spacing: Float = 0f,
    val indent: Float = 0f,
    val outlineLevel: Int = 0,
    val runs: List<OfficeTextRun> = emptyList(),
    val bookmark: String? = null
) : OfficeElement

data class OfficeHeading(
    val text: String,
    val styleName: String? = null,
    val level: Int = 1,
    val runs: List<OfficeTextRun> = emptyList()
) : OfficeElement

data class OfficeListItem(
    val text: String,
    val bullet: String = "• ",
    val runs: List<OfficeTextRun> = emptyList()
) : OfficeElement

data class OfficeTextRun(
    val text: String,
    val characterStyle: String? = null,
    val hyperlink: String? = null,
    val field: String? = null,
    val language: String = "en-US",
    val styleName: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

data class OfficeTable(
    val rows: List<OfficeTableRow>,
    val numColumns: Int = 0
) : OfficeElement

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
) : OfficeElement

data class OfficeShape(
    val type: String,
    val bounds: OfficeRect = OfficeRect(),
    val fillColorHex: String? = null
) : OfficeElement

data class OfficeFormula(
    val formulaSyntax: String,
    val displayMathML: String? = null,
    val displayOMML: String? = null
) : OfficeElement

data class OfficeBookmark(
    val name: String
) : OfficeElement

data class OfficeComment(
    val author: String,
    val text: String,
    val date: String
) : OfficeElement

data class OfficeFootnoteElement(
    val text: String,
    val noteId: String
) : OfficeElement

data class OfficeSection(
    val name: String,
    val elements: List<OfficeElement> = emptyList()
) : OfficeElement

object OfficePageBreak : OfficeElement

data class OfficeHyperlink(
    val text: String,
    val targetUri: String
) : OfficeElement

data class OfficeField(
    val type: String, // e.g., "PageNumber", "Date"
    val value: String
) : OfficeElement

data class OfficeRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)

// ==========================================
// LAYER 4: Separated Style Family
// ==========================================
data class DocumentStyles(
    val paragraphStyles: Map<String, ParagraphStyle> = emptyMap(),
    val characterStyles: Map<String, CharacterStyle> = emptyMap()
)

data class ParagraphStyle(
    val name: String,
    val fontSizeSp: Float = 12f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val colorHex: String? = null,
    val alignment: String = "Left",
    val fontFamily: String? = null,
    val parentStyleName: String? = null
)

data class CharacterStyle(
    val name: String,
    val fontSizeSp: Float = 12f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val colorHex: String? = null,
    val fontFamily: String? = null,
    val parentStyleName: String? = null
)

// ==========================================
// LAYER 5: Resource Manager
// ==========================================
data class OfficeResources(
    val images: Map<String, OfficeImage> = emptyMap(),
    val embeddedFonts: List<String> = emptyList(),
    val media: List<String> = emptyList(),
    val objects: List<String> = emptyList(),
    val charts: List<String> = emptyList()
)

// ==========================================
// LAYER 6: Metadata Model
// ==========================================
data class DocumentMetadata(
    val title: String = "",
    val subject: String = "",
    val author: String = "",
    val creator: String = "",
    val keywords: List<String> = emptyList(),
    val language: String = "en-US",
    val created: String = "",
    val modified: String = "",
    val creationDate: String = "",
    val revision: String = "1",
    val generator: String = "Papirus Office",
    val wordCount: Int = 0,
    val paragraphCount: Int = 0,
    val characterCount: Int = 0,
    val description: String = ""
)

// ==========================================
// LAYER 7: Document Cursor
// ==========================================
data class DocumentCursor(
    val elementIndex: Int = 0,
    val paragraphIndex: Int = 0,
    val runIndex: Int = 0,
    val offset: Int = 0
)

data class DocumentProperties(
    val isAutoCorrectEnabled: Boolean = true,
    val isAutoCapitalizationEnabled: Boolean = true,
    val isReadOnly: Boolean = false
)

data class DocumentSection(
    val name: String = "",
    val elements: List<OfficeElement> = emptyList()
)

data class DocumentHeader(
    val elements: List<OfficeElement> = emptyList()
)

data class DocumentFooter(
    val elements: List<OfficeElement> = emptyList()
)

data class DocumentFootnote(
    val elements: List<OfficeElement> = emptyList()
)

data class OfficeFile(
    val file: File,
    val uri: String? = null,
    val displayName: String = file.name
)

// ==========================================
// LAYER 8: Rich Adapter (OfficeParsedDocument -> OfficeDocument)
// ==========================================
fun OfficeParsedDocument.toOfficeDocument(): OfficeDocument {
    val docElements = this.elements.map { elem ->
        when (elem) {
            is OfficeDocumentElement.Paragraph -> {
                // Return as standard OfficeDocElement legacy wrapper to guarantee compatibility with existing screens
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
        body = DocumentBody(elements = docElements),
        odtPackageData = this.odtPackageData
    )
}

// ==========================================
// LAYER 10: Bridge to Layout Engine / Layout Tree
// ==========================================
data class LayoutNode(
    val element: OfficeElement,
    val widthDp: Float = 0f,
    val heightDp: Float = 0f,
    val paddingDp: Float = 8f
)

data class LayoutTree(
    val rootNodes: List<LayoutNode> = emptyList()
)

fun OfficeDocument.toLayoutTree(): LayoutTree {
    val nodes = this.body.elements.map { elem ->
        when (elem) {
            is OfficeDocElement.ParagraphElement -> LayoutNode(elem, paddingDp = 4f)
            is OfficeDocElement.TableElement -> LayoutNode(elem, paddingDp = 8f)
            is OfficeDocElement.ImageElement -> LayoutNode(elem, widthDp = elem.image.widthDp, heightDp = elem.image.heightDp, paddingDp = 12f)
            is OfficeParagraph -> LayoutNode(elem, paddingDp = 4f)
            is OfficeTable -> LayoutNode(elem, paddingDp = 8f)
            is OfficeImage -> LayoutNode(elem, widthDp = elem.widthDp, heightDp = elem.heightDp, paddingDp = 12f)
            else -> LayoutNode(elem, paddingDp = 6f)
        }
    }
    return LayoutTree(rootNodes = nodes)
}
