package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.*
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OdtDocumentWriter : DocumentFormatWriter {

    /** An image resolved to embeddable package bytes. */
    private data class ImageEmbed(
        val href: String,
        val mime: String,
        val bytes: ByteArray
    )

    override fun write(document: OfficeDocument): ByteArray {
        PapirusLogger.d("ODT", "WRITE_START")
        val elements = document.body.elements
        PapirusLogger.d("ODT", "BODY_ELEMENTS=${elements.size}")

        try {
            val packageData = document.odtPackageData
            val useOriginalContentXml = !document.isModified &&
                (packageData?.entries?.containsKey("content.xml") == true || packageData?.originalContentXml != null)

            // Resolve embedded images only when regenerating content.xml; a
            // preserved original already references its own Pictures/ entries.
            val embeds = if (useOriginalContentXml) emptyList() else collectImages(document)
            val imageHrefs = embeds.associate { it.first to it.second.href }

            val contentXmlBytes = if (useOriginalContentXml) {
                PapirusLogger.d("ODT", "CONTENT_XML_PRESERVED_EXACT_ORIGINAL")
                packageData?.entries?.get("content.xml")
                    ?: packageData?.originalContentXml?.toByteArray(Charsets.UTF_8)
                    ?: buildContentXml(document, imageHrefs)
            } else {
                PapirusLogger.d("ODT", "CONTENT_XML_GENERATED_STRUCTURED")
                buildContentXml(document, imageHrefs)
            }
            PapirusLogger.d("ODT", "CONTENT_XML_READY")
            val baos = ByteArrayOutputStream()

            ZipOutputStream(baos).use { zout ->
                // 1. mimetype (MUST be first entry, uncompressed STORED, zero extra field per ODF specification Part 2 / Part 3)
                val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(Charsets.UTF_8)
                val mimeEntry = ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = mimeBytes.size.toLong()
                    compressedSize = mimeBytes.size.toLong()
                    crc = CRC32().apply { update(mimeBytes) }.value
                    extra = ByteArray(0)
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
                    // Best effort: newly added images are embedded even though the
                    // preserved manifest cannot be merged safely here.
                    for ((_, embed) in embeds) {
                        if (packageData.entries.containsKey(embed.href)) continue
                        zout.putNextEntry(ZipEntry(embed.href))
                        zout.write(embed.bytes)
                        zout.closeEntry()
                    }
                } else {
                    PapirusLogger.d("ODT", "FRESH_GENERATION_MODE")
                    // Fresh package generation mode: generate standard ODF structure
                    val manifestXmlBytes = buildManifestXml(embeds.map { it.second })
                    zout.putNextEntry(ZipEntry("META-INF/manifest.xml"))
                    zout.write(manifestXmlBytes)
                    zout.closeEntry()

                    for ((_, embed) in embeds) {
                        zout.putNextEntry(ZipEntry(embed.href))
                        zout.write(embed.bytes)
                        zout.closeEntry()
                    }

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

    private fun buildContentXml(document: OfficeDocument, imageHrefs: Map<String, String> = emptyMap()): ByteArray {
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
            writeElement(sb, element, imageHrefs)
        }

        sb.append("    </office:text>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun writeElement(sb: StringBuilder, element: OfficeElement, imageHrefs: Map<String, String> = emptyMap()) {
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
                val href = imageHrefs[element.imagePath]
                if (href == null) {
                    // Unresolvable image (missing file): skip instead of writing
                    // a dangling absolute device path into the package.
                    PapirusLogger.w("ODT", "Skipping unresolvable image: ${element.imagePath}")
                } else {
                    val frameName = href.substringAfterLast("/")
                    sb.append("      <draw:frame draw:name=\"${escapeXml(frameName)}\" text:anchor-type=\"paragraph\">\n")
                    sb.append("        <draw:image xlink:href=\"${escapeXml(href)}\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>\n")
                    sb.append("      </draw:frame>\n")
                }
            }
            is OfficeDocElement.ParagraphElement -> {
                writeElement(sb, element.paragraph, imageHrefs)
            }
            is OfficeDocElement.TableElement -> {
                writeElement(sb, element.table, imageHrefs)
            }
            is OfficeDocElement.ImageElement -> {
                writeElement(sb, element.image, imageHrefs)
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

    private fun buildManifestXml(embeds: List<ImageEmbed> = emptyList()): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\" manifest:version=\"1.2\">\n")
        sb.append("  <manifest:file-entry manifest:full-path=\"/\" manifest:version=\"1.2\" manifest:media-type=\"application/vnd.oasis.opendocument.text\"/>\n")
        sb.append("  <manifest:file-entry manifest:full-path=\"content.xml\" manifest:media-type=\"text/xml\"/>\n")
        sb.append("  <manifest:file-entry manifest:full-path=\"styles.xml\" manifest:media-type=\"text/xml\"/>\n")
        sb.append("  <manifest:file-entry manifest:full-path=\"meta.xml\" manifest:media-type=\"text/xml\"/>\n")
        sb.append("  <manifest:file-entry manifest:full-path=\"settings.xml\" manifest:media-type=\"text/xml\"/>\n")
        for (embed in embeds) {
            sb.append("  <manifest:file-entry manifest:full-path=\"${escapeXml(embed.href)}\" manifest:media-type=\"${escapeXml(embed.mime)}\"/>\n")
        }
        sb.append("</manifest:manifest>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * Resolves top-level [OfficeImage] elements to embeddable package entries.
     * Returns pairs of (original imagePath -> embed). Images whose files are
     * missing or oversized are skipped (logged) rather than embedded.
     */
    private fun collectImages(document: OfficeDocument): List<Pair<String, ImageEmbed>> {
        val result = mutableListOf<Pair<String, ImageEmbed>>()
        var counter = 0
        fun unwrap(element: OfficeElement): OfficeImage? = when (element) {
            is OfficeImage -> element
            is OfficeDocElement.ImageElement -> element.image
            else -> null
        }
        for (element in document.body.elements) {
            val image = unwrap(element) ?: continue
            if (result.any { it.first == image.imagePath }) continue
            try {
                val source = image.imageFile?.takeIf { it.isFile }
                    ?: java.io.File(image.imagePath).takeIf { it.isFile }
                    ?: continue
                if (source.length() > com.makerandreas.papirusoffice.data.util.ZipSafe.MAX_IMAGE_BYTES) {
                    PapirusLogger.w("ODT", "Skipping oversized image: ${source.name}")
                    continue
                }
                val rawExt = source.extension.lowercase().replace("[^a-z0-9]".toRegex(), "")
                val ext = rawExt.ifEmpty { "png" }
                val mime = when (ext) {
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "gif" -> "image/gif"
                    "bmp" -> "image/bmp"
                    "webp" -> "image/webp"
                    "svg" -> "image/svg+xml"
                    else -> "image/$ext"
                }
                counter++
                val href = "Pictures/image$counter.$ext"
                result.add(image.imagePath to ImageEmbed(href, mime, source.readBytes()))
            } catch (e: Exception) {
                PapirusLogger.w("ODT", "Skipping unreadable image ${image.imagePath}: ${e.message}")
            }
        }
        return result
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
