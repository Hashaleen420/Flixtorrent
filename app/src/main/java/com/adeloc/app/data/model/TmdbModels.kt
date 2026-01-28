package com.adeloc.app.data.model

import com.google.gson.annotations.SerializedName

// 1. Wrapper for lists of movies/shows
data class MovieResponse(
    val results: List<Movie>
)

// 2. The main item (Movie OR TV Show)
data class Movie(
    val id: Int,

    // Movies use "title", TV shows use "name". We capture both.
    @SerializedName("title") val _title: String?,
    @SerializedName("name") val _name: String?,

    @SerializedName("poster_path") val poster_path: String?,
    @SerializedName("backdrop_path") val backdrop_path: String?,
    @SerializedName("overview") val overview: String?
) {
    // Helper to get the correct text automatically
    val title: String
        get() = _title ?: _name ?: "Unknown Title"
}

// 3. For External IDs (IMDb)
data class ExternalIdResponse(
    val imdb_id: String?,
    val tvdb_id: Int?,
    val facebook_id: String?,
    val instagram_id: String?,
    val twitter_id: String?
)

// 4. NEW: For TV Show Details (Seasons)
data class TvDetailResponse(
    val seasons: List<Season>
)

data class Season(
    val season_number: Int,
    val episode_count: Int,
    val name: String?
)