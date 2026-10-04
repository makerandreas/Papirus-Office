package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.OfficeFontFace

/**
 * Resolves an ODF `style:font-name` value through the document's
 * `office:font-face-decls` table.
 *
 * ODF 1.4 Part 3 section 20.277 is the reason this exists: if a font face
 * declaration is referenced by name, "the font-matching algorithms for
 * selecting a font declaration based on the font-family, font-style,
 * font-variant, font-weight and font-size descriptors are not used but the
 * referenced font face declaration is used directly". The alias therefore has
 * to be replaced by the family the declaration names, in both XML parts,
 * before [com.makerandreas.papirusoffice.data.FontRegistry] classifies it.
 * `style:name` is an identifier, not necessarily a real family
 * (section 19.502.3), so passing the raw alias to the registry would classify
 * `Aptos1` as an unknown name instead of as Aptos.
 *
 * Nothing here changes what is painted: a resolved choice still maps to a
 * generic family through `FontChoice.composeFamily` until Plan 10 A1 loads
 * the bundled files.
 */
object FontFaceResolver {

    /**
     * First family of a `svg:font-family` value (ODF 1.4 Part 3 19.532),
     * which may be a comma-separated list with quote-wrapped names. Null when
     * the value is blank or holds no usable name.
     */
    fun firstFamily(raw: String?): String? =
        raw?.split(',')
            ?.map { it.trim().trim('\'', '"').trim() }
            ?.firstOrNull { it.isNotEmpty() }

    /**
     * Declared family for [name], or null when the declaration table holds no
     * usable entry for it. Callers keep the raw name in that case so the
     * registry's own classification still answers. A declaration whose family
     * repeats the alias resolves to that same string, which is what the file
     * declares and never loops.
     */
    fun familyFor(name: String?, fontFaces: Map<String, OfficeFontFace>): String? {
        val key = normalize(name) ?: return null
        if (fontFaces.isEmpty()) return null
        val face = fontFaces[key]
            ?: fontFaces.values.firstOrNull { candidate ->
                normalize(candidate.name)?.equals(key, ignoreCase = true) == true
            }
        return face?.let { firstFamily(it.family) }
    }

    /** Lookup key: surrounding whitespace and quotes removed. */
    private fun normalize(name: String?): String? =
        name?.trim()?.trim('\'', '"')?.trim()?.takeIf { it.isNotEmpty() }
}
