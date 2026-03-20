// File: main/java/com/aquaa/edusoul/adapters/SubjectAdapter.kt
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
import com.aquaa.edusoul.models.Subject
import java.util.ArrayList

/*
 * SubjectAdapter: RecyclerView adapter for displaying a list of subjects.
 * Handles binding Subject data to the item_subject layout and provides
 * callbacks for edit and delete actions.
 * Migrated to Kotlin.
 */
class SubjectAdapter(
    private val context: Context,
    private var subjectList: ArrayList<Subject>,
    private val listener: OnSubjectActionsListener?
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    interface OnSubjectActionsListener {
        fun onEditSubject(subject: Subject, position: Int)
        fun onDeleteSubject(subject: Subject, position: Int)
        // NEW: Add method for managing topics
        fun onManageTopics(subject: Subject, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: SubjectViewHolder, position: Int) {
        val subject = subjectList[position]
        holder.textViewSubjectName.text = subject.subjectName
        holder.textViewSubjectCode.text = subject.subjectCode ?: "" // Display blank if null
        holder.textViewSubjectDescription.text = subject.description ?: "" // Display blank if null

        // Set up edit and delete buttons if a listener is provided
        listener?.let {
            holder.imageButtonEditSubject.visibility = View.VISIBLE
            holder.imageButtonDeleteSubject.visibility = View.VISIBLE
            // NEW: Make Manage Topics button visible and set its listener
            holder.imageButtonManageTopics.visibility = View.VISIBLE

            holder.imageButtonEditSubject.setOnClickListener {
                listener.onEditSubject(subject, holder.adapterPosition)
            }
            holder.imageButtonDeleteSubject.setOnClickListener {
                listener.onDeleteSubject(subject, holder.adapterPosition)
            }
            // NEW: Set click listener for Manage Topics button
            holder.imageButtonManageTopics.setOnClickListener {
                listener.onManageTopics(subject, holder.adapterPosition)
            }
        } ?: run {
            // If no listener, hide all action buttons
            holder.imageButtonEditSubject.visibility = View.GONE
            holder.imageButtonDeleteSubject.visibility = View.GONE
            holder.imageButtonManageTopics.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return subjectList.size
    }

    fun setSubjects(newList: List<Subject>) {
        this.subjectList.clear()
        this.subjectList.addAll(newList)
        notifyDataSetChanged()
    }

    class SubjectViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewSubjectName: TextView = itemView.findViewById(R.id.textViewSubjectName)
        val textViewSubjectCode: TextView = itemView.findViewById(R.id.textViewSubjectCode)
        val textViewSubjectDescription: TextView = itemView.findViewById(R.id.textViewSubjectDescription)
        val imageButtonEditSubject: ImageButton = itemView.findViewById(R.id.imageButtonEditSubject)
        val imageButtonDeleteSubject: ImageButton = itemView.findViewById(R.id.imageButtonDeleteSubject)
        // NEW: Reference to the Manage Topics button
        val imageButtonManageTopics: ImageButton = itemView.findViewById(R.id.imageButtonManageTopics)
    }
}