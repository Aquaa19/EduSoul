// File: main/java/com/aquaa/edusoul/adapters/ScoreEntryAdapter.kt
package com.aquaa.edusoul.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.models.StudentScoreStatus
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class ScoreEntryAdapter(
    private val context: Context,
    private var studentScoreList: MutableList<StudentScoreStatus>, // Use Kotlin's MutableList
    private var maxMarks: Int
) : RecyclerView.Adapter<ScoreEntryAdapter.ScoreViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_student_for_score_entry, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ScoreViewHolder, position: Int) {
        val currentStatus = studentScoreList[position]
        holder.textViewStudentName.text = currentStatus.student.fullName
        holder.textInputLayoutScore.helperText = "/ $maxMarks"

        holder.scoreWatcher?.let { holder.editTextScore.removeTextChangedListener(it) }

        val currentScoreText = holder.editTextScore.text.toString()
        val newScoreText = currentStatus.score
        if (currentScoreText != newScoreText) {
            holder.editTextScore.setText(newScoreText)
        }

        holder.scoreWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentStatus.score = s.toString()

                try {
                    if (s.toString().isNotBlank()) {
                        val score = s.toString().toDouble()
                        if (score > maxMarks) {
                            holder.textInputLayoutScore.error = "Max: $maxMarks"
                        } else {
                            holder.textInputLayoutScore.error = null
                        }
                    } else {
                        holder.textInputLayoutScore.error = null
                    }
                } catch (e: NumberFormatException) {
                    holder.textInputLayoutScore.error = "Invalid"
                }
            }
        }
        holder.editTextScore.addTextChangedListener(holder.scoreWatcher)
    }

    override fun getItemCount(): Int {
        return studentScoreList.size
    }

    fun getScoresData(): List<StudentScoreStatus> {
        return this.studentScoreList
    }

    fun setStudentScoreList(newList: List<StudentScoreStatus>) {
        this.studentScoreList.clear()
        this.studentScoreList.addAll(newList)
        notifyDataSetChanged()
    }

    fun setMaxMarks(newMaxMarks: Int) {
        this.maxMarks = newMaxMarks
        notifyDataSetChanged()
    }

    class ScoreViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewStudentName: TextView = itemView.findViewById(R.id.textViewStudentNameForScore)
        val textInputLayoutScore: TextInputLayout = itemView.findViewById(R.id.textInputLayoutScore)
        val editTextScore: TextInputEditText = itemView.findViewById(R.id.editTextScore)
        var scoreWatcher: TextWatcher? = null
    }
}