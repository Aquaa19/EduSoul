package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.Student // Assuming we'll display Student objects
// REMOVED: import java.util.List // Not actually needed, Kotlin's List is sufficient
import java.util.ArrayList // ADDED: Explicitly import java.util.ArrayList

/*
 * StudentEnrollmentAdapter: RecyclerView adapter for managing student enrollments.
 * Used to display lists of students who are either enrolled or available for enrollment
 * in a specific batch. Provides callbacks for enroll/unenroll actions.
 * Migrated to Kotlin.
 */
class StudentEnrollmentAdapter(
    private val context: Context,
    // UPDATED: Change parameter type to explicitly expect java.util.ArrayList
    private var studentList: java.util.ArrayList<Student>,
    private val isEnrolledList: Boolean, // Differentiates between "Enroll" and "Unenroll" button
    private val listener: OnEnrollmentActionListener? // Made nullable
) : RecyclerView.Adapter<StudentEnrollmentAdapter.EnrollmentViewHolder>() {

    // Interface for handling enrollment actions
    interface OnEnrollmentActionListener {
        fun onEnrollStudent(student: Student, position: Int)
        fun onUnenrollStudent(student: Student, position: Int)
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): EnrollmentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student_enrollment_status, parent, false)
        return EnrollmentViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: EnrollmentViewHolder, position: Int) {
        val student = studentList[position]
        holder.textViewStudentName.text = student.fullName

        if (isEnrolledList) {
            holder.buttonAction.text = "Unenroll"
            holder.buttonAction.setOnClickListener {
                listener?.onUnenrollStudent(student, holder.adapterPosition)
            }
        } else {
            holder.buttonAction.text = "Enroll"
            holder.buttonAction.setOnClickListener {
                listener?.onEnrollStudent(student, holder.adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }

    // The `newStudentList` parameter still expects Kotlin's `List`, which is fine.
    // The internal `addAll` call will work with `java.util.ArrayList`.
    fun setStudents(newStudentList: List<Student>) {
        this.studentList.clear()
        this.studentList.addAll(newStudentList) // This now calls java.util.ArrayList.addAll
        notifyDataSetChanged()
    }

    // Method to remove an item from the list (used after enroll/unenroll action)
    fun removeItem(position: Int) {
        if (studentList.size > position && position >= 0) { // Check bounds first
            studentList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, studentList.size)
        }
    }

    // Method to add an item to the list (used after enroll/unenroll action)
    fun addItem(student: Student) {
        studentList.add(student)
        notifyItemInserted(studentList.size - 1)
    }


    class EnrollmentViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewStudentIcon: ImageView = itemView.findViewById(R.id.imageViewStudentEnrollmentIcon)
        val textViewStudentName: TextView = itemView.findViewById(R.id.textViewStudentNameEnrollment)
        val buttonAction: Button = itemView.findViewById(R.id.buttonEnrollmentAction)
    }
}