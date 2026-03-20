package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.SyllabusTopic
// Removed import for SyllabusTopicStatus.java as it's no longer used directly in adapter

/*
 * Adapter for displaying and updating syllabus progress.
 * Migrated to Kotlin and uses SyllabusTopic data class directly.
 */
class SyllabusProgressAdapter(
    private val context: Context,
    private var topicStatusList: MutableList<Pair<SyllabusTopic, Boolean>> // Now a list of Pairs
) : RecyclerView.Adapter<SyllabusProgressAdapter.SyllabusProgressViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyllabusProgressViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_syllabus_topic_progress, parent, false)
        return SyllabusProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: SyllabusProgressViewHolder, position: Int) {
        val (currentTopic, isCompleted) = topicStatusList[position] // Destructure the Pair

        holder.textViewTopicName.text = currentTopic.topicName
        holder.textViewTopicDescription.text = currentTopic.description

        // Remove listener before setting checked state to prevent unwanted callbacks
        holder.checkBoxCompleted.setOnCheckedChangeListener(null)
        holder.checkBoxCompleted.isChecked = isCompleted

        // Set listener after setting checked state
        holder.checkBoxCompleted.setOnCheckedChangeListener { _, checked ->
            // Update the boolean status in the list directly
            topicStatusList[position] = currentTopic to checked
        }
    }

    override fun getItemCount(): Int {
        return topicStatusList.size
    }

    // This method now returns the updated list of pairs, useful for saving changes
    fun getTopicStatusList(): List<Pair<SyllabusTopic, Boolean>> {
        return topicStatusList
    }

    // This method updates the adapter's data with a new list of pairs
    fun setTopicStatusList(newList: MutableList<Pair<SyllabusTopic, Boolean>>) {
        this.topicStatusList = newList
        notifyDataSetChanged()
    }

    class SyllabusProgressViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBoxCompleted: CheckBox = itemView.findViewById(R.id.checkboxTopicCompleted)
        val textViewTopicName: TextView = itemView.findViewById(R.id.textViewTopicNameProgress)
        val textViewTopicDescription: TextView = itemView.findViewById(R.id.textViewTopicDescriptionProgress)
    }
}