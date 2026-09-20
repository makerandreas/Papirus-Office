package com.makerandreas.papirusoffice.data.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

/**
 * Guarded IO helpers for parsing untrusted archives.
 *
 * Office documents are ZIP files from arbitrary sources; entries must never be
 * read without bounds, otherwise a hostile file (zip bomb) can OOM the app or
 * fill the disk. All document parsers must use [InputStream.readCappedBytes]
 * and [InputStream.copyCappedTo] instead of the unbounded stdlib
 * [InputStream.readBytes] / [InputStream.copyTo].
 */
object ZipSafe {
    /** Max bytes for a single ZIP entry (content.xml etc.). */
    const val MAX_ZIP_ENTRY_BYTES = 48L * 1024 * 1024

    /** Max entries scanned in a single archive (zip-bomb guard). */
    const val MAX_ZIP_ENTRIES = 10_000L

    /** Max whole-document bytes loaded into memory. */
    const val MAX_DOCUMENT_BYTES = 256L * 1024 * 1024

    /** Max bytes for a single embedded image extraction. */
    const val MAX_IMAGE_BYTES = 50L * 1024 * 1024

    /** Max embedded images extracted from one document. */
    const val MAX_IMAGE_COUNT = 200

    /** Max bytes for a downloaded template / font file. */
    const val MAX_DOWNLOAD_BYTES = 100L * 1024 * 1024
    const val MAX_FONT_BYTES = 30L * 1024 * 1024

    /**
     * Max worksheet rows materialized per XLSX sheet (Phase 6). Bounds the
     * parsed *model* (TableRow/Cell objects); the streamed raw XML is
     * separately bounded by [MAX_DOCUMENT_BYTES] via [ZipScanBudget].
     */
    const val MAX_XLSX_ROWS = 200_000

    /** Max spreadsheet column index (XFD, the OOXML spec limit). Guards the sparse-cell fill loop. */
    const val MAX_XLSX_COLUMN_INDEX = 16383
}

/**
 * Reads an [InputStream] fully with a hard byte cap.
 *
 * Use for ZIP entries as well as any other untrusted stream (SAF, assets,
 * network); a hostile source must never be read with unbounded stdlib
 * [InputStream.readBytes].
 *
 * @throws IOException if the stream exceeds [maxBytes].
 */
fun InputStream.readCappedBytes(maxBytes: Long = ZipSafe.MAX_ZIP_ENTRY_BYTES): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    var total = 0L
    while (true) {
        val n = read(buf)
        if (n == -1) break
        total += n
        if (total > maxBytes) {
            throw IOException("Zip entry exceeds ${maxBytes / 1024 / 1024} MB safety cap")
        }
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}

/**
 * Copies with a hard byte cap.
 * @throws IOException if more than [maxBytes] would be copied.
 */
fun InputStream.copyCappedTo(out: OutputStream, maxBytes: Long): Long {
    val buf = ByteArray(8192)
    var total = 0L
    while (true) {
        val n = read(buf)
        if (n == -1) break
        total += n
        if (total > maxBytes) {
            throw IOException("Stream exceeds ${maxBytes / 1024 / 1024} MB safety cap")
        }
        out.write(buf, 0, n)
    }
    return total
}

/**
 * Cross-entry scan budget for one archive pass (Phase 6 hardening).
 *
 * Per-entry caps ([InputStream.readCappedBytes]) stop single-entry bombs; the
 * budget additionally stops many-entries / many-total-bytes bombs across a
 * whole scan. Create one budget per scan pass and thread it through
 * [ZipInputStream.nextEntryBudgeted], [InputStream.readCappedBytes] and
 * [BudgetedInputStream].
 */
class ZipScanBudget(
    val maxEntries: Long = ZipSafe.MAX_ZIP_ENTRIES,
    val maxTotalBytes: Long = ZipSafe.MAX_DOCUMENT_BYTES
) {
    var entriesCharged: Long = 0L
        private set
    var bytesCharged: Long = 0L
        private set

    /** @throws IOException if the entry count cap is exceeded. */
    fun chargeEntry(entryName: String) {
        entriesCharged += 1
        if (entriesCharged > maxEntries) {
            throw IOException("Zip archive exceeds $maxEntries entries safety cap (at '$entryName')")
        }
    }

    /** @throws IOException if the total-bytes cap is exceeded. */
    fun chargeBytes(byteCount: Long) {
        if (byteCount <= 0) return
        bytesCharged += byteCount
        if (bytesCharged > maxTotalBytes) {
            throw IOException("Zip archive exceeds ${maxTotalBytes / 1024 / 1024} MB total safety cap")
        }
    }
}

/**
 * [ZipInputStream.nextEntry] with entry-count budgeting.
 * A null [budget] behaves exactly like plain [ZipInputStream.nextEntry].
 */
fun ZipInputStream.nextEntryBudgeted(budget: ZipScanBudget?): java.util.zip.ZipEntry? {
    val entry = nextEntry ?: return null
    budget?.chargeEntry(entry.name)
    return entry
}

/**
 * [InputStream.readCappedBytes] variant that additionally charges [budget].
 */
fun InputStream.readCappedBytes(maxBytes: Long, budget: ZipScanBudget?): ByteArray {
    val bytes = readCappedBytes(maxBytes)
    budget?.chargeBytes(bytes.size.toLong())
    return bytes
}

/**
 * Pass-through stream that charges every consumed byte to [budget].
 *
 * Use when a parser (e.g. XmlPullParser) consumes a ZIP entry as a stream
 * instead of a buffered [ByteArray], so streamed bytes stay inside the
 * archive's total-bytes budget. Does NOT close [budget]; closing this stream
 * closes the wrapped stream (callers parsing ZIP entries must still call
 * [ZipInputStream.closeEntry] themselves).
 */
class BudgetedInputStream(
    wrapped: InputStream,
    private val budget: ZipScanBudget?
) : java.io.FilterInputStream(wrapped) {

    override fun read(): Int {
        val byte = super.read()
        if (byte != -1) budget?.chargeBytes(1)
        return byte
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = super.read(b, off, len)
        if (n > 0) budget?.chargeBytes(n.toLong())
        return n
    }
}
