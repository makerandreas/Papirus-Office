package com.makerandreas.papirusoffice.data

import java.io.File

object DocumentImages {
    const val DEFAULT_WIDTH_UNITS = 300f
    const val DEFAULT_HEIGHT_UNITS = 200f
    const val MAX_DECODE_EDGE_PX = 2048

    data class Box(val widthUnits: Float, val heightUnits: Float)

    data class DecodeTarget(val file: File, val box: Box)

    fun box(w: Float, h: Float): Box =
        if (w <= 0f || h <= 0f) Box(DEFAULT_WIDTH_UNITS, DEFAULT_HEIGHT_UNITS) else Box(w, h)

    fun decodeSizePx(box: Box, density: Float): Pair<Int, Int> {
        val w = (box.widthUnits * density).toInt().coerceIn(1, MAX_DECODE_EDGE_PX)
        val h = (box.heightUnits * density).toInt().coerceIn(1, MAX_DECODE_EDGE_PX)
        return w to h
    }

    fun cacheKey(file: File, size: Pair<Int, Int>): String =
        "${file.absolutePath}_${file.lastModified()}_${size.first}x${size.second}"

    fun resolve(own: File?, path: String?, store: Map<String, File>): File? {
        if (own != null && own.exists()) return own
        if (path == null) return null
        val clean = path.removePrefix("./").lowercase()
        val name = File(path).name.lowercase()
        return store[clean] ?: store[name] ?: store.entries.firstOrNull { it.key.lowercase().endsWith(name) }?.value
    }

    fun predecodePageWindow(pageHeights: List<Float>, gapDp: Float, screenHeightDp: Float): IntRange {
        if (pageHeights.isEmpty()) return IntRange.EMPTY
        var accumulated = 0f
        var endIndex = 0
        for (i in pageHeights.indices) {
            accumulated += pageHeights[i] + gapDp
            endIndex = i
            if (accumulated >= screenHeightDp) break
        }
        return 0..endIndex
    }

    fun predecodeTargets(pages: List<PlacedPage>, window: IntRange, extractedImages: Map<String, File>): List<DecodeTarget> {
        val targets = mutableListOf<DecodeTarget>()
        val clampedWindow = window.first.coerceAtLeast(0)..window.last.coerceAtMost(pages.size - 1)
        for (i in clampedWindow) {
            val page = pages[i]
            for (node in page.nodes) {
                val elem = node.element
                if (elem is OfficeImage) {
                    val file = resolve(elem.file, elem.imagePath, extractedImages)
                    if (file != null) {
                        targets.add(DecodeTarget(file, box(elem.widthDp, elem.heightDp)))
                    }
                } else if (elem is OfficeDocElement.ImageElement) {
                    val file = resolve(elem.image.file, elem.image.imagePath, extractedImages)
                    if (file != null) {
                        targets.add(DecodeTarget(file, box(elem.image.widthDp, elem.image.heightDp)))
                    }
                }
            }
        }
        return targets
    }
}
