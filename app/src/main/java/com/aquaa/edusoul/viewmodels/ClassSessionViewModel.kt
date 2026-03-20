package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.dialogs.AiAssistantDialogFragment.Companion.TAG
import com.aquaa.edusoul.models.*
import com.aquaa.edusoul.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClassSessionViewModel(
    private val repository: ClassSessionRepository,
    private val recurringClassSessionRepository: RecurringClassSessionRepository,
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedDate = MutableLiveData<String>()

    // LiveData for the main RecyclerView (used by Admin/Teacher)
    val sessionsForSelectedDate: LiveData<List<ClassSession>> = _selectedDate.switchMap { date ->
        repository.getClassSessionsForDate(date).asLiveData()
    }

    // --- NEW LiveData for Manager's Schedule View ---
    private val _sessions = MutableLiveData<List<ClassSession>>()
    val sessions: LiveData<List<ClassSession>> get() = _sessions


    // LiveData for the dialog spinners
    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData()
    val allSubjects: LiveData<List<Subject>> = subjectRepository.getAllSubjects().asLiveData()
    val allTeachers: LiveData<List<User>> = userRepository.getAllTeachers().asLiveData()


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _isDataReadyForDialog = MediatorLiveData<Boolean>()
    val isDataReadyForDialog: LiveData<Boolean> get() = _isDataReadyForDialog

    init {
        _isDataReadyForDialog.value = false

        val updater = {
            val batchesLoaded = allBatches.value != null
            val subjectsLoaded = allSubjects.value != null
            val teachersLoaded = allTeachers.value != null
            _isDataReadyForDialog.value = batchesLoaded && subjectsLoaded && teachersLoaded
        }

        _isDataReadyForDialog.addSource(allBatches) { updater() }
        _isDataReadyForDialog.addSource(allSubjects) { updater() }
        _isDataReadyForDialog.addSource(allTeachers) { updater() }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                calendar.time = sdf.parse(date) ?: return@launch
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val recurringSessions = recurringClassSessionRepository.getRecurringSessionsForDay(dayOfWeek)

                for (recurrSession in recurringSessions) {
                    val existingSession = repository.findClassSessionByDetails(
                        date = date,
                        batchId = recurrSession.batchId,
                        startTime = recurrSession.startTime
                    )

                    if (existingSession == null) {
                        val newSession = ClassSession(
                            id = "",
                            batchId = recurrSession.batchId,
                            subjectId = recurrSession.subjectId,
                            teacherUserId = recurrSession.teacherUserId,
                            sessionDate = date,
                            startTime = recurrSession.startTime,
                            endTime = recurrSession.endTime,
                            topicCovered = null,
                            status = "Scheduled",
                            isAttendanceMarked = false
                        )
                        val newRowId = repository.insertClassSession(newSession)
                        if (newRowId.isNotBlank()) {
                            Log.d("ClassSessionViewModel", "Synced new session for Batch ID ${recurrSession.batchId} on $date at ${recurrSession.startTime}")
                        } else {
                            Log.e("ClassSessionViewModel", "Failed to sync new session for Batch ID ${recurrSession.batchId} on $date at ${recurrSession.startTime}")
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error syncing weekly schedule: ${e.message}")
                Log.e("ClassSessionViewModel", "Error syncing weekly schedule", e)
            }
        }
    }

    /**
     * NEW FUNCTION
     * Loads all class sessions for a given date. Used by the Manager's schedule view.
     * @param date The date for which to load sessions.
     */
    fun loadSessionsForDate(date: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getSessionsByDate(date).collect { sessionList ->
                    _sessions.value = sessionList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading sessions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun generateAndSyncMonthlySessions(year: Int, month: Int, teacherId: String, allBatches: List<Batch>) {
        withContext(Dispatchers.IO) {
            _isLoading.postValue(true)
            _errorMessage.postValue(null)
            try {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                var sessionsGeneratedCount = 0

                for (dayOfMonth in 1..lastDayOfMonth) {
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    val currentDate = sdfDate.format(calendar.time)
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                    val recurringSessionsForDay = recurringClassSessionRepository.getRecurringSessionsForDay(dayOfWeek)

                    for (recurrSession in recurringSessionsForDay) {
                        val existingSession = repository.findClassSessionByDetails(
                            date = currentDate,
                            batchId = recurrSession.batchId,
                            startTime = recurrSession.startTime
                        )

                        if (existingSession == null) {
                            val newSession = ClassSession(
                                id = "",
                                batchId = recurrSession.batchId,
                                subjectId = recurrSession.subjectId,
                                teacherUserId = recurrSession.teacherUserId,
                                sessionDate = currentDate,
                                startTime = recurrSession.startTime,
                                endTime = recurrSession.endTime,
                                topicCovered = null,
                                status = "Scheduled",
                                isAttendanceMarked = false
                            )
                            val newRowId = repository.insertClassSession(newSession)
                            if (newRowId.isNotBlank()) {
                                sessionsGeneratedCount++
                                Log.d(TAG, "Generated new session: ${newSession.sessionDate} ${newSession.startTime} for Batch ${newSession.batchId}")
                            } else {
                                Log.w(TAG, "Failed to insert new session for ${newSession.sessionDate} ${newSession.startTime}.")
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if (sessionsGeneratedCount > 0) {
                        _errorMessage.value = "$sessionsGeneratedCount class sessions generated for ${SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)}."
                    } else {
                        _errorMessage.value = "No new class sessions to generate for this month based on timetable."
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error generating monthly sessions: ${e.message}"
                }
                Log.e(TAG, "Error generating monthly sessions", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }


    fun addClassSession(session: ClassSession) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (session.sessionDate.isBlank() || session.startTime.isBlank() || session.endTime.isBlank()) {
                    _errorMessage.value = "Date, start time, and end time cannot be empty."
                    _isLoading.value = false
                    return@launch
                }
                val newRowId = repository.insertClassSession(session)
                if (newRowId.isNotBlank()) {
                    _errorMessage.value = "Session added successfully."
                } else {
                    _errorMessage.value = "Failed to add session."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding session: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateClassSession(session: ClassSession) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rowsAffected = repository.updateClassSession(session)
                if (rowsAffected > 0) {
                    _errorMessage.value = "Session updated successfully."
                } else {
                    _errorMessage.value = "Failed to update session."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating session: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getSessionsForMonth(startDate: String, endDate: String): List<ClassSession> {
        return repository.getClassSessionsForDateRange(startDate, endDate)
    }

    fun deleteClassSession(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rowsAffected = repository.deleteClassSessionById(sessionId)
                if (rowsAffected > 0) {
                    _errorMessage.value = "Session deleted successfully."
                } else {
                    _errorMessage.value = "Failed to delete session."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting session: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
