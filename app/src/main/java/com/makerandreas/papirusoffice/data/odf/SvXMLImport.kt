package com.makerandreas.papirusoffice.data.odf

import android.content.Context
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.util.DocumentParsingLogger
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.ArrayDeque

/**
 * Modern ODF SAX Import Filter class in Papirus Engine,
 * mirroring SvXMLImport in LibreOffice xmloff module ("xo" library).
 *
 * Maintains a stack of element contexts (SvXMLImportContext) for robust context-driven
 * document parsing and token mapping.
 */
class SvXMLImport(
    private val context: Context,
    val extractedImages: Map<String, File> = emptyMap()
) {

    private val contextStack = ArrayDeque<SvXMLImportContext>()
    private val parsedElements = mutableListOf<OfficeDocumentElement>()

    fun addElement(element: OfficeDocumentElement) {
        parsedElements.add(element)
    }

    /**
     * Executes context-driven ODF XML import for content.xml input stream.
     */
    fun parseOdfXml(
        xmlContent: String,
        fileName: String,
        isOdt: Boolean = true,
        isOds: Boolean = false,
        isOdp: Boolean = false
    ): OfficeParsedDocument {
        parsedElements.clear()
        contextStack.clear()

        // Push initial Root document context
        val rootContext = OdfDocumentContentContext(this, OdfXmlToken.XML_DOCUMENT_CONTENT)
        contextStack.push(rootContext)

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val rawTagName = parser.name ?: ""
                        val token = OdfXmlToken.fromTag(rawTagName)
                        
                        val attributes = mutableMapOf<String, String>()
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrPrefix = parser.getAttributePrefix(i)
                            val fullName = if (!attrPrefix.isNullOrEmpty()) "$attrPrefix:$attrName" else attrName
                            attributes[fullName] = parser.getAttributeValue(i)
                            // Also map unprefixed name for convenience
                            attributes[attrName] = parser.getAttributeValue(i)
                        }

                        val currentTop = contextStack.peek() ?: rootContext
                        val childContext = currentTop.createChildContext(token, attributes)
                        
                        // Push child context onto stack and notify start element
                        contextStack.push(childContext)
                        childContext.onStartElement(token, attributes)

                        if (token == OdfXmlToken.XML_UNKNOWN && rawTagName.isNotBlank()) {
                            DocumentParsingLogger.logUnsupportedTag(
                                context = context,
                                fileName = fileName,
                                tagName = rawTagName,
                                attributes = attributes
                            )
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text ?: ""
                        if (text.isNotEmpty()) {
                            contextStack.peek()?.onCharacters(text)
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val rawTagName = parser.name ?: ""
                        val token = OdfXmlToken.fromTag(rawTagName)
                        
                        if (!contextStack.isEmpty()) {
                            val topContext = contextStack.pop()
                            topContext.onEndElement(token)
                        }
                    }
                }
                eventType = parser.next()
            }

            // Build full plain text from parsed elements
            val plainTextBuilder = StringBuilder()
            parsedElements.forEach { element ->
                when (element) {
                    is OfficeDocumentElement.Paragraph -> plainTextBuilder.append(element.text).append("\n\n")
                    is OfficeDocumentElement.Heading -> plainTextBuilder.append(element.text).append("\n\n")
                    is OfficeDocumentElement.ListItem -> plainTextBuilder.append(element.bullet).append(element.text).append("\n")
                    is OfficeDocumentElement.Table -> {
                        element.rows.forEach { row ->
                            plainTextBuilder.append(row.cells.joinToString("\t") { it.text }).append("\n")
                        }
                        plainTextBuilder.append("\n")
                    }
                    else -> {}
                }
            }

            return OfficeParsedDocument(
                elements = parsedElements.toList(),
                rawXml = xmlContent,
                plainText = plainTextBuilder.toString().trim(),
                extractedImages = extractedImages,
                isOdt = isOdt,
                isDocx = false,
                isOds = isOds,
                isXlsx = false,
                isOdp = isOdp,
                isPptx = false,
                isParsingFailed = false,
                failureReason = null
            )

        } catch (e: Exception) {
            val errorMsg = "ODF SAX Import Error: ${e.localizedMessage ?: "Failed parsing XML"}"
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = fileName,
                errorMsg = errorMsg,
                cause = e
            )
            return OfficeParsedDocument(
                elements = emptyList(),
                rawXml = xmlContent,
                plainText = "",
                extractedImages = extractedImages,
                isOdt = isOdt,
                isDocx = false,
                isOds = isOds,
                isXlsx = false,
                isOdp = isOdp,
                isPptx = false,
                isParsingFailed = true,
                failureReason = errorMsg
            )
        }
    }
}
