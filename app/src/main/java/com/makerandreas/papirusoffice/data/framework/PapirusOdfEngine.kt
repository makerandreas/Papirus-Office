package com.makerandreas.papirusoffice.data.framework

object PapirusOdfEngine {
    data class OdfProperties(
        val title: String = "",
        val author: String = "",
        val subject: String = "",
        val description: String = "",
        val secretValue: String = ""
    ) {
        val generator: String = "Papirus Office"
    }

    data class ZipEntryInfo(
        val name: String,
        val rawSize: Long,
        val compressedSize: Long
    )

    private val logs = mutableListOf<String>()
    private var docProperties = OdfProperties("Untitled Document", "User", "Document", "Papirus Office Document")

    fun getLogs(): List<String> = logs.toList()
    fun clearLogs() { logs.clear() }

    fun getDocProperties(): OdfProperties = docProperties
    fun updateDocProperties(props: OdfProperties) { docProperties = props }

    fun getOdfZipContents(doc: String): List<ZipEntryInfo> = emptyList()
    fun simulateUnzipFile(doc: String, entry: String): String = "extracted_$entry"

    fun simulateMakeTextDoc(title: String, logo: Boolean, list: List<String>): String = "doc_created"
    fun simulateMakeSheet(start: Int, mult: Int): String = "sheet_created"
    fun simulateMakeSlides(title: String, bullets: List<String>): String = "slides_created"

    fun simulateSlideRearrange(): String = "slides_rearranged"
    fun simulateCombineTexts(): String = "texts_combined"
    fun simulateCombineSheets(): String = "sheets_combined"
    fun simulateCombineDecks(): String = "decks_combined"
}
