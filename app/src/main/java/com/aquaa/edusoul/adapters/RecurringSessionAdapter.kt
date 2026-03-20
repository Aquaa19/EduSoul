// File: main/java/com/aquaa/edusoul/adapters/RecurringSessionAdapter.kt
package com.aquaa.edusoul.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.RecurringClassSession
import java.util.*

/*
 * RecurringSessionAdapter: RecyclerView adapter for displaying recurring class sessions.
 * Provides callbacks for edit and delete actions.
 */
class RecurringSessionAdapter(
    private var sessionList: MutableList<RecurringClassSession>,
    private val listener: OnRecurringSessionActionListener? = null,
    // Corrected lambda parameter types from Long? to String?
    private val getSubjectName: (String?) -> String,
    private val getTeacherName: (String?) -> String
) : RecyclerView.Adapter<RecurringSessionAdapter.SessionViewHolder>() {

    interface OnRecurringSessionActionListener {
        fun onEditSession(session: RecurringClassSession)
        fun onDeleteSession(session: RecurringClassSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recurring_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessionList[position]

        holder.textViewSessionTime.text = "${session.startTime} - ${session.endTime}"
        // session.subjectId and session.teacherUserId are already String?
        holder.textViewSessionSubject.text = getSubjectName(session.subjectId)
        holder.textViewSessionTeacher.text = getTeacherName(session.teacherUserId)

        // Set up edit and delete buttons if a listener is provided
        listener?.let { currentListener -> // Capture the non-null listener with a distinct name
            // The edit button (imageButtonEditRecurringSession) is not in the provided XML.
            // So, its visibility is set to GONE here and its listener is conditionally set.
            holder.imageButtonEditRecurringSession?.visibility = View.GONE // Hide edit button if not in layout

            holder.buttonDeleteRecurringSession.visibility = View.VISIBLE // Use the correct ID for delete button

            holder.imageButtonEditRecurringSession?.setOnClickListener { // This 'it' is the View
                currentListener.onEditSession(session) // Use the captured listener
            }
            holder.buttonDeleteRecurringSession.setOnClickListener { // This 'it' is the View
                currentListener.onDeleteSession(session) // Use the captured listener
            }
        } ?: run {
            holder.imageButtonEditRecurringSession?.visibility = View.GONE
            holder.buttonDeleteRecurringSession.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return sessionList.size
    }

    fun updateData(newSessions: List<RecurringClassSession>) {
        sessionList.clear()
        sessionList.addAll(newSessions.sortedWith(compareBy({ it.startTime }, { it.endTime })))
        notifyDataSetChanged()
    }

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Corrected variable names to match XML IDs
        val textViewSessionTime: TextView = itemView.findViewById(R.id.textViewSessionTime)
        val textViewSessionSubject: TextView = itemView.findViewById(R.id.textViewSessionSubject)
        val textViewSessionTeacher: TextView = itemView.findViewById(R.id.textViewSessionTeacher)
        // `imageButtonEditRecurringSession` ID is not present in item_recurring_session.xml.
        // It's declared as nullable to avoid NullPointerException.
        val imageButtonEditRecurringSession: ImageButton? = itemView.findViewById(R.id.imageButtonEditRecurringSession)
        // `buttonDeleteRecurringSession` is the correct ID from item_recurring_session.xml.
        val buttonDeleteRecurringSession: ImageButton = itemView.findViewById(R.id.buttonDeleteRecurringSession)
    }
}