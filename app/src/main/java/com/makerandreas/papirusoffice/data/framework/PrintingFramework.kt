package com.makerandreas.papirusoffice.data.framework

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * LibreOffice SDK Chapter 41 & Java Print Service (JPS) Framework implementation for Papirus Office.
 * Provides XPrintable, XPagePrintable, XPrintJobBroadcaster, PrintDescriptor, PrintOptions,
 * PagePrintSettings, and native Android PrintManager adapters.
 */

enum class PaperOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class PaperFormat {
    A3, A4, A5, B4, B5, LETTER, LEGAL, TABLOID, USER
}

enum class PrintableState {
    JOB_STARTED,
    JOB_COMPLETED,
    JOB_SPOOLED,
    JOB_ABORTED,
    JOB_FAILED,
    JOB_SPOOLING_FAILED
}

data class PrintJobEvent(
    val state: PrintableState,
    val source: Any? = null
)

interface XPrintJobListener {
    fun printJobEvent(e: PrintJobEvent)
    fun disposing(source: Any?)
}

interface XPrintJobBroadcaster {
    fun addPrintJobListener(listener: XPrintJobListener)
    fun removePrintJobListener(listener: XPrintJobListener)
}

interface XPrintable {
    fun getPrinter(): MediaDescriptor
    fun setPrinter(printerProps: MediaDescriptor)
    fun print(options: MediaDescriptor)
}

interface XPagePrintable {
    fun getPagePrintSettings(): MediaDescriptor
    fun setPagePrintSettings(settings: MediaDescriptor)
    fun printPages(options: MediaDescriptor)
}

/**
 * Java Print Service (JPS) DocFlavor simulation
 */
data class DocFlavor(
    val mimeType: String,
    val representationClass: String
) {
    companion object {
        val GIF = DocFlavor("image/gif", "java.io.InputStream")
        val JPEG = DocFlavor("image/jpeg", "java.io.InputStream")
        val PNG = DocFlavor("image/png", "java.io.InputStream")
        val PDF = DocFlavor("application/pdf", "java.io.InputStream")
        val POSTSCRIPT = DocFlavor("application/postscript", "java.io.InputStream")
        val TEXT_PLAIN = DocFlavor("text/plain", "java.io.InputStream")
        val AUTOSENSE = DocFlavor("application/octet-stream", "java.io.InputStream")
    }
}

/**
 * PrintDescriptor holds printer properties (Table 1 - com.sun.star.view.PrinterDescriptor)
 */
data class PrintDescriptor(
    var name: String = "Papirus Virtual Printer",
    var paperOrientation: PaperOrientation = PaperOrientation.PORTRAIT,
    var paperFormat: PaperFormat = PaperFormat.A4,
    var paperSizeWidth: Int = 21000, // 100th mm
    var paperSizeHeight: Int = 29700,
    var isBusy: Boolean = false,
    var canSetPaperOrientation: Boolean = true,
    var canSetPaperFormat: Boolean = true,
    var canSetPaperSize: Boolean = true
) {
    fun toPropertyValues(): MediaDescriptor {
        return listOf(
            PropertyValue("Name", name),
            PropertyValue("PaperOrientation", paperOrientation),
            PropertyValue("PaperFormat", paperFormat),
            PropertyValue("PaperSizeWidth", paperSizeWidth),
            PropertyValue("PaperSizeHeight", paperSizeHeight),
            PropertyValue("IsBusy", isBusy),
            PropertyValue("CanSetPaperOrientation", canSetPaperOrientation),
            PropertyValue("CanSetPaperFormat", canSetPaperFormat),
            PropertyValue("CanSetPaperSize", canSetPaperSize)
        )
    }

    companion object {
        fun fromPropertyValues(props: MediaDescriptor): PrintDescriptor {
            val desc = PrintDescriptor()
            for (p in props) {
                when (p.name) {
                    "Name" -> desc.name = p.value as? String ?: desc.name
                    "PaperOrientation" -> desc.paperOrientation = p.value as? PaperOrientation ?: desc.paperOrientation
                    "PaperFormat" -> desc.paperFormat = p.value as? PaperFormat ?: desc.paperFormat
                    "PaperSizeWidth" -> desc.paperSizeWidth = (p.value as? Number)?.toInt() ?: desc.paperSizeWidth
                    "PaperSizeHeight" -> desc.paperSizeHeight = (p.value as? Number)?.toInt() ?: desc.paperSizeHeight
                    "IsBusy" -> desc.isBusy = p.value as? Boolean ?: desc.isBusy
                }
            }
            return desc
        }
    }
}

/**
 * PrintOptions holds options passed to XPrintable.print() (Table 3 - com.sun.star.view.PrintOptions)
 */
data class PrintOptions(
    var copyCount: Int = 1,
    var fileName: String = "",
    var collate: Boolean = true,
    var pages: String = "1-",
    var wait: Boolean = true,
    var printerName: String = "Papirus Virtual Printer"
) {
    fun toPropertyValues(): MediaDescriptor {
        return listOf(
            PropertyValue("CopyCount", copyCount),
            PropertyValue("FileName", fileName),
            PropertyValue("Collate", collate),
            PropertyValue("Pages", pages),
            PropertyValue("Wait", wait),
            PropertyValue("PrinterName", printerName)
        )
    }

    companion object {
        fun fromPropertyValues(props: MediaDescriptor): PrintOptions {
            val opt = PrintOptions()
            for (p in props) {
                when (p.name) {
                    "CopyCount" -> opt.copyCount = (p.value as? Number)?.toInt() ?: opt.copyCount
                    "FileName" -> opt.fileName = p.value as? String ?: opt.fileName
                    "Collate" -> opt.collate = p.value as? Boolean ?: opt.collate
                    "Pages" -> opt.pages = p.value as? String ?: opt.pages
                    "Wait" -> opt.wait = p.value as? Boolean ?: opt.wait
                    "PrinterName" -> opt.printerName = p.value as? String ?: opt.printerName
                }
            }
            return opt
        }
    }
}

/**
 * PagePrintSettings holds settings passed to XPagePrintable (Table 9 - com.sun.star.text.PagePrintSettings)
 */
data class PagePrintSettings(
    var pageRows: Short = 1,
    var pageColumns: Short = 1,
    var leftMargin: Int = 0,
    var rightMargin: Int = 0,
    var topMargin: Int = 0,
    var bottomMargin: Int = 0,
    var horiMargin: Int = 0,
    var vertMargin: Int = 0,
    var isLandscape: Boolean = false
) {
    fun toPropertyValues(): MediaDescriptor {
        return listOf(
            PropertyValue("PageRows", pageRows),
            PropertyValue("PageColumns", pageColumns),
            PropertyValue("LeftMargin", leftMargin),
            PropertyValue("RightMargin", rightMargin),
            PropertyValue("TopMargin", topMargin),
            PropertyValue("BottomMargin", bottomMargin),
            PropertyValue("HoriMargin", horiMargin),
            PropertyValue("VertMargin", vertMargin),
            PropertyValue("IsLandscape", isLandscape)
        )
    }
}

/**
 * Standard implementation of XPrintable and XPrintJobBroadcaster for Papirus Office documents.
 */
open class PapirusPrintableDocument(
    val docName: String,
    val docType: String,
    val contentPages: List<String>
) : XPrintable, XPagePrintable, XPrintJobBroadcaster {

    private val TAG = "PapirusPrintableDocument"
    private var printerDescriptor = PrintDescriptor(name = "Papirus $docType Printer")
    private var pagePrintSettings = PagePrintSettings()
    private val listeners = mutableListOf<XPrintJobListener>()

    override fun getPrinter(): MediaDescriptor = printerDescriptor.toPropertyValues()

    override fun setPrinter(printerProps: MediaDescriptor) {
        printerDescriptor = PrintDescriptor.fromPropertyValues(printerProps)
        Log.d(TAG, "Configured printer descriptor for $docName: ${printerDescriptor.name}")
    }

    override fun print(options: MediaDescriptor) {
        val opts = PrintOptions.fromPropertyValues(options)
        notifyListeners(PrintJobEvent(PrintableState.JOB_STARTED, this))
        Log.d(TAG, "Printing $docName (${contentPages.size} pages) to ${opts.printerName}, range=${opts.pages}, copies=${opts.copyCount}")
        notifyListeners(PrintJobEvent(PrintableState.JOB_SPOOLED, this))
        notifyListeners(PrintJobEvent(PrintableState.JOB_COMPLETED, this))
    }

    override fun getPagePrintSettings(): MediaDescriptor = pagePrintSettings.toPropertyValues()

    override fun setPagePrintSettings(settings: MediaDescriptor) {
        for (p in settings) {
            when (p.name) {
                "PageRows" -> pagePrintSettings.pageRows = (p.value as? Number)?.toShort() ?: 1
                "PageColumns" -> pagePrintSettings.pageColumns = (p.value as? Number)?.toShort() ?: 1
                "IsLandscape" -> pagePrintSettings.isLandscape = p.value as? Boolean ?: false
            }
        }
        Log.d(TAG, "Updated PagePrintSettings: cols=${pagePrintSettings.pageColumns}, landscape=${pagePrintSettings.isLandscape}")
    }

    override fun printPages(options: MediaDescriptor) {
        print(options)
    }

    override fun addPrintJobListener(listener: XPrintJobListener) {
        listeners.add(listener)
    }

    override fun removePrintJobListener(listener: XPrintJobListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(event: PrintJobEvent) {
        listeners.toList().forEach { it.printJobEvent(event) }
    }
}

/**
 * Android PrintDocumentAdapter that converts Papirus document content into native PDF pages
 * and renders them to Android PrintManager.
 */
class PapirusPrintDocumentAdapter(
    private val context: Context,
    private val jobName: String,
    private val docTitle: String,
    private val pageContents: List<String>,
    private val isLandscape: Boolean = false
) : PrintDocumentAdapter() {

    private var pdfDocument: PdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val pageCount = if (pageContents.isEmpty()) 1 else pageContents.size
        val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pageCount)
            .build()

        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        pdfDocument = PdfDocument()
        val totalPages = if (pageContents.isEmpty()) 1 else pageContents.size

        val pageWidth = if (isLandscape) 792 else 612 // Letter points (72 dpi)
        val pageHeight = if (isLandscape) 612 else 792

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        val bodyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
        }

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
        }

        for (i in 0 until totalPages) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                pdfDocument?.close()
                return
            }

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val page = pdfDocument?.startPage(pageInfo)
            val canvas = page?.canvas

            canvas?.let { c ->
                c.drawColor(Color.WHITE)

                // Draw Header
                c.drawText(docTitle, 36f, 40f, titlePaint)
                c.drawLine(36f, 50f, (pageWidth - 36).toFloat(), 50f, titlePaint)

                // Draw Body Content
                val content = pageContents.getOrElse(i) { "Empty Page Content" }
                val lines = content.split("\n")
                var y = 80f
                for (line in lines) {
                    if (y > pageHeight - 60) break
                    c.drawText(line, 36f, y, bodyPaint)
                    y += 18f
                }

                // Draw Footer
                val footerText = "Papirus Office - Page ${i + 1} of $totalPages"
                c.drawText(footerText, 36f, (pageHeight - 20).toFloat(), footerPaint)
            }

            pdfDocument?.finishPage(page)
        }

        try {
            destination?.fileDescriptor?.let { fd ->
                FileOutputStream(fd).use { out ->
                    pdfDocument?.writeTo(out)
                }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            Log.e("PapirusPrintAdapter", "Error writing PDF: ${e.localizedMessage}", e)
            callback?.onWriteFailed(e.localizedMessage)
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }
    }
}
