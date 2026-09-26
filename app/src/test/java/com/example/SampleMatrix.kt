package com.example

import java.io.File

/**
 * The six `tests/inky` sample pairs as plan 5 (roadmap PR 15/16a/16b)
 * describes them: reference page counts with their provenance, and the
 * per-format page-count windows. Values and sources are audit-007
 * (`anti-slop/audit-007-2026-09-26-sample-matrix.md`) §1 and §11.3.
 *
 * Windows are *recorded* here so the dump can print them; they are not
 * asserted until PR 16b (`PaginationFidelityTest`). The only pagination
 * guard in force is `Sample5UnifiedPaginationTest`'s 12..30.
 */
object SampleMatrix {

    data class Reference(
        /** Pages Microsoft 365 renders for the DOCX (user, 2026-09-26; `docProps/app.xml` where present). */
        val docxPages: Int,
        val docxSource: String,
        /** Pages Collabora Office renders for the ODT; null until the regenerated fixtures land. */
        val odtPages: Int?,
        val odtSource: String
    )

    data class Window(val min: Int, val max: Int) {
        operator fun contains(pages: Int): Boolean = pages in min..max
        override fun toString(): String = "$min..$max"
    }

    val references: Map<Int, Reference> = mapOf(
        1 to Reference(15, "M365 (user, 2026-09-26); app.xml carries no page count", null, "pending Collabora regeneration"),
        2 to Reference(23, "app.xml <Pages>23</Pages>, confirmed by M365", null, "pending Collabora regeneration"),
        3 to Reference(20, "M365 (user, 2026-09-26); WPS export carries no page count", null, "pending Collabora regeneration"),
        4 to Reference(10, "M365 (user, 2026-09-26); OnlyOffice export carries no page count", null, "pending Collabora regeneration"),
        5 to Reference(18, "app.xml <Pages>18</Pages>, confirmed by M365; roadmap §0", null, "pending Collabora regeneration"),
        6 to Reference(21, "M365 (user, 2026-09-26); roadmap v2 §0", null, "pending Collabora regeneration")
    )

    /** audit-007 §11.3, DOCX side. */
    val docxWindows: Map<Int, Window> = mapOf(
        1 to Window(12, 18),
        2 to Window(18, 28),
        3 to Window(16, 24),
        4 to Window(8, 12),
        5 to Window(15, 21),
        6 to Window(15, 26)
    )

    /** audit-007 §11.3, ODT side, provisional until the Collabora counts exist (Sample-1 bridges 9 and 15). */
    val odtWindows: Map<Int, Window> = mapOf(
        1 to Window(9, 18),
        2 to Window(15, 22),
        3 to Window(12, 20),
        4 to Window(7, 12),
        5 to Window(12, 21),
        6 to Window(15, 26)
    )

    val sampleNumbers: List<Int> = (1..6).toList()

    fun odtName(sample: Int): String = "Sample-$sample.odt"

    fun docxName(sample: Int): String = "Sample-$sample.docx"

    /** All twelve fixture names, ODT first per pair. */
    val fileNames: List<String> = sampleNumbers.flatMap { listOf(odtName(it), docxName(it)) }

    fun referenceFor(fileName: String): Int? {
        val sample = fileName.removePrefix("Sample-").substringBefore('.').toIntOrNull() ?: return null
        val ref = references[sample] ?: return null
        return if (fileName.endsWith(".odt", ignoreCase = true)) ref.odtPages else ref.docxPages
    }

    fun windowFor(fileName: String): Window? {
        val sample = fileName.removePrefix("Sample-").substringBefore('.').toIntOrNull() ?: return null
        return if (fileName.endsWith(".odt", ignoreCase = true)) odtWindows[sample] else docxWindows[sample]
    }

    /** Resolves `tests/inky/<name>` from the module, project or repository working directory. */
    fun findTestFile(name: String): File {
        val candidate = "tests/inky/$name"
        for (base in listOf("", "../", "../../")) {
            val f = File(base + candidate)
            if (f.exists() && f.length() > 0) return f
        }
        return File(candidate)
    }
}
