package com.makerandreas.papirusoffice.data.framework

import android.content.Context
import android.util.Log
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Papirus XML Import/Export Engine.
 * Implements simulations and integrations for LibreOffice SDK Guide Chapter 50: Importing XML.
 * Supports XSLT Filters, DOM Parsing, Labeled String Parsing, JAXB unmarshalling, and FilterFactory query.
 */
object PapirusXmlEngine {

    private const val TAG = "PapirusXmlEngine"
    private val logBuffer = mutableListOf<String>()

    fun getLogs(): List<String> = logBuffer.toList()

    fun clearLogs() {
        logBuffer.clear()
        addLog("XML Engine initialized.")
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] $message"
        Log.d(TAG, logLine)
        logBuffer.add(logLine)
    }

    // --- XML SAMPLES ---

    val PAY_XML = """
        <?xml version="1.0"?>
        <payments>
          <payment>
            <purpose>CD</purpose>
            <amount>12.95</amount>
            <tax>19.1234</tax>
            <maturity>2008-03-01</maturity>
          </payment>
          <payment>
            <purpose>DVD</purpose>
            <amount>19.95</amount>
            <tax>19.4321</tax>
            <maturity>2008-03-02</maturity>
          </payment>
          <payment>
            <purpose>Clothes</purpose>
            <amount>99.95</amount>
            <tax>18.5678</tax>
            <maturity>2008-03-03</maturity>
          </payment>
          <payment>
            <purpose>Book</purpose>
            <amount>9.49</amount>
            <tax>18.9876</tax>
            <maturity>2008-03-04</maturity>
          </payment>
        </payments>
    """.trimIndent()

    val COMPANY_XML = """
        <?xml version="1.0" encoding="UTF-8" ?>
        <Companies>
          <Company>
            <Name>ABC</Name>
            <Executive type="CEO">
                <LastName>Smith</LastName>
                <FirstName>Jim</FirstName>
                <street>123 Broad Street</street>
                <city>Manchester</city>
                <state>Cheshire</state>
                <zip>11234</zip>
            </Executive>
          </Company>
          <Company>
            <Name>NBC</Name>
            <Executive type="President">
                <LastName>Jones</LastName>
                <FirstName>Lucy</FirstName>
                <street>23 Bradford St</street>
                <city>Asbury</city>
                <state>Lincs</state>
                <zip>33451</zip>
            </Executive>
          </Company>
          <Company>
            <Name>BBC</Name>
            <Executive type="Boss">
                <LastName>Singh</LastName>
                <FirstName>Oxley</FirstName>
                <street>16d Towers</street>
                <city>Wimbledon</city>
                <state>London</state>
                <zip>77392</zip>
            </Executive>
          </Company>
        </Companies>
    """.trimIndent()

    val CLUBS_XML = """
        <?xml version="1.0"?>
        <club-database>
          <association id="BAWA">
            <club id="Q21" charter="2002">
              <name>Castro Valley Wrestling Club</name>
              <contact>Ron Maes</contact>
              <location>Castro Valley</location>
              <phone>510-555-1491</phone>
              <email>cvwcron@example.com</email>
              <age-groups type="KCJOW"/>
              <info>Practices every Tuesday/Thursday.</info>
            </club>
            <club id="Q22" charter="2005">
              <name>Coastside Grapplers</name>
              <contact>Jane Doe</contact>
              <location>Half Moon Bay</location>
              <phone>650-555-0922</phone>
              <email>coast@example.com</email>
              <age-groups type="JOW"/>
              <info>Practices every Monday/Wednesday.</info>
            </club>
          </association>
          <association id="CAGWA">
            <club id="G33" charter="1999">
              <name>Big Bear Grizzlies</name>
              <contact>John Grizzly</contact>
              <location>Big Bear</location>
              <phone>909-555-7731</phone>
              <email>grizzly@example.com</email>
              <age-groups type="KCJ"/>
              <info>Weekend camps available.</info>
            </club>
          </association>
        </club-database>
    """.trimIndent()

    val WEATHER_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <current>
            <city id="1610780" name="Hat Yai">
                <coord lat="7.01" lon="100.48"/>
                <country>TH</country>
                <sun rise="2017-01-01T23:30:31" set="2017-01-02T11:14:18"/>
            </city>
            <temperature max="25" min="25" unit="metric" value="25"/>
            <humidity unit="%" value="94"/>
            <pressure unit="hPa" value="1011"/>
            <wind>
                <speed name="Gentle Breeze" value="3.6"/>
                <gusts/>
                <direction code="NNE" name="North-northeast" value="30"/>
            </wind>
            <clouds name="broken clouds" value="75"/>
            <visibility value="5000"/>
            <precipitation mode="no"/>
            <weather icon="10n" number="501" value="moderate rain"/>
            <lastupdate value="2017-01-02T15:30:00"/>
        </current>
    """.trimIndent()

    // --- SECTION 1: FILTER FACTORY REGISTRY METADATA ---

    data class FilterProps(
        val name: String,
        val type: String,
        val documentService: String,
        val filterService: String,
        val flags: Int,
        val uiname: String,
        val userData: List<String>,
        val templateName: String = ""
    )

    private val filterRegistry = mapOf(
        "Pay" to FilterProps(
            name = "Pay",
            type = "Pay",
            documentService = "com.sun.star.sheet.SpreadsheetDocument",
            filterService = "com.sun.star.comp.Calc.XmlFilterAdaptor",
            flags = 0x80043, // Import + Export + Own
            uiname = "XML Payments",
            userData = listOf(
                "com.sun.star.documentconversion.XSLTFilter",
                "false",
                "com.sun.star.comp.Calc.XMLOasisImporter",
                "com.sun.star.comp.Calc.XMLOasisExporter",
                "file:///soffice/share/xslt/payImport.xsl",
                "file:///soffice/share/xslt/payExport.xsl"
            ),
            templateName = ""
        ),
        "Clubs" to FilterProps(
            name = "Clubs",
            type = "Clubs",
            documentService = "com.sun.star.text.TextDocument",
            filterService = "com.sun.star.comp.Writer.XmlFilterAdaptor",
            flags = 0x80043,
            uiname = "XML Clubs",
            userData = listOf(
                "com.sun.star.documentconversion.XSLTFilter",
                "false",
                "com.sun.star.comp.Writer.XMLOasisImporter",
                "com.sun.star.comp.Writer.XMLOasisExporter",
                "file:///soffice/share/xslt/clubsImport.xsl",
                "file:///soffice/share/xslt/clubsExport.xsl"
            ),
            templateName = "file:///templates/clubsTemplate.ott"
        ),
        "AbiWord" to FilterProps(
            name = "AbiWord",
            type = "AbiWord",
            documentService = "com.sun.star.text.TextDocument",
            filterService = "com.sun.star.comp.Writer.AbiWordFilter",
            flags = 0x00041, // Import + Export
            uiname = "AbiWord Document",
            userData = emptyList()
        )
    )

    fun queryFilterNames(): List<String> {
        addLog("[FilterFactory] Querying all registered filter names.")
        return filterRegistry.keys.toList()
    }

    fun getFilterProperties(filterName: String): FilterProps? {
        addLog("[FilterFactory] Fetching properties for filter: \"$filterName\"")
        val props = filterRegistry[filterName]
        if (props != null) {
            val flagHex = "0x" + Integer.toHexString(props.flags)
            addLog("[FilterFactory] Filter found: \"${props.uiname}\", Flags: $flagHex")
            addLog("[FilterFactory]   Is Import: ${isImport(props.flags)}")
            addLog("[FilterFactory]   Is Export: ${isExport(props.flags)}")
        } else {
            addLog("[FilterFactory] Filter \"$filterName\" not found in registry.")
        }
        return props
    }

    private fun isImport(flags: Int): Boolean = (flags and 0x00001) != 0 || (flags and 0x00002) != 0
    private fun isExport(flags: Int): Boolean = (flags and 0x00002) != 0 || (flags and 0x00004) != 0

    // --- SECTION 2: COMMAND LINE & XSLT TRANSFORMS ---

    fun simulateInfilter(filename: String, filterName: String): Boolean {
        addLog("[CLI] Command: infilter \"$filename\" \"$filterName\"")
        val filter = filterRegistry[filterName]
        if (filter == null) {
            addLog("[CLI] Error: Filter \"$filterName\" is not registered.")
            return false
        }
        addLog("[CLI] Match found: ${filter.uiname} targeting ${filter.documentService}")
        if (filename.endsWith("pay.xml") && filterName == "Pay") {
            addLog("[XSLT] Parsing pay.xml with libxslt processor...")
            addLog("[XSLT] Applying template: payImport.xsl")
            addLog("[XSLT] Successfully translated pay.xml elements to Flat ODF XML Spreadsheet!")
            addLog("[Office] Instantiated Scalc document with converted payments data grid.")
            return true
        } else if (filename.endsWith("clubs.xml") && filterName == "Clubs") {
            addLog("[XSLT] Parsing clubs.xml with libxslt processor...")
            addLog("[XSLT] Applying template: clubsImport.xsl using styled paragraphs (clubsTemplate.ott)")
            addLog("[XSLT] Successfully translated clubs.xml to styled Flat ODF XML Text!")
            addLog("[Office] Instantiated Swriter document with associations and club elements.")
            return true
        }
        addLog("[CLI] Error: Unable to transform \"$filename\" with \"$filterName\". Unsupported test scenario.")
        return false
    }

    fun simulateConvert(filename: String, targetFormatAndFilter: String): String? {
        addLog("[CLI] Command: convert \"$filename\" \"$targetFormatAndFilter\"")
        val parts = targetFormatAndFilter.split(":")
        val targetFormat = parts[0]
        val filterName = if (parts.size > 1) parts[1] else null

        if (filename.endsWith(".ods") && targetFormat == "xml" && filterName == "Pay") {
            addLog("[XSLT] Flat XML Export initiated for Calc sheet.")
            addLog("[XSLT] Loading payExport.xsl exporter rules...")
            addLog("[XSLT] Converting 2D row/column structure back to <payments><payment> elements.")
            addLog("[CLI] Saved output XML file: payment.xml")
            return PAY_XML
        } else if (filename.endsWith(".odt") && targetFormat == "xml" && filterName == "Clubs") {
            addLog("[XSLT] Flat XML Export initiated for Writer text.")
            addLog("[XSLT] Mapping styled paragraphs (e.g., Club_20_Name) to XML tags.")
            addLog("[CLI] Saved output XML file: clubsEx.xml")
            return CLUBS_XML
        }
        addLog("[CLI] Error: Unsupported conversion scenario.")
        return null
    }

    // --- SECTION 3: DOM PARSING (Section 3.1) ---

    fun parseCompaniesDom(): List<Map<String, String>> {
        addLog("[DOM] Loading and parsing company.xml into a Document tree.")
        val list = mutableListOf<Map<String, String>>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputSource = org.xml.sax.InputSource(java.io.StringReader(COMPANY_XML))
            val doc: Document = builder.parse(inputSource)
            doc.documentElement.normalize()

            addLog("[DOM] Root node: <${doc.documentElement.nodeName}>")
            val companyNodes = doc.getElementsByTagName("Company")
            addLog("[DOM] Found ${companyNodes.length} Company elements.")

            for (i in 0 until companyNodes.length) {
                val companyNode = companyNodes.item(i)
                if (companyNode.nodeType == Node.ELEMENT_NODE) {
                    val element = companyNode as Element
                    val name = element.getElementsByTagName("Name").item(0).textContent.trim()
                    
                    val execNode = element.getElementsByTagName("Executive").item(0) as Element
                    val execType = execNode.getAttribute("type")
                    val lastName = execNode.getElementsByTagName("LastName").item(0).textContent.trim()
                    val firstName = execNode.getElementsByTagName("FirstName").item(0).textContent.trim()
                    val street = execNode.getElementsByTagName("street").item(0).textContent.trim()
                    val city = execNode.getElementsByTagName("city").item(0).textContent.trim()
                    val state = execNode.getElementsByTagName("state").item(0).textContent.trim()
                    val zip = execNode.getElementsByTagName("zip").item(0).textContent.trim()

                    addLog("[DOM] Extracted: Company = $name, Exec = $firstName $lastName ($execType)")
                    list.add(
                        mapOf(
                            "company" to name,
                            "execType" to execType,
                            "lastName" to lastName,
                            "firstName" to firstName,
                            "street" to street,
                            "city" to city,
                            "state" to state,
                            "zip" to zip
                        )
                    )
                }
            }
        } catch (e: Exception) {
            addLog("[DOM] Parsing Error: ${e.localizedMessage}")
        }
        return list
    }

    fun parsePaymentsTo2D(): Array<Array<Any>> {
        addLog("[DOM] Mapping pay.xml elements to a structured 2D array.")
        val cols = arrayOf("Purpose", "Amount", "Tax", "Maturity")
        val dataList = mutableListOf<Array<Any>>()
        dataList.add(cols.map { it as Any }.toTypedArray())

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(org.xml.sax.InputSource(java.io.StringReader(PAY_XML)))
            val payments = doc.getElementsByTagName("payment")

            addLog("[DOM] Found ${payments.length} payment nodes. Initializing table mapping.")
            for (i in 0 until payments.length) {
                val node = payments.item(i) as Element
                val purpose = node.getElementsByTagName("purpose").item(0).textContent.trim()
                val amount = node.getElementsByTagName("amount").item(0).textContent.trim().toDouble()
                val tax = node.getElementsByTagName("tax").item(0).textContent.trim().toDouble()
                val maturity = node.getElementsByTagName("maturity").item(0).textContent.trim()

                addLog("[DOM] Mapped payment row $i: $purpose, $$amount, $tax%, $maturity")
                dataList.add(arrayOf(purpose, amount, tax, maturity))
            }
        } catch (e: Exception) {
            addLog("[DOM] Mapping Error: ${e.localizedMessage}")
        }
        return dataList.toTypedArray()
    }

    // --- SECTION 4: LABELED STRINGS & TOKENIZATION (Section 3.2) ---

    fun extractXmlAsLabeledStrings(xmlContent: String): String {
        addLog("[LabeledStrings] Traversing DOM to generate labeled indented text.")
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(org.xml.sax.InputSource(java.io.StringReader(xmlContent)))
            val writer = StringWriter()
            val rootNodes = doc.childNodes
            for (i in 0 until rootNodes.length) {
                visitNodeAndWrite(writer, rootNodes.item(i), "")
            }
            addLog("[LabeledStrings] Generated labeled layout representation.")
            writer.toString().trim()
        } catch (e: Exception) {
            "[Error: ${e.localizedMessage}]"
        }
    }

    private fun visitNodeAndWrite(writer: StringWriter, node: Node, indent: String) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            writer.write("$indent${node.nodeName}")
            // Write attributes with '='
            val attrs = node.attributes
            if (attrs != null) {
                for (i in 0 until attrs.length) {
                    val attr = attrs.item(i)
                    writer.write("  ${attr.nodeName}= \"${attr.nodeValue}\"")
                }
            }

            // Write child text or children
            val children = node.childNodes
            var hasText = false
            for (i in 0 until children.length) {
                val child = children.item(i)
                if (child.nodeType == Node.TEXT_NODE) {
                    val text = child.nodeValue.trim()
                    if (text.isNotEmpty()) {
                        writer.write(": \"$text\"")
                        hasText = true
                    }
                }
            }
            writer.write("\n")

            if (!hasText) {
                for (i in 0 until children.length) {
                    visitNodeAndWrite(writer, children.item(i), "$indent  ")
                }
            }
        }
    }

    /**
     * Splits labeled string into token arrays like BuildXMLSheet.java
     */
    fun tokenizeLabeledStringToTable(text: String): Array<Array<Any>> {
        addLog("[Tokenizer] Converting labeled text lines into a aligned spreadsheet grid.")
        val lines = text.split("\n")
        val parsedRows = mutableListOf<List<String>>()
        var maxCols = 0

        for (line in lines) {
            if (line.trim().isEmpty()) continue
            val tokens = splitLine(line)
            if (tokens.isNotEmpty()) {
                parsedRows.add(tokens)
                if (tokens.size > maxCols) {
                    maxCols = tokens.size
                }
            }
        }

        // Align and pad rows
        val table = Array(parsedRows.size) { r ->
            val row = parsedRows[r]
            Array<Any>(maxCols) { c ->
                if (c < row.size) row[c] else ""
            }
        }
        addLog("[Tokenizer] Grid created. Size: ${table.size}x$maxCols. Indented cells mapped successfully.")
        return table
    }

    private fun splitLine(line: String): List<String> {
        val padded = "$line "
        var inQuote = false
        var isIndenting = true
        var spaceCount = 0
        val tokens = mutableListOf<String>()
        val currentWord = StringBuilder()

        for (i in 0 until padded.length - 1) {
            val ch = padded[i]
            if (ch != ' ' && isIndenting) {
                isIndenting = false
            }

            if (ch == ' ' && isIndenting) {
                spaceCount++
                if (spaceCount % 2 == 0) {
                    tokens.add("") // map two spaces indent to empty column
                }
            } else if (ch == '"' || (ch == ' ' && !inQuote)) {
                if (ch == '"') {
                    inQuote = !inQuote
                }
                if (!inQuote && currentWord.isNotEmpty()) {
                    val lastCh = currentWord.last()
                    if (lastCh == ':' || lastCh == '=') {
                        currentWord.deleteCharAt(currentWord.length - 1) // strip assignment indicator
                    }
                    tokens.add(currentWord.toString())
                    currentWord.clear()
                }
            } else {
                currentWord.append(ch)
            }
        }
        return tokens
    }

    // --- SECTION 5: JAXB UNMARSHALLING SIMULATION (Section 3.3) ---

    // Java unmarshalling schemas
    data class PaymentsJaxb(val payments: List<PaymentJaxb>)
    data class PaymentJaxb(val purpose: String, val amount: Double, val tax: Double, val maturity: String)

    data class ClubDatabaseJaxb(val associations: List<AssociationJaxb>)
    data class AssociationJaxb(val id: String, val clubs: List<ClubJaxb>)
    data class ClubJaxb(val id: String, val charter: Int, val name: String, val contact: String, val location: String, val phone: String, val email: String)

    fun simulateUnmarshallPay(): PaymentsJaxb {
        addLog("[JAXB] Unmarshalling pay.xml using compiled Payments.class and ObjectFactory.java classes...")
        val paymentList = listOf(
            PaymentJaxb("CD", 12.95, 19.1234, "2008-03-01"),
            PaymentJaxb("DVD", 19.95, 19.4321, "2008-03-02"),
            PaymentJaxb("Clothes", 99.95, 18.5678, "2008-03-03"),
            PaymentJaxb("Book", 9.49, 18.9876, "2008-03-04")
        )
        paymentList.forEach {
            addLog("[JAXB] Unmarshalled Payment entity: ${it.purpose} -> $${it.amount}")
        }
        addLog("[JAXB] Completed unmarshalling of Payments context.")
        return PaymentsJaxb(paymentList)
    }

    fun simulateUnmarshallClubs(): ClubDatabaseJaxb {
        addLog("[JAXB] Unmarshalling clubs.xml using ClubDatabase.class context...")
        val associations = listOf(
            AssociationJaxb("BAWA", listOf(
                ClubJaxb("Q21", 2002, "Castro Valley Wrestling Club", "Ron Maes", "Castro Valley", "510-555-1491", "cvwcron@example.com"),
                ClubJaxb("Q22", 2005, "Coastside Grapplers", "Jane Doe", "Half Moon Bay", "650-555-0922", "coast@example.com")
            )),
            AssociationJaxb("CAGWA", listOf(
                ClubJaxb("G33", 1999, "Big Bear Grizzlies", "John Grizzly", "Big Bear", "909-555-7731", "grizzly@example.com")
            ))
        )
        associations.forEach { assoc ->
            addLog("[JAXB] Association ID: ${assoc.id}")
            assoc.clubs.forEach { club ->
                addLog("[JAXB]   Loaded Club: ${club.name} (Chartered in ${club.charter})")
            }
        }
        addLog("[JAXB] Completed unmarshalling of ClubDatabase context.")
        return ClubDatabaseJaxb(associations)
    }

    fun simulateUnmarshallWeatherWithConflictResolve(resolveConflict: Boolean): Boolean {
        addLog("[JAXB] Compiling schema weather.xsd with xjc compiler...")
        if (!resolveConflict) {
            addLog("[xjc ERROR] Property \"Value\" is already defined in weather.xsd.")
            addLog("[xjc ERROR] Multi-context attributes share identical name 'value' (e.g., speed value, humidity value, pressure value, clouds value, lastupdate value).")
            addLog("[xjc ERROR] Resolve the compilation error using <jaxb:property> tag inside weather.xsd.")
            return false
        } else {
            addLog("[xjc] Resolving conflict via <jaxb:property name=\"valueAttribute\"/> annotation binding.")
            addLog("[xjc] Custom binding mapped: xmlns:jaxb=\"https://java.sun.com/xml/ns/jaxb\"")
            addLog("[xjc] Successfully resolved name conflicts. Compilation of package 'Weather' complete.")
            addLog("[JAXB] Unmarshalling weather.xml...")
            addLog("[JAXB] Successfully loaded Current object hierarchy: City = Hat Yai, WeatherIcon = 10n, TemperatureValue = 25")
            return true
        }
    }
}
