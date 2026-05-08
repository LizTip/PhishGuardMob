package com.phishguard.ai.phishguard2.data

import com.phishguard.ai.phishguard2.network.PredictionRequest
import com.phishguard.ai.phishguard2.network.PredictionResponse
import com.phishguard.ai.phishguard2.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

/**
 * Repository pattern: Mediates between the network API and local Room database.
 * This is a key academic requirement for clean architecture.
 */
class PhishGuardRepository(private val scanResultDao: ScanResultDao) {

    // Expose local history as a Flow for real-time UI updates
    val allScans: Flow<List<ScanResultEntity>> = scanResultDao.getAllScans()

    /**
     * Performs a scan via API and saves the result to the local database.
     */
    suspend fun scanAndSave(url: String): Result<PredictionResponse> {
        return try {
            val response: Response<PredictionResponse> = RetrofitClient.api.predict(PredictionRequest(url))
            
            if (response.isSuccessful) {
                val prediction = response.body()
                if (prediction != null) {
                    // Save to local Room database
                    scanResultDao.insertScan(
                        ScanResultEntity(
                            url = prediction.url,
                            prediction = prediction.prediction,
                            probability = prediction.probability,
                            analystNotes = prediction.analystNotes
                        )
                    )
                    Result.success(prediction)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears all saved scans from the local database.
     */
    suspend fun clearAllHistory() {
        scanResultDao.deleteAll()
    }
}
