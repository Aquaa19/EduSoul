// File: main/java/com/aquaa/edusoul/activities/teacher/ViewScheduleActivity.kt
package com.aquaa.edusoul.activities.teacher

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.ClassSessionAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.TeacherViewScheduleViewModel
import com.aquaa.edusoul.viewmodels.TeacherViewScheduleViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ViewScheduleActivity : BaseActivity() {

    private val TAG = "ViewScheduleActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var textViewSelectedDateDisplay: TextView
    private lateinit var recyclerViewClassSessions: RecyclerView
    // Removed: private lateinit var fabMarkAttendance: FloatingActionButton // This FAB is not in the layout
    private lateinit var textViewNoSessions: TextView
    private lateinit var calendarViewSchedule: CalendarView

    private lateinit var classSessionAdapter: ClassSessionAdapter
    private lateinit var teacherViewScheduleViewModel: TeacherViewScheduleViewModel
    private lateinit var dbDateFormatter: SimpleDateFormat
    private var currentSelectedDateYYYYMMDD: String? = null

    private lateinit var authManager: AuthManager
    private var currentTeacherId: String = ""

    private lateinit var subjectRepository: SubjectRepository
    private lateinit var batchRepository: BatchRepository
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_schedule)

        toolbar = findViewById(R.id.toolbarViewSchedule)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textViewSelectedDateDisplay = findViewById(R.id.textViewTeacherSelectedDate)
        recyclerViewClassSessions = findViewById(R.id.recyclerViewTeacherClassSessions)
        // Removed: fabMarkAttendance = findViewById(R.id.fabMarkAttendanceDaily) // Removed: No such ID in layout
        textViewNoSessions = findViewById(R.id.textViewNoTeacherSessions)
        calendarViewSchedule = findViewById(R.id.calendarViewTeacherSchedule)
        dbDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        authManager = AuthManager(this)

        subjectRepository = SubjectRepository()
        batchRepository = BatchRepository()
        userRepository = UserRepository()

        val classSessionRepository = ClassSessionRepository()
        val factory = TeacherViewScheduleViewModelFactory(
            classSessionRepository, subjectRepository, batchRepository, userRepository
        )
        teacherViewScheduleViewModel = ViewModelProvider(this, factory)[TeacherViewScheduleViewModel::class.java]

        classSessionAdapter = ClassSessionAdapter(
            this, ArrayList(), // No listener for view mode
            getSubjectNameById = { subjectId ->
                subjectId?.let { runBlocking { subjectRepository.getSubjectById(it)?.subjectName } } ?: "N/A"
            },
            getBatchNameById = { batchId ->
                batchId?.let { runBlocking { batchRepository.getBatchById(it)?.batchName } } ?: "N/A"
            },
            getTeacherNameById = { teacherId ->
                teacherId?.let { runBlocking { userRepository.getUserById(it)?.fullName } } ?: "N/A"
            }
        )
        recyclerViewClassSessions.layoutManager = LinearLayoutManager(this)
        recyclerViewClassSessions.adapter = classSessionAdapter

        val today = Calendar.getInstance()
        updateSelectedDateText(today)
        currentSelectedDateYYYYMMDD = dbDateFormatter.format(today.time)

        lifecycleScope.launch(Dispatchers.Main) {
            val currentUser: User? = authManager.getLoggedInUser()
            currentTeacherId = currentUser?.id ?: ""

            if (currentTeacherId.isBlank()) {
                Toast.makeText(this@ViewScheduleActivity, "Error: Teacher user not identified.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            teacherViewScheduleViewModel.setTeacherUserId(currentTeacherId)
            teacherViewScheduleViewModel.selectDate(currentSelectedDateYYYYMMDD!!)
            setupEventListeners()
            setupObservers()
        }
    }

    private fun setupEventListeners() {
        calendarViewSchedule.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            updateSelectedDateText(selectedCalendar)
            currentSelectedDateYYYYMMDD = dbDateFormatter.format(selectedCalendar.time)
            teacherViewScheduleViewModel.selectDate(currentSelectedDateYYYYMMDD!!)
        }

        // Removed: fabMarkAttendance.setOnClickListener block, as the FAB is no longer present
    }

    private fun setupObservers() {
        teacherViewScheduleViewModel.sessionsForSelectedDate.observe(this) { sessions ->
            classSessionAdapter.setSessions(sessions)
            textViewNoSessions.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        teacherViewScheduleViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                teacherViewScheduleViewModel.clearErrorMessage()
            }
        }
    }

    private fun updateSelectedDateText(calendar: Calendar) {
        val displaySdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        textViewSelectedDateDisplay.text = "Schedule for: " + displaySdf.format(calendar.time)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}