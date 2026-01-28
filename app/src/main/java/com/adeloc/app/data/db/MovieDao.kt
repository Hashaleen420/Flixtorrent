package com.adeloc.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovieDao {
    // Get all watched movies sorted by recent
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    suspend fun getHistory(): List<WatchEntity>

    // Get specific movie progress to resume
    @Query("SELECT * FROM watch_history WHERE tmdbId = :id LIMIT 1")
    suspend fun getProgress(id: Int): WatchEntity?

    // Insert or Update movie progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: WatchEntity)
}