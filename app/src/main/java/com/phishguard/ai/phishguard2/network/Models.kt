package com.phishguard.ai.phishguard2.network

import com.google.gson.annotations.SerializedName

/**
 * Request body for the /predict endpoint
 */
data class PredictionRequest(
    @SerializedName("url") val url: String
)

/**
 * Response body from the /predict endpoint
 */
data class PredictionResponse(
    @SerializedName("prediction") val prediction: String,
    @SerializedName("probability") val probability: Double,
    @SerializedName("analyst_notes") val analystNotes: String,
    @SerializedName("url") val url: String
)
