package com.adeloc.app.data.model

data class TorrentioResponse(val streams: List<TStream>?)

data class TStream(
    val title: String,
    val infoHash: String?
)