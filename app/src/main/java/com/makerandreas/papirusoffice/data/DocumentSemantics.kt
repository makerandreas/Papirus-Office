package com.makerandreas.papirusoffice.data

/** A stable half-open range over [DocumentBody.elements]. */
data class BodyElementRange(
    val startInclusive: Int,
    val endExclusive: Int
) {
    init {
        require(startInclusive >= 0) { "Body range start must be non-negative" }
        require(endExclusive >= startInclusive) { "Body range end must not precede its start" }
    }

    val isEmpty: Boolean get() = startInclusive == endExclusive

    operator fun contains(elementIndex: Int): Boolean = elementIndex in startInclusive until endExclusive
}

enum class DocumentIndexKind {
    TABLE_OF_CONTENT,
    ALPHABETICAL_INDEX,
    TABLE_INDEX,
    ILLUSTRATION_INDEX,
    OBJECT_INDEX,
    USER_INDEX,
    BIBLIOGRAPHY
}

/** One authored entry whose rendered paragraph remains in normal body flow. */
data class DocumentIndexEntry(
    val elementIndex: Int,
    val level: Int? = null,
    val targetAnchor: String? = null,
    val displayedPageLabel: String? = null,
    /** Visible entry text without the trailing page label, tabs collapsed to single spaces. */
    val text: String = ""
) {
    init {
        require(elementIndex >= 0) { "Index entry element must be non-negative" }
        require(level == null || level > 0) { "Index entry level must be positive" }
    }
}

/** Semantic identity for an authored index snapshot without nesting body elements. */
data class DocumentIndexRange(
    val id: String,
    val kind: DocumentIndexKind,
    val name: String? = null,
    val styleName: String? = null,
    val bodyRange: BodyElementRange,
    val entries: List<DocumentIndexEntry> = emptyList(),
    val isProtected: Boolean = false,
    /** Elements produced by `text:index-title`; always inside [bodyRange] and never listed as entries. */
    val titleRange: BodyElementRange? = null
) {
    init {
        require(id.isNotBlank()) { "Index id must not be blank" }
        require(
            titleRange == null || (
                titleRange.startInclusive >= bodyRange.startInclusive &&
                    titleRange.endExclusive <= bodyRange.endExclusive
                )
        ) { "Index title range must fall inside the index body range" }
        require(entries.all { it.elementIndex in bodyRange }) {
            "Index entries must fall inside the index body range"
        }
    }
}

enum class SectionDisplay { VISIBLE, CONDITIONAL, HIDDEN }

/** A named section sidecar; nested sections use [parentId] and [depth]. */
data class DocumentSectionRange(
    val id: String,
    val name: String,
    val bodyRange: BodyElementRange,
    val styleName: String? = null,
    val parentId: String? = null,
    val depth: Int = 0,
    val display: SectionDisplay = SectionDisplay.VISIBLE,
    val condition: String? = null,
    val isProtected: Boolean = false,
    val protectionKey: String? = null
) {
    init {
        require(id.isNotBlank()) { "Section id must not be blank" }
        require(name.isNotBlank()) { "Section name must not be blank" }
        require(depth >= 0) { "Section depth must be non-negative" }
    }
}

enum class TableColumnWidthKind { AUTO, ABSOLUTE, RELATIVE }

/** Absolute values use [LayoutUnits]; relative values are positive weights. */
data class TableColumnWidthSpec(
    val kind: TableColumnWidthKind = TableColumnWidthKind.AUTO,
    val value: Float = 0f
) {
    init {
        require(value >= 0f && value.isFinite()) { "Table column width must be finite and non-negative" }
    }
}

data class OfficeTableColumnSpec(
    val styleName: String? = null,
    val width: TableColumnWidthSpec = TableColumnWidthSpec(),
    val repeatCount: Int = 1,
    val defaultCellStyleName: String? = null
) {
    init {
        require(repeatCount > 0) { "Table column repeat count must be positive" }
    }
}

enum class TableCellOccupancy { ORIGIN, COVERED }

enum class TableVerticalAlignment { TOP, MIDDLE, BOTTOM, AUTOMATIC }

enum class TableBorderLineStyle { NONE, SOLID, DOTTED, DASHED, DOUBLE, OTHER }

data class TableBorder(
    val widthUnits: Float = 0f,
    val style: TableBorderLineStyle = TableBorderLineStyle.NONE,
    val colorHex: String? = null,
    val sourceStyle: String? = null
) {
    init {
        require(widthUnits >= 0f && widthUnits.isFinite()) { "Table border width must be finite and non-negative" }
    }
}

data class TableInsets(
    val topUnits: Float = 0f,
    val endUnits: Float = 0f,
    val bottomUnits: Float = 0f,
    val startUnits: Float = 0f
) {
    init {
        require(listOf(topUnits, endUnits, bottomUnits, startUnits).all { it >= 0f && it.isFinite() }) {
            "Table insets must be finite and non-negative"
        }
    }
}

data class TableCellBoxStyle(
    val padding: TableInsets = TableInsets(),
    val borderTop: TableBorder = TableBorder(),
    val borderEnd: TableBorder = TableBorder(),
    val borderBottom: TableBorder = TableBorder(),
    val borderStart: TableBorder = TableBorder(),
    val backgroundColorHex: String? = null,
    val verticalAlignment: TableVerticalAlignment = TableVerticalAlignment.AUTOMATIC
)

data class TableRowStyle(
    val minimumHeightUnits: Float? = null,
    val exactHeightUnits: Float? = null,
    val keepTogether: Boolean = false
) {
    init {
        require(minimumHeightUnits == null || minimumHeightUnits >= 0f && minimumHeightUnits.isFinite()) {
            "Minimum row height must be finite and non-negative"
        }
        require(exactHeightUnits == null || exactHeightUnits >= 0f && exactHeightUnits.isFinite()) {
            "Exact row height must be finite and non-negative"
        }
    }
}

data class OfficeFontFace(
    val name: String,
    val family: String,
    val genericFamily: String? = null,
    val pitch: String? = null,
    val charset: String? = null
) {
    init {
        require(name.isNotBlank()) { "Font-face name must not be blank" }
        require(family.isNotBlank()) { "Font-face family must not be blank" }
    }
}

/** Source-package facts used to prevent a simplified writer from discarding structure. */
data class OdtSourceFeatures(
    val hasAuthoredIndexes: Boolean = false,
    val hasNamedSections: Boolean = false,
    val hasAdvancedTables: Boolean = false,
    val hasFontFaceDeclarations: Boolean = false
) {
    val requiresStructuralWriter: Boolean
        get() = hasAuthoredIndexes || hasNamedSections || hasAdvancedTables
}
