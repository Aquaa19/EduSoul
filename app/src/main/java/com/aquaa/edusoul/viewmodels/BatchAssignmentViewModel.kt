// In main/java/com/aquaa/edusoul/viewmodels/BatchAssignmentViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.BatchAssignment
import com.aquaa.edusoul.repositories.BatchAssignmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatchAssignmentViewModel(private val repository: BatchAssignmentRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun addBatchAssignment(batchAssignment: BatchAssignment) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newRowId = repository.insertBatchAssignment(batchAssignment)
                withContext(Dispatchers.Main) {
                    // FIX: Change comparison to check if the String ID is not blank
                    if (newRowId.isNotBlank()) {
                        _errorMessage.value = "Batch assignment added successfully!"
                    } else {
                        _errorMessage.value = "Failed to add batch assignment."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error adding batch assignment: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateBatchAssignment(batchAssignment: BatchAssignment) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = repository.updateBatchAssignment(batchAssignment)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Batch assignment updated successfully!"
                    } else {
                        _errorMessage.value = "Failed to update batch assignment."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error updating batch assignment: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // FIX: Change parameter type from Long to String
    fun deleteBatchAssignment(batchAssignmentId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // repository.deleteBatchAssignmentById now expects String
                val rowsAffected = repository.deleteBatchAssignmentById(batchAssignmentId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Batch assignment deleted successfully!"
                    } else {
                        _errorMessage.value = "Failed to delete batch assignment."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error deleting batch assignment: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // FIX: Change parameter type from Long to String
    fun getBatchAssignmentsByHomework(homeworkId: String): LiveData<List<BatchAssignment>> {
        // repository.getBatchAssignmentsByHomework now expects String
        return repository.getBatchAssignmentsByHomework(homeworkId).asLiveData()
    }

    // FIX: Change parameter types from Long to String
    fun getBatchAssignment(homeworkId: String, batchId: String): LiveData<BatchAssignment?> {
        val liveData = MutableLiveData<BatchAssignment?>()
        viewModelScope.launch(Dispatchers.IO) {
            // repository.getBatchAssignment now expects String
            val assignment = repository.getBatchAssignment(homeworkId, batchId)
            liveData.postValue(assignment)
        }
        return liveData
    }
}
