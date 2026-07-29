package com.makerandreas.papirusoffice.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit Service interface for Papirus Office Cloud API.
 */
interface PapirusCloudApiService {

    @GET("v1/templates")
    suspend fun getTemplates(
        @Query("module") module: String? = null
    ): Response<TemplateListResponse>

    @GET("v1/templates/{id}")
    suspend fun getTemplateDetail(
        @Path("id") templateId: String
    ): Response<DocumentTemplate>

    @POST("v1/sync/metadata")
    suspend fun syncDocumentMetadata(
        @Body syncRequest: SyncRequest
    ): Response<SyncResponse>
}
