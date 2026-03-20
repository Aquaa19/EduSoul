// File: EduSoul/app/src/main/java/com/aquaa/edusoul/viewmodels/MarkAttendanceViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Attendance
import com.aquaa.edusoul.models.ClassSession
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentAttendanceData
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.BatchRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.lifecycle.switchMap
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext // Explicitly import coroutineContext

class MarkAttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val classSessionRepository: ClassSessionRepository,
    private val subjectRepository: SubjectRepository, // Injected
    private val batchRepository: BatchRepository // Injected
) : ViewModel() {

    private val TAG = "MarkAttendanceVM"

    private val _classSessionId = MutableLiveData<String>()
    private val _classSessionDetails = MutableLiveData<ClassSession?>()
    val classSession: LiveData<ClassSession?> get() = _classSessionDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _attendanceMarkedStatus = MutableLiveData<Boolean>(false)
    val attendanceMarkedStatus: LiveData<Boolean> get() = _attendanceMarkedStatus

    private val _isEditMode = MutableLiveData<Boolean>(false)
    val isEditMode: LiveData<Boolean> get() = _isEditMode

    // LiveData for class sessions to populate the spinner in the Activity
    private val _classSessionsForDate = MutableLiveData<List<Pair<ClassSession, String>>>()
    val classSessionsForDate: LiveData<List<Pair<ClassSession, String>>> get() = _classSessionsForDate


    fun setAttendanceMarkedStatus(isMarked: Boolean) {
        _attendanceMarkedStatus.postValue(isMarked)
    }

    fun setEditMode(inEditMode: Boolean) {
        _isEditMode.postValue(inEditMode)
    }

    val studentAttendanceList: LiveData<List<StudentAttendanceData>> = _classSessionId.switchMap { sessionId ->
        // Log when switchMap is triggered for a new session ID
        Log.d(TAG, "studentAttendanceList switchMap triggered. Session ID: '$sessionId'.")

        if (sessionId.isBlank()) {
            Log.d(TAG, "Session ID is blank. Clearing attendance status, edit mode, and emitting empty list.")
            setAttendanceMarkedStatus(false)
            setEditMode(false)
            _classSessionDetails.postValue(null)
            // Return an empty flow converted to LiveData
            return@switchMap flowOf(emptyList<StudentAttendanceData>()).asLiveData(Dispatchers.IO)
        }

        liveData(Dispatchers.IO) {
            // Get a unique ID for this specific coroutine instance for logging
            // Use explicitly imported coroutineContext to resolve the reference
            val coroutineContextId = kotlin.coroutines.coroutineContext[Job]?.hashCode()
            Log.d(TAG, "liveData block [$coroutineContextId] START for session: $sessionId")

            _isLoading.postValue(true)
            _errorMessage.postValue(null) // Clear previous errors when loading new data

            try {
                Log.d(TAG, "liveData block [$coroutineContextId] Fetching session details for: $sessionId")
                val session = classSessionRepository.getClassSessionById(sessionId)
                _classSessionDetails.postValue(session) // Post session details to LiveData

                if (session == null || session.id.isBlank()) {
                    Log.w(TAG, "liveData block [$coroutineContextId] Error: Class session not found or ID is blank for $sessionId.")
                    _errorMessage.postValue("Error: Class session not found.")
                    setAttendanceMarkedStatus(false)
                    setEditMode(false)
                    emit(emptyList<StudentAttendanceData>()) // Emit empty list on error
                    Log.d(TAG, "liveData block [$coroutineContextId] END early due to missing session.")
                    return@liveData
                }

                Log.d(TAG, "liveData block [$coroutineContextId] Session details found: ${session.id}. Fetching students for batch: ${session.batchId}")

                // Use firstOrNull() to safely get students, or an empty list if flow is empty
                val studentsInBatch = studentRepository.getStudentsByBatch(session.batchId).firstOrNull() ?: emptyList()
                Log.d(TAG, "liveData block [$coroutineContextId] Fetched ${studentsInBatch.size} students for batch ${session.batchId}")

                if (studentsInBatch.isEmpty()) {
                    Log.d(TAG, "liveData block [$coroutineContextId] No students in batch. Emitting empty list. Setting edit mode true.")
                    emit(emptyList<StudentAttendanceData>()) // Emit empty list if no students
                    setAttendanceMarkedStatus(false) // No attendance if no students
                    setEditMode(true) // Default to edit mode if no existing attendance
                    Log.d(TAG, "liveData block [$coroutineContextId] END due to no students.")
                    return@liveData
                }

                Log.d(TAG, "liveData block [$coroutineContextId] Collecting attendance for session $sessionId")
                // Collect existing attendance records in real-time
                attendanceRepository.getAttendanceForSession(sessionId).collect { existingAttendance ->
                    Log.d(TAG, "liveData block [$coroutineContextId] Real-time attendance update: ${existingAttendance.size} records for session $sessionId.")

                    val attendanceMap = existingAttendance.associateBy { it.studentId }

                    // Combine student data with existing attendance records
                    val combinedList = studentsInBatch.map { student ->
                        val record = attendanceMap[student.id]
                        StudentAttendanceData(
                            student = student,
                            status = record?.attendanceStatus ?: Attendance.STATUS_PRESENT, // Default to Present
                            remarks = record?.remarks,
                            attendanceId = record?.id
                        )
                    }
                    Log.d(TAG, "liveData block [$coroutineContextId] Emitting combined list of ${combinedList.size} items.")
                    emit(combinedList) // Emit the combined list to the LiveData

                    // Update attendance status and edit mode based on fetched data
                    val isAttendanceActuallyMarked = existingAttendance.isNotEmpty()
                    setAttendanceMarkedStatus(isAttendanceActuallyMarked)
                    if (isAttendanceActuallyMarked) {
                        setEditMode(false) // If marked, default to view mode
                    } else {
                        setEditMode(true) // If not marked, default to edit mode
                    }
                }
            } catch (e: Exception) {
                // IMPORTANT: Filter out CancellationException. This is expected behavior when
                // switchMap cancels an old coroutine and should NOT be reported as an error.
                if (e is CancellationException) {
                    Log.d(TAG, "liveData block [$coroutineContextId] was cancelled (expected behavior): ${e.message}")
                    // Do nothing, as this is a normal cancellation
                } else {
                    _errorMessage.postValue("Failed to load attendance data: ${e.message}")
                    Log.e(TAG, "liveData block [$coroutineContextId] Error loading attendance data: ${e.message}", e)
                    setAttendanceMarkedStatus(false)
                    setEditMode(false)
                    emit(emptyList<StudentAttendanceData>()) // Emit empty list on actual error
                }
            } finally {
                _isLoading.postValue(false) // Ensure loading indicator is turned off
                Log.d(TAG, "liveData block [$coroutineContextId] FINALLY block executed.")
            }
        }
    }

    // New suspend function to load class sessions and prepare display strings
    suspend fun loadClassSessionsForDate(teacherId: String, date: String) {
        _isLoading.postValue(true)
        try {
            val sessions = classSessionRepository.getScheduledSessionsForTeacher(teacherId, date)
                .firstOrNull() ?: emptyList()

            val sessionsWithDisplayNames = sessions.map { session ->
                // Fetch subject and batch names in a suspend context
                val subjectName = subjectRepository.getSubjectById(session.subjectId)?.subjectName ?: "Unknown Subject"
                val batchName = batchRepository.getBatchById(session.batchId)?.batchName ?: "Unknown Batch"
                session to "${session.startTime} - $subjectName ($batchName)" // Pair of ClassSession and display string
            }
            _classSessionsForDate.postValue(sessionsWithDisplayNames)
        } catch (e: Exception) {
            _errorMessage.postValue("Failed to load class sessions: ${e.message}")
            Log.e(TAG, "Error loading class sessions: ${e.message}", e)
            _classSessionsForDate.postValue(emptyList()) // Post empty list on error
        } finally {
            _isLoading.postValue(false)
        }
    }


    fun loadAttendanceDataForSession(sessionId: String) {
        _classSessionId.value = sessionId
    }

    fun saveAttendance(sessionId: String, attendanceDataList: List<StudentAttendanceData>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val session = classSessionRepository.getClassSessionById(sessionId)
                if (session == null || session.id.isBlank()) {
                    withContext(Dispatchers.Main) { _errorMessage.postValue("Cannot save: Session not found.") }
                    return@launch
                }

                val recordsToSave = attendanceDataList.map { data ->
                    Attendance(
                        id = data.attendanceId ?: "", // Use existing ID if present, otherwise empty string for new
                        studentId = data.student.id,
                        classSessionId = sessionId,
                        attendanceStatus = data.status,
                        remarks = data.remarks,
                        markedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                }
                attendanceRepository.upsertAll(recordsToSave) // Use upsertAll for efficiency
                withContext(Dispatchers.Main) {
                    _errorMessage.postValue("Attendance saved successfully.")
                    setAttendanceMarkedStatus(true) // Confirm attendance is marked
                    setEditMode(false) // Exit edit mode after saving
                }

                Log.d(TAG, "saveAttendance: Upsert successful. Real-time listener will update data.")

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.postValue("Failed to save attendance: ${e.message}")
                }
                Log.e(TAG, "Error saving attendance: ${e.message}", e)
            } finally {
                withContext(Dispatchers.Main) { _isLoading.postValue(false) }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}