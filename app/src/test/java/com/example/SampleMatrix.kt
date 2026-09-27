package com.example

import java.io.File

/**
 * The six `tests/inky` sample pairs as plan 5 describes them: reference page
 * counts with their provenance, and the per-format page-count windows.
 *
 * Re-baselined in PR 16 (Plan 5b) against the regenerated fixtures: every
 * `.docx` is saved by Microsoft 365 (`docProps/app.xml` `<Application>`
 * "Microsoft Office Word", `<Pages>` read by the user in the M365 app), every
 * `.odt` by Collabora Office 26.04 (`meta:page-count`). Values and sources are
 * audit-008 (`anti-slop/audit-008-2026-09-27-fixture-rebaseline.md`) §1 and §4,
 * which supersedes audit-007 §1 and §11.3.
 *
 * Windows are *recorded* here so the dump can print them; they are not
 * asserted until Plan 5e (`PaginationFidelityTest`). The only pagination
 * guard in force is `Sample5UnifiedPaginationTest`'s interim window.
 */
object SampleMatrix {

    data class Reference(
        /** Pages Microsoft 365 renders for the DOCX (`docProps/app.xml` `<Pages>`, confirmed by the user in M365). */
        val docxPages: Int,
        val docxSource: String,
        /** Pages Collabora Office renders for the ODT (`meta.xml` `meta:page-count`, confirmed by the user). */
        val odtPages: Int,
        val odtSource: String
    )

    data class Window(val min: Int, val max: Int) {
        operator fun contains(pages: Int): Boolean = pages in min..max
        override fun toString(): String = "$min..$max"
    }

    val references: Map<Int, Reference> = mapOf(
        1 to Reference(15, "M365, app.xml <Pages>15</Pages>", 15, "Collabora 26.04, meta:page-count 15"),
        2 to Reference(23, "M365, app.xml <Pages>23</Pages>", 23, "Collabora 26.04, meta:page-count 23"),
        3 to Reference(22, "M365, app.xml <Pages>22</Pages> (user re-checked; audit-007 had 20)", 22, "Collabora 26.04, meta:page-count 22"),
        4 to Reference(10, "M365, app.xml <Pages>10</Pages>", 11, "Collabora 26.04, meta:page-count 11"),
        5 to Reference(18, "M365, app.xml <Pages>18</Pages>", 19, "Collabora 26.04, meta:page-count 19"),
        6 to Reference(21, "M365, app.xml <Pages>21</Pages>", 22, "Collabora 26.04, meta:page-count 22")
    )

    /**
     * audit-008 §4, DOCX side. Staged windows of roughly ±20 % around the
     * reference; only Sample-3 moved (16..24 around 20 became 18..26 around 22).
     */
    val docxWindows: Map<Int, Window> = mapOf(
        1 to Window(12, 18),
        2 to Window(18, 28),
        3 to Window(18, 26),
        4 to Window(8, 12),
        5 to Window(15, 21),
        6 to Window(15, 26)
    )

    /**
     * audit-008 §4, ODT side, re-derived from the Collabora counts with the
     * same staging as the DOCX side. The audit-007 provisional windows for
     * Sample-2 (15..22) and Sample-3 (12..20) excluded their own references.
     */
    val odtWindows: Map<Int, Window> = mapOf(
        1 to Window(12, 18),
        2 to Window(18, 28),
        3 to Window(18, 26),
        4 to Window(9, 13),
        5 to Window(15, 23),
        6 to Window(17, 27)
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
