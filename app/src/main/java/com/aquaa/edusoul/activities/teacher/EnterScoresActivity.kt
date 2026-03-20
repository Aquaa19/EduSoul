// File: main/java/com/aquaa/edusoul/activities/teacher/EnterScoresActivity.kt
package com.aquaa.edusoul.activities.teacher

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.ScoreEntryAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.StudentScoreStatus
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.EnterScoresViewModel
import com.aquaa.edusoul.viewmodels.EnterScoresViewModelFactory
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.activities.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnterScoresActivity : BaseActivity() {

    private val TAG = "EnterScoresActivity"

    private lateinit var spinnerSelectExam: Spinner
    private lateinit var recyclerViewEnterScores: RecyclerView
    private lateinit var buttonSubmitScores: Button
    private lateinit var textViewNoStudents: TextView
    private lateinit var textViewMaxMarks: TextView

    private lateinit var authManager: AuthManager
    private lateinit var scoreEntryAdapter: ScoreEntryAdapter
    private lateinit var studentScoreList: MutableList<StudentScoreStatus>
    private lateinit var examList: List<Exam>
    private var currentTeacher: User? = null
    private var selectedExam: Exam? = null

    private lateinit var enterScoresViewModel: EnterScoresViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_scores)

        val toolbar = findViewById<Toolbar>(R.id.toolbarEnterScores)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            currentTeacher = authManager.getLoggedInUser()

            if (currentTeacher == null) {
                Toast.makeText(this@EnterScoresActivity, "Error: Could not identify teacher.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            val examRepository = ExamRepository()
            val studentRepository = StudentRepository()
            val resultRepository = ResultRepository()
            val userRepository = UserRepository()

            val factory = EnterScoresViewModelFactory(examRepository, studentRepository, resultRepository, userRepository)
            enterScoresViewModel = ViewModelProvider(this@EnterScoresActivity, factory)[EnterScoresViewModel::class.java]

            currentTeacher?.id?.let { teacherId ->
                enterScoresViewModel.setCurrentTeacherId(teacherId)
            }

            spinnerSelectExam = findViewById(R.id.spinnerSelectExamForScores)
            recyclerViewEnterScores = findViewById(R.id.recyclerViewEnterScores)
            buttonSubmitScores = findViewById(R.id.buttonSubmitScores)
            textViewNoStudents = findViewById(R.id.textViewNoStudentsForScores)
            textViewMaxMarks = findViewById(R.id.textViewExamMaxMarks)

            studentScoreList = mutableListOf()
            examList = mutableListOf()

            scoreEntryAdapter = ScoreEntryAdapter(this@EnterScoresActivity, studentScoreList, 0)
            recyclerViewEnterScores.layoutManager = LinearLayoutManager(this@EnterScoresActivity)
            recyclerViewEnterScores.adapter = scoreEntryAdapter

            setupObservers()

            spinnerSelectExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val item = parent.getItemAtPosition(position)
                    if (item is Exam) {
                        selectedExam = item
                        if (selectedExam != null && selectedExam!!.id.isNotBlank()) {
                            textViewMaxMarks.text = "Maximum Marks: ${selectedExam!!.maxMarks}"
                            textViewMaxMarks.visibility = View.VISIBLE
                            scoreEntryAdapter.setMaxMarks(selectedExam!!.maxMarks)
                            enterScoresViewModel.selectExam(selectedExam!!.id)
                        } else {
                            clearStudentList()
                            textViewMaxMarks.visibility = View.GONE
                            enterScoresViewModel.selectExam(null)
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    clearStudentList()
                    enterScoresViewModel.selectExam(null)
                }
            }

            buttonSubmitScores.setOnClickListener { submitScores() }
        }
    }

    private fun setupObservers() {
        enterScoresViewModel.examsForTeacher.observe(this) { exams ->
            examList = if (exams.isNotEmpty()) {
                exams
            } else {
                listOf(Exam(id = "", examName = "No relevant exams found", subjectId = null, batchId = null, examDate = "", maxMarks = 0, description = null, teacherId = null))
            }
            val examAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, examList)
            examAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSelectExam.adapter = examAdapterSpinner
        }

        enterScoresViewModel.studentScores.observe(this) { studentScores ->
            // Fix: Use scoreEntryAdapter instead of studentAttendanceAdapter
            scoreEntryAdapter.setStudentScoreList(studentScores)
            if (scoreEntryAdapter.itemCount == 0) {
                textViewNoStudents.text = "No students found for this exam's batch."
                textViewNoStudents.visibility = View.VISIBLE
                recyclerViewEnterScores.visibility = View.GONE
                buttonSubmitScores.visibility = View.GONE
            } else {
                textViewNoStudents.visibility = View.GONE
                recyclerViewEnterScores.visibility = View.VISIBLE
                buttonSubmitScores.visibility = View.VISIBLE
                buttonSubmitScores.isEnabled = true
                buttonSubmitScores.text = "Submit Scores"
            }
        }

        enterScoresViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (it.contains("successfully")) {
                    buttonSubmitScores.isEnabled = false
                    buttonSubmitScores.text = "Scores Submitted"
                }
                enterScoresViewModel.clearErrorMessage()
            }
        }
    }

    private fun clearStudentList() {
        studentScoreList.clear()
        scoreEntryAdapter.notifyDataSetChanged()
        textViewNoStudents.text = "Select an exam to list students."
        textViewNoStudents.visibility = View.VISIBLE
        recyclerViewEnterScores.visibility = View.GONE
        buttonSubmitScores.visibility = View.GONE
    }

    private fun submitScores() {
        val currentExam = selectedExam
        if (currentExam == null || currentExam.id.isBlank()) {
            Toast.makeText(this, "Please select a valid exam.", Toast.LENGTH_SHORT).show()
            return
        }

        val scoreData = scoreEntryAdapter.getScoresData()
        if (scoreData.isEmpty()) {
            Toast.makeText(this, "No scores entered to submit.", Toast.LENGTH_SHORT).show()
            return
        }

        val teacherId = currentTeacher?.id ?: ""
        enterScoresViewModel.saveScores(currentExam.id, scoreData, teacherId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}