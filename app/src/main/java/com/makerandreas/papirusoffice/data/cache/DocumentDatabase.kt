package com.makerandreas.papirusoffice.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for Papirus Office local cache.
 */
@Database(
    entities = [DocumentCacheEntity::class, InkyDocumentMetadataEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DocumentDatabase : RoomDatabase() {

    abstract fun documentCacheDao(): DocumentCacheDao
    abstract fun inkyDocumentMetadataDao(): InkyDocumentMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: DocumentDatabase? = null

        fun getInstance(context: Context): DocumentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DocumentDatabase::class.java,
                    "papirus_document_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
