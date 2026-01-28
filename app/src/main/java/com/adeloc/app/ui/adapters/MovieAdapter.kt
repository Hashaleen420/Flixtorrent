package com.adeloc.app.ui.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.adeloc.app.R
import com.adeloc.app.data.model.Movie
import com.adeloc.app.ui.PlayerActivity
import com.adeloc.app.utils.Constants
import com.bumptech.glide.Glide

class MovieAdapter(private val movies: List<Movie>) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.moviePoster)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        // Load poster from TMDB using Glide
        Glide.with(holder.itemView.context)
            .load(Constants.IMG_URL + movie.poster_path)
            .placeholder(android.R.color.darker_gray)
            .into(holder.poster)

        // Open Player on click
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlayerActivity::class.java).apply {
                putExtra("TMDB_ID", movie.id)
                putExtra("MOVIE_TITLE", movie.title)
                putExtra("POSTER", movie.poster_path)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = movies.size
}