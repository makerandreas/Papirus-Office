package com.makerandreas.papirusoffice.data
import java.util.Locale

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DocxParseResult(
    val text: String,
    val extractedImages: Map<String, File> = emptyMap(),
    val parsedDocument: OfficeParsedDocument? = null
)

class DocxDocumentParser(private val context: Context) {

    private val officeParser = OfficeDocumentParser(context)

    val parsingProgress: androidx.lifecycle.LiveData<ParsingProgress> get() = officeParser.parsingProgress

    suspend fun parseDocument(file: File, bypassCache: Boolean = false): DocxParseResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext DocxParseResult("")

        val fileName = file.name.lowercase(Locale.ROOT)
        if (fileName.endsWith(".docx") || fileName.endsWith(".docm") || fileName.endsWith(".odt") || fileName.endsWith(".ott") || fileName.endsWith(".ods") || fileName.endsWith(".ots") || fileName.endsWith(".odp") || fileName.endsWith(".otp") || fileName.endsWith(".xlsx") || fileName.endsWith(".xlsm") || fileName.endsWith(".pptx") || fileName.endsWith(".pptm") || isZipFile(file)) {
            val parsedDoc = officeParser.parseDocument(file, bypassCache)
            return@withContext DocxParseResult(
                text = parsedDoc.plainText,
                extractedImages = parsedDoc.extractedImages,
                parsedDocument = parsedDoc
            )
        } else {
            // Plain text fallback
            return@withContext try {
                DocxParseResult(file.readText())
            } catch (e: Exception) {
                DocxParseResult("Error reading file: ${e.message}")
            }
        }
    }

    private fun isZipFile(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val b1 = input.read()
                val b2 = input.read()
                b1 == 'P'.code && b2 == 'K'.code
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveDocument(file: File, document: com.makerandreas.papirusoffice.data.OfficeDocument): Boolean = withContext(Dispatchers.IO) {
        val saveFileName = file.name.lowercase(Locale.ROOT)
        if ((saveFileName.endsWith(".docx") || saveFileName.endsWith(".docm")) && document.containsEmbeddedImages()) {
            PapirusLogger.w(
                "DocxDocumentParser",
                "Refusing to save ${file.name}: the DOCX writer cannot preserve embedded images yet"
            )
            return@withContext false
        }

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
            isOdt = file.name.endsWith(".odt", ignoreCase = true),
            isDocx = file.name.endsWith(".docx", ignoreCase = true) || file.name.endsWith(".docm", ignoreCase = true),
            isOds = file.name.endsWith(".ods", ignoreCase = true),
            isXlsx = file.name.endsWith(".xlsx", ignoreCase = true) || file.name.endsWith(".xlsm", ignoreCase = true),
            isOdp = file.name.endsWith(".odp", ignoreCase = true),
            isPptx = file.name.endsWith(".pptx", ignoreCase = true) || file.name.endsWith(".pptm", ignoreCase = true),
            isParsingFailed = false
        )
        val fileName = file.name.lowercase(Locale.ROOT)
        if (parsedDoc.isXlsx) return@withContext officeParser.saveXlsxDocument(file, parsedDoc)
        if (parsedDoc.isOds) return@withContext officeParser.saveOdsDocument(file, parsedDoc)
        if (parsedDoc.isOdp) return@withContext officeParser.saveOdpDocument(file, parsedDoc)
        if (parsedDoc.isOdt) return@withContext officeParser.saveOdtDocument(file, parsedDoc)
        if (parsedDoc.isPptx) return@withContext officeParser.savePptxDocument(file, parsedDoc)

        return@withContext saveDocxZip(file, generateDocxXmlFromElements(document.body.elements))
    }

    suspend fun saveDocument(file: File, text: String): Boolean = withContext(Dispatchers.IO) {
        val fileName = file.name.lowercase(Locale.ROOT)
        val isXlsx = fileName.endsWith(".xlsx") || fileName.endsWith(".xlsm")
        val isOds = fileName.endsWith(".ods")
        val isOdp = fileName.endsWith(".odp")
        val isOdt = fileName.endsWith(".odt")
        val isPptx = fileName.endsWith(".pptx") || fileName.endsWith(".pptm")
        val isDocx = fileName.endsWith(".docx") || fileName.endsWith(".docm")
        
        if (isXlsx) {
            return@withContext officeParser.saveXlsxDocument(file, text)
        }
        if (isOds) {
            return@withContext officeParser.saveOdsDocument(file, text)
        }
        if (isOdp) {
            return@withContext officeParser.saveOdpDocument(file, text)
        }
        if (isOdt) {
            return@withContext officeParser.saveOdtDocument(file, text)
        }
        if (isPptx) {
            return@withContext officeParser.savePptxDocument(file, text)
        }

        if (!isDocx) {
            // Write raw text for plain text fallback
            return@withContext try {
                file.writeText(text)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        
        val tempFile = File(context.cacheDir, "temp_save_" + file.name)
        val success = try {
            val targetEntry = if (isDocx) "word/document.xml" else "content.xml"
            
            // Check if file is empty or doesn't exist yet (new document case)
            if (!file.exists() || file.length() == 0L) {
                // If it's a new document, create full standard ODT / DOCX ZIP container
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    if (isOdt) {
                        // 1. mimetype (must be uncompressed/stored or first entry in ODT)
                        val mimeEntry = java.util.zip.ZipEntry("mimetype")
                        mimeEntry.method = java.util.zip.ZipEntry.STORED
                        val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(Charsets.UTF_8)
                        mimeEntry.size = mimeBytes.size.toLong()
                        val crc = java.util.zip.CRC32()
                        crc.update(mimeBytes)
                        mimeEntry.crc = crc.value
                        zout.putNextEntry(mimeEntry)
                        zout.write(mimeBytes)
                        zout.closeEntry()

                        // 2. META-INF/manifest.xml
                        val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                        zout.putNextEntry(manifestEntry)
                        zout.write(generateOdtManifestXml())
                        zout.closeEntry()

                        // 3. styles.xml
                        val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                        zout.putNextEntry(stylesEntry)
                        zout.write(generateOdtStylesXml())
                        zout.closeEntry()

                        // 4. content.xml
                        val contentEntry = java.util.zip.ZipEntry("content.xml")
                        zout.putNextEntry(contentEntry)
                        zout.write(generateOdtXml(text))
                        zout.closeEntry()
                    } else {
                        // 1. [Content_Types].xml
                        val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                        zout.putNextEntry(ctEntry)
                        zout.write(generateDocxContentTypesXml())
                        zout.closeEntry()

                        // 2. _rels/.rels
                        val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                        zout.putNextEntry(relsEntry)
                        zout.write(generateDocxRelsXml())
                        zout.closeEntry()

                        // 3. word/_rels/document.xml.rels
                        val docRelsEntry = java.util.zip.ZipEntry("word/_rels/document.xml.rels")
                        zout.putNextEntry(docRelsEntry)
                        zout.write(generateDocxDocumentRelsXml())
                        zout.closeEntry()

                        // 4. word/document.xml
                        val newEntry = java.util.zip.ZipEntry(targetEntry)
                        zout.putNextEntry(newEntry)
                        zout.write(generateDocxXml(text))
                        zout.closeEntry()
                    }
                }
            } else {
                java.util.zip.ZipInputStream(file.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundTarget = false
                        var foundMime = false
                        var foundManifest = false
                        var foundStyles = false
                        var foundContentTypes = false
                        var foundRels = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "mimetype") foundMime = true
                            if (entryName == "META-INF/manifest.xml") foundManifest = true
                            if (entryName == "styles.xml") foundStyles = true
                            if (entryName == "[Content_Types].xml") foundContentTypes = true
                            if (entryName == "_rels/.rels") foundRels = true

                            if (entryName == "mimetype") {
                                foundMime = true
                                val mimeStr = if (isOds) "application/vnd.oasis.opendocument.spreadsheet"
                                              else if (isOdp) "application/vnd.oasis.opendocument.presentation"
                                              else "application/vnd.oasis.opendocument.text"
                                val mimeBytes = mimeStr.toByteArray(Charsets.UTF_8)
                                val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                                    method = java.util.zip.ZipEntry.STORED
                                    size = mimeBytes.size.toLong()
                                    crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                                }
                                zout.putNextEntry(mimeEntry)
                                zout.write(mimeBytes)
                            } else if (entryName == targetEntry) {
                                foundTarget = true
                                val updatedXmlBytes = if (isDocx) {
                                    generateDocxXml(text)
                                } else {
                                    generateOdtXml(text)
                                }
                                val newEntry = java.util.zip.ZipEntry(targetEntry)
                                zout.putNextEntry(newEntry)
                                zout.write(updatedXmlBytes)
                            } else {
                                val newEntry = java.util.zip.ZipEntry(entryName)
                                zout.putNextEntry(newEntry)
                                zin.copyTo(zout)
                            }
                            
                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }
                        
                        if (!foundTarget) {
                            val newEntry = java.util.zip.ZipEntry(targetEntry)
                            zout.putNextEntry(newEntry)
                            val updatedXmlBytes = if (isDocx) generateDocxXml(text) else generateOdtXml(text)
                            zout.write(updatedXmlBytes)
                            zout.closeEntry()
                        }

                        if (isOdt) {
                            if (!foundManifest) {
                                val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                                zout.putNextEntry(manifestEntry)
                                zout.write(generateOdtManifestXml())
                                zout.closeEntry()
                            }
                            if (!foundStyles) {
                                val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                                zout.putNextEntry(stylesEntry)
                                zout.write(generateOdtStylesXml())
                                zout.closeEntry()
                            }
                        } else if (isDocx) {
                            if (!foundContentTypes) {
                                val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                                zout.putNextEntry(ctEntry)
                                zout.write(generateDocxContentTypesXml())
                                zout.closeEntry()
                            }
                            if (!foundRels) {
                                val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                                zout.putNextEntry(relsEntry)
                                zout.write(generateDocxRelsXml())
                                zout.closeEntry()
                            }
                        }
                    }
                }
            }
            
            // Copy tempFile back to file
            tempFile.copyTo(file, overwrite = true)
            com.makerandreas.papirusoffice.data.cache.DocumentCacheRepository(context).saveCachedDocument(file, text)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
        return@withContext success
    }

    private suspend fun saveDocxZip(file: File, documentXml: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_save_" + file.name)
        val success = try {
            if (!file.exists() || file.length() == 0L) {
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    zout.putNextEntry(java.util.zip.ZipEntry("[Content_Types].xml"))
                    zout.write(generateDocxContentTypesXml())
                    zout.closeEntry()
                    zout.putNextEntry(java.util.zip.ZipEntry("_rels/.rels"))
                    zout.write(generateDocxRelsXml())
                    zout.closeEntry()
                    zout.putNextEntry(java.util.zip.ZipEntry("word/_rels/document.xml.rels"))
                    zout.write(generateDocxDocumentRelsXml())
                    zout.closeEntry()
                    zout.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
                    zout.write(documentXml)
                    zout.closeEntry()
                }
            } else {
                java.util.zip.ZipInputStream(file.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundTarget = false
                        while (entry != null) {
                            if (entry.name == "word/document.xml") {
                                foundTarget = true
                                zout.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
                                zout.write(documentXml)
                            } else {
                                zout.putNextEntry(java.util.zip.ZipEntry(entry.name))
                                zin.copyTo(zout)
                            }
                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }
                        if (!foundTarget) {
                            zout.putNextEntry(java.util.zip.ZipEntry("word/document.xml"))
                            zout.write(documentXml)
                            zout.closeEntry()
                        }
                    }
                }
            }
            tempFile.copyTo(file, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
        return@withContext success
    }

    private fun generateOdtManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
 <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.text"/>
 <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
 <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
</manifest:manifest>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateOdtStylesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" office:version="1.2">
  <office:styles/>
</office:document-styles>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateDocxContentTypesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateDocxRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateDocxDocumentRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateDocxXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
        sb.append("  <w:body>\n")
        
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            val escapedText = escapeXml(p)
            sb.append("    <w:p>\n")
            sb.append("      <w:r>\n")
            sb.append("        <w:t xml:space=\"preserve\">$escapedText</w:t>\n")
            sb.append("      </w:r>\n")
            sb.append("    </w:p>\n")
        }
        
        sb.append("    <w:sectPr>\n")
        sb.append("      <w:pgSz w:w=\"12240\" w:h=\"15840\"/>\n")
        sb.append("      <w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/>\n")
        sb.append("    </w:sectPr>\n")
        sb.append("  </w:body>\n")
        sb.append("</w:document>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    fun generateDocxXmlFromElements(elements: List<OfficeElement>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
        sb.append("  <w:body>\n")
        if (elements.isEmpty()) {
            sb.append("    <w:p><w:r><w:t/></w:r></w:p>\n")
        } else {
            for (element in elements) {
                writeDocxElement(sb, element)
            }
        }
        sb.append("    <w:sectPr>\n")
        sb.append("      <w:pgSz w:w=\"12240\" w:h=\"15840\"/>\n")
        sb.append("      <w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/>\n")
        sb.append("    </w:sectPr>\n")
        sb.append("  </w:body>\n")
        sb.append("</w:document>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun writeDocxElement(sb: StringBuilder, element: OfficeElement) {
        when (element) {
            is OfficeHeading -> {
                val level = (element.level - 1).coerceIn(0, 8)
                val style = element.styleName ?: "Heading${element.level}"
                sb.append("    <w:p>\n")
                sb.append("      <w:pPr><w:pStyle w:val=\"${escapeXml(style)}\"/><w:outlineLvl w:val=\"$level\"/></w:pPr>\n")
                sb.append("      <w:r><w:t xml:space=\"preserve\">${escapeXml(element.text)}</w:t></w:r>\n")
                sb.append("    </w:p>\n")
            }
            is OfficeParagraph -> {
                val style = element.styleName
                sb.append("    <w:p>\n")
                if (!style.isNullOrBlank()) {
                    sb.append("      <w:pPr><w:pStyle w:val=\"${escapeXml(style)}\"/>")
                    if (element.outlineLevel > 0) {
                        sb.append("<w:outlineLvl w:val=\"${(element.outlineLevel - 1).coerceIn(0, 8)}\"/>")
                    }
                    sb.append("</w:pPr>\n")
                }
                sb.append("      <w:r><w:t xml:space=\"preserve\">${escapeXml(element.text)}</w:t></w:r>\n")
                sb.append("    </w:p>\n")
            }
            is OfficeListItem -> {
                sb.append("    <w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr>")
                sb.append("<w:r><w:t xml:space=\"preserve\">${escapeXml(element.text)}</w:t></w:r></w:p>\n")
            }
            is OfficeTable -> {
                sb.append("    <w:tbl>\n")
                for (row in element.rows) {
                    sb.append("      <w:tr>\n")
                    for (cell in row.cells) {
                        sb.append("        <w:tc><w:p><w:r><w:t xml:space=\"preserve\">${escapeXml(cell.text)}</w:t></w:r></w:p></w:tc>\n")
                    }
                    sb.append("      </w:tr>\n")
                }
                sb.append("    </w:tbl>\n")
            }
            is OfficeImage, is OfficeDocElement.ImageElement -> {
                throw IllegalStateException("DOCX serialization does not support embedded images")
            }
            is OfficePageBreak -> {
                sb.append("    <w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
            }
            is OfficeDocElement.ParagraphElement -> writeDocxElement(sb, element.paragraph)
            is OfficeDocElement.TableElement -> writeDocxElement(sb, element.table)
            else -> { }
        }
    }

    private fun generateOdtXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("office:version=\"1.2\">\n")
        sb.append("  <office:body>\n")
        sb.append("    <office:text>\n")
        
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            val escapedText = escapeXml(p)
            sb.append("      <text:p>$escapedText</text:p>\n")
        }
        
        sb.append("    </office:text>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;")
    }
}
