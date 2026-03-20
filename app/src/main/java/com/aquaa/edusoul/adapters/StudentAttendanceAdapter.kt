// File: main/java/com/aquaa/edusoul/adapters/StudentAttendanceAdapter.kt
package com.aquaa.edusoul.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.StudentAttendanceData // ✅ Use the new data class
import com.aquaa.edusoul.models.Attendance // FIX: Import Attendance model
import com.google.android.material.textfield.TextInputEditText

class StudentAttendanceAdapter(
    private var studentDataList: List<StudentAttendanceData>
) : RecyclerView.Adapter<StudentAttendanceAdapter.AttendanceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_for_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(studentDataList[position])
    }

    override fun getItemCount(): Int = studentDataList.size

    fun updateList(newList: List<StudentAttendanceData>) {
        this.studentDataList = newList
        notifyDataSetChanged()
    }

    fun getAttendanceData(): List<StudentAttendanceData> = this.studentDataList

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewStudentName: TextView = itemView.findViewById(R.id.textViewStudentNameAttendance)
        private val radioGroupStatus: RadioGroup = itemView.findViewById(R.id.radioGroupAttendanceStatus)
        private val editTextRemarks: TextInputEditText = itemView.findViewById(R.id.editTextAttendanceRemarks)
        private var textWatcher: TextWatcher? = null

        fun bind(currentData: StudentAttendanceData) {
            textViewStudentName.text = currentData.student.fullName
            editTextRemarks.setText(currentData.remarks)

            radioGroupStatus.setOnCheckedChangeListener(null)
            textWatcher?.let { editTextRemarks.removeTextChangedListener(it) }

            when (currentData.status) {
                Attendance.STATUS_ABSENT -> radioGroupStatus.check(R.id.radioButtonAbsent) // FIX: Use Attendance.STATUS_ABSENT
                Attendance.STATUS_LATE -> radioGroupStatus.check(R.id.radioButtonLate) // FIX: Use Attendance.STATUS_LATE
                else -> radioGroupStatus.check(R.id.radioButtonPresent) // FIX: Use Attendance.STATUS_PRESENT
            }

            radioGroupStatus.setOnCheckedChangeListener { _, checkedId ->
                currentData.status = when (checkedId) {
                    R.id.radioButtonAbsent -> Attendance.STATUS_ABSENT // FIX: Use Attendance.STATUS_ABSENT
                    R.id.radioButtonLate -> Attendance.STATUS_LATE // FIX: Use Attendance.STATUS_LATE
                    else -> Attendance.STATUS_PRESENT // FIX: Use Attendance.STATUS_PRESENT
                }
            }

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    currentData.remarks = s?.toString()
                }
            }
            editTextRemarks.addTextChangedListener(textWatcher)
        }
    }
}