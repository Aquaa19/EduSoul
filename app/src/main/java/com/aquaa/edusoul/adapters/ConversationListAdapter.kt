package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.viewmodels.UserWithUnreadCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ConversationListAdapter(
    private val context: Context,
    private var contactList: List<UserWithUnreadCount>,
    private val listener: OnContactClickListener
) : RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {

    interface OnContactClickListener {
        fun onContactClick(contact: User)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactWithUnread = contactList[position]
        holder.bind(contactWithUnread, listener)
    }

    override fun getItemCount(): Int = contactList.size

    fun updateContacts(newContacts: List<UserWithUnreadCount>) {
        this.contactList = newContacts
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewUserIcon: ImageView = itemView.findViewById(R.id.imageViewUserIcon)
        private val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName)
        private val textViewUnreadCount: TextView = itemView.findViewById(R.id.textViewUnreadCount)
        private val textViewLastMessageContent: TextView = itemView.findViewById(R.id.textViewLastMessageContent)
        private val textViewLastMessageTimestamp: TextView = itemView.findViewById(R.id.textViewLastMessageTimestamp)

        fun bind(userWithUnread: UserWithUnreadCount, listener: OnContactClickListener) {
            val user = userWithUnread.user
            textViewUserName.text = user.fullName
            if (userWithUnread.unreadCount > 0) {
                textViewUnreadCount.text = userWithUnread.unreadCount.toString()
                textViewUnreadCount.visibility = View.VISIBLE
            } else {
                textViewUnreadCount.visibility = View.GONE
            }

            val iconResId = when(user.role) {
                User.ROLE_TEACHER -> R.drawable.ic_teacher
                User.ROLE_PARENT -> R.drawable.ic_parent
                else -> R.drawable.ic_person_placeholder
            }
            imageViewUserIcon.setImageResource(iconResId)

            // Bind the last message content and format the timestamp
            textViewLastMessageContent.text = userWithUnread.lastMessageContent ?: "No messages yet."
            textViewLastMessageTimestamp.text = formatTimestamp(userWithUnread.lastMessageTimestamp)

            itemView.setOnClickListener {
                listener.onContactClick(user)
            }
        }

        private fun formatTimestamp(timestamp: String?): String {
            if (timestamp.isNullOrBlank()) {
                return "N/A"
            }

            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val date = sdf.parse(timestamp)
                val now = Date()
                val diff = now.time - date.time

                when {
                    diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                    diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
                    diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
                    else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
                }
            } catch (e: Exception) {
                timestamp
            }
        }
    }
}