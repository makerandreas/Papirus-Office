package com.makerandreas.papirusoffice.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room Database for Papirus Office application.
 */
@Database(
    entities = [DocumentMetadata::class],
    version = 1,
    exportSchema = false
)
abstract class PapirusDatabase : RoomDatabase() {

    abstract fun documentMetadataDao(): DocumentMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: PapirusDatabase? = null

        fun getInstance(context: Context): PapirusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PapirusDatabase::class.java,
                    "papirus_office_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
