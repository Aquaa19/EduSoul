// main/java/com/aquaa/edusoul/adapters/HomeworkAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.Homework

class HomeworkAdapter(
    private val context: Context,
    private var homeworkList: MutableList<Homework>,
    private val listener: OnHomeworkActionsListener,
    // Changed lambda parameter type from Long to String, and return type to String
    private val getSubjectNameById: (String) -> String
) : RecyclerView.Adapter<HomeworkAdapter.HomeworkViewHolder>() {

    interface OnHomeworkActionsListener {
        fun onEdit(homework: Homework)
        fun onDelete(homework: Homework)
        fun onAssign(homework: Homework)
        fun onGrade(homework: Homework)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeworkViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_homework, parent, false)
        return HomeworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeworkViewHolder, position: Int) {
        val homework = homeworkList[position]
        holder.bind(homework, listener)
    }

    override fun getItemCount(): Int = homeworkList.size

    fun setHomework(newHomeworkList: List<Homework>) {
        this.homeworkList.clear()
        this.homeworkList.addAll(newHomeworkList)
        notifyDataSetChanged()
    }

    inner class HomeworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewHomeworkTitle)
        private val subject: TextView = itemView.findViewById(R.id.textViewHomeworkSubject)
        private val dueDate: TextView = itemView.findViewById(R.id.textViewHomeworkDueDate)
        private val editButton: Button = itemView.findViewById(R.id.buttonEdit)
        private val deleteButton: Button = itemView.findViewById(R.id.buttonDelete)
        private val assignButton: Button = itemView.findViewById(R.id.buttonAssign)
        private val gradeButton: Button = itemView.findViewById(R.id.buttonGrade)

        fun bind(homework: Homework, listener: OnHomeworkActionsListener) {
            title.text = homework.title
            dueDate.text = "Due: ${homework.dueDate}"
            // Call getSubjectNameById with the String subjectId
            subject.text = "Subject: ${getSubjectNameById(homework.subjectId)}"

            editButton.setOnClickListener { listener.onEdit(homework) }
            deleteButton.setOnClickListener { listener.onDelete(homework) }
            assignButton.setOnClickListener { listener.onAssign(homework) }
            gradeButton.setOnClickListener { listener.onGrade(homework) }
        }
    }
}