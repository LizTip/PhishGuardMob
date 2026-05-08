package com.phishguard.ai.phishguard2.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the scan history.
 * Defines the SQL queries and maps them to Kotlin functions.
 */
@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResultEntity)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()
}
