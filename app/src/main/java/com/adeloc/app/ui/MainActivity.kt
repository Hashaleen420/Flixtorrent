package com.adeloc.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.adeloc.app.data.api.RetrofitClient
import com.adeloc.app.data.db.AppDatabase
import com.adeloc.app.data.model.Movie
import com.adeloc.app.databinding.ActivityMainBinding
import com.adeloc.app.ui.adapters.MainAdapter
import com.adeloc.app.utils.Constants
import kotlinx.coroutines.launch

// Wrapper to hold Row Title + List of Movies
data class RowItem(val title: String, val movies: List<Movie>)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainRecycler.layoutManager = LinearLayoutManager(this)

        binding.swipeRefresh.setOnRefreshListener {
            loadHomeContent()
        }

        // Toggle Search Box
        binding.searchBtn.setOnClickListener {
            binding.searchView.visibility =
                if (binding.searchView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // NEW: Open Settings Activity (Real-Debrid Login)
        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) search(query)
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })

        loadHomeContent()
    }

    private fun loadHomeContent() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            val rows = mutableListOf<RowItem>()

            try {
                // 1. CONTINUE WATCHING (Always First)
                val history = AppDatabase.get(applicationContext).movieDao().getHistory()
                if (history.isNotEmpty()) {
                    val historyMovies = history.take(10).map {
                        Movie(it.tmdbId, it.title, null, it.posterPath, null, null)
                    }
                    rows.add(RowItem("Continue Watching", historyMovies))
                }

                // 2. TRENDING (New & Existing)
                // NEW: Viral Right Now (Movies + TV)
                val trendingToday = RetrofitClient.tmdb.getTrending(Constants.TMDB_KEY)
                rows.add(RowItem("Trending Today 🔥", trendingToday.results))

                // Existing: Popular Movies
                val popular = RetrofitClient.tmdb.getPopular(Constants.TMDB_KEY)
                rows.add(RowItem("Trending Movies", popular.results))

                // Existing: Popular TV
                val tvSeries = RetrofitClient.tmdb.getTrendingTV(Constants.TMDB_KEY)
                rows.add(RowItem("Popular Series", tvSeries.results))

                // 3. FLIXORENT EXCLUSIVE
                // Shuffled list to make it look unique
                rows.add(RowItem("Only on FLIXORENT", popular.results.shuffled()))

                // 4. STREAMING GIANTS (The "Big Three")
                val netflix = RetrofitClient.tmdb.getByProvider(Constants.TMDB_KEY, "8")
                rows.add(RowItem("Netflix Originals", netflix.results))

                val disney = RetrofitClient.tmdb.getByProvider(Constants.TMDB_KEY, "337")
                rows.add(RowItem("Disney+ Favorites", disney.results))

                val apple = RetrofitClient.tmdb.getByProvider(Constants.TMDB_KEY, "350")
                rows.add(RowItem("Apple TV+ Exclusives", apple.results))

                // 5. OTHER PLATFORMS
                val amazon = RetrofitClient.tmdb.getByProvider(Constants.TMDB_KEY, "9")
                rows.add(RowItem("Popular on Amazon Prime", amazon.results))

                val hulu = RetrofitClient.tmdb.getByProvider(Constants.TMDB_KEY, "15")
                rows.add(RowItem("Streaming on Hulu", hulu.results))

                // 6. SPECIAL LISTS
                val topRated = RetrofitClient.tmdb.getTopRated(Constants.TMDB_KEY)
                rows.add(RowItem("Critically Acclaimed (Top Rated)", topRated.results))

                val theaters = RetrofitClient.tmdb.getInTheaters(Constants.TMDB_KEY)
                rows.add(RowItem("Now in Theaters", theaters.results))

                val upcoming = RetrofitClient.tmdb.getUpcoming(Constants.TMDB_KEY)
                rows.add(RowItem("Coming Soon", upcoming.results))

                // 7. GENRE COLLECTIONS
                val anime = RetrofitClient.tmdb.getAnime(Constants.TMDB_KEY)
                rows.add(RowItem("Anime Hits", anime.results))

                val action = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "28")
                rows.add(RowItem("Action & Adventure", action.results))

                val scifi = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "878")
                rows.add(RowItem("Sci-Fi Universes", scifi.results))

                val comedy = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "35")
                rows.add(RowItem("Comedy Collections", comedy.results))

                val horror = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "27")
                rows.add(RowItem("Late Night Horror", horror.results))

                val romance = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "10749")
                rows.add(RowItem("Romantic Picks", romance.results))

                val drama = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "18")
                rows.add(RowItem("Award Winning Dramas", drama.results))

                val docu = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "99")
                rows.add(RowItem("Documentaries", docu.results))

                val family = RetrofitClient.tmdb.getByGenre(Constants.TMDB_KEY, "10751")
                rows.add(RowItem("Kids & Family", family.results))

                // Finally, set the adapter
                binding.mainRecycler.adapter = MainAdapter(rows)

            } catch (e: Exception) {
                e.printStackTrace()
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun search(query: String) {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                val rows = mutableListOf<RowItem>()

                // 1. Movies
                val res = RetrofitClient.tmdb.search(Constants.TMDB_KEY, query)
                if (res.results.isNotEmpty()) {
                    rows.add(RowItem("Movies: '$query'", res.results))
                }

                // 2. TV Series
                val tvRes = RetrofitClient.tmdb.searchTv(Constants.TMDB_KEY, query)
                if (tvRes.results.isNotEmpty()) {
                    rows.add(RowItem("TV Series: '$query'", tvRes.results))
                }

                binding.mainRecycler.adapter = MainAdapter(rows)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }
}