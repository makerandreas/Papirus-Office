package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.OfficeFontFace

/**
 * Resolves an ODF font reference through the document's
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
     * First family of a family list such as `svg:font-family` or
     * `fo:font-family` (ODF 1.4 Part 3 19.532), which may be comma-separated
     * with quote-wrapped names: `'Times New Roman', serif` answers
     * `Times New Roman`. Null when the value is blank or holds no usable name.
     *
     * A blank entry is skipped rather than ending the search, so a malformed
     * leading separator does not hide a usable name behind it. No fixture
     * declares a list (audit-017 section 4.1), so this only affects files
     * outside the corpus.
     */
    fun firstFamily(raw: String?): String? =
        raw?.split(',')
            ?.map { it.trim().trim('\'', '"').trim() }
            ?.firstOrNull { it.isNotEmpty() }

    /**
     * The family a `style:font-name` (or `fo:font-family`) value names.
     *
     * [name] is the reference the style carries and [fontFaces] is
     * `DocumentStyles.fontFaces`. A declared name answers its declaration's
     * family. A name with no declaration, and a declaration with no usable
     * family, answer the reference itself, so
     * [com.makerandreas.papirusoffice.data.FontRegistry] can still classify it
     * instead of receiving null. Only a blank reference answers null.
     *
     * The lookup tries the exact identifier first and then ignores case,
     * which is how the style cascade in [SvXMLImport] resolves style names.
     */
    fun familyFor(name: String?, fontFaces: Map<String, OfficeFontFace>): String? {
        val requested = firstFamily(name) ?: return null
        val declaration = fontFaces[requested]
            ?: fontFaces.entries.firstOrNull { it.key.equals(requested, ignoreCase = true) }?.value
        return declaration?.let { firstFamily(it.family) } ?: requested
    }
}
