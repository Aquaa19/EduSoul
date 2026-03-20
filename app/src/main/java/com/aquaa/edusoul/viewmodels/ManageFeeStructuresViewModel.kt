package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.FeeStructure
import com.aquaa.edusoul.repositories.FeeStructureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageFeeStructuresViewModel(private val repository: FeeStructureRepository) : ViewModel() {

    // LiveData for all fee structures to display in the RecyclerView
    val allFeeStructures: LiveData<List<FeeStructure>> = repository.getAllFeeStructures().asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Changed 'feeStructure: FeeStructure' parameter type implicitly now that FeeStructure model has String ID
    fun addFeeStructure(feeStructure: FeeStructure) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if fee structure title already exists
                if (repository.feeStructureTitleExists(feeStructure.title)) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Fee structure with this title already exists."
                    }
                    return@launch
                }

                // Repository's insertFeeStructure now returns String (Firestore ID)
                val newRowId = repository.insertFeeStructure(feeStructure)
                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) { // Check for non-blank String ID
                        _errorMessage.value = "Fee structure added successfully!"
                    } else {
                        _errorMessage.value = "Failed to add fee structure."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error adding fee structure: ${e.message}"
                }
                Log.e("ManageFeeStructuresVM", "Error adding fee structure", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    // Changed 'feeStructure: FeeStructure' parameter type implicitly now that FeeStructure model has String ID
    fun updateFeeStructure(feeStructure: FeeStructure) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if fee structure title already exists, excluding the current fee structure being edited
                // Repository's countFeeStructuresWithTitle expects String ID
                if (repository.countFeeStructuresWithTitle(feeStructure.title, feeStructure.id) > 0) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Fee structure with this title already exists."
                    }
                    return@launch
                }

                // Repository's updateFeeStructure now returns Int (rows affected)
                val rowsAffected = repository.updateFeeStructure(feeStructure)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Fee structure updated successfully!"
                    } else {
                        _errorMessage.value = "Failed to update fee structure."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating fee structure: ${e.message}"
                }
                Log.e("ManageFeeStructuresVM", "Error updating fee structure", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    // Changed parameter type from Long to String
    fun deleteFeeStructure(feeStructureId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Repository's deleteFeeStructureById now returns Int (rows affected)
                val rowsAffected = repository.deleteFeeStructureById(feeStructureId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Fee structure deleted successfully."
                    } else {
                        _errorMessage.value = "Failed to delete fee structure."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting fee structure: ${e.message}"
                }
                Log.e("ManageFeeStructuresVM", "Error deleting fee structure", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}