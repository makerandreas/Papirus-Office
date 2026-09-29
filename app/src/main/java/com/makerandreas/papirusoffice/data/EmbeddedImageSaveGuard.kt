package com.makerandreas.papirusoffice.data

/**
 * True when the document model contains embedded graphics that a serializer
 * must preserve rather than flatten to text or silently omit.
 */
internal fun OfficeDocument.containsEmbeddedImages(): Boolean {
    if (resources.images.isNotEmpty()) return true

    fun containsImage(elements: List<OfficeElement>): Boolean = elements.any { element ->
        when (element) {
            is OfficeImage,
            is OfficeDocElement.ImageElement -> true
            is OfficeSection -> containsImage(element.elements)
            else -> false
        }
    }

    return containsImage(body.elements)
}
