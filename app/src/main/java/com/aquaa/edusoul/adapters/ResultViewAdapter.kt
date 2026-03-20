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
import com.aquaa.edusoul.models.StudentResultDetails
import java.text.NumberFormat
import java.util.ArrayList
import java.util.Locale

/*
 * ResultViewAdapter: RecyclerView adapter for displaying student exam results.
 * It binds `StudentResultDetails` data to the `item_student_result` layout and provides
 * callbacks for edit actions (for the admin).
 */
class ResultViewAdapter(
    private val context: Context,
    private var resultList: MutableList<StudentResultDetails>,
    private val listener: OnResultActionsListener? = null,
    private val isAdminView: Boolean // Flag to show/hide edit button
) : RecyclerView.Adapter<ResultViewAdapter.ResultViewHolder>() {

    interface OnResultActionsListener {
        fun onEditResult(resultDetails: StudentResultDetails, position: Int)
    }

    private val currencyFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale.US) // Use NumberInstance for scores

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ResultViewHolder, position: Int) {
        val result = resultList[position]

        holder.textViewStudentName.text = result.studentName
        holder.textViewAdmissionNumber.text = "Admission No: ${result.admissionNumber}"
        holder.textViewMarks.text = "Marks: ${result.marksObtained}/${result.maxMarks}"

        val remarksText = if (!result.resultRemarks.isNullOrBlank()) "Remarks: ${result.resultRemarks}" else "Remarks: N/A"
        holder.textViewRemarks.text = remarksText

        // FIX: Changed to reference textViewEnteredBy as per item_student_result.xml
        holder.textViewEnteredBy.text = "Entered by: ${result.teacherName}" // Display teacher's name

        // Show/hide edit button based on isAdminView flag
        if (isAdminView && listener != null) {
            holder.imageButtonEditResult.visibility = View.VISIBLE
            holder.imageButtonEditResult.setOnClickListener {
                listener.onEditResult(result, holder.adapterPosition)
            }
        } else {
            holder.imageButtonEditResult.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return resultList.size
    }

    fun setResults(newList: List<StudentResultDetails>) {
        this.resultList.clear()
        this.resultList.addAll(newList)
        notifyDataSetChanged()
    }

    class ResultViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewStudentName: TextView = itemView.findViewById(R.id.textViewStudentNameResult) // Corrected ID
        val textViewAdmissionNumber: TextView = itemView.findViewById(R.id.textViewAdmNoResult) // Corrected ID
        val textViewMarks: TextView = itemView.findViewById(R.id.textViewScore) // Corrected ID
        val textViewRemarks: TextView = itemView.findViewById(R.id.textViewRemarks) // Corrected ID
        // FIX: Changed to reference textViewEnteredBy as per item_student_result.xml
        val textViewEnteredBy: TextView = itemView.findViewById(R.id.textViewEnteredBy)
        val imageButtonEditResult: ImageButton = itemView.findViewById(R.id.imageButtonEditResult)
    }
}
