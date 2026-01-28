package com.adeloc.app.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response // Import this!
import retrofit2.http.*

interface RealDebridApi {

    // 1. Add Magnet Link
    @FormUrlEncoded
    @POST("torrents/addMagnet")
    suspend fun addMagnet(
        @Header("Authorization") token: String,
        @Field("magnet") magnet: String
    ): RdAddResponse

    // 2. Select All Files (FIXED: Returns Response<Unit> to handle empty 204 reply)
    @FormUrlEncoded
    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Field("files") files: String = "all"
    ): Response<Unit>

    // 3. Get Torrent Info
    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): RdTorrentInfo

    // 4. Unrestrict the Link
    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(
        @Header("Authorization") token: String,
        @Field("link") link: String
    ): RdUnrestrictResponse
}

// --- MODELS ---
data class RdAddResponse(val id: String)
data class RdTorrentInfo(val links: List<String>, val status: String)
data class RdUnrestrictResponse(@SerializedName("download") val download: String)