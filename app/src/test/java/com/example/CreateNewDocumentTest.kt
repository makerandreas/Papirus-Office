package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.core.util.TemplateManager
import com.example.modules.pagella.PagellaPdfCreator
import com.example.ui.home.NewDocumentScreen
import com.example.ui.theme.PapirusTheme
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CreateNewDocumentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun templateManager_extractsUntitledOdtOdsOdp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val templates = listOf(
            Triple("odt", "text", TemplateManager::getInkyNormalTemplateFile),
            Triple("ods", "spreadsheet", TemplateManager::getCalcDefaultTemplateFile),
            Triple("odp", "presentation", TemplateManager::getSlidiaDefaultTemplateFile)
        )
        val office = "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
        val manifest = "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0"
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        for ((extension, bodyType, extract) in templates) {
            val expected = context.assets.open("templates/Untitled.$extension").use { it.readBytes() }
            val file = extract(context)
            assertNotNull("Expected Untitled.$extension asset", file)
            assertEquals("untitled.$extension", file!!.name)
            assertArrayEquals("Must copy the blank, not an unrelated fallback", expected, file.readBytes())
            ZipFile(file).use { zip ->
                val mime = "application/vnd.oasis.opendocument.$bodyType"
                assertEquals(mime, zip.getInputStream(zip.getEntry("mimetype")).bufferedReader().use { it.readText() })
                val content = zip.getInputStream(zip.getEntry("content.xml")).use { factory.newDocumentBuilder().parse(it) }
                assertEquals("1.4", content.documentElement.getAttributeNS(office, "version"))
                assertEquals(1, content.getElementsByTagNameNS(office, bodyType).length)
                val packageManifest = zip.getInputStream(zip.getEntry("META-INF/manifest.xml")).use { factory.newDocumentBuilder().parse(it) }
                assertEquals("1.4", packageManifest.documentElement.getAttributeNS(manifest, "version"))
                val entries = packageManifest.getElementsByTagNameNS(manifest, "file-entry")
                val root = (0 until entries.length).map { entries.item(it) as org.w3c.dom.Element }
                    .single { it.getAttributeNS(manifest, "full-path") == "/" }
                assertEquals(mime, root.getAttributeNS(manifest, "media-type"))
                when (extension) {
                    "ods" -> assertEquals(1, content.getElementsByTagNameNS("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "table").length)
                    "odp" -> assertEquals(1, content.getElementsByTagNameNS("urn:oasis:names:tc:opendocument:xmlns:drawing:1.0", "page").length)
                }
            }
            // New Document must not reuse a stale cache copy after an asset update.
            file.writeText("stale cache")
            assertArrayEquals(expected, extract(context)!!.readBytes())
        }
    }

    @Test
    fun newDocumentScreen_displaysCarouselGridAndPagellaGroup_andOpensUntitledTemplates() {
        var navigatedModule: String? = null

        composeRule.setContent {
            PapirusTheme {
                NewDocumentScreen(
                    onBack = {},
                    onNavigateToModule = { module -> navigatedModule = module }
                )
            }
        }

        composeRule.waitForIdle()

        // Verify group title and 3-column carousel grid items
        composeRule.onNodeWithTag("group_title_create_new_document").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("create_new_carousel_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("item_new_inky").assertIsDisplayed()
        composeRule.onNodeWithTag("item_new_cellina").assertIsDisplayed()
        composeRule.onNodeWithTag("item_new_slidia").assertIsDisplayed()

        // Click Inky Document -> opens untitled.odt
        composeRule.onNodeWithTag("item_new_inky").performScrollTo().performClick()
        assertEquals("Inky", navigatedModule)
        assertEquals("Inky", MainActivity.openedFileType)
        assertTrue(
            "Expected openedFilePath to end with untitled.odt, was ${MainActivity.openedFilePath}",
            MainActivity.openedFilePath?.endsWith("untitled.odt") == true
        )

        // Click Cellina Spreadsheet -> opens untitled.ods
        composeRule.onNodeWithTag("item_new_cellina").performScrollTo().performClick()
        assertEquals("Cellina", navigatedModule)
        assertEquals("Cellina", MainActivity.openedFileType)
        assertTrue(
            "Expected openedFilePath to end with untitled.ods, was ${MainActivity.openedFilePath}",
            MainActivity.openedFilePath?.endsWith("untitled.ods") == true
        )

        // Click Slidia Presentation -> opens untitled.odp
        composeRule.onNodeWithTag("item_new_slidia").performScrollTo().performClick()
        assertEquals("Slidia", navigatedModule)
        assertEquals("Slidia", MainActivity.openedFileType)
        assertTrue(
            "Expected openedFilePath to end with untitled.odp, was ${MainActivity.openedFilePath}",
            MainActivity.openedFilePath?.endsWith("untitled.odp") == true
        )

        // Verify Create Pagella PDF Document group and 3 items
        composeRule.onNodeWithTag("group_title_pagella_pdf").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Create Pagella PDF Document").assertIsDisplayed()
        composeRule.onNodeWithTag("item_pagella_from_image").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("item_pagella_from_camera").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("item_pagella_convert_document").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun pagellaPdfCreator_createsPdfFromImageAndDocument() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Create a sample PNG image and convert it to PDF
        val imageFile = File(context.cacheDir, "sample_image.png")
        val bmp = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        FileOutputStream(imageFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val imagePdf = PagellaPdfCreator.createPdfFromImageFile(context, imageFile, "sample_image")
        assertNotNull("Expected PDF file created from image", imagePdf)
        assertTrue(imagePdf!!.exists() && imagePdf.name.endsWith(".pdf") && imagePdf.length() > 0L)

        // 2. Convert untitled.odt to PDF
        val odtTemplate = TemplateManager.getInkyNormalTemplateFile(context)
        assertNotNull(odtTemplate)
        val docPdf = PagellaPdfCreator.convertDocumentToPdf(context, odtTemplate!!, "untitled.odt")
        assertNotNull("Expected PDF file converted from ODT document", docPdf)
        assertTrue(docPdf!!.exists() && docPdf.name.endsWith(".pdf") && docPdf.length() > 0L)
    }
}
