package com.adeloc.app.data.model

data class StreamSource(
    val name: String,       // e.g. "Torrentio 1080p" or "Server 1 (Web)"
    val url: String,        // Magnet link or HTTP URL
    val quality: String,    // "4K", "1080p"
    val isTorrent: Boolean
)