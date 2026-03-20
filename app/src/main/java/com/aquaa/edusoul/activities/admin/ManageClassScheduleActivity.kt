// File: src/main/java/com/aquaa/edusoul/activities/admin/ManageClassScheduleActivity.kt
package com.aquaa.edusoul.activities.admin

import android.Manifest
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.ClassSessionAdapter
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.ClassSession
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.RecurringClassSessionRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ClassSessionViewModel
import com.aquaa.edusoul.viewmodels.ClassSessionViewModelFactory
import com.aquaa.edusoul.utils.AiAssistantConstants // Import AiAssistantConstants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ManageClassScheduleActivity : BaseActivity(), ClassSessionAdapter.OnSessionActionsListener {

    private val TAG = "ManageClassSchedule"

    private lateinit var toolbar: Toolbar
    private lateinit var textViewSelectedDateDisplay: TextView
    private lateinit var recyclerViewClassSessions: RecyclerView
    private lateinit var fabAddClassSession: FloatingActionButton
    private lateinit var textViewNoSessions: TextView
    private lateinit var calendarViewSchedule: CalendarView

    private lateinit var classSessionAdapter: ClassSessionAdapter
    private lateinit var classSessionViewModel: ClassSessionViewModel
    private lateinit var dbDateFormatter: SimpleDateFormat
    private var currentSelectedDateYYYYMMDD: String? = null

    private lateinit var subjectRepository: SubjectRepository
    private lateinit var batchRepository: BatchRepository
    private lateinit var userRepository: UserRepository

    private var currentMonth = -1 // To track current displayed month in CalendarView
    private var currentYear = -1 // To track current displayed year in CalendarView
    private var currentTeacherId: String = "" // Added to store the logged-in teacher ID

    private val requestCalendarPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
            val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false
            if (readGranted && writeGranted) {
                Log.d(TAG, "All calendar permissions granted.")
                syncAllSessionsToCalendar()
            } else {
                Toast.makeText(this, "Both Read and Write calendar permissions are required to sync schedule automatically.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_class_schedule)

        toolbar = findViewById(R.id.toolbarManageSchedule)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textViewSelectedDateDisplay = findViewById(R.id.textViewSelectedDate)
        recyclerViewClassSessions = findViewById(R.id.recyclerViewClassSessions)
        fabAddClassSession = findViewById(R.id.fabAddClassSession)
        textViewNoSessions = findViewById(R.id.textViewNoSessions)
        calendarViewSchedule = findViewById(R.id.calendarViewSchedule)
        dbDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fabAddClassSession.isEnabled = false

        batchRepository = BatchRepository()
        subjectRepository = SubjectRepository()
        userRepository = UserRepository()
        val classSessionRepository = ClassSessionRepository()
        val recurringClassSessionRepository = RecurringClassSessionRepository()


        val factory = ClassSessionViewModelFactory(
            classSessionRepository, recurringClassSessionRepository, batchRepository, subjectRepository, userRepository
        )
        classSessionViewModel = ViewModelProvider(this, factory)[ClassSessionViewModel::class.java]

        classSessionAdapter = ClassSessionAdapter(
            this, ArrayList(), this,
            getSubjectNameById = { subjectId ->
                // subjectId is already String?, no need for .toString()
                subjectId?.let { runBlocking { subjectRepository.getSubjectById(it)?.subjectName } } ?: "N/A"
            },
            getBatchNameById = { batchId ->
                // batchId is already String?, no need for .toString()
                batchId?.let { runBlocking { batchRepository.getBatchById(it)?.batchName } } ?: "N/A"
            },
            getTeacherNameById = { teacherId ->
                // teacherId is already String?, no need for .toString()
                teacherId?.let { runBlocking { userRepository.getUserById(it)?.fullName } } ?: "N/A"
            }
        )
        recyclerViewClassSessions.layoutManager = LinearLayoutManager(this)
        recyclerViewClassSessions.adapter = classSessionAdapter

        val today = Calendar.getInstance()
        updateSelectedDateText(today)
        currentSelectedDateYYYYMMDD = dbDateFormatter.format(today.time)

        // Initialize currentMonth and currentYear for tracking
        currentMonth = today.get(Calendar.MONTH)
        currentYear = today.get(Calendar.YEAR)

        setupEventListeners()
        setupObservers()

        // Fetch logged-in teacher's ID
        lifecycleScope.launch(Dispatchers.Main) {
            val authManager = com.aquaa.edusoul.auth.AuthManager(this@ManageClassScheduleActivity)
            val currentUser = authManager.getLoggedInUser()
            currentTeacherId = currentUser?.id ?: ""

            if (currentTeacherId.isBlank()) {
                Toast.makeText(this@ManageClassScheduleActivity, "Error: Teacher ID not found.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            // Trigger initial month's session generation
            classSessionViewModel.allBatches.observe(this@ManageClassScheduleActivity) { allBatchesList ->
                // Only trigger generation if batches are loaded and it's the initial load or a month change
                if (currentTeacherId.isNotBlank() && allBatchesList != null && allBatchesList.isNotEmpty()) {
                    lifecycleScope.launch {
                        classSessionViewModel.generateAndSyncMonthlySessions(
                            currentYear,
                            currentMonth,
                            currentTeacherId,
                            allBatchesList
                        )
                    }
                }
            }

            // After initial generation, load sessions for the selected day
            classSessionViewModel.selectDate(currentSelectedDateYYYYMMDD!!)

            if (intent.getBooleanExtra(AiAssistantConstants.EXTRA_PREFILL_DATA, false)) {
                val subjectName = intent.getStringExtra(AiAssistantConstants.EXTRA_SUBJECT_NAME)
                val batchName = intent.getStringExtra(AiAssistantConstants.EXTRA_BATCH_NAME)
                val dayOfWeekStr = intent.getStringExtra(AiAssistantConstants.EXTRA_DAY_OF_WEEK)
                val startTime = intent.getStringExtra(AiAssistantConstants.EXTRA_START_TIME)
                val endTime = intent.getStringExtra(AiAssistantConstants.EXTRA_END_TIME)
                val teacherName = intent.getStringExtra(AiAssistantConstants.EXTRA_TEACHER_NAME)

                val calendar = Calendar.getInstance()
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val targetDayOfWeek = when (dayOfWeekStr?.lowercase()) {
                    "sunday" -> Calendar.SUNDAY
                    "monday" -> Calendar.MONDAY
                    "tuesday" -> Calendar.TUESDAY
                    "wednesday" -> Calendar.WEDNESDAY
                    "thursday" -> Calendar.THURSDAY
                    "friday" -> Calendar.FRIDAY
                    "saturday" -> Calendar.SATURDAY
                    else -> currentDayOfWeek
                }

                if (targetDayOfWeek != currentDayOfWeek) {
                    val daysToAdd = (targetDayOfWeek - currentDayOfWeek + 7) % 7
                    calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                    calendarViewSchedule.date = calendar.timeInMillis
                    updateSelectedDateText(calendar)
                    currentSelectedDateYYYYMMDD = dbDateFormatter.format(calendar.time)
                }

                val prefillBundle = Bundle().apply {
                    putString(AiAssistantConstants.EXTRA_SUBJECT_NAME, subjectName)
                    putString(AiAssistantConstants.EXTRA_BATCH_NAME, batchName)
                    putString(AiAssistantConstants.EXTRA_START_TIME, startTime)
                    putString(AiAssistantConstants.EXTRA_END_TIME, endTime)
                    putString(AiAssistantConstants.EXTRA_TEACHER_NAME, teacherName)
                }
                showAddEditClassSessionDialog(null, currentSelectedDateYYYYMMDD!!, prefillBundle)
            }
        }
    }

    private fun setupEventListeners() {
        calendarViewSchedule.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            updateSelectedDateText(selectedCalendar)
            currentSelectedDateYYYYMMDD = dbDateFormatter.format(selectedCalendar.time)

            // Check if month or year has changed
            if (month != currentMonth || year != currentYear) {
                currentMonth = month
                currentYear = year
                if (currentTeacherId.isNotBlank()) {
                    classSessionViewModel.allBatches.value?.let { allBatchesList ->
                        lifecycleScope.launch {
                            classSessionViewModel.generateAndSyncMonthlySessions(
                                currentYear,
                                currentMonth,
                                currentTeacherId,
                                allBatchesList
                            )
                        }
                    }
                }
            }
            classSessionViewModel.selectDate(currentSelectedDateYYYYMMDD!!)
        }

        fabAddClassSession.setOnClickListener {
            if (!currentSelectedDateYYYYMMDD.isNullOrEmpty()) {
                showAddEditClassSessionDialog(null, currentSelectedDateYYYYMMDD!!)
            } else {
                Toast.makeText(this, "Please select a date first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        classSessionViewModel.sessionsForSelectedDate.observe(this) { sessions ->
            classSessionAdapter.setSessions(sessions)
            textViewNoSessions.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        classSessionViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                classSessionViewModel.clearErrorMessage()
            }
        }

        classSessionViewModel.isDataReadyForDialog.observe(this) { isReady ->
            fabAddClassSession.isEnabled = isReady
        }
    }

    private fun updateSelectedDateText(calendar: Calendar) {
        val displaySdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) // Added yyyy to format
        textViewSelectedDateDisplay.text = "Schedule for: " + displaySdf.format(calendar.time)
    }

    private fun showAddEditClassSessionDialog(
        sessionToEdit: ClassSession?,
        sessionDate: String,
        prefillData: Bundle? = null
    ) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_class_session, null)
        builder.setView(dialogView)

        val spinnerBatch = dialogView.findViewById<Spinner>(R.id.spinnerBatchSessionDialog)
        val spinnerSubject = dialogView.findViewById<Spinner>(R.id.spinnerSubjectSessionDialog)
        val spinnerTeacher = dialogView.findViewById<Spinner>(R.id.spinnerTeacherSessionDialog)
        val editTextStartTime = dialogView.findViewById<EditText>(R.id.editTextStartTimeSessionDialog)
        val editTextEndTime = dialogView.findViewById<EditText>(R.id.editTextEndTimeSessionDialog)
        val editTextTopic = dialogView.findViewById<EditText>(R.id.editTextTopicSessionDialog)

        val batches = classSessionViewModel.allBatches.value ?: emptyList()
        val subjects = classSessionViewModel.allSubjects.value ?: emptyList()
        val teachers = classSessionViewModel.allTeachers.value ?: emptyList()

        spinnerBatch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(batches))
        spinnerSubject.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(subjects))
        spinnerTeacher.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teachers.map { it.fullName })

        setupTimePicker(editTextStartTime)
        setupTimePicker(editTextEndTime)

        builder.setTitle(if (sessionToEdit == null) "Add New Session for $sessionDate" else "Edit Session")

        if (sessionToEdit != null) {
            // Corrected: IDs for setSelection are now String
            spinnerBatch.setSelection(batches.indexOfFirst { it.id == sessionToEdit.batchId }.coerceAtLeast(0))
            spinnerSubject.setSelection(subjects.indexOfFirst { it.id == sessionToEdit.subjectId }.coerceAtLeast(0))
            // Corrected: Use teacher.id for comparison in find, but spinner uses fullName
            spinnerTeacher.setSelection(teachers.indexOfFirst { it.id == sessionToEdit.teacherUserId }.coerceAtLeast(0))


            editTextStartTime.setText(sessionToEdit.startTime)
            editTextEndTime.setText(sessionToEdit.endTime)
            editTextTopic.setText(sessionToEdit.topicCovered)
        } else if (prefillData != null) {
            val prefillSubjectName = prefillData.getString(AiAssistantConstants.EXTRA_SUBJECT_NAME)
            val prefillBatchName = prefillData.getString(AiAssistantConstants.EXTRA_BATCH_NAME)
            val prefillStartTime = prefillData.getString(AiAssistantConstants.EXTRA_START_TIME)
            val prefillEndTime = prefillData.getString(AiAssistantConstants.EXTRA_END_TIME)
            val prefillTeacherName = prefillData.getString(AiAssistantConstants.EXTRA_TEACHER_NAME)

            val subjectPosition = subjects.indexOfFirst { it.subjectName.equals(prefillSubjectName, ignoreCase = true) }
            if (subjectPosition != -1) spinnerSubject.setSelection(subjectPosition)

            val batchPosition = batches.indexOfFirst { it.batchName.equals(prefillBatchName, ignoreCase = true) }
            if (batchPosition != -1) spinnerBatch.setSelection(batchPosition)

            val teacherPosition = teachers.indexOfFirst { it.fullName.equals(prefillTeacherName, ignoreCase = true) }
            if (teacherPosition != -1) spinnerTeacher.setSelection(teacherPosition)

            editTextStartTime.setText(prefillStartTime)
            editTextEndTime.setText(prefillEndTime)

        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedBatch = spinnerBatch.selectedItem as? Batch
            val selectedSubject = spinnerSubject.selectedItem as? Subject
            val selectedTeacherName = spinnerTeacher.selectedItem as? String
            // Corrected: Find the actual Teacher User object by full name
            val selectedTeacher = (classSessionViewModel.allTeachers.value ?: emptyList()).find { it.fullName == selectedTeacherName }

            if (selectedBatch == null || selectedSubject == null || selectedTeacher == null) {
                Toast.makeText(this, "Please select Batch, Subject, and Teacher.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val startTime = editTextStartTime.text.toString().trim()
            val endTime = editTextEndTime.text.toString().trim()
            val topic = editTextTopic.text.toString().trim()

            if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
                Toast.makeText(this, "Invalid time format. Use HH:MM (24-hour).", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val session = ClassSession(
                id = sessionToEdit?.id ?: "", // Corrected: Default ID to empty string for String type
                batchId = selectedBatch.id, // Corrected: batchId is String
                subjectId = selectedSubject.id, // Corrected: subjectId is String
                teacherUserId = selectedTeacher.id, // Corrected: teacherUserId is String
                sessionDate = sessionDate,
                startTime = startTime,
                endTime = endTime,
                topicCovered = topic.ifEmpty { null },
                status = "Scheduled"
            )

            if (sessionToEdit == null) {
                classSessionViewModel.addClassSession(session)
            } else {
                classSessionViewModel.updateClassSession(session)
            }
            alertDialog.dismiss()
        }
    }

    private fun isValidTimeFormat(time: String): Boolean = time.matches("^([01]\\d|2[0-3]):([0-5]\\d)$".toRegex())

    private fun setupTimePicker(timeEditText: EditText) {
        timeEditText.setOnClickListener {
            val currentTime = Calendar.getInstance()
            val hour = currentTime.get(Calendar.HOUR_OF_DAY)
            val minute = currentTime.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                timeEditText.setText(String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute))
            }, hour, minute, true).show()
        }
    }

    override fun onEditSession(session: ClassSession, position: Int) {
        showAddEditClassSessionDialog(session, session.sessionDate)
    }

    override fun onDeleteSession(session: ClassSession, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Session")
            .setMessage("Are you sure you want to delete this session?")
            .setPositiveButton("Delete") { _, _ -> classSessionViewModel.deleteClassSession(session.id) } // Corrected: session.id is String
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.manage_class_schedule_menu, menu)
        menu.findItem(R.id.action_sync_calendar).title = "Sync Month to Calendar"
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_sync_calendar -> {
                checkPermissionAndSync()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkPermissionAndSync() {
        val hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

        if (hasReadPermission && hasWritePermission) {
            syncAllSessionsToCalendar()
        } else {
            requestCalendarPermissionsLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        }
    }

    private fun getCalendarId(): Long {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        var calCursor: Cursor? = null
        try {
            calCursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.VISIBLE + " = 1 AND " + CalendarContract.Calendars.IS_PRIMARY + "=1",
                null,
                CalendarContract.Calendars._ID + " ASC"
            )
            if (calCursor != null && calCursor.count <= 0) {
                calCursor.close()
                calCursor = contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    CalendarContract.Calendars.VISIBLE + " = 1",
                    null,
                    CalendarContract.Calendars._ID + " ASC"
                )
            }
            if (calCursor != null && calCursor.moveToFirst()) {
                val calIDCol = calCursor.getColumnIndex(CalendarContract.Calendars._ID)
                return calCursor.getLong(calIDCol)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in getCalendarId", e)
            Toast.makeText(this, "Calendar permission denied.", Toast.LENGTH_SHORT).show()
        } finally {
            calCursor?.close()
        }
        return -1L
    }

    private fun syncAllSessionsToCalendar() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = calendarViewSchedule.date
        }

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time) // Corrected: Added yyyy to format

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = dbDateFormatter.format(calendar.time)
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val endDate = dbDateFormatter.format(calendar.time)

        Toast.makeText(this, "Fetching schedule for $monthName...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            val sessionsToSync = classSessionViewModel.getSessionsForMonth(startDate, endDate)

            withContext(Dispatchers.Main) {
                if (sessionsToSync.isNullOrEmpty()) {
                    Toast.makeText(this@ManageClassScheduleActivity, "No sessions to sync for $monthName.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val calId = getCalendarId()
                if (calId == -1L) {
                    Toast.makeText(this@ManageClassScheduleActivity, "No writable calendar found.", Toast.LENGTH_LONG).show()
                    return@withContext
                }

                var eventsAdded = 0
                for (session in sessionsToSync) {
                    val subjectName = runBlocking { subjectRepository.getSubjectById(session.subjectId)?.subjectName ?: "Unknown" }
                    val batchName = runBlocking { batchRepository.getBatchById(session.batchId)?.batchName ?: "Unknown" }

                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        val startDateTime = sdf.parse("${session.sessionDate} ${session.startTime}")
                        val endDateTime = sdf.parse("${session.sessionDate} ${session.endTime}")

                        if (startDateTime != null && endDateTime != null) {
                            val values = ContentValues().apply {
                                put(CalendarContract.Events.DTSTART, startDateTime.time)
                                put(CalendarContract.Events.DTEND, endDateTime.time)
                                put(CalendarContract.Events.TITLE, "Class: $subjectName - $batchName")
                                put(CalendarContract.Events.DESCRIPTION, session.topicCovered ?: "Class Session")
                                put(CalendarContract.Events.CALENDAR_ID, calId)
                                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                            }
                            val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

                            uri?.lastPathSegment?.toLongOrNull()?.let { eventID ->
                                val reminderValues = ContentValues().apply {
                                    put(CalendarContract.Reminders.MINUTES, 15)
                                    put(CalendarContract.Reminders.EVENT_ID, eventID)
                                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                                }
                                contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                                eventsAdded++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not create calendar event for session ${session.id}: ${e.message}", e)
                    }
                }

                if (eventsAdded > 0) {
                    Toast.makeText(this@ManageClassScheduleActivity, "$eventsAdded sessions for $monthName synced to your calendar.", Toast.LENGTH_LONG).show()
                } else if (sessionsToSync.isNotEmpty()) {
                    Toast.makeText(this@ManageClassScheduleActivity, "Could not sync sessions. Please check permissions or if they are already synced.", Toast.LENGTH_LONG).show() // Added clearer message
                }
            }
        }
    }
}