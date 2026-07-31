package com.makerandreas.papirusoffice.data

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface StorageProvider {
    suspend fun read(uri: String): ByteArray
    suspend fun write(uri: String, data: ByteArray): Boolean
    fun canHandle(uri: String): Boolean
}

class SafStorageProvider(private val context: Context) : StorageProvider {
    override suspend fun read(uri: String): ByteArray = withContext(Dispatchers.IO) {
        PapirusLogger.d("SafStorageProvider", "Reading via SAF URI: $uri")
        try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() } ?: ByteArray(0)
        } catch (e: Exception) {
            PapirusLogger.e("SafStorageProvider", "Failed to read SAF URI: $uri", e)
            ByteArray(0)
        }
    }

    override suspend fun write(uri: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        PapirusLogger.d("SafStorageProvider", "Writing via SAF URI: $uri, size=${data.size} bytes")
        try {
            context.contentResolver.openOutputStream(Uri.parse(uri), "rwt")?.use {
                it.write(data)
                true
            } ?: false
        } catch (e: Exception) {
            PapirusLogger.e("SafStorageProvider", "Failed to write SAF URI: $uri", e)
            false
        }
    }

    override fun canHandle(uri: String): Boolean {
        return uri.startsWith("content://")
    }
}

class AssetStorageProvider(private val context: Context) : StorageProvider {
    override suspend fun read(uri: String): ByteArray = withContext(Dispatchers.IO) {
        val assetPath = uri.removePrefix("asset://")
        PapirusLogger.d("AssetStorageProvider", "Reading asset: $assetPath")
        try {
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            PapirusLogger.e("AssetStorageProvider", "Failed to read asset: $assetPath", e)
            ByteArray(0)
        }
    }

    override suspend fun write(uri: String, data: ByteArray): Boolean {
        PapirusLogger.w("AssetStorageProvider", "Assets are read-only! Cannot write to: $uri")
        return false
    }

    override fun canHandle(uri: String): Boolean {
        return uri.startsWith("asset://")
    }
}

class MemoryStorageProvider : StorageProvider {
    private val memoryCache = mutableMapOf<String, ByteArray>()

    override suspend fun read(uri: String): ByteArray = withContext(Dispatchers.IO) {
        PapirusLogger.d("MemoryStorageProvider", "Reading from memory cache: $uri")
        memoryCache[uri] ?: ByteArray(0)
    }

    override suspend fun write(uri: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        PapirusLogger.d("MemoryStorageProvider", "Writing to memory cache: $uri, size=${data.size}")
        memoryCache[uri] = data
        true
    }

    override fun canHandle(uri: String): Boolean {
        return uri.startsWith("memory://")
    }
}

class CloudStorageProvider : StorageProvider {
    private val simulatedCloudStore = mutableMapOf<String, ByteArray>()

    override suspend fun read(uri: String): ByteArray = withContext(Dispatchers.IO) {
        val path = uri.removePrefix("cloud://")
        PapirusLogger.d("CloudStorageProvider", "Downloading from Cloud: $path")
        simulatedCloudStore[uri] ?: ByteArray(0)
    }

    override suspend fun write(uri: String, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val path = uri.removePrefix("cloud://")
        PapirusLogger.d("CloudStorageProvider", "Uploading to Cloud: $path, size=${data.size}")
        simulatedCloudStore[uri] = data
        true
    }

    override fun canHandle(uri: String): Boolean {
        return uri.startsWith("cloud://")
    }
}

class StorageManager private constructor(private val context: Context) {
    private val providers = listOf(
        SafStorageProvider(context),
        AssetStorageProvider(context),
        MemoryStorageProvider(),
        CloudStorageProvider()
    )

    suspend fun readDocument(uri: String): ByteArray {
        val provider = providers.firstOrNull { it.canHandle(uri) }
        return provider?.read(uri) ?: ByteArray(0)
    }

    suspend fun writeDocument(uri: String, data: ByteArray): Boolean {
        val provider = providers.firstOrNull { it.canHandle(uri) }
        return provider?.write(uri, data) ?: false
    }

    companion object {
        @Volatile
        private var instance: StorageManager? = null

        fun getInstance(context: Context): StorageManager {
            return instance ?: synchronized(this) {
                instance ?: StorageManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
