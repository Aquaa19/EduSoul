// File: EduSoul/app/src/main/java/com/aquaa/edusoul/activities/teacher/MarkAttendanceActivity.kt
package com.aquaa.edusoul.activities.teacher

import android.app.DatePickerDialog
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
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.StudentAttendanceAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.ClassSession
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.viewmodels.MarkAttendanceViewModel
import com.aquaa.edusoul.viewmodels.MarkAttendanceViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarkAttendanceActivity : BaseActivity() {

    private val TAG = "MarkAttendanceActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var spinnerSelectClassSession: Spinner
    private lateinit var recyclerViewStudentAttendance: RecyclerView
    private lateinit var buttonSubmitAttendance: Button
    private lateinit var textViewNoStudentsInClass: TextView
    private lateinit var editTextAttendanceDate: TextInputEditText

    private lateinit var viewModel: MarkAttendanceViewModel
    private lateinit var studentAttendanceAdapter: StudentAttendanceAdapter
    private var classSessionList: List<Pair<ClassSession, String>> = emptyList() // Updated type to Pair
    private var currentTeacher: User? = null
    private lateinit var selectedCalendar: Calendar
    private lateinit var dbDateFormatter: SimpleDateFormat

    // Flag to control initial spinner selection during programmatic updates
    private var blockSpinnerSelectionCallback = false


    // Constants for Intent Extras
    companion object {
        const val EXTRA_SESSION_ID = "SESSION_ID"
        const val EXTRA_SESSION_DATE = "SESSION_DATE"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_attendance)

        toolbar = findViewById(R.id.toolbarMarkAttendance)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        selectedCalendar = Calendar.getInstance()
        dbDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val authManager = AuthManager(this)

        // Initialize views and setup ViewModel/RecyclerView in main thread
        initViews()
        initViewModel()

        studentAttendanceAdapter = StudentAttendanceAdapter(emptyList())
        recyclerViewStudentAttendance.layoutManager = LinearLayoutManager(this@MarkAttendanceActivity)
        recyclerViewStudentAttendance.adapter = studentAttendanceAdapter

        setupDatePicker()
        setupEventListeners()
        setupObservers() // Call observers first so they're ready when data arrives

        // Launch coroutine on IO dispatcher for suspend calls
        lifecycleScope.launch(Dispatchers.IO) {
            currentTeacher = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) { // Switch back to Main for UI updates and initial setup calls
                if (currentTeacher == null) {
                    Toast.makeText(this@MarkAttendanceActivity, "Error: Could not identify teacher.", Toast.LENGTH_LONG).show()
                    finish()
                    return@withContext
                }

                val sessionIdFromIntent = intent.getStringExtra(EXTRA_SESSION_ID)
                val sessionDateFromIntent = intent.getStringExtra(EXTRA_SESSION_DATE)

                // Load sessions for the selected date via ViewModel
                val dateToLoad = sessionDateFromIntent ?: dbDateFormatter.format(selectedCalendar.time)
                currentTeacher?.id?.let { teacherId ->
                    viewModel.loadClassSessionsForDate(teacherId, dateToLoad)
                } ?: run {
                    Toast.makeText(this@MarkAttendanceActivity, "Error: Teacher information not available.", Toast.LENGTH_SHORT).show()
                    clearSessionSpinnerAndUpdateViewModel("")
                }
            }
        }
    }

    private fun initViews() {
        editTextAttendanceDate = findViewById(R.id.editTextAttendanceDate)
        spinnerSelectClassSession = findViewById(R.id.spinnerSelectClassSession)
        recyclerViewStudentAttendance = findViewById(R.id.recyclerViewStudentAttendance)
        buttonSubmitAttendance = findViewById(R.id.buttonSubmitAttendance)
        textViewNoStudentsInClass = findViewById(R.id.textViewNoStudentsInClass)
    }

    private fun initViewModel() {
        val factory = MarkAttendanceViewModelFactory(
            AttendanceRepository(),
            StudentRepository(),
            ClassSessionRepository(),
            SubjectRepository(), // Pass SubjectRepository to ViewModel
            BatchRepository() // Pass BatchRepository to ViewModel
        )
        viewModel = ViewModelProvider(this, factory)[MarkAttendanceViewModel::class.java]
    }

    private fun setupDatePicker() {
        updateDateInView()
        editTextAttendanceDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, monthOfYear, dayOfMonth ->
                    selectedCalendar.set(year, monthOfYear, dayOfMonth)
                    updateDateInView()
                    // Launch a coroutine to call the suspend function on ViewModel
                    lifecycleScope.launch {
                        Log.d(TAG, "Date changed by DatePicker. Calling ViewModel to load sessions.")
                        val dateToLoad = dbDateFormatter.format(selectedCalendar.time)
                        currentTeacher?.id?.let { teacherId ->
                            viewModel.loadClassSessionsForDate(teacherId, dateToLoad)
                        } ?: run {
                            Toast.makeText(this@MarkAttendanceActivity, "Error: Teacher information not available.", Toast.LENGTH_SHORT).show()
                            clearSessionSpinnerAndUpdateViewModel("")
                        }
                    }
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun updateDateInView() {
        editTextAttendanceDate.setText(dbDateFormatter.format(selectedCalendar.time))
    }

    private fun setupEventListeners() {
        spinnerSelectClassSession.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Use the flag to ignore initial programmatic selection
                if (blockSpinnerSelectionCallback) {
                    Log.d(TAG, "Spinner onItemSelected: BLOCKED programmatic selection.")
                    return
                }

                // Access the ClassSession from the Pair
                val selectedSessionPair = classSessionList.getOrNull(position)
                val selectedSession = selectedSessionPair?.first // Get the ClassSession object

                if (selectedSession != null && selectedSession.id.isNotBlank()) {
                    Log.d(TAG, "Spinner onItemSelected: User selected session: ${selectedSession.id}")
                    viewModel.loadAttendanceDataForSession(selectedSession.id)
                } else {
                    // This might happen if "No sessions" or similar is selected by the user
                    Log.d(TAG, "Spinner onItemSelected: User selected invalid/empty session. Position: $position")
                    viewModel.loadAttendanceDataForSession("") // Tell ViewModel no session is active
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                if (blockSpinnerSelectionCallback) {
                    Log.d(TAG, "Spinner onNothingSelected: BLOCKED.")
                    return
                }
                Log.d(TAG, "Spinner onNothingSelected: Clearing ViewModel session.")
                viewModel.loadAttendanceDataForSession("")
            }
        }

        buttonSubmitAttendance.setOnClickListener {
            if (viewModel.isEditMode.value == true) {
                val selectedSession = viewModel.classSession.value
                val attendanceData = studentAttendanceAdapter.getAttendanceData()

                if (selectedSession == null || selectedSession.id.isBlank()) {
                    Toast.makeText(this, "Please select a valid session.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (attendanceData.isEmpty()){
                    Toast.makeText(this, "Student list is empty.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.saveAttendance(selectedSession.id, attendanceData)
            } else {
                // If not in edit mode, toggle to edit mode
                viewModel.setEditMode(true)
            }
        }
    }

    private fun setupObservers() {
        viewModel.studentAttendanceList.observe(this) { studentList ->
            studentAttendanceAdapter.updateList(studentList)
            // Visibility of RecyclerView and TextViewNoStudentsInClass is now handled by isEditMode observer
            // and the attendanceMarkedStatus observer.
        }

        viewModel.attendanceMarkedStatus.observe(this) { isMarked ->
            if (isMarked) {
                buttonSubmitAttendance.text = "Update Marked Attendance"
                if (viewModel.isEditMode.value == false) { // Only update text/visibility if not in edit mode
                    textViewNoStudentsInClass.text = "Attendance already marked for this session."
                    textViewNoStudentsInClass.visibility = View.VISIBLE
                    recyclerViewStudentAttendance.visibility = View.GONE
                }
            } else {
                buttonSubmitAttendance.text = "Submit Attendance"
                if (viewModel.isEditMode.value == false) { // Only update text/visibility if not in edit mode
                    textViewNoStudentsInClass.text = "Select a class session to see students."
                    textViewNoStudentsInClass.visibility = View.VISIBLE
                    recyclerViewStudentAttendance.visibility = View.GONE
                }
            }
            buttonSubmitAttendance.visibility = if (viewModel.studentAttendanceList.value?.isNotEmpty() == true) View.VISIBLE else View.GONE
        }

        viewModel.isEditMode.observe(this) { inEditMode ->
            if (inEditMode) {
                recyclerViewStudentAttendance.visibility = View.VISIBLE
                textViewNoStudentsInClass.visibility = View.GONE
                buttonSubmitAttendance.text = "Submit Attendance" // Button text for saving changes
            } else {
                recyclerViewStudentAttendance.visibility = View.GONE
                // Re-evaluate text and visibility based on attendanceMarkedStatus when exiting edit mode
                viewModel.attendanceMarkedStatus.value?.let { isMarked ->
                    if (isMarked) {
                        textViewNoStudentsInClass.text = "Attendance already marked for this session."
                        textViewNoStudentsInClass.visibility = View.VISIBLE
                    } else {
                        textViewNoStudentsInClass.text = "Select a class session to see students."
                        textViewNoStudentsInClass.visibility = View.VISIBLE
                    }
                }
                // Button text will be set by attendanceMarkedStatus observer
            }
            buttonSubmitAttendance.visibility = if (viewModel.studentAttendanceList.value?.isNotEmpty() == true) View.VISIBLE else View.GONE
        }

        // Observer for class sessions from ViewModel to populate the spinner
        viewModel.classSessionsForDate.observe(this) { sessionsWithDisplayNames ->
            Log.d(TAG, "classSessionsForDate observer triggered with ${sessionsWithDisplayNames.size} sessions.")
            classSessionList = sessionsWithDisplayNames // Update the activity's local list of Pairs

            blockSpinnerSelectionCallback = true // Block listener during programmatic update

            if (classSessionList.isEmpty()) {
                val noSessionsAdapter = ArrayAdapter(this@MarkAttendanceActivity, android.R.layout.simple_spinner_item, listOf("No sessions for this date"))
                noSessionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSelectClassSession.adapter = noSessionsAdapter
                spinnerSelectClassSession.setSelection(0, false)
                viewModel.loadAttendanceDataForSession("") // No session to load attendance for
            } else {
                val displayList = classSessionList.map { it.second } // Use the pre-prepared display string
                val adapter = ArrayAdapter(this@MarkAttendanceActivity, android.R.layout.simple_spinner_item, displayList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSelectClassSession.adapter = adapter

                // Handle pre-selection from intent
                val sessionIdFromIntent = intent.getStringExtra(EXTRA_SESSION_ID)
                var sessionToLoadId: String? = null
                val indexToSelect = if (sessionIdFromIntent != null) {
                    classSessionList.indexOfFirst { it.first.id == sessionIdFromIntent }.also {
                        if (it != -1) sessionToLoadId = sessionIdFromIntent
                        else Log.w(TAG, "PreselectSessionId $sessionIdFromIntent not found in loaded sessions for this date.")
                    }
                } else -1

                if (sessionToLoadId != null && indexToSelect != -1) {
                    Log.d(TAG, "Pre-selecting session ID: $sessionToLoadId at index $indexToSelect")
                    spinnerSelectClassSession.setSelection(indexToSelect, false)
                    // Trigger loading attendance for the pre-selected session
                    viewModel.loadAttendanceDataForSession(sessionToLoadId!!)
                } else {
                    Log.d(TAG, "No pre-selection or pre-selection failed. Selecting first session and loading attendance.")
                    spinnerSelectClassSession.setSelection(0, false)
                    viewModel.loadAttendanceDataForSession(classSessionList[0].first.id) // Load attendance for the first session
                }
            }
            blockSpinnerSelectionCallback = false // Re-enable listener
            Log.d(TAG, "Spinner update complete from ViewModel observer. blockSpinnerSelectionCallback = false")
        }


        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun clearSessionSpinnerAndUpdateViewModel(viewModelSessionIdToLoad: String) {
        // Assumed to be called on Main thread or will delegate UI to Main thread
        Log.d(TAG, "clearSessionSpinnerAndUpdateViewModel called. ViewModel will load session: '$viewModelSessionIdToLoad'")
        blockSpinnerSelectionCallback = true
        val noSessionsAdapter = ArrayAdapter(this@MarkAttendanceActivity, android.R.layout.simple_spinner_item, listOf("Select a session"))
        noSessionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSelectClassSession.adapter = noSessionsAdapter
        classSessionList = emptyList() // Clear local list to match new type
        viewModel.loadAttendanceDataForSession(viewModelSessionIdToLoad) // Update ViewModel
        blockSpinnerSelectionCallback = false
    }

    private fun clearStudentList() {
        studentAttendanceAdapter.updateList(emptyList())
        textViewNoStudentsInClass.text = "Select a class session to see students."
        textViewNoStudentsInClass.visibility = View.VISIBLE
        recyclerViewStudentAttendance.visibility = View.GONE
        buttonSubmitAttendance.visibility = View.GONE
        viewModel.setAttendanceMarkedStatus(false)
        viewModel.setEditMode(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}