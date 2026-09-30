package com.example.modules.inky

import android.content.Context
import coil.imageLoader
import coil.memory.MemoryCache
import com.example.ui.components.documentImageRequest
import com.makerandreas.papirusoffice.data.DocumentImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Plan 6C (F-3): decodes the images of the first screenful plus one page
 * into Coil's memory cache on a background dispatcher, so they appear in the
 * frame after decode instead of after a separate on-page request.
 */
internal object ImagePredecoder {

    /** A slow or broken file must not hold the other images back for long. */
    private const val TIMEOUT_MS = 3_000L

    fun cacheKeys(targets: List<DocumentImages.DecodeTarget>, density: Float): List<String> =
        targets.map { DocumentImages.cacheKey(it.file, DocumentImages.decodeSizePx(it.box, density)) }

    /** Keys among [targets] that are not in the memory cache yet. */
    fun uncachedKeys(context: Context, targets: List<DocumentImages.DecodeTarget>, density: Float): Set<String> {
        val cache = context.imageLoader.memoryCache ?: return emptySet()
        return cacheKeys(targets, density).filterTo(LinkedHashSet()) { cache[MemoryCache.Key(it)] == null }
    }

    /** Decodes every target in parallel; returns when all finish or the timeout passes. */
    suspend fun predecode(context: Context, targets: List<DocumentImages.DecodeTarget>, density: Float) {
        if (targets.isEmpty()) return
        val loader = context.imageLoader
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(TIMEOUT_MS) {
                coroutineScope {
                    targets.map { target ->
                        async {
                            val size = DocumentImages.decodeSizePx(target.box, density)
                            loader.execute(documentImageRequest(context, target.file, size))
                        }
                    }.awaitAll()
                }
            }
        }
    }
}
