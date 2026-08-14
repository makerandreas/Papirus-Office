package com.makerandreas.papirusoffice.data

import android.content.Context
import com.makerandreas.papirusoffice.data.writer.OdtDocumentParser
import com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter
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
    private val parser = OdtDocumentParser()
    private val writer = OdtDocumentWriter()

    override suspend fun read(source: DocumentReference, context: Context): OfficeDocument = withContext(Dispatchers.IO) {
        val bytes = when (source) {
            is DocumentReference.LocalFile -> {
                if (source.file.exists() && source.file.length() > 0) source.file.readBytes() else ByteArray(0)
            }
            is DocumentReference.SafUri -> {
                context.contentResolver.openInputStream(source.uri)?.use { it.readBytes() } ?: ByteArray(0)
            }
            else -> ByteArray(0)
        }
        if (bytes.isEmpty()) return@withContext OfficeDocument()
        return@withContext parser.parse(bytes)
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
                    context.contentResolver.openOutputStream(destination.uri)?.use { stream ->
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

        val elements = mutableListOf<OfficeElement>()
        val paragraphs = parseResult.text.split("\n\n").filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) {
            elements.add(OfficeParagraph(""))
        } else {
            paragraphs.forEach { pText ->
                elements.add(OfficeParagraph(pText))
            }
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
            val plainText = document.toPlainText()

            when (destination) {
                is DocumentReference.LocalFile -> {
                    docxParser.saveDocument(destination.file, plainText)
                }
                is DocumentReference.SafUri -> {
                    val tempFile = File.createTempFile("temp_write_docx", ".docx", context.cacheDir)
                    docxParser.saveDocument(tempFile, plainText)
                    context.contentResolver.openOutputStream(destination.uri)?.use { stream ->
                        tempFile.inputStream().use { input -> input.copyTo(stream) }
                    }
                    tempFile.delete()
                    true
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

private fun OfficeDocument.toOfficeParsedDocument(format: String): OfficeParsedDocument {
    val parsedElements = body.elements.mapNotNull { element ->
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
        isParsingFailed = false
    )
}

