package com.aquaa.edusoul.activities.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.ResultViewAdapter
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.StudentResultDetails
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ResultViewModel
import com.aquaa.edusoul.viewmodels.ResultViewModelFactory
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.util.ArrayList
import java.util.Locale

class ViewResultsActivity : BaseActivity(), ResultViewAdapter.OnResultActionsListener {

    private val TAG = "ViewResultsActivity"

    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var textViewNoResults: TextView
    private lateinit var spinnerSelectExam: Spinner
    private lateinit var resultAdapter: ResultViewAdapter

    private var selectedExamId: String? = null

    private lateinit var resultViewModel: ResultViewModel
    private var examListLiveData: List<Exam> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)

        val toolbar = findViewById<Toolbar>(R.id.toolbarViewResults)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val examRepository = ExamRepository()
        val resultRepository = ResultRepository()
        val studentRepository = StudentRepository()
        val subjectRepository = SubjectRepository()
        val userRepository = UserRepository()

        // Fix: Pass all required repositories to the factory
        val factory = ResultViewModelFactory(
            resultRepository,
            examRepository,
            studentRepository,
            subjectRepository,
            userRepository
        )
        resultViewModel = ViewModelProvider(this, factory)[ResultViewModel::class.java]

        recyclerViewResults = findViewById(R.id.recyclerViewResults)
        textViewNoResults = findViewById(R.id.textViewNoResults)
        spinnerSelectExam = findViewById(R.id.spinnerFilterExam)

        resultAdapter = ResultViewAdapter(this, mutableListOf(), this, true)
        recyclerViewResults.layoutManager = LinearLayoutManager(this)
        recyclerViewResults.adapter = resultAdapter

        setupEventListeners()
        setupObservers()

        selectedExamId = intent.getStringExtra("EXAM_ID")
    }

    private fun setupEventListeners() {
        spinnerSelectExam.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedExam = parent?.getItemAtPosition(position) as? Exam
                selectedExam?.let {
                    selectedExamId = it.id
                    resultViewModel.selectExam(it.id)
                } ?: run {
                    selectedExamId = null
                    resultViewModel.clearSelectedExam()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedExamId = null
                resultViewModel.clearSelectedExam()
            }
        }
    }

    private fun setupObservers() {
        resultViewModel.allExams.observe(this) { exams ->
            examListLiveData = exams
            val examNames = exams.map { it.examName }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, examNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSelectExam.adapter = adapter

            selectedExamId?.let { initialExamId ->
                val examToSelect = exams.firstOrNull { it.id == initialExamId }
                examToSelect?.let {
                    val position = exams.indexOf(it)
                    if (position != -1) {
                        spinnerSelectExam.setSelection(position)
                    }
                }
            }

            if (selectedExamId.isNullOrBlank() && exams.isNotEmpty()) {
                spinnerSelectExam.setSelection(0)
            }
        }

        resultViewModel.detailedResults.observe(this) { results ->
            resultAdapter.setResults(results)
            textViewNoResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }

        resultViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                resultViewModel.clearErrorMessage()
            }
        }
    }

    override fun onEditResult(resultDetails: StudentResultDetails, position: Int) {
        showEditResultDialog(resultDetails)
    }

    private fun showEditResultDialog(resultDetails: StudentResultDetails) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_edit_result_admin, null)
        builder.setView(dialogView)

        // Fix: Correctly reference TextView for student name/info
        val textViewEditResultInfo = dialogView.findViewById<TextView>(R.id.textViewEditResultInfo)
        // Fix: Correctly reference TextInputLayout and EditText for marks
        val tilEditScore = dialogView.findViewById<TextInputLayout>(R.id.tilEditScore) // Corrected ID
        val editTextEditScore = dialogView.findViewById<EditText>(R.id.editTextEditScore) // Corrected ID

        val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

        // Fix: Combine student name and exam info into the existing TextView
        val examName = examListLiveData.firstOrNull { it.id == resultDetails.examId }?.examName ?: "N/A"
        textViewEditResultInfo.text = "Editing score for ${resultDetails.studentName}\nExam: $examName (Max: ${resultDetails.maxMarks})"

        editTextEditScore.setText(resultDetails.marksObtained?.let { numberFormatter.format(it) } ?: "")
        // Fix: Set helper text for max marks using the correct TextInputLayout ID
        tilEditScore.helperText = "Max Marks: ${resultDetails.maxMarks}"


        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newMarksStr = editTextEditScore.text.toString().trim()
            // Fix: Retain original remarks from resultDetails, as this dialog does not support editing them
            val newRemarks = resultDetails.resultRemarks

            if (newMarksStr.isEmpty()) {
                tilEditScore.error = "Marks cannot be empty." // Corrected ID
                return@setOnClickListener
            }

            val newMarks: Double
            try {
                newMarks = newMarksStr.toDouble()
                if (newMarks < 0 || newMarks > resultDetails.maxMarks) {
                    tilEditScore.error = "Marks must be between 0 and ${resultDetails.maxMarks}." // Corrected ID
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                tilEditScore.error = "Invalid marks format." // Corrected ID
                return@setOnClickListener
            }

            resultViewModel.updateResult(resultDetails.resultId, newMarks, newRemarks)
            alertDialog.dismiss()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}