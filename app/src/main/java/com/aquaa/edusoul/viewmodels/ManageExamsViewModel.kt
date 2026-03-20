// File: main/java/com/aquaa/edusoul/viewmodels/ManageExamsViewModel.kt
package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User // Import the User model
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository // Import UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageExamsViewModel(
    private val examRepository: ExamRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository // Injected UserRepository
) : ViewModel() {

    private val TAG = "ManageExamsViewModel" // Added TAG for logging

    // LiveData for all exams to display in the RecyclerView
    val allExams: LiveData<List<Exam>> = examRepository.getAllExams().asLiveData(Dispatchers.IO)

    // LiveData for dialog spinners
    val allSubjects: LiveData<List<Subject>> = subjectRepository.getAllSubjects().asLiveData(Dispatchers.IO)
    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData(Dispatchers.IO)

    // New LiveData for all teacher users to populate the teacher spinner
    val allTeachers: LiveData<List<User>> = userRepository.getAllTeachers().asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Modified: Added teacherId parameter
    fun addExam(exam: Exam, teacherId: String?) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Assign the selected teacher ID to the exam object
                exam.teacherId = teacherId
                Log.d(TAG, "Adding exam: ${exam.examName} with assigned teacherId: ${exam.teacherId}")

                val newRowId = examRepository.insertExam(exam)
                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) {
                        _errorMessage.value = "Exam added successfully!"
                    } else {
                        _errorMessage.value = "Failed to add exam."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error adding exam: ${e.message}"
                }
                Log.e(TAG, "Error adding exam", e) // Log the exception
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    // Modified: Added teacherId parameter
    fun updateExam(exam: Exam, teacherId: String?) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Assign the selected teacher ID to the exam object
                exam.teacherId = teacherId
                Log.d(TAG, "Updating exam: ${exam.examName} with assigned teacherId: ${exam.teacherId}")

                val rowsAffected = examRepository.updateExam(exam)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Exam updated successfully!"
                    } else {
                        _errorMessage.value = "Failed to update exam."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating exam: ${e.message}"
                }
                Log.e(TAG, "Error updating exam", e) // Log the exception
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    // Changed parameter type from Long to String
    fun deleteExam(examId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Repository's deleteExamById now returns Int (rows affected)
                val rowsAffected = examRepository.deleteExamById(examId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Exam deleted successfully."
                    } else {
                        _errorMessage.value = "Failed to delete exam."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting exam: ${e.message}"
                }
                Log.e(TAG, "Error deleting exam", e) // Log the exception
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}