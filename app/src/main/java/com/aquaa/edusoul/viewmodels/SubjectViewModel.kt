// File: main/java/com/aquaa/edusoul/viewmodels/SubjectViewModel.kt
package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.repositories.SubjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubjectViewModel(private val repository: SubjectRepository) : ViewModel() {

    // LiveData for all subjects to display in the RecyclerView
    val allSubjects: LiveData<List<Subject>> = repository.getAllSubjects().asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Changed 'subject: Subject' parameter type implicitly now that Subject model has String ID
    fun addSubject(subject: Subject) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if subject code already exists
                if (!subject.subjectCode.isNullOrBlank() && repository.isSubjectCodeExists(subject.subjectCode!!)) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Subject code '${subject.subjectCode}' already exists."
                    }
                    return@launch
                }

                // Repository's insertSubject now returns String (Firestore ID)
                val newRowId = repository.insertSubject(subject)
                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) { // Check for non-blank String ID
                        _errorMessage.value = "Subject added successfully!"
                    } else {
                        _errorMessage.value = "Failed to add subject."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error adding subject: ${e.message}"
                }
                Log.e("SubjectViewModel", "Error adding subject", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    // Changed 'subject: Subject' parameter type implicitly now that Subject model has String ID
    fun updateSubject(subject: Subject) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if subject code already exists, excluding the current subject being edited
                if (!subject.subjectCode.isNullOrBlank() && repository.isSubjectCodeExists(subject.subjectCode!!, subject.id)) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Subject code '${subject.subjectCode}' already exists."
                    }
                    return@launch
                }

                // Repository's updateSubject now returns Int (rows affected)
                val rowsAffected = repository.updateSubject(subject)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Subject updated successfully!"
                    } else {
                        _errorMessage.value = "Failed to update subject."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating subject: ${e.message}"
                }
                Log.e("SubjectViewModel", "Error updating subject", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    // Changed parameter type from Long to String
    fun deleteSubject(subjectId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Repository's deleteSubjectById now returns Int (rows affected)
                val rowsAffected = repository.deleteSubjectById(subjectId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Subject deleted successfully."
                    } else {
                        _errorMessage.value = "Failed to delete subject."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting subject: ${e.message}"
                }
                Log.e("SubjectViewModel", "Error deleting subject", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}