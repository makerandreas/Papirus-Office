package com.example

import java.security.MessageDigest
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Detect a silent fixture replacement before interpreting a changed pagination dump. */
class FixtureIdentityTest {
    @Test
    fun writerFixturesMatchRecordedIdentityAndReferencePages() {
        val manifest = Properties().apply {
            SampleMatrix.findTestFile("fixture-identities.properties").inputStream().use { load(it) }
        }
        assertEquals(SampleMatrix.fileNames.toSet(), manifest.stringPropertyNames())
        for (name in SampleMatrix.fileNames) {
            val fields = manifest.getProperty(name).split('|')
            assertEquals("$name manifest fields", 3, fields.size)
            val bytes = SampleMatrix.findTestFile(name).readBytes()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals("$name changed; review producer, reference and window before re-baselining", fields[0], digest)
            assertTrue("$name must record its producer", fields[1].isNotBlank())
            assertEquals("$name saved page count", SampleMatrix.referenceFor(name), fields[2].toInt())
        }
    }
}
