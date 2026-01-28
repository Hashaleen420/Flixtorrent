package com.adeloc.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// VERSION 3 (Wipes old data to prevent crash)
@Database(entities = [WatchEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flixorent_db"
                )
                    .fallbackToDestructiveMigration() // Important!
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}