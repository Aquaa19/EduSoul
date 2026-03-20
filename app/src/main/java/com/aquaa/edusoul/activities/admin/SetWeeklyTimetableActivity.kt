// File: main/java/com/aquaa/edusoul/activities/admin/SetWeeklyTimetableActivity.kt
package com.aquaa.edusoul.activities.admin

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.RecurringSessionAdapter
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.RecurringClassSession
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.*
import com.aquaa.edusoul.viewmodels.SetWeeklyTimetableViewModel
import com.aquaa.edusoul.viewmodels.SetWeeklyTimetableViewModelFactory
import com.google.android.material.textfield.TextInputLayout
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.coroutines.runBlocking // For synchronous calls in adapter lambdas

class SetWeeklyTimetableActivity : BaseActivity(), RecurringSessionAdapter.OnRecurringSessionActionListener {

    private val TAG = "SetWeeklyTimetable"
    private lateinit var viewModel: SetWeeklyTimetableViewModel

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var spinnerSelectBatch: Spinner
    private lateinit var progressBar: ProgressBar

    // Day-specific UI components
    private val recyclerViews by lazy {
        mapOf(
            Calendar.MONDAY to findViewById<RecyclerView>(R.id.recyclerViewMonday),
            Calendar.TUESDAY to findViewById<RecyclerView>(R.id.recyclerViewTuesday),
            Calendar.WEDNESDAY to findViewById<RecyclerView>(R.id.recyclerViewWednesday),
            Calendar.THURSDAY to findViewById<RecyclerView>(R.id.recyclerViewThursday),
            Calendar.FRIDAY to findViewById<RecyclerView>(R.id.recyclerViewFriday),
            Calendar.SATURDAY to findViewById<RecyclerView>(R.id.recyclerViewSaturday),
            Calendar.SUNDAY to findViewById<RecyclerView>(R.id.recyclerViewSunday)
        )
    }
    private val noSessionsTextViews by lazy {
        mapOf(
            Calendar.MONDAY to findViewById<TextView>(R.id.textViewNoSessionsMonday),
            Calendar.TUESDAY to findViewById<TextView>(R.id.textViewNoSessionsTuesday),
            Calendar.WEDNESDAY to findViewById<TextView>(R.id.textViewNoSessionsWednesday),
            Calendar.THURSDAY to findViewById<TextView>(R.id.textViewNoSessionsThursday),
            Calendar.FRIDAY to findViewById<TextView>(R.id.textViewNoSessionsFriday),
            Calendar.SATURDAY to findViewById<TextView>(R.id.textViewNoSessionsSaturday),
            Calendar.SUNDAY to findViewById<TextView>(R.id.textViewNoSessionsSunday)
        )
    }
    private val addButtons by lazy {
        mapOf(
            Calendar.MONDAY to findViewById<ImageButton>(R.id.buttonAddMonday),
            Calendar.TUESDAY to findViewById<ImageButton>(R.id.buttonAddTuesday),
            Calendar.WEDNESDAY to findViewById<ImageButton>(R.id.buttonAddWednesday),
            Calendar.THURSDAY to findViewById<ImageButton>(R.id.buttonAddThursday),
            Calendar.FRIDAY to findViewById<ImageButton>(R.id.buttonAddFriday),
            Calendar.SATURDAY to findViewById<ImageButton>(R.id.buttonAddSaturday),
            Calendar.SUNDAY to findViewById<ImageButton>(R.id.buttonAddSunday)
        )
    }
    private val adapters = mutableMapOf<Int, RecurringSessionAdapter>()

    // Member variables to hold the fully loaded lists for spinners
    private val subjectList = mutableListOf<Subject>()
    private val teacherList = mutableListOf<User>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_weekly_timetable)

        toolbar = findViewById(R.id.toolbarSetWeeklyTimetable)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        spinnerSelectBatch = findViewById(R.id.spinnerSelectBatchForTimetable)
        progressBar = findViewById(R.id.progressBar)

        setupViewModel()
        setupRecyclerViews()
        setupEventListeners()
        setupObservers()
    }

    private fun setupViewModel() {
        val factory = SetWeeklyTimetableViewModelFactory(
            RecurringClassSessionRepository(),
            BatchRepository(),
            SubjectRepository(),
            UserRepository()
        )
        viewModel = ViewModelProvider(this, factory)[SetWeeklyTimetableViewModel::class.java]
    }

    private fun setupRecyclerViews() {
        for ((day, recyclerView) in recyclerViews) {
            val adapter = RecurringSessionAdapter(
                mutableListOf(),
                this,
                getSubjectName = { subjectId ->
                    subjectId?.let { sId ->
                        runBlocking { SubjectRepository().getSubjectById(sId)?.subjectName }
                    } ?: "Unknown"
                },
                getTeacherName = { teacherId ->
                    teacherId?.let { tId ->
                        runBlocking { UserRepository().getUserById(tId)?.fullName }
                    } ?: "Unknown"
                }
            )
            adapters[day] = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
        }
    }

    private fun setupEventListeners() {
        spinnerSelectBatch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBatch = parent?.getItemAtPosition(position) as? Batch
                selectedBatch?.let {
                    viewModel.selectBatch(it.id) // it.id is String
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        for ((day, button) in addButtons) {
            button.setOnClickListener {
                val selectedBatch = spinnerSelectBatch.selectedItem as? Batch
                if (selectedBatch != null && !selectedBatch.id.isBlank()) {
                    showAddSessionDialog(day, selectedBatch.id) // selectedBatch.id is String
                } else {
                    Toast.makeText(this, "Please select a batch first.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.allBatches.observe(this) { batches ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(batches ?: emptyList()))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSelectBatch.adapter = adapter
        }

        viewModel.allSubjects.observe(this) { subjects ->
            subjectList.clear()
            if (subjects != null) {
                subjectList.addAll(subjects)
            }
        }

        viewModel.allTeachers.observe(this) { teachers ->
            teacherList.clear()
            if (teachers != null) {
                teacherList.addAll(teachers)
            }
        }

        viewModel.recurringSessionsForBatch.observe(this) { sessions ->
            val groupedByDay = sessions.groupBy { it.dayOfWeek }
            for ((day, recyclerView) in recyclerViews) {
                val daySessions = groupedByDay[day] ?: emptyList()
                adapters[day]?.updateData(daySessions)
                noSessionsTextViews[day]?.visibility = if (daySessions.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        viewModel.isLoading.observe(this){ isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun showAddSessionDialog(dayOfWeek: Int, batchId: String) { // batchId is String
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_class_session, null)
        val spinnerSubject = dialogView.findViewById<Spinner>(R.id.spinnerSubjectSessionDialog)
        val spinnerTeacher = dialogView.findViewById<Spinner>(R.id.spinnerTeacherSessionDialog)
        val editTextStartTime = dialogView.findViewById<EditText>(R.id.editTextStartTimeSessionDialog)
        val editTextEndTime = dialogView.findViewById<EditText>(R.id.editTextEndTimeSessionDialog)

        // Hide unused fields in this specific dialog
        dialogView.findViewById<Spinner>(R.id.spinnerBatchSessionDialog).visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.textViewBatchSessionLabel).visibility = View.GONE
        dialogView.findViewById<EditText>(R.id.editTextTopicSessionDialog).visibility = View.GONE
        dialogView.findViewById<TextInputLayout>(R.id.tilTopicSessionDialog).visibility = View.GONE

        val teacherNames = teacherList.map { it.fullName }
        spinnerSubject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(subjectList))
        spinnerTeacher.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(teacherNames))

        setupTimePicker(editTextStartTime)
        setupTimePicker(editTextEndTime)

        AlertDialog.Builder(this)
            .setTitle("Add Recurring Session for " + getDayName(dayOfWeek))
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val subject = spinnerSubject.selectedItem as? Subject

                val selectedTeacherName = spinnerTeacher.selectedItem as? String
                val teacher = teacherList.find { it.fullName == selectedTeacherName }

                val startTime = editTextStartTime.text.toString()
                val endTime = editTextEndTime.text.toString()

                if (subject != null && teacher != null && startTime.isNotBlank() && endTime.isNotBlank()) {
                    val session = RecurringClassSession(
                        id = "", // Default ID to empty string for String type
                        batchId = batchId, // batchId is String
                        subjectId = subject.id, // subject.id is String
                        teacherUserId = teacher.id, // teacher.id is String
                        dayOfWeek = dayOfWeek,
                        startTime = startTime,
                        endTime = endTime
                    )
                    viewModel.addOrUpdateRecurringSession(session)
                } else {
                    Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onEditSession(session: RecurringClassSession) {
        Log.d(TAG, "Edit session clicked for: ${session.id}")
        Toast.makeText(this, "Edit functionality not fully implemented for recurring sessions yet.", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteSession(session: RecurringClassSession) {
        AlertDialog.Builder(this)
            .setTitle("Delete Recurring Session")
            .setMessage("Are you sure you want to delete this recurring session?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRecurringSession(session.id) // session.id is String
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupTimePicker(editText: EditText) {
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute -> editText.setText(String.format(Locale.US, "%02d:%02d", hour, minute)) },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun getDayName(dayConstant: Int): String {
        return when (dayConstant) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Unknown Day"
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