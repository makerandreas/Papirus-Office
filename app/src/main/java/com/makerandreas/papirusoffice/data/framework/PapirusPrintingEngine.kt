package com.makerandreas.papirusoffice.data.framework

import android.content.Context
import android.print.PrintManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Papirus Printing Engine.
 * Combines LibreOffice SDK Chapter 41 (XPrintable, XPagePrintable, PrintDescriptor)
 * with Android Native PrintManager to provide print capabilities for Writer (Inky),
 * Calc (Cellina), Impress (Slidia), Pagella (Forms/PDF), and SDK Example Inspection.
 */
object PapirusPrintingEngine {

    private const val TAG = "PapirusPrintingEngine"

    /**
     * Executes native Android printing for a document using PapirusPrintDocumentAdapter
     */
    fun printDocument(
        context: Context,
        docTitle: String,
        docType: String,
        pages: List<String>,
        isLandscape: Boolean = false
    ) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Log.e(TAG, "PrintManager service is unavailable on this device.")
                return
            }

            // Create printable document model (LibreOffice XPrintable wrapper)
            val printableDoc = PapirusPrintableDocument(
                docName = docTitle,
                docType = docType,
                contentPages = pages
            )

            // Attach listener to log job lifecycle
            printableDoc.addPrintJobListener(object : XPrintJobListener {
                override fun printJobEvent(e: PrintJobEvent) {
                    Log.d(TAG, "Print Job Event for '$docTitle': State=${e.state}")
                }

                override fun disposing(source: Any?) {
                    Log.d(TAG, "Print Job Disposing for '$docTitle'")
                }
            })

            // Trigger print execution
            val options = PrintOptions(printerName = "Android System Spooler", pages = "1-", wait = true)
            printableDoc.print(options.toPropertyValues())

            // Launch native Android Print UI
            val jobName = "${docTitle.replace(" ", "_")}_Print"
            val adapter = PapirusPrintDocumentAdapter(
                context = context,
                jobName = jobName,
                docTitle = docTitle,
                pageContents = pages,
                isLandscape = isLandscape
            )

            printManager.print(jobName, adapter, null)
            Log.d(TAG, "Native Print Manager dialog launched for '$docTitle'")
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating print job: ${e.localizedMessage}", e)
        }
    }

    /**
     * Helper to print sample documents across Papirus modules
     */
    fun printSampleDocument(context: Context, sampleType: String) {
        val (title, type, pages) = when (sampleType.uppercase()) {
            "WRITER", "INKY" -> Triple(
                "Inky Document - Executive Briefing",
                "Writer",
                listOf(
                    "EXECUTIVE SUMMARY\n\nPapirus Office Writer (Inky) delivers rich document authoring and printing.\n" +
                            "This page tests multi-line paragraph layout, headers, footers, and margins.",
                    "CHAPTER 2: ENGINE ARCHITECTURE\n\nUtilizing LibreOffice SDK Chapter 41 XPrintable & XPagePrintable interfaces.\n" +
                            "Provides cross-platform document rendering and PDF export."
                )
            )
            "CALC", "CELLINA" -> Triple(
                "Cellina Sheet - Financial Quarterly Report",
                "Calc",
                listOf(
                    "QUARTERLY FINANCIAL REPORT (Q1-Q4)\n\n" +
                            "Revenue: $1,250,000\nExpenses: $820,000\nNet Profit: $430,000\n\n" +
                            "Item | Q1 | Q2 | Q3 | Q4\n" +
                            "Sales | 250k | 300k | 320k | 380k\n" +
                            "Ops | 180k | 200k | 210k | 230k"
                )
            )
            "IMPRESS", "SLIDIA" -> Triple(
                "Slidia Presentation - Product Launch Slides",
                "Impress",
                listOf(
                    "SLIDE 1: Papirus Office Product Launch\n\n- Unified LibreOffice Engine\n- Native Android Jetpack Compose UI",
                    "SLIDE 2: Key Features\n\n- Inky Text Editor\n- Cellina Spreadsheet\n- Slidia Presentation\n- Pagella Forms"
                )
            )
            else -> Triple(
                "Papirus Office Form - Official Template",
                "Forms",
                listOf("FORM DOCUMENT\n\nUser Information & Signatures.\nPrinted via Papirus Engine.")
            )
        }

        printDocument(context, title, type, pages)
    }

    /**
     * Lists available Java SDK example reference files in assets/sdk_examples/
     */
    suspend fun listSdkExamples(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            val list = context.assets.list("sdk_examples")?.toList() ?: emptyList()
            list.filter { it.endsWith(".java") }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing SDK examples: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    /**
     * Reads content of a Java SDK example reference file from assets/sdk_examples/
     */
    suspend fun readSdkExampleContent(context: Context, fileName: String): String = withContext(Dispatchers.IO) {
        try {
            context.assets.open("sdk_examples/$fileName").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SDK example '$fileName': ${e.localizedMessage}", e)
            "// Failed to load $fileName: ${e.localizedMessage}"
        }
    }
}
