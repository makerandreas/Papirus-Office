package com.example

import com.makerandreas.papirusoffice.data.navigation.NavigatorCategories
import com.makerandreas.papirusoffice.data.navigation.NavigatorCategoryAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Plan 3B (PR 13) guard for the Navigator's empty states.
 *
 * `NavigatorCategories` decides whether an empty category may say "No x in this
 * document" (a parser can see that class) or must say "Not yet available in
 * this build" (nothing can). The split is only trustworthy while it matches the
 * source tree, so this test re-derives it: a readable category must have a
 * producer-side constructor for its element class, and a not-yet-readable one
 * must have none. A later parser PR that fills a list without re-classifying
 * fails here instead of shipping a Navigator that still claims the document has
 * no bookmarks.
 *
 * Producers are every Kotlin file outside `data/navigation/`. The index engine
 * lives inside that package and reshapes `doc.sections` into `OfficeSection`;
 * counting it would classify sections as readable on the strength of a list no
 * parser fills.
 */
class NavigatorCategoryHonestyTest {

    private val sheetEmptyRowKey = Regex("NavigatorEmptyRow\\(\"([a-z]+)\"")

    private fun mainSourceRoot(): File {
        val candidates = listOf(File("src/main/java"), File("app/src/main/java"))
        return candidates.firstOrNull { it.isDirectory }
            ?: error("app source root not found from ${File(".").absolutePath}")
    }

    private fun kotlinFiles(root: File): List<File> =
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun producerFiles(root: File): List<File> =
        kotlinFiles(root).filterNot { it.path.replace(File.separatorChar, '/').contains("/data/navigation/") }

    /**
     * `ClassName(` outside its own declaration (`class OfficeImage(`), with the
     * file and line of each hit so a failure names where the class is built.
     */
    private fun producersConstructing(files: List<File>, elementClass: String): List<String> {
        val call = Regex("\\b$elementClass\\(")
        val declaration = Regex("\\b(class|object|interface)\\s+$")
        val hits = mutableListOf<String>()
        for (file in files) {
            file.readLines().forEachIndexed { index, line ->
                for (match in call.findAll(line)) {
                    if (declaration.containsMatchIn(line.substring(0, match.range.first))) continue
                    hits += "${file.name}:${index + 1}"
                }
            }
        }
        return hits
    }

    @Test
    fun readableCategoriesHaveAProducer() {
        val producers = producerFiles(mainSourceRoot())
        for (category in NavigatorCategories.ALL) {
            if (category.availability != NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS) continue
            assertTrue(
                "${category.key} is classified readable but names no element class",
                category.elementClasses.isNotEmpty()
            )
            val evidence = category.elementClasses.flatMap { producersConstructing(producers, it) }
            assertTrue(
                "${category.key} is classified readable, but nothing outside data/navigation/ constructs " +
                    category.elementClasses.joinToString() + "; reclassify it in NavigatorCategories",
                evidence.isNotEmpty()
            )
        }
    }

    @Test
    fun notYetReadableCategoriesHaveNoProducer() {
        val producers = producerFiles(mainSourceRoot())
        for (category in NavigatorCategories.ALL) {
            if (category.availability != NavigatorCategoryAvailability.NOT_READABLE_YET) continue
            for (elementClass in category.elementClasses) {
                val evidence = producersConstructing(producers, elementClass)
                assertTrue(
                    "${category.key} is classified not-yet-readable, but " + evidence.take(3) + " constructs " +
                        elementClass + "; the rows can appear now, so reclassify the category (and its owner plan)",
                    evidence.isEmpty()
                )
            }
        }
    }

    @Test
    fun sessionStateCategoriesClaimNoElementClass() {
        for (category in NavigatorCategories.ALL) {
            if (category.availability != NavigatorCategoryAvailability.SESSION_OR_LAYOUT_STATE) continue
            assertTrue(
                "${category.key} is session or layout state and cannot claim an element class",
                category.elementClasses.isEmpty()
            )
        }
    }

    @Test
    fun everySheetCategoryKeyIsDeclared() {
        val sheet = File(mainSourceRoot(), "com/example/ui/components/UniversalNavigatorSheet.kt")
        assertTrue("UniversalNavigatorSheet.kt not found at ${sheet.path}", sheet.isFile)
        val keys = sheetEmptyRowKey.findAll(sheet.readText()).map { it.groupValues[1] }.toSet()
        assertTrue("no NavigatorEmptyRow(...) call found in the sheet", keys.isNotEmpty())
        for (key in keys) {
            assertTrue(
                "sheet renders an undeclared Navigator category '$key'",
                NavigatorCategories.ALL.any { it.key == key }
            )
            // Strict lookup: an unknown key must fail loudly, not fall back.
            NavigatorCategories.of(key)
        }
    }

    @Test
    fun everyDeclaredCategoryIsRendered() {
        val sheet = File(mainSourceRoot(), "com/example/ui/components/UniversalNavigatorSheet.kt")
        val keys = sheetEmptyRowKey.findAll(sheet.readText()).map { it.groupValues[1] }.toSet()
        for (category in NavigatorCategories.ALL) {
            assertTrue(
                "NavigatorCategories declares '${category.key}' but the sheet never renders it",
                keys.contains(category.key)
            )
        }
    }

    @Test
    fun catalogKeysAreUnique() {
        val keys = NavigatorCategories.ALL.map { it.key }
        assertEquals("duplicate category keys silently drop entries in associateBy", keys.distinct(), keys)
    }
}
