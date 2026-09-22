package com.makerandreas.papirusoffice.data.navigation

import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeShape
import com.makerandreas.papirusoffice.data.OfficeTable
import java.util.Locale

/**
 * Kind of Navigator-listed object. Prefixes follow the creating suite's UI
 * language (LibreOffice / Microsoft Office), not the Papirus app locale.
 */
enum class NavigatorObjectKind {
    HEADING,
    TABLE,
    IMAGE,
    CHART,
    OBJECT,
    SHAPE,
    FRAME,
    SECTION,
    BOOKMARK,
    PAGE,
    FOOTNOTE
}

/**
 * Localized stems used both to *recognize* style/object names in a file and
 * to *generate* fallback Navigator labels when the document left the object unnamed.
 */
data class NavigatorLocalePack(
    val languageTag: String,
    val headingStyleTokens: List<String>,
    val prefixes: Map<NavigatorObjectKind, String>
) {
    fun autoName(kind: NavigatorObjectKind, index: Int): String {
        val stem = prefixes[kind] ?: prefixes[NavigatorObjectKind.OBJECT] ?: "Object"
        return if (kind == NavigatorObjectKind.HEADING ||
            kind == NavigatorObjectKind.PAGE ||
            kind == NavigatorObjectKind.FOOTNOTE
        ) {
            "$stem $index"
        } else {
            "$stem$index"
        }
    }

    fun untitledHeading(level: Int): String = autoName(NavigatorObjectKind.HEADING, level)
}

/**
 * Language-aware Navigator strings.
 *
 * Recognition always scans every pack (an Indonesian `Judul1` heading must
 * still index when the app UI is English). Auto-names follow [detect] on the
 * document: metadata language, then style/object evidence.
 */
object NavigatorStringCatalog {

    val ENGLISH = NavigatorLocalePack(
        languageTag = "en",
        headingStyleTokens = listOf("heading", "title", "header"),
        prefixes = mapOf(
            NavigatorObjectKind.HEADING to "Heading",
            NavigatorObjectKind.TABLE to "Table",
            NavigatorObjectKind.IMAGE to "Image",
            NavigatorObjectKind.CHART to "Chart",
            NavigatorObjectKind.OBJECT to "Object",
            NavigatorObjectKind.SHAPE to "Shape",
            NavigatorObjectKind.FRAME to "Frame",
            NavigatorObjectKind.SECTION to "Section",
            NavigatorObjectKind.BOOKMARK to "Bookmark",
            NavigatorObjectKind.PAGE to "Page",
            NavigatorObjectKind.FOOTNOTE to "Footnote"
        )
    )

    val INDONESIAN = NavigatorLocalePack(
        languageTag = "id",
        headingStyleTokens = listOf("judul", "bab"),
        prefixes = mapOf(
            NavigatorObjectKind.HEADING to "Judul",
            NavigatorObjectKind.TABLE to "Tabel",
            NavigatorObjectKind.IMAGE to "Gambar",
            NavigatorObjectKind.CHART to "Grafik",
            NavigatorObjectKind.OBJECT to "Objek",
            NavigatorObjectKind.SHAPE to "Bentuk",
            NavigatorObjectKind.FRAME to "Bingkai",
            NavigatorObjectKind.SECTION to "Bagian",
            NavigatorObjectKind.BOOKMARK to "Penanda",
            NavigatorObjectKind.PAGE to "Halaman",
            NavigatorObjectKind.FOOTNOTE to "Catatan kaki"
        )
    )

    /** Extra heading tokens seen in other suite UI languages (recognition only). */
    private val extraHeadingTokens = listOf("titre", "überschrift", "ueberschrift", "encabezado")

    private val allPacks = listOf(INDONESIAN, ENGLISH)

    fun forLanguageTag(tag: String?): NavigatorLocalePack {
        val lower = tag?.lowercase(Locale.ROOT).orEmpty()
        return when {
            lower == "id" || lower.startsWith("id-") || lower == "in" || lower.startsWith("in-") -> INDONESIAN
            else -> ENGLISH
        }
    }

    /**
     * Prefer document evidence (Judul / Tabel / Gambar) over the metadata default
     * of `en-US`, which many parsers leave untouched.
     * Kept for legacy / \"Follow document\" mode — see [resolveNavigatorLocale].
     */
    fun detect(document: OfficeDocument): NavigatorLocalePack {
        val evidence = collectEvidence(document)
        var idHits = 0
        var enHits = 0
        for (token in evidence) {
            val lower = token.lowercase(Locale.ROOT)
            if (matchesPack(lower, INDONESIAN)) idHits++
            if (matchesPack(lower, ENGLISH)) enHits++
        }
        if (idHits > 0 && idHits >= enHits) return INDONESIAN

        val meta = document.metadata.language
        if (meta.isNotBlank()) {
            val fromMeta = forLanguageTag(meta)
            if (fromMeta.languageTag == "id") return INDONESIAN
        }
        return ENGLISH
    }

    /**
     * P2-2: Resolve Navigator locale pack according to user preference.
     * - When [preferAppLocale] is true (default), the *app* language decides prefixes
     *   (e.g. English app → \"Table1\", \"Image1\"), even if the document contains Indonesian
     *   evidence like `Judul1`. Heading *recognition* remains locale-agnostic via
     *   [headingLevelFromStyleName] which scans all packs.
     * - When false, fall back to legacy [detect] (document evidence → metadata).
     */
    fun resolveNavigatorLocale(
        document: OfficeDocument,
        preferAppLocale: Boolean,
        appLanguageTag: String?
    ): NavigatorLocalePack {
        return if (preferAppLocale) {
            forLanguageTag(appLanguageTag ?: Locale.getDefault().language)
        } else {
            detect(document)
        }
    }

    fun headingLevelFromStyleName(styleName: String?): Int {
        if (styleName.isNullOrBlank()) return 0
        val lower = styleName.lowercase(Locale.ROOT)
        val isHeading = allPacks.any { pack -> pack.headingStyleTokens.any { lower.contains(it) } } ||
            extraHeadingTokens.any { lower.contains(it) }
        if (!isHeading) return 0
        val digit = DIGIT_REGEX.find(styleName)?.value?.toIntOrNull()
        return when {
            digit != null -> digit.coerceIn(1, 6)
            else -> 1
        }
    }

    fun kindOfStoredName(name: String?): NavigatorObjectKind? {
        if (name.isNullOrBlank()) return null
        val lower = name.lowercase(Locale.ROOT)
        for (pack in allPacks) {
            pack.prefixes.forEach { (kind, stem) ->
                if (kind == NavigatorObjectKind.HEADING) return@forEach
                if (lower.startsWith(stem.lowercase(Locale.ROOT))) return kind
            }
        }
        if (lower.startsWith("picture") || lower.startsWith("image")) return NavigatorObjectKind.IMAGE
        if (lower.startsWith("bagan")) return NavigatorObjectKind.CHART
        return null
    }

    private fun collectEvidence(document: OfficeDocument): List<String> {
        val out = mutableListOf<String>()
        document.styles.paragraphStyles.values.forEach { style ->
            out.add(style.name)
            style.parentStyleName?.let { out.add(it) }
        }
        document.body.elements.forEach { el ->
            when (el) {
                is OfficeParagraph -> el.styleName?.let { out.add(it) }
                is OfficeHeading -> el.styleName?.let { out.add(it) }
                is OfficeTable -> el.name?.let { out.add(it) }
                is OfficeImage -> el.name?.let { out.add(it) }
                is OfficeShape -> {
                    el.name?.let { out.add(it) }
                    out.add(el.type)
                }
                else -> {}
            }
        }
        document.resources.objects.forEach { out.add(it) }
        return out
    }

    private fun matchesPack(lower: String, pack: NavigatorLocalePack): Boolean {
        if (pack.headingStyleTokens.any { lower.contains(it) }) return true
        return pack.prefixes.values.any { stem ->
            val s = stem.lowercase(Locale.ROOT)
            s.length >= 4 && lower.contains(s)
        }
    }

    private val DIGIT_REGEX = Regex("""\d+""")
}
