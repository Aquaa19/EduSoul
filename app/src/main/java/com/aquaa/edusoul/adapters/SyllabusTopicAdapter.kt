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
import com.aquaa.edusoul.models.SyllabusTopic
import java.util.ArrayList

/*
 * SyllabusTopicAdapter: RecyclerView adapter for displaying a list of syllabus topics.
 * Handles binding SyllabusTopic data to the item_syllabus_topic layout and provides
 * callbacks for edit and delete actions.
 * Migrated to Kotlin.
 */
class SyllabusTopicAdapter(
    private val context: Context,
    private var topicList: ArrayList<SyllabusTopic>,
    private val listener: OnTopicActionsListener?
) : RecyclerView.Adapter<SyllabusTopicAdapter.TopicViewHolder>() {

    interface OnTopicActionsListener {
        fun onEditTopic(topic: SyllabusTopic, position: Int)
        fun onDeleteTopic(topic: SyllabusTopic, position: Int)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_syllabus_topic, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: TopicViewHolder, position: Int) {
        val topic = topicList[position]
        holder.textViewTopicName.text = topic.topicName
        holder.textViewTopicDescription.text = topic.description ?: "" // Display blank if null
        holder.textViewTopicOrder.text = "Order: ${topic.order ?: "N/A"}" // Display order

        // Set up edit and delete buttons if a listener is provided
        listener?.let {
            holder.imageButtonEditTopic.visibility = View.VISIBLE
            holder.imageButtonDeleteTopic.visibility = View.VISIBLE

            holder.imageButtonEditTopic.setOnClickListener {
                listener.onEditTopic(topic, holder.adapterPosition)
            }
            holder.imageButtonDeleteTopic.setOnClickListener {
                listener.onDeleteTopic(topic, holder.adapterPosition)
            }
        } ?: run {
            holder.imageButtonEditTopic.visibility = View.GONE
            holder.imageButtonDeleteTopic.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return topicList.size
    }

    fun setTopics(newList: List<SyllabusTopic>) {
        this.topicList.clear()
        this.topicList.addAll(newList)
        notifyDataSetChanged()
    }

    class TopicViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTopicName: TextView = itemView.findViewById(R.id.textViewTopicName)
        val textViewTopicDescription: TextView = itemView.findViewById(R.id.textViewTopicDescription)
        val textViewTopicOrder: TextView = itemView.findViewById(R.id.textViewTopicOrder) // Added for order
        val imageButtonEditTopic: ImageButton = itemView.findViewById(R.id.imageButtonEditTopic)
        val imageButtonDeleteTopic: ImageButton = itemView.findViewById(R.id.imageButtonDeleteTopic)
    }
}