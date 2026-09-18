package com.makerandreas.papirusoffice.data.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

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
    const val MAX_ZIP_ENTRIES = 10_000

    /** Max whole-document bytes loaded into memory. */
    const val MAX_DOCUMENT_BYTES = 256L * 1024 * 1024

    /** Max bytes for a single embedded image extraction. */
    const val MAX_IMAGE_BYTES = 50L * 1024 * 1024

    /** Max embedded images extracted from one document. */
    const val MAX_IMAGE_COUNT = 200

    /** Max bytes for a downloaded template / font file. */
    const val MAX_DOWNLOAD_BYTES = 100L * 1024 * 1024
    const val MAX_FONT_BYTES = 30L * 1024 * 1024
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
