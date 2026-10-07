package com.example

import com.makerandreas.papirusoffice.data.DocumentEngine
import com.makerandreas.papirusoffice.data.DocumentSession
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.SessionManager
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DocumentSessionManagerTest {

    private val sessionManager = SessionManager.getInstance()

    @Before
    @After
    fun resetSession() {
        sessionManager.setCurrentSession(null)
    }

    @Test
    fun settingCurrentSessionUpdatesCurrentStateFlow() {
        assertNull(sessionManager.current.value)

        val mockEngine = mockk<DocumentEngine>(relaxed = true)
        val mockDoc = mockk<OfficeDocument>(relaxed = true)
        val session = DocumentSession(
            engine = mockEngine,
            document = mockDoc,
            file = null,
            dirty = false
        )

        sessionManager.setCurrentSession(session)

        assertEquals(session, sessionManager.current.value)
    }

    @Test
    fun mutatingDirtyFlagBumpsDirtyRevision() {
        val initialRevision = sessionManager.dirtyRevision.value

        val mockEngine = mockk<DocumentEngine>(relaxed = true)
        val mockDoc = mockk<OfficeDocument>(relaxed = true)
        val session = DocumentSession(
            engine = mockEngine,
            document = mockDoc,
            file = null,
            dirty = false
        )
        sessionManager.setCurrentSession(session)

        // Toggling dirty to true should increment revision
        session.dirty = true
        assertEquals(initialRevision + 1, sessionManager.dirtyRevision.value)
        assertTrue(session.dirty)

        // Setting same value again should NOT increment revision (guard check)
        session.dirty = true
        assertEquals(initialRevision + 1, sessionManager.dirtyRevision.value)

        // Toggling back to false should increment revision again
        session.dirty = false
        assertEquals(initialRevision + 2, sessionManager.dirtyRevision.value)
        assertFalse(session.dirty)
    }

    @Test
    fun markCurrentSessionDirtyUpdatesSession() {
        val initialRevision = sessionManager.dirtyRevision.value

        val mockEngine = mockk<DocumentEngine>(relaxed = true)
        val mockDoc = mockk<OfficeDocument>(relaxed = true)
        val session = DocumentSession(
            engine = mockEngine,
            document = mockDoc,
            file = null,
            dirty = false
        )
        sessionManager.setCurrentSession(session)

        sessionManager.markCurrentSessionDirty(true)

        assertTrue(session.dirty)
        assertEquals(initialRevision + 1, sessionManager.dirtyRevision.value)
    }
}
