package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.*
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OdtDocumentWriter : DocumentFormatWriter {

    override fun write(document: OfficeDocument): ByteArray {
        PapirusLogger.d("ODT", "WRITE_START")
        val elements = document.body.elements
        PapirusLogger.d("ODT", "BODY_ELEMENTS=${elements.size}")

        try {
            val contentXmlBytes = buildContentXml(document)
            PapirusLogger.d("ODT", "CONTENT_XML_BUILT")

            val packageData = document.odtPackageData
            val baos = ByteArrayOutputStream()

            ZipOutputStream(baos).use { zout ->
                // 1. mimetype (MUST be first entry and uncompressed STORED per ODF specification Part 3)
                val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(Charsets.UTF_8)
                val mimeEntry = ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = mimeBytes.size.toLong()
                    crc = CRC32().apply { update(mimeBytes) }.value
                }
                zout.putNextEntry(mimeEntry)
                zout.write(mimeBytes)
                zout.closeEntry()

                // 2. Write updated content.xml
                zout.putNextEntry(ZipEntry("content.xml"))
                zout.write(contentXmlBytes)
                zout.closeEntry()

                if (packageData != null && packageData.entries.isNotEmpty()) {
                    PapirusLogger.d("ODT", "PACKAGE_PRESERVATION_MODE entries=${packageData.entries.size}")
                    // Package-preserving mode: preserve all original package entries (styles, manifest, settings, meta, pictures, thumbnails, etc.)
                    for ((entryName, entryBytes) in packageData.entries) {
                        if (entryName == "mimetype" || entryName == "content.xml") {
                            // Already handled
                            continue
                        }
                        zout.putNextEntry(ZipEntry(entryName))
                        zout.write(entryBytes)
                        zout.closeEntry()
                    }
                } else {
                    PapirusLogger.d("ODT", "FRESH_GENERATION_MODE")
                    // Fresh package generation mode: generate standard ODF structure
                    val manifestXmlBytes = buildManifestXml()
                    zout.putNextEntry(ZipEntry("META-INF/manifest.xml"))
                    zout.write(manifestXmlBytes)
                    zout.closeEntry()

                    val stylesXmlBytes = buildStylesXml(document)
                    zout.putNextEntry(ZipEntry("styles.xml"))
                    zout.write(stylesXmlBytes)
                    zout.closeEntry()

                    val metaXmlBytes = buildMetaXml(document)
                    zout.putNextEntry(ZipEntry("meta.xml"))
                    zout.write(metaXmlBytes)
                    zout.closeEntry()

                    val settingsXmlBytes = buildSettingsXml()
                    zout.putNextEntry(ZipEntry("settings.xml"))
                    zout.write(settingsXmlBytes)
                    zout.closeEntry()
                }
            }

            val packageBytes = baos.toByteArray()
            PapirusLogger.d("ODT", "PACKAGE_BUILT")
            PapirusLogger.d("ODT", "WRITE_BYTES=${packageBytes.size}")
            PapirusLogger.d("ODT", "WRITE_SUCCESS")
            return packageBytes
        } catch (e: Exception) {
            PapirusLogger.e("ODT", "WRITE_FAILED reason=${e.message}", e)
            throw e
        }
    }

    private fun buildContentXml(document: OfficeDocument): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" ")
        sb.append("xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:draw:1.0\" ")
        sb.append("xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" ")
        sb.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
        sb.append("office:version=\"1.2\">\n")

        sb.append("  <office:automatic-styles>\n")
        sb.append("    <style:style style:name=\"P1\" style:family=\"paragraph\" style:parent-style-name=\"Standard\"/>\n")
        sb.append("    <style:style style:name=\"T_Bold\" style:family=\"text\"><style:text-properties fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" style:font-weight-complex=\"bold\"/></style:style>\n")
        sb.append("    <style:style style:name=\"T_Italic\" style:family=\"text\"><style:text-properties fo:font-style=\"italic\" style:font-style-asian=\"italic\" style:font-style-complex=\"italic\"/></style:style>\n")
        sb.append("    <style:style style:name=\"T_BoldItalic\" style:family=\"text\"><style:text-properties fo:font-weight=\"bold\" fo:font-style=\"italic\" style:font-weight-asian=\"bold\" style:font-style-asian=\"italic\"/></style:style>\n")
        sb.append("    <style:style style:name=\"T_Underline\" style:family=\"text\"><style:text-properties style:text-underline-style=\"solid\" style:text-underline-width=\"auto\" style:text-underline-color=\"font-color\"/></style:style>\n")
        sb.append("    <style:style style:name=\"T_BoldUnderline\" style:family=\"text\"><style:text-properties fo:font-weight=\"bold\" style:text-underline-style=\"solid\" style:text-underline-width=\"auto\" style:text-underline-color=\"font-color\"/></style:style>\n")
        sb.append("    <style:style style:name=\"T_ItalicUnderline\" style:family=\"text\"><style:text-properties fo:font-style=\"italic\" style:text-underline-style=\"solid\" style:text-underline-width=\"auto\" style:text-underline-color=\"font-color\"/></style:style>\n")
        sb.append("    <style:style style:name=\"T_BoldItalicUnderline\" style:family=\"text\"><style:text-properties fo:font-weight=\"bold\" fo:font-style=\"italic\" style:text-underline-style=\"solid\" style:text-underline-width=\"auto\" style:text-underline-color=\"font-color\"/></style:style>\n")
        sb.append("    <style:style style:name=\"Table1\" style:family=\"table\"><style:table-properties style:width=\"100%\" table:align=\"margins\"/></style:style>\n")
        sb.append("    <style:style style:name=\"Table1.Col\" style:family=\"table-column\"><style:table-column-properties style:column-width=\"auto\"/></style:style>\n")
        sb.append("  </office:automatic-styles>\n")

        sb.append("  <office:body>\n")
        sb.append("    <office:text>\n")

        val elements = document.body.elements
        for (element in elements) {
            writeElement(sb, element)
        }

        sb.append("    </office:text>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun writeElement(sb: StringBuilder, element: OfficeElement) {
        when (element) {
            is OfficeParagraph -> {
                val styleAttr = if (!element.styleName.isNullOrEmpty()) " text:style-name=\"${escapeXml(element.styleName)}\"" else " text:style-name=\"P1\""
                sb.append("      <text:p$styleAttr>")
                writeRunsOrText(sb, element.text, element.runs)
                sb.append("</text:p>\n")
            }
            is OfficeHeading -> {
                val level = if (element.level > 0) element.level else 1
                val styleName = if (!element.styleName.isNullOrEmpty()) element.styleName else "Heading_$level"
                sb.append("      <text:h text:outline-level=\"$level\" text:style-name=\"${escapeXml(styleName)}\">")
                writeRunsOrText(sb, element.text, element.runs)
                sb.append("</text:h>\n")
            }
            is OfficeListItem -> {
                sb.append("      <text:list><text:list-item><text:p>")
                writeRunsOrText(sb, element.text, element.runs)
                sb.append("</text:p></text:list-item></text:list>\n")
            }
            is OfficeTable -> {
                val tableName = "Table1"
                val cols = if (element.numColumns > 0) element.numColumns else (element.rows.maxOfOrNull { it.cells.size } ?: 1)
                sb.append("      <table:table table:name=\"$tableName\" table:style-name=\"$tableName\">\n")
                sb.append("        <table:table-column table:style-name=\"Table1.Col\" table:number-columns-repeated=\"$cols\"/>\n")
                for (row in element.rows) {
                    sb.append("        <table:table-row>\n")
                    for (cell in row.cells) {
                        sb.append("          <table:table-cell office:value-type=\"string\">\n")
                        if (cell.paragraphs.isNotEmpty()) {
                            for (p in cell.paragraphs) {
                                sb.append("            <text:p>")
                                writeRunsOrText(sb, p.text, p.runs)
                                sb.append("</text:p>\n")
                            }
                        } else {
                            sb.append("            <text:p>${escapeXml(cell.text)}</text:p>\n")
                        }
                        sb.append("          </table:table-cell>\n")
                    }
                    sb.append("        </table:table-row>\n")
                }
                sb.append("      </table:table>\n")
            }
            is OfficeImage -> {
                sb.append("      <draw:frame draw:name=\"Image\">\n")
                sb.append("        <draw:image xlink:href=\"${escapeXml(element.imagePath)}\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>\n")
                sb.append("      </draw:frame>\n")
            }
            is OfficeDocElement.ParagraphElement -> {
                writeElement(sb, element.paragraph)
            }
            is OfficeDocElement.TableElement -> {
                writeElement(sb, element.table)
            }
            is OfficeDocElement.ImageElement -> {
                writeElement(sb, element.image)
            }
            else -> {
                // Ignore other non-printable element types safely
            }
        }
    }

    private fun writeRunsOrText(sb: StringBuilder, plainText: String, runs: List<OfficeTextRun>) {
        if (runs.isNotEmpty()) {
            for (run in runs) {
                val effectiveStyle = when {
                    run.isBold && run.isItalic && run.isUnderline -> "T_BoldItalicUnderline"
                    run.isBold && run.isItalic -> "T_BoldItalic"
                    run.isBold && run.isUnderline -> "T_BoldUnderline"
                    run.isItalic && run.isUnderline -> "T_ItalicUnderline"
                    run.isBold -> "T_Bold"
                    run.isItalic -> "T_Italic"
                    run.isUnderline -> "T_Underline"
                    !run.styleName.isNullOrEmpty() -> run.styleName
                    !run.characterStyle.isNullOrEmpty() -> run.characterStyle
                    else -> null
                }
                if (effectiveStyle != null) {
                    sb.append("<text:span text:style-name=\"${escapeXml(effectiveStyle)}\">")
                    sb.append(escapeXml(run.text))
                    sb.append("</text:span>")
                } else {
                    sb.append(escapeXml(run.text))
                }
            }
        } else {
            sb.append(escapeXml(plainText))
        }
    }

    private fun buildStylesXml(document: OfficeDocument): ByteArray {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2">
              <office:styles>
                <style:default-style style:family="paragraph">
                  <style:paragraph-properties/>
                  <style:text-properties fo:font-size="12pt" fo:font-family="Liberation Serif, Times New Roman"/>
                </style:default-style>
                <style:style style:name="Standard" style:family="paragraph" style:class="text"/>
                <style:style style:name="Body_20_Text" style:display-name="Body Text" style:family="paragraph" style:parent-style-name="Standard"/>
                <style:style style:name="Heading_1" style:display-name="Heading 1" style:family="paragraph" style:parent-style-name="Standard">
                  <style:text-properties fo:font-size="18pt" fo:font-weight="bold" style:font-size-asian="18pt" style:font-weight-asian="bold"/>
                </style:style>
                <style:style style:name="Heading_2" style:display-name="Heading 2" style:family="paragraph" style:parent-style-name="Standard">
                  <style:text-properties fo:font-size="14pt" fo:font-weight="bold" style:font-size-asian="14pt" style:font-weight-asian="bold"/>
                </style:style>
                <style:style style:name="Heading_3" style:display-name="Heading 3" style:family="paragraph" style:parent-style-name="Standard">
                  <style:text-properties fo:font-size="12pt" fo:font-weight="bold" style:font-size-asian="12pt" style:font-weight-asian="bold"/>
                </style:style>
              </office:styles>
            </office:document-styles>
        """.trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun buildMetaXml(document: OfficeDocument): ByteArray {
        val title = escapeXml(document.metadata.title)
        val author = escapeXml(document.metadata.author)
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2">
              <office:meta>
                <dc:title>$title</dc:title>
                <dc:creator>$author</dc:creator>
              </office:meta>
            </office:document-meta>
        """.trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun buildSettingsXml(): ByteArray {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-settings xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" office:version="1.2">
              <office:settings/>
            </office:document-settings>
        """.trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun buildManifestXml(): ByteArray {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
              <manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.text"/>
              <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="settings.xml" manifest:media-type="text/xml"/>
            </manifest:manifest>
        """.trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun escapeXml(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
