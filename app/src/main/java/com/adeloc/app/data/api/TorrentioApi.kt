package com.adeloc.app.data.api
import com.adeloc.app.data.model.TorrentioResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface TorrentioApi {
    // Movies: stream/movie/tt12345.json
    @GET("stream/movie/{id}.json")
    suspend fun getStreams(@Path("id") imdbId: String): TorrentioResponse

    // Series: stream/series/tt12345:1:1.json
    // We pass the whole "tt12345:1:1" string as one parameter to be safe
    @GET("stream/series/{videoId}.json")
    suspend fun getSeriesStreams(@Path("videoId") videoId: String): TorrentioResponse
}