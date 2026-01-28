package com.adeloc.app.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val TMDB_URL = "https://api.themoviedb.org/3/"
    private const val TORRENTIO_URL = "https://torrentio.strem.fun/"
    private const val RD_URL = "https://api.real-debrid.com/rest/1.0/"

    // 1. TMDB Client
    val tmdb: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(TMDB_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    // 2. Torrentio Client
    val torrentio: TorrentioApi by lazy {
        Retrofit.Builder()
            .baseUrl(TORRENTIO_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TorrentioApi::class.java)
    }

    // 3. Real-Debrid Client (THIS IS WHAT WAS MISSING)
    val realDebrid: RealDebridApi by lazy {
        Retrofit.Builder()
            .baseUrl(RD_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RealDebridApi::class.java)
    }
}