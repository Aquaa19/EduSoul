// File: main/java/com/aquaa/edusoul/viewmodels/TeacherViewScheduleViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.ClassSession
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherViewScheduleViewModel(
    private val classSessionRepository: ClassSessionRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "TeacherViewScheduleVM"

    private val _teacherUserId = MutableLiveData<String>()
    private val _selectedDate = MutableLiveData<String>()

    val sessionsForSelectedDate: LiveData<List<ClassSession>> = _selectedDate.switchMap { date ->
        _teacherUserId.switchMap { teacherId ->
            liveData(Dispatchers.IO) {
                if (teacherId.isBlank() || date.isBlank()) {
                    emit(emptyList())
                    return@liveData
                }
                // Fix: Changed getClassSessionsForTeacherByDate to getScheduledSessionsForTeacher
                val sessions = classSessionRepository.getScheduledSessionsForTeacher(teacherId, date).firstOrNull() ?: emptyList()
                emit(sessions)
            }
        }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setTeacherUserId(userId: String) {
        _teacherUserId.value = userId
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    suspend fun getSubjectNameById(subjectId: String?): String {
        return subjectId?.let { subjectRepository.getSubjectById(it)?.subjectName } ?: "Unknown Subject"
    }

    suspend fun getBatchNameById(batchId: String?): String {
        return batchId?.let { batchRepository.getBatchById(it)?.batchName } ?: "Unknown Batch"
    }

    suspend fun getTeacherNameById(teacherId: String?): String {
        return teacherId?.let { userRepository.getUserById(it)?.fullName } ?: "Unknown Teacher"
    }
}