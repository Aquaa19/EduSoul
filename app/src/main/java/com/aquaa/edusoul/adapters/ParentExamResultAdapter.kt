// File: main/java/com/aquaa/edusoul/adapters/ParentExamResultAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.ParentExamResultDetails
import java.util.Locale

/*
 * ParentExamResultAdapter: RecyclerView adapter for displaying parent exam results.
 * Migrated to Kotlin.
 */
class ParentExamResultAdapter(
    private val context: Context,
    private var examResultDetailsList: MutableList<ParentExamResultDetails>,
    private val listener: OnItemClickListener?
) : RecyclerView.Adapter<ParentExamResultAdapter.ParentExamResultViewHolder>() {

    interface OnItemClickListener {
        fun onExamResultCardClick(examResultDetails: ParentExamResultDetails)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ParentExamResultViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_parent_exam_result, parent, false)
        return ParentExamResultViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ParentExamResultViewHolder, position: Int) {
        val currentItem = examResultDetailsList[position]

        holder.textViewParentExamName.text = currentItem.examName
        holder.textViewParentExamSubject.text = "Subject: ${currentItem.subjectName}"
        holder.textViewParentExamDate.text = "Date: ${currentItem.examDate}"

        if (currentItem.marksObtained != null) {
            val marksText = String.format(Locale.US, "%.0f / %.0f",
                currentItem.marksObtained, currentItem.examMaxMarks)
            holder.textViewParentExamMarks.text = marksText
        } else {
            holder.textViewParentExamMarks.text = "N/A"
        }

        val remarks = currentItem.resultRemarks
        if (!remarks.isNullOrEmpty()) {
            holder.textViewParentExamRemarks.text = "Remarks: $remarks"
            holder.textViewParentExamRemarks.visibility = View.VISIBLE
        } else {
            holder.textViewParentExamRemarks.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            listener?.onExamResultCardClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return examResultDetailsList.size
    }

    fun updateData(newExamResultDetailsList: List<ParentExamResultDetails>) {
        this.examResultDetailsList.clear()
        this.examResultDetailsList.addAll(newExamResultDetailsList)
        notifyDataSetChanged()
    }

    class ParentExamResultViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewParentExamName: TextView = itemView.findViewById(R.id.textViewParentExamName)
        val textViewParentExamSubject: TextView = itemView.findViewById(R.id.textViewParentExamSubject)
        val textViewParentExamDate: TextView = itemView.findViewById(R.id.textViewParentExamDate)
        val textViewParentExamMarks: TextView = itemView.findViewById(R.id.textViewParentExamMarks)
        val textViewParentExamRemarks: TextView = itemView.findViewById(R.id.textViewParentExamRemarks)
    }
}