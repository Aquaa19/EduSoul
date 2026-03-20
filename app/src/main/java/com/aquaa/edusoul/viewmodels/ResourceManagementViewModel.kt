// File: src/main/java/com/aquaa/edusoul/viewmodels/ResourceManagementViewModel.kt
package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.dialogs.AiAssistantDialogFragment.Companion.TAG
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.LearningResource
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.LearningResourceRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.UserRepository // Import UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine // Import combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResourceManagementViewModel(
    private val resourceRepository: LearningResourceRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository, // Injected UserRepository
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository
) : ViewModel() {

    private val _currentTeacherId = MutableLiveData<String>() // Used when operating as a teacher
    private val _isAdminMode = MutableLiveData<Boolean>(false) // New LiveData for admin mode

    // Combined LiveData for resources based on role
    val resourcesForTeacherOrAdmin: LiveData<List<LearningResource>> =
        combine(_currentTeacherId.asFlow(), _isAdminMode.asFlow()) { teacherId, isAdminMode ->
            if (isAdminMode) {
                // If admin, fetch all resources
                resourceRepository.getAllLearningResources()
            } else if (teacherId.isNotBlank()) {
                // If teacher, fetch resources by their ID
                resourceRepository.getLearningResourcesByTeacher(teacherId)
            } else {
                flowOf(emptyList()) // No teacher ID or not in admin mode
            }
        }.flatMapLatest { it } // flatMapLatest to switch between different resource flows
            .asLiveData(Dispatchers.IO)


    // LiveData for batches dropdown - will be all batches for admin, assigned for teacher
    val batchesForDropdown: LiveData<List<Batch>> = combine(_currentTeacherId.asFlow(), _isAdminMode.asFlow()) { teacherId, isAdminMode ->
        if (isAdminMode) {
            batchRepository.getAllBatches()
        } else if (teacherId.isNotBlank()) {
            batchRepository.getBatchesForTeacher(teacherId)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
        .asLiveData(Dispatchers.IO)


    // LiveData for subjects dropdown - will be all subjects for admin, assigned for teacher
    val subjectsForDropdown: LiveData<List<Subject>> = combine(_currentTeacherId.asFlow(), _isAdminMode.asFlow()) { teacherId, isAdminMode ->
        if (isAdminMode) {
            subjectRepository.getAllSubjects()
        } else if (teacherId.isNotBlank()) {
            teacherSubjectBatchLinkRepository.getTeacherSubjectBatchLinksForTeacher(teacherId)
                .flatMapLatest { links ->
                    if (links.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        subjectRepository.getAllSubjects().map { allSubjects ->
                            val assignedSubjectIds = links.map { it.subjectId }.toSet()
                            allSubjects.filter { it.id in assignedSubjectIds }
                        }
                    }
                }
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
        .asLiveData(Dispatchers.IO)


    // REMOVED: NEW: LiveData for teachers dropdown (only for admin mode)
    /*
    val teachersForDropdown: LiveData<List<User>> = _isAdminMode.switchMap { isAdmin ->
        Log.d(TAG, "ViewModel: _isAdminMode switchMap triggered. isAdmin = $isAdmin")
        if (isAdmin) {
            Log.d(TAG, "ViewModel: Fetching all teachers via userRepository.getAllTeachers()")
            userRepository.getAllTeachers().asLiveData(Dispatchers.IO)
        } else {
            Log.d(TAG, "ViewModel: Not in Admin mode. Returning empty teacher list.")
            MutableLiveData(emptyList())
        }
    }
    */

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
        Log.d("ResourceManagementVM", "setLoading called: $isLoading")
    }

    fun setTeacherId(teacherId: String) {
        Log.d(TAG, "ViewModel: setTeacherId called: $teacherId")
        _currentTeacherId.value = teacherId
        _isAdminMode.value = false // Ensure admin mode is off if setting teacher ID
    }

    // New function to set admin mode
    fun setAdminMode() {
        Log.d(TAG, "ViewModel: setAdminMode called.")
        _isAdminMode.value = true
        _currentTeacherId.value = "" // Clear teacher ID if in admin mode
    }

    fun addResource(resource: LearningResource) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newRowId = resourceRepository.insertLearningResource(resource)
                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) {
                        _errorMessage.value = "Resource uploaded successfully."
                    } else {
                        _errorMessage.value = "Failed to upload resource."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error uploading resource: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateResource(resource: LearningResource) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = resourceRepository.updateLearningResource(resource)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Resource updated successfully."
                    } else {
                        _errorMessage.value = "Failed to update resource."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating resource: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteResource(resourceId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = resourceRepository.deleteLearningResourceById(resourceId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Resource deleted."
                    } else {
                        _errorMessage.value = "Failed to delete resource."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting resource: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}