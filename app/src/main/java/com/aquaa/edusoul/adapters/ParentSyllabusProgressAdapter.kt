// File: main/java/com/aquaa/edusoul/adapters/ParentSyllabusProgressAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.core.content.ContextCompat // For getting colors from resources
import androidx.recyclerview.widget.RecyclerView

import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.ParentSyllabusProgressDetails

// Updated to Kotlin
class ParentSyllabusProgressAdapter(
    private val context: Context,
    private var progressDetailsList: List<ParentSyllabusProgressDetails>, // Now uses Kotlin List
    private val listener: OnItemClickListener?
) : RecyclerView.Adapter<ParentSyllabusProgressAdapter.ParentSyllabusProgressViewHolder>() {

    interface OnItemClickListener {
        fun onSyllabusTopicCardClick(progressDetails: ParentSyllabusProgressDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentSyllabusProgressViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_parent_syllabus_progress, parent, false)
        return ParentSyllabusProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParentSyllabusProgressViewHolder, position: Int) {
        val currentItem = progressDetailsList[position]

        holder.textViewTopicName.text = currentItem.topicName
        holder.textViewSubjectName.text = "Subject: ${currentItem.subjectName}"
        holder.textViewBatchName.text = "Batch: ${currentItem.batchName}"

        // Topic Description visibility
        val description = currentItem.description
        if (!description.isNullOrEmpty()) {
            holder.textViewTopicDescription.text = "Description: $description"
            holder.textViewTopicDescription.visibility = View.VISIBLE
        } else {
            holder.textViewTopicDescription.visibility = View.GONE
        }

        // Completion Status, Date, and Progress Bar
        if (currentItem.isCompleted) {
            holder.textViewCompletionStatus.text = "Status: Completed"
            holder.textViewCompletionStatus.setTextColor(ContextCompat.getColor(context, R.color.green_paid))
            holder.progressBarTopicProgress.progress = 100
            holder.progressBarTopicProgress.progressTintList = ContextCompat.getColorStateList(context, R.color.green_paid)

            val completionDate = currentItem.completionDate
            if (!completionDate.isNullOrEmpty()) {
                holder.textViewCompletionDate.text = "Date: $completionDate"
                holder.textViewCompletionDate.visibility = View.VISIBLE
            } else {
                holder.textViewCompletionDate.visibility = View.GONE
            }
        } else {
            holder.textViewCompletionStatus.text = "Status: Pending"
            holder.textViewCompletionStatus.setTextColor(ContextCompat.getColor(context, R.color.red_due))
            holder.progressBarTopicProgress.progress = 0
            holder.progressBarTopicProgress.progressTintList = ContextCompat.getColorStateList(context, R.color.red_due)
            holder.textViewCompletionDate.visibility = View.GONE
        }

        // Updated By Teacher visibility
        val teacherName = currentItem.teacherName
        if (!teacherName.isNullOrEmpty() && teacherName != "N/A") {
            holder.textViewUpdatedByTeacher.text = "Updated by: $teacherName"
            holder.textViewUpdatedByTeacher.visibility = View.VISIBLE
        } else {
            holder.textViewUpdatedByTeacher.visibility = View.GONE
        }

        // Set click listener for the entire card (optional)
        holder.itemView.setOnClickListener {
            listener?.onSyllabusTopicCardClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return progressDetailsList.size
    }

    // Method to update the data and notify the adapter of changes
    fun updateData(newData: List<ParentSyllabusProgressDetails>) {
        this.progressDetailsList = newData
        notifyDataSetChanged()
    }

    class ParentSyllabusProgressViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTopicName: TextView = itemView.findViewById(R.id.textViewTopicName)
        val textViewSubjectName: TextView = itemView.findViewById(R.id.textViewSubjectName)
        val textViewBatchName: TextView = itemView.findViewById(R.id.textViewBatchName)
        val textViewTopicDescription: TextView = itemView.findViewById(R.id.textViewTopicDescription)
        val textViewCompletionStatus: TextView = itemView.findViewById(R.id.textViewCompletionStatus)
        val textViewCompletionDate: TextView = itemView.findViewById(R.id.textViewCompletionDate)
        val textViewUpdatedByTeacher: TextView = itemView.findViewById(R.id.textViewUpdatedByTeacher)
        val progressBarTopicProgress: ProgressBar = itemView.findViewById(R.id.progressBarTopicProgress)
    }
}