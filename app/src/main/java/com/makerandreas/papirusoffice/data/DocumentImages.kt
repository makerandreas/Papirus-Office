package com.makerandreas.papirusoffice.data

import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Plan 6C (F-3): the pure half of image presentation. Everything here is
 * free of Compose and Coil so the sizing, lookup and predecode window can be
 * unit-tested; `DocxEmbeddedImage` and `ImagePredecoder` apply it.
 */
object DocumentImages {

    /** Box the paginator reserves when a file declares no extent (layout units). */
    const val DEFAULT_WIDTH_UNITS = 200f
    const val DEFAULT_HEIGHT_UNITS = 150f

    /**
     * Decode resolution per layout unit, times density. Page sheets on phones
     * are drawn below 1 dp per unit, so 1.5 keeps images sharp up to roughly
     * 150-200 % zoom without decoding a new bitmap for every zoom step.
     */
    const val DECODE_SCALE = 1.5f

    /** Longest decoded edge in pixels, whatever the declared extent says. */
    const val MAX_DECODE_EDGE_PX = 2048

    data class Box(val widthUnits: Float, val heightUnits: Float)

    /** The reserved box: the declared extent, or the paginator's default. */
    fun box(widthUnits: Float, heightUnits: Float): Box = Box(
        widthUnits = if (widthUnits > 0f) widthUnits else DEFAULT_WIDTH_UNITS,
        heightUnits = if (heightUnits > 0f) heightUnits else DEFAULT_HEIGHT_UNITS
    )

    /**
     * Decode target in pixels for a box. It depends only on the declared
     * extent and density, never on zoom, so the predecoder and the on-page
     * image ask for the same bitmap and the second one is a memory-cache hit.
     */
    fun decodeSizePx(box: Box, density: Float): Pair<Int, Int> {
        var w = box.widthUnits * density * DECODE_SCALE
        var h = box.heightUnits * density * DECODE_SCALE
        val longest = max(w, h)
        if (longest > MAX_DECODE_EDGE_PX) {
            val shrink = MAX_DECODE_EDGE_PX / longest
            w *= shrink
            h *= shrink
        }
        return w.roundToInt().coerceAtLeast(1) to h.roundToInt().coerceAtLeast(1)
    }

    /**
     * Memory-cache key: the stored file plus its size and timestamp, so a
     * re-extracted file (Plan 6B recovery) never reuses a stale bitmap.
     */
    fun cacheKey(file: File, decodeSize: Pair<Int, Int>): String =
        "papirus-media:${file.absolutePath}:${file.length()}:${file.lastModified()}:" +
            "${decodeSize.first}x${decodeSize.second}"

    /**
     * Finds the stored file for an image reference. The element's own file
     * wins while it exists; otherwise the media store's alias map is asked by
     * package path, the path without a leading `./`, the basename, and a
     * case-insensitive basename. Returns null when the media is really gone,
     * so the caller shows the missing-image box instead of a wrong picture.
     */
    fun resolve(imageFile: File?, imagePath: String, extracted: Map<String, File>): File? {
        imageFile?.takeIf { it.isFile }?.let { return it }
        val path = imagePath.replace('\\', '/').removePrefix("./")
        val basename = path.substringAfterLast('/')
        val candidates = listOf(
            path,
            path.lowercase(Locale.ROOT),
            basename,
            basename.lowercase(Locale.ROOT)
        )
        for (key in candidates) {
            if (key.isBlank()) continue
            extracted[key]?.takeIf { it.isFile }?.let { return it }
        }
        return null
    }

    /**
     * Pages to predecode: every page whose sheet starts inside the first
     * screenful (from the top of the stack), plus the next page.
     */
    fun predecodePageWindow(sheetHeightsDp: List<Float>, gapDp: Float, viewportHeightDp: Float): IntRange {
        if (sheetHeightsDp.isEmpty()) return IntRange.EMPTY
        var top = 0f
        var lastVisible = 0
        for ((index, height) in sheetHeightsDp.withIndex()) {
            if (top >= viewportHeightDp) break
            lastVisible = index
            top += height + gapDp
        }
        return 0..minOf(lastVisible + 1, sheetHeightsDp.lastIndex)
    }

    data class DecodeTarget(val file: File, val box: Box)

    /** The resolvable images on [window]'s pages, in page order, each file once. */
    fun predecodeTargets(
        pages: List<PageLayout>,
        window: IntRange,
        extracted: Map<String, File>
    ): List<DecodeTarget> {
        val seen = HashSet<String>()
        val out = mutableListOf<DecodeTarget>()
        for (index in window) {
            val page = pages.getOrNull(index) ?: continue
            for (layout in page.elements) {
                val image = when (val element = layout.element) {
                    is OfficeImage -> element
                    is OfficeDocElement.ImageElement -> element.image
                    else -> null
                } ?: continue
                val file = resolve(image.imageFile, image.imagePath, extracted) ?: continue
                if (seen.add(file.absolutePath)) {
                    out += DecodeTarget(file, box(image.widthDp, image.heightDp))
                }
            }
        }
        return out
    }
}
