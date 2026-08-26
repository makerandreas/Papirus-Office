package com.makerandreas.papirusoffice.data.writer

/**
 * Represents a text selection range [start, end].
 */
data class SelectionRange(
    val start: Int,
    val end: Int
) {
    val min: Int get() = kotlin.math.min(start, end)
    val max: Int get() = kotlin.math.max(start, end)
    val length: Int get() = max - min
    val isCollapsed: Boolean get() = start == end
}
