package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.*
import com.makerandreas.papirusoffice.data.util.readCappedBytes
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Closed capability result for the current document state and structural writer. */
data class OdtSaveCapability(
    val isSupported: Boolean,
    val blockingFeatures: List<String> = emptyList()
)

class UnsupportedOdtStructureException(
    val features: List<String>
) : IllegalStateException(
    "Modified ODT serialization does not support: ${features.joinToString(", ")}"
)

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
            rejectUnsupportedStructuralRewrite(document)
            val useOriginalContentXml = !document.isModified &&
                (packageData?.entries?.containsKey("content.xml") == true || packageData?.originalContentXml != null)

            // A preserved original references its own Pictures/ entries. Verify
            // those entries are present before reusing the XML, otherwise fail
            // instead of writing a package with dangling image references.
            if (useOriginalContentXml) validatePreservedPackageImages(document)
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

    fun saveCapability(document: OfficeDocument): OdtSaveCapability {
        if (!document.isModified) return OdtSaveCapability(isSupported = true)
        val source = document.odtPackageData?.sourceFeatures ?: OdtSourceFeatures()
        val unsupported = buildList {
            if (source.hasAuthoredIndexes || document.authoredIndexes.isNotEmpty()) add("authored indexes")
            if (source.hasNamedSections || document.namedSectionRanges.isNotEmpty()) add("named sections")
            if (source.hasAdvancedTables || document.body.elements.any { it.hasUnsupportedTableSemantics() }) {
                add("source table structure")
            }
        }
        return OdtSaveCapability(
            isSupported = unsupported.isEmpty(),
            blockingFeatures = unsupported
        )
    }

    private fun rejectUnsupportedStructuralRewrite(document: OfficeDocument) {
        val capability = saveCapability(document)
        if (!capability.isSupported) {
            throw UnsupportedOdtStructureException(capability.blockingFeatures)
        }
    }

    @Suppress("DEPRECATION")
    private fun OfficeElement.hasUnsupportedTableSemantics(): Boolean {
        val table = when (this) {
            is OfficeTable -> this
            is OfficeDocElement.TableElement -> this.table
            is OfficeSection -> return elements.any { it.hasUnsupportedTableSemantics() }
            else -> return false
        }
        return table.columns.isNotEmpty() || table.styleName != null || table.rows.any { row ->
            row.styleName != null || row.isHeader || row.repeatCount != 1 || row.rowStyle != TableRowStyle() ||
                row.cells.any { cell ->
                    cell.startColumn != 0 || cell.columnSpan != 1 || cell.rowSpan != 1 ||
                        cell.occupancy != TableCellOccupancy.ORIGIN || cell.repeatCount != 1 ||
                        cell.styleName != null || cell.boxStyle != TableCellBoxStyle()
                }
        }
    }

    /**
     * The names the writer generates for itself, resolved against the names the
     * document already declares.
     *
     * Plan 9 decision D4, which the owner restated as a rule: an opened
     * document keeps the paragraph, character and other styles it arrived with,
     * and the writer may not override them with the defaults it uses to create
     * a new document. Every one of these used to be a hardcoded literal, so a
     * document declaring its own `P1` had that style silently replaced. All six
     * `tests/inky` fixtures declare `P1` and four declare `Table1`, so the
     * collision was the normal case rather than an edge case.
     */
    /** Every `style:name` in a style block, so the writer never reuses one. */
    private val STYLE_NAME_PATTERN = Regex("style:name=\"([^\"]+)\"")

    private data class GeneratedStyleNames(
        val paragraph: String,
        val table: String,
        val tableColumn: String,
        val bold: String,
        val italic: String,
        val boldItalic: String,
        val underline: String,
        val boldUnderline: String,
        val italicUnderline: String,
        val boldItalicUnderline: String
    )

    /**
     * Hands out style names the document is not already using. A name the
     * source declares belongs to the source and is never handed to the writer;
     * the writer's own name gets a `_papirus_N` suffix instead. A rename is
     * logged at debug because it is normal bookkeeping, not a defect.
     */
    private class StyleNameAllocator(taken: Collection<String>) {
        private val used = taken.toMutableSet()

        fun reserve(base: String): String {
            if (used.add(base)) return base
            var n = 1
            while (true) {
                val candidate = "${base}_papirus_$n"
                if (used.add(candidate)) {
                    PapirusLogger.d("ODT", "STYLE_NAME_TAKEN base=$base using=$candidate")
                    return candidate
                }
                n++
            }
        }
    }

    /**
     * Collects derived text styles while the body is walked. Keyed by the
     * formatting signature so two runs with the same formatting share one
     * style instead of emitting a near-duplicate for each run.
     */
    private inner class DerivedStyleSink(private val alloc: StyleNameAllocator) {
        private val byKey = HashMap<String, String>()
        val styles = LinkedHashMap<String, String>()

        fun styleFor(run: OfficeTextRun): String? {
            val key = directFormatKey(run) ?: return null
            byKey[key]?.let { return it }
            val name = alloc.reserve("T_papirus")
            byKey[key] = name
            styles[name] = derivedTextStyleXml(name, run)
            return name
        }
    }

    /** Everything the element walk needs in order to write style references. */
    private class WriteContext(
        val imageHrefs: Map<String, String>,
        val names: GeneratedStyleNames,
        val sink: DerivedStyleSink
    )

    private fun allocateGeneratedNames(alloc: StyleNameAllocator) = GeneratedStyleNames(
        paragraph = alloc.reserve("P1"),
        table = alloc.reserve("Table1"),
        tableColumn = alloc.reserve("Table1.Col"),
        bold = alloc.reserve("T_Bold"),
        italic = alloc.reserve("T_Italic"),
        boldItalic = alloc.reserve("T_BoldItalic"),
        underline = alloc.reserve("T_Underline"),
        boldUnderline = alloc.reserve("T_BoldUnderline"),
        italicUnderline = alloc.reserve("T_ItalicUnderline"),
        boldItalicUnderline = alloc.reserve("T_BoldItalicUnderline")
    )

    private fun buildContentXml(document: OfficeDocument, imageHrefs: Map<String, String> = emptyMap()): ByteArray {
        // Decision D4: the cheapest way to guarantee a document keeps the
        // styles it arrived with is to copy the source's own automatic-styles
        // block through verbatim, rather than re-derive it from the resolved
        // style model, which cannot round-trip properties it does not carry.
        val originalContentXml = readOriginalPart(document, "content.xml")
        val originalAutomaticStyles = extractAutomaticStyles(originalContentXml)
        val takenNames = collectStyleNames(originalAutomaticStyles) +
            collectStyleNames(readOriginalPart(document, "styles.xml"))
        val alloc = StyleNameAllocator(takenNames)
        val names = allocateGeneratedNames(alloc)

        // Derived styles are collected while the body is written and emitted
        // afterwards, because automatic-styles precedes the body in the XML.
        val sink = DerivedStyleSink(alloc)
        val ctx = WriteContext(imageHrefs, names, sink)
        val body = StringBuilder()
        writeElements(body, document.body.elements, ctx)

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" ")
        sb.append("xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:draw:1.0\" ")
        sb.append("xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" ")
        // Required by style:font-face's svg:font-family (section 2.1 item 5).
        sb.append("xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" ")
        sb.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
        // ODF 1.4 Part 3 section 19.390; was 1.2.
        sb.append("office:version=\"1.4\">\n")

        // font-face-decls precedes automatic-styles in the ODF content model.
        sb.append(buildFontFaceDecls(document))
        sb.append("  <office:automatic-styles>\n")
        if (!originalAutomaticStyles.isNullOrBlank()) {
            sb.append(originalAutomaticStyles).append('\n')
        }
        sb.append(automaticStylesXml(names))
        for ((_, styleXml) in sink.styles) sb.append(styleXml).append('\n')
        sb.append("  </office:automatic-styles>\n")

        sb.append("  <office:body>\n")
        sb.append("    <office:text>\n")
        sb.append(body)
        sb.append("    </office:text>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    /** The source's own package part, when the document was opened from one. */
    private fun readOriginalPart(document: OfficeDocument, name: String): String? {
        val data = document.odtPackageData ?: return null
        data.entries[name]?.let { return it.toString(Charsets.UTF_8) }
        return when (name) {
            "content.xml" -> data.originalContentXml
            "styles.xml" -> data.originalStylesXml
            else -> null
        }
    }

    /**
     * The inner XML of the source's `<office:automatic-styles>` block, verbatim.
     * Null when the source has none, which is the case for a document created
     * from `templates/styles/Default.ott` (audit-025 section 5.1 finding J).
     */
    private fun extractAutomaticStyles(contentXml: String?): String? {
        if (contentXml.isNullOrBlank()) return null
        val open = contentXml.indexOf("<office:automatic-styles")
        if (open < 0) return null
        val contentStart = contentXml.indexOf('>', open) + 1
        if (contentStart <= 0) return null
        val close = contentXml.indexOf("</office:automatic-styles>", contentStart)
        if (close < 0) return null
        return contentXml.substring(contentStart, close)
    }

    private fun collectStyleNames(xml: String?): Set<String> =
        if (xml.isNullOrBlank()) emptySet()
        else STYLE_NAME_PATTERN.findAll(xml).map { it.groupValues[1] }.toSet()

    private fun automaticStylesXml(names: GeneratedStyleNames): String = """    <style:style style:name="${names.paragraph}" style:family="paragraph" style:parent-style-name="Standard"/>
    <style:style style:name="${names.bold}" style:family="text"><style:text-properties fo:font-weight="bold" style:font-weight-asian="bold" style:font-weight-complex="bold"/></style:style>
    <style:style style:name="${names.italic}" style:family="text"><style:text-properties fo:font-style="italic" style:font-style-asian="italic" style:font-style-complex="italic"/></style:style>
    <style:style style:name="${names.boldItalic}" style:family="text"><style:text-properties fo:font-weight="bold" fo:font-style="italic" style:font-weight-asian="bold" style:font-style-asian="italic"/></style:style>
    <style:style style:name="${names.underline}" style:family="text"><style:text-properties style:text-underline-style="solid" style:text-underline-width="auto" style:text-underline-color="font-color"/></style:style>
    <style:style style:name="${names.boldUnderline}" style:family="text"><style:text-properties fo:font-weight="bold" style:text-underline-style="solid" style:text-underline-width="auto" style:text-underline-color="font-color"/></style:style>
    <style:style style:name="${names.italicUnderline}" style:family="text"><style:text-properties fo:font-style="italic" style:text-underline-style="solid" style:text-underline-width="auto" style:text-underline-color="font-color"/></style:style>
    <style:style style:name="${names.boldItalicUnderline}" style:family="text"><style:text-properties fo:font-weight="bold" fo:font-style="italic" style:text-underline-style="solid" style:text-underline-width="auto" style:text-underline-color="font-color"/></style:style>
    <style:style style:name="${names.table}" style:family="table"><style:table-properties style:width="100%" table:align="margins"/></style:style>
    <style:style style:name="${names.tableColumn}" style:family="table-column"><style:table-column-properties style:column-width="auto"/></style:style>
"""

    /**
     * Which style reference a run carries. The eight fixed combinations are
     * preferred because they keep the output close to what the document
     * already had; formatting they cannot express is derived instead of
     * dropped, which is what used to happen to a coloured run.
     */
    private fun effectiveRunStyle(run: OfficeTextRun, ctx: WriteContext): String? {
        val n = ctx.names
        val bold = run.isBold == true
        val italic = run.isItalic == true
        val underline = run.isUnderline == true
        val needsDerived = run.colorHex != null || run.highlight != null || run.isHidden == true
        if (needsDerived) return ctx.sink.styleFor(run)
        return when {
            bold && italic && underline -> n.boldItalicUnderline
            bold && italic -> n.boldItalic
            bold && underline -> n.boldUnderline
            italic && underline -> n.italicUnderline
            bold -> n.bold
            italic -> n.italic
            underline -> n.underline
            // A name the document declared is used as it is: decision D4 says
            // the document keeps its own names.
            !run.styleName.isNullOrEmpty() -> run.styleName
            !run.characterStyle.isNullOrEmpty() -> run.characterStyle
            else -> null
        }
    }

    private fun directFormatKey(run: OfficeTextRun): String? {
        val parts = ArrayList<String>(7)
        if (run.isBold == true) parts += "b"
        if (run.isItalic == true) parts += "i"
        if (run.isUnderline == true) parts += "u"
        run.colorHex?.takeIf { it.isNotBlank() }?.let { parts += "c$it" }
        run.highlight?.takeIf { it.isNotBlank() }?.let { parts += "h$it" }
        if (run.isHidden == true) parts += "v"
        return if (parts.isEmpty()) null else parts.joinToString("-")
    }

    private fun derivedTextStyleXml(name: String, run: OfficeTextRun): String {
        val props = ArrayList<String>(7)
        if (run.isBold == true) {
            props += "fo:font-weight=\"bold\" style:font-weight-asian=\"bold\" style:font-weight-complex=\"bold\""
        }
        if (run.isItalic == true) {
            props += "fo:font-style=\"italic\" style:font-style-asian=\"italic\" style:font-style-complex=\"italic\""
        }
        if (run.isUnderline == true) {
            props += "style:text-underline-style=\"solid\" style:text-underline-width=\"auto\" style:text-underline-color=\"font-color\""
        }
        run.colorHex?.takeIf { it.isNotBlank() }?.let { props += "fo:color=\"${escapeXml(it)}\"" }
        run.highlight?.takeIf { it.isNotBlank() }?.let { props += "fo:background-color=\"${escapeXml(it)}\"" }
        if (run.isHidden == true) props += "text:display=\"none\""
        return if (props.isEmpty()) {
            "    <style:style style:name=\"$name\" style:family=\"text\"/>"
        } else {
            "    <style:style style:name=\"$name\" style:family=\"text\"><style:text-properties ${props.joinToString(" ")}/></style:style>"
        }
    }

    /**
     * Writes the body, grouping consecutive list items into one list.
     *
     * Section 0.1's defect: `writeElement` opened and closed a `<text:list>`
     * around every single item, so a five-item list was written as five
     * one-item lists and the numbering restarted at 1 on each line.
     */
    /**
     * Declared font faces, so a saved document names the fonts it uses instead
     * of leaving the reader to substitute its own. Empty when the document
     * declares none, which keeps today's output byte-identical for documents
     * that never had any.
     */
    private fun buildFontFaceDecls(document: OfficeDocument): String {
        val faces = document.styles.fontFaces
        if (faces.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("  <office:font-face-decls>\n")
        for ((_, face) in faces) {
            val attrs = StringBuilder()
            attrs.append(" style:name=\"${escapeXml(face.name)}\"")
            attrs.append(" svg:font-family=\"${escapeXml(face.family)}\"")
            face.genericFamily?.takeIf { it.isNotBlank() }
                ?.let { attrs.append(" style:font-family-generic=\"${escapeXml(it)}\"") }
            face.pitch?.takeIf { it.isNotBlank() }
                ?.let { attrs.append(" style:font-pitch=\"${escapeXml(it)}\"") }
            face.charset?.takeIf { it.isNotBlank() }
                ?.let { attrs.append(" style:font-charset=\"${escapeXml(it)}\"") }
            sb.append("    <style:font-face$attrs/>\n")
        }
        sb.append("  </office:font-face-decls>\n")
        return sb.toString()
    }

    /** Distinct, non-blank bookmark names from the element's own list and field. */
    private fun bookmarkNames(list: List<OfficeBookmark>, single: String? = null): List<String> {
        val out = LinkedHashSet<String>()
        single?.takeIf { it.isNotBlank() }?.let { out += it }
        list.forEach { if (it.name.isNotBlank()) out += it.name }
        return out.toList()
    }

    private fun bookmarkStarts(names: List<String>): String =
        names.joinToString("") { "<text:bookmark-start text:name=\"${escapeXml(it)}\"/>" }

    private fun bookmarkEnds(names: List<String>): String =
        names.joinToString("") { "<text:bookmark-end text:name=\"${escapeXml(it)}\"/>" }

    private fun writeElements(sb: StringBuilder, elements: List<OfficeElement>, ctx: WriteContext) {
        var i = 0
        while (i < elements.size) {
            val element = elements[i]
            if (element is OfficeListItem) {
                var end = i + 1
                while (end < elements.size && elements[end] is OfficeListItem) end++
                @Suppress("UNCHECKED_CAST")
                val run = elements.subList(i, end) as List<OfficeListItem>
                writeListRun(sb, run, ctx)
                i = end
            } else {
                writeElement(sb, element, ctx)
                i++
            }
        }
    }

    /**
     * One list for the whole run. A deeper item nests inside the previous
     * item's `<text:list-item>`, which is deliberately left open until the
     * level comes back up; that is how ODF expresses an outline level.
     */
    private fun writeListRun(sb: StringBuilder, items: List<OfficeListItem>, ctx: WriteContext) {
        var openLists = 0
        var currentLevel = 0
        var previousLevel = 0
        for (item in items) {
            val level = if (item.level > 0) item.level else 1
            if (openLists == 0) {
                sb.append(listOpenTag(item))
                openLists = 1
                currentLevel = level
            } else if (level > currentLevel) {
                sb.append(listOpenTag(item))
                openLists++
                currentLevel = level
            } else {
                while (currentLevel > level && openLists > 0) {
                    sb.append("</text:list-item></text:list>\n")
                    openLists--
                    currentLevel--
                }
                if (previousLevel >= level) sb.append("</text:list-item>\n")
            }
            val marks = bookmarkNames(item.bookmarks)
            sb.append("<text:list-item><text:p>")
            sb.append(bookmarkStarts(marks))
            writeRunsOrText(sb, item.text, item.runs, ctx)
            sb.append(bookmarkEnds(marks))
            sb.append("</text:p>")
            previousLevel = level
        }
        while (openLists > 0) {
            sb.append("</text:list-item></text:list>\n")
            openLists--
        }
    }

    private fun listOpenTag(item: OfficeListItem): String {
        val styleAttr = item.styleName?.takeIf { it.isNotBlank() }
            ?.let { " text:list-style-name=\"${escapeXml(it)}\"" } ?: ""
        return "      <text:list$styleAttr>"
    }

    /** Columns a row really occupies, counting a spanned cell once. */
    private fun tableSlotCount(table: OfficeTable): Int =
        table.rows.maxOfOrNull { row ->
            row.cells.filter { it.occupancy == TableCellOccupancy.ORIGIN }.sumOf { it.columnSpan }
        }?.takeIf { it > 0 } ?: 1

    private fun writeElement(sb: StringBuilder, element: OfficeElement, ctx: WriteContext) {
        val imageHrefs = ctx.imageHrefs
        val names = ctx.names
        when (element) {
            is OfficeParagraph -> {
                val marks = bookmarkNames(element.bookmarks, element.bookmark)
                val styleName = if (!element.styleName.isNullOrEmpty()) element.styleName else names.paragraph
                sb.append("      <text:p text:style-name=\"${escapeXml(styleName)}\">")
                // bookmark-start/end sit inside the paragraph, which is where
                // the ODF content model allows them.
                sb.append(bookmarkStarts(marks))
                writeRunsOrText(sb, element.text, element.runs, ctx)
                sb.append(bookmarkEnds(marks))
                sb.append("</text:p>\n")
            }
            is OfficeHeading -> {
                val marks = bookmarkNames(element.bookmarks)
                val level = if (element.level > 0) element.level else 1
                val styleName = if (!element.styleName.isNullOrEmpty()) element.styleName else "Heading_$level"
                sb.append("      <text:h text:outline-level=\"$level\" text:style-name=\"${escapeXml(styleName)}\">")
                sb.append(bookmarkStarts(marks))
                writeRunsOrText(sb, element.text, element.runs, ctx)
                sb.append(bookmarkEnds(marks))
                sb.append("</text:h>\n")
            }
            is OfficeListItem -> {
                // Reached only outside a run; writeElements groups runs before
                // they get here, so this is the single-item case.
                writeListRun(sb, listOf(element), ctx)
            }
            is OfficeTable -> {
                val tableName = names.table
                val cols = if (element.numColumns > 0) element.numColumns else tableSlotCount(element)
                sb.append("      <table:table table:name=\"$tableName\" table:style-name=\"$tableName\">\n")
                sb.append("        <table:table-column table:style-name=\"${names.tableColumn}\" table:number-columns-repeated=\"$cols\"/>\n")
                for (row in element.rows) {
                    sb.append("        <table:table-row>\n")
                    for (cell in row.cells) {
                        // A covered slot belongs to a cell that already spans
                        // it. Writing it again would add a phantom cell and
                        // shift every column after it.
                        if (cell.occupancy != TableCellOccupancy.ORIGIN) continue
                        val cellAttrs = StringBuilder()
                        if (cell.columnSpan > 1) {
                            cellAttrs.append(" table:number-columns-spanned=\"${cell.columnSpan}\"")
                        }
                        if (cell.rowSpan > 1) {
                            cellAttrs.append(" table:number-rows-spanned=\"${cell.rowSpan}\"")
                        }
                        // The cell style is the document's own name, so
                        // decision D4 says it is written as it arrived.
                        cell.styleName?.takeIf { it.isNotBlank() }?.let {
                            cellAttrs.append(" table:style-name=\"${escapeXml(it)}\"")
                        }
                        sb.append("          <table:table-cell office:value-type=\"string\"$cellAttrs>\n")
                        if (cell.paragraphs.isNotEmpty()) {
                            for (p in cell.paragraphs) {
                                sb.append("            <text:p>")
                                writeRunsOrText(sb, p.text, p.runs, ctx)
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
                    val frameName = element.name?.takeIf { it.isNotBlank() } ?: href.substringAfterLast("/")
                    sb.append("      <draw:frame draw:name=\"${escapeXml(frameName)}\" text:anchor-type=\"paragraph\">\n")
                    sb.append("        <draw:image xlink:href=\"${escapeXml(href)}\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>\n")
                    sb.append("      </draw:frame>\n")
                }
            }
            is OfficeSection -> {
                // Sections used to fall through to the empty else branch and
                // disappear from the saved document entirely (section 0.1).
                sb.append("      <text:section text:name=\"${escapeXml(element.name)}\">\n")
                writeElements(sb, element.elements, ctx)
                sb.append("      </text:section>\n")
            }
            is OfficeHyperlink -> {
                sb.append("      <text:p text:style-name=\"${escapeXml(names.paragraph)}">")
                sb.append("<text:a xlink:href=\"${escapeXml(element.targetUri)}\" xlink:type=\"simple\">")
                sb.append(escapeXml(element.text))
                sb.append("</text:a></text:p>\n")
            }
            is OfficeBookmark -> {
                sb.append("      <text:p text:style-name=\"${escapeXml(names.paragraph)}">")
                sb.append("<text:bookmark text:name=\"${escapeXml(element.name)}\"/>")
                sb.append("</text:p>\n")
            }
            is OfficeDocElement.ParagraphElement -> {
                writeElement(sb, element.paragraph, ctx)
            }
            is OfficeDocElement.TableElement -> {
                writeElement(sb, element.table, ctx)
            }
            is OfficeDocElement.ImageElement -> {
                writeElement(sb, element.image, ctx)
            }
            else -> {
                // Ignore other non-printable element types safely
            }
        }
    }

    private fun writeRunsOrText(sb: StringBuilder, plainText: String, runs: List<OfficeTextRun>, ctx: WriteContext) {
        if (runs.isNotEmpty()) {
            for (run in runs) {
                // Run flags are tri-state: only a stated property writes a span
                // style, so a run that says nothing keeps the paragraph's style.
                val styleName = effectiveRunStyle(run, ctx)
                // A run that carries a target is wrapped in text:a; the span
                // goes inside it, which is the order ODF requires.
                val link = run.hyperlink?.takeIf { it.isNotBlank() }
                if (link != null) {
                    sb.append("<text:a xlink:href=\"${escapeXml(link)}\" xlink:type=\"simple\">")
                }
                if (styleName != null) {
                    sb.append("<text:span text:style-name=\"${escapeXml(styleName)}\">")
                    sb.append(escapeXml(run.text))
                    sb.append("</text:span>")
                } else {
                    sb.append(escapeXml(run.text))
                }
                if (link != null) sb.append("</text:a>")
            }
        } else {
            sb.append(escapeXml(plainText))
        }
    }

    private fun buildStylesXml(document: OfficeDocument): ByteArray {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.4">
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
            <office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.4">
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
            <office:document-settings xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" office:version="1.4">
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

    private fun validatePreservedPackageImages(document: OfficeDocument) {
        val entries = document.odtPackageData?.entries.orEmpty()
        if (document.resources.images.isNotEmpty()) {
            throw IllegalStateException("ODT package contains detached image resources that cannot be verified")
        }
        fun images(elements: List<OfficeElement>): List<OfficeImage> = elements.flatMap { element ->
            when (element) {
                is OfficeImage -> listOf(element)
                is OfficeDocElement.ImageElement -> listOf(element.image)
                is OfficeSection -> images(element.elements)
                else -> emptyList()
            }
        }
        for (image in images(document.body.elements)) {
            val normalizedPath = image.imagePath.removePrefix("./")
            val payload = entries[image.imagePath] ?: entries[normalizedPath]
                ?: throw IllegalStateException("Original ODT package is missing image media: ${image.imagePath}")
            if (payload.isEmpty() || payload.size.toLong() > com.makerandreas.papirusoffice.data.util.ZipSafe.MAX_IMAGE_BYTES) {
                throw IllegalStateException("Original ODT image media is empty or oversized: ${image.imagePath}")
            }
        }
    }

    /**
     * Resolves top-level [OfficeImage] elements to embeddable package entries.
     * Returns pairs of (original imagePath -> embed). Missing, unreadable, or
     * oversized payloads fail the save instead of silently dropping the image.
     */
    private fun collectImages(document: OfficeDocument): List<Pair<String, ImageEmbed>> {
        if (document.resources.images.isNotEmpty()) {
            throw IllegalStateException("ODT writer cannot serialize detached image resources")
        }

        fun containsImage(elements: List<OfficeElement>): Boolean = elements.any { element ->
            when (element) {
                is OfficeImage,
                is OfficeDocElement.ImageElement -> true
                is OfficeSection -> containsImage(element.elements)
                else -> false
            }
        }
        fun hasNestedImage(elements: List<OfficeElement>): Boolean = elements.any { element ->
            element is OfficeSection && containsImage(element.elements)
        }
        if (hasNestedImage(document.body.elements)) {
            throw IllegalStateException("ODT writer cannot safely serialize images nested in sections")
        }

        val result = mutableListOf<Pair<String, ImageEmbed>>()
        val packageEntries = document.odtPackageData?.entries.orEmpty()
        var counter = 0
        fun unwrap(element: OfficeElement): OfficeImage? = when (element) {
            is OfficeImage -> element
            is OfficeDocElement.ImageElement -> element.image
            else -> null
        }
        for (element in document.body.elements) {
            val image = unwrap(element) ?: continue
            if (result.any { it.first == image.imagePath }) continue
            val source = image.imageFile?.takeIf { it.isFile && it.canRead() }
                ?: java.io.File(image.imagePath).takeIf { it.isFile && it.canRead() }
            val originalEntry = packageEntries[image.imagePath]
            val bytes = when {
                source != null -> {
                    if (source.length() > com.makerandreas.papirusoffice.data.util.ZipSafe.MAX_IMAGE_BYTES) {
                        throw IllegalStateException("Embedded image exceeds the ODT image size limit: ${source.name}")
                    }
                    source.inputStream().use {
                        it.readCappedBytes(com.makerandreas.papirusoffice.data.util.ZipSafe.MAX_IMAGE_BYTES)
                    }
                }
                originalEntry != null -> originalEntry
                else -> throw IllegalStateException("Cannot safely save missing embedded image: ${image.imagePath}")
            }
            if (bytes.isEmpty() || bytes.size.toLong() > com.makerandreas.papirusoffice.data.util.ZipSafe.MAX_IMAGE_BYTES) {
                throw IllegalStateException("Embedded image is empty or exceeds the ODT image size limit: ${image.imagePath}")
            }

            val rawExt = (source?.extension ?: image.imagePath.substringAfterLast('.', "png"))
                .lowercase()
                .replace("[^a-z0-9]".toRegex(), "")
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
            val href = if (source == null) {
                val entryName = image.imagePath
                val safePath = entryName.startsWith("Pictures/") &&
                    entryName.split('/').none { it == ".." || it.isBlank() } &&
                    !entryName.contains('\\')
                if (!safePath || originalEntry == null) {
                    throw IllegalStateException("Cannot safely reuse embedded image entry: $entryName")
                }
                entryName
            } else {
                var candidate: String
                do {
                    counter++
                    candidate = "Pictures/papirus-image$counter.$ext"
                } while (candidate in packageEntries || result.any { it.second.href == candidate })
                candidate
            }
            result.add(image.imagePath to ImageEmbed(href, mime, bytes))
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
