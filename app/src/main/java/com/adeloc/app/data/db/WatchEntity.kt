package com.adeloc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchEntity(
    @PrimaryKey val tmdbId: Int,
    val title: String,
    val posterPath: String,
    val timestamp: Long,
    val position: Long = 0,
    val duration: Long = 0,
    // NEW: Remembers the link you chose
    val lastUrl: String = "",
    val lastQuality: String = ""
)