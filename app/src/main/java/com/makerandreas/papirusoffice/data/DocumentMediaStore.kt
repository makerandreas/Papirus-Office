package com.makerandreas.papirusoffice.data

import com.makerandreas.papirusoffice.data.util.ZipSafe
import com.makerandreas.papirusoffice.data.util.copyCappedTo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipFile

/** Durable, bounded storage for media extracted from office packages. */
class DocumentMediaStore(
    private val rootDir: File,
    private val limits: Limits = Limits()
) {
    data class Limits(
        val maxImageBytes: Long = ZipSafe.MAX_IMAGE_BYTES,
        val maxImageCount: Int = ZipSafe.MAX_IMAGE_COUNT,
        val maxDocumentMediaBytes: Long = ZipSafe.MAX_DOCUMENT_MEDIA_BYTES,
        val maxStoreBytes: Long = ZipSafe.MAX_STORED_MEDIA_BYTES,
        val maxZipEntries: Int = ZipSafe.MAX_ZIP_ENTRIES.toInt()
    ) {
        init {
            require(maxImageBytes > 0)
            require(maxImageCount > 0)
            require(maxDocumentMediaBytes > 0)
            require(maxStoreBytes > 0)
            require(maxZipEntries > 0)
        }
    }

    private data class SourceIdentity(
        val canonicalPath: String,
        val length: Long,
        val lastModified: Long,
        val key: String
    )

    private data class StoredEntry(
        val packagePath: String,
        val fileName: String,
        val size: Long,
        val sha256: String
    )

    private data class Manifest(
        val identity: SourceIdentity,
        val entries: MutableMap<String, StoredEntry>
    )

    private data class ResolvedEntry(val packagePath: String, val file: File, val size: Long)

    private val lock = storeLocks.computeIfAbsent(rootDir.absolutePath) { Any() }

    /** Returns package-path and safe alias keys mapped to durable media files. */
    fun extractImages(source: File): Map<String, File> = synchronized(lock) {
        if (!source.isFile) emptyMap() else extractImagesLocked(source)
    }

    internal fun storedBytes(): Long = synchronized(lock) {
        val children = rootDir.listFiles().orEmpty()
        children.filter { it.isDirectory }.sumOf(::directoryMediaBytes) +
            children.filter { it.isFile && isStagingFile(it) }.sumOf { it.length() }
    }

    private fun extractImagesLocked(source: File): Map<String, File> {
        if (!rootDir.exists() && !rootDir.mkdirs()) throw IOException("Cannot create media store")
        removeStagingFiles()

        val identity = sourceIdentity(source)
        val sourceDir = File(rootDir, identity.key)
        removeStaleVersions(identity)

        var manifest = readManifest(File(sourceDir, MANIFEST_NAME), identity)
        if (manifest == null) {
            sourceDir.deleteRecursively()
            if (!sourceDir.mkdirs()) throw IOException("Cannot create document media directory")
            manifest = Manifest(identity, mutableMapOf())
        }

        val imagesDir = File(sourceDir, IMAGES_DIR)
        if (!imagesDir.exists() && !imagesDir.mkdirs()) throw IOException("Cannot create image directory")
        removeUntrackedFiles(imagesDir, manifest.entries.values.map { it.fileName }.toSet())
        if (!makeRoom(0L, sourceDir)) {
            sourceDir.deleteRecursively()
            return emptyMap()
        }

        val resolved = linkedMapOf<String, ResolvedEntry>()
        var documentBytes = 0L
        var mediaCount = 0
        val seenPaths = mutableSetOf<String>()

        ZipFile(source).use { zip ->
            val entries = zip.entries()
            var scannedEntries = 0
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                scannedEntries++
                if (scannedEntries > limits.maxZipEntries) break

                val path = normalizePackagePath(entry.name) ?: continue
                if (!entry.isDirectory && isMediaPath(path) && seenPaths.add(path)) {
                    if (mediaCount >= limits.maxImageCount) break
                    mediaCount++

                    val previous = manifest.entries[path]
                    val previousFile = previous?.let { File(imagesDir, it.fileName) }
                    val declaredSize = entry.size
                    val reusable = previous != null && previousFile?.isFile == true &&
                        previousFile.length() == previous.size &&
                        (declaredSize < 0 || declaredSize == previous.size)

                    if (reusable) {
                        if (previous!!.size <= limits.maxImageBytes &&
                            documentBytes + previous.size <= limits.maxDocumentMediaBytes) {
                            resolved[path] = ResolvedEntry(path, previousFile!!, previous.size)
                            documentBytes += previous.size
                            continue
                        }
                        manifest.entries.remove(path)
                        previousFile?.delete()
                    }

                    if (declaredSize == 0L || declaredSize > limits.maxImageBytes ||
                        (declaredSize >= 0 && documentBytes + declaredSize > limits.maxDocumentMediaBytes)) {
                        manifest.entries.remove(path)
                        previousFile?.delete()
                        continue
                    }

                    val entryLimit = minOf(
                        limits.maxImageBytes,
                        limits.maxDocumentMediaBytes - documentBytes
                    )
                    if (entryLimit <= 0L) break

                    val tempFile = File(rootDir, ".media-${identity.key}-${System.nanoTime()}.tmp")
                    val digest = MessageDigest.getInstance("SHA-256")
                    val size = try {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(tempFile).use { rawOutput ->
                                val digestOutput = DigestOutputStream(rawOutput, digest)
                                val copied = input.copyCappedTo(digestOutput, entryLimit)
                                digestOutput.flush()
                                rawOutput.fd.sync()
                                copied
                            }
                        }
                    } catch (_: Exception) {
                        tempFile.delete()
                        manifest.entries.remove(path)
                        previousFile?.delete()
                        continue
                    }

                    if (size <= 0L || size > limits.maxImageBytes || documentBytes + size > limits.maxDocumentMediaBytes) {
                        tempFile.delete()
                        manifest.entries.remove(path)
                        previousFile?.delete()
                        continue
                    }

                    val fileName = mediaFileName(path)
                    val destination = File(imagesDir, fileName)
                    if (!makeRoom(0L, sourceDir)) {
                        tempFile.delete()
                        manifest.entries.remove(path)
                        previousFile?.delete()
                        continue
                    }
                    destination.delete()
                    if (!tempFile.renameTo(destination)) {
                        tempFile.delete()
                        throw IOException("Cannot commit extracted media")
                    }

                    val stored = StoredEntry(path, fileName, size, digest.digest().toHex())
                    manifest.entries[path] = stored
                    resolved[path] = ResolvedEntry(path, destination, size)
                    documentBytes += size
                }
            }
        }

        manifest.entries.keys.retainAll(resolved.keys)
        removeUntrackedFiles(imagesDir, manifest.entries.values.map { it.fileName }.toSet())
        writeManifest(File(sourceDir, MANIFEST_NAME), manifest)
        sourceDir.setLastModified(System.currentTimeMillis())
        return buildAliases(resolved.values.toList())
    }

    private fun makeRoom(requiredBytes: Long, protectedDir: File): Boolean {
        if (requiredBytes > limits.maxStoreBytes) return false
        var total = storedBytes()
        if (total + requiredBytes <= limits.maxStoreBytes) return true

        val candidates = rootDir.listFiles()
            ?.filter { it.isDirectory && it.canonicalFile != protectedDir.canonicalFile }
            ?.sortedBy { it.lastModified() }
            .orEmpty()
        for (candidate in candidates) {
            candidate.deleteRecursively()
            total = storedBytes()
            if (total + requiredBytes <= limits.maxStoreBytes) return true
        }
        return total + requiredBytes <= limits.maxStoreBytes
    }

    private fun removeStaleVersions(identity: SourceIdentity) {
        rootDir.listFiles()?.forEach { directory ->
            if (!directory.isDirectory || directory.name == identity.key) return@forEach
            val manifest = readManifest(File(directory, MANIFEST_NAME), expected = null)
            if (manifest?.identity?.canonicalPath == identity.canonicalPath) {
                directory.deleteRecursively()
            }
        }
    }

    private fun sourceIdentity(source: File): SourceIdentity {
        val canonicalPath = source.canonicalPath
        val length = source.length()
        val lastModified = source.lastModified()
        val keyMaterial = "$canonicalPath:$length:$lastModified".toByteArray(Charsets.UTF_8)
        val key = MessageDigest.getInstance("SHA-256").digest(keyMaterial).toHex()
        return SourceIdentity(canonicalPath, length, lastModified, key)
    }

    private fun readManifest(file: File, expected: SourceIdentity?): Manifest? {
        if (!file.isFile) return null
        return try {
            val properties = Properties().apply { FileInputStream(file).use { load(it) } }
            if (properties.getProperty("version") != MANIFEST_VERSION) return null
            val path = decode(properties.getProperty("source.path") ?: return null)
            val length = properties.getProperty("source.length")?.toLongOrNull() ?: return null
            val lastModified = properties.getProperty("source.modified")?.toLongOrNull() ?: return null
            val key = properties.getProperty("source.key") ?: return null
            val identity = SourceIdentity(path, length, lastModified, key)
            if (expected != null && identity != expected) return null

            val count = properties.getProperty("entry.count")?.toIntOrNull()?.coerceIn(0, limits.maxImageCount) ?: return null
            val entries = mutableMapOf<String, StoredEntry>()
            repeat(count) { index ->
                val prefix = "entry.$index."
                val entryPath = decode(properties.getProperty(prefix + "path") ?: return null)
                val fileName = properties.getProperty(prefix + "file") ?: return null
                if (!fileName.matches(Regex("[0-9a-f]{64}(?:\\.[a-z0-9]{1,10})?"))) return null
                val size = properties.getProperty(prefix + "size")?.toLongOrNull()?.takeIf { it in 1L..limits.maxImageBytes } ?: return null
                val hash = properties.getProperty(prefix + "sha256")?.takeIf { it.matches(Regex("[0-9a-f]{64}")) } ?: return null
                entries[entryPath] = StoredEntry(entryPath, fileName, size, hash)
            }
            Manifest(identity, entries)
        } catch (_: Exception) {
            null
        }
    }

    private fun writeManifest(file: File, manifest: Manifest) {
        val properties = Properties().apply {
            setProperty("version", MANIFEST_VERSION)
            setProperty("source.path", encode(manifest.identity.canonicalPath))
            setProperty("source.length", manifest.identity.length.toString())
            setProperty("source.modified", manifest.identity.lastModified.toString())
            setProperty("source.key", manifest.identity.key)
            setProperty("entry.count", manifest.entries.size.toString())
            manifest.entries.values.sortedBy { it.packagePath }.forEachIndexed { index, entry ->
                val prefix = "entry.$index."
                setProperty(prefix + "path", encode(entry.packagePath))
                setProperty(prefix + "file", entry.fileName)
                setProperty(prefix + "size", entry.size.toString())
                setProperty(prefix + "sha256", entry.sha256)
            }
        }
        val temp = File(file.parentFile, "$MANIFEST_NAME.tmp")
        FileOutputStream(temp).use { output ->
            properties.store(output, null)
            output.fd.sync()
        }
        if (file.exists() && !file.delete()) {
            temp.delete()
            throw IOException("Cannot replace media manifest")
        }
        if (!temp.renameTo(file)) {
            temp.delete()
            throw IOException("Cannot commit media manifest")
        }
    }

    private fun removeUntrackedFiles(imagesDir: File, knownFiles: Set<String>) {
        imagesDir.listFiles()?.forEach { file ->
            if (file.name !in knownFiles || file.name.endsWith(".tmp")) file.delete()
        }
    }

    private fun removeStagingFiles() {
        rootDir.listFiles()?.filter { it.isFile && isStagingFile(it) }?.forEach { it.delete() }
    }

    private fun isStagingFile(file: File): Boolean =
        file.name.startsWith(".media-") && file.name.endsWith(".tmp")

    private fun directoryMediaBytes(directory: File): Long {
        val images = File(directory, IMAGES_DIR)
        return images.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun buildAliases(entries: List<ResolvedEntry>): Map<String, File> {
        val aliases = linkedMapOf<String, File>()
        val conflicts = mutableSetOf<String>()
        val caseInsensitive = mutableMapOf<String, MutableSet<File>>()
        entries.forEach { entry ->
            aliasNames(entry.packagePath).forEach { alias ->
                val existing = aliases[alias]
                if (existing == null || existing == entry.file) {
                    aliases[alias] = entry.file
                } else {
                    aliases.remove(alias)
                    conflicts += alias
                }
                caseInsensitive.getOrPut(alias.lowercase(java.util.Locale.ROOT)) { linkedSetOf() }.add(entry.file)
            }
        }
        conflicts.forEach(aliases::remove)
        caseInsensitive.forEach { (alias, files) ->
            if (files.size == 1) aliases.putIfAbsent(alias, files.single())
        }
        return aliases
    }

    private fun aliasNames(path: String): Set<String> {
        val basename = path.substringAfterLast('/')
        val aliases = linkedSetOf(path, basename)
        if (path.startsWith("word/", ignoreCase = true)) aliases += path.substringAfter('/', path)
        return aliases.filter(String::isNotBlank).toSet()
    }

    private fun normalizePackagePath(raw: String): String? {
        val normalized = raw.replace('\\', '/').removePrefix("./")
        if (normalized.isBlank() || normalized.startsWith('/') || normalized.split('/').any { it == ".." }) return null
        return normalized
    }

    private fun isMediaPath(path: String): Boolean =
        path.startsWith("word/media/", ignoreCase = true) ||
            path.startsWith("pictures/", ignoreCase = true)

    private fun mediaFileName(packagePath: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(packagePath.toByteArray(Charsets.UTF_8)).toHex()
        val extension = packagePath.substringAfterLast('.', "")
            .lowercase(java.util.Locale.ROOT)
            .takeIf { it.matches(Regex("[a-z0-9]{1,10}")) }
        return if (extension == null) hash else "$hash.$extension"
    }

    private fun encode(value: String): String = value.toByteArray(Charsets.UTF_8).toHex()

    private fun decode(value: String): String {
        if (value.length % 2 != 0 || !value.matches(Regex("[0-9a-fA-F]*"))) throw IOException("Invalid manifest encoding")
        val bytes = ByteArray(value.length / 2) { index -> value.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
        return String(bytes, Charsets.UTF_8)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private val storeLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()
        private const val MANIFEST_NAME = "manifest.properties"
        private const val MANIFEST_VERSION = "1"
        private const val IMAGES_DIR = "images"
    }
}
