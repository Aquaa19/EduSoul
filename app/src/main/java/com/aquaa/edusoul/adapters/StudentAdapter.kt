// File: main/java/com/aquaa/edusoul/adapters/StudentAdapter.kt
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
import com.aquaa.edusoul.models.Student // Ensure you have the Student model
import java.util.ArrayList
// REMOVED: import java.util.List // Not actually needed, Kotlin's List is sufficient

/*
 * StudentAdapter: Manages the list of students for the Admin.
 * Migrated to Kotlin.
 */
class StudentAdapter(
    private val context: Context,
    private var studentList: MutableList<Student>, // Changed to mutable list
    private val listener: OnStudentActionsListener? // Made nullable
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    interface OnStudentActionsListener {
        fun onEditStudent(student: Student, position: Int)
        fun onDeleteStudent(student: Student, position: Int)
        // fun onViewStudentDetails(student: Student, position: Int) // Optional: for a details view
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: StudentViewHolder, position: Int) {
        val student = studentList[position]
        holder.textViewStudentName.text = student.fullName
        holder.textViewStudentGrade.text = "Grade: ${student.gradeOrClass ?: "N/A"}"
        holder.textViewAdmissionNumber.text = "Adm No: ${student.admissionNumber ?: "N/A"}"
        // You can load profile image using Glide/Picasso if profileImagePath is available

        holder.imageButtonEditStudent.setOnClickListener {
            listener?.onEditStudent(student, holder.adapterPosition)
        }

        holder.imageButtonDeleteStudent.setOnClickListener {
            listener?.onDeleteStudent(student, holder.adapterPosition)
        }

        // Optional: Click listener for the whole item to view details
        // holder.itemView.setOnClickListener {
        //     listener?.onViewStudentDetails(student, holder.adapterPosition)
        // }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }

    fun setStudents(newStudentList: List<Student>) {
        this.studentList.clear()
        this.studentList.addAll(newStudentList) // Explicit cast for addAll
        notifyDataSetChanged()
    }

    class StudentViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewStudentIcon: ImageView = itemView.findViewById(R.id.imageViewStudentIcon)
        val textViewStudentName: TextView = itemView.findViewById(R.id.textViewStudentName)
        val textViewStudentGrade: TextView = itemView.findViewById(R.id.textViewStudentGrade)
        val textViewAdmissionNumber: TextView = itemView.findViewById(R.id.textViewAdmissionNumber)
        val imageButtonEditStudent: ImageButton = itemView.findViewById(R.id.imageButtonEditStudent)
        val imageButtonDeleteStudent: ImageButton = itemView.findViewById(R.id.imageButtonDeleteStudent)
    }
}