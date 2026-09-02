package com.example.modules.inky

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.util.Log
import java.io.OutputStream

/**
 * Highly compatible PDF Exporter for the Inky (Writer) module of Papirus Office.
 * Implements high-precision text auto-wrapping, heading formatting, headers/footers,
 * and multi-page pagination.
 */
object InkyPdfExporter {
    private const val TAG = "InkyPdfExporter"

    /**
     * Exports document title and body text to a PDF written to the provided output stream.
     * This avoids complex external library overhead and performs optimally even on low-RAM devices.
     */
    fun exportToPdf(
        context: Context,
        docTitle: String,
        bodyText: String,
        outputStream: OutputStream
    ): Boolean {
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()

            // Page dimensions in points (72 points = 1 inch, Letter size: 612 x 792)
            val pageWidth = 612 
            val pageHeight = 792 
            
            // Margins (approx 0.75 inch)
            val leftMargin = 54f 
            val rightMargin = 54f
            val topMargin = 72f  
            val bottomMargin = 72f
            
            val contentWidth = pageWidth - leftMargin - rightMargin

            // Paints
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = TextPaint().apply {
                color = Color.parseColor("#1B5E20") // Rich primary-like green or dark color
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.parseColor("#212121") // Eye-friendly off-black
                textSize = 11f
                isAntiAlias = true
            }

            val footerPaint = TextPaint().apply {
                color = Color.GRAY
                textSize = 8f
                isAntiAlias = true
            }

            // Split into paragraphs by blank lines
            val paragraphs = bodyText.split("\n\n")

            // Pagination tracking
            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Header builder for each page
            fun drawHeader(pageNum: Int) {
                canvas?.let { c ->
                    c.drawColor(Color.WHITE)
                    // Header text
                    c.drawText(docTitle, leftMargin, 40f, footerPaint)
                    val headerPaint = Paint().apply {
                        color = Color.parseColor("#E0E0E0")
                        strokeWidth = 1f
                    }
                    c.drawLine(leftMargin, 48f, pageWidth - rightMargin, 48f, headerPaint)
                }
            }

            // Footer builder for each page
            fun drawFooter(pageNum: Int) {
                canvas?.let { c ->
                    val footerText = "Papirus Inky • Page $pageNum"
                    c.drawText(footerText, leftMargin, pageHeight - 35f, footerPaint)
                    val dividerPaint = Paint().apply {
                        color = Color.parseColor("#E0E0E0")
                        strokeWidth = 0.5f
                    }
                    c.drawLine(leftMargin, pageHeight - 45f, pageWidth - rightMargin, pageHeight - 45f, dividerPaint)
                }
            }

            // Initialize first page header
            drawHeader(currentPageNumber)

            var y = topMargin

            // Helper to break a single long line of text into wrapped lines that fit the page width
            fun wrapText(text: String, paint: TextPaint, maxWidth: Float): List<String> {
                val words = text.split(" ")
                val wrappedLines = mutableListOf<String>()
                var currentLine = StringBuilder()

                for (word in words) {
                    if (word.isEmpty()) continue
                    val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                    val testWidth = paint.measureText(testLine)
                    if (testWidth <= maxWidth) {
                        currentLine.append(if (currentLine.isEmpty()) word else " $word")
                    } else {
                        if (currentLine.isNotEmpty()) {
                            wrappedLines.add(currentLine.toString())
                        }
                        currentLine = StringBuilder(word)
                    }
                }
                if (currentLine.isNotEmpty()) {
                    wrappedLines.add(currentLine.toString())
                }
                return wrappedLines
            }

            // Render Document Title on page 1 below the header
            canvas?.drawText(docTitle, leftMargin, y, titlePaint)
            y += 40f

            for (paragraph in paragraphs) {
                if (paragraph.trim().isEmpty()) {
                    y += 10f
                    continue
                }

                // Respect inner manual line breaks inside the paragraph
                val subLines = paragraph.split("\n")
                for (subLine in subLines) {
                    if (subLine.isBlank()) continue

                    // Simple logic to detect headings (short, bold/uppercase-like lines)
                    val isHeading = subLine.length < 80 && (subLine.uppercase() == subLine || subLine.startsWith("#"))
                    val displayLine = if (subLine.startsWith("#")) subLine.trimStart('#', ' ') else subLine
                    
                    val paintToUse = if (isHeading) subtitlePaint else bodyPaint
                    val lineHeight = if (isHeading) 22f else 16f
                    val paragraphSpacing = if (isHeading) 12f else 6f

                    val wrappedLines = wrapText(displayLine, paintToUse, contentWidth)
                    for (line in wrappedLines) {
                        // Check pagination height constraints
                        if (y + lineHeight > pageHeight - bottomMargin) {
                            // Finish current page
                            drawFooter(currentPageNumber)
                            pdfDocument.finishPage(page)

                            // Start next page
                            currentPageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            
                            // Setup next page header
                            drawHeader(currentPageNumber)
                            y = topMargin
                        }

                        canvas?.drawText(line, leftMargin, y, paintToUse)
                        y += lineHeight
                    }
                    y += paragraphSpacing
                }
                y += 10f // Space between paragraph blocks
            }

            // Finish the final page
            drawFooter(currentPageNumber)
            pdfDocument.finishPage(page)

            // Write PDF to output
            pdfDocument.writeTo(outputStream)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF", e)
            return false
        } finally {
            pdfDocument?.close()
        }
    }
}
