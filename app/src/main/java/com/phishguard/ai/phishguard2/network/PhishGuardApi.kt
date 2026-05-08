package com.phishguard.ai.phishguard2.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the PhishGuard AI API
 */
interface PhishGuardApi {
    @POST("predict")
    suspend fun predict(@Body request: PredictionRequest): Response<PredictionResponse>
}
