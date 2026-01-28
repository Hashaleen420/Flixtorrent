package com.adeloc.app.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.adeloc.app.R
import com.adeloc.app.data.api.RetrofitClient
import com.adeloc.app.data.db.AppDatabase
import com.adeloc.app.data.db.WatchEntity
import com.adeloc.app.data.model.Season
import com.adeloc.app.data.model.StreamSource
import com.adeloc.app.data.scraper.WebScraper
import com.adeloc.app.databinding.ActivityPlayerBinding
import com.adeloc.app.utils.Constants
import com.github.se_bastiaan.torrentstream.StreamStatus
import com.github.se_bastiaan.torrentstream.Torrent
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
// --- CAST IMPORTS ---
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity(), TorrentListener {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var torrentStream: TorrentStream

    // --- CAST VARIABLES ---
    private var castContext: CastContext? = null
    private lateinit var castSessionManagerListener: SessionManagerListener<CastSession>

    // Movie Data
    private var tmdbId = 0
    private var movieTitle = ""
    private var posterPath = ""
    private var currentSeason = 1
    private var currentEpisode = 1
    private var isTvShow = false

    // Source Data
    private var foundSources: List<StreamSource> = emptyList()
    private var currentSource: StreamSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        tmdbId = intent.getIntExtra("TMDB_ID", 0)
        movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: ""
        posterPath = intent.getStringExtra("POSTER") ?: ""

        setupPlayer()
        setupCast()
        setupUI()

        if (tmdbId != 0) {
            checkHistoryOrLoad()
        }
    }

    // --- 1. CAST SETUP & LISTENER (The Fix) ---

    private fun setupCast() {
        try {
            castContext = CastContext.getSharedInstance(this)
            CastButtonFactory.setUpMediaRouteButton(applicationContext, binding.mediaRouteButton)

            // LISTENER: Detects when you connect/disconnect
            castSessionManagerListener = object : SessionManagerListener<CastSession> {
                override fun onSessionStarted(session: CastSession, sessionId: String) {
                    // Handover: If local player is running, send it to TV
                    if (player.isPlaying || player.currentPosition > 0) {
                        val url = currentSource?.url
                        if (!url.isNullOrEmpty()) {
                            castVideoToTv(url, player.currentPosition)
                        }
                    }
                }
                override fun onSessionEnded(session: CastSession, error: Int) {
                    // Optional: Resume on phone when casting stops
                    // player.play()
                }
                override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {}
                override fun onSessionStarting(session: CastSession) {}
                override fun onSessionEnding(session: CastSession) {}
                override fun onSessionResuming(session: CastSession, sessionId: String) {}
                override fun onSessionStartFailed(session: CastSession, error: Int) {}
                override fun onSessionResumeFailed(session: CastSession, error: Int) {}
                override fun onSessionSuspended(session: CastSession, reason: Int) {}
            }

        } catch (e: Exception) {
            binding.mediaRouteButton.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        castContext?.sessionManager?.addSessionManagerListener(castSessionManagerListener, CastSession::class.java)
    }

    override fun onPause() {
        super.onPause()
        if (player.currentPosition > 0) saveProgress(player.currentPosition, player.duration)
        player.pause()
        castContext?.sessionManager?.removeSessionManagerListener(castSessionManagerListener, CastSession::class.java)
    }

    // --- 2. CASTING LOGIC ---

    private fun castVideoToTv(url: String, position: Long) {
        val session = castContext?.sessionManager?.currentCastSession
        if (session == null || !session.isConnected) return

        // Auto-Detect File Type (MP4 vs MKV)
        val mimeType = if (url.contains(".mkv", true)) "video/x-matroska" else "video/mp4"

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        metadata.putString(MediaMetadata.KEY_TITLE, movieTitle)
        if (posterPath.isNotEmpty()) {
            val imageUrl = "https://image.tmdb.org/t/p/w500$posterPath"
            metadata.addImage(WebImage(Uri.parse(imageUrl)))
        }

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(metadata)
            .build()

        val options = MediaLoadOptions.Builder()
            .setAutoplay(true)
            .setPlayPosition(position)
            .build()

        val remoteMediaClient = session.remoteMediaClient
        remoteMediaClient?.load(mediaInfo, options)

        Toast.makeText(this, "Casting to TV...", Toast.LENGTH_SHORT).show()

        // Pause local player immediately
        if (player.isPlaying) player.pause()

        // Save progress locally so we don't lose it
        saveProgress(position, player.duration)
    }

    // --- 3. SMART RESUME ---

    private fun checkHistoryOrLoad() {
        lifecycleScope.launch {
            val history = AppDatabase.get(applicationContext).movieDao().getProgress(tmdbId)
            if (history != null && history.lastUrl.isNotEmpty()) {
                showResumeDialog(history)
            } else {
                startSearchProcess()
            }
        }
    }

    private fun showResumeDialog(history: WatchEntity) {
        val percentage = if (history.duration > 0) (history.position * 100) / history.duration else 0
        val savedSource = StreamSource(history.title, history.lastUrl, history.lastQuality, true)

        AlertDialog.Builder(this)
            .setTitle("Resume Playing?")
            .setMessage("Continue ${history.title} from ${percentage}%?\nSource: ${history.lastQuality}")
            .setPositiveButton("Resume") { _, _ ->
                currentSource = savedSource
                binding.infoText.text = "Resuming Session..."
                playWithRealDebrid(savedSource, playInternally = true)
            }
            .setNegativeButton("Find New Links") { _, _ ->
                startSearchProcess()
            }
            .setCancelable(false)
            .show()
    }

    private fun startSearchProcess() {
        if (tmdbId != 0) checkIfTvShow()
    }

    // --- 4. SEARCH & SCRAPING ---

    private fun checkIfTvShow() {
        lifecycleScope.launch {
            try {
                val tvDetails = RetrofitClient.tmdb.getTvDetails(tmdbId, Constants.TMDB_KEY)
                isTvShow = true
                showSeasonDialog(tvDetails.seasons)
            } catch (e: Exception) {
                isTvShow = false
                findAllLinks()
            }
        }
    }

    private fun showSeasonDialog(seasons: List<Season>) {
        val validSeasons = seasons.filter { it.season_number > 0 }
        val names = validSeasons.map { "Season ${it.season_number}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Select Season").setItems(names) { _, i ->
            showEpisodeDialog(validSeasons[i])
        }.setCancelable(false).show()
    }

    private fun showEpisodeDialog(season: Season) {
        val episodes = (1..season.episode_count).map { "Episode $it" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Select Episode").setItems(episodes) { _, i ->
            currentSeason = season.season_number
            currentEpisode = i + 1
            binding.infoText.text = "Searching sources..."
            findAllLinks()
        }.setCancelable(false).show()
    }

    private fun findAllLinks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sources = mutableListOf<StreamSource>()
            val trackers = listOf("udp://tracker.opentrackr.org:1337/announce", "udp://open.stealth.si:80/announce", "udp://tracker.torrent.eu.org:451/announce")
            val trackerParams = trackers.joinToString("") { "&tr=$it" }

            try {
                val ids = if (isTvShow) RetrofitClient.tmdb.getTvIds(tmdbId, Constants.TMDB_KEY) else RetrofitClient.tmdb.getIds(tmdbId, Constants.TMDB_KEY)
                ids.imdb_id?.let { imdb ->
                    val torrents = if (isTvShow) RetrofitClient.torrentio.getSeriesStreams("$imdb:$currentSeason:$currentEpisode") else RetrofitClient.torrentio.getStreams(imdb)
                    torrents.streams?.forEach {
                        if (!it.title.contains(".avi", ignoreCase = true)) {
                            val cleanTitle = it.title.replace("\n", " ")
                            val magnet = "magnet:?xt=urn:btih:${it.infoHash}&dn=${Uri.encode(cleanTitle)}$trackerParams"
                            val sizeRegex = "\\b(\\d+(?:\\.\\d+)?\\s*[GM]B)\\b".toRegex(RegexOption.IGNORE_CASE)
                            val size = sizeRegex.find(it.title)?.value ?: "Unknown"
                            val seedsRegex = "👤\\s*(\\d+)".toRegex()
                            val seeds = seedsRegex.find(it.title)?.groupValues?.get(1) ?: "?"
                            val quality = if (cleanTitle.contains("4k", true)) "4K" else if (cleanTitle.contains("1080", true)) "1080p" else "HD"
                            sources.add(StreamSource(cleanTitle, magnet, "Torrentio | $quality | S:$seeds | $size", true))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            try {
                val q = if (isTvShow) "$movieTitle S${currentSeason}E${currentEpisode}" else movieTitle
                sources.addAll(WebScraper.scrapeWeb(q))
            } catch (e: Exception) { e.printStackTrace() }

            withContext(Dispatchers.Main) {
                binding.loadingBar.visibility = View.GONE
                val sorted = sources.sortedWith(compareByDescending<StreamSource> {
                    if (it.quality.contains("4K")) 3 else if (it.quality.contains("1080")) 2 else 0
                }.thenByDescending {
                    try { it.quality.substringAfter("S:").substringBefore("|").trim().toInt() } catch(e: Exception) {0}
                })
                foundSources = sorted
                showSourceDialog(sorted)
            }
        }
    }

    private fun setupUI() {
        binding.loadingBar.visibility = View.VISIBLE
        binding.infoText.text = "Checking details..."
        binding.playerView.visibility = View.GONE
        binding.downloadBtn.setOnClickListener { if (foundSources.isNotEmpty()) showSourceDialog(foundSources) }
    }

    private fun showSourceDialog(sources: List<StreamSource>) {
        if (sources.isEmpty()) { binding.infoText.text = "No links found."; return }
        val dialogView = layoutInflater.inflate(R.layout.dialog_list, null)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerSources)
        val title = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val cancel = dialogView.findViewById<Button>(R.id.btnCancel)

        title.text = "Select Source (${sources.size} found)"
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recycler.adapter = com.adeloc.app.ui.adapters.SourceAdapter(sources) { selectedSource ->
            dialog.dismiss()
            currentSource = selectedSource
            showActionDialog(selectedSource)
        }
        cancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), (resources.displayMetrics.heightPixels * 0.90).toInt())
    }

    private fun showActionDialog(source: StreamSource) {
        val options = arrayOf("Play In-App (Real-Debrid)", "Play External (Real-Debrid)", "Play External (No RD)", "Download", "Play In-App (Slow)")
        AlertDialog.Builder(this).setTitle(source.name).setItems(options) { _, i ->
            when (i) {
                0 -> playWithRealDebrid(source, true)
                1 -> playWithRealDebrid(source, false)
                2 -> playUniversally(source)
                3 -> startDownload(source)
                4 -> startTorrentStream(source)
            }
        }.show()
    }

    // --- 5. REAL DEBRID LOGIC ---
    private fun playWithRealDebrid(source: StreamSource, playInternally: Boolean) {
        val token = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("rd_token", "") ?: ""
        if (token.isEmpty()) { Toast.makeText(this, "Login in Settings first!", Toast.LENGTH_LONG).show(); return }

        binding.loadingBar.visibility = View.VISIBLE
        binding.infoText.visibility = View.VISIBLE
        binding.infoText.text = "Resolving Premium Link..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bearer = "Bearer $token"
                val id = RetrofitClient.realDebrid.addMagnet(bearer, source.url).id
                RetrofitClient.realDebrid.selectFiles(bearer, id, "all")

                var link = ""
                repeat(10) {
                    val info = RetrofitClient.realDebrid.getTorrentInfo(bearer, id)
                    if (info.links.isNotEmpty()) { link = info.links[0]; return@repeat }
                    delay(1000)
                }

                if (link.isNotEmpty()) {
                    val finalUrl = RetrofitClient.realDebrid.unrestrictLink(bearer, link).download
                    withContext(Dispatchers.Main) {
                        binding.loadingBar.visibility = View.GONE
                        binding.infoText.visibility = View.GONE
                        if (playInternally) checkPositionAndPlay(finalUrl) else playUniversally(StreamSource(source.name, finalUrl, source.quality, false))
                    }
                } else {
                    withContext(Dispatchers.Main) { Toast.makeText(this@PlayerActivity, "Timed out", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@PlayerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // --- 6. PLAYBACK & SAVE LOGIC ---
    private fun checkPositionAndPlay(url: String) {
        lifecycleScope.launch {
            val history = AppDatabase.get(applicationContext).movieDao().getProgress(tmdbId)
            val pos = if (history != null && history.position > 10000 && history.position < (history.duration * 0.98)) history.position else 0L
            playDirectlyInApp(url, pos)
        }
    }

    private fun playDirectlyInApp(url: String, startPosition: Long) {
        // --- AUTO-SWITCH TO CAST IF CONNECTED ---
        val session = castContext?.sessionManager?.currentCastSession
        if (session != null && session.isConnected) {
            castVideoToTv(url, startPosition)
            return
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        hideSystemUI()
        binding.downloadBtn.visibility = View.GONE
        binding.infoText.visibility = View.GONE
        binding.playerView.visibility = View.VISIBLE
        if (torrentStream.isStreaming) torrentStream.stopStream()

        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        player.setMediaItem(mediaItem)
        if (startPosition > 0) {
            player.seekTo(startPosition)
            Toast.makeText(this, "Resuming...", Toast.LENGTH_SHORT).show()
        }
        player.prepare()
        player.play()
        startProgressTracker()
    }

    private fun startProgressTracker() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (player.isPlaying || player.isLoading) {
                if (player.currentPosition > 1000) saveProgress(player.currentPosition, player.duration)
                delay(10000)
            }
        }
    }

    private fun saveProgress(pos: Long, dur: Long) {
        val urlToSave = currentSource?.url ?: ""
        val qualityToSave = currentSource?.quality ?: ""
        lifecycleScope.launch(Dispatchers.IO) {
            val entity = WatchEntity(tmdbId, movieTitle, posterPath, System.currentTimeMillis(), pos, dur, urlToSave, qualityToSave)
            AppDatabase.get(applicationContext).movieDao().insert(entity)
        }
    }

    // --- DEFAULTS ---
    private fun playUniversally(source: StreamSource) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(source.url)
            if (source.url.startsWith("http")) intent.setDataAndType(Uri.parse(source.url), "video/*")
            startActivity(Intent.createChooser(intent, "Select Video Player"))
            saveProgress(0, 0)
        } catch (e: Exception) { Toast.makeText(this, "No player found", Toast.LENGTH_SHORT).show() }
    }

    private fun startDownload(source: StreamSource) {
        if (source.url.startsWith("magnet")) {
            playUniversally(source)
            Toast.makeText(this, "Select a Torrent App (Flud/ADM)", Toast.LENGTH_LONG).show()
        } else {
            try {
                val request = DownloadManager.Request(Uri.parse(source.url))
                request.setTitle(movieTitle)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${movieTitle}.mp4")
                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this, "Download Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun startTorrentStream(source: StreamSource) {
        if (castContext?.sessionManager?.currentCastSession?.isConnected == true) {
            Toast.makeText(this, "Cannot Cast Torrents directly. Use Real-Debrid.", Toast.LENGTH_LONG).show()
            return
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        hideSystemUI()
        binding.downloadBtn.visibility = View.GONE
        binding.infoText.visibility = View.VISIBLE
        binding.playerView.visibility = View.VISIBLE
        binding.infoText.text = "Initializing Engine (Beta)..."
        binding.loadingBar.visibility = View.VISIBLE
        if (torrentStream.isStreaming) torrentStream.stopStream()
        torrentStream.startStream(source.url)
        saveProgress(0, 0)
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        val options = TorrentOptions.Builder().saveLocation(File(cacheDir, "downloads")).removeFilesAfterStop(true).autoDownload(true).build()
        torrentStream = TorrentStream.init(options)
        torrentStream.addListener(this)
    }

    // Listeners
    override fun onStreamPrepared(torrent: Torrent?) { runOnUiThread { binding.infoText.text = "Metadata loaded..." } }
    override fun onStreamStarted(torrent: Torrent?) { runOnUiThread { binding.infoText.text = "Buffering..." } }
    override fun onStreamReady(torrent: Torrent) {
        runOnUiThread {
            binding.infoText.visibility = View.GONE
            binding.loadingBar.visibility = View.GONE
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(torrent.videoFile)))
            player.prepare()
            player.play()
        }
    }
    override fun onStreamProgress(torrent: Torrent?, status: StreamStatus?) {
        runOnUiThread {
            if (status != null && status.bufferProgress < 100) {
                val speed = status.downloadSpeed / 1024
                binding.infoText.text = "Buffering: ${status.bufferProgress}%\nSpeed: ${speed}KB/s"
            }
        }
    }
    override fun onStreamError(torrent: Torrent?, e: Exception?) { runOnUiThread { binding.infoText.text = "Error: ${e?.message}" } }
    override fun onStreamStopped() {}
    override fun onDestroy() { super.onDestroy(); player.release(); torrentStream.stopStream() }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}