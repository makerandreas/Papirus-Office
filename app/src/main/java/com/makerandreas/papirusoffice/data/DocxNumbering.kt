package com.makerandreas.papirusoffice.data

// `Charsets.UTF_8` below is `kotlin.text.Charsets`, a Kotlin default import for
// JVM targets. There is no `java.nio.charset.Charsets` type: the Java classes are
// `Charset` and `StandardCharsets`. Adding `import java.nio.charset.Charsets`
// here breaks `:app:compileDebugKotlin` (it did, twice, on PR #35).
import com.makerandreas.papirusoffice.data.util.ZipSafe
import com.makerandreas.papirusoffice.data.util.readCappedBytes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Reader for `word/numbering.xml` (ECMA-376 Part 1 §17.9, `[MS-OI29500]` §17.9 p.123-126).
 *
 * Plan 8B. Produces one [NumberingSpec] per `w:num w:numId` so the parser can
 * resolve a paragraph's `DocxNumberingRef(numId, ilvl)` to a label through the
 * existing [NumberingCounterState].
 *
 * Bounds enforced per Microsoft's implementation notes:
 *  * `abstractNumId` is ignored when negative ([MS-OI29500] §17.9.1, p.123)
 *  * `ilvl` is clipped to 0..255 (§17.9.4, p.123)
 *  * `lvlText` supports at most nine `%[1-9]` placeholders and at most 31
 *    characters after substitution (§17.9.11, p.124)
 *  * A numbering level's `pPr` accepts only `jc`, `ind` and `tabs` (§17.9.22,
 *    p.125); other children are ignored rather than crashing.
 *  * `numId 0` is the suppression sentinel and never has a spec.
 */
object DocxNumberingReader {

    /** Maximum number of levels Word allows per abstractNum ([MS-OI29500] §17.9.11). */
    private const val MAX_LEVELS = 9

    /** [MS-OI29500] §17.9.18: at least 0; 0 suppresses numbering. */
    private const val MIN_NUM_ID = 1

    data class NumberingParseResult(
        /** numId (>=1) -> NumberingSpec for rendering. */
        val numSpecs: Map<Int, NumberingSpec> = emptyMap(),
        /** abstractNumId -> base NumberingSpec (before lvlOverride); used internally. */
        val abstractSpecs: Map<Int, NumberingSpec> = emptyMap(),
        /** numId -> abstractNumId reference. */
        val numToAbstract: Map<Int, Int> = emptyMap(),
        /** numId -> per-ilvl startOverride (when a w:lvlOverride is present). */
        val startOverrides: Map<Pair<Int, Int>, Int> = emptyMap(),
        /**
         * Why the read produced nothing, when it was not simply "no numbering part".
         *
         * A reader that swallows a parse failure and reports "this document has
         * no lists" makes every list label silently vanish, and the two states
         * look identical to a caller. Naming the throwable keeps them apart
         * without changing the empty-result contract callers already rely on.
         */
        val parseError: String? = null
    ) {
        fun isEmpty() = numSpecs.isEmpty()
    }

    fun read(file: File): NumberingParseResult {
        if (!file.exists() || !file.name.endsWith(".docx", ignoreCase = true)) return NumberingParseResult()
        return try {
            var numberingXml: String? = null
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/numbering.xml") {
                        numberingXml = zip.readCappedBytes(ZipSafe.MAX_ZIP_ENTRY_BYTES)
                            .toString(Charsets.UTF_8)
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            numberingXml?.let { parseNumberingXml(it) }
                ?: NumberingParseResult(parseError = "no word/numbering.xml entry")
        } catch (t: Exception) {
            NumberingParseResult(parseError = "${t::class.simpleName}: ${t.message}")
        }
    }

    private fun parseNumberingXml(xml: String): NumberingParseResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), "UTF-8")

        // abstractNumId -> raw (pre-override) NumberingSpec
        val abstractSpecs = LinkedHashMap<Int, NumberingSpec>()
        // abstractNumId -> abstract display name (w:name)
        val abstractNames = LinkedHashMap<Int, String>()
        // Temporary level accumulators, keyed by abstractNumId
        val abstractLevels = LinkedHashMap<Int, LinkedHashMap<Int, NumberingLevelSpec>>()

        // numId -> abstractNumId
        val numToAbstract = LinkedHashMap<Int, Int>()
        // (numId, ilvl) -> startOverride value
        val startOverrides = LinkedHashMap<Pair<Int, Int>, Int>()

        var inAbstractNum = false
        var currentAbstractId: Int? = null
        var currentAbstractName: String? = null
        var currentLevels = LinkedHashMap<Int, NumberingLevelSpec>()

        var inNum = false
        var currentNumId: Int? = null
        var currentNumAbstractId: Int? = null

        var inLvl = false
        var currentLvlIlvl: Int = 0
        // Level accumulators
        var lvlStart: Int = 1
        var lvlNumFmt: String = "decimal"
        var lvlIsBullet: Boolean = false
        var lvlLvlText: String = "%1."
        var lvlJc: String = "left"
        var lvlSuff: String = "tab"
        var lvlIsLgl: Boolean = false
        var lvlBulletChar: String = "\u2022"
        var lvlIndentLeft: Float? = null
        var lvlIndentHanging: Float? = null
        var lvlFontFamily: String? = null
        var lvlFontSizeSp: Float? = null
        var lvlIsBold: Boolean? = null
        var lvlIsItalic: Boolean? = null
        var lvlColorHex: String? = null

        var inLvlPPr = false
        var inLvlRPr = false

        fun flushLevel() {
            if (!inLvl) return
            val ilvl = currentLvlIlvl.coerceIn(0, NumberingSpec.MAX_NUMBERING_LEVELS - 1)
            val levelNumber = ilvl + 1
            // Parse lvlText into prefix, placeholders, suffix
            val parsed = parseLvlText(lvlLvlText, lvlIsBullet)
            val displayLevels = parsed.displayLevels.coerceAtLeast(1).coerceAtMost(MAX_LEVELS)
            val indentStartUnits = lvlIndentLeft
            val hangingUnits = lvlIndentHanging
            // Bullet glyph for bullet formats; when numFmt is "bullet", prefer the
            // explicitly-declared bulletChar.  Otherwise let the formatter default.
            val bulletChar = when {
                lvlIsBullet -> lvlBulletChar.ifEmpty { "\u2022" }
                lvlNumFmt.equals("bullet", ignoreCase = true) -> lvlBulletChar.ifEmpty { "\u2022" }
                else -> "\u2022"
            }
            val spec = NumberingLevelSpec(
                level = levelNumber,
                isBullet = lvlIsBullet || lvlNumFmt.equals("bullet", ignoreCase = true),
                numFormat = mapNumFmt(lvlNumFmt),
                numPrefix = parsed.prefix,
                numSuffix = parsed.suffix,
                displayLevels = displayLevels,
                startValue = lvlStart.coerceAtLeast(0),
                bulletChar = bulletChar,
                fontFamily = lvlFontFamily,
                fontSizeSp = lvlFontSizeSp,
                isBold = lvlIsBold,
                isItalic = lvlIsItalic,
                colorHex = lvlColorHex,
                indentStartUnits = indentStartUnits,
                firstLineIndentUnits = hangingUnits?.let { -it },
                labelFollowedBy = lvlSuff
            )
            currentLevels[levelNumber] = spec
            inLvl = false
        }

        fun resetLevelAccumulators() {
            lvlStart = 1
            lvlNumFmt = "decimal"
            lvlIsBullet = false
            lvlLvlText = "%1."
            lvlJc = "left"
            lvlSuff = "tab"
            lvlIsLgl = false
            lvlBulletChar = "\u2022"
            lvlIndentLeft = null
            lvlIndentHanging = null
            lvlFontFamily = null
            lvlFontSizeSp = null
            lvlIsBold = null
            lvlIsItalic = null
            lvlColorHex = null
            inLvlPPr = false
            inLvlRPr = false
        }

        fun flushAbstract() {
            flushLevel()
            if (inAbstractNum) {
                val id = currentAbstractId
                if (id != null && id >= 0) {
                    val name = currentAbstractName ?: "abstractNum$id"
                    val levels = currentLevels.toMap()
                    abstractLevels[id] = LinkedHashMap(levels)
                    abstractSpecs[id] = NumberingSpec(
                        name = "abstractNum$id",
                        displayName = name,
                        levels = levels
                    )
                    abstractNames[id] = name
                }
            }
            inAbstractNum = false
            currentAbstractId = null
            currentAbstractName = null
            currentLevels = LinkedHashMap()
        }

        fun flushNum() {
            if (inNum) {
                val nid = currentNumId
                val aid = currentNumAbstractId
                if (nid != null && nid >= MIN_NUM_ID && aid != null && aid >= 0) {
                    numToAbstract[nid] = aid
                }
            }
            inNum = false
            currentNumId = null
            currentNumAbstractId = null
        }

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name?.lowercase(Locale.ROOT)?.substringAfter(':')
            if (eventType == XmlPullParser.START_TAG) {
                when (name) {
                    "abstractnum" -> {
                        flushAbstract()
                        inAbstractNum = true
                        currentAbstractId = attrInt(parser, "abstractnumid")
                        currentAbstractName = null
                        currentLevels = LinkedHashMap()
                    }
                    "name" -> if (inAbstractNum) {
                        currentAbstractName = attr(parser, "val")
                    }
                    "multileveltype" -> { /* informational, not load-bearing */ }
                    "lvl" -> {
                        if (inAbstractNum) {
                            flushLevel()
                            resetLevelAccumulators()
                            inLvl = true
                            currentLvlIlvl = attrInt(parser, "ilvl") ?: 0
                            // tplc is template GUID; ignored
                        } else if (inNum) {
                            // w:lvl inside a w:lvlOverride
                            val ilvl = attrInt(parser, "ilvl") ?: 0
                            // Start override is on the override parent; start on the
                            // lvl inside the override matches the start value but we
                            // already captured it at <w:startOverride>.
                            // In the unusual case the override provides a full <w:lvl>,
                            // merge only the start value - the base level keeps its
                            // other format properties (Word keeps the original formatting
                            // but restarts the count).
                            val start = attrInt(parser, "val")
                            if (start != null) {
                                val nid = currentNumId ?: 0
                                if (nid >= MIN_NUM_ID) {
                                    startOverrides[nid to (ilvl + 1)] = start
                                }
                            }
                        }
                    }
                    "start" -> if (inLvl) {
                        lvlStart = attrInt(parser, "val")?.coerceAtLeast(0) ?: 1
                    }
                    "numfmt" -> if (inLvl) {
                        val fmt = attr(parser, "val") ?: "decimal"
                        lvlNumFmt = fmt
                        lvlIsBullet = fmt.equals("bullet", ignoreCase = true)
                    }
                    "lvltext" -> if (inLvl) {
                        lvlLvlText = attr(parser, "val") ?: ""
                    }
                    "lvljc" -> if (inLvl) {
                        lvlJc = attr(parser, "val") ?: "left"
                    }
                    "suff" -> if (inLvl) {
                        lvlSuff = attr(parser, "val") ?: "tab"
                    }
                    "islegal" -> if (inLvl) {
                        val raw = attr(parser, "val")
                        lvlIsLgl = raw == null || raw.lowercase(Locale.ROOT) !in setOf("0", "false", "off")
                    }
                    "pPr" -> if (inLvl) {
                        inLvlPPr = true
                    }
                    "rPr" -> if (inLvl) {
                        inLvlRPr = true
                    }
                    "ind" -> if (inLvlPPr) {
                        // Word allows only ind (and jc/tabs) in level pPr per [MS-OI29500] p.125
                        lvlIndentLeft = attrInt(parser, "left")?.let { LayoutUnits.twipsToUnits(it) }
                        lvlIndentHanging = attrInt(parser, "hanging")?.let { LayoutUnits.twipsToUnits(it) }
                    }
                    "jc" -> if (inLvlPPr) {
                        // ignored for label formatting; alignment of paragraph content
                        // is handled at the paragraph style level, not the label.
                    }
                    "tabs" -> if (inLvlPPr) {
                        // tab stops are a property of the paragraph layout, not the
                        // label string; skip without crashing.
                    }
                    "rfonts" -> if (inLvlRPr) {
                        // ascii/hAnsi are the label font; eastAsia/cs ignored
                        lvlFontFamily = attr(parser, "ascii") ?: attr(parser, "hansi")
                    }
                    "sz" -> if (inLvlRPr) {
                        val halfPoints = attrInt(parser, "val")
                        lvlFontSizeSp = halfPoints?.let { LayoutUnits.halfPointsToPt(it) }
                    }
                    "b" -> if (inLvlRPr) {
                        val raw = attr(parser, "val")
                        lvlIsBold = raw == null || raw.lowercase(Locale.ROOT) !in setOf("0", "false", "off")
                    }
                    "i" -> if (inLvlRPr) {
                        val raw = attr(parser, "val")
                        lvlIsItalic = raw == null || raw.lowercase(Locale.ROOT) !in setOf("0", "false", "off")
                    }
                    "color" -> if (inLvlRPr) {
                        lvlColorHex = attr(parser, "val")
                    }
                    // w:lvlPicBulletId, w:lvlOverride are handled via their enclosing tags.
                    "num" -> {
                        flushNum()
                        inNum = true
                        currentNumId = attrInt(parser, "numid")
                        currentNumAbstractId = null
                    }
                    "abstractnumid" -> if (inNum) {
                        currentNumAbstractId = attrInt(parser, "val")
                    }
                    "lvloverride" -> if (inNum) {
                        val ilvl = attrInt(parser, "ilvl")
                        // w:startOverride is an attribute on w:lvlOverride itself.
                        val startVal = attrInt(parser, "startoverride")
                        val nid = currentNumId
                        if (nid != null && nid >= MIN_NUM_ID && ilvl != null && startVal != null) {
                            startOverrides[nid to (ilvl + 1)] = startVal.coerceAtLeast(0)
                        }
                    }
                    "startoverride" -> {
                        // consumed as attribute; ignore empty element form.
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                when (name) {
                    "lvl" -> {
                        if (inLvl) {
                            flushLevel()
                            resetLevelAccumulators()
                        }
                    }
                    "pPr" -> inLvlPPr = false
                    "rPr" -> inLvlRPr = false
                    "abstractnum" -> flushAbstract()
                    "num" -> flushNum()
                }
            }
            eventType = parser.next()
        }
        flushLevel()
        flushAbstract()
        flushNum()

        // Build numSpecs by copying abstract levels and applying startOverrides
        val numSpecs = LinkedHashMap<Int, NumberingSpec>()
        for ((nid, aid) in numToAbstract) {
            val base = abstractSpecs[aid] ?: continue
            val levels = LinkedHashMap<Int, NumberingLevelSpec>()
            for ((lvl, spec) in base.levels) {
                val override = startOverrides[nid to lvl]
                levels[lvl] = if (override != null) spec.copy(startValue = override) else spec
            }
            numSpecs[nid] = base.copy(
                name = "num$nid",
                displayName = base.displayName ?: "abstractNum$aid",
                levels = levels
            )
        }
        return NumberingParseResult(
            numSpecs = numSpecs,
            abstractSpecs = abstractSpecs,
            numToAbstract = numToAbstract,
            startOverrides = startOverrides
        )
    }

    /** Pull an attribute by its lowercased local name. */
    private fun attr(parser: XmlPullParser, name: String): String? {
        val target = name.lowercase(Locale.ROOT)
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i).lowercase(Locale.ROOT).substringAfter(':')
            if (attrName == target) return parser.getAttributeValue(i)
        }
        return null
    }

    private fun attrInt(parser: XmlPullParser, name: String): Int? =
        attr(parser, name)?.toIntOrNull()

    /**
     * Parse an `lvlText` like `"%1."`, `"%1.%2"`, `"BAB %1"`, `""` (bullet/no-number)
     * into (prefix, displayLevels, suffix). Supports up to nine placeholders.
     */
    private data class ParsedLvlText(val prefix: String, val displayLevels: Int, val suffix: String)

    private fun parseLvlText(raw: String, isBullet: Boolean): ParsedLvlText {
        if (isBullet || raw.isBlank()) {
            return ParsedLvlText(prefix = "", displayLevels = 1, suffix = "")
        }
        // Find first %n placeholder and last %n placeholder.
        val placeholderRegex = Regex("%([1-9])")
        val matches = placeholderRegex.findAll(raw).toList()
        if (matches.isEmpty()) {
            // No placeholder -> literal text as prefix, no number.
            return ParsedLvlText(prefix = raw.take(31), displayLevels = 1, suffix = "")
        }
        val firstIdx = matches.first().range.first
        val lastIdx = matches.last().range.last
        val maxLevel = matches.maxOf { it.groupValues[1].toInt() }
        val minLevel = matches.minOf { it.groupValues[1].toInt() }
        val prefix = raw.substring(0, firstIdx)
        val suffix = raw.substring(lastIdx + 1)
        // [MS-OI29500] §17.9.11: at most 9 %[1-9] and at most 31 chars after substitution.
        // We enforce the cap at formatting time by taking prefix/core+suffix up to 31.
        val displayLevels = (maxLevel - minLevel + 1).coerceAtLeast(1).coerceAtMost(MAX_LEVELS)
        return ParsedLvlText(
            prefix = prefix,
            displayLevels = displayLevels,
            suffix = suffix
        )
    }

    /**
     * Map a `w:numFmt` value to the format string `NumberingFormatter.formatOrdinal`
     * understands: "1"/"a"/"A"/"i"/"I"/"none"/"bullet".
     */
    private fun mapNumFmt(fmt: String): String = when (fmt.trim().lowercase(Locale.ROOT)) {
        "decimal", "decimalzerowidth", "arabic", "numbered", "1", "ordinal", "cardinaltext",
        "ordinaltext" -> "1"
        "lowerletter", "loweralpha", "a" -> "a"
        "upperletter", "upperalpha", "a_" -> "A"
        "lowerroman", "i" -> "i"
        "upperroman", "i_" -> "I"
        "bullet", "none", "", "bullettext" -> "none"
        // Other formats (chineseCounting, hebrew1, etc.) are not localised yet; fall
        // back to arabic so the reader degrades gracefully instead of crashing.
        else -> "1"
    }
}
