package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentImages
import com.makerandreas.papirusoffice.data.DocumentMetadata
import com.makerandreas.papirusoffice.data.DocumentSerializer
import com.makerandreas.papirusoffice.data.DocxDocumentParser
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficeParagraph
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan6ImageFoundationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun fixture(name: String): File {
        var root: File? = File(".").absoluteFile
        while (root != null && !File(root, ".git").exists()) root = root.parentFile
        val file = File(root ?: File("."), "tests/inky/$name")
        assertTrue("Missing fixture: ${file.absolutePath}", file.isFile)
        return file
    }

    @Test
    fun sample6OdtAndDocxImagesResolveToEquivalentLayoutUnits() = runBlocking {
        val parser = com.makerandreas.papirusoffice.data.OfficeDocumentParser(context)
        val odt = parser.parseDocument(fixture("Sample-6.odt"), bypassCache = true)
        val docx = parser.parseDocument(fixture("Sample-6.docx"), bypassCache = true)
        assertFalse("ODT parse failed: ${odt.failureReason}", odt.isParsingFailed)
        assertFalse("DOCX parse failed: ${docx.failureReason}", docx.isParsingFailed)

        val odtImages = odt.elements.filterIsInstance<OfficeDocumentElement.ImageElement>()
        val docxImages = docx.elements.filterIsInstance<OfficeDocumentElement.ImageElement>()
        assertEquals("Sample-6 has three image frames in each format", 3, odtImages.size)
        assertEquals("Sample-6 has three image frames in each format", odtImages.size, docxImages.size)

        odtImages.zip(docxImages).forEachIndexed { index, (odtImage, docxImage) ->
            assertTrue("DOCX image $index width must be parsed", docxImage.widthDp > 0f)
            assertTrue("DOCX image $index height must be parsed", docxImage.heightDp > 0f)
            assertEquals("image $index width ODT/DOCX", odtImage.widthDp, docxImage.widthDp, 0.1f)
            assertEquals("image $index height ODT/DOCX", odtImage.heightDp, docxImage.heightDp, 0.1f)
        }

        val converted = docx.toOfficeDocument().body.elements.filterIsInstance<OfficeImage>()
        assertEquals(docxImages.map { it.widthDp to it.heightDp }, converted.map { it.widthDp to it.heightDp })
    }

    @Test
    fun legacyDrawingExtentFallbackIsAppliedWhenWordExtentIsAbsent() = runBlocking {
        val source = fixture("Sample-6.docx")
        val modified = File(context.cacheDir, "plan6-a-ext-fallback.docx")
        ZipInputStream(source.inputStream()).use { input ->
            ZipOutputStream(modified.outputStream()).use { output ->
                var entry = input.nextEntry
                while (entry != null) {
                    val bytes = input.readBytes()
                    val content = if (entry.name == "word/document.xml") {
                        bytes.toString(Charsets.UTF_8)
                            .replace(Regex("<wp:extent[^>]*/>"), "<wp:extent/>")
                            .toByteArray(Charsets.UTF_8)
                    } else bytes
                    output.putNextEntry(java.util.zip.ZipEntry(entry.name))
                    output.write(content)
                    output.closeEntry()
                    input.closeEntry()
                    entry = input.nextEntry
                }
            }
        }
        try {
            val parsed = com.makerandreas.papirusoffice.data.OfficeDocumentParser(context)
                .parseDocument(modified, bypassCache = true)
            assertFalse("modified DOCX fixture failed: ${parsed.failureReason}", parsed.isParsingFailed)
            val images = parsed.elements.filterIsInstance<OfficeDocumentElement.ImageElement>()
            assertEquals(3, images.size)
            assertTrue("DrawingML a:ext fallback should fill the missing wp:extent dimensions", images.all {
                it.widthDp > 0f && it.heightDp > 0f
            })
        } finally {
            modified.delete()
        }
    }

    @Test
    fun docxImageSaveRefusesBeforeTouchingExistingFile() = runBlocking {
        val target = File(context.cacheDir, "plan6-image-guard.docx")
        val original = "do-not-replace".toByteArray()
        target.writeBytes(original)
        try {
            val document = OfficeDocument(
                body = DocumentBody(
                    elements = listOf(
                        OfficeParagraph("text"),
                        OfficeImage(imagePath = "word/media/picture.png", widthDp = 96f, heightDp = 48f)
                    )
                )
            )
            val parserSave = DocxDocumentParser(context).saveDocument(target, document)
            assertFalse("direct DOCX parser save must refuse unsupported images", parserSave)
            assertTrue("direct refusal must leave original bytes untouched", target.readBytes().contentEquals(original))

            val serializerSave = DocumentSerializer(context).serializeToFormat(document, "DOCX", target)
            assertFalse("Inky's DocumentSerializer path must propagate the image refusal", serializerSave)
            assertTrue("serializer refusal must leave original bytes untouched", target.readBytes().contentEquals(original))
        } finally {
            target.delete()
        }
    }

    @Test
    fun odtImageSaveFailsClosedWhenEmbeddedPayloadIsUnavailable() = runBlocking {
        val target = File(context.cacheDir, "plan6-missing-image.odt")
        val original = "do-not-replace".toByteArray()
        target.writeBytes(original)
        try {
            val document = OfficeDocument(
                metadata = DocumentMetadata(title = "Missing image"),
                body = DocumentBody(
                    elements = listOf(OfficeImage(imagePath = "Pictures/missing.png", widthDp = 96f, heightDp = 48f))
                )
            )
            val success = DocumentSerializer(context).serializeToFormat(document, "ODT", target)
            assertFalse("ODT must not report success after silently omitting an image", success)
            assertTrue("refused save must leave the original bytes untouched", target.readBytes().contentEquals(original))
        } finally {
            target.delete()
        }
    }

    @Test
    fun odtSaveCanReuseOriginalPackagedMediaWhenExtractedFileIsGone() {
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val document = OfficeDocument(
            body = DocumentBody(
                elements = listOf(OfficeImage(imagePath = "Pictures/keep.png", widthDp = 96f, heightDp = 48f))
            ),
            odtPackageData = com.makerandreas.papirusoffice.data.OdtPackageData(
                entries = mapOf("Pictures/keep.png" to imageBytes),
                originalContentXml = "<content/>"
            ),
            isModified = true
        )
        val bytes = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter().write(document)
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val output = ByteArrayOutputStream()
                zip.copyTo(output)
                entries[entry.name] = output.toByteArray()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertTrue("original image package entry must survive", entries["Pictures/keep.png"]!!.contentEquals(imageBytes))
        val content = entries["content.xml"]!!.toString(Charsets.UTF_8)
        assertTrue("generated content must still reference the preserved media path", content.contains("Pictures/keep.png"))
    }

    @Test
    fun tallerThanPageImageStillPaginatesAndUsesDocumentImagesFallback() {
        val spec = PageStyleSpec.FALLBACK
        val tallHeight = spec.bodyHeightDp + 300f
        val wideWidth = spec.contentWidthDp + 200f
        val document = OfficeDocument(
            body = DocumentBody(
                elements = listOf(
                    OfficeParagraph("Intro paragraph before oversized image."),
                    OfficeImage(imagePath = "Pictures/tall.png", widthDp = wideWidth, heightDp = tallHeight),
                    OfficeImage(imagePath = "Pictures/unmeasured.png", widthDp = 0f, heightDp = 0f),
                    OfficeParagraph("Trailing paragraph after images.")
                )
            )
        )

        val result = LayoutEngine(spec).performLayout(document)
        assertEquals("intro, oversized image and trailing content must split across 3 pages", 3, result.pages.size)
        assertEquals("intro paragraph stays on page 1", 1, result.elementPageIndex[0])
        assertEquals("oversized image starts on page 2", 2, result.elementPageIndex[1])
        assertEquals("unmeasured image follows on page 3", 3, result.elementPageIndex[2])
        assertEquals("trailing paragraph lands on page 3", 3, result.elementPageIndex[3])

        val tallPlacement = result.pages[1].elements.single()
        assertEquals(
            "an image wider than the body must be clamped to the content width",
            spec.contentWidthDp,
            tallPlacement.bounds.right - tallPlacement.bounds.left,
            0.01f
        )
        assertEquals(
            "an image taller than a page keeps its declared height instead of looping",
            tallHeight,
            tallPlacement.bounds.bottom - tallPlacement.bounds.top,
            0.01f
        )

        val fallbackPlacement = result.pages[2].elements.first()
        assertEquals(
            "an image with no extent falls back to DocumentImages' default width",
            DocumentImages.DEFAULT_WIDTH_UNITS,
            fallbackPlacement.bounds.right - fallbackPlacement.bounds.left,
            0.01f
        )
        assertEquals(
            "an image with no extent falls back to DocumentImages' default height",
            DocumentImages.DEFAULT_HEIGHT_UNITS,
            fallbackPlacement.bounds.bottom - fallbackPlacement.bounds.top,
            0.01f
        )

        val directImageRejection = runCatching {
            DocxDocumentParser(context).generateDocxXmlFromElements(
                listOf(OfficeImage(imagePath = "word/media/picture.png", widthDp = 96f, heightDp = 48f))
            )
        }
        assertTrue(
            "direct DOCX element XML generation must fail closed on OfficeImage",
            directImageRejection.exceptionOrNull() is IllegalStateException
        )
    }
}
