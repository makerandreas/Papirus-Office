package com.makerandreas.papirusoffice.data

import android.graphics.Paint
import android.graphics.Typeface

/**
 * What the paginator needs from a resolved style: advances and a line
 * height, both in layout units (96 per inch, see [LayoutUnits]).
 */
interface Measurable {
    val choice: FontChoice
    /** Font size in layout units: 12 pt = 16. */
    val fontSizeUnits: Float
    /** Ascent + descent of the face at [fontSizeUnits]; "single" spacing. */
    val naturalLineHeightUnits: Float
    /** Line height after the style's factor or exact value is applied. */
    val lineHeightUnits: Float
    /** Advance width of [text] in layout units. */
    fun widthOf(text: String): Float
}

/**
 * Glyph advance provider. The platform [Paint] on device, a fixed table on
 * the JVM, both answering in layout units so a test and a phone paginate the
 * same paragraph the same way for the same source.
 */
interface AdvanceSource {
    val name: String
    fun advance(text: String, fontSizeUnits: Float, isBold: Boolean, isItalic: Boolean, choice: FontChoice): Float
    fun naturalLineHeight(fontSizeUnits: Float, isBold: Boolean, isItalic: Boolean, choice: FontChoice): Float
}

/**
 * Deterministic advance table for JVM tests and for devices whose graphics
 * stack is unavailable. Widths are em fractions of the Times New Roman /
 * Liberation Serif regular face (the body face of all six sample pairs,
 * audit-007 §3) rounded to three places; sans faces are scaled up by the
 * Arial-to-Times average width ratio, monospace is 0.6 em, bold adds 5 %.
 * The table is a stand-in for measuring, not a claim about any real font.
 */
object TableAdvanceSource : AdvanceSource {
    override val name: String = "table"

    private const val DEFAULT_EM = 0.5f
    private const val SANS_RATIO = 1.08f
    private const val BOLD_RATIO = 1.05f
    private const val MONO_EM = 0.6f

    private val serifEm: Map<Char, Float> = buildMap<Char, Float> {
        put(' ', 0.25f)
        "0123456789".forEach { put(it, 0.5f) }
        val lower = floatArrayOf(
            0.444f, 0.5f, 0.444f, 0.5f, 0.444f, 0.333f, 0.5f, 0.5f, 0.278f, 0.278f, 0.5f, 0.278f, 0.778f,
            0.5f, 0.5f, 0.5f, 0.5f, 0.333f, 0.389f, 0.278f, 0.5f, 0.5f, 0.722f, 0.5f, 0.5f, 0.444f
        )
        val upper = floatArrayOf(
            0.722f, 0.667f, 0.667f, 0.722f, 0.611f, 0.556f, 0.722f, 0.722f, 0.333f, 0.389f, 0.722f, 0.611f, 0.889f,
            0.722f, 0.722f, 0.556f, 0.722f, 0.667f, 0.556f, 0.611f, 0.722f, 0.722f, 0.944f, 0.722f, 0.722f, 0.611f
        )
        for (i in 0 until 26) {
            put('a' + i, lower[i])
            put('A' + i, upper[i])
        }
        put('.', 0.25f); put(',', 0.25f); put(':', 0.278f); put(';', 0.278f)
        put('-', 0.333f); put('(', 0.333f); put(')', 0.333f); put('!', 0.333f)
        put('\'', 0.18f); put('"', 0.408f); put('?', 0.444f); put('/', 0.278f)
        put('[', 0.333f); put(']', 0.333f); put('*', 0.5f); put('&', 0.778f)
        put('%', 0.833f); put('@', 0.921f); put('#', 0.5f); put('$', 0.5f)
        put('_', 0.5f); put('+', 0.564f); put('=', 0.564f); put('<', 0.564f); put('>', 0.564f)
    }

    /** Natural line height as a fraction of the em: ascent + descent of the class's reference face. */
    private fun naturalEm(generic: GenericFamily): Float = when (generic) {
        GenericFamily.SERIF -> 1.107f
        GenericFamily.SANS_SERIF -> 1.117f
        GenericFamily.MONOSPACE -> 1.133f
        GenericFamily.SYMBOL, GenericFamily.DEFAULT -> 1.15f
    }

    fun emWidth(ch: Char, generic: GenericFamily, isBold: Boolean): Float {
        val base = when (generic) {
            GenericFamily.MONOSPACE -> MONO_EM
            GenericFamily.SANS_SERIF -> (serifEm[ch] ?: DEFAULT_EM) * SANS_RATIO
            else -> serifEm[ch] ?: DEFAULT_EM
        }
        return if (isBold) base * BOLD_RATIO else base
    }

    override fun advance(text: String, fontSizeUnits: Float, isBold: Boolean, isItalic: Boolean, choice: FontChoice): Float {
        if (text.isEmpty() || fontSizeUnits <= 0f) return 0f
        var em = 0f
        for (ch in text) em += emWidth(ch, choice.generic, isBold)
        return em * fontSizeUnits
    }

    override fun naturalLineHeight(fontSizeUnits: Float, isBold: Boolean, isItalic: Boolean, choice: FontChoice): Float =
        fontSizeUnits * naturalEm(choice.generic)
}

/**
 * Platform advances through [Paint] at `textSize = fontSizeUnits`, so one
 * Paint pixel is one layout unit and no scale factor sits between the two.
 * The face is the generic platform family of the registry's class until plan
 * 10 loads the bundled files; the class is the same one the renderer paints.
 */
class PaintAdvanceSource : AdvanceSource {
    override val name: String = "paint"

    private val paint = Paint().apply { isAntiAlias = true }

    private fun configure(fontSizeUnits: Float, isBold: Boolean, isItalic: Boolean, choice: FontChoice) {
        val base = when (choice.generic) {
            GenericFamily.SERIF -> Typeface.SERIF
            GenericFamily.SANS_SERIF -> Typeface.SANS_SERIF
            GenericFamily.MONOSPACE -> Typeface.MONOSPACE
            GenericFamily.SYMBOL, GenericFamily.DEFAULT -> Typeface.DEFAULT
        }
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        paint.typeface = Typeface.create(base, style)
        paint.textSize = fontSizeUnits
    }

    override fun advance(text: String, fontSizeUnits: Float, isBold: Boolean, isItalic: Boolean, choice: FontChoice): Float {
        if (text.isEmpty() || fontSizeUnits <= 0f) return 0f
        configure(fontSizeUnits, isBold, isItalic, choice)
        return paint.measureText(text)
    }

    override fun naturalLineHeight(fontSizeUnits: Float, isBold: Boolean, isItalic: Boolean, choice: FontChoice): Float {
        configure(fontSizeUnits, isBold, isItalic, choice)
        val fm = paint.fontMetrics
        val height = fm.descent - fm.ascent
        return if (height > 0f) height else fontSizeUnits * 1.15f
    }

    /**
     * True when this Paint measures real glyphs: a wide and a narrow string
     * of equal length must differ. A stubbed Paint (JVM without native
     * graphics) answers per character and is not a measurement.
     */
    fun measuresRealGlyphs(): Boolean = try {
        val probe = FontRegistry.resolve("serif")
        val wide = advance("MMMM", 100f, isBold = false, isItalic = false, choice = probe)
        val narrow = advance("iiii", 100f, isBold = false, isItalic = false, choice = probe)
        wide > 0f && narrow > 0f && wide > narrow * 1.2f
    } catch (t: Throwable) {
        false
    }
}

/**
 * Metrics of one resolved [ParagraphStyle] (roadmap E-EN-2): the family
 * comes from [FontRegistry], the size from [LayoutUnits.ptToUnits], the
 * advances from an [AdvanceSource]. Measure and display share the style
 * object, so what wraps here is what paints there. Plan 5A only provides the
 * class; [LayoutEngine.layoutParagraph] switches to it in PR 16b.
 */
class TextMetrics private constructor(
    val style: ParagraphStyle,
    override val choice: FontChoice,
    private val source: AdvanceSource
) : Measurable {

    override val fontSizeUnits: Float = LayoutUnits.ptToUnits(style.fontSizeSp.coerceAtLeast(1f))

    override val naturalLineHeightUnits: Float =
        source.naturalLineHeight(fontSizeUnits, style.isBold, style.isItalic, choice)
            .coerceAtLeast(fontSizeUnits)

    /**
     * Proportional spacing multiplies the natural single line height, which
     * is how both Writer (`fo:line-height="115%"`) and Word
     * (`w:line=276 lineRule=auto`) define it; an exact height replaces it.
     */
    override val lineHeightUnits: Float =
        style.lineHeightExactUnits?.takeIf { it > 0f }
            ?: (naturalLineHeightUnits * style.lineHeightFactor.coerceAtLeast(0.1f))

    val sourceName: String get() = source.name

    override fun widthOf(text: String): Float =
        source.advance(text, fontSizeUnits, style.isBold, style.isItalic, choice)

    companion object {
        @Volatile
        private var platformSource: AdvanceSource? = null

        /**
         * Paint when it measures real glyphs, else the deterministic table.
         * Decided once per process; tests pass a source explicitly.
         */
        fun defaultSource(): AdvanceSource {
            platformSource?.let { return it }
            val chosen: AdvanceSource = try {
                val paint = PaintAdvanceSource()
                if (paint.measuresRealGlyphs()) paint else TableAdvanceSource
            } catch (t: Throwable) {
                TableAdvanceSource
            }
            platformSource = chosen
            return chosen
        }

        fun forStyle(style: ParagraphStyle, source: AdvanceSource = defaultSource()): TextMetrics =
            TextMetrics(style, FontRegistry.resolve(style.fontFamily), source)
    }
}
