package com.example.modules.pagella

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.modules.inky.InkyPdfExporter
import com.makerandreas.papirusoffice.data.DocxDocumentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PagellaPdfCreator {
    private const val TAG = "PagellaPdfCreator"
    private const val MAX_BITMAP_DIMENSION = 2048

    private val SUPPORTED_DOCUMENT_EXTENSIONS = setOf(
        "odt", "ott", "ods", "ots", "odp", "otp",
        "docx", "doc", "xlsx", "xls", "pptx", "ppt"
    )

    fun isSupportedOfficeDocumentName(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ext in SUPPORTED_DOCUMENT_EXTENSIONS
    }

    fun resolveDisplayName(context: Context, uri: Uri, fallback: String): String {
        return try {
            var resolved: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1 && cursor.moveToFirst()) {
                    resolved = cursor.getString(idx)
                }
            }
            resolved?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun sanitizeBaseName(raw: String, defaultBase: String): String {
        val withoutPath = raw.substringAfterLast('/').substringAfterLast('\\').trim()
        val withoutExt = withoutPath.substringBeforeLast('.').ifBlank { withoutPath }
        val clean = withoutExt.replace("[^A-Za-z0-9 _.,+()\\[\\]-]".toRegex(), "_").take(80).trim()
        return clean.ifBlank { defaultBase }
    }

    private fun outputPdfFile(context: Context, baseName: String): File {
        val dir = File(context.filesDir, "opened_docs").apply { mkdirs() }
        val safeBase = sanitizeBaseName(baseName, "Pagella_Document")
        return File(dir, "$safeBase.pdf")
    }

    suspend fun createPdfFromImageUri(
        context: Context,
        imageUri: Uri,
        preferredBaseName: String? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val rawName = preferredBaseName ?: resolveDisplayName(context, imageUri, "Image_Import.jpg")
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            }
            val bitmap = if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, MAX_BITMAP_DIMENSION)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOpts)
                }
            } else {
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } ?: return@withContext null

            try {
                createPdfFromBitmap(context, bitmap, rawName)
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PDF from image URI", e)
            null
        }
    }

    suspend fun createPdfFromImageFile(
        context: Context,
        imageFile: File,
        preferredBaseName: String? = null
    ): File? = withContext(Dispatchers.IO) {
        if (!imageFile.exists()) return@withContext null
        try {
            val rawName = preferredBaseName ?: imageFile.name
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, boundsOpts)
            val bitmap = if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                val sampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, MAX_BITMAP_DIMENSION)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeFile(imageFile.absolutePath, decodeOpts)
            } else {
                BitmapFactory.decodeFile(imageFile.absolutePath)
            } ?: return@withContext null

            try {
                createPdfFromBitmap(context, bitmap, rawName)
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PDF from image file", e)
            null
        }
    }

    fun createPdfFromBitmap(
        context: Context,
        bitmap: Bitmap,
        baseName: String
    ): File? {
        val targetFile = outputPdfFile(context, baseName)
        var pdfDocument: PdfDocument? = null
        val writtenByNative = try {
            pdfDocument = PdfDocument()
            val isLandscape = bitmap.width > bitmap.height
            val pageWidth = if (isLandscape) 842 else 595
            val pageHeight = if (isLandscape) 595 else 842
            val margin = 36f

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val availWidth = pageWidth - margin * 2f
            val availHeight = pageHeight - margin * 2f
            val scale = minOf(
                availWidth / bitmap.width.toFloat().coerceAtLeast(1f),
                availHeight / bitmap.height.toFloat().coerceAtLeast(1f)
            )
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val left = (pageWidth - drawWidth) / 2f
            val top = (pageHeight - drawHeight) / 2f
            val destRect = RectF(left, top, left + drawWidth, top + drawHeight)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, null, destRect, paint)
            pdfDocument.finishPage(page)

            FileOutputStream(targetFile).use { out ->
                pdfDocument.writeTo(out)
            }
            targetFile.exists() && targetFile.length() > 0L
        } catch (e: Exception) {
            Log.w(TAG, "Native PdfDocument unavailable, using PDF 1.4 fallback writer", e)
            false
        } finally {
            try {
                pdfDocument?.close()
            } catch (_: Exception) {
            }
        }

        if (writtenByNative) {
            return targetFile
        }
        return if (writeFallbackImagePdf(bitmap, targetFile)) targetFile else null
    }

    suspend fun convertDocumentToPdf(
        context: Context,
        documentFile: File,
        preferredBaseName: String? = null
    ): File? = withContext(Dispatchers.IO) {
        if (!documentFile.exists()) return@withContext null
        try {
            val baseName = sanitizeBaseName(preferredBaseName ?: documentFile.name, "Converted_Document")
            val targetFile = outputPdfFile(context, baseName)
            if (com.example.core.jni.OfficeEngineClient.convertToPdf(context, documentFile, targetFile)) {
                return@withContext targetFile
            }
            val parser = DocxDocumentParser(context)
            val parseResult = parser.parseDocument(documentFile)
            if (parseResult.parsedDocument?.isParsingFailed == true) {
                return@withContext null
            }
            val bodyText = parseResult.text.ifBlank { baseName }
            val exported = try {
                FileOutputStream(targetFile).use { out ->
                    InkyPdfExporter.exportToPdf(
                        context = context,
                        docTitle = baseName,
                        bodyText = bodyText,
                        outputStream = out
                    )
                }
            } catch (_: Exception) {
                false
            }
            if (exported && targetFile.exists() && targetFile.length() > 0L) {
                targetFile
            } else if (writeFallbackTextPdf(baseName, bodyText, targetFile)) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert document to PDF", e)
            null
        }
    }

    private fun writeFallbackImagePdf(bitmap: Bitmap, targetFile: File): Boolean {
        return try {
            val jpegStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, jpegStream)
            val jpegBytes = jpegStream.toByteArray()

            val imgW = bitmap.width.coerceAtLeast(1)
            val imgH = bitmap.height.coerceAtLeast(1)
            val isLandscape = imgW > imgH
            val pageWidth = if (isLandscape) 842 else 595
            val pageHeight = if (isLandscape) 595 else 842
            val margin = 36f
            val availWidth = pageWidth - margin * 2f
            val availHeight = pageHeight - margin * 2f
            val scale = minOf(availWidth / imgW.toFloat(), availHeight / imgH.toFloat())
            val drawW = (imgW * scale).toInt().coerceAtLeast(1)
            val drawH = (imgH * scale).toInt().coerceAtLeast(1)
            val left = ((pageWidth - drawW) / 2f).toInt()
            val bottom = ((pageHeight - drawH) / 2f).toInt()

            val contentAscii = "q\n$drawW 0 0 $drawH $left $bottom cm\n/Im0 Do\nQ\n"
                .toByteArray(Charsets.US_ASCII)

            FileOutputStream(targetFile).use { out ->
                val offsets = IntArray(6)
                var pos = 0
                fun writeAscii(s: String) {
                    val b = s.toByteArray(Charsets.US_ASCII)
                    out.write(b)
                    pos += b.size
                }
                fun writeBytes(b: ByteArray) {
                    out.write(b)
                    pos += b.size
                }

                writeAscii("%PDF-1.4\n")
                offsets[1] = pos
                writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
                offsets[2] = pos
                writeAscii("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
                offsets[3] = pos
                writeAscii(
                    "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] " +
                        "/Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"
                )
                offsets[4] = pos
                writeAscii(
                    "4 0 obj\n<< /Type /XObject /Subtype /Image /Width $imgW /Height $imgH " +
                        "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.size} >>\nstream\n"
                )
                writeBytes(jpegBytes)
                writeAscii("\nendstream\nendobj\n")
                offsets[5] = pos
                writeAscii("5 0 obj\n<< /Length ${contentAscii.size} >>\nstream\n")
                writeBytes(contentAscii)
                writeAscii("endstream\nendobj\n")

                val xrefPos = pos
                writeAscii("xref\n0 6\n0000000000 65535 f \n")
                for (i in 1..5) {
                    writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
                }
                writeAscii("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
            }
            targetFile.exists() && targetFile.length() > 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write fallback image PDF", e)
            false
        }
    }

    private fun escapePdfText(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when {
                ch == '\\' -> sb.append("\\\\")
                ch == '(' -> sb.append("\\(")
                ch == ')' -> sb.append("\\)")
                ch.code in 32..126 -> sb.append(ch)
                else -> sb.append(' ')
            }
        }
        return sb.toString()
    }

    private fun writeFallbackTextPdf(docTitle: String, bodyText: String, targetFile: File): Boolean {
        return try {
            val pageWidth = 612
            val pageHeight = 792
            val lines = bodyText.lines().flatMap { rawLine ->
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) listOf("") else trimmed.chunked(80)
            }.take(38)

            val streamBuilder = StringBuilder()
            streamBuilder.append("BT\n/F1 16 Tf\n54 730 Td\n(${escapePdfText(docTitle)}) Tj\n")
            streamBuilder.append("/F1 11 Tf\n0 -24 Td\n")
            for (line in lines) {
                streamBuilder.append("(${escapePdfText(line)}) Tj\n0 -16 Td\n")
            }
            streamBuilder.append("ET\n")
            val contentBytes = streamBuilder.toString().toByteArray(Charsets.US_ASCII)

            FileOutputStream(targetFile).use { out ->
                val offsets = IntArray(6)
                var pos = 0
                fun writeAscii(s: String) {
                    val b = s.toByteArray(Charsets.US_ASCII)
                    out.write(b)
                    pos += b.size
                }
                fun writeBytes(b: ByteArray) {
                    out.write(b)
                    pos += b.size
                }

                writeAscii("%PDF-1.4\n")
                offsets[1] = pos
                writeAscii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
                offsets[2] = pos
                writeAscii("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
                offsets[3] = pos
                writeAscii(
                    "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] " +
                        "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n"
                )
                offsets[4] = pos
                writeAscii("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")
                offsets[5] = pos
                writeAscii("5 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n")
                writeBytes(contentBytes)
                writeAscii("endstream\nendobj\n")

                val xrefPos = pos
                writeAscii("xref\n0 6\n0000000000 65535 f \n")
                for (i in 1..5) {
                    writeAscii(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
                }
                writeAscii("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
            }
            targetFile.exists() && targetFile.length() > 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write fallback text PDF", e)
            false
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        while (width / inSampleSize > maxDimension || height / inSampleSize > maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
