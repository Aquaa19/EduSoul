// File: main/java/com/aquaa/edusoul/adapters/MessageAdapter.kt
package com.aquaa.edusoul.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.Message
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(private val currentUserId: String) : // Changed currentUserId type to String
    ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder.itemViewType == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).bind(message)
        } else {
            (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) { // Comparison is now String == String
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContent: TextView = itemView.findViewById(R.id.textViewMessageContent)
        private val timestamp: TextView = itemView.findViewById(R.id.textViewMessageTimestamp)

        fun bind(message: Message) {
            messageContent.text = message.messageContent
            timestamp.text = formatTimestamp(message.timestamp)
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContent: TextView = itemView.findViewById(R.id.textViewMessageContent)
        private val timestamp: TextView = itemView.findViewById(R.id.textViewMessageTimestamp)

        fun bind(message: Message) {
            messageContent.text = message.messageContent
            timestamp.text = formatTimestamp(message.timestamp)
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(timestamp)
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            timestamp
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}