package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.DocumentMetadata
import com.makerandreas.papirusoffice.data.OdtPackageData
import com.makerandreas.papirusoffice.data.OdtSourceFeatures
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.util.ZipSafe
import com.makerandreas.papirusoffice.data.util.ZipScanBudget
import com.makerandreas.papirusoffice.data.util.nextEntryBudgeted
import com.makerandreas.papirusoffice.data.util.readCappedBytes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/** One package reader and semantic import path for every ODT entry point. */
class OdtImportPipeline(
    private val diagnostics: OdfImportDiagnostics = SilentOdfImportDiagnostics
) {
    fun parse(
        bytes: ByteArray,
        fileName: String = "document.odt",
        extractedImages: Map<String, File> = emptyMap(),
        onStylesParsed: (() -> Unit)? = null
    ): OfficeParsedDocument = ByteArrayInputStream(bytes).use { input ->
        parse(input, fileName, extractedImages, onStylesParsed)
    }

    fun parse(
        file: File,
        extractedImages: Map<String, File> = emptyMap(),
        onStylesParsed: (() -> Unit)? = null
    ): OfficeParsedDocument = file.inputStream().use { input ->
        parse(input, file.name, extractedImages, onStylesParsed)
    }

    fun parse(
        input: InputStream,
        fileName: String,
        extractedImages: Map<String, File> = emptyMap(),
        onStylesParsed: (() -> Unit)? = null
    ): OfficeParsedDocument {
        val entries = try {
            readEntries(input)
        } catch (error: Exception) {
            val message = "ODT package read failed: ${error.localizedMessage ?: "invalid package"}"
            diagnostics.malformedXml(fileName, message, error)
            return failed(fileName, message)
        }
        if (entries.isEmpty()) {
            val message = "ODT package contains no entries"
            diagnostics.malformedXml(fileName, message, null)
            return failed(fileName, message)
        }

        val contentXml = entries["content.xml"]?.toString(Charsets.UTF_8)
        val stylesXml = entries["styles.xml"]?.toString(Charsets.UTF_8)
        val metadata = parseMetadata(entries["meta.xml"])
        val sourceFeatures = detectSourceFeatures(contentXml, stylesXml)
        val packageData = OdtPackageData(
            entries = entries,
            originalContentXml = contentXml,
            originalStylesXml = stylesXml,
            originalManifestXml = entries["META-INF/manifest.xml"]?.toString(Charsets.UTF_8),
            originalMetaXml = entries["meta.xml"]?.toString(Charsets.UTF_8),
            originalSettingsXml = entries["settings.xml"]?.toString(Charsets.UTF_8),
            sourceFeatures = sourceFeatures
        )

        if (contentXml == null && stylesXml == null) {
            val message = "ODT package has neither content.xml nor styles.xml"
            diagnostics.malformedXml(fileName, message, null)
            return failed(fileName, message).copy(
                odtPackageData = packageData,
                metadata = metadata
            )
        }

        val importer = SvXMLImport(extractedImages, diagnostics)
        if (contentXml == null) {
            importer.parseOdfStyles(stylesXml)
            onStylesParsed?.invoke()
            return OfficeParsedDocument(
                elements = listOf(OfficeDocumentElement.Paragraph(text = "")),
                rawXml = "",
                plainText = "",
                extractedImages = extractedImages,
                isOdt = true,
                odtPackageData = packageData,
                pageCount = metadata.pageCount,
                styles = importer.toDocumentStyles(),
                metadata = metadata
            )
        }

        return importer.parseOdfXml(
            xmlContent = contentXml,
            fileName = fileName,
            stylesXmlContent = stylesXml,
            isOdt = true,
            isOds = false,
            isOdp = false,
            onStylesParsed = onStylesParsed
        ).copy(
            odtPackageData = packageData,
            pageCount = metadata.pageCount,
            metadata = metadata
        )
    }

    private fun readEntries(input: InputStream): Map<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        val budget = ZipScanBudget()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntryBudgeted(budget)
            while (entry != null) {
                val name = entry.name
                if (!isSafeEntryName(name)) throw IOException("Unsafe ODT package entry: $name")
                if (!entry.isDirectory) {
                    if (entries.containsKey(name)) throw IOException("Duplicate ODT package entry: $name")
                    entries[name] = zip.readCappedBytes(ZipSafe.MAX_ZIP_ENTRY_BYTES, budget)
                }
                zip.closeEntry()
                entry = zip.nextEntryBudgeted(budget)
            }
        }
        return entries
    }

    private fun isSafeEntryName(name: String): Boolean {
        if (name.isBlank() || name.startsWith('/') || '\\' in name || '\u0000' in name) return false
        return name.split('/').none { it == ".." }
    }

    private fun failed(fileName: String, message: String): OfficeParsedDocument = OfficeParsedDocument(
        elements = emptyList(),
        rawXml = "",
        plainText = "",
        isOdt = true,
        isParsingFailed = true,
        failureReason = "$fileName: $message"
    )

    private fun parseMetadata(metaXml: ByteArray?): DocumentMetadata {
        if (metaXml == null) return DocumentMetadata()
        var title = ""
        var subject = ""
        var creator = ""
        var initialCreator = ""
        var language = "en-US"
        var creationDate = ""
        var modified = ""
        var generator = ""
        var description = ""
        var pageCount = 0
        var wordCount = 0
        var paragraphCount = 0
        var characterCount = 0
        val keywords = mutableListOf<String>()
        var target: String? = null
        val text = StringBuilder()

        try {
            val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
            parser.setInput(ByteArrayInputStream(metaXml), "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val local = parser.name.orEmpty().substringAfterLast(':')
                        if (local == "document-statistic") {
                            pageCount = attribute(parser, "page-count")?.toIntOrNull() ?: pageCount
                            wordCount = attribute(parser, "word-count")?.toIntOrNull() ?: wordCount
                            paragraphCount = attribute(parser, "paragraph-count")?.toIntOrNull() ?: paragraphCount
                            characterCount = attribute(parser, "character-count")?.toIntOrNull() ?: characterCount
                        } else if (local in METADATA_TEXT_ELEMENTS) {
                            target = local
                            text.setLength(0)
                        }
                    }
                    XmlPullParser.TEXT -> if (target != null) text.append(parser.text.orEmpty())
                    XmlPullParser.END_TAG -> {
                        val local = parser.name.orEmpty().substringAfterLast(':')
                        if (local == target) {
                            val value = text.toString().trim()
                            when (local) {
                                "title" -> title = value
                                "subject" -> subject = value
                                "creator" -> creator = value
                                "initial-creator" -> initialCreator = value
                                "keyword" -> if (value.isNotEmpty()) keywords += value
                                "language" -> if (value.isNotEmpty()) language = value
                                "creation-date" -> creationDate = value
                                "date" -> modified = value
                                "generator" -> generator = value
                                "description" -> description = value
                            }
                            target = null
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
            return DocumentMetadata()
        }

        val author = creator.ifBlank { initialCreator }
        return DocumentMetadata(
            title = title,
            subject = subject,
            author = author,
            creator = author,
            keywords = keywords,
            language = language,
            modified = modified,
            creationDate = creationDate,
            generator = generator.ifBlank { "Papirus Office" },
            wordCount = wordCount,
            paragraphCount = paragraphCount,
            characterCount = characterCount,
            pageCount = pageCount,
            description = description
        )
    }

    private fun detectSourceFeatures(contentXml: String?, stylesXml: String?): OdtSourceFeatures {
        var indexes = false
        var sections = false
        var tables = false
        var fontFaces = false

        fun scan(xml: String?) {
            if (xml.isNullOrBlank()) return
            try {
                val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
                parser.setInput(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        when (parser.name.orEmpty().substringAfterLast(':')) {
                            "table-of-content", "alphabetical-index", "table-index",
                            "illustration-index", "object-index", "user-index" -> indexes = true
                            "section" -> sections = true
                            "table" -> tables = true
                            "font-face" -> fontFaces = true
                        }
                    }
                    event = parser.next()
                }
            } catch (_: Exception) {
                // The semantic parser reports malformed XML; feature detection is advisory.
            }
        }

        scan(contentXml)
        scan(stylesXml)
        return OdtSourceFeatures(indexes, sections, tables, fontFaces)
    }

    private fun attribute(parser: XmlPullParser, localName: String): String? {
        for (index in 0 until parser.attributeCount) {
            if (parser.getAttributeName(index).substringAfterLast(':') == localName) {
                return parser.getAttributeValue(index)
            }
        }
        return null
    }

    private companion object {
        val METADATA_TEXT_ELEMENTS = setOf(
            "title", "subject", "creator", "initial-creator", "keyword", "language",
            "creation-date", "date", "generator", "description"
        )
    }
}
