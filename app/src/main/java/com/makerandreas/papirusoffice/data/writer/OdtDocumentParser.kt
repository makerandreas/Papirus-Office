package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.PapirusLogger
import com.makerandreas.papirusoffice.data.ParserReport
import com.makerandreas.papirusoffice.data.odf.OdfImportDiagnostics
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import com.makerandreas.papirusoffice.data.odf.SilentOdfImportDiagnostics
import com.makerandreas.papirusoffice.data.toOfficeDocument
import java.io.File

/** Compatibility facade over the authoritative ODT package/import pipeline. */
class OdtDocumentParser(
    diagnostics: OdfImportDiagnostics = SilentOdfImportDiagnostics
) {
    private val pipeline = OdtImportPipeline(diagnostics)

    fun parse(bytes: ByteArray): OfficeDocument {
        PapirusLogger.d("ODT", "READ_START")
        val parsed = pipeline.parse(bytes)
        if (parsed.isParsingFailed) {
            PapirusLogger.e("ODT", "READ_FAILED reason=${parsed.failureReason}")
            return OfficeDocument(
                parserReport = ParserReport(
                    format = "ODT",
                    warnings = listOfNotNull(parsed.failureReason)
                )
            )
        }
        PapirusLogger.d("ODT", "BODY_ELEMENTS=${parsed.elements.size}")
        PapirusLogger.d("ODT", "READ_SUCCESS")
        return parsed.toOfficeDocument()
    }

    fun parse(file: File): OfficeDocument {
        PapirusLogger.d("ODT", "READ_START file=${file.name}")
        val parsed = pipeline.parse(file)
        if (parsed.isParsingFailed) {
            PapirusLogger.e("ODT", "READ_FAILED reason=${parsed.failureReason}")
            return OfficeDocument(
                parserReport = ParserReport(
                    format = "ODT",
                    warnings = listOfNotNull(parsed.failureReason)
                )
            )
        }
        PapirusLogger.d("ODT", "BODY_ELEMENTS=${parsed.elements.size}")
        PapirusLogger.d("ODT", "READ_SUCCESS")
        return parsed.toOfficeDocument()
    }
}
