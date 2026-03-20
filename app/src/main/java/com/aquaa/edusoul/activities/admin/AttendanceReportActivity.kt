package com.aquaa.edusoul.activities.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.AttendanceReportAdapter
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository // Added import
import com.aquaa.edusoul.repositories.SubjectRepository // Added import
import com.aquaa.edusoul.viewmodels.AttendanceReportViewModel
import com.aquaa.edusoul.viewmodels.AttendanceReportViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AttendanceReportActivity : BaseActivity() {

    private val TAG = "AttendanceReportAct"

    private lateinit var toolbar: Toolbar
    private lateinit var editTextReportDate: TextInputEditText
    private lateinit var buttonPickDateReport: ImageButton
    private lateinit var spinnerSelectBatchReport: Spinner
    private lateinit var buttonGenerateReport: Button
    private lateinit var textViewReportTitle: TextView
    private lateinit var recyclerViewAttendanceReport: RecyclerView
    private lateinit var textViewNoAttendanceData: TextView

    private lateinit var viewModel: AttendanceReportViewModel
    private lateinit var attendanceReportAdapter: AttendanceReportAdapter

    private lateinit var dbDateFormatter: SimpleDateFormat
    private lateinit var selectedCalendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_report)

        initViews()
        initViewModel()

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Attendance Report"
        }

        dbDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        selectedCalendar = Calendar.getInstance()

        // Adapter setup
        attendanceReportAdapter = AttendanceReportAdapter(emptyList())
        recyclerViewAttendanceReport.layoutManager = LinearLayoutManager(this)
        recyclerViewAttendanceReport.adapter = attendanceReportAdapter

        setupDatePicker()
        setupEventListeners()
        setupObservers()
        updateDateInView()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbarAttendanceReport)
        editTextReportDate = findViewById(R.id.editTextReportDate)
        buttonPickDateReport = findViewById(R.id.buttonPickDateReport)
        spinnerSelectBatchReport = findViewById(R.id.spinnerSelectBatchReport)
        buttonGenerateReport = findViewById(R.id.buttonGenerateReport)
        textViewReportTitle = findViewById(R.id.textViewReportTitle)
        recyclerViewAttendanceReport = findViewById(R.id.recyclerViewAttendanceReport)
        textViewNoAttendanceData = findViewById(R.id.textViewNoAttendanceData)
    }

    private fun initViewModel() {
        // Instantiate the required repositories
        val attendanceRepository = AttendanceRepository()
        val studentRepository = StudentRepository()
        val batchRepository = BatchRepository()
        val classSessionRepository = ClassSessionRepository() // Instantiated
        val subjectRepository = SubjectRepository() // Instantiated

        val factory = AttendanceReportViewModelFactory(
            attendanceRepository,
            studentRepository,
            batchRepository,
            classSessionRepository, // Passed to factory
            subjectRepository // Passed to factory
        )
        viewModel = ViewModelProvider(this, factory)[AttendanceReportViewModel::class.java]
    }

    private fun setupDatePicker() {
        val dateClickListener = View.OnClickListener {
            DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, monthOfYear)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        editTextReportDate.setOnClickListener(dateClickListener)
        buttonPickDateReport.setOnClickListener(dateClickListener)
    }

    private fun setupEventListeners() {
        buttonGenerateReport.setOnClickListener {
            val selectedDateStr = editTextReportDate.text.toString()
            val selectedBatch = spinnerSelectBatchReport.selectedItem as? Batch

            // Corrected: Check if batch ID is blank instead of -1L
            if (selectedBatch == null || selectedBatch.id.isBlank()) {
                Toast.makeText(this, "Please select a valid batch.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.i(TAG, "Generating report for Date: $selectedDateStr, Batch ID: ${selectedBatch.id}")
            // Corrected: Pass String ID
            viewModel.generateReport(selectedBatch.id, selectedDateStr)
        }
    }

    private fun setupObservers() {
        viewModel.processedReport.observe(this) { reportList ->
            Log.d(TAG, "Report LiveData updated. Size: ${reportList.size}")
            attendanceReportAdapter.updateList(reportList)
            textViewNoAttendanceData.visibility = if (reportList.isEmpty()) View.VISIBLE else View.GONE
            recyclerViewAttendanceReport.visibility = if (reportList.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.reportSummary.observe(this) { summary ->
            textViewReportTitle.visibility = View.VISIBLE
            textViewReportTitle.text = summary
        }

        // Observe allBatches from ViewModel to populate the spinner
        viewModel.allBatches.observe(this) { batches ->
            val displayList = if (batches.isNotEmpty()) {
                batches
            } else {
                // Corrected: Use empty string for Batch ID as it's now String type
                listOf(Batch("", "No Batches Available", null, null, null, null))
            }
            // Ensure the spinner is populated when batches are loaded
            spinnerSelectBatchReport.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayList)
        }
    }

    private fun updateDateInView() {
        editTextReportDate.setText(dbDateFormatter.format(selectedCalendar.time))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}