package com.example

import com.makerandreas.papirusoffice.data.DocumentImages
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.PageElementLayout
import com.makerandreas.papirusoffice.data.PageLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Plan 6C (F-3): sizing, lookup and predecode window, without Compose or Coil. */
class DocumentImagesTest {

    @get:Rule val temp = TemporaryFolder()

    private fun file(name: String, bytes: Int = 16): File =
        temp.newFile(name).apply { writeBytes(ByteArray(bytes)) }

    @Test
    fun declaredExtentIsTheBoxAndMissingExtentUsesThePaginatorDefault() {
        assertEquals(DocumentImages.Box(292f, 165f), DocumentImages.box(292f, 165f))
        assertEquals(
            DocumentImages.Box(DocumentImages.DEFAULT_WIDTH_UNITS, DocumentImages.DEFAULT_HEIGHT_UNITS),
            DocumentImages.box(0f, -1f)
        )
    }

    @Test
    fun decodeSizeFollowsExtentAndDensityAndIsCapped() {
        val box = DocumentImages.Box(200f, 100f)
        assertEquals(600 to 300, DocumentImages.decodeSizePx(box, density = 2f))
        val huge = DocumentImages.decodeSizePx(DocumentImages.Box(4000f, 1000f), density = 3f)
        assertEquals(DocumentImages.MAX_DECODE_EDGE_PX, huge.first)
        assertEquals(512, huge.second)
        val tiny = DocumentImages.decodeSizePx(DocumentImages.Box(0.01f, 0.01f), density = 1f)
        assertEquals(1 to 1, tiny)
    }

    @Test
    fun cacheKeyChangesWhenTheStoredFileIsRewritten() {
        val f = file("a.png")
        val size = 10 to 10
        val before = DocumentImages.cacheKey(f, size)
        assertEquals(before, DocumentImages.cacheKey(f, size))
        f.writeBytes(ByteArray(32))
        f.setLastModified(f.lastModified() + 5_000)
        assertNotEquals(before, DocumentImages.cacheKey(f, size))
        assertNotEquals(DocumentImages.cacheKey(f, size), DocumentImages.cacheKey(f, 20 to 20))
    }

    @Test
    fun resolvePrefersTheElementFileThenStoreAliasesAndReportsMissing() {
        val own = file("own.png")
        val stored = file("stored.png")
        val store = mapOf("word/media/image1.png" to stored, "image2.png" to stored)
        assertEquals(own, DocumentImages.resolve(own, "word/media/image1.png", store))
        assertEquals(stored, DocumentImages.resolve(File(temp.root, "gone.png"), "word/media/image1.png", store))
        assertEquals(stored, DocumentImages.resolve(null, "./word/media/image1.png", store))
        assertEquals(stored, DocumentImages.resolve(null, "Pictures/IMAGE2.PNG".lowercase(), store))
        assertEquals(stored, DocumentImages.resolve(null, "media/image2.png", store))
        assertNull(DocumentImages.resolve(null, "word/media/image9.png", store))
        stored.delete()
        assertNull("a deleted store file is missing, not a stale hit",
            DocumentImages.resolve(null, "word/media/image1.png", store))
    }

    @Test
    fun predecodeWindowIsFirstScreenfulPlusOnePage() {
        val pages = List(6) { 500f }
        // Screen shows only part of page 0: page 0 plus the next one.
        assertEquals(0..1, DocumentImages.predecodePageWindow(pages, 16f, 400f))
        // Screen reaches into page 1 (top at 516): pages 0-1 plus page 2.
        assertEquals(0..2, DocumentImages.predecodePageWindow(pages, 16f, 800f))
        // A page top exactly at the screen edge is not on screen.
        assertEquals(0..1, DocumentImages.predecodePageWindow(pages, 16f, 516f))
        assertEquals(0..0, DocumentImages.predecodePageWindow(listOf(500f), 16f, 2000f))
        assertEquals(0..5, DocumentImages.predecodePageWindow(pages, 16f, 10_000f))
        assertTrue(DocumentImages.predecodePageWindow(emptyList(), 16f, 800f).isEmpty())
    }

    @Test
    fun predecodeTargetsTakeResolvableImagesOnWindowPagesOnce() {
        val a = file("a.png")
        val b = file("b.png")
        val c = file("c.png")
        fun img(path: String, f: File?, w: Float = 100f, h: Float = 50f) =
            PageElementLayout(element = OfficeImage(imagePath = path, imageFile = f, widthDp = w, heightDp = h))
        val pages = listOf(
            PageLayout(1, elements = listOf(
                PageElementLayout(element = OfficeParagraph(text = "x")),
                img("a.png", a),
                img("missing.png", null)
            )),
            PageLayout(2, elements = listOf(img("a.png", a), img("b.png", b, 0f, 0f))),
            PageLayout(3, elements = listOf(img("c.png", c)))
        )
        val targets = DocumentImages.predecodeTargets(pages, 0..1, emptyMap())
        assertEquals(listOf(a, b), targets.map { it.file })
        assertEquals(DocumentImages.Box(100f, 50f), targets[0].box)
        assertEquals(DocumentImages.box(0f, 0f), targets[1].box)
        assertEquals(listOf(a, b, c), DocumentImages.predecodeTargets(pages, 0..9, emptyMap()).map { it.file })
    }
}
