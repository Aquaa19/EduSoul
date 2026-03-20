// File: main/java/com/aquaa/edusoul/activities/parent/ParentViewAttendanceActivity.kt
package com.aquaa.edusoul.activities.parent

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.ParentAttendanceDetailAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.ParentAttendanceDetail
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ParentViewAttendanceViewModel
import com.aquaa.edusoul.viewmodels.ParentViewAttendanceViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext

class ParentViewAttendanceActivity : AppCompatActivity() {

    private val TAG = "ParentViewAttendanceAct"

    private lateinit var autoCompleteChildSelection: AutoCompleteTextView
    private lateinit var editTextStartDate: TextInputEditText
    private lateinit var editTextEndDate: TextInputEditText
    private lateinit var buttonFilterAttendance: MaterialButton
    private lateinit var recyclerViewAttendance: RecyclerView
    private lateinit var textViewNoAttendance: TextView

    private lateinit var authManager: AuthManager
    private var currentParentUserId: String = "" // Changed from Long to String
    private var selectedChild: Student? = null

    private lateinit var childrenOfParent: MutableList<Student>
    private lateinit var childNameToIdMap: MutableMap<String, String> // Changed value type from Long to String

    private lateinit var startDateCalendar: Calendar
    private lateinit var endDateCalendar: Calendar
    private lateinit var dateFormatter: SimpleDateFormat

    private lateinit var attendanceAdapter: ParentAttendanceDetailAdapter

    private lateinit var parentViewAttendanceViewModel: ParentViewAttendanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_view_attendance)

        val toolbar = findViewById<Toolbar>(R.id.toolbarParentViewAttendance)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            val user = authManager.getLoggedInUser()
            currentParentUserId = user?.id ?: "" // User.id is String, default to empty string if null

            if (currentParentUserId.isBlank()) { // Check for blank String ID
                Toast.makeText(this@ParentViewAttendanceActivity, "Error: Parent user not identified.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            initializeViews()
            setupViewModel()
            setupDatePickers()
            buttonFilterAttendance.setOnClickListener { loadAttendanceData() }
        }
    }

    private fun initializeViews() {
        autoCompleteChildSelection = findViewById(R.id.autoCompleteChildSelection)
        editTextStartDate = findViewById(R.id.editTextStartDate)
        editTextEndDate = findViewById(R.id.editTextEndDate)
        buttonFilterAttendance = findViewById(R.id.buttonFilterAttendance)
        recyclerViewAttendance = findViewById(R.id.recyclerViewAttendance)
        textViewNoAttendance = findViewById(R.id.textViewNoAttendance)

        childrenOfParent = mutableListOf()
        childNameToIdMap = mutableMapOf() // Initialized to store String IDs
        startDateCalendar = Calendar.getInstance()
        endDateCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
        dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        attendanceAdapter = ParentAttendanceDetailAdapter(this, mutableListOf())
        recyclerViewAttendance.layoutManager = LinearLayoutManager(this)
        recyclerViewAttendance.adapter = attendanceAdapter
    }

    private fun setupViewModel() {
        // AppDatabase.getDatabase is not used for DAOs directly
        val studentRepository = StudentRepository()
        val attendanceRepository = AttendanceRepository()
        val classSessionRepository = ClassSessionRepository()
        val subjectRepository = SubjectRepository()
        val batchRepository = BatchRepository()
        val userRepository = UserRepository()

        val factory = ParentViewAttendanceViewModelFactory(
            studentRepository,
            attendanceRepository,
            classSessionRepository,
            subjectRepository,
            batchRepository,
            userRepository
        )
        parentViewAttendanceViewModel = ViewModelProvider(this, factory)[ParentViewAttendanceViewModel::class.java]
        parentViewAttendanceViewModel.setParentUserId(currentParentUserId) // currentParentUserId is String

        setupChildSelectionAndObservers()
    }

    private fun setupChildSelectionAndObservers() {
        parentViewAttendanceViewModel.childrenOfParent.observe(this) { students ->
            childrenOfParent.clear()
            childrenOfParent.addAll(students)
            childNameToIdMap.clear()
            val childNames = mutableListOf<String>()

            if (childrenOfParent.isNotEmpty()) {
                childrenOfParent.forEach { student ->
                    val displayName = "${student.fullName} (Adm: ${student.admissionNumber})"
                    childNames.add(displayName)
                    childNameToIdMap[displayName] = student.id // Student.id is String
                }

                val childAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, childNames)
                autoCompleteChildSelection.setAdapter(childAdapter)

                if (selectedChild == null) {
                    autoCompleteChildSelection.setText(childNames[0], false)
                    selectedChild = childrenOfParent[0]
                    loadAttendanceData()
                }

                autoCompleteChildSelection.setOnItemClickListener { parent, _, position, _ ->
                    val selectedName = parent.getItemAtPosition(position) as String
                    val childId = childNameToIdMap[selectedName] // childId is String?
                    selectedChild = childrenOfParent.find { it.id == childId }
                    loadAttendanceData()
                }
                autoCompleteChildSelection.isEnabled = true
            } else {
                autoCompleteChildSelection.setText("No children linked", false)
                autoCompleteChildSelection.isEnabled = false
                textViewNoAttendance.text = "No children linked to this parent account."
                textViewNoAttendance.visibility = View.VISIBLE
                recyclerViewAttendance.visibility = View.GONE
            }
        }

        parentViewAttendanceViewModel.attendanceReport.observe(this) { fetchedAttendance ->
            attendanceAdapter.updateData(fetchedAttendance)
            if (fetchedAttendance.isNotEmpty()) {
                textViewNoAttendance.visibility = View.GONE
                recyclerViewAttendance.visibility = View.VISIBLE
            } else {
                textViewNoAttendance.text = "No attendance records found for ${selectedChild?.fullName ?: "this child"} in the selected period."
                textViewNoAttendance.visibility = View.VISIBLE
                recyclerViewAttendance.visibility = View.GONE
            }
        }

        parentViewAttendanceViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                parentViewAttendanceViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupDatePickers() {
        endDateCalendar = Calendar.getInstance()
        startDateCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
        updateDateRangeDisplay()

        editTextStartDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                startDateCalendar.set(year, month, dayOfMonth)
                updateDateRangeDisplay()
            }, startDateCalendar.get(Calendar.YEAR), startDateCalendar.get(Calendar.MONTH), startDateCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        editTextEndDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                endDateCalendar.set(year, month, dayOfMonth)
                updateDateRangeDisplay()
            }, endDateCalendar.get(Calendar.YEAR), endDateCalendar.get(Calendar.MONTH), endDateCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateDateRangeDisplay() {
        editTextStartDate.setText(dateFormatter.format(startDateCalendar.time))
        editTextEndDate.setText(dateFormatter.format(endDateCalendar.time))
    }

    private fun loadAttendanceData() {
        val child = selectedChild
        if (child == null) {
            textViewNoAttendance.text = "Please select a child to view attendance."
            textViewNoAttendance.visibility = View.VISIBLE
            recyclerViewAttendance.visibility = View.GONE
            return
        }

        val startDate = dateFormatter.format(startDateCalendar.time)
        val endDate = dateFormatter.format(endDateCalendar.time)
        parentViewAttendanceViewModel.selectChildAndDateRange(child.id, startDate, endDate) // child.id is String
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}