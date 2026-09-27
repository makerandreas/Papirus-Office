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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CreateNewDocumentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun templateManager_extractsUntitledOdtOdsOdp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val odtFile = TemplateManager.getInkyNormalTemplateFile(context)
        assertNotNull("Expected untitled.odt template file", odtFile)
        assertEquals("untitled.odt", odtFile!!.name)
        assertTrue(odtFile.exists() && odtFile.length() > 0L)

        val odsFile = TemplateManager.getCalcDefaultTemplateFile(context)
        assertNotNull("Expected untitled.ods template file", odsFile)
        assertEquals("untitled.ods", odsFile!!.name)
        assertTrue(odsFile.exists() && odsFile.length() > 0L)

        val odpFile = TemplateManager.getSlidiaDefaultTemplateFile(context)
        assertNotNull("Expected untitled.odp template file", odpFile)
        assertEquals("untitled.odp", odpFile!!.name)
        assertTrue(odpFile.exists() && odpFile.length() > 0L)
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
