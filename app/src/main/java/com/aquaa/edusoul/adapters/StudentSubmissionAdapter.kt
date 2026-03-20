// main/java/com/aquaa/edusoul/adapters/StudentSubmissionAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.StudentAssignment
import com.aquaa.edusoul.models.StudentAssignmentStatus

class StudentSubmissionAdapter(
    private val context: Context,
    private var submissions: MutableList<StudentAssignmentStatus>,
    private val listener: OnSubmissionActionsListener
) : RecyclerView.Adapter<StudentSubmissionAdapter.SubmissionViewHolder>() {

    interface OnSubmissionActionsListener {
        fun onViewSubmission(submission: StudentAssignmentStatus)
        fun onRemind(submission: StudentAssignmentStatus)
        // Add a new action for grading
        fun onGrade(submission: StudentAssignmentStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_student_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        val submission = submissions[position]
        holder.bind(submission)
    }

    override fun getItemCount(): Int = submissions.size

    fun updateSubmissions(newSubmissions: List<StudentAssignmentStatus>) {
        this.submissions.clear()
        this.submissions.addAll(newSubmissions)
        notifyDataSetChanged()
    }

    inner class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val studentName: TextView = itemView.findViewById(R.id.textViewStudentName)
        private val submissionStatus: TextView = itemView.findViewById(R.id.textViewSubmissionStatus)
        private val actionButton: Button = itemView.findViewById(R.id.buttonAction)

        fun bind(submission: StudentAssignmentStatus) {
            studentName.text = submission.studentName

            // Use non-null asserted call (!!) because assignment is expected to be present for status display
            var statusText = "Status: ${submission.assignment!!.status}"
            if (submission.assignment!!.status == StudentAssignment.STATUS_SUBMITTED && submission.submissionDate != null) {
                statusText += " on ${submission.submissionDate}"
            }
            submissionStatus.text = statusText

            when (submission.assignment!!.status) {
                StudentAssignment.STATUS_SUBMITTED -> {
                    actionButton.text = "Grade"
                    actionButton.setTextColor(ContextCompat.getColor(context, R.color.green_paid))
                    actionButton.setOnClickListener { listener.onGrade(submission) }
                }
                StudentAssignment.STATUS_GRADED -> {
                    actionButton.text = "View Graded"
                    actionButton.setTextColor(ContextCompat.getColor(context, R.color.blue_theme_light_primary))
                    actionButton.setOnClickListener { listener.onGrade(submission) } // Re-use onGrade for viewing/editing grade
                }
                StudentAssignment.STATUS_ASSIGNED, StudentAssignment.STATUS_MISSED -> {
                    actionButton.text = "Remind"
                    actionButton.setTextColor(ContextCompat.getColor(context, R.color.orange_overpaid))
                    actionButton.setOnClickListener { listener.onRemind(submission) }
                }
                else -> {
                    actionButton.visibility = View.GONE
                }
            }
        }
    }
}