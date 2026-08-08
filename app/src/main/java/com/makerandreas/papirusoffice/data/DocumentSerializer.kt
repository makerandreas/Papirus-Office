package com.makerandreas.papirusoffice.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocumentSerializer(private val context: Context) {
    private val officeParser = OfficeDocumentParser(context)
    
    suspend fun serializeToFormat(
        document: OfficeDocument, 
        format: String, 
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val parsedElements = document.body.elements.mapNotNull { element ->
            when (element) {
                is com.makerandreas.papirusoffice.data.OfficeDocElement.ParagraphElement -> OfficeDocumentElement.Paragraph(text = element.paragraph.text)
                is com.makerandreas.papirusoffice.data.OfficeParagraph -> OfficeDocumentElement.Paragraph(text = element.text)
                is com.makerandreas.papirusoffice.data.OfficeHeading -> OfficeDocumentElement.Heading(text = element.text, level = element.level)
                is com.makerandreas.papirusoffice.data.OfficeListItem -> OfficeDocumentElement.ListItem(text = element.text, bullet = element.bullet)
                is com.makerandreas.papirusoffice.data.OfficeDocElement.TableElement -> OfficeDocumentElement.Table(rows = element.table.rows.map { r -> TableRow(cells = r.cells.map { c -> TableCell(text = c.text, paragraphs = emptyList()) }) }, numColumns = element.table.numColumns)
                is com.makerandreas.papirusoffice.data.OfficeTable -> OfficeDocumentElement.Table(rows = element.rows.map { r -> TableRow(cells = r.cells.map { c -> TableCell(text = c.text, paragraphs = emptyList()) }) }, numColumns = element.numColumns)
                else -> null
            }
        }
        
        val parsedDoc = OfficeParsedDocument(
            elements = parsedElements,
            rawXml = "",
            plainText = parsedElements.joinToString("\n\n") { 
                when (it) {
                    is OfficeDocumentElement.Paragraph -> it.text
                    is OfficeDocumentElement.Heading -> it.text
                    is OfficeDocumentElement.ListItem -> it.text
                    is OfficeDocumentElement.Table -> it.rows.joinToString("\n") { row -> row.cells.joinToString("\t") { cell -> cell.text } }
                    else -> ""
                }
            },
            extractedImages = emptyMap(),
            isOdt = format.equals("ODT", ignoreCase = true),
            isDocx = format.equals("DOCX", ignoreCase = true),
            isOds = format.equals("ODS", ignoreCase = true),
            isXlsx = format.equals("XLSX", ignoreCase = true),
            isOdp = format.equals("ODP", ignoreCase = true),
            isPptx = format.equals("PPTX", ignoreCase = true),
            isParsingFailed = false
        )
        
        return@withContext when (format.uppercase()) {
            "ODT" -> officeParser.saveOdtDocument(outputFile, parsedDoc)
            "DOCX" -> {
                // For DOCX we can use the old raw generator for now or bridge it
                // Actually DocxDocumentParser had the logic. Let's keep it here.
                val parser = DocxDocumentParser(context)
                parser.saveDocument(outputFile, parsedDoc.plainText)
            }
            "ODS" -> officeParser.saveOdsDocument(outputFile, parsedDoc)
            "XLSX" -> officeParser.saveXlsxDocument(outputFile, parsedDoc)
            "ODP" -> officeParser.saveOdpDocument(outputFile, parsedDoc)
            "PPTX" -> officeParser.savePptxDocument(outputFile, parsedDoc)
            else -> {
                outputFile.writeText(parsedDoc.plainText)
                true
            }
        }
    }
}
