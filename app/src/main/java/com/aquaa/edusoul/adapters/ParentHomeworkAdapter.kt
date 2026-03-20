package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.ParentHomeworkDetails
import com.aquaa.edusoul.models.StudentAssignment
import java.io.File // Keep this import if still used for local path handling in `ParentViewHomeworkActivity` (though it should be Firebase Storage URL now)

class ParentHomeworkAdapter(
    private val context: Context,
    private var homeworkList: MutableList<ParentHomeworkDetails>,
    private val listener: OnHomeworkInteractionListener
) : RecyclerView.Adapter<ParentHomeworkAdapter.HomeworkViewHolder>() {

    interface OnHomeworkInteractionListener {
        fun onAttachFile(homework: ParentHomeworkDetails)
        fun onConfirmSubmit(homework: ParentHomeworkDetails)
        fun onViewSubmission(homework: ParentHomeworkDetails)
        fun onDeleteSubmission(homework: ParentHomeworkDetails)
        fun onViewAttachment(homework: ParentHomeworkDetails) // Updated to pass ParentHomeworkDetails
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeworkViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_parent_homework, parent, false)
        return HomeworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeworkViewHolder, position: Int) {
        val homework = homeworkList[position]
        holder.bind(homework)
    }

    override fun getItemCount(): Int = homeworkList.size

    fun updateData(newHomeworkList: List<ParentHomeworkDetails>) {
        this.homeworkList.clear()
        this.homeworkList.addAll(newHomeworkList)
        notifyDataSetChanged()
    }

    inner class HomeworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewHomeworkTitle)
        private val subject: TextView = itemView.findViewById(R.id.textViewSubject)
        private val description: TextView = itemView.findViewById(R.id.textViewDescription)
        private val dueDate: TextView = itemView.findViewById(R.id.textViewDueDate)
        private val viewAttachmentButton: Button = itemView.findViewById(R.id.buttonViewAttachment)
        private val attachFileButton: Button = itemView.findViewById(R.id.buttonAttachFile)
        private val confirmSubmitButton: Button = itemView.findViewById(R.id.buttonConfirmSubmit)
        private val deleteButton: Button = itemView.findViewById(R.id.buttonDeleteSubmission)
        private val attachedFileName: TextView = itemView.findViewById(R.id.textViewAttachedFileName)
        private val submissionActions: LinearLayout = itemView.findViewById(R.id.submissionActions)
        private val gradingLayout: LinearLayout = itemView.findViewById(R.id.gradingLayout)
        private val marksTextView: TextView = itemView.findViewById(R.id.textViewMarks)
        private val remarksTextView: TextView = itemView.findViewById(R.id.textViewRemarks)


        fun bind(homework: ParentHomeworkDetails) {
            title.text = homework.homeworkTitle
            subject.text = "Subject: ${homework.subjectName}"
            description.text = homework.homeworkDescription
            dueDate.text = "Due: ${homework.dueDate}"

            // Handle teacher's attachment
            if (!homework.homeworkAttachmentPath.isNullOrEmpty() && !homework.homeworkAttachmentMimeType.isNullOrEmpty()) { // Check MIME type too
                viewAttachmentButton.visibility = View.VISIBLE
                viewAttachmentButton.setOnClickListener { listener.onViewAttachment(homework) } // Pass the whole object
            } else {
                viewAttachmentButton.visibility = View.GONE
            }

            // Handle student's submission state
            when (homework.status) {
                StudentAssignment.STATUS_SUBMITTED -> {
                    submissionActions.visibility = View.VISIBLE
                    attachFileButton.visibility = View.GONE
                    confirmSubmitButton.text = "View Submission"
                    confirmSubmitButton.setOnClickListener { listener.onViewSubmission(homework) }
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener { listener.onDeleteSubmission(homework) }
                    attachedFileName.visibility = View.GONE
                    gradingLayout.visibility = View.GONE
                }
                StudentAssignment.STATUS_GRADED -> {
                    submissionActions.visibility = View.GONE
                    gradingLayout.visibility = View.VISIBLE
                    marksTextView.text = "Marks: ${homework.marksObtained ?: "N/A"} / ${homework.homeworkMaxMarks}"
                    remarksTextView.text = "Remarks: ${homework.remarks ?: "No remarks"}"
                }
                else -> { // Assigned or Missed
                    submissionActions.visibility = View.VISIBLE
                    attachFileButton.visibility = View.VISIBLE
                    attachFileButton.setOnClickListener { listener.onAttachFile(homework) }
                    deleteButton.visibility = View.GONE
                    gradingLayout.visibility = View.GONE

                    // If a file is pre-selected but not yet submitted
                    if (homework.submissionPath != null) {
                        confirmSubmitButton.visibility = View.VISIBLE
                        confirmSubmitButton.text = "Submit"
                        confirmSubmitButton.setOnClickListener { listener.onConfirmSubmit(homework) }
                        // Corrected: Display only filename, not full path
                        attachedFileName.text = "Attached: ${File(homework.submissionPath).name}"
                        attachedFileName.visibility = View.VISIBLE
                    } else {
                        confirmSubmitButton.visibility = View.GONE
                        attachedFileName.visibility = View.GONE
                    }
                }
            }
        }
    }
}