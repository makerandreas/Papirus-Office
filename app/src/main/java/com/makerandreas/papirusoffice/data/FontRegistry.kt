package com.makerandreas.papirusoffice.data

import androidx.compose.ui.text.font.FontFamily
import java.util.Locale

/** Generic family a name collapses to when no bundled face is chosen. */
enum class GenericFamily { SERIF, SANS_SERIF, MONOSPACE, SYMBOL, DEFAULT }

/** Where the face behind a [FontChoice] comes from, in substitution order. */
enum class FontSource {
    /** The requested family itself ships in `assets/fonts`. */
    BUNDLED_EXACT,
    /** A bundled face with the same advance widths as the requested family. */
    BUNDLED_METRIC_COMPATIBLE,
    /** A bundled face chosen for looks; its widths differ from the requested family. */
    BUNDLED_STAND_IN,
    /** A family the user installed (see [FontProvider]), registered at runtime. */
    USER,
    /** Nothing bundled or installed; the platform's generic family of that class. */
    SYSTEM_GENERIC,
    /** Unknown name: the platform default family. */
    DEFAULT
}

/**
 * One resolved font decision. [family] is the face that both measures and
 * paints the run; [metricCompatible] says whether line breaks computed with
 * it can be trusted to match the requested family.
 */
data class FontChoice(
    val requested: String?,
    val family: String,
    val generic: GenericFamily,
    val source: FontSource,
    val metricCompatible: Boolean,
    /** File stem under `assets/fonts` (e.g. `LiberationSerif` for `LiberationSerif-Regular.ttf`), null when not bundled. */
    val assetStem: String? = null
) {
    /**
     * Display family for this decision in plan 5A. Real `Typeface` loading
     * from [assetStem] is plan 10 A1; until then every choice paints with the
     * platform's generic family of its class, exactly as `OfficeRuns` did
     * before the registry existed, so the mapping can land without a visual
     * change. The decision itself (which face, why) is already the one plan
     * 10 will load.
     */
    val composeFamily: FontFamily
        get() = when (generic) {
            GenericFamily.SERIF -> FontFamily.Serif
            GenericFamily.SANS_SERIF -> FontFamily.SansSerif
            GenericFamily.MONOSPACE -> FontFamily.Monospace
            GenericFamily.SYMBOL -> FontFamily.Default
            GenericFamily.DEFAULT -> FontFamily.Default
        }
}

/**
 * Font substitution seam shared by measurement ([TextMetrics]) and display
 * ([OfficeRuns.fontFamilyFor]) so the two can never pick different faces
 * for one name (roadmap E-EN-5; substitution order from roadmap §0):
 *
 *  1. exact: the name is a bundled family,
 *  2. bundled metric-compatible substitute (Liberation for Times New Roman,
 *     Arial, Courier New; Carlito for Calibri; Caladea for Cambria),
 *  3. bundled stand-in that is *not* metric-compatible (Martel Sans for
 *     Aptos, user decision 2026-09-26),
 *  4. a family the user installed and registered through [registerUserFamilies],
 *  5. the platform generic family the name's class suggests,
 *  6. the platform default.
 *
 * The input corpus is audit-007 §8: the eleven family names the six sample
 * pairs actually declare. Names are matched case-insensitively with quotes
 * and surrounding whitespace removed, which is how ODF `fo:font-family`
 * writes them (`'Times New Roman'`).
 */
object FontRegistry {

    data class BundledFace(val family: String, val assetStem: String, val generic: GenericFamily)

    /** Faces present in `app/src/main/assets/fonts` (all SIL OFL). */
    val bundledFaces: List<BundledFace> = listOf(
        BundledFace("Liberation Serif", "LiberationSerif", GenericFamily.SERIF),
        BundledFace("Liberation Sans", "LiberationSans", GenericFamily.SANS_SERIF),
        BundledFace("Liberation Sans Narrow", "LiberationSansNarrow", GenericFamily.SANS_SERIF),
        BundledFace("Liberation Mono", "LiberationMono", GenericFamily.MONOSPACE),
        BundledFace("Carlito", "Carlito", GenericFamily.SANS_SERIF),
        BundledFace("Caladea", "Caladea", GenericFamily.SERIF),
        BundledFace("Gentium Basic", "GenBas", GenericFamily.SERIF),
        BundledFace("Gentium Book Basic", "GenBkBas", GenericFamily.SERIF),
        BundledFace("Martel Sans", "MartelSans", GenericFamily.SANS_SERIF),
        BundledFace("OpenSymbol", "opens___", GenericFamily.SYMBOL)
    )

    private val bundledByKey: Map<String, BundledFace> = bundledFaces.associateBy { key(it.family) }

    /** Requested family -> bundled face whose advances match it. */
    private val metricCompatible: Map<String, String> = mapOf(
        "times new roman" to "Liberation Serif",
        "times" to "Liberation Serif",
        "arial" to "Liberation Sans",
        "helvetica" to "Liberation Sans",
        "arial narrow" to "Liberation Sans Narrow",
        "courier new" to "Liberation Mono",
        "courier" to "Liberation Mono",
        "calibri" to "Carlito",
        "cambria" to "Caladea",
        "cambria math" to "Caladea",
        "gentium" to "Gentium Basic",
        "symbol" to "OpenSymbol",
        "wingdings" to "OpenSymbol"
    )

    /**
     * Requested family -> bundled face chosen for appearance only. Aptos and
     * Aptos Display are Microsoft's 365 defaults; no metric-compatible face
     * exists in the open, so Martel Sans (Google Fonts, OFL) stands in and
     * is recorded as not metric-compatible. It rarely reaches text in the
     * corpus: every body paragraph resolves to Times New Roman first.
     */
    private val standIns: Map<String, String> = mapOf(
        "aptos" to "Martel Sans",
        "aptos display" to "Martel Sans"
    )

    /** Class hint for names that have no bundled face at all. */
    private val genericHints: Map<String, GenericFamily> = mapOf(
        "serif" to GenericFamily.SERIF,
        "sans-serif" to GenericFamily.SANS_SERIF,
        "sans serif" to GenericFamily.SANS_SERIF,
        "monospace" to GenericFamily.MONOSPACE,
        "roboto" to GenericFamily.SANS_SERIF,
        "noto sans" to GenericFamily.SANS_SERIF,
        "noto serif" to GenericFamily.SERIF,
        "open sans" to GenericFamily.SANS_SERIF,
        "segoe ui" to GenericFamily.SANS_SERIF,
        "segoe ui variable" to GenericFamily.SANS_SERIF,
        "basic sans" to GenericFamily.SANS_SERIF,
        "microsoft yahei ui" to GenericFamily.SANS_SERIF,
        "ms mincho" to GenericFamily.SERIF,
        "georgia" to GenericFamily.SERIF,
        "garamond" to GenericFamily.SERIF,
        "verdana" to GenericFamily.SANS_SERIF,
        "tahoma" to GenericFamily.SANS_SERIF,
        "consolas" to GenericFamily.MONOSPACE,
        "lucida console" to GenericFamily.MONOSPACE
    )

    /** Family names supplied by the user's font directory; class is guessed from the name. */
    @Volatile
    private var userFamilies: Map<String, GenericFamily> = emptyMap()

    /**
     * Records the families [FontProvider] found in the user font directory so
     * step 4 of the order can answer. Idempotent; replaces the previous set.
     */
    fun registerUserFamilies(families: Collection<String>) {
        userFamilies = families
            .map { key(it) }
            .filter { it.isNotBlank() }
            .associateWith { classify(it) }
    }

    fun clearUserFamilies() {
        userFamilies = emptyMap()
    }

    /** The resolved decision for [requested]; never throws, never returns null. */
    fun resolve(requested: String?): FontChoice {
        val k = key(requested)
        if (k.isEmpty()) {
            return FontChoice(requested, "default", GenericFamily.DEFAULT, FontSource.DEFAULT, metricCompatible = false)
        }
        bundledByKey[k]?.let { face ->
            return FontChoice(requested, face.family, face.generic, FontSource.BUNDLED_EXACT, metricCompatible = true, assetStem = face.assetStem)
        }
        metricCompatible[k]?.let { familyName ->
            val face = bundledByKey.getValue(key(familyName))
            return FontChoice(requested, face.family, face.generic, FontSource.BUNDLED_METRIC_COMPATIBLE, metricCompatible = true, assetStem = face.assetStem)
        }
        standIns[k]?.let { familyName ->
            val face = bundledByKey.getValue(key(familyName))
            return FontChoice(requested, face.family, face.generic, FontSource.BUNDLED_STAND_IN, metricCompatible = false, assetStem = face.assetStem)
        }
        userFamilies[k]?.let { generic ->
            return FontChoice(requested, requested!!.trim().trim('\'', '"'), generic, FontSource.USER, metricCompatible = true)
        }
        val generic = classify(k)
        return if (generic == GenericFamily.DEFAULT) {
            FontChoice(requested, "default", GenericFamily.DEFAULT, FontSource.DEFAULT, metricCompatible = false)
        } else {
            FontChoice(requested, genericName(generic), generic, FontSource.SYSTEM_GENERIC, metricCompatible = false)
        }
    }

    /** Compose family for display; the same decision [TextMetrics] measures with. */
    fun composeFamilyFor(requested: String?): FontFamily = resolve(requested).composeFamily

    /** Normalised lookup key: lower case, quotes and whitespace stripped. */
    fun key(name: String?): String =
        name?.trim()?.trim('\'', '"')?.trim()?.lowercase(Locale.ROOT).orEmpty()

    private fun classify(k: String): GenericFamily {
        genericHints[k]?.let { return it }
        return when {
            k.contains("mono") || k.contains("courier") || k.contains("console") || k.contains("code") -> GenericFamily.MONOSPACE
            k.contains("sans") || k.contains("gothic") || k.contains("ui") -> GenericFamily.SANS_SERIF
            k.contains("serif") || k.contains("roman") || k.contains("mincho") || k.contains("book") -> GenericFamily.SERIF
            k.contains("symbol") || k.contains("dings") -> GenericFamily.SYMBOL
            else -> GenericFamily.DEFAULT
        }
    }

    private fun genericName(generic: GenericFamily): String = when (generic) {
        GenericFamily.SERIF -> "serif"
        GenericFamily.SANS_SERIF -> "sans-serif"
        GenericFamily.MONOSPACE -> "monospace"
        GenericFamily.SYMBOL -> "symbol"
        GenericFamily.DEFAULT -> "default"
    }
}
