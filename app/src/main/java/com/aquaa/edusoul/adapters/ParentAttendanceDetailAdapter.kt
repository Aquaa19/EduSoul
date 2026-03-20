// File: main/java/com/aquaa/edusoul/adapters/ParentAttendanceDetailAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.ParentAttendanceDetail
import com.aquaa.edusoul.models.Attendance
import kotlin.collections.MutableList

/*
 * ParentAttendanceDetailAdapter: RecyclerView adapter for displaying parent attendance details.
 * Migrated to Kotlin.
 */
class ParentAttendanceDetailAdapter(
    private val context: Context,
    private var attendanceList: MutableList<ParentAttendanceDetail> // Use MutableList
) : RecyclerView.Adapter<ParentAttendanceDetailAdapter.ViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_parent_attendance_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
        val detail = attendanceList[position]

        holder.textViewSessionDate.text = "Date: ${detail.sessionDate}"
        holder.textViewSessionTime.text = "Time: ${detail.startTime} - ${detail.endTime}"
        holder.textViewSubjectName.text = "Subject: ${detail.subjectName}"
        holder.textViewBatchName.text = "Batch: ${detail.batchName}"
        holder.textViewAttendanceStatus.text = "Status: ${detail.attendanceStatus}"
        holder.textViewRemarks.text = "Remarks: ${detail.remarks?.takeIf { it.isNotBlank() } ?: "N/A"}"

        // Customize status text color based on status
        when (detail.attendanceStatus) {
            Attendance.STATUS_PRESENT -> holder.textViewAttendanceStatus.setTextColor(ContextCompat.getColor(context, R.color.status_present))
            Attendance.STATUS_ABSENT -> holder.textViewAttendanceStatus.setTextColor(ContextCompat.getColor(context, R.color.status_absent))
            Attendance.STATUS_LATE -> holder.textViewAttendanceStatus.setTextColor(ContextCompat.getColor(context, R.color.status_late))
            else -> holder.textViewAttendanceStatus.setTextColor(Color.GRAY) // Default
        }
    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    fun updateData(newData: List<ParentAttendanceDetail>) {
        this.attendanceList.clear()
        this.attendanceList.addAll(newData)
        notifyDataSetChanged()
    }

    class ViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewSessionDate: TextView = itemView.findViewById(R.id.textViewSessionDate)
        val textViewSessionTime: TextView = itemView.findViewById(R.id.textViewSessionTime)
        val textViewSubjectName: TextView = itemView.findViewById(R.id.textViewSubjectName)
        val textViewBatchName: TextView = itemView.findViewById(R.id.textViewBatchName)
        val textViewAttendanceStatus: TextView = itemView.findViewById(R.id.textViewAttendanceStatus)
        val textViewRemarks: TextView = itemView.findViewById(R.id.textViewRemarks)
    }
}