// File: main/java/com/aquaa/edusoul/adapters/ClassSessionAdapter.kt
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
import com.aquaa.edusoul.models.ClassSession
import java.util.ArrayList

class ClassSessionAdapter(
    private val context: Context,
    private var sessionList: ArrayList<ClassSession>,
    private val listener: OnSessionActionsListener? = null,
    // Corrected lambda parameter types from Long? to String?
    private val getSubjectNameById: (String?) -> String,
    private val getBatchNameById: (String?) -> String,
    private val getTeacherNameById: (String?) -> String
) : RecyclerView.Adapter<ClassSessionAdapter.SessionViewHolder>() {

    interface OnSessionActionsListener {
        fun onEditSession(session: ClassSession, position: Int)
        fun onDeleteSession(session: ClassSession, position: Int)
    }

    // Secondary constructor for when no listener is provided (e.g., ViewScheduleActivity)
    // No longer used by ManageClassScheduleActivity, but kept for compatibility
    // Corrected lambda parameter types from Long? to String?
    constructor(context: Context, sessionList: ArrayList<ClassSession>,
                getSubjectNameById: (String?) -> String, getBatchNameById: (String?) -> String) :
            this(context, sessionList, null, getSubjectNameById, getBatchNameById, { "N/A" })


    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_class_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: SessionViewHolder, position: Int) {
        val session = sessionList[position]

        holder.textViewSessionTime.text = "${session.startTime} - ${session.endTime}"

        // Use injected lambdas to get names. session.subjectId, session.batchId, session.teacherUserId are already Strings.
        val subjectName = getSubjectNameById(session.subjectId)
        val batchName = getBatchNameById(session.batchId)
        val teacherName = getTeacherNameById(session.teacherUserId)

        holder.textViewSessionSubject.text = subjectName
        holder.textViewSessionBatch.text = "Batch: $batchName"
        holder.textViewSessionTeacher.text = "Teacher: $teacherName" // Display teacher's name

        // Set up edit and delete buttons if a listener is provided
        if (listener != null) {
            holder.buttonEditSession.visibility = View.VISIBLE
            holder.buttonDeleteSession.visibility = View.VISIBLE

            holder.buttonEditSession.setOnClickListener {
                listener.onEditSession(session, holder.adapterPosition)
            }
            holder.buttonDeleteSession.setOnClickListener {
                listener.onDeleteSession(session, holder.adapterPosition)
            }
        } else {
            holder.buttonEditSession.visibility = View.GONE
            holder.buttonDeleteSession.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return sessionList.size
    }

    fun setSessions(newList: List<ClassSession>) {
        this.sessionList.clear()
        this.sessionList.addAll(newList)
        notifyDataSetChanged()
    }

    class SessionViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewSessionTime: TextView = itemView.findViewById(R.id.textViewSessionTime)
        val textViewSessionSubject: TextView = itemView.findViewById(R.id.textViewSessionSubject)
        val textViewSessionBatch: TextView = itemView.findViewById(R.id.textViewSessionBatch)
        val textViewSessionTeacher: TextView = itemView.findViewById(R.id.textViewSessionTeacher)
        val buttonEditSession: ImageButton = itemView.findViewById(R.id.imageButtonEditSession)
        val buttonDeleteSession: ImageButton = itemView.findViewById(R.id.imageButtonDeleteSession)
    }
}