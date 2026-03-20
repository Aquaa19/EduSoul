// File: main/java/com/aquaa/edusoul/adapters/TeacherAssignmentAdapter.kt
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
import com.aquaa.edusoul.models.TeacherAssignmentDetails

class TeacherAssignmentAdapter(
    private val context: Context,
    private var assignmentList: MutableList<TeacherAssignmentDetails>,
    private val listener: OnAssignmentRemoveListener?
) : RecyclerView.Adapter<TeacherAssignmentAdapter.AssignmentViewHolder>() {

    interface OnAssignmentRemoveListener {
        fun onRemoveAssignment(assignment: TeacherAssignmentDetails, position: Int)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): AssignmentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_teacher_assignment, parent, false)
        return AssignmentViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: AssignmentViewHolder, position: Int) {
        val assignment = assignmentList[position]
        val assignmentText = "${assignment.subjectName}  —  ${assignment.batchName}"
        holder.textViewAssignmentDetails.text = assignmentText
        holder.buttonRemoveAssignment.setOnClickListener {
            listener?.onRemoveAssignment(assignment, holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return assignmentList.size
    }

    fun setAssignments(newList: List<TeacherAssignmentDetails>) {
        this.assignmentList.clear()
        this.assignmentList.addAll(newList) // Removed unnecessary cast
        notifyDataSetChanged()
    }

    class AssignmentViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewAssignmentDetails: TextView = itemView.findViewById(R.id.textViewAssignmentDetails)
        val buttonRemoveAssignment: ImageButton = itemView.findViewById(R.id.buttonRemoveAssignment)
    }
}