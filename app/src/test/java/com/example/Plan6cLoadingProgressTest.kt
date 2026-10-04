package com.example

import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentImages
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.LoadingStage
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Collections

/**
 * Plan 6C (F-4): the loading screen reports real stages in pipeline order,
 * and the open path contains no timed steps. The timings printed here are a
 * JVM smoke measurement for the CI log, not a device latency claim.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan6cLoadingProgressTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun repoRoot(): File {
        var root: File? = File(".").absoluteFile
        while (root != null && !File(root, "settings.gradle.kts").exists() && !File(root, ".git").exists()) root = root.parentFile
        return root ?: File(".").absoluteFile
    }

    private fun fixture(name: String): File =
        File(repoRoot(), "tests/inky/$name").also { assertTrue("Missing fixture: $it", it.isFile) }

    private val freshParseStages = listOf(
        LoadingStage.OPENING_PACKAGE,
        LoadingStage.VALIDATING,
        LoadingStage.EXTRACTING_MEDIA,
        LoadingStage.READING_STYLES,
        LoadingStage.READING_BODY
    )

    @Test
    fun stageOrdinalsFollowThePipeline() {
        val order = LoadingStage.values().toList()
        assertEquals(freshParseStages + LoadingStage.CACHED + LoadingStage.LAYOUT, order)
        assertEquals("percent rises with the pipeline", order.map { it.percent }.sorted(), order.map { it.percent })
        order.forEach { assertTrue(context.getString(it.messageRes).isNotBlank()) }
    }

    @Test
    fun freshAndCachedOpensAnnounceRealStagesInOrder() = runBlocking {
        for (name in listOf("Sample-6.odt", "Sample-6.docx")) {
            val parser = OfficeDocumentParser(context)
            val seen = Collections.synchronizedList(mutableListOf<LoadingStage>())
            parser.stageListener = { seen += it }

            val parsed = parser.parseDocument(fixture(name), bypassCache = true)
            assertFalse("$name failed: ${parsed.failureReason}", parsed.isParsingFailed)
            assertEquals("$name fresh open", freshParseStages, seen.toList())

            seen.clear()
            parser.parseDocument(fixture(name))
            assertEquals("$name cached open", listOf(LoadingStage.CACHED), seen.toList())
        }
    }

    @Test
    fun openAndFirstLayoutTimingSmoke() = runBlocking {
        for (name in listOf("Sample-6.odt", "Sample-6.docx")) {
            val parser = OfficeDocumentParser(context)
            val t0 = System.nanoTime()
            val parsed = parser.parseDocument(fixture(name), bypassCache = true)
            val t1 = System.nanoTime()
            val doc = parsed.toOfficeDocument()
            val layout = LayoutEngine(doc.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK).performLayout(doc)
            val t2 = System.nanoTime()
            val window = DocumentImages.predecodePageWindow(layout.pages.map { it.heightDp * 0.45f }, 16f, 800f)
            val targets = DocumentImages.predecodeTargets(layout.pages, window, parsed.extractedImages)
            targets.forEach { assertTrue("predecode target must exist: ${it.file}", it.file.isFile) }
            println(
                "Plan6C timing $name: parse=${(t1 - t0) / 1_000_000} ms, " +
                    "layout=${(t2 - t1) / 1_000_000} ms, pages=${layout.pages.size}, " +
                    "predecodeWindow=$window, predecodeImages=${targets.size}"
            )
            assertTrue("$name produced no pages", layout.pages.isNotEmpty())
            // A generous JVM ceiling that only catches a reintroduced sleep or a hang.
            assertTrue("$name open+layout took ${(t2 - t0) / 1_000_000} ms", (t2 - t0) < 60_000_000_000L)
        }
    }

    // region source scan: no timed steps in the open path

    private fun mainSource(path: String): String =
        File(repoRoot(), "app/src/main/java/$path").readText()

    private fun block(src: String, start: String, end: String): String {
        val from = src.indexOf(start)
        assertTrue("marker not found: $start", from >= 0)
        val to = src.indexOf(end, from + start.length)
        assertTrue("marker not found: $end", to > from)
        return src.substring(from, to)
    }

    @Test
    fun inkyLoadingSequenceHasNoArtificialDelay() {
        val inky = mainSource("com/example/modules/inky/InkyModule.kt")
        val loading = block(inky, "val runDocumentLoading", "val handleOpenDocument")
        assertFalse("runDocumentLoading must not sleep", loading.contains("delay("))

        // The Recents/open path: its only delay is the scroll restore, which
        // runs in its own coroutine after the content is already set.
        val open = block(inky, "LaunchedEffect(com.example.MainActivity.openedFileNonce", "DisposableEffect(docxParser)")
        val lines = open.lines()
        lines.forEachIndexed { i, line ->
            if (line.contains("delay(")) {
                assertTrue(
                    "unexpected delay in the open path: ${line.trim()}",
                    lines.getOrNull(i + 1)?.contains("scrollState.scrollTo") == true
                )
            }
        }
    }

    @Test
    fun parserOpenPathHasNoTimerDrivenProgress() {
        val parser = mainSource("com/makerandreas/papirusoffice/data/OfficeDocumentParser.kt")
        val parse = block(parser, "suspend fun parseDocument(file: File, bypassCache", "\n    }\n")
        assertFalse(parse.contains("delay("))
        assertFalse("no event-count progress", parser.contains("eventCount % "))
        assertFalse(parser.contains("loading_status_still_processing"))
        val strings = File(repoRoot(), "app/src/main/res/values/strings.xml").readText()
        for (gone in listOf("loading_status_odf", "loading_status_rendering", "loading_status_preparing",
                "loading_status_still_processing")) {
            assertFalse("$gone was the text of a timed step", strings.contains("\"$gone\""))
        }
    }

    // endregion
}
