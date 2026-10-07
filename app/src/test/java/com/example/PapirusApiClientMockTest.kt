package com.example

import com.makerandreas.papirusoffice.data.api.DocumentTemplate
import com.makerandreas.papirusoffice.data.api.PapirusApiClient
import com.makerandreas.papirusoffice.data.api.PapirusCloudApiService
import com.makerandreas.papirusoffice.data.api.TemplateListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import android.util.Log

class PapirusApiClientMockTest {

    private lateinit var mockService: PapirusCloudApiService

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        mockService = mockk()
        PapirusApiClient.customApiService = mockService
    }

    @After
    fun tearDown() {
        PapirusApiClient.customApiService = null
        unmockkStatic(Log::class)
    }

    @Test
    fun fetchTemplatesReturnsApiDataWhenSuccessful() = runTest {
        val remoteTemplates = listOf(
            DocumentTemplate(
                id = "tpl_remote_1",
                title = "Remote Resume",
                category = "Career",
                moduleType = "INKY",
                description = "Modern remote resume template"
            )
        )
        val apiResponse = Response.success(
            TemplateListResponse(status = "success", total = 1, templates = remoteTemplates)
        )
        coEvery { mockService.getTemplates("INKY") } returns apiResponse

        val result = PapirusApiClient.fetchTemplatesWithFallback("INKY")

        assertEquals(1, result.size)
        assertEquals("tpl_remote_1", result[0].id)
        assertEquals("Remote Resume", result[0].title)
        coVerify(exactly = 1) { mockService.getTemplates("INKY") }
    }

    @Test
    fun fetchTemplatesFallsBackWhenApiFailsWithHttpError() = runTest {
        val errorBody = "{\"error\": \"Internal Server Error\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val errorResponse = Response.error<TemplateListResponse>(500, errorBody)
        coEvery { mockService.getTemplates(any()) } returns errorResponse

        val result = PapirusApiClient.fetchTemplatesWithFallback(null)

        // Fallback templates should be returned
        assertFalse(result.isEmpty())
        assertTrue(result.any { it.moduleType == "INKY" })
        assertTrue(result.any { it.moduleType == "CELLINA" })
    }

    @Test
    fun fetchTemplatesFallsBackWhenNetworkExceptionOccurs() = runTest {
        coEvery { mockService.getTemplates(any()) } throws IOException("Simulated network timeout")

        val result = PapirusApiClient.fetchTemplatesWithFallback("CELLINA")

        // Should gracefully fallback without throwing exception
        assertFalse(result.isEmpty())
        assertTrue(result.all { it.moduleType == "CELLINA" })
    }

    @Test
    fun fetchTemplatesFiltersFallbackByModuleType() = runTest {
        coEvery { mockService.getTemplates("SLIDIA") } throws IOException("Offline")

        val result = PapirusApiClient.fetchTemplatesWithFallback("SLIDIA")

        assertFalse(result.isEmpty())
        assertTrue(result.all { it.moduleType == "SLIDIA" })
    }
}
