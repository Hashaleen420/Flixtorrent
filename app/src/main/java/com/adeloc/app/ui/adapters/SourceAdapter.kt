package com.adeloc.app.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.adeloc.app.data.model.StreamSource
import com.adeloc.app.databinding.ItemSourceBinding

class SourceAdapter(
    private val sources: List<StreamSource>,
    private val onClick: (StreamSource) -> Unit
) : RecyclerView.Adapter<SourceAdapter.SourceViewHolder>() {

    inner class SourceViewHolder(val binding: ItemSourceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val binding = ItemSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        val source = sources[position]

        // 1. Set Main Title
        // Use regex to remove the messy details from the title if you want it cleaner
        // or just show the full filename:
        holder.binding.sourceTitle.text = source.name

        // 2. Parse Badge and Info
        // Expecting format: "Provider | Info 1 | Info 2"
        val parts = source.quality.split("|")

        if (parts.isNotEmpty()) {
            val providerName = parts[0].trim() // e.g. "Torrentio" or "1337x"
            holder.binding.sourceBadge.text = providerName

            // 3. Set Badge Color
            val badgeColor = when (providerName.lowercase()) {
                "torrentio" -> Color.parseColor("#E91E63") // Pink
                "yts" -> Color.parseColor("#4CAF50")      // Green
                "1337x" -> Color.parseColor("#FF5722")    // Orange
                "eztv" -> Color.parseColor("#2196F3")     // Blue
                "tgx" -> Color.parseColor("#9C27B0")      // Purple
                else -> Color.parseColor("#607D8B")       // Grey
            }
            holder.binding.sourceBadge.setBackgroundColor(badgeColor)

            // 4. Set Info Text (Seeds, Size, Quality)
            if (parts.size > 1) {
                // Joins everything else: "1080p | S:120 | 1.4GB"
                holder.binding.sourceInfo.text = parts.drop(1).joinToString(" | ").trim()
            } else {
                holder.binding.sourceInfo.text = source.quality
            }
        }

        holder.itemView.setOnClickListener { onClick(source) }
    }

    override fun getItemCount() = sources.size
}