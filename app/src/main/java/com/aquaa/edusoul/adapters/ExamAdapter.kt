// File: main/java/com/aquaa/edusoul/adapters/ExamAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.Exam

/*
 * ExamAdapter: RecyclerView adapter for displaying a list of exams.
 * Handles binding Exam data to the item_exam layout and provides
 * callbacks for edit and delete actions.
 * Migrated to Kotlin. No longer directly uses DatabaseHelper.
 */
class ExamAdapter(
    private val context: Context,
    private var examList: MutableList<Exam>,
    private val listener: OnExamActionsListener?,
    // Corrected lambda parameter types from Long? to String?
    private val getSubjectNameById: (String?) -> String,
    private val getBatchNameById: (String?) -> String
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

    interface OnExamActionsListener {
        fun onEditExam(exam: Exam, position: Int)
        fun onDeleteExam(exam: Exam, position: Int)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_exam, parent, false)
        return ExamViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ExamViewHolder, position: Int) {
        val exam = examList[position]
        holder.textViewExamName.text = exam.examName

        // exam.batchId and exam.subjectId are already String?
        val batchName = getBatchNameById(exam.batchId)
        val subjectName = getSubjectNameById(exam.subjectId)
        holder.textViewExamDetails.text = "Batch: ${batchName} | Subject: ${subjectName}"

        val dateAndMarks = "Date: ${exam.examDate} | Max Marks: ${exam.maxMarks}"
        holder.textViewExamDateAndMarks.text = dateAndMarks

        holder.imageButtonEditExam.setOnClickListener {
            listener?.onEditExam(exam, holder.adapterPosition)
        }

        holder.imageButtonDeleteExam.setOnClickListener {
            listener?.onDeleteExam(exam, holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return examList.size
    }

    fun setExams(newList: List<Exam>) {
        this.examList.clear()
        this.examList.addAll(newList)
        notifyDataSetChanged()
    }

    class ExamViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewExamIcon: ImageView = itemView.findViewById(R.id.imageViewExamIcon)
        val textViewExamName: TextView = itemView.findViewById(R.id.textViewExamName)
        val textViewExamDetails: TextView = itemView.findViewById(R.id.textViewExamDetails)
        val textViewExamDateAndMarks: TextView = itemView.findViewById(R.id.textViewExamDateAndMarks)
        val imageButtonEditExam: ImageButton = itemView.findViewById(R.id.imageButtonEditExam)
        val imageButtonDeleteExam: ImageButton = itemView.findViewById(R.id.imageButtonDeleteExam)
    }
}