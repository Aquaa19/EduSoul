// File: main/java/com/aquaa/edusoul/adapters/SessionDetailAdapter.kt
package com.aquaa.edusoul.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.SessionAttendanceDetail
import com.aquaa.edusoul.models.Attendance // FIX: Import Attendance model

class SessionDetailAdapter(
    private val detailList: List<SessionAttendanceDetail>
) : RecyclerView.Adapter<SessionDetailAdapter.DetailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_detail, parent, false)
        return DetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(detailList[position])
    }

    override fun getItemCount(): Int = detailList.size

    class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sessionTime: TextView = itemView.findViewById(R.id.textViewSessionTime)
        private val sessionStatus: TextView = itemView.findViewById(R.id.textViewSessionStatus)
        private val subjectName: TextView = itemView.findViewById(R.id.textViewSubjectNameDetail)
        private val remarks: TextView = itemView.findViewById(R.id.textViewRemarksDetail)

        fun bind(detail: SessionAttendanceDetail) {
            sessionTime.text = detail.sessionTime
            sessionStatus.text = detail.status
            subjectName.text = detail.subjectName

            // Set color based on status
            sessionStatus.setTextColor(
                when (detail.status) {
                    Attendance.STATUS_PRESENT -> Color.parseColor("#008000") // Green // FIX: Use Attendance.STATUS_PRESENT
                    Attendance.STATUS_LATE -> Color.parseColor("#FFA500") // Orange // FIX: Use Attendance.STATUS_LATE
                    Attendance.STATUS_ABSENT -> Color.RED // FIX: Use Attendance.STATUS_ABSENT
                    else -> Color.DKGRAY
                }
            )

            // Show remarks only if they exist
            if (!detail.remarks.isNullOrBlank()) {
                remarks.text = "Remarks: ${detail.remarks}"
                remarks.visibility = View.VISIBLE
            } else {
                remarks.visibility = View.GONE
            }
        }
    }
}