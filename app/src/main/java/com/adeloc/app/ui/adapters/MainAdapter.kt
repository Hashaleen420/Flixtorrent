package com.adeloc.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adeloc.app.R
import com.adeloc.app.ui.RowItem

class MainAdapter(private val rows: List<RowItem>) :
    RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

    class MainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.categoryTitle)
        val childRecycler: RecyclerView = view.findViewById(R.id.childRecycler)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return MainViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val row = rows[position]
        holder.title.text = row.title

        // Create horizontal layout for the posters
        val layoutManager = LinearLayoutManager(
            holder.itemView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        holder.childRecycler.layoutManager = layoutManager
        holder.childRecycler.adapter = MovieAdapter(row.movies)
    }

    override fun getItemCount() = rows.size
}