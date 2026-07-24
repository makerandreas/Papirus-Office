package com.makerandreas.papirusoffice.data

import java.io.File

/**
 * Modern data model representing parsed document structure from ODT or DOCX format.
 */
sealed class OfficeDocumentElement {
    data class Paragraph(
        val text: String,
        val styleName: String? = null,
        val runs: List<TextRun> = emptyList()
    ) : OfficeDocumentElement()

    data class Heading(
        val text: String,
        val level: Int = 1,
        val styleName: String? = null
    ) : OfficeDocumentElement()

    data class ListItem(
        val text: String,
        val level: Int = 1,
        val bullet: String = "• "
    ) : OfficeDocumentElement()

    data class Table(
        val rows: List<TableRow>,
        val numColumns: Int = 0
    ) : OfficeDocumentElement()

    data class ImageElement(
        val imagePath: String,
        val imageFile: File? = null,
        val widthDp: Float = 0f,
        val heightDp: Float = 0f
    ) : OfficeDocumentElement()
}

data class TableRow(
    val cells: List<TableCell>
)

data class TableCell(
    val text: String,
    val paragraphs: List<OfficeDocumentElement.Paragraph> = emptyList()
)

data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

data class OfficeParsedDocument(
    val elements: List<OfficeDocumentElement> = emptyList(),
    val rawXml: String = "",
    val plainText: String = "",
    val extractedImages: Map<String, File> = emptyMap(),
    val isOdt: Boolean = false,
    val isDocx: Boolean = false,
    val isParsingFailed: Boolean = false,
    val failureReason: String? = null
)
