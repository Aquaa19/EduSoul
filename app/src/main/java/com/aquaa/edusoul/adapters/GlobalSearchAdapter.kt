// File: main/java/com/aquaa/edusoul/adapters/GlobalSearchAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.SearchItem

class GlobalSearchAdapter(
    private val context: Context,
    private var searchResults: MutableList<SearchItem>,
    private val onItemClickListener: OnItemClickListener? = null
) : RecyclerView.Adapter<GlobalSearchAdapter.SearchResultViewHolder>() {

    interface OnItemClickListener {
        fun onSearchResultClick(item: SearchItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val item = searchResults[position]

        holder.textViewTitle.text = item.getTitle()
        holder.textViewSubtitle.text = item.getSubtitle()

        val iconResId = item.getIconResId()
        if (iconResId != 0) {
            holder.imageViewIcon.setImageResource(iconResId)
            // Removed: holder.imageViewIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
            // The tint is already applied via `app:tint="?attr/colorPrimary"` in the XML layout.
        } else {
            // Fallback to a default or hide if no icon is specified
            holder.imageViewIcon.setImageResource(R.drawable.ic_search) // Default search icon
            // Removed: holder.imageViewIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
            // The tint is already applied via `app:tint="?attr/colorPrimary"` in the XML layout.
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.onSearchResultClick(item)
        }
    }

    override fun getItemCount(): Int = searchResults.size

    fun updateData(newResults: List<SearchItem>) {
        this.searchResults.clear()
        this.searchResults.addAll(newResults)
        notifyDataSetChanged()
    }

    class SearchResultViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewResultIcon)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewResultTitle)
        val textViewSubtitle: TextView = itemView.findViewById(R.id.textViewResultSubtitle)
    }
}
