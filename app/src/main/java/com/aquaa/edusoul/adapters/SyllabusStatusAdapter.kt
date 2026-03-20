// File: main/java/com/aquaa/edusoul/adapters/SyllabusStatusAdapter.kt
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
import com.aquaa.edusoul.models.SyllabusTopicStatusAdmin

class SyllabusStatusAdapter(
    private val context: Context,
    private var topicStatusList: MutableList<SyllabusTopicStatusAdmin>
) : RecyclerView.Adapter<SyllabusStatusAdapter.StatusViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): StatusViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_syllabus_status_report, parent, false)
        return StatusViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: StatusViewHolder, position: Int) {
        val status = topicStatusList[position]
        holder.topicName.text = status.topicName

        if (status.isCompleted) {
            holder.statusIcon.setImageResource(R.drawable.ic_check_circle)
            holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.log_level_debug)) // Green tint

            var teacherDisplayName = status.teacherName
            if (teacherDisplayName.isNullOrEmpty()) {
                teacherDisplayName = "Unknown Teacher"
            }
            val details = "Completed by $teacherDisplayName on ${status.completionDate ?: "N/A"}"
            holder.completionDetails.text = details
            holder.completionDetails.visibility = View.VISIBLE
        } else {
            holder.statusIcon.setImageResource(R.drawable.ic_radio_button_unchecked)
            holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.dark_grey)) // Grey tint
            holder.completionDetails.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return topicStatusList.size
    }

    fun setTopics(newList: List<SyllabusTopicStatusAdmin>) {
        this.topicStatusList.clear()
        this.topicStatusList.addAll(newList) // Removed unnecessary cast to ArrayList
        notifyDataSetChanged()
    }

    class StatusViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val statusIcon: ImageView = itemView.findViewById(R.id.imageViewStatusIcon)
        val topicName: TextView = itemView.findViewById(R.id.textViewTopicNameStatus)
        val completionDetails: TextView = itemView.findViewById(R.id.textViewCompletionDetails)
    }
}