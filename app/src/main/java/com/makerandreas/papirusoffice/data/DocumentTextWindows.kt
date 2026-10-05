package com.makerandreas.papirusoffice.data

import androidx.compose.ui.text.TextRange

/**
 * Window of the flat editor string owned by one textual document element.
 * [start] is inclusive, [end] is exclusive, both offsets into the text joined
 * with [DocumentTextProjection.BLOCK_SEPARATOR] that the merger also consumes.
 */
data class DocumentTextWindow(
    val elementIndex: Int,
    val text: String,
    val start: Int,
    val end: Int
)

/**
 * Bidirectional map between the shared editor-text projection and its body
 * elements. The source blocks use [DocumentTextProjection] in document order,
 * so window N always covers block N; structural elements own no text.
 */
object DocumentTextWindows {

    fun compute(elements: List<OfficeElement>, globalText: String): Map<Int, DocumentTextWindow> {
        val sourceBlocks = elements.mapNotNull { DocumentTextProjection.elementText(it) }
        // Consecutive hard newlines inside a paragraph are not paragraph separators.
        val blocks = if (sourceBlocks.joinToString(DocumentTextProjection.BLOCK_SEPARATOR) == globalText) {
            sourceBlocks
        } else {
            globalText.split(DocumentTextProjection.BLOCK_SEPARATOR)
        }
        val windows = LinkedHashMap<Int, DocumentTextWindow>(blocks.size)
        var blockIndex = 0
        var offset = 0
        elements.forEachIndexed { elementIndex, element ->
            if (!DocumentTextProjection.isTextual(element)) return@forEachIndexed
            if (blockIndex >= blocks.size) return@forEachIndexed
            val text = blocks[blockIndex]
            windows[elementIndex] = DocumentTextWindow(elementIndex, text, offset, offset + text.length)
            offset += text.length + DocumentTextProjection.BLOCK_SEPARATOR.length
            blockIndex++
        }
        return windows
    }

    /** Maps a local field value (window-relative) back into the global string. */
    fun applyLocalEdit(globalText: String, window: DocumentTextWindow, newLocalText: String): String {
        return globalText.substring(0, window.start) + newLocalText + globalText.substring(window.end)
    }

    /** Element whose window contains [offset]; boundary offsets belong to the earlier window. */
    fun elementForOffset(windows: Map<Int, DocumentTextWindow>, offset: Int): DocumentTextWindow? {
        return windows.values.firstOrNull { offset >= it.start && offset <= it.end }
            ?: windows.values.lastOrNull()
    }

    /** Maps a window-local selection onto the global edit string (used by Viewer selection sync). */
    fun toGlobalSelection(window: DocumentTextWindow, local: TextRange): TextRange {
        val start = (local.start + window.start).coerceIn(window.start, window.end)
        val end = (local.end + window.start).coerceIn(window.start, window.end)
        return TextRange(minOf(start, end), maxOf(start, end))
    }
}
