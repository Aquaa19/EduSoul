// File: main/java/com/aquaa/edusoul/adapters/AnnouncementAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.Announcement
import java.util.ArrayList

/*
 * AnnouncementAdapter: RecyclerView adapter for displaying announcements.
 * Handles binding Announcement data to the item_announcement layout and provides
 * callbacks for edit and delete actions (for relevant roles).
 * Migrated to Kotlin.
 */
class AnnouncementAdapter(
    private val context: Context,
    private var announcementList: MutableList<Announcement>,
    private val listener: OnAnnouncementActionsListener? // Listener can be null if no actions are allowed
) : RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder>() {

    interface OnAnnouncementActionsListener {
        fun onEditAnnouncement(announcement: Announcement, position: Int)
        fun onDeleteAnnouncement(announcement: Announcement, position: Int)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_announcement, parent, false)
        return AnnouncementViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: AnnouncementViewHolder, position: Int) {
        val announcement = announcementList[position]

        holder.textViewTitle.text = announcement.title
        holder.textViewContentPreview.text = announcement.content
        holder.textViewAudience.text = "To: ${announcement.targetAudience}"
        holder.textViewDate.text = "Published: ${announcement.publishDate ?: "N/A"}"

        // Re-enabled: Display author's name if available, assuming textViewAnnouncementAuthor now exists in XML
        if (holder.textViewAuthor != null && !announcement.authorName.isNullOrBlank()) {
            holder.textViewAuthor.text = "By: ${announcement.authorName}"
            holder.textViewAuthor.visibility = View.VISIBLE
        } else {
            holder.textViewAuthor?.visibility = View.GONE
        }

        // Set up edit and delete buttons if a listener is provided (e.g., for Admin/Teacher)
        if (listener != null) {
            holder.imageButtonEditAnnouncement.visibility = View.VISIBLE
            holder.imageButtonDeleteAnnouncement.visibility = View.VISIBLE

            holder.imageButtonEditAnnouncement.setOnClickListener {
                listener.onEditAnnouncement(announcement, holder.adapterPosition)
            }
            holder.imageButtonDeleteAnnouncement.setOnClickListener {
                listener.onDeleteAnnouncement(announcement, holder.adapterPosition)
            }
        } else {
            // If no listener, hide action buttons (e.g., for Parent view)
            holder.imageButtonEditAnnouncement.visibility = View.GONE
            holder.imageButtonDeleteAnnouncement.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return announcementList.size
    }

    /**
     * Updates the adapter's data with a new list of announcements and notifies the RecyclerView.
     */
    fun setAnnouncements(newList: List<Announcement>) {
        this.announcementList.clear()
        this.announcementList.addAll(newList)
        notifyDataSetChanged()
    }

    class AnnouncementViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewAnnouncementTitle)
        val textViewContentPreview: TextView = itemView.findViewById(R.id.textViewAnnouncementContentPreview)
        val textViewAudience: TextView = itemView.findViewById(R.id.textViewAnnouncementAudience)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewAnnouncementDate)
        // This TextView is now assumed to be present after the XML update.
        val textViewAuthor: TextView? = itemView.findViewById(R.id.textViewAnnouncementAuthor)

        val imageButtonEditAnnouncement: ImageButton = itemView.findViewById(R.id.imageButtonEditAnnouncement)
        val imageButtonDeleteAnnouncement: ImageButton = itemView.findViewById(R.id.imageButtonDeleteAnnouncement)
    }
}