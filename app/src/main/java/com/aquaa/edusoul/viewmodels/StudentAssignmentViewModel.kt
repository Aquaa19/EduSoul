// In main/java/com/aquaa/edusoul/viewmodels/StudentAssignmentViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.StudentAssignment
import com.aquaa.edusoul.repositories.StudentAssignmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentAssignmentViewModel(private val repository: StudentAssignmentRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun addStudentAssignment(studentAssignment: StudentAssignment) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newRowId = repository.insertStudentAssignment(studentAssignment)
                withContext(Dispatchers.Main) {
                    // FIX: Change comparison to check if the String ID is not blank
                    if (newRowId.isNotBlank()) {
                        _errorMessage.value = "Student assignment added successfully!"
                    } else {
                        _errorMessage.value = "Failed to add student assignment."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error adding student assignment: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateStudentAssignment(studentAssignment: StudentAssignment) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = repository.updateStudentAssignment(studentAssignment)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Student assignment updated successfully!"
                    } else {
                        _errorMessage.value = "Failed to update student assignment."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error updating student assignment: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // FIX: Change parameter type from Long to String
    fun deleteStudentAssignment(studentAssignmentId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // repository.deleteStudentAssignmentById now expects String
                val rowsAffected = repository.deleteStudentAssignmentById(studentAssignmentId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Student assignment deleted successfully!"
                    } else {
                        _errorMessage.value = "Failed to delete student assignment."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error deleting student assignment: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // FIX: Change parameter type from Long to String
    fun getStudentAssignmentsByHomework(homeworkId: String): LiveData<List<StudentAssignment>> {
        // repository.getStudentAssignmentsByHomework now expects String
        return repository.getStudentAssignmentsByHomework(homeworkId).asLiveData()
    }

    // FIX: Change parameter type from Long to String
    fun getStudentAssignmentsByStudent(studentId: String): LiveData<List<StudentAssignment>> {
        // repository.getStudentAssignmentsByStudent now expects String
        return repository.getStudentAssignmentsByStudent(studentId).asLiveData()
    }
}
