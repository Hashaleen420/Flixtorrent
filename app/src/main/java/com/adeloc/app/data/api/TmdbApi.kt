package com.adeloc.app.data.api

import com.adeloc.app.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    // --- 1. TRENDING & POPULAR (Main Content) ---

    // NEW: "Trending Today" (Movies + Shows mixed, updates daily)
    @GET("trending/all/day")
    suspend fun getTrending(@Query("api_key") key: String): MovieResponse

    @GET("movie/popular")
    suspend fun getPopular(@Query("api_key") key: String): MovieResponse

    @GET("movie/top_rated")
    suspend fun getTopRated(@Query("api_key") key: String): MovieResponse

    @GET("movie/upcoming")
    suspend fun getUpcoming(@Query("api_key") key: String): MovieResponse

    @GET("movie/now_playing")
    suspend fun getInTheaters(@Query("api_key") key: String): MovieResponse

    // --- 2. TV SERIES ---

    @GET("trending/tv/week")
    suspend fun getTrendingTV(@Query("api_key") apiKey: String): MovieResponse

    // Details for Seasons/Episodes
    @GET("tv/{id}")
    suspend fun getTvDetails(@Path("id") id: Int, @Query("api_key") k: String): TvDetailResponse

    // --- 3. DISCOVERY (Genres, Providers, Anime) ---

    @GET("discover/movie")
    suspend fun getByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: String,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): MovieResponse

    @GET("discover/movie")
    suspend fun getByProvider(
        @Query("api_key") apiKey: String,
        @Query("with_watch_providers") providerId: String,
        @Query("watch_region") region: String = "US",
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): MovieResponse

    // NEW: Anime Specific (Genre 16 = Animation, Language = ja)
    @GET("discover/movie?with_genres=16&with_original_language=ja")
    suspend fun getAnime(
        @Query("api_key") apiKey: String,
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): MovieResponse

    // --- 4. SEARCH ---

    @GET("search/movie")
    suspend fun search(@Query("api_key") k: String, @Query("query") q: String): MovieResponse

    @GET("search/tv")
    suspend fun searchTv(@Query("api_key") k: String, @Query("query") q: String): MovieResponse

    // --- 5. ID LOOKUP (For Torrentio) ---

    @GET("movie/{id}/external_ids")
    suspend fun getIds(@Path("id") id: Int, @Query("api_key") k: String): ExternalIdResponse

    @GET("tv/{id}/external_ids")
    suspend fun getTvIds(@Path("id") id: Int, @Query("api_key") k: String): ExternalIdResponse
}