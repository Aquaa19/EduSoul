package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.TeacherAssignmentDetails
import com.aquaa.edusoul.models.TeacherSubjectBatchLink
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeacherAssignmentViewModel(
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository
) : ViewModel() {

    private val TAG = "TeacherAssignmentVM"

    // Changed type from Long to String for currentTeacherId
    private val _currentTeacherId = MutableLiveData<String>()
    val currentTeacherId: LiveData<String> get() = _currentTeacherId

    // LiveData to track the assignment status of the currently selected subject/batch
    // True if an assignment exists for the current teacher, selected subject, and selected batch
    private val _isCurrentSelectionAssigned = MutableLiveData<Boolean>()
    val isCurrentSelectionAssigned: LiveData<Boolean> get() = _isCurrentSelectionAssigned

    val allSubjects: LiveData<List<Subject>> = subjectRepository.getAllSubjects().asLiveData(Dispatchers.IO)
    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData(Dispatchers.IO)

    private val _teacherAssignments = MutableLiveData<List<TeacherAssignmentDetails>>()
    val teacherAssignments: LiveData<List<TeacherAssignmentDetails>> get() = _teacherAssignments

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        // Observe currentTeacherId changes to fetch assignments for that teacher
        _currentTeacherId.observeForever { teacherId ->
            if (teacherId.isNotBlank()) {
                fetchTeacherAssignments(teacherId)
            } else {
                _teacherAssignments.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _currentTeacherId.removeObserver {  } // Remove the observer set in init
    }

    // Changed parameter type from Long to String
    fun setCurrentTeacherId(teacherId: String) {
        _currentTeacherId.value = teacherId
    }

    private fun fetchTeacherAssignments(teacherId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val links = teacherSubjectBatchLinkRepository.getTeacherSubjectBatchLinksForTeacher(teacherId).firstOrNull() ?: emptyList()
                val assignmentsDetails = mutableListOf<TeacherAssignmentDetails>()

                links.forEach { link ->
                    val subject = subjectRepository.getSubjectById(link.subjectId)
                    val batch = batchRepository.getBatchById(link.batchId)

                    if (subject != null && batch != null) {
                        assignmentsDetails.add(
                            TeacherAssignmentDetails(
                                assignmentId = link.id, // Use the correct ID for the assignment link
                                teacherId = link.teacherUserId,
                                subjectId = link.subjectId,
                                batchId = link.batchId,
                                subjectName = subject.subjectName,
                                batchName = batch.batchName
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    _teacherAssignments.value = assignmentsDetails.sortedBy { it.subjectName + it.batchName }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error fetching assignments: ${e.message}"
                    Log.e(TAG, "Error fetching assignments", e)
                }
            }
        }
    }

    fun assignTeacherToSubjectAndBatch(teacherId: String, subjectId: String, batchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // First, check if the assignment already exists
                val existingLink = teacherSubjectBatchLinkRepository.getExistingLink(teacherId, subjectId, batchId)
                if (existingLink != null) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Teacher is already assigned to this subject and batch."
                        _isCurrentSelectionAssigned.value = true // Ensure UI reflects existing assignment
                    }
                    return@launch
                }

                val newLink = TeacherSubjectBatchLink(
                    id = "", // Firestore will generate the ID, or in this composite ID case, it's set in repo
                    teacherUserId = teacherId,
                    subjectId = subjectId,
                    batchId = batchId
                )
                val assignedId = teacherSubjectBatchLinkRepository.insertTeacherSubjectBatchLink(newLink)

                withContext(Dispatchers.Main) {
                    if (assignedId.isNotBlank()) {
                        _errorMessage.value = "Assignment successful!"
                        _isCurrentSelectionAssigned.value = true // Update status to assigned
                        fetchTeacherAssignments(teacherId) // Refresh the list of assignments
                    } else {
                        _errorMessage.value = "Assignment failed."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error assigning teacher: ${e.message}"
                    Log.e(TAG, "Error assigning teacher", e)
                }
            }
        }
    }

    fun removeTeacherAssignment(assignmentLinkId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = teacherSubjectBatchLinkRepository.deleteTeacherSubjectBatchLink(assignmentLinkId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Assignment removed successfully."
                        _isCurrentSelectionAssigned.value = false // Update status to not assigned
                        fetchTeacherAssignments(_currentTeacherId.value ?: "") // Refresh the list
                    } else {
                        _errorMessage.value = "Failed to remove assignment."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error removing assignment: ${e.message}"
                    Log.e(TAG, "Error removing assignment", e)
                }
            }
        }
    }

    /**
     * Checks if the currently selected subject and batch are assigned to the current teacher.
     * Call this when spinner selections change.
     */
    fun checkCurrentSelectionAssignment(teacherId: String, selectedSubjectId: String?, selectedBatchId: String?) {
        if (selectedSubjectId.isNullOrBlank() || selectedBatchId.isNullOrBlank() || teacherId.isBlank()) {
            _isCurrentSelectionAssigned.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingLink = teacherSubjectBatchLinkRepository.getExistingLink(teacherId, selectedSubjectId, selectedBatchId)
                withContext(Dispatchers.Main) {
                    _isCurrentSelectionAssigned.value = (existingLink != null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking current selection assignment status: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isCurrentSelectionAssigned.value = false
                }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}