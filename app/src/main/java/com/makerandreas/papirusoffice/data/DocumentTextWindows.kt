package com.makerandreas.papirusoffice.data

/**
 * Window of the flat editor string owned by one textual document element.
 * [start] is inclusive, [end] is exclusive, both offsets into the global
 * `\n\n`-joined text that [DocumentTextMerger] also consumes.
 */
data class DocumentTextWindow(
    val elementIndex: Int,
    val text: String,
    val start: Int,
    val end: Int
)

/**
 * Bidirectional map between the flat edit text and textual body elements.
 * Both sides split on `"\n\n"` in document order, so window N always covers
 * block N; structural elements (images, tables, breaks) own no text.
 */
object DocumentTextWindows {

    fun compute(elements: List<OfficeElement>, globalText: String): Map<Int, DocumentTextWindow> {
        val blocks = globalText.split("\n\n")
        val windows = LinkedHashMap<Int, DocumentTextWindow>(blocks.size)
        var blockIndex = 0
        var offset = 0
        elements.forEachIndexed { elementIndex, element ->
            if (!DocumentTextMerger.isTextual(element)) return@forEachIndexed
            if (blockIndex >= blocks.size) return@forEachIndexed
            val text = blocks[blockIndex]
            windows[elementIndex] = DocumentTextWindow(elementIndex, text, offset, offset + text.length)
            offset += text.length + 2
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
}
