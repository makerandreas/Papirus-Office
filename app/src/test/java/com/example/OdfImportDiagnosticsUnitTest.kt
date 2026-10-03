package com.example

import com.makerandreas.papirusoffice.data.odf.OdfImportDiagnostics
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs as plain JUnit: the ODT semantic pipeline does not require Android Context. */
class OdfImportDiagnosticsUnitTest {
    @Test
    fun packageFailureReportsThroughInjectedDiagnostics() {
        val diagnostics = RecordingDiagnostics()
        val parsed = OdtImportPipeline(diagnostics).parse("not a package".toByteArray(), "broken.odt")

        assertTrue(parsed.isParsingFailed)
        assertTrue(parsed.failureReason.orEmpty().contains("broken.odt"))
        assertTrue(diagnostics.malformed.isNotEmpty())
    }

    private class RecordingDiagnostics : OdfImportDiagnostics {
        val malformed = mutableListOf<String>()

        override fun unsupportedTag(
            fileName: String,
            tagName: String,
            attributes: Map<String, String>
        ) = Unit

        override fun malformedXml(fileName: String, errorMessage: String, cause: Throwable?) {
            malformed += "$fileName:$errorMessage"
        }
    }
}
