package com.phishguard.ai.phishguard2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a saved scan result in the local database.
 * This meets the academic requirement for local persistence.
 */
@Entity(tableName = "scan_history")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val prediction: String,
    val probability: Double,
    val analystNotes: String,
    val timestamp: Long = System.currentTimeMillis()
)
