package com.phishguard.ai.phishguard2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room Database class.
 * Singleton pattern is used to ensure only one instance exists.
 */
@Database(entities = [ScanResultEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phishguard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
