package com.makerandreas.papirusoffice.data.odf

/** Reporting seam that keeps the semantic importer independent of Android storage. */
interface OdfImportDiagnostics {
    fun unsupportedTag(fileName: String, tagName: String, attributes: Map<String, String>)
    fun malformedXml(fileName: String, errorMessage: String, cause: Throwable?)
}

object SilentOdfImportDiagnostics : OdfImportDiagnostics {
    override fun unsupportedTag(fileName: String, tagName: String, attributes: Map<String, String>) = Unit
    override fun malformedXml(fileName: String, errorMessage: String, cause: Throwable?) = Unit
}
