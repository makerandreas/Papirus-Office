package com.makerandreas.papirusoffice.data

import com.makerandreas.papirusoffice.data.util.ZipSafe
import com.makerandreas.papirusoffice.data.util.readCappedBytes
import android.content.Context
import com.makerandreas.papirusoffice.data.writer.OdtDocumentParser
import com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter
import com.makerandreas.papirusoffice.data.odf.AndroidOdfImportDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Explicit document serializer contract for reading and writing OfficeDocument
 */
interface DocumentSerializerContract {
    suspend fun read(source: DocumentReference, context: Context): OfficeDocument
    suspend fun write(
        document: OfficeDocument,
        destination: DocumentReference,
        context: Context
    ): Boolean
}

class OdtDocumentSerializer : DocumentSerializerContract {
    private val writer = OdtDocumentWriter()

    override suspend fun read(source: DocumentReference, context: Context): OfficeDocument = withContext(Dispatchers.IO) {
        val bytes = when (source) {
            is DocumentReference.LocalFile -> {
                if (source.file.exists() && source.file.length() in 1..ZipSafe.MAX_DOCUMENT_BYTES) source.file.readBytes() else ByteArray(0)
            }
            is DocumentReference.SafUri -> {
                context.contentResolver.openInputStream(source.uri)?.use { it.readCappedBytes(ZipSafe.MAX_DOCUMENT_BYTES) } ?: ByteArray(0)
            }
            else -> ByteArray(0)
        }
        if (bytes.isEmpty()) return@withContext OfficeDocument()
        return@withContext OdtDocumentParser(AndroidOdfImportDiagnostics(context)).parse(bytes)
    }

    override suspend fun write(
        document: OfficeDocument,
        destination: DocumentReference,
        context: Context
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val bytes = writer.write(document)
            when (destination) {
                is DocumentReference.LocalFile -> {
                    destination.file.writeBytes(bytes)
                    true
                }
                is DocumentReference.SafUri -> {
                    val output = context.contentResolver.openOutputStream(destination.uri)
                        ?: return@withContext false
                    output.use { stream ->
                        stream.write(bytes)
                        stream.flush()
                    }
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            PapirusLogger.e("OdtDocumentSerializer", "Write failed", e)
            false
        }
    }
}

class DocxDocumentSerializer : DocumentSerializerContract {

    override suspend fun read(source: DocumentReference, context: Context): OfficeDocument = withContext(Dispatchers.IO) {
        val file = when (source) {
            is DocumentReference.LocalFile -> source.file
            is DocumentReference.SafUri -> {
                val tempFile = File.createTempFile("temp_docx", ".docx", context.cacheDir)
                context.contentResolver.openInputStream(source.uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                tempFile
            }
            else -> return@withContext OfficeDocument()
        }

        if (!file.exists() || file.length() == 0L) return@withContext OfficeDocument()

        val docxParser = DocxDocumentParser(context)
        val parseResult = docxParser.parseDocument(file)
        val parsed = parseResult.parsedDocument
        if (parsed != null && !parsed.isParsingFailed) {
            return@withContext parsed.toOfficeDocument()
        }
        val elements = if (parseResult.text.isBlank()) {
            listOf(OfficeParagraph(""))
        } else {
            parseResult.text.split("\n\n").map { OfficeParagraph(it) }
        }
        return@withContext OfficeDocument(
            metadata = DocumentMetadata(title = file.name),
            body = DocumentBody(elements = elements)
        )
    }

    override suspend fun write(
        document: OfficeDocument,
        destination: DocumentReference,
        context: Context
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val docxParser = DocxDocumentParser(context)

            when (destination) {
                is DocumentReference.LocalFile -> {
                    docxParser.saveDocument(destination.file, document)
                }
                is DocumentReference.SafUri -> {
                    val tempFile = File.createTempFile("temp_write_docx", ".docx", context.cacheDir)
                    try {
                        if (!docxParser.saveDocument(tempFile, document)) return@withContext false
                        val output = context.contentResolver.openOutputStream(destination.uri) ?: return@withContext false
                        output.use { stream -> tempFile.inputStream().use { input -> input.copyTo(stream) } }
                        true
                    } finally {
                        tempFile.delete()
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            PapirusLogger.e("DocxDocumentSerializer", "Write failed", e)
            false
        }
    }
}

class DocumentSerializer(private val context: Context) {
    private val odtSerializer = OdtDocumentSerializer()
    private val docxSerializer = DocxDocumentSerializer()

    suspend fun serializeToFormat(
        document: OfficeDocument,
        format: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val dest = DocumentReference.LocalFile(outputFile)
        return@withContext when (format.uppercase()) {
            "ODT" -> odtSerializer.write(document, dest, context)
            "DOCX" -> docxSerializer.write(document, dest, context)
            else -> {
                val officeParser = OfficeDocumentParser(context)
                val parsedDoc = document.toOfficeParsedDocument(format)
                when (format.uppercase()) {
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
    }
}

internal fun OfficeDocument.toOfficeParsedDocument(format: String): OfficeParsedDocument {
    // Element kinds without a parsed form are dropped below; sidecar ranges are
    // remapped through oldToNew so they keep addressing the same content (audit-015 F-5).
    val oldToNew = IntArray(body.elements.size) { -1 }
    var nextIndex = 0
    val parsedElements = body.elements.mapIndexedNotNull { oldIndex, element ->
        when (element) {
            is OfficeDocElement.ParagraphElement -> element.paragraph.toParsedParagraph()
            is OfficeParagraph -> element.toParsedParagraph()
            is OfficeHeading -> OfficeDocumentElement.Heading(
                text = element.text,
                level = element.level,
                styleName = element.styleName,
                runs = element.runs.map { it.toParsedRun() },
                pageBreakOffsets = element.pageBreakOffsets,
                bookmarks = element.bookmarks.map { it.name }
            )
            is OfficeListItem -> OfficeDocumentElement.ListItem(
                text = element.text,
                bullet = element.bullet,
                level = element.level,
                isOrdered = element.isOrdered,
                styleName = element.styleName,
                runs = element.runs.map { it.toParsedRun() },
                labelFontSizeSp = element.labelFontSizeSp,
                labelFontFamily = element.labelFontFamily,
                bookmarks = element.bookmarks.map { it.name }
            )
            is OfficeDocElement.TableElement -> element.table.toParsedTable()
            is OfficeTable -> element.toParsedTable()
            is OfficeDocElement.ImageElement -> element.image.toParsedImage()
            is OfficeImage -> element.toParsedImage()
            is OfficePageBreak -> OfficeDocumentElement.PageBreak
            else -> null
        }?.also { oldToNew[oldIndex] = nextIndex++ }
    }
    return OfficeParsedDocument(
        elements = parsedElements,
        rawXml = "",
        plainText = toPlainText(),
        extractedImages = emptyMap(),
        isOdt = format.equals("ODT", ignoreCase = true),
        isDocx = format.equals("DOCX", ignoreCase = true),
        isOds = format.equals("ODS", ignoreCase = true),
        isXlsx = format.equals("XLSX", ignoreCase = true),
        isOdp = format.equals("ODP", ignoreCase = true),
        isPptx = format.equals("PPTX", ignoreCase = true),
        isParsingFailed = false,
        styles = styles,
        metadata = metadata,
        bookmarks = bookmarks.map { it.name },
        authoredIndexes = DocumentRanges.remapIndexes(authoredIndexes, oldToNew, parsedElements.size),
        namedSectionRanges = DocumentRanges.remapSections(namedSectionRanges, oldToNew, parsedElements.size)
    )
}

private fun OfficeParagraph.toParsedParagraph(): OfficeDocumentElement.Paragraph =
    OfficeDocumentElement.Paragraph(
        text = text,
        styleName = styleName,
        runs = runs.map { it.toParsedRun() },
        pageBreakOffsets = pageBreakOffsets,
        bookmarks = (bookmarks.map { it.name } + listOfNotNull(bookmark)).distinct()
    )

private fun OfficeTextRun.toParsedRun(): TextRun = TextRun(
    text = text,
    isBold = isBold,
    isItalic = isItalic,
    isUnderline = isUnderline,
    styleName = styleName ?: characterStyle,
    hyperlink = hyperlink,
    colorHex = colorHex,
    highlight = highlight,
    isHidden = isHidden,
    fontSizeSp = fontSizeSp,
    fontFamily = fontFamily
)

private fun OfficeImage.toParsedImage(): OfficeDocumentElement.ImageElement =
    OfficeDocumentElement.ImageElement(
        imagePath = imagePath,
        imageFile = imageFile,
        widthDp = widthDp,
        heightDp = heightDp,
        name = name
    )

private fun OfficeTable.toParsedTable(): OfficeDocumentElement.Table = OfficeDocumentElement.Table(
    rows = rows.map { row ->
        TableRow(
            cells = row.cells.map { cell ->
                TableCell(
                    text = cell.text,
                    paragraphs = cell.paragraphs.map { it.toParsedParagraph() },
                    startColumn = cell.startColumn,
                    columnSpan = cell.columnSpan,
                    rowSpan = cell.rowSpan,
                    occupancy = cell.occupancy,
                    repeatCount = cell.repeatCount,
                    styleName = cell.styleName,
                    boxStyle = cell.boxStyle,
                    sourceCellOrdinal = cell.sourceCellOrdinal
                )
            },
            styleName = row.styleName,
            isHeader = row.isHeader,
            repeatCount = row.repeatCount,
            rowStyle = row.rowStyle,
            sourceRowOrdinal = row.sourceRowOrdinal
        )
    },
    numColumns = numColumns,
    name = name,
    columns = columns,
    styleName = styleName,
    tableWidth = tableWidth,
    diagnostics = diagnostics
)

