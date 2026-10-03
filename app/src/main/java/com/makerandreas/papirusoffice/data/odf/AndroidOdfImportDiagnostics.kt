package com.makerandreas.papirusoffice.data.odf

import android.content.Context
import com.makerandreas.papirusoffice.data.util.DocumentParsingLogger

/** Android-backed diagnostics adapter kept outside the semantic importer. */
class AndroidOdfImportDiagnostics(
    private val context: Context
) : OdfImportDiagnostics {
    override fun unsupportedTag(fileName: String, tagName: String, attributes: Map<String, String>) {
        DocumentParsingLogger.logUnsupportedTag(context, fileName, tagName, attributes)
    }

    override fun malformedXml(fileName: String, errorMessage: String, cause: Throwable?) {
        DocumentParsingLogger.logMalformedXml(context, fileName, errorMessage, cause)
    }
}
