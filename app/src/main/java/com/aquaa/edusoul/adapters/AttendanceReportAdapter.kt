package com.aquaa.edusoul.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.StudentDailyAttendance

class AttendanceReportAdapter(
    private var reportList: List<StudentDailyAttendance>
) : RecyclerView.Adapter<AttendanceReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_report_student, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reportList[position])
    }

    override fun getItemCount(): Int = reportList.size

    fun updateList(newList: List<StudentDailyAttendance>) {
        this.reportList = newList
        notifyDataSetChanged()
    }

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val studentName: TextView = itemView.findViewById(R.id.textViewStudentNameReport)
        private val innerRecyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewSessionDetails)

        fun bind(dailyAttendance: StudentDailyAttendance) {
            studentName.text = dailyAttendance.student.fullName

            // Setup the inner RecyclerView for session details
            innerRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            innerRecyclerView.adapter = SessionDetailAdapter(dailyAttendance.sessionDetails)
        }
    }
}