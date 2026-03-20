package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.RecurringClassSession
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.RecurringClassSessionRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn // Import flowOn
import kotlinx.coroutines.flow.map // Import map
import kotlinx.coroutines.flow.onEach // Import onEach
import kotlinx.coroutines.flow.catch // Import catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetWeeklyTimetableViewModel(
    private val recurringSessionRepository: RecurringClassSessionRepository,
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedBatchId = MutableLiveData<String>()
    val selectedBatchId: LiveData<String> get() = _selectedBatchId

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // --- MODIFICATION START ---
    val recurringSessionsForBatch: LiveData<List<RecurringClassSession>> = _selectedBatchId.switchMap { batchId ->
        if (batchId.isNotBlank()) {
            recurringSessionRepository.getRecurringSessionsForBatch(batchId)
                .onEach { _isLoading.postValue(true) } // Set loading to true when flow starts emitting
                .map { sessions ->
                    _isLoading.postValue(false) // Set loading to false when data is received
                    sessions
                }
                .catch { e ->
                    _errorMessage.postValue("Error loading sessions: ${e.message}")
                    _isLoading.postValue(false) // Set loading to false on error
                    emit(emptyList()) // Emit empty list on error
                }
                .flowOn(Dispatchers.IO)
                .asLiveData(viewModelScope.coroutineContext) // Use viewModelScope for lifecycle awareness
        } else {
            MutableLiveData(emptyList())
        }
    }
    // --- MODIFICATION END ---

    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData(Dispatchers.IO)
    val allSubjects: LiveData<List<Subject>> = subjectRepository.getAllSubjects().asLiveData(Dispatchers.IO)
    val allTeachers: LiveData<List<User>> = userRepository.getAllTeachers().asLiveData(Dispatchers.IO)


    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun selectBatch(batchId: String) {
        if (_selectedBatchId.value != batchId) {
            _selectedBatchId.value = batchId
        }
    }

    fun addOrUpdateRecurringSession(session: RecurringClassSession) {
        _isLoading.value = true // Set loading to true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val newRowId = if (session.id.isNotBlank()) {
                    recurringSessionRepository.updateRecurringSession(session)
                    session.id
                } else {
                    recurringSessionRepository.insert(session)
                }

                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) {
                        _errorMessage.value = "Session added/updated successfully."
                    } else {
                        _errorMessage.value = "Failed to add/update session (might be duplicate)."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error adding/updating session: ${e.message}"
                }
                Log.e("SetWeeklyTimetableVM", "Error adding/updating session", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false // Set loading to false on completion/error
                }
            }
        }
    }

    fun deleteRecurringSession(sessionId: String) {
        _isLoading.value = true // Set loading to true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val rowsAffected = recurringSessionRepository.deleteById(sessionId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Session deleted successfully."
                    } else {
                        _errorMessage.value = "Failed to delete session."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting session: ${e.message}"
                }
                Log.e("SetWeeklyTimetableVM", "Error deleting session", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false // Set loading to false on completion/error
                }
            }
        }
    }

    fun clearTimetableForSelectedBatch() {
        val currentBatchId = _selectedBatchId.value
        if (currentBatchId.isNullOrBlank()) {
            _errorMessage.value = "No batch selected to clear timetable."
            return
        }

        _isLoading.value = true // Set loading to true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                recurringSessionRepository.clearTimetableForBatch(currentBatchId)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Timetable cleared for selected batch."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error clearing timetable: ${e.message}"
                }
                Log.e("SetWeeklyTimetableVM", "Error clearing timetable", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false // Set loading to false on completion/error
                }
            }
        }
    }
}