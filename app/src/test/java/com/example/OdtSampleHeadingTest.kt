package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.toOfficeDocument
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OdtSampleHeadingTest {

    @Test
    fun testSample1OdtHeadings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = File("tests/Sample 1.odt")
        println("File exists: ${file.exists()}, length: ${file.length()}")
        
        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        println("ParsedDoc elements count: ${parsedDoc.elements.size}")
        
        val headings = parsedDoc.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        println("Found headings count in parsedDoc: ${headings.size}")
        headings.forEachIndexed { i, h ->
            println("Heading $i: level=${h.level}, style=${h.styleName}, text='${h.text}'")
        }

        val officeDoc = parsedDoc.toOfficeDocument()
        println("OfficeDoc body elements count: ${officeDoc.body.elements.size}")
        
        val indexEngine = DocumentIndexEngine(officeDoc)
        val index = indexEngine.reindex()
        println("DocumentIndex headings count: ${index.headings.size}")
        index.headings.forEach { h ->
            println("Index Heading: ${h.title}, level=${h.outlineLevel}, children=${h.children.size}")
        }
    }
}
