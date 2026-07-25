package com.makerandreas.papirusoffice.data.writer

/**
 * Abstract base class for character attributes / paragraph formatting in Writer paragraphs.
 */
abstract class SwTextAttr(
    var start: Int,
    var end: Int
) {
    fun isValid(textLength: Int): Boolean {
        return start in 0..textLength && end in start..textLength
    }
}

// ==========================================
// Formatting Attributes
// ==========================================

class SwFormatBold(start: Int, end: Int) : SwTextAttr(start, end)

class SwFormatItalic(start: Int, end: Int) : SwTextAttr(start, end)

class SwFormatUnderline(
    start: Int,
    end: Int,
    val colorHex: String = "#000000"
) : SwTextAttr(start, end)

class SwFormatStrikethrough(start: Int, end: Int) : SwTextAttr(start, end)

class SwFormatFont(
    start: Int,
    end: Int,
    val fontName: String,
    val fontSizeSp: Int
) : SwTextAttr(start, end)

class SwFormatColor(
    start: Int,
    end: Int,
    val colorHex: String
) : SwTextAttr(start, end)

// ==========================================
// Nested Attributes (Hyperlink, Ruby, Meta)
// ==========================================

class SwFormatHyperlink(
    start: Int,
    end: Int,
    val url: String,
    val title: String = ""
) : SwTextAttr(start, end)

class SwFormatRuby(
    start: Int,
    end: Int,
    val rubyText: String,
    val positionAbove: Boolean = true
) : SwTextAttr(start, end)

class SwFormatMeta(
    start: Int,
    end: Int,
    val metaType: String,
    val metadata: Map<String, String> = emptyMap()
) : SwTextAttr(start, end)

// ==========================================
// Field Attribute
// ==========================================

class SwFormatField(
    start: Int,
    end: Int,
    val fieldType: SwFieldType
) : SwTextAttr(start, end)
