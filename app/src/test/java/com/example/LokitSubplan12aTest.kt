package com.example

import com.example.core.jni.LibreOfficeCore
import com.example.core.jni.LokitEngine
import com.example.core.jni.LokitRuntime
import com.example.core.jni.OfficeEngineService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import javax.xml.parsers.DocumentBuilderFactory

class LokitSubplan12aTest {

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(File("src/main/java"), File("app/src/main/java"))
        val root = candidates.firstOrNull { it.isDirectory }
            ?: error("app source root not found from ${File(".").absolutePath}")
        val file = File(root, relativePath)
        assertTrue("main source not found: ${file.path}", file.isFile)
        return file.readText()
    }

    /** Code lines only: a comment that names an API must not count as a use of it. */
    private fun readMainCode(relativePath: String): String =
        readMainSource(relativePath).lineSequence()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
            }
            .joinToString("\n")

    private val expectedJniExports: Map<String, Set<String>> = mapOf(
        "org.libreoffice.kit.LibreOfficeKit" to setOf(
            "getLibreOfficeKitHandle",
            "initializeNative",
            "putenv",
            "redirectStdio"
        ),
        "org.libreoffice.kit.Office" to setOf(
            "bindMessageCallback",
            "destroy",
            "destroyAndExit",
            "documentLoadNative",
            "getError",
            "setDocumentPassword",
            "setOptionalFeatures"
        ),
        "org.libreoffice.kit.Document" to setOf(
            "bindMessageCallback",
            "destroy",
            "getCommandValues",
            "getDocumentHeight",
            "getDocumentTypeNative",
            "getDocumentWidth",
            "getPart",
            "getPartName",
            "getPartPageRectangles",
            "getParts",
            "getTextSelection",
            "initializeForRendering",
            "paintTileNative",
            "paste",
            "postKeyEvent",
            "postMouseEvent",
            "postUnoCommand",
            "resetSelection",
            "saveAs",
            "setClientZoom",
            "setGraphicSelection",
            "setPart",
            "setPartMode",
            "setTextSelection"
        )
    )

    @Test
    fun `org libreoffice kit classes declare exact native methods exported by liblo-native-code`() {
        for ((className, expectedMethods) in expectedJniExports) {
            val clazz = Class.forName(className)
            val actualNativeMethods = clazz.declaredMethods
                .filter { Modifier.isNative(it.modifiers) }
                .map { it.name }
                .toSet()
            assertEquals("JNI methods mismatch for $className", expectedMethods, actualNativeMethods)
        }
    }

    @Test
    fun `LibreOfficeCore declares no orphaned custom native methods`() {
        val orphanMethods = LibreOfficeCore::class.java.declaredMethods
            .filter { Modifier.isNative(it.modifiers) }
            .map { it.name }
        assertTrue(
            "LibreOfficeCore must delegate JNI to org.libreoffice.kit.* instead of declaring missing symbols: $orphanMethods",
            orphanMethods.isEmpty()
        )
    }

    @Test
    fun `LokitRuntime generates well-formed fonts conf with metric-compatible aliases`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "lokit-fonts-test-${System.nanoTime()}")
        try {
            val fontsConf = LokitRuntime.prepareFontConfig(
                filesDir = File(tempDir, "files"),
                cacheDir = File(tempDir, "cache")
            )
            assertTrue("fonts.conf must exist", fontsConf.isFile)
            val xml = fontsConf.readText()
            assertTrue(xml.contains("<family>Calibri</family>"))
            assertTrue(xml.contains("<family>Carlito</family>"))
            assertTrue(xml.contains("<family>Cambria</family>"))
            assertTrue(xml.contains("<family>Caladea</family>"))
            assertTrue(xml.contains("<family>Times New Roman</family>"))
            assertTrue(xml.contains("<family>Liberation Serif</family>"))
            assertTrue(xml.contains("<family>Arial</family>"))
            assertTrue(xml.contains("<family>Liberation Sans</family>"))
            assertTrue(xml.contains("<family>Courier New</family>"))
            assertTrue(xml.contains("<family>Liberation Mono</family>"))

            val dbf = DocumentBuilderFactory.newInstance().apply {
                isValidating = false
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            val parsed = dbf.newDocumentBuilder().parse(fontsConf)
            assertEquals("fontconfig", parsed.documentElement.tagName)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `AndroidManifest registers OfficeEngineService in isolated office process`() {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        )
        val manifest = candidates.firstOrNull { it.isFile }
            ?: error("AndroidManifest.xml not found")
        val text = manifest.readText()
        assertTrue(text.contains(OfficeEngineService::class.java.name))
        assertTrue(text.contains("android:process=\":office\""))
    }

    /**
     * The `:office` transport must work on minSdk 24, where `Messenger(IBinder)` does not
     * exist. Pins the API-1 pair (`ResultReceiver` + `startService`) by source scan so a
     * later refactor cannot silently reintroduce the API 26 constructor.
     */
    @Test
    fun officeTransportStaysWithinMinSdk24Apis() {
        val service = readMainCode("com/example/core/jni/OfficeEngineService.kt")
        val client = readMainCode("com/example/core/jni/OfficeEngineClient.kt")
        assertTrue("Service must accept jobs through onStartCommand", service.contains("override fun onStartCommand("))
        assertTrue("Service must report through ResultReceiver", service.contains("ResultReceiver"))
        assertTrue("Service must not bind for jobs", service.contains("override fun onBind(intent: Intent?): IBinder? = null"))
        assertTrue("Client must send the job with startService", client.contains("startService("))
        assertTrue("Client must carry a ResultReceiver", client.contains("ResultReceiver"))
        assertTrue("Client must bound the wait", client.contains("withTimeoutOrNull"))
        assertTrue("Messenger(IBinder) is API 26 and must not be used", !service.contains("Messenger(") && !client.contains("Messenger("))
    }

    @Test
    fun `simulated mode stays safe without native library`() {
        assertFalse(LokitEngine.isNativeAvailable)
        assertEquals(LokitEngine.EngineMode.SIMULATED, LokitEngine.mode)
        assertTrue(LibreOfficeCore.initialize("/tmp", enableOoxml = true, enableOmml = true))
    }
}
