package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.parent.ParentViewResourcesActivity // Import only if still used for conditional logic
import com.aquaa.edusoul.models.LearningResource
import java.util.ArrayList

class LearningResourceAdapter(
    private val context: Context,
    private var resourceList: MutableList<LearningResource>,
    private val listener: OnLearningResourceActionsListener?
) : RecyclerView.Adapter<LearningResourceAdapter.ResourceViewHolder>() {

    interface OnLearningResourceActionsListener {
        fun onEditResource(resource: LearningResource, position: Int)
        fun onDeleteResource(resource: LearningResource, position: Int)
        fun onViewResource(resource: LearningResource)
        fun onDownloadResource(resource: LearningResource) // NEW: Add download listener
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_learning_resource, parent, false)
        return ResourceViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ResourceViewHolder, position: Int) {
        val resource = resourceList[position]
        holder.textViewTitle.text = resource.title
        holder.textViewDescription.text = resource.description

        when {
            resource.fileMimeType.startsWith("image/") -> holder.imageViewIcon.setImageResource(R.drawable.ic_photo)
            resource.fileMimeType.startsWith("video/") -> holder.imageViewIcon.setImageResource(R.drawable.ic_results)
            resource.fileMimeType == "application/pdf" || resource.fileMimeType == "application/msword" || resource.fileMimeType.startsWith("application/vnd.openxmlformats-officedocument") -> holder.imageViewIcon.setImageResource(R.drawable.ic_assignment)
            resource.fileMimeType == "text/html" -> holder.imageViewIcon.setImageResource(R.drawable.ic_add_link)
            else -> holder.imageViewIcon.setImageResource(R.drawable.ic_add)
        }

        holder.imageButtonView.setOnClickListener {
            listener?.onViewResource(resource)
        }

        // NEW: Set click listener for download button
        holder.imageButtonDownload.setOnClickListener {
            listener?.onDownloadResource(resource)
        }

        // Conditionally show/hide edit and delete buttons based on the listener's type
        // Assuming ParentViewResourcesActivity only allows view/download, not edit/delete
        val isParentView = context is ParentViewResourcesActivity // Check if the context is the ParentViewResourcesActivity
        if (isParentView) {
            holder.imageButtonEdit.visibility = View.GONE
            holder.imageButtonDelete.visibility = View.GONE
            holder.imageButtonEdit.setOnClickListener(null)
            holder.imageButtonDelete.setOnClickListener(null)
            // Ensure download button is visible in parent view
            holder.imageButtonDownload.visibility = View.VISIBLE
        } else {
            // For other views (e.g., Teacher), show all buttons and set listeners.
            holder.imageButtonEdit.visibility = View.VISIBLE
            holder.imageButtonDelete.visibility = View.VISIBLE
            holder.imageButtonEdit.setOnClickListener {
                listener?.onEditResource(resource, holder.adapterPosition)
            }
            holder.imageButtonDelete.setOnClickListener {
                listener?.onDeleteResource(resource, holder.adapterPosition)
            }
            holder.imageButtonDownload.visibility = View.VISIBLE // Also visible for teachers
        }
    }

    override fun getItemCount(): Int {
        return resourceList.size
    }

    fun setResources(newList: List<LearningResource>) {
        this.resourceList.clear()
        this.resourceList.addAll(newList)
        notifyDataSetChanged()
    }

    class ResourceViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewResourceIcon)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewResourceTitle)
        val textViewDescription: TextView = itemView.findViewById(R.id.textViewResourceDescription)
        val imageButtonView: ImageButton = itemView.findViewById(R.id.imageButtonViewResource)
        val imageButtonDownload: ImageButton = itemView.findViewById(R.id.imageButtonDownloadResource) // NEW
        val imageButtonEdit: ImageButton = itemView.findViewById(R.id.imageButtonEditResource)
        val imageButtonDelete: ImageButton = itemView.findViewById(R.id.imageButtonDeleteResource)
    }
}