package com.makerandreas.papirusoffice.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Durable copies of opened documents live under [Context.getFilesDir], not
 * [Context.getCacheDir]. Cache is trimmed on low-memory (Realme C3 / LMK) and
 * must never be the source of truth for session restore.
 */
object OpenedDocumentStore {
    const val DIR_NAME = "opened"
    const val MAX_INCOMING_FILE_BYTES = 250L * 1024 * 1024

    fun openedDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sanitizeFileName(raw: String?): String {
        val base = raw?.substringAfterLast('/')?.substringAfterLast('\\')?.trim()
            .orEmpty().ifEmpty { "document.odt" }
        val safe = base.replace("[^A-Za-z0-9 _.,+()\\[\\]-]".toRegex(), "_").take(120)
        val withExt = if (safe.contains('.')) safe else "$safe.odt"
        return withExt.ifBlank { "document.odt" }
    }

    fun persistFromStream(
        context: Context,
        displayName: String,
        copy: (OutputStream) -> Unit
    ): File {
        val dir = openedDir(context)
        val target = uniqueFile(dir, sanitizeFileName(displayName))
        FileOutputStream(target).use { output ->
            copy(output)
            output.flush()
            output.fd.sync()
        }
        return target
    }

    fun persistFromUri(context: Context, uri: Uri, displayName: String): File {
        tryTakePersistablePermission(context, uri)
        return persistFromStream(context, displayName) { output ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                copyCapped(input, output, displayName)
            } ?: throw java.io.IOException("Unable to open input stream for: $uri")
        }
    }

    fun allocateFile(context: Context, displayName: String): File {
        return uniqueFile(openedDir(context), sanitizeFileName(displayName))
    }

    fun persistFromFile(context: Context, source: File, displayName: String = source.name): File {
        if (!source.exists()) throw java.io.IOException("Source file missing: ${source.absolutePath}")
        val dir = openedDir(context)
        if (source.canonicalFile.parentFile?.canonicalPath == dir.canonicalPath) {
            return source
        }
        return persistFromStream(context, displayName) { output ->
            source.inputStream().use { input -> copyCapped(input, output, displayName) }
        }
    }

    fun tryTakePersistablePermission(context: Context, uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // SAF providers are not required to offer persistable grants.
            }
        } catch (_: Exception) {
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        val candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val stem = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) ".${name.substringAfterLast('.')}" else ""
        var i = 1
        while (i < 1000) {
            val next = File(dir, "${stem}_$i$ext")
            if (!next.exists()) return next
            i++
        }
        return File(dir, "${stem}_${System.currentTimeMillis()}$ext")
    }

    private fun copyCapped(input: InputStream, output: OutputStream, displayName: String) {
        val buffer = ByteArray(8192)
        var total = 0L
        val limit = MAX_INCOMING_FILE_BYTES
        while (true) {
            val n = input.read(buffer)
            if (n == -1) break
            total += n
            if (total > limit) {
                throw java.io.IOException(
                    "File exceeds ${limit / 1024 / 1024} MB limit: $displayName"
                )
            }
            output.write(buffer, 0, n)
        }
    }
}
